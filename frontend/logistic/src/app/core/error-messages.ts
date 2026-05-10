/**
 * Перевод типичных заголовков и текстов Problem Detail / ответов Spring в русские сообщения для UI.
 */

const TITLE_EN_TO_RU: Record<string, string> = {
  'Bad Request': 'Некорректный запрос',
  Unauthorized: 'Требуется авторизация',
  Forbidden: 'Доступ запрещён',
  'Not Found': 'Не найдено',
  Conflict: 'Конфликт данных',
  'Internal Server Error': 'Ошибка сервера',
  'Service Unavailable': 'Сервис недоступен',
};

const TITLE_BY_STATUS: Record<number, string> = {
  0: 'Нет соединения с сервером',
  400: 'Некорректный запрос',
  401: 'Требуется авторизация',
  403: 'Доступ запрещён',
  404: 'Не найдено',
  409: 'Конфликт данных',
  422: 'Ошибка проверки данных',
  500: 'Ошибка сервера',
  502: 'Ошибка шлюза',
  503: 'Сервис недоступен',
  504: 'Превышено время ожидания',
};

/** Точные совпадения detail/reason от backend и типичные тексты Spring Security / Handler */
const DETAIL_EXACT: Record<string, string> = {
  'Username already exists': 'Такой логин уже занят',
  'Invalid credentials': 'Неверный логин или пароль',
  Invalid: 'Неверный логин или пароль',
  'User not found after registration':
    'Не удалось завершить регистрацию (обратитесь к администратору)',
  'Incomplete trip data': 'Заполните все обязательные поля рейса',
  'Foreign catalog entry': 'Выбранная запись справочника недоступна',
  'Trip already completed': 'Рейс уже завершён',
  'Document generation failed': 'Не удалось сформировать документы',
  'snapshot failed': 'Не удалось сохранить состояние рейса',
  'Malformed JSON request': 'Некорректный JSON в запросе',
  'Unexpected server error': 'Внутренняя ошибка сервера',
  Forbidden: 'Недостаточно прав',
  Unauthorized: 'Требуется вход в систему',
  'Resource not found': 'Ресурс не найден',
  'Authentication required': 'Требуется авторизация',
};

const FIELD_EN_TO_RU: Record<string, string> = {
  username: 'Логин',
  password: 'Пароль',
  roles: 'Роли',
  shipperId: 'Отправитель',
  consigneeId: 'Получатель',
  driverId: 'Водитель',
  vehicleId: 'Транспорт',
  cargoDescription: 'Описание груза',
  cargoWeightKg: 'Вес груза',
  routeFrom: 'Маршрут (откуда)',
  routeTo: 'Маршрут (куда)',
  loadDate: 'Дата погрузки',
  unloadDate: 'Дата разгрузки',
  priceAmount: 'Сумма',
  currency: 'Валюта',
  name: 'Название',
  inn: 'ИНН',
  plateNumber: 'Госномер',
  fullName: 'ФИО',
  loadCapacityKg: 'Грузоподъёмность',
};

/** Фразы Bean Validation / Hibernate Validator (англ.) → русский */
const CONSTRAINT_PHRASES: Array<{ en: RegExp; ru: string }> = [
  { en: /^must not be blank$/i, ru: 'не может быть пустым' },
  { en: /^must not be null$/i, ru: 'обязательное поле' },
  { en: /^must not be empty$/i, ru: 'не может быть пустым' },
  {
    en: /^size must be between (\d+) and (\d+)$/i,
    ru: 'длина должна быть от $1 до $2 символов',
  },
  {
    en: /^length must be between (\d+) and (\d+)$/i,
    ru: 'длина должна быть от $1 до $2 символов',
  },
  { en: /^must be greater than or equal to (.+)$/i, ru: 'не меньше $1' },
  { en: /^must be less than or equal to (.+)$/i, ru: 'не больше $1' },
  {
    en: /^must be greater than (.+)$/i,
    ru: 'должно быть больше $1',
  },
  {
    en: /^Password must be at least (\d+) characters$/i,
    ru: 'пароль не короче $1 символов',
  },
];

