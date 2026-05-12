import { HttpHeaders, HttpRequest, HttpResponse } from '@angular/common/http';
import type { JwtResponse, LoginRequest, RegisterRequest } from '../auth.models';
import type {
  CustomerRequest,
  CustomerResponse,
  DriverRequest,
  DriverResponse,
  PerformerRequest,
  PerformerResponse,
  VehicleRequest,
  VehicleResponse,
} from '../catalog.models';
import type {
  OrderCreateRequest,
  OrderDocumentDescriptor,
  OrderResponse,
  OrderUpdateRequest,
  TripFormDraftResponse,
} from '../order.models';
import type { UserCreateRequest, UserResponse } from '../user.models';

function base64UrlEncodeJson(obj: unknown): string {
  const json = JSON.stringify(obj);
  const bytes = new TextEncoder().encode(json);
  let binary = '';
  for (const b of bytes) {
    binary += String.fromCharCode(b);
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
}

/** Three-part JWT with decodable payload (signature not verified by the app). */
export function mockJwt(sub: string, roles: string[]): string {
  const header = base64UrlEncodeJson({ alg: 'HS256', typ: 'JWT' });
  const payload = base64UrlEncodeJson({ sub, roles });
  return `${header}.${payload}.mock-signature`;
}

function performerRow(
  id: number,
  shortName: string,
  fullName: string,
  phone: string,
  bankName: string,
  inn: string,
  bik: string,
  kpp: string,
  paymentAccount: string,
  corrAccount: string,
  requisites: string,
): PerformerResponse {
  return {
    id,
    shortName,
    fullName,
    phone,
    bankName,
    inn,
    bik,
    kpp,
    paymentAccount,
    corrAccount,
    requisites,
  };
}

let customers: CustomerResponse[] = [
  {
    id: 1,
    shortName: 'Северторг',
    fullName: 'ООО «Северная розничная компания»',
    phone: '+7 (495) 100-01-01',
    requisites: 'ИНН 7701001001, КПП 770101001, ОГРН 1027701001001, р/с 40702810900000001001 в ПАО «Мокбанк», к/с 30101810000000000555, БИК 044525555',
  },
  {
    id: 2,
    shortName: 'ВостокОпт',
    fullName: 'ООО «Восточный оптовый склад»',
    phone: '+7 (812) 200-02-02',
    requisites: 'ИНН 7802002002, КПП 780201001, ОГРН 1027802002002, р/с 40702810900000002002 в ПАО «Мокбанк», к/с 30101810000000000555, БИК 044030555',
  },
  {
    id: 3,
    shortName: 'ЮгЛогистик',
    fullName: 'ООО «Южный логистический хаб»',
    phone: '+7 (863) 300-03-03',
    requisites: 'ИНН 6103003003, КПП 610301001, ОГРН 1026103003003, р/с 40702810900000003003 в ПАО «Мокбанк», к/с 30101810000000000555, БИК 046015555',
  },
  {
    id: 4,
    shortName: 'ЗападПродукт',
    fullName: 'ООО «Западные продукты питания»',
    phone: '+7 (383) 400-04-04',
    requisites: 'ИНН 5404004004, КПП 540401001, ОГРН 1025404004004, р/с 40702810900000004004 в ПАО «Мокбанк», к/с 30101810000000000555, БИК 045004555',
  },
  {
    id: 5,
    shortName: 'МетроСтрой',
    fullName: 'ООО «Метрополия строительных материалов»',
    phone: '+7 (499) 500-05-05',
    requisites: 'ИНН 7705005005, КПП 770501001, ОГРН 1027705005005, р/с 40702810900000005005 в ПАО «Мокбанк», к/с 30101810000000000555, БИК 044525555',
  },
  {
    id: 6,
    shortName: 'АгроТрейд',
    fullName: 'ООО «Агроторговая сеть»',
    phone: '+7 (844) 600-06-06',
    requisites: 'ИНН 6306006006, КПП 630601001, ОГРН 1026306006006, р/с 40702810900000006006 в ПАО «Мокбанк», к/с 30101810000000000555, БИК 043601555',
  },
];

let performers: PerformerResponse[] = [
  performerRow(
    2,
    'АльфаТранс',
    'ООО «Альфа транспортные перевозки»',
    '+7 (495) 700-70-01',
    'ПАО «Мокбанк»',
    '7707707707',
    '044525555',
    '770701001',
    '40702810900000007007',
    '30101810000000000555',
    'ОКПО 12345678, ОКВЭД 49.41, юр. адрес: 101000, г. Москва, ул. Моковая, д. 1, офис 101',
  ),
  performerRow(
    3,
    'БетаКарго',
    'ООО «Бета карго сервис»',
    '+7 (812) 700-70-02',
    'ПАО «Мокбанк»',
    '7807807808',
    '044030555',
    '780701001',
    '40702810900000008008',
    '30101810000000000555',
    'ОКПО 23456789, ОКВЭД 49.41, юр. адрес: 190000, г. Санкт-Петербург, наб. Моковая, д. 2',
  ),
  performerRow(
    4,
    'ГаммаЛайн',
    'ООО «Гамма линии доставки»',
    '+7 (383) 700-70-03',
    'ПАО «Мокбанк»',
    '5405405405',
    '045004555',
    '540501001',
    '40702810900000009009',
    '30101810000000000555',
    'ОКПО 34567890, ОКВЭД 49.41, юр. адрес: 630000, г. Новосибирск, пр-т Моковый, д. 3',
  ),
];

let drivers: DriverResponse[] = [
  { id: 1, performerId: 2, fullName: 'Иванов Иван Иванович', phone: '+7 (903) 111-11-11' },
  { id: 2, performerId: 2, fullName: 'Петров Алексей Сергеевич', phone: '+7 (903) 222-22-22' },
  { id: 3, performerId: 3, fullName: 'Волкова Мария Павловна', phone: '+7 (921) 333-33-33' },
  { id: 4, performerId: 3, fullName: 'Козлов Дмитрий Николаевич', phone: '+7 (921) 444-44-44' },
  { id: 5, performerId: 4, fullName: 'Смирнова Елена Викторовна', phone: '+7 (913) 555-55-55' },
];

let vehicles: VehicleResponse[] = [
  {
    id: 1,
    performerId: 2,
    brandModel: 'Volvo FH16, седельный тягач',
    plateNumber: 'A001BC77',
    type: 'Седельный тягач',
  },
  {
    id: 2,
    performerId: 2,
    brandModel: 'Scania R450, седельный тягач',
    plateNumber: 'B002CD77',
    type: 'Седельный тягач',
  },
  {
    id: 3,
    performerId: 3,
    brandModel: 'MAN TGX 18.500',
    plateNumber: 'C003EF99',
    type: 'Седельный тягач',
  },
  {
    id: 4,
    performerId: 3,
    brandModel: 'Mercedes-Benz Actros 1845',
    plateNumber: 'D004GH99',
    type: 'Бортовой с тентом',
  },
  {
    id: 5,
    performerId: 4,
    brandModel: 'DAF XF 106',
    plateNumber: 'E005JK50',
    type: 'Седельный тягач',
  },
];

let nextCustomerId = 10;
let nextPerformerId = 20;
let nextDriverId = 30;
function jwtSubForMockProfile(req: HttpRequest<unknown>): string {
  const auth = req.headers.get('Authorization');
  if (auth?.startsWith('Bearer ')) {
    return decodeJwtSub(auth.slice('Bearer '.length).trim());
  }
  if (typeof sessionStorage !== 'undefined') {
    const stored = sessionStorage.getItem('access_token');
    if (stored) {
      return decodeJwtSub(stored);
    }
  }
  return 'mockuser';
}

function decodeJwtSub(token: string): string {
  const part = token.split('.')[1];
  if (!part) {
    return 'mockuser';
  }
  try {
    let base64 = part.replace(/-/g, '+').replace(/_/g, '/');
    while (base64.length % 4) {
      base64 += '=';
    }
    const json = atob(base64);
    const payload = JSON.parse(json) as { sub?: unknown };
    return typeof payload.sub === 'string' ? payload.sub : 'mockuser';
  } catch {
    return 'mockuser';
  }
}

let nextVehicleId = 40;
let nextAdminUserId = 900;

function seedOrder(partial: Partial<OrderResponse> & Pick<OrderResponse, 'id'>): OrderResponse {
  return {
    customerId: 1,
    performerId: 2,
    vehicleId: null,
    driverId: null,
    orderNumber: 100 + partial.id,
    orderDate: '2024-06-01',
    loadingPlace: 'Склад погрузки (адрес уточняется у диспетчера)',
    loadingContact: '+7 (495) 000-00-01, диспетчер круглосуточно',
    unloadingPlace: 'Пункт выгрузки (адрес уточняется у диспетчера)',
    unloadingContact: '+7 (495) 000-00-02, приемка по предварительному звонку',
    tripCount: 1,
    pricePerTrip: 32000,
    totalPrice: 32000,
    templateVersion: 1,
    completed: false,
    customerShortName: 'Северторг',
    performerShortName: 'АльфаТранс',
    ...partial,
  };
}

let mockOrders: OrderResponse[] = [
  seedOrder({
    id: 1,
    customerId: 1,
    performerId: 2,
    vehicleId: null,
    driverId: null,
    orderDate: '2024-06-22',
    loadingPlace: 'Москва, Складской проезд, стр. 5, ворота 3',
    loadingContact: '+7 (903) 010-01-01, старший смены Петров С.И.',
    unloadingPlace: 'Московская область, Люберцы, ул. Заводская, 12',
    unloadingContact: '+7 (903) 010-01-02, приемка с 09:00 до 18:00',
    pricePerTrip: 28500,
    totalPrice: 28500,
    tripCount: 1,
    completed: false,
    customerShortName: 'Северторг',
    performerShortName: 'АльфаТранс',
  }),
  seedOrder({
    id: 2,
    customerId: 1,
    performerId: 2,
    vehicleId: 1,
    driverId: 1,
    orderDate: '2024-06-02',
    loadingPlace: 'Склад Север, стеллажный комплекс, зона погрузки B3',
    loadingContact: '+7 (495) 100-11-11, диспетчер склада',
    unloadingPlace: 'Торговый парк "Восток", въезд с Каширского шоссе, КПП #2',
    unloadingContact: '+7 (495) 100-22-22, ответственный за разгрузку Иванова О.П.',
    totalPrice: 45000,
    pricePerTrip: 22500,
    tripCount: 2,
    completed: true,
    customerShortName: 'Северторг',
    performerShortName: 'АльфаТранс',
  }),
  seedOrder({
    id: 3,
    customerId: 2,
    performerId: 3,
    vehicleId: 3,
    driverId: 3,
    orderDate: '2024-06-10',
    loadingPlace: 'Портовый терминал 12, контейнерная площадка',
    loadingContact: '+7 (812) 200-33-33, служба допуска транспорта',
    unloadingPlace: 'Распределительный центр "Юг", рампа B, док 7',
    unloadingContact: '+7 (863) 300-44-44, складской координатор',
    totalPrice: 126000,
    pricePerTrip: 42000,
    tripCount: 3,
    completed: false,
    customerShortName: 'ВостокОпт',
    performerShortName: 'БетаКарго',
  }),
  seedOrder({
    id: 4,
    customerId: 3,
    performerId: 2,
    vehicleId: 2,
    driverId: 2,
    orderDate: '2024-06-05',
    loadingPlace: 'Холодильный хаб "А", температура режим +2...+6 C',
    loadingContact: '+7 (863) 300-55-55, ответственный за рефрежим',
    unloadingPlace: 'Городской хаб, кольцевая дорога, съезд 14 км',
    unloadingContact: '+7 (495) 500-66-66, круглосуточная приемка',
    totalPrice: 190000,
    pricePerTrip: 47500,
    tripCount: 4,
    completed: true,
    customerShortName: 'ЮгЛогистик',
    performerShortName: 'АльфаТранс',
  }),
  seedOrder({
    id: 5,
    customerId: 4,
    performerId: 4,
    vehicleId: null,
    driverId: null,
    orderDate: '2024-06-26',
    loadingPlace: 'Новосибирск, ул. Складская, 88, ворота для рефрижераторов',
    loadingContact: '+7 (383) 400-77-77, служба охраны и пропусков',
    unloadingPlace: 'Новосибирск, пищевой комбинат, въезд со стороны товарного двора',
    unloadingContact: '+7 (383) 400-88-88, отдел снабжения',
    totalPrice: 51800,
    pricePerTrip: 51800,
    tripCount: 1,
    completed: false,
    customerShortName: 'ЗападПродукт',
    performerShortName: 'ГаммаЛайн',
  }),
  seedOrder({
    id: 6,
    customerId: 5,
    performerId: 2,
    vehicleId: 1,
    driverId: 1,
    orderDate: '2024-06-12',
    loadingPlace: 'Завод ЖБИ, проходная #5, погрузка краном',
    loadingContact: '+7 (499) 500-99-99, производственный отдел',
    unloadingPlace: 'Строительная площадка, участок 9, указатели "МетроСтрой"',
    unloadingContact: '+7 (499) 500-10-10, прораб объекта Сидоров В.К.',
    totalPrice: 171000,
    pricePerTrip: 28500,
    tripCount: 6,
    completed: false,
    customerShortName: 'МетроСтрой',
    performerShortName: 'АльфаТранс',
  }),
  seedOrder({
    id: 7,
    customerId: 6,
    performerId: 3,
    vehicleId: 4,
    driverId: 4,
    orderDate: '2024-06-08',
    loadingPlace: 'Элеватор, дорога #2, весовая перед погрузкой',
    loadingContact: '+7 (844) 600-12-12, лаборатория качества зерна',
    unloadingPlace: 'Перерабатывающий завод, въезд на сырьевой склад',
    unloadingContact: '+7 (844) 600-13-13, диспетчер приемки сырья',
    totalPrice: 114000,
    pricePerTrip: 38000,
    tripCount: 3,
    completed: true,
    customerShortName: 'АгроТрейд',
    performerShortName: 'БетаКарго',
  }),
  seedOrder({
    id: 8,
    customerId: 2,
    performerId: 4,
    vehicleId: 5,
    driverId: 5,
    orderDate: '2024-06-14',
    loadingPlace: 'Кросс-док M7, зона консолидации грузов',
    loadingContact: '+7 (812) 200-14-14, начальник смены',
    unloadingPlace: 'Региональный РЦ "Запад", шлюз автоматической идентификации',
    unloadingContact: '+7 (383) 400-15-15, служба логистики РЦ',
    totalPrice: 165000,
    pricePerTrip: 55000,
    tripCount: 3,
    completed: false,
    customerShortName: 'ВостокОпт',
    performerShortName: 'ГаммаЛайн',
  }),
];

let nextOrderId = 9;

const ALL_DOC_TYPES = [
  'CONTRACT_APPLICATION',
  'WAYBILL',
  'ACT_OF_WORK',
] as const;

function documentDescriptors(): OrderDocumentDescriptor[] {
  return ALL_DOC_TYPES.map((documentType) => ({
    documentType,
    formats: ['PDF', 'DOCX'] as const,
    requiresCompleteData: false,
  }));
}

const minimalPdfBytes = new Uint8Array(
  '%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF'
    .split('')
    .map((c) => c.charCodeAt(0)),
);

function parseUrl(raw: string): { pathname: string; searchParams: URLSearchParams } {
  try {
    const u = new URL(raw);
    return { pathname: u.pathname, searchParams: u.searchParams };
  } catch {
    const [pathOnly, query = ''] = raw.split('?');
    return { pathname: pathOnly ?? raw, searchParams: new URLSearchParams(query) };
  }
}

function json(res: HttpResponse<unknown>): HttpResponse<unknown> {
  return res.clone({
    headers: res.headers.set('Content-Type', 'application/json'),
  });
}

function filterByQuery<T extends { shortName?: string; fullName?: string | null }>(
  rows: T[],
  q: string | null,
): T[] {
  if (!q?.trim()) {
    return rows;
  }
  const needle = q.trim().toLowerCase();
  return rows.filter((r) => {
    const sn = (r.shortName ?? '').toLowerCase();
    const fn = (r.fullName ?? '').toLowerCase();
    return sn.includes(needle) || fn.includes(needle);
  });
}

function filterDrivers(rows: DriverResponse[], q: string | null): DriverResponse[] {
  if (!q?.trim()) {
    return rows;
  }
  const needle = q.trim().toLowerCase();
  return rows.filter((r) => r.fullName.toLowerCase().includes(needle));
}

function filterVehicles(rows: VehicleResponse[], q: string | null): VehicleResponse[] {
  if (!q?.trim()) {
    return rows;
  }
  const needle = q.trim().toLowerCase();
  return rows.filter((r) => {
    const bm = (r.brandModel ?? '').toLowerCase();
    const pn = (r.plateNumber ?? '').toLowerCase();
    return bm.includes(needle) || pn.includes(needle);
  });
}

function getOrder(id: number): OrderResponse | undefined {
  return mockOrders.find((o) => o.id === id);
}

function tripFormDraft(customerId?: number | null): TripFormDraftResponse {
  const ordersForCustomer =
    customerId == null
      ? mockOrders
      : mockOrders.filter((o) => o.customerId === customerId);
  const last = ordersForCustomer.reduce<OrderResponse | null>(
    (acc, o) => (!acc || o.orderNumber > acc.orderNumber ? o : acc),
    null,
  );
  const maxNum = ordersForCustomer.reduce(
    (m, o) => (o.orderNumber > m ? o.orderNumber : m),
    0,
  );
  return {
    nextOrderNumber: maxNum > 0 ? maxNum + 1 : 1,
    lastPerformerId: last?.performerId ?? null,
    lastDriverId: last?.driverId ?? null,
    lastVehicleId: last?.vehicleId ?? null,
  };
}

export function handleMockApiRequest(
  req: HttpRequest<unknown>,
): HttpResponse<unknown> | null {
  const { pathname, searchParams } = parseUrl(req.url);
  const method = req.method.toUpperCase();

  if (method === 'POST' && pathname === '/api/v1/auth/login') {
    const body = req.body as LoginRequest;
    return json(
      new HttpResponse({
        status: 200,
        body: { token: mockJwt(body.username, ['ADMIN', 'USER']) } satisfies JwtResponse,
      }),
    );
  }

  if (method === 'POST' && pathname === '/api/v1/auth/register') {
    const body = req.body as RegisterRequest;
    return json(
      new HttpResponse({
        status: 200,
        body: { token: mockJwt(body.username, ['ADMIN', 'USER']) } satisfies JwtResponse,
      }),
    );
  }

  if (method === 'GET' && pathname === '/api/v1/me') {
    const username = jwtSubForMockProfile(req);
    const user: UserResponse = {
      id: 1,
      username,
      roles: ['ADMIN', 'USER'],
    };
    return json(new HttpResponse({ status: 200, body: user }));
  }

  if (method === 'PUT' && pathname === '/api/v1/me/password') {
    return new HttpResponse({ status: 204, body: null });
  }

  if (method === 'GET' && pathname === '/api/v1/me/trip-form-draft') {
    const cidRaw = searchParams.get('customerId');
    const customerId =
      cidRaw === null || cidRaw === '' ? null : Number(cidRaw);
    const draft = tripFormDraft(
      Number.isFinite(customerId) ? customerId : null,
    );
    return json(new HttpResponse({ status: 200, body: draft }));
  }

  if (method === 'POST' && pathname === '/api/v1/admin/users') {
    const body = req.body as UserCreateRequest;
    const created: UserResponse = {
      id: nextAdminUserId++,
      username: body.username,
      roles: body.roles,
    };
    return json(new HttpResponse({ status: 200, body: created }));
  }

  const ordersMatch = /^\/api\/v1\/orders\/(\d+)$/.exec(pathname);
  if (ordersMatch) {
    const id = Number(ordersMatch[1]);
    if (method === 'GET') {
      const o = getOrder(id);
      return o ? json(new HttpResponse({ status: 200, body: o })) : null;
    }
    if (method === 'PUT') {
      const body = req.body as OrderUpdateRequest;
      const existing = getOrder(id);
      if (!existing) {
        return null;
      }
      const merged: OrderResponse = {
        ...existing,
        ...body,
        id: existing.id,
        customerShortName: existing.customerShortName,
        performerShortName: existing.performerShortName,
      };
      mockOrders = mockOrders.map((o) => (o.id === id ? merged : o));
      return json(new HttpResponse({ status: 200, body: merged }));
    }
    if (method === 'DELETE') {
      const existing = getOrder(id);
      if (!existing) {
        return null;
      }
      mockOrders = mockOrders.filter((o) => o.id !== id);
      return new HttpResponse({ status: 204, body: null });
    }
  }

  const documentsMatch = /^\/api\/v1\/orders\/(\d+)\/documents$/.exec(pathname);
  if (documentsMatch && method === 'GET') {
    const id = Number(documentsMatch[1]);
    if (!getOrder(id)) {
      return null;
    }
    return json(new HttpResponse({ status: 200, body: documentDescriptors() }));
  }

  const fileMatch = /^\/api\/v1\/orders\/(\d+)\/documents\/([^/]+)\/file$/.exec(
    pathname,
  );
  if (fileMatch && method === 'GET') {
    const id = Number(fileMatch[1]);
    const docType = fileMatch[2];
    if (!getOrder(id)) {
      return null;
    }
    const format = (searchParams.get('format') ?? 'PDF').toUpperCase();
    const ext = format === 'DOCX' ? 'docx' : 'pdf';
    const mime =
      format === 'DOCX'
        ? 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
        : 'application/pdf';
    const bodyBlob =
      format === 'DOCX'
        ? new Blob([new Uint8Array([0x50, 0x4b, 0x03, 0x04])], { type: mime })
        : new Blob([minimalPdfBytes], { type: mime });
    const fileName = `${docType.toLowerCase()}.${ext}`;
    const headers = new HttpHeaders({
      'Content-Disposition': `attachment; filename="${fileName}"`,
    });
    return new HttpResponse({ status: 200, body: bodyBlob, headers });
  }

  const completeMatch = /^\/api\/v1\/orders\/(\d+)\/complete$/.exec(pathname);
  if (completeMatch && method === 'POST') {
    const id = Number(completeMatch[1]);
    const existing = getOrder(id);
    if (!existing) {
      return null;
    }
    const updated: OrderResponse = { ...existing, completed: true };
    mockOrders = mockOrders.map((o) => (o.id === id ? updated : o));
    return json(new HttpResponse({ status: 200, body: updated }));
  }

  const reopenMatch = /^\/api\/v1\/orders\/(\d+)\/reopen$/.exec(pathname);
  if (reopenMatch && method === 'POST') {
    const id = Number(reopenMatch[1]);
    const existing = getOrder(id);
    if (!existing) {
      return null;
    }
    const updated: OrderResponse = { ...existing, completed: false };
    mockOrders = mockOrders.map((o) => (o.id === id ? updated : o));
    return json(new HttpResponse({ status: 200, body: updated }));
  }

  if (pathname === '/api/v1/orders' && method === 'GET') {
    return json(new HttpResponse({ status: 200, body: [...mockOrders] }));
  }

  if (pathname === '/api/v1/orders' && method === 'POST') {
    const body = req.body as OrderCreateRequest;
    const id = nextOrderId++;
    const cust = customers.find((c) => c.id === body.customerId);
    const perf = performers.find((p) => p.id === body.performerId);
    const trips = body.tripCount ?? 1;
    const perTrip = body.pricePerTrip ?? 35000;
    const total = body.totalPrice ?? perTrip * trips;
    const created = seedOrder({
      id,
      customerId: body.customerId,
      performerId: body.performerId,
      vehicleId: body.vehicleId ?? null,
      driverId: body.driverId ?? null,
      orderDate: body.orderDate ?? '2024-06-30',
      orderNumber: body.orderNumber ?? id + 100,
      loadingPlace: body.loadingPlace ?? 'Адрес погрузки уточняется у заказчика',
      loadingContact:
        body.loadingContact ??
        '+7 (495) 000-00-99, диспетчер мок-сервиса',
      unloadingPlace: body.unloadingPlace ?? 'Адрес выгрузки уточняется у заказчика',
      unloadingContact:
        body.unloadingContact ??
        '+7 (495) 000-00-98, приемка мок-сервиса',
      tripCount: trips,
      pricePerTrip: perTrip,
      totalPrice: total,
      customerShortName: cust?.shortName ?? 'Неизвестный заказчик',
      performerShortName: perf?.shortName ?? 'Неизвестный перевозчик',
      completed: false,
    });
    mockOrders = [...mockOrders, created];
    return json(new HttpResponse({ status: 200, body: created }));
  }

  const cat = matchCatalog(pathname);
  if (cat) {
    return handleCatalog(method, cat, searchParams, req.body);
  }

  return null;
}

type CatalogKind = 'customers' | 'performers' | 'drivers' | 'vehicles';

function matchCatalog(pathname: string): { kind: CatalogKind; id: number | null } | null {
  const m =
    /^\/api\/v1\/(customers|performers|drivers|vehicles)(?:\/(\d+))?$/.exec(pathname);
  if (!m) {
    return null;
  }
  const kind = m[1] as CatalogKind;
  const id = m[2] != null ? Number(m[2]) : null;
  return { kind, id };
}

function handleCatalog(
  method: string,
  cat: { kind: CatalogKind; id: number | null },
  searchParams: URLSearchParams,
  body: unknown,
): HttpResponse<unknown> | null {
  const q = searchParams.get('q');

  if (cat.kind === 'customers') {
    if (method === 'GET' && cat.id === null) {
      return json(
        new HttpResponse({
          status: 200,
          body: filterByQuery(customers, q),
        }),
      );
    }
    if (method === 'GET' && cat.id !== null) {
      const row = customers.find((c) => c.id === cat.id);
      return row ? json(new HttpResponse({ status: 200, body: row })) : null;
    }
    if (method === 'POST' && cat.id === null) {
      const b = body as CustomerRequest;
      const id = nextCustomerId++;
      const row: CustomerResponse = {
        id,
        shortName: b.shortName,
        fullName: b.fullName ?? null,
        phone: b.phone ?? null,
        requisites: b.requisites ?? null,
      };
      customers = [...customers, row];
      return json(new HttpResponse({ status: 200, body: row }));
    }
    if (method === 'PUT' && cat.id !== null) {
      const b = body as CustomerRequest;
      const existing = customers.find((c) => c.id === cat.id);
      if (!existing) {
        return null;
      }
      const row: CustomerResponse = {
        ...existing,
        shortName: b.shortName,
        fullName: b.fullName ?? null,
        phone: b.phone ?? null,
        requisites: b.requisites ?? null,
      };
      customers = customers.map((c) => (c.id === cat.id ? row : c));
      return json(new HttpResponse({ status: 200, body: row }));
    }
    if (method === 'DELETE' && cat.id !== null) {
      if (!customers.some((c) => c.id === cat.id)) {
        return null;
      }
      customers = customers.filter((c) => c.id !== cat.id);
      return new HttpResponse({ status: 204, body: null });
    }
  }

  if (cat.kind === 'performers') {
    if (method === 'GET' && cat.id === null) {
      return json(
        new HttpResponse({
          status: 200,
          body: filterByQuery(performers, q),
        }),
      );
    }
    if (method === 'GET' && cat.id !== null) {
      const row = performers.find((p) => p.id === cat.id);
      return row ? json(new HttpResponse({ status: 200, body: row })) : null;
    }
    if (method === 'POST' && cat.id === null) {
      const b = body as PerformerRequest;
      const id = nextPerformerId++;
      const row: PerformerResponse = {
        id,
        shortName: b.shortName,
        fullName: b.fullName ?? null,
        phone: b.phone ?? null,
        bankName: b.bankName ?? null,
        inn: b.inn ?? null,
        bik: b.bik ?? null,
        kpp: b.kpp ?? null,
        paymentAccount: b.paymentAccount ?? null,
        corrAccount: b.corrAccount ?? null,
        requisites: b.requisites ?? null,
      };
      performers = [...performers, row];
      return json(new HttpResponse({ status: 200, body: row }));
    }
    if (method === 'PUT' && cat.id !== null) {
      const b = body as PerformerRequest;
      const existing = performers.find((p) => p.id === cat.id);
      if (!existing) {
        return null;
      }
      const row: PerformerResponse = {
        ...existing,
        shortName: b.shortName,
        fullName: b.fullName ?? null,
        phone: b.phone ?? null,
        bankName: b.bankName ?? null,
        inn: b.inn ?? null,
        bik: b.bik ?? null,
        kpp: b.kpp ?? null,
        paymentAccount: b.paymentAccount ?? null,
        corrAccount: b.corrAccount ?? null,
        requisites: b.requisites ?? null,
      };
      performers = performers.map((p) => (p.id === cat.id ? row : p));
      return json(new HttpResponse({ status: 200, body: row }));
    }
    if (method === 'DELETE' && cat.id !== null) {
      if (!performers.some((p) => p.id === cat.id)) {
        return null;
      }
      performers = performers.filter((p) => p.id !== cat.id);
      return new HttpResponse({ status: 204, body: null });
    }
  }

  if (cat.kind === 'drivers') {
    if (method === 'GET' && cat.id === null) {
      return json(
        new HttpResponse({
          status: 200,
          body: filterDrivers(drivers, q),
        }),
      );
    }
    if (method === 'GET' && cat.id !== null) {
      const row = drivers.find((d) => d.id === cat.id);
      return row ? json(new HttpResponse({ status: 200, body: row })) : null;
    }
    if (method === 'POST' && cat.id === null) {
      const b = body as DriverRequest;
      const id = nextDriverId++;
      const row: DriverResponse = {
        id,
        performerId: b.performerId,
        fullName: b.fullName,
        phone: b.phone ?? null,
      };
      drivers = [...drivers, row];
      return json(new HttpResponse({ status: 200, body: row }));
    }
    if (method === 'PUT' && cat.id !== null) {
      const b = body as DriverRequest;
      const existing = drivers.find((d) => d.id === cat.id);
      if (!existing) {
        return null;
      }
      const row: DriverResponse = {
        ...existing,
        performerId: b.performerId,
        fullName: b.fullName,
        phone: b.phone ?? null,
      };
      drivers = drivers.map((d) => (d.id === cat.id ? row : d));
      return json(new HttpResponse({ status: 200, body: row }));
    }
    if (method === 'DELETE' && cat.id !== null) {
      if (!drivers.some((d) => d.id === cat.id)) {
        return null;
      }
      drivers = drivers.filter((d) => d.id !== cat.id);
      return new HttpResponse({ status: 204, body: null });
    }
  }

  if (cat.kind === 'vehicles') {
    if (method === 'GET' && cat.id === null) {
      return json(
        new HttpResponse({
          status: 200,
          body: filterVehicles(vehicles, q),
        }),
      );
    }
    if (method === 'GET' && cat.id !== null) {
      const row = vehicles.find((v) => v.id === cat.id);
      return row ? json(new HttpResponse({ status: 200, body: row })) : null;
    }
    if (method === 'POST' && cat.id === null) {
      const b = body as VehicleRequest;
      const id = nextVehicleId++;
      const row: VehicleResponse = {
        id,
        performerId: b.performerId,
        brandModel: b.brandModel ?? null,
        plateNumber: b.plateNumber ?? null,
        type: b.type ?? null,
      };
      vehicles = [...vehicles, row];
      return json(new HttpResponse({ status: 200, body: row }));
    }
    if (method === 'PUT' && cat.id !== null) {
      const b = body as VehicleRequest;
      const existing = vehicles.find((v) => v.id === cat.id);
      if (!existing) {
        return null;
      }
      const row: VehicleResponse = {
        ...existing,
        performerId: b.performerId,
        brandModel: b.brandModel ?? null,
        plateNumber: b.plateNumber ?? null,
        type: b.type ?? null,
      };
      vehicles = vehicles.map((v) => (v.id === cat.id ? row : v));
      return json(new HttpResponse({ status: 200, body: row }));
    }
    if (method === 'DELETE' && cat.id !== null) {
      if (!vehicles.some((v) => v.id === cat.id)) {
        return null;
      }
      vehicles = vehicles.filter((v) => v.id !== cat.id);
      return new HttpResponse({ status: 204, body: null });
    }
  }

  return null;
}
