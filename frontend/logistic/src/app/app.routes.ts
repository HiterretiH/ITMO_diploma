import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/login/login.component').then((m) => m.LoginComponent),
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
          import('./pages/trips/trip-list.component').then((m) => m.TripListComponent),
      },
      {
        path: 'trips/:tripId',
        loadComponent: () =>
          import('./pages/trip-edit/trip-edit.component').then((m) => m.TripEditComponent),
      },
      {
        path: 'catalogs',
        loadComponent: () =>
          import('./pages/catalogs/catalogs.component').then((m) => m.CatalogsComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
