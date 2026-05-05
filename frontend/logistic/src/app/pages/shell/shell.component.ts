import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MenuItem } from 'primeng/api';
import { Button, ButtonDirective } from 'primeng/button';
import { Menu } from 'primeng/menu';
import { Toolbar } from 'primeng/toolbar';
import { AuthService } from '../../core/auth.service';
import { UserCreateDialogComponent } from '../admin/user-create-dialog.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    Toolbar,
    Button,
    ButtonDirective,
    Menu,
    UserCreateDialogComponent,
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.css',
})
export class ShellComponent {
  readonly auth = inject(AuthService);

  adminUserDialogVisible = false;

  readonly catalogItems: MenuItem[] = [
    {
      label: 'Контрагенты',
      routerLink: ['/catalogs/counterparties'],
    },
    {
      label: 'Водители',
      routerLink: ['/catalogs/drivers'],
    },
    {
      label: 'Транспорт',
      routerLink: ['/catalogs/vehicles'],
    },
  ];
}