function translateFieldToken(field: string): string {
  return FIELD_EN_TO_RU[field] ?? field;
}

function translateConstraintMessage(msg: string): string {
  const t = msg.trim();
  for (const { en, ru } of CONSTRAINT_PHRASES) {
    const m = t.match(en);
    if (m) {
      return ru.replace(/\$(\d)/g, (_, d) => m[parseInt(d, 10)] ?? '');
    }
  }
  return t;
}

/** Одна часть сообщения (до «; ») — поле: текст или целиком текст */
function translateDetailSegment(segment: string): string {
  const s = segment.trim();
  if (!s) {
    return s;
  }
  if (DETAIL_EXACT[s]) {
    return DETAIL_EXACT[s];
  }
  const colon = /^([\w.]+):\s*(.+)$/.exec(s);
  if (colon) {
    const field = colon[1];
    const rest = colon[2].trim();
    if (DETAIL_EXACT[rest]) {
      return `${translateFieldToken(field)}: ${DETAIL_EXACT[rest]}`;
    }
    return `${translateFieldToken(field)}: ${translateConstraintMessage(rest)}`;
  }
  if (s.startsWith('No static resource ')) {
    const path = s.slice('No static resource '.length);
    return `Ресурс не найден: ${path}`;
  }
  return translateConstraintMessage(s);
}

/** Переводит текст detail/reason от сервера (включая составные validation-сообщения). */
export function translateBackendDetail(detail: string | undefined | null): string {
  if (detail == null || detail === '') {
    return '';
  }
  const trimmed = detail.trim();
  if (DETAIL_EXACT[trimmed]) {
    return DETAIL_EXACT[trimmed];
  }
  return trimmed
    .split(';')
    .map((p) => translateDetailSegment(p))
    .filter(Boolean)
    .join('; ');
}

export function translateProblemTitle(
  title: string | undefined | null,
  httpStatus: number,
): string {
  const t = title?.trim();
  if (t) {
    const ru = TITLE_EN_TO_RU[t];
    if (ru) {
      return ru;
    }
    if (/[а-яА-ЯёЁ]/.test(t)) {
      return t;
    }
  }
  return TITLE_BY_STATUS[httpStatus] ?? `Ошибка ${httpStatus}`;
}

/** Для тостов: заголовок и подпись на русском */
export function localizeProblemToast(
  rawTitle: string | undefined | null,
  rawDetail: string | undefined | null,
  httpStatus: number,
): { summary: string; detail?: string } {
  const summary = translateProblemTitle(rawTitle, httpStatus);
  const d = translateBackendDetail(rawDetail ?? '');
  return { summary, detail: d || undefined };
}

/** Сообщение Angular HttpClient при сбое запроса → понятный русский текст */
export function localizeHttpClientMessage(
  message: string,
  httpStatus: number,
): string {
  if (/Http failure/i.test(message)) {
    const phrase = TITLE_BY_STATUS[httpStatus];
    const codeLabel =
      httpStatus === 0 ? 'сеть недоступна' : `код ${httpStatus}`;
    if (phrase) {
      return `${phrase} Проверьте подключение и попробуйте снова (${codeLabel}).`;
    }
    return `Не удалось связаться с сервером. Проверьте сеть и повторите попытку (${codeLabel}).`;
  }
  if (/^[A-Za-z]/.test(message) && !/[а-яА-ЯёЁ]/.test(message)) {
    const tr = translateBackendDetail(message);
    if (tr !== message.trim()) {
      return tr;
    }
  }
  return message;
}

/** Заголовок и текст тоста, когда тело ответа не Problem JSON */
export function summaryAndDetailForPlainHttpError(
  status: number,
  message: string,
): { summary: string; detail: string } {
  const summary =
    TITLE_BY_STATUS[status] ??
    (status === 0 ? 'Нет соединения с сервером' : `Ошибка ${status}`);
  const detail = localizeHttpClientMessage(message, status);
  return { summary, detail };
}
