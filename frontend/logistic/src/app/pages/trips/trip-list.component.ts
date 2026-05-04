import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TripApiService } from '../../core/trip-api.service';
import { TripResponse, TripStatus } from '../../core/trip.models';

@Component({
  selector: 'app-trip-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './trip-list.component.html',
  styleUrl: './trip-list.component.css',
})
export class TripListComponent implements OnInit {
  private readonly api = inject(TripApiService);

  trips: TripResponse[] = [];
  filter: TripStatus | '' = '';
  error: string | null = null;

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.error = null;
    const st = this.filter === '' ? undefined : this.filter;
    this.api.list(st).subscribe({
      next: (t) => (this.trips = t),
      error: () => (this.error = 'Не удалось загрузить рейсы'),
    });
  }

  statusLabel(s: TripStatus): string {
    switch (s) {
      case 'DRAFT':
        return 'Черновик';
      case 'PENDING_APPROVAL':
        return 'На согласовании';
      case 'APPROVED':
        return 'Утверждён';
      case 'ARCHIVED':
        return 'Архив';
      default:
        return s;
    }
  }
}
