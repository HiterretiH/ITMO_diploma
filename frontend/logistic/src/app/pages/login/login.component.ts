import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/auth.service';
import { GuestHeaderComponent } from '../../shared/guest-header/guest-header.component';
import { Button } from 'primeng/button';
import { Card } from 'primeng/card';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Password } from 'primeng/password';

@Component({
  selector: 'app-login',
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
    Message,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  busy = false;
  approvedMessage: string | null = null;

  ngOnInit(): void {
    if (this.route.snapshot.queryParamMap.get('approved') === '1') {
      this.approvedMessage =
        'Регистрация одобрена. Войдите в систему с вашим логином и паролем.';
    }
  }

  usernameError(): string | null {
    const c = this.form.controls.username;
    if (!c.touched || !c.errors) {
      return null;
    }
    return c.errors['required'] ? 'Укажите логин' : null;
  }

  passwordError(): string | null {
    const c = this.form.controls.password;
    if (!c.touched || !c.errors) {
      return null;
    }
    return c.errors['required'] ? 'Укажите пароль' : null;
  }

  submit(): void {
    if (this.form.invalid || this.busy) {
      this.form.markAllAsTouched();
      return;
    }
    const { username, password } = this.form.getRawValue();
    const trimmed = username.trim();
    this.busy = true;
    this.auth.login(trimmed, password).subscribe({
      next: () => void this.router.navigateByUrl('/orders'),
      error: (err: HttpErrorResponse) => {
        if (err.status === 403) {
          this.auth.setPendingRegistrationUsername(trimmed);
          void this.router.navigateByUrl('/registration-status');
        }
        this.busy = false;
      },
      complete: () => {
        this.busy = false;
      },
    });
  }
}
