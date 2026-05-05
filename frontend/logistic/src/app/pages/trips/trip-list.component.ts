import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { DropdownModule } from 'primeng/dropdown';
import { Button } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TripApiService } from '../../core/trip-api.service';
import { TripResponse, TripStatus } from '../../core/trip.models';
import { TripStatusBadgeComponent } from '../../shared/layout/trip-status-badge.component';

@Component({
  selector: 'app-trip-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    TableModule,
    DropdownModule,
    Button,
    TripStatusBadgeComponent,
  ],
  templateUrl: './trip-list.component.html',
  styleUrl: './trip-list.component.css',
})
export class TripListComponent implements OnInit {
  private readonly api = inject(TripApiService);
  private readonly router = inject(Router);

  trips: TripResponse[] = [];
  /** null — все статусы */
  filter: TripStatus | null = null;

  readonly statusOptions: { label: string; value: TripStatus | null }[] = [
    { label: 'Все', value: null },
    { label: 'Черновик', value: 'DRAFT' },
    { label: 'На согласовании', value: 'PENDING_APPROVAL' },
    { label: 'Утверждён', value: 'APPROVED' },
    { label: 'Архив', value: 'ARCHIVED' },
  ];

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    const st = this.filter == null ? undefined : this.filter;
    this.api.list(st).subscribe((t) => (this.trips = t));
  }

  openTrip(id: number): void {
    void this.router.navigate(['/trips', id]);
  }
}
