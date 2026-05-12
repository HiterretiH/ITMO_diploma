import { AbstractControl } from '@angular/forms';

/** Matches backend @Size(min = 6, max = 128) on new passwords (register, password change, admin user create). */
export function newPasswordApiErrorMessage(
  control: AbstractControl | null | undefined,
): string | null {
  if (!control?.touched || !control.errors) {
    return null;
  }
  const e = control.errors;
  if (e['required']) {
    return 'Укажите пароль';
  }
  if (e['minlength']) {
    return 'Не короче 6 символов';
  }
  if (e['maxlength']) {
    return 'Не длиннее 128 символов';
  }
  return null;
}

export function currentPasswordRequiredMessage(
  control: AbstractControl | null | undefined,
): string | null {
  if (!control?.touched || !control.errors?.['required']) {
    return null;
  }
  return 'Укажите текущий пароль';
}

export function registerUsernameErrorMessage(
  control: AbstractControl | null | undefined,
): string | null {
  if (!control?.touched || !control.errors) {
    return null;
  }
  const e = control.errors;
  if (e['required']) {
    return 'Укажите логин';
  }
  if (e['minlength']) {
    return 'Не короче 3 символов';
  }
  if (e['maxlength']) {
    return 'Не длиннее 128 символов';
  }
  return null;
}
