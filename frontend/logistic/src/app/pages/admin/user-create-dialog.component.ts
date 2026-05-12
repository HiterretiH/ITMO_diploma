import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { MultiSelectModule } from 'primeng/multiselect';
import { Password } from 'primeng/password';
import { InputText } from 'primeng/inputtext';
import { AdminApiService } from '../../core/admin-api.service';
import { Role } from '../../core/user.models';
import { newPasswordApiErrorMessage } from '../../shared/forms/password-api-messages';

@Component({
  selector: 'app-user-create-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    Dialog,
    Button,
    InputText,
    Password,
    MultiSelectModule,
  ],
  templateUrl: './user-create-dialog.component.html',
  styleUrl: './user-create-dialog.component.css',
})
export class UserCreateDialogComponent {
  private readonly api = inject(AdminApiService);
  private readonly fb = inject(FormBuilder);
  private readonly messages = inject(MessageService);

  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();

  saving = false;

  usernameError(): string | null {
    const c = this.form.controls.username;
    if (!c.touched || !c.errors) {
      return null;
    }
    if (c.errors['required']) {
      return 'Укажите логин';
    }
    if (c.errors['minlength']) {
      return 'Не короче 3 символов';
    }
    if (c.errors['maxlength']) {
      return 'Не длиннее 128 символов';
    }
    return null;
  }

  passwordError(): string | null {
    return newPasswordApiErrorMessage(this.form.controls.password);
  }

  readonly roleOptions: { label: string; value: Role }[] = [
    { label: 'Пользователь', value: 'USER' },
    { label: 'Администратор', value: 'ADMIN' },
  ];

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(128)]],
    password: [
      '',
      [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(128),
      ],
    ],
    roles: this.fb.nonNullable.control<Role[]>(['USER'], {
      validators: [(c) => ((c.value?.length ?? 0) > 0 ? null : { roles: true })],
    }),
  });

  onVisibleChange(v: boolean): void {
    this.visibleChange.emit(v);
    if (!v) {
      this.form.reset({
        username: '',
        password: '',
        roles: ['USER'],
      });
    }
  }

  submit(): void {
    if (this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.saving = true;
    this.api
      .createUser({
        username: v.username.trim(),
        password: v.password,
        roles: v.roles,
      })
      .subscribe({
        next: (u) => {
          this.saving = false;
          this.messages.add({
            severity: 'success',
            summary: 'Пользователь создан',
            detail: u.username,
            life: 4000,
          });
          this.onVisibleChange(false);
        },
        error: () => {
          this.saving = false;
        },
      });
  }
}
