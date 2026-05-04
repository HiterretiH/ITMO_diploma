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

## Frontend

```powershell
cd frontend\logistic
npm ci
npm start
```

`ng serve` проксирует `/api` и `/v3` на `http://localhost:8080` (`proxy.conf.json`). Откройте приложение по адресу, который выводит Angular CLI (обычно `http://localhost:4200`).

Сборка:

```powershell
npm run build
```

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
