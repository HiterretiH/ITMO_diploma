import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Card } from 'primeng/card';
import { Message } from 'primeng/message';
import { Password } from 'primeng/password';
import { MeApiService } from '../../core/me-api.service';
import { Role, UserResponse } from '../../core/user.models';

const passwordChangeMatchValidator: ValidatorFn = (
  group: AbstractControl,
): ValidationErrors | null => {
  const np = group.get('newPassword')?.value as string | undefined;
  const nc = group.get('newPasswordConfirm')?.value as string | undefined;
  if (np == null || nc == null) {
    return null;
  }
  return np === nc ? null : { passwordMismatch: true };
};

@Component({
  selector: 'app-account-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, Card, Password, Button, Message],
  templateUrl: './account-page.component.html',
  styleUrl: './account-page.component.css',
})
export class AccountPageComponent implements OnInit {
  private readonly me = inject(MeApiService);
  private readonly messages = inject(MessageService);
  private readonly fb = inject(FormBuilder);

  profile: UserResponse | null = null;
  loadError: string | null = null;
  busy = false;

  readonly passwordForm = this.fb.nonNullable.group(
    {
      currentPassword: ['', Validators.required],
      newPassword: [
        '',
        [
          Validators.required,
          Validators.minLength(6),
          Validators.maxLength(128),
        ],
      ],
      newPasswordConfirm: ['', Validators.required],
    },
    { validators: passwordChangeMatchValidator },
  );

  ngOnInit(): void {
    this.me.getProfile().subscribe({
      next: (p) => {
        this.profile = p;
        this.loadError = null;
      },
      error: () => {
        this.loadError = 'Не удалось загрузить данные профиля.';
      },
    });
  }

  roleLabel(r: Role): string {
    return r === 'ADMIN' ? 'Администратор' : 'Пользователь';
  }

  rolesLine(roles: Role[]): string {
    return roles.map((r) => this.roleLabel(r)).join(', ');
  }

  submitPassword(): void {
    if (this.busy) {
      return;
    }
    this.passwordForm.markAllAsTouched();
    if (this.passwordForm.invalid) {
      return;
    }
    const v = this.passwordForm.getRawValue();
    this.busy = true;
    this.me
      .changePassword({
        currentPassword: v.currentPassword,
        newPassword: v.newPassword,
      })
      .subscribe({
        next: () => {
          this.busy = false;
          this.passwordForm.reset();
          this.messages.add({
            severity: 'info',
            summary: 'Пароль изменён',
            detail:
              'При следующем входе используйте новый пароль. Текущая сессия действует до выхода.',
            life: 8000,
          });
        },
        error: () => {
          this.busy = false;
        },
      });
  }
}
