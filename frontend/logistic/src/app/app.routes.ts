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
      { path: '', pathMatch: 'full', redirectTo: 'trips' },
      {
        path: 'trips',
        loadComponent: () =>
          import('./pages/trips/trip-list.component').then(
            (m) => m.TripListComponent,
          ),
      },
      {
        path: 'trips/new',
        loadComponent: () =>
          import('./pages/trips/trip-new.component').then(
            (m) => m.TripNewComponent,
          ),
      },
      {
        path: 'trips/:tripId',
        loadComponent: () =>
          import('./pages/trip-edit/trip-edit.component').then(
            (m) => m.TripEditComponent,
          ),
      },
      {
        path: 'catalogs',
        pathMatch: 'full',
        redirectTo: 'catalogs/counterparties',
      },
      {
        path: 'catalogs/counterparties',
        loadComponent: () =>
          import('./pages/catalogs/counterparties/counterparties-page.component').then(
            (m) => m.CounterpartiesPageComponent,
          ),
      },
      {
        path: 'catalogs/drivers',
        loadComponent: () =>
          import('./pages/catalogs/drivers/drivers-page.component').then(
            (m) => m.DriversPageComponent,
          ),
      },
      {
        path: 'catalogs/vehicles',
        loadComponent: () =>
          import('./pages/catalogs/vehicles/vehicles-page.component').then(
            (m) => m.VehiclesPageComponent,
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
