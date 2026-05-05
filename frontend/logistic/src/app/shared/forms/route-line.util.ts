const CONTACT_PREFIX = 'Контакт: ';

/** Сборка строки маршрута для API (как в `.ide/main.py`). */
export function buildRouteLine(
  address: string | null | undefined,
  contact: string | null | undefined,
): string | null {
  const a = (address ?? '').trim();
  const c = (contact ?? '').trim();
  if (!a && !c) {
    return null;
  }
  if (!c) {
    return a;
  }
  if (!a) {
    return `${CONTACT_PREFIX}${c}`;
  }
  return `${a}\n${CONTACT_PREFIX}${c}`;
}

/** Разбор `routeFrom` / `routeTo` из API обратно в адрес и контакт. */
export function parseRouteLine(route: string | null | undefined): {
  address: string;
  contact: string;
} {
  const s = (route ?? '').replace(/\r\n/g, '\n');
  if (!s.trim()) {
    return { address: '', contact: '' };
  }
  const idx = s.indexOf('\n');
  if (idx === -1) {
    if (s.startsWith(CONTACT_PREFIX)) {
      return {
        address: '',
        contact: s.slice(CONTACT_PREFIX.length).trim(),
      };
    }
    return { address: s.trim(), contact: '' };
  }
  const address = s.slice(0, idx).trim();
  const rest = s.slice(idx + 1).trim();
  let contact = '';
  if (rest.startsWith(CONTACT_PREFIX)) {
    contact = rest.slice(CONTACT_PREFIX.length).trim();
  }
  return { address, contact };
}
