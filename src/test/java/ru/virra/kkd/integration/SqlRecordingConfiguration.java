package ru.virra.kkd.integration;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Test-only observation of actual JDBC calls made by Hibernate, without replacing the driver. */
@TestConfiguration
class SqlRecordingConfiguration {
    @Bean
    static JdbcCalls jdbcCalls() {
        return new JdbcCalls();
    }

    @Bean
    static BeanPostProcessor recordingDataSource(JdbcCalls calls) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                return bean instanceof DataSource dataSource
                        ? new RecordingDataSource(dataSource, calls) : bean;
            }
        };
    }

    static final class JdbcCalls {
        private long dataSelects;
        private long unbatchedResultInserts;
        private final List<Integer> issueBatches = new ArrayList<>();
        private final List<Integer> metricBatches = new ArrayList<>();

        synchronized void reset() {
            dataSelects = 0;
            unbatchedResultInserts = 0;
            issueBatches.clear();
            metricBatches.clear();
        }

        synchronized long dataSelects() { return dataSelects; }
        synchronized long unbatchedResultInserts() { return unbatchedResultInserts; }
        synchronized List<Integer> issueBatches() { return List.copyOf(issueBatches); }
        synchronized List<Integer> metricBatches() { return List.copyOf(metricBatches); }

        synchronized void prepared(String sql) {
            if (sql.startsWith("select") && !sql.contains("nextval(")) dataSelects++;
        }

        synchronized void batch(String sql, int size) {
            if (sql.startsWith("insert into kkd_issue ")) issueBatches.add(size);
            if (sql.startsWith("insert into kkd_run_metric ")) metricBatches.add(size);
        }

        synchronized void individualInsert(String sql) {
            if (sql.startsWith("insert into kkd_issue ") || sql.startsWith("insert into kkd_run_metric ")) {
                unbatchedResultInserts++;
            }
        }
    }

    private static final class RecordingDataSource extends DelegatingDataSource implements AutoCloseable {
        private final JdbcCalls calls;

        RecordingDataSource(DataSource delegate, JdbcCalls calls) {
            super(delegate);
            this.calls = calls;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return connection(super.getConnection());
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return connection(super.getConnection(username, password));
        }

        private Connection connection(Connection delegate) {
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                        Object result = invoke(delegate, method, args);
                        if (method.getName().equals("prepareStatement") && args[0] instanceof String sql) {
                            String normalized = sql.stripLeading().toLowerCase(Locale.ROOT);
                            calls.prepared(normalized);
                            return statement((PreparedStatement) result, normalized);
                        }
                        return result;
                    });
        }

        private PreparedStatement statement(PreparedStatement delegate, String sql) {
            int[] pending = {0};
            return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                    new Class<?>[]{PreparedStatement.class}, (proxy, method, args) -> {
                        Object result = invoke(delegate, method, args);
                        switch (method.getName()) {
                            case "addBatch" -> pending[0]++;
                            case "clearBatch" -> pending[0] = 0;
                            case "executeBatch", "executeLargeBatch" -> {
                                calls.batch(sql, pending[0]);
                                pending[0] = 0;
                            }
                            case "executeUpdate", "executeLargeUpdate", "execute" -> calls.individualInsert(sql);
                            default -> { }
                        }
                        return result;
                    });
        }

        @Override
        public void close() throws Exception {
            if (getTargetDataSource() instanceof AutoCloseable closeable) closeable.close();
        }
    }

    private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }
}
