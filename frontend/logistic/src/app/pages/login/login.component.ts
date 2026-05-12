import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { GuestHeaderComponent } from '../../shared/guest-header/guest-header.component';
import { Button } from 'primeng/button';
import { Card } from 'primeng/card';
import { InputText } from 'primeng/inputtext';
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
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  busy = false;

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
    this.busy = true;
    this.auth.login(username.trim(), password).subscribe({
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
