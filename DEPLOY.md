# Деплой на production

Инструкция по развёртыванию и сопровождению production-стека на VPS.

| Режим | Compose-файл | Документация |
|-------|--------------|--------------|
| Production (сервер) | [`docker-compose.yml`](docker-compose.yml) | этот файл |
| Локальная разработка | [`docker-compose.dev.yml`](docker-compose.dev.yml) | [README.md](README.md) |

## Содержание

1. [Архитектура](#архитектура)
2. [Структура каталога deploy](#структура-каталога-deploy)
3. [Требования](#требования)
4. [Настройка сервера](#настройка-сервера)
5. [Первичное развёртывание](#первичное-развёртывание)
6. [Переменные окружения](#переменные-окружения)
7. [Обновление версии](#обновление-версии)
8. [Резервное копирование](#резервное-копирование)
9. [Диагностика](#диагностика)
10. [Справочник команд](#справочник-команд)
11. [Чеклист первого деплоя](#чеклист-первого-деплоя)

---

## Архитектура

Production использует один файл [`docker-compose.yml`](docker-compose.yml). На VPS работают два уровня прокси:

1. **nginx на хосте** принимает HTTPS (порты 80/443) и перенаправляет весь трафик на `http://127.0.0.1:8080`.
2. **Контейнер `frontend`** слушает этот порт и либо отдаёт статику Angular (`/`), либо проксирует запросы `/api/` и `/v3/` на контейнер `backend` внутри сети Docker Compose.

Контейнеры `backend` (Spring Boot, профиль `prod`) и `db` (PostgreSQL 16) **не** проброшены на хост — к ним нельзя подключиться снаружи VPS. Сгенерированные документы backend пишет в том `docs`; данные БД хранятся в томе `pgdata`. Миграции схемы выполняет Flyway при каждом старте backend.

**Загрузка интерфейса:** браузер запрашивает страницу по HTTPS → host-nginx → frontend → в ответ приходят `index.html`, JS и CSS.

**Работа с API:** браузер отправляет запрос на `/api/v1/...` с JWT в заголовке → host-nginx → frontend → backend → PostgreSQL → ответ в формате JSON возвращается по той же цепочке.

### Сервисы и порты

| Сервис | Доступ с хоста | Назначение |
|--------|----------------|------------|
| `frontend` | `127.0.0.1:8080` → `80` | UI + reverse proxy к API внутри сети Compose |
| `backend` | только внутри Docker | Spring Boot, профиль `prod` (Swagger отключён) |
| `db` | только внутри Docker | PostgreSQL 16, том `pgdata` |

**Важно:** в host-nginx **не** настраивайте отдельный `location /api/` на порт `7272` (это порт dev-стека). Backend в production недоступен с хоста; единственный upstream — `http://127.0.0.1:8080`. Пример: [`deploy/nginx/site.conf.example`](deploy/nginx/site.conf.example).

Тома Docker:

- `pgdata` — данные PostgreSQL;
- `docs` — сгенерированные файлы документов (`LOGISTIC_STORAGE_ROOT=/data/docs` в контейнере backend).

---

## Структура каталога deploy

```
deploy/
├── nginx/
│   └── site.conf.example    # пример host-nginx (заменить example.com)
└── scripts/
    ├── create-archive.sh    # архив для копирования на сервер
    └── unarchive.sh         # распаковка (если репозиторий уже на сервере)
```

Скрипты запускаются в bash (Linux, macOS, WSL). На Windows без WSL используйте `git clone` или создайте архив в WSL.

---

## Требования

### Аппаратные и сетевые

- VPS: Ubuntu 22.04 или 24.04 (или аналог с `systemd` и `apt`)
- 2+ ГБ RAM, 20+ ГБ свободного диска
- Публичный IPv4
- DNS: A-запись домена на IP сервера (при необходимости отдельная запись для `www`)

Проверка DNS (с любой машины):

```bash
dig +short <ваш-домен>
```

Ответ должен совпадать с IP VPS.

### Рабочий каталог

Далее используется `~/logistic` — каталог с `docker-compose.yml` и `.env`. Другой путь допустим, замените его во всех командах.

---

## Настройка сервера

Выполняется **один раз** на чистом VPS до первого деплоя приложения. Команды ниже — под Ubuntu; для других дистрибутивов см. официальные инструкции Docker и nginx.

Подключитесь по SSH:

```bash
ssh user@<ip-или-домен-сервера>
```

### 1. Обновление системы и базовые пакеты

```bash
sudo apt update
sudo apt upgrade -y
sudo apt install -y ca-certificates curl git ufw openssl
```

`git` нужен при деплое через `git clone`. `openssl` — для генерации секретов в `.env`.

### 2. Docker Engine и Compose plugin

Установка из официального репозитория Docker ([документация](https://docs.docker.com/engine/install/ubuntu/)):

```bash
sudo apt remove -y docker.io docker-doc docker-compose docker-compose-v2 podman-docker containerd runc 2>/dev/null || true

sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

Проверка:

```bash
docker --version
docker compose version
sudo systemctl enable docker
sudo systemctl start docker
```

Запуск `docker compose` без `sudo` (перелогиньтесь после команды или выполните `newgrp docker`):

```bash
sudo usermod -aG docker "$USER"
```

Тест:

```bash
docker run --rm hello-world
```

### 3. nginx

```bash
sudo apt install -y nginx
sudo systemctl enable nginx
sudo systemctl start nginx
nginx -v
```

На этом этапе nginx отвечает на порту 80 с дефолтной страницей — это нормально. Конфигурация под приложение — в шаге 4 раздела [первичного развёртывания](#первичное-развёртывание).

### 4. Certbot (Let's Encrypt)

```bash
sudo apt install -y certbot python3-certbot-nginx
certbot --version
```

Сертификат выпускается позже, когда приложение уже слушает `127.0.0.1:8080` и в DNS указан домен.

### 5. Firewall (UFW)

**Сначала** разрешите SSH, иначе можно потерять доступ к серверу:

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status
```

Порты PostgreSQL (`5432`), backend и `8080` наружу **не открывать** — frontend в production привязан к `127.0.0.1:8080`.

### 6. Автозапуск и обновления (рекомендуется)

Docker и nginx уже добавлены в автозагрузку (`systemctl enable`). Для автоматического обновления пакетов безопасности Ubuntu:

```bash
sudo apt install -y unattended-upgrades
sudo dpkg-reconfigure -plow unattended-upgrades
```

Продление TLS-сертификатов certbot обычно настраивает systemd-timer или cron при первом `certbot --nginx`. Проверка:

```bash
sudo certbot renew --dry-run
```

### Итог настройки

| Компонент | Команда проверки |
|-----------|------------------|
| Docker | `docker --version` |
| Compose | `docker compose version` |
| nginx | `systemctl is-active nginx` |
| Certbot | `certbot --version` |
| UFW | `sudo ufw status` |

После этого переходите к [первичному развёртыванию](#первичное-развёртывание).

---

## Первичное развёртывание

Порядок шагов: код → `.env` → контейнеры → nginx/TLS → проверка → отключение bootstrap. Предполагается, что [настройка сервера](#настройка-сервера) уже выполнена.

### 1. Доставка кода на сервер

**Вариант A — git (предпочтительно):**

```bash
ssh user@your-server
git clone <url-репозитория> ~/logistic
cd ~/logistic
```

**Вариант B — архив (без git на сервере):**

На машине с исходниками, из **корня** репозитория:

```bash
chmod +x deploy/scripts/*.sh   # один раз, если нужно
./deploy/scripts/create-archive.sh
scp deploy-archive.tar.gz user@your-server:~/
```

На сервере (при первом деплое скриптов ещё нет — распаковка через `tar`):

```bash
mkdir -p ~/logistic
tar -xzf ~/deploy-archive.tar.gz -C ~/logistic
cd ~/logistic
```

При последующих обновлениях через архив можно использовать [`deploy/scripts/unarchive.sh`](deploy/scripts/unarchive.sh):

```bash
./deploy/scripts/unarchive.sh ~/logistic ~/deploy-archive.tar.gz
```

Архив **не включает** `.env`, `node_modules`, артефакты сборки и каталог `diploma/`.

### 2. Файл `.env`

```bash
cd ~/logistic
cp .env.example .env
chmod 600 .env
nano .env
```

Сгенерировать секреты:

```bash
openssl rand -base64 48    # POSTGRES_PASSWORD
openssl rand -base64 48    # JWT_SECRET (минимум 32 символа)
openssl rand -base64 24    # BOOTSTRAP_ADMIN_PASSWORD
```

Минимально заполненный `.env` для первого запуска:

```env
POSTGRES_DB=logistic
POSTGRES_USER=logistic
POSTGRES_PASSWORD=<случайная_строка>

JWT_SECRET=<случайная_строка_32+_символов>

CORS_ALLOWED_ORIGIN_PATTERNS=https://<ваш-домен>,https://www.<ваш-домен>

BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_USERNAME=admin
BOOTSTRAP_ADMIN_PASSWORD=<сильный_пароль>

TYPEDATA_ENABLED=false
TYPEDATA_TOKEN=
```

`CORS_ALLOWED_ORIGIN_PATTERNS` должен совпадать с URL, по которому пользователи открывают сайт (схема `https://`, без завершающего `/`).

Полный список переменных — в разделе [переменные окружения](#переменные-окружения).

### 3. Сборка и запуск контейнеров

```bash
cd ~/logistic
docker compose build
docker compose up -d
docker compose ps
```

Ожидаемый вывод `docker compose ps`:

| Сервис | Состояние | PORTS |
|--------|-----------|-------|
| `db` | running (healthy) | `5432/tcp` |
| `backend` | running | `8080/tcp` |
| `frontend` | running | `127.0.0.1:8080->80/tcp` |

Проверка **до** настройки публичного nginx (с хоста доступен только frontend):

```bash
curl -I http://127.0.0.1:8080
docker compose logs --tail=50 backend
```

В логах backend не должно быть ошибок Flyway и подключения к БД.

### 4. nginx и TLS на хосте

1. Скопируйте пример конфигурации.
2. Замените `example.com` на свой домен в `server_name` и путях к сертификатам (или отредактируйте файл до копирования).
3. Подключите сайт и проверьте синтаксис.

```bash
cd ~/logistic
sudo mkdir -p /var/www/certbot
sudo cp deploy/nginx/site.conf.example /etc/nginx/sites-available/logistic
sudo ln -sf /etc/nginx/sites-available/logistic /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
```

**Если TLS-сертификатов ещё нет** и `nginx -t` падает на блоке `listen 443 ssl` — временно закомментируйте HTTPS-сервер в конфиге и в HTTP-блоке вместо `return 301` поставьте прокси на `127.0.0.1:8080` (подробнее в конце [`site.conf.example`](deploy/nginx/site.conf.example)). После этого:

```bash
sudo nginx -t && sudo systemctl reload nginx
sudo certbot --nginx -d <ваш-домен> -d www.<ваш-домен>
sudo nginx -t && sudo systemctl reload nginx
```

Certbot обычно сам правит конфиг nginx; при необходимости сверьте его с примером из репозитория.

### 5. Проверка после деплоя

1. Откройте сайт по HTTPS — страница входа.
2. Войдите под `BOOTSTRAP_ADMIN_USERNAME` / `BOOTSTRAP_ADMIN_PASSWORD`.
3. Создайте заказчика и заказ (рейс).
4. Зарегистрируйте тестового пользователя — он должен попасть на страницу «Ожидание подтверждения».
5. Под admin откройте **Администрирование → Заявки на регистрацию** (`/admin/registration-requests`), одобрите заявку.
6. Убедитесь, что скачивание документов по завершённому заказу работает.

### 6. Отключить bootstrap-admin

После первого успешного входа администратора отредактируйте `.env`:

```env
BOOTSTRAP_ADMIN_ENABLED=false
```

```bash
docker compose up -d
```

Пароль admin далее меняется через UI (`/account`), не через повторный bootstrap.

---

## Переменные окружения

Шаблон: [`.env.example`](.env.example) в корне репозитория. Файл `.env` **не коммитьте**.

| Переменная | Обязательна | Передаётся в backend | Описание |
|------------|-------------|----------------------|----------|
| `POSTGRES_DB` | нет (`logistic`) | через JDBC URL | Имя базы |
| `POSTGRES_USER` | нет (`logistic`) | да | Пользователь PostgreSQL |
| `POSTGRES_PASSWORD` | **да** | да | Пароль БД |
| `JWT_SECRET` | **да** | да | Секрет JWT, ≥ 32 символов |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | **да** | да | Origin фронтенда через запятую |
| `BOOTSTRAP_ADMIN_ENABLED` | нет | да | `true` только при первом деплое |
| `BOOTSTRAP_ADMIN_USERNAME` | при bootstrap | да | Логин начального admin |
| `BOOTSTRAP_ADMIN_PASSWORD` | при bootstrap | да | Пароль начального admin |
| `TYPEDATA_ENABLED` | нет (`false`) | да | Подсказки адресов ([TypeData](https://typedata.net)) |
| `TYPEDATA_TOKEN` | при включённом TypeData | да | API-токен |
| `HIKARI_MAX_POOL_SIZE` | нет (`20`) | да | Размер пула соединений HikariCP |

При `BOOTSTRAP_ADMIN_ENABLED=true` пароль `BOOTSTRAP_ADMIN_PASSWORD` должен быть задан — иначе backend не создаст учётную запись.

TypeData: запросы к внешнему API ограничены таймаутами (connect 3 с, read 5 с); при сбое внешнего API подсказки из истории адресов всё равно возвращаются.

---

## Обновление версии

### Через git

```bash
cd ~/logistic
git pull
docker compose build
docker compose up -d
docker compose ps
docker compose logs --tail=30 backend
```

### Через архив

На рабочей машине:

```bash
./deploy/scripts/create-archive.sh
scp deploy-archive.tar.gz user@your-server:~/
```

На сервере:

```bash
cd ~/logistic
tar -xzf ~/deploy-archive.tar.gz
docker compose build
docker compose up -d
```

Flyway применит новые миграции при старте backend. Тома `pgdata` и `docs` сохраняются.

**Не перезаписывайте** `.env` при обновлении — в архиве его нет.

### Откат

Если после обновления что-то сломалось:

```bash
git checkout <предыдущий-тег-или-коммит>   # при деплое через git
docker compose build
docker compose up -d
```

Миграции Flyway **не откатываются** автоматически — для отката схемы нужен восстановленный дамп БД.

### Остановка стека

```bash
docker compose down          # остановить контейнеры, тома сохраняются
docker compose down -v     # ОПАСНО: удалит тома pgdata и docs
```

---

## Резервное копирование

### Дамп PostgreSQL

```bash
cd ~/logistic
docker compose exec -T db pg_dump -U logistic logistic > backup-$(date +%Y%m%d-%H%M).sql
```

Имя пользователя и БД замените, если в `.env` заданы другие `POSTGRES_USER` / `POSTGRES_DB`.

### Восстановление

Остановите запись в приложение, затем (перезапишет данные в БД):

```bash
cd ~/logistic
docker compose exec -T db psql -U logistic -d logistic < backup-YYYYMMDD-HHMM.sql
```

### Файлы документов

Том `docs` хранит сгенерированные PDF/DOCX. Список томов проекта:

```bash
docker volume ls
```

Имя тома зависит от имени каталога проекта (например, `logistic_docs`). Для полного бэкапа сохраняйте SQL-дамп и volume `docs`.

---

## Диагностика

### 502 Bad Gateway

```bash
ss -tlnp | grep 8080
curl -I http://127.0.0.1:8080
docker compose ps
docker compose logs --tail=100 frontend
docker compose logs --tail=100 backend
```

Частые причины:

- контейнер `frontend` не запущен;
- в `docker-compose.yml` у `frontend` нет `127.0.0.1:8080:80`;
- host-nginx проксирует `/api/` на порт `7272` вместо единого upstream на `8080`;
- backend ещё не поднялся — подождите или смотрите `docker compose logs backend`.

### Backend не стартует

```bash
docker compose logs backend
```

Проверьте в `.env`: `POSTGRES_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGIN_PATTERNS`. Убедитесь, что `db` в состоянии `healthy`.

### Ошибки CORS в браузере

`CORS_ALLOWED_ORIGIN_PATTERNS` должен содержать точный origin сайта (`https://...`), совпадающий с адресной строкой браузера.

### Истечение JWT

Токен действует 24 ч. При истечении нужен повторный вход. При временной недоступности БД API может вернуть `503` — сессия при этом не сбрасывается.

### Медленные подсказки адресов / пул HikariCP

В логах: `HikariPool - Connection is not available`. Часто при включённом TypeData и высокой нагрузке. Действия:

```bash
docker compose restart backend
```

При необходимости увеличьте `HIKARI_MAX_POOL_SIZE` в `.env` и выполните `docker compose up -d`.

### Просмотр логов

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f db
```

---

## Справочник команд

Все команды `docker compose` — из каталога `~/logistic`.

| Действие | Команда |
|----------|---------|
| Запуск | `docker compose up -d` |
| Остановка | `docker compose down` |
| Пересборка | `docker compose build` |
| Перезапуск backend | `docker compose restart backend` |
| Создать архив | `./deploy/scripts/create-archive.sh` |
| Проверка nginx | `sudo nginx -t` |
| Перезагрузка nginx | `sudo systemctl reload nginx` |
| Продление TLS | `sudo certbot renew` |

Локальная разработка (не production):

```bash
docker compose -f docker-compose.dev.yml up
```

Порты dev: UI `4200`, API `7272`, PostgreSQL `7727`. Подробности — [README.md](README.md).

---

## Чеклист первого деплоя

- [ ] VPS с Ubuntu, DNS A-запись указывает на сервер
- [ ] Выполнена [настройка сервера](#настройка-сервера): Docker, Compose, nginx, certbot, UFW
- [ ] `docker run hello-world` и `docker compose version` работают без ошибок
- [ ] Код в `~/logistic`, из `.env.example` создан `.env` с сильными паролями
- [ ] `CORS_ALLOWED_ORIGIN_PATTERNS` указывает на HTTPS-URL сайта
- [ ] `docker compose up -d`, `curl -I http://127.0.0.1:8080` возвращает ответ
- [ ] В nginx-конфиге заменён домен, certbot выдан, сайт открывается по HTTPS
- [ ] UFW: открыты только SSH, 80, 443
- [ ] Вход admin, проверена модерация регистрации
- [ ] В `.env` установлено `BOOTSTRAP_ADMIN_ENABLED=false`, выполнен `docker compose up -d`
- [ ] Настроен периодический бэкап БД (и при необходимости тома `docs`)
