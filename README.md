# Logistic document MVP

Информационная система для формирования комплекта перевозочных документов (Spring Boot + Angular + PostgreSQL).

## Регламент разработки

- Каждая логическая задача: код → тесты → `.\gradlew.bat test` (backend) / `npm test` и `npm run build` (frontend) → отдельный коммит.
- Следующий шаг начинается только после успешной проверки и коммита.

## Сброс БД при изменении `V1__init.sql`

Flyway хранит checksum начальной миграции. Если вы меняли [backend/src/main/resources/db/migration/V1__init.sql](backend/src/main/resources/db/migration/V1__init.sql), существующий volume Postgres может дать ошибку checksum. Для локальной разработки пересоздайте том:

```powershell
docker compose down -v
docker compose up -d db
```

Затем снова поднимите сервисы или выполните `.\gradlew.bat bootRun` против чистой БД.

## Требования

- JDK 17+, Node.js 20+, Docker (для интеграционных тестов и Compose).

## Backend

```powershell
cd backend
.\gradlew.bat bootRun
```

По умолчанию БД: `jdbc:postgresql://localhost:5432/logistic`, пользователь `logistic`/`logistic`. Файлы документов: `%USERPROFILE%\.logistic-storage` (переменная `logistic.storage.root`).

Интеграционные тесты используют Testcontainers (нужен Docker):

```powershell
cd backend
.\gradlew.bat test
```

Пользователь по умолчанию после первого старта (профиль не `test`): `admin` / `admin123`.

## Frontend (Angular 19 + PrimeNG)

Работает поверх REST API (`/api/v1/...`). В режиме разработки `environment.apiBase` пустой: браузер ходит на тот же origin (`http://localhost:4200`), а dev-server по [`proxy.conf.js`](frontend/logistic/proxy.conf.js) проксирует `/api` и `/v3` на backend.

- **Локальный backend** (`.\gradlew.bat bootRun`, порт по умолчанию **8080**): `npm start`.
- **Backend в Docker Compose** (на хосте API на порту **7272**, см. `docker-compose.yml`): `npm run start:docker-api`  
  или задайте `LOGISTIC_API_PROXY_TARGET` (например `http://127.0.0.1:7272`), если порт другой.

```powershell
cd frontend\logistic
npm ci
npm start
# при API из Docker:
npm run start:docker-api
```

Откройте в браузере URL из вывода CLI (обычно `http://localhost:4200`), затем:

1. Страница **«Вход»** или **«Регистрация»** — самостоятельная регистрация создаёт пользователя с ролью `EMPLOYEE` и сразу выдаёт JWT; либо войдите под существующей учётной записью (`EMPLOYEE`, `MANAGER`, `ADMIN`; после первого старта backend: `admin` / `admin123`).
2. **Рейсы** — список, фильтр по статусу, «Создать» ведёт на `POST /trips` и открывает карточку рейса.
3. **Справочники** — контрагенты, водители, ТС, **места погрузки/разгрузки** (CRUD в диалогах).
4. **Карточка рейса** — шаги PrimeNG Stepper; рейс создаётся в `IN_PROGRESS`, правки доступны и до, и после завершения; **«Завершить рейс»** генерирует документы (`COMPLETED`), скачивание PDF/DOCX только для завершённых; **«Удалить»** физически удаляет рейс; вкладка **«Аудит»**.
5. Роль **ADMIN** — пункт «Пользователи», кнопка «+ Пользователь» (создание через API).

Юнит-тесты (Vitest через `@analogjs/vitest-angular`, без браузера):

```powershell
cd frontend\logistic
npm test
```

Покрытие (HTML-отчёт в `frontend/logistic/coverage/`, папка в `.gitignore`):

```powershell
cd frontend\logistic
npm run test:coverage
```

Сборка production:

```powershell
npm run build
```

Статические файлы — `frontend/logistic/dist/logistic`. При раздаче UI и API с одного хоста `environment.production.ts` использует `apiBase: ''`; для отдельного домена API задайте базовый URL в окружении и пересоберите приложение.

## Docker Compose

Сборка jar backend в Docker идёт через образ `gradle` (не требуется скачивание Gradle-дистрибутива wrapper’ом внутри контейнера); в `backend/settings.gradle` задан `pluginManagement` (Maven Central + Plugin Portal), чтобы плагины Spring Boot находились надёжнее. Для ускорения локальной разработки можно предварительно собрать jar (`.\gradlew.bat bootJar`). Нужен доступ к Docker Hub для базовых образов (`gradle`, `eclipse-temurin`, `node`, `nginx`, `postgres`) и к Maven Central при сборке backend.

Backend стартует только после готовности Postgres (`healthcheck` + `depends_on`).

```powershell
docker compose build
docker compose up
```

- UI: http://localhost:4200  
- API (с хоста): http://localhost:7272  
- PostgreSQL (с хоста): `localhost:7727` (внутри сети Compose сервис `db` по-прежнему слушает `5432`)  

В Compose для JWT задан `JWT_SECRET`; для продакшена используйте надёжный секрет и внешнее хранилище конфигурации.

## API collection

Для ручного тестирования полного сценария (auth → справочники → рейс → генерация документов → архив):

- файл коллекции: `api/logistic-api.http`
- OpenAPI (ручная фиксированная спецификация): `api/openapi.yaml`
- OpenAPI (живой runtime-контракт через springdoc): локально — `http://localhost:8080/v3/api-docs`; в Docker Compose — `http://localhost:7272/v3/api-docs`
- запускать по шагам из IDE (HTTP Client / REST Client)
- коллекция использует дефолтного `admin/admin123` и создаёт demo-пользователей и данные.
