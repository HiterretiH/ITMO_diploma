import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { TripApiService } from '../../core/trip-api.service';

/** POST /trips и редирект на карточку (фаза E5 дорабатывает при необходимости). */
@Component({
  selector: 'app-trip-new',
  standalone: true,
  template: `<p>Создание рейса…</p>`,
})
export class TripNewComponent implements OnInit {
  private readonly api = inject(TripApiService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.api.create().subscribe({
      next: (trip) => void this.router.navigateByUrl(`/trips/${trip.id}`),
      error: () => void this.router.navigateByUrl('/trips'),
    });
  }
}
