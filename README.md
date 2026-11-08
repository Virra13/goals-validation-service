# Контроль качества данных по целям

Сервис проверяет активные цели и их ключевые результаты (КР), сохраняет результаты в PostgreSQL и отдаёт сводку, список проблем и сравнение прогонов через REST API. Стек проекта — Java 17, Spring Boot 3, PostgreSQL 15, Liquibase и Maven.

## Запуск

Из корня проекта с запущенным Docker:

```bash
docker compose up --build -d
```

Compose запускает PostgreSQL и приложение. При старте Liquibase создаёт схему и загружает 24 цели (20 активных) и два демопрогона. В демоданных шесть подразделений на трёх уровнях, чтобы показать вложенные ветви.

После завершения запуска API доступен на `http://localhost:8080`, PostgreSQL — на порту `5432`. Запуск приложения можно проверить через `docker compose logs kkd`.

Для переопределения настроек скопируйте [`.env.example`](.env.example) в `.env` и измените нужные значения. `DB_PORT` и `APP_PORT` задают порты на машине, например если локальный PostgreSQL уже занял `5432`. Остановить контейнеры с сохранением данных можно командой `docker compose down`.

## Примеры API

```bash
curl "http://localhost:8080/api/runs"
curl "http://localhost:8080/api/kkd/summary?run_id=2&unit_id=1"
curl "http://localhost:8080/api/kkd/summary?run_id=2&unit_id=2"
curl "http://localhost:8080/api/kkd/summary?run_id=1&unit_id=1"
curl "http://localhost:8080/api/kkd/issues?run_id=2"
curl "http://localhost:8080/api/kkd/issues?run_id=2&offset=5"
curl "http://localhost:8080/api/kkd/issues?run_id=2&unit_id=2&rule_code=GOAL_NO_KR&severity=critical"
```

В Windows PowerShell используйте `curl.exe`. Сводка для корневого подразделения (`unit_id=1`) включает все ветви. Во втором прогоне 10 проблем, из них 6 новых и 4 сохраняющиеся. Ещё 3 проблемы из первого прогона исправлены. У первого прогона поля сравнения равны `null`.

Между прогонами доля целей с ответственным по всей дирекции снизилась с 90% до 85%. В департаменте клиентских решений и его дочернем подразделении (`unit_id=2`) та же метрика выросла с 87,5% до 100%. В сводке это даёт `worse` и `better` соответственно.

Два запроса списка проблем возвращают по пять разных записей с `total: 10`. Запрос с фильтрами возвращает одну критическую проблему из дочернего подразделения. Несуществующий прогон или подразделение даёт HTTP 404, неверный параметр — HTTP 422. Оба ответа содержат объект `error` с полями `code` и `message`.

Размер страницы `limit` ограничен диапазоном 1–500. Пустой переданный `unit_id=` возвращает 422; отсутствие параметра оставляет выборку без фильтра подразделения.

Новые прогоны рассчитывает внутренний `KkdRunExecutionService.execute(comparedToRunId)`. Публичный HTTP-запуск и расписание не входят в три обязательных GET API и не реализованы. Для периодической работы нужен вызывающий адаптер существующего проекта. Сравнение разрешено только с завершённым прогоном.

## Тесты и миграции

Для тестов нужен JDK 17 или новее. Тесты JUnit 5 проверяют границы веса и давности обновления, расчёт метрик и сравнение прогонов. Они запускаются без PostgreSQL.

```bash
./mvnw test
```

В PowerShell команда запуска тестов — `.\mvnw.cmd test`. С установленным Maven также работает `mvn test`.

Дополнительные интеграционные тесты используют настоящий PostgreSQL и включаются переменными `KKD_TEST_DB_*`. Для них нужна **отдельная пустая тестовая БД**: Liquibase создаст демоданные, а тесты временно создают свои записи и ограничение таблицы для проверки отката. Не указывайте рабочую БД или БД с общими данными. Без этих переменных интеграционный класс пропускается, остальные 38 сценариев выполняются без БД.

```powershell
$env:KKD_TEST_DB_URL = 'jdbc:postgresql://localhost:5432/kkd_integration'
$env:KKD_TEST_DB_USER = 'user'
$env:KKD_TEST_DB_PASSWORD = '12345'
.\mvnw.cmd test
```

