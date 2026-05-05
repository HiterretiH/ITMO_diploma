# Logistic document MVP

Информационная система для формирования комплекта перевозочных документов (Spring Boot + Angular + PostgreSQL).

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

Работает поверх REST API (`/api/v1/...`). В режиме разработки запросы с фронта идут на тот же origin (`localhost:4200`), а `proxy.conf.json` перенаправляет `/api` и `/v3` на backend `http://localhost:8080`.

```powershell
cd frontend\logistic
npm ci
npm start
```

Откройте в браузере URL из вывода CLI (обычно `http://localhost:4200`), затем:

1. Страница **«Вход»** или **«Регистрация»** — самостоятельная регистрация создаёт пользователя с ролью `EMPLOYEE` и сразу выдаёт JWT; либо войдите под существующей учётной записью (`EMPLOYEE`, `MANAGER`, `ADMIN`; после первого старта backend: `admin` / `admin123`).
2. **Рейсы** — список, фильтр по статусу, «Создать» ведёт на `POST /trips` и открывает карточку рейса.
3. **Справочники** — контрагенты, водители, ТС (CRUD в диалогах).
4. **Карточка рейса** — шаги PrimeNG Stepper, сохранение только в статусе `DRAFT`; для ролей MANAGER/ADMIN — утверждение и архив; при `APPROVED`/`ARCHIVED` — скачивание PDF/DOCX; вкладка **«Аудит»**.
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

Сборка jar backend выполняется образом; для ускорения локальной разработки можно предварительно собрать jar (`.\gradlew.bat bootJar`).

```powershell
docker compose build
docker compose up
```

- UI: http://localhost:4200  
- API: http://localhost:8080  
- PostgreSQL: localhost:5432  

В Compose для JWT задан `JWT_SECRET`; для продакшена используйте надёжный секрет и внешнее хранилище конфигурации.

## API collection

Для ручного тестирования полного сценария (auth → справочники → рейс → генерация документов → архив):

- файл коллекции: `api/logistic-api.http`
- OpenAPI (ручная фиксированная спецификация): `api/openapi.yaml`
- OpenAPI (живой runtime-контракт через springdoc): `http://localhost:8080/v3/api-docs`
- запускать по шагам из IDE (HTTP Client / REST Client)
- коллекция использует дефолтного `admin/admin123` и создаёт demo-пользователей и данные.
