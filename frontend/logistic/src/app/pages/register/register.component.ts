import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { GuestHeaderComponent } from '../../shared/guest-header/guest-header.component';
import { Button } from 'primeng/button';
import { Card } from 'primeng/card';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import {
  newPasswordApiErrorMessage,
  registerUsernameErrorMessage,
} from '../../shared/forms/password-api-messages';

const passwordMatchValidator: ValidatorFn = (
  group: AbstractControl,
): ValidationErrors | null => {
  const password = group.get('password')?.value as string | undefined;
  const confirm = group.get('passwordConfirm')?.value as string | undefined;
  if (password == null || confirm == null) {
    return null;
  }
  return password === confirm ? null : { passwordMismatch: true };
};

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    GuestHeaderComponent,
    RouterLink,
    Card,
    InputText,
    Password,
    Button,
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css',
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group(
    {
      username: [
        '',
        [
          Validators.required,
          Validators.minLength(3),
          Validators.maxLength(128),
        ],
      ],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(6),
          Validators.maxLength(128),
        ],
      ],
      passwordConfirm: ['', Validators.required],
    },
    { validators: passwordMatchValidator },
  );

  busy = false;

  usernameError(): string | null {
    return registerUsernameErrorMessage(this.form.controls.username);
  }

  passwordError(): string | null {
    return newPasswordApiErrorMessage(this.form.controls.password);
  }

  submit(): void {
    if (this.form.invalid || this.busy) {
      this.form.markAllAsTouched();
      return;
    }
    const { username, password } = this.form.getRawValue();
    this.busy = true;
    this.auth.register(username.trim(), password).subscribe({
      next: () => void this.router.navigateByUrl('/orders'),
      error: () => {
        this.busy = false;
      },
      complete: () => {
        this.busy = false;
      },
    });
  }
}
