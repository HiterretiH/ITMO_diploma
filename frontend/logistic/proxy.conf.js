/**
 * Dev server: запросы на тот же origin (/api, /v3) пересылаются на backend.
 *
 * По умолчанию: http://localhost:8080 (локальный ./gradlew bootRun).
 * Backend из docker-compose на хосте слушает порт 7272 — см. npm run start:docker-api
 * или переменную окружения LOGISTIC_API_PROXY_TARGET.
 */
const target =
  (process.env.LOGISTIC_API_PROXY_TARGET || '').trim() ||
  'http://localhost:8080';

module.exports = {
  '/api': {
    target,
    secure: false,
    changeOrigin: true,
  },
  '/v3': {
    target,
    secure: false,
    changeOrigin: true,
  },
};