Полный набор содержит 44 сценария. Он проверяет в том числе фактическое сохранение прогонов, одинаковое число SQL SELECT при росте количества подразделений, несколько пакетов INSERT, откат одной JPA-транзакции и API с данными PostgreSQL. Исполняемый JAR собирается командой `.\mvnw.cmd package`; файл — `target/kkd-0.0.1-SNAPSHOT.jar`.

Для проверки полного отката создайте отдельную пустую БД в запущенном PostgreSQL. Ниже указаны стандартные имя пользователя, пароль и порт из Compose. Если вы их переопределили, подставьте свои значения.

```bash
docker compose exec postgres createdb -U kkd kkd_migration_check
export DB_URL=jdbc:postgresql://localhost:5432/kkd_migration_check
export DB_USER=user
export DB_PASSWORD=12345
./mvnw process-resources liquibase:update
./mvnw liquibase:rollback -Dliquibase.rollbackCount=14
./mvnw liquibase:update
```

В PowerShell задайте переменные так и запустите те же три цели:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/kkd_migration_check'
$env:DB_USER = 'user'
$env:DB_PASSWORD = '12345'
.\mvnw.cmd process-resources liquibase:update
.\mvnw.cmd liquibase:rollback '-Dliquibase.rollbackCount=14'
.\mvnw.cmd liquibase:update
```

В журнале Liquibase первый и повторный накат применяют все 14 changeset, а откат снимает все 14. После повторного наката запрос ниже должен вернуть `2`. Для повторной проверки можно удалить созданную для неё БД командой `docker compose exec postgres dropdb -U kkd kkd_migration_check` и создать её заново.

Дополнительно можно проверить частичный откат семи миграций стартовых данных, не удаляя схему: `.\mvnw.cmd liquibase:rollback '-Dliquibase.rollbackCount=7'`, затем `.\mvnw.cmd liquibase:update`. После него должны восстановиться 2 прогона, 24 цели, 17 проблем и 48 значений метрик. Для каждого цикла используйте только отдельную проверочную БД.

```bash
docker compose exec postgres psql -U kkd -d kkd_migration_check -tAc "SELECT count(*) FROM kkd_run"
```

## Архитектура

- [`domain.rule`](src/main/java/ru/virra/kkd/domain/rule) — `RuleChecker` запускает четыре независимых правила для активной цели. `GOAL_NO_OWNER` и `GOAL_NO_KR` дают `critical`, `KR_WEIGHTS_SUM` — `major`, `KR_STALE_PROGRESS` — `minor`. Правила получают данные цели и КР и возвращают список нарушений без обращения к БД.
- [`domain.metric`](src/main/java/ru/virra/kkd/domain/metric) — `MetricsCalculator` считает долю целей без проблем, число проблем на активную цель, долю целей с ответственным и долю целей с корректной суммой весов среди целей с КР. При нулевом знаменателе значение равно `null`.
- [`domain.comparison`](src/main/java/ru/virra/kkd/domain/comparison) — `RunComparator` вычитает прошлое значение метрики из текущего. Рост долей считается улучшением, рост числа проблем на цель — ухудшением. Проблемы сравниваются по правилу, типу и ID объекта. Разность и пересечение множеств дают счётчики новых, сохраняющихся и исправленных проблем.
- [`service`](src/main/java/ru/virra/kkd/service) — `KkdRunExecutionService` читает активные цели, КР и иерархию из одного снимка БД. `UnitMetricsCalculator` складывает счётчики от листьев к корню без запросов по каждому подразделению. Сводка читает узкие проекции проблем без описаний. Прошлый прогон задан ссылкой `compared_to_run_id`. Если ссылки нет, прошлые значения, дельты и тренды возвращаются как `null`.
- [`controller`](src/main/java/ru/virra/kkd/controller) принимает HTTP-запросы и формирует ответы, [`repository`](src/main/java/ru/virra/kkd/repository) выполняет запросы к PostgreSQL. `KkdResultWriter` сохраняет проблемы и метрики через JPA с пакетной отправкой INSERT в одной транзакции с прогоном. [`readmodel`](src/main/java/ru/virra/kkd/readmodel) содержит модели чтения.

## Использование ИИ

Решение разработано с ИИ-ассистентом. Спецификация работы находится в [`specs/`](specs/) и включает [требования и критерии приёмки](specs/requirements.md) и [проектирование](specs/design.md).
