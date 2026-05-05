import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Госномер: буквы (RU/EN) и цифры, без пробелов, 4–10 символов после нормализации. */
export const plateValidator: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const raw = (control.value ?? '').toString().replace(/\s+/g, '').toUpperCase();
  if (raw === '') {
    return null;
  }
  return /^[A-ZА-ЯЁ0-9]{4,10}$/i.test(raw) ? null : { plate: true };
};
