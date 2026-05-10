import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { guestGuard } from './guards/guest.guard';
import { roleGuard } from './guards/role.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./pages/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./pages/register/register.component').then(
        (m) => m.RegisterComponent,
      ),
  },
  {
    path: 'forbidden',
    loadComponent: () =>
      import('./pages/forbidden/forbidden.component').then(
        (m) => m.ForbiddenComponent,
      ),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/shell/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'orders' },
      {
        path: 'orders',
        loadComponent: () =>
          import('./pages/trips/trip-list.component').then(
            (m) => m.TripListComponent,
          ),
      },
      {
        path: 'orders/new',
        loadComponent: () =>
          import('./pages/trips/trip-new.component').then(
            (m) => m.TripNewComponent,
          ),
      },
      {
        path: 'orders/:orderId/edit',
        loadComponent: () =>
          import('./pages/trips/trip-new.component').then(
            (m) => m.TripNewComponent,
          ),
      },
      {
        path: 'orders/:orderId',
        loadComponent: () =>
          import('./pages/trip-edit/trip-edit.component').then(
            (m) => m.TripEditComponent,
          ),
      },
      {
        path: 'account',
        loadComponent: () =>
          import('./pages/account/account-page.component').then(
            (m) => m.AccountPageComponent,
          ),
      },
      {
        path: 'catalogs',
        pathMatch: 'full',
        redirectTo: 'catalogs/customers',
      },
      {
        path: 'catalogs/customers',
        loadComponent: () =>
          import('./pages/catalogs/customers/customers.component').then(
            (m) => m.CustomersComponent,
          ),
      },
      {
        path: 'catalogs/performers',
        loadComponent: () =>
          import('./pages/catalogs/performers/performers.component').then(
            (m) => m.PerformersComponent,
          ),
      },
      {
        path: 'catalogs/drivers',
        loadComponent: () =>
          import('./pages/catalogs/drivers/drivers.component').then(
            (m) => m.DriversComponent,
          ),
      },
      {
        path: 'catalogs/vehicles',
        loadComponent: () =>
          import('./pages/catalogs/vehicles/vehicles.component').then(
            (m) => m.VehiclesComponent,
          ),
      },
      {
        path: 'admin/users',
        canActivate: [roleGuard('ADMIN')],
        loadComponent: () =>
          import('./pages/admin/users/users-page.component').then(
            (m) => m.UsersPageComponent,
          ),
      },
    ],
  },
  {
    path: '**',
    loadComponent: () =>
      import('./pages/not-found/not-found.component').then(
        (m) => m.NotFoundComponent,
      ),
  },
];
