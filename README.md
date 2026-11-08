# Модуль контроля качества данных

Сервис проверяет активные цели и их ключевые результаты (КР), сохраняет результаты в PostgreSQL и отдаёт сводку, список проблем и сравнение прогонов через REST API. Стек проекта — Java 17, Spring Boot 3, PostgreSQL 15, Liquibase и Maven.

## Запуск

Перед первым запуском создайте `.env` из [`.env.example`](.env.example) и при необходимости измените значения. В Windows PowerShell из корня проекта:

```powershell
Copy-Item .env.example .env
```

Если `.env` уже есть, пропустите копирование. Вместо файла можно задать свои `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_PORT`, `APP_PORT` и `KKD_LOG_LEVEL` в окружении перед выполнением команд Compose. В Linux и macOS для копирования используйте `cp .env.example .env`.

Команда запуска приложения:

```bash
docker compose up 
```

Compose запускает PostgreSQL и приложение. При старте Liquibase создаёт схему и загружает 24 цели (20 активных) и два демопрогона. В демоданных шесть подразделений на трёх уровнях, чтобы показать вложенные ветви.

После завершения запуска API доступен на `http://localhost:8080`, PostgreSQL — на порту `5432`. Запуск приложения можно проверить через `docker compose logs kkd`.

Compose берёт значения из `.env` или переменных окружения. `DB_PORT` и `APP_PORT` задают порты на машине, например если локальный PostgreSQL уже занял `5432`. Остановить контейнеры с сохранением данных можно командой `docker compose down`.

`KKD_LOG_LEVEL` задаёт уровень логов приложения (`INFO` по умолчанию, `DEBUG` для этапов расчёта, обхода подразделений и записи пакетов). На `INFO` видны выполненные прогоны, запросы чтения API и отклонённые запросы; `WARN` сообщает о неполном наборе сохранённых метрик. Логи контейнера доступны через `docker compose logs kkd`.

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

## Тесты и миграции

Для тестов нужен JDK 17 или новее. Чтобы запустить все 56 сценариев одной командой, запустите Docker Desktop и выполните из корня проекта:

```powershell
.\mvnw.cmd verify
```

В Linux и macOS используйте `./mvnw verify`, с установленным Maven также работает `mvn verify`. Testcontainers поднимает временный PostgreSQL 15 для шести интеграционных тестов, Liquibase загружает стартовые данные, а после тестов контейнер удаляется. 

Команда `.\mvnw.cmd test` запускает 50 тестов без БД и не требует Docker. При `docker compose up --build -d` сборочный этап Dockerfile выполняет эти 50 тестов; интеграционные тесты запускаются на этапе `verify`. Полный набор проверяет в том числе фактическое сохранение прогонов, число SQL SELECT при росте количества подразделений, пакеты INSERT, откат одной JPA-транзакции и API с данными PostgreSQL. Исполняемый JAR собирается командой `.\mvnw.cmd package`; файл — `target/kkd-0.0.1-SNAPSHOT.jar`.

При обычном запуске приложение автоматически применяет 14 миграций. Полный цикл **накат → откат → повторный накат** можно проверить на той же локальной демобазе. Для этого нужен Maven и JDK 17 или новее на компьютере. Остановите приложение, затем в Windows PowerShell задайте параметры подключения из своего `.env` и выполните команды:

```powershell
docker compose stop kkd
$env:DB_URL = 'jdbc:postgresql://localhost:5432/kkd_db'
$env:DB_USER = 'user'
$env:DB_PASSWORD = '12345'
mvn process-resources liquibase:rollback '-Dliquibase.rollbackCount=14'
mvn liquibase:update
docker compose start kkd
curl.exe 'http://localhost:8080/api/runs'
```

В примере указаны значения из `.env.example`. Если вы изменили `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` или `APP_PORT`, подставьте их в `DB_URL`, учётные данные и адрес API.

## Архитектура

- [`domain.rule`](src/main/java/ru/virra/kkd/domain/rule) — `RuleChecker` запускает четыре независимых правила для активной цели. `GOAL_NO_OWNER` и `GOAL_NO_KR` дают `critical`, `KR_WEIGHTS_SUM` — `major`, `KR_STALE_PROGRESS` — `minor`. Правила получают данные цели и КР и возвращают список нарушений без обращения к БД.
- [`domain.metric`](src/main/java/ru/virra/kkd/domain/metric) — `MetricsCalculator` считает долю целей без проблем, число проблем на активную цель, долю целей с ответственным и долю целей с корректной суммой весов среди целей с КР. При нулевом знаменателе значение равно `null`.
- [`domain.comparison`](src/main/java/ru/virra/kkd/domain/comparison) — `RunComparator` вычитает прошлое значение метрики из текущего. Рост долей считается улучшением, рост числа проблем на цель — ухудшением. Проблемы сравниваются по правилу, типу и ID объекта. Разность и пересечение множеств дают счётчики новых, сохраняющихся и исправленных проблем.
- [`service`](src/main/java/ru/virra/kkd/service) — `KkdRunExecutionService` читает активные цели, КР и иерархию из одного снимка БД. `UnitMetricsCalculator` складывает счётчики от листьев к корню без запросов по каждому подразделению. Сводка читает узкие проекции проблем без описаний. Прошлый прогон задан ссылкой `compared_to_run_id`. Если ссылки нет, прошлые значения, дельты и тренды возвращаются как `null`.
- [`controller`](src/main/java/ru/virra/kkd/controller) принимает HTTP-запросы и формирует ответы, [`repository`](src/main/java/ru/virra/kkd/repository) выполняет запросы к PostgreSQL. `KkdResultWriter` сохраняет проблемы и метрики через JPA с пакетной отправкой INSERT в одной транзакции с прогоном. [`readmodel`](src/main/java/ru/virra/kkd/readmodel) содержит модели чтения.

## Использование ИИ

Решение разработано с ИИ-ассистентом. Спецификация работы находится в [`specs/`](specs/) и включает [требования и критерии приёмки](specs/requirements.md) и [проектирование](specs/design.md).
