import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  username = '';
  password = '';
  error: string | null = null;
  busy = false;

  submit(): void {
    this.error = null;
    this.busy = true;
    this.auth.login(this.username, this.password).subscribe({
      next: () => void this.router.navigateByUrl('/'),
      error: () => {
        this.error = 'Неверный логин или пароль';
        this.busy = false;
      },
      complete: () => (this.busy = false),
    });
  }
}
