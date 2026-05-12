import { CommonModule } from '@angular/common';
import { Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Toolbar } from 'primeng/toolbar';
import { ThemeService } from '../../core/theme.service';

@Component({
  selector: 'app-guest-header',
  standalone: true,
  imports: [CommonModule, RouterLink, Toolbar, Button],
  templateUrl: './guest-header.component.html',
  styleUrl: './guest-header.component.css',
})
export class GuestHeaderComponent {
  readonly theme = inject(ThemeService);

  /** Show text link to login (e.g. on register). */
  readonly showLoginLink = input(false);
  /** Show text link to register (e.g. on login). */
  readonly showRegisterLink = input(false);

  cycleTheme(): void {
    this.theme.cycle();
  }

  themeIcon(): string {
    return this.theme.cycleIcon();
  }
}
