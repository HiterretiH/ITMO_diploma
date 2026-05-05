import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** ИНН РФ: пусто или 10/12 цифр (как в OpenAPI). */
export const innValidator: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const v = (control.value ?? '').toString().trim();
  if (v === '') {
    return null;
  }
  return /^([0-9]{10}|[0-9]{12})$/.test(v) ? null : { inn: true };
};
