import { FormControl, Validators } from '@angular/forms';
import {
  currentPasswordRequiredMessage,
  newPasswordApiErrorMessage,
  registerUsernameErrorMessage,
} from './password-api-messages';

describe('password-api-messages', () => {
  it('newPasswordApiErrorMessage returns distinct reasons', () => {
    const c1 = new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(128),
      ],
    });
    c1.markAsTouched();
    c1.setValue('');
    c1.updateValueAndValidity();
    expect(newPasswordApiErrorMessage(c1)).toBe('Укажите пароль');

    const c2 = new FormControl('abc', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(128),
      ],
    });
    c2.markAsTouched();
    expect(newPasswordApiErrorMessage(c2)).toBe('Не короче 6 символов');

    const long = 'x'.repeat(129);
    const c3 = new FormControl(long, {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(128),
      ],
    });
    c3.markAsTouched();
    expect(newPasswordApiErrorMessage(c3)).toBe('Не длиннее 128 символов');
  });

  it('currentPasswordRequiredMessage', () => {
    const c = new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    });
    c.markAsTouched();
    expect(currentPasswordRequiredMessage(c)).toBe('Укажите текущий пароль');
  });

  it('registerUsernameErrorMessage', () => {
    const c = new FormControl('ab', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(128),
      ],
    });
    c.markAsTouched();
    expect(registerUsernameErrorMessage(c)).toBe('Не короче 3 символов');
  });
});
