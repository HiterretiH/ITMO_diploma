import { Component, Input } from '@angular/core';
import { Tag } from 'primeng/tag';
import { TripStatus } from '../../core/trip.models';

@Component({
  selector: 'app-trip-status-badge',
  standalone: true,
  imports: [Tag],
  template: ` <p-tag [severity]="severity" [value]="label" [rounded]="true" /> `,
})
export class TripStatusBadgeComponent {
  @Input({ required: true }) status!: TripStatus;

  get label(): string {
    switch (this.status) {
      case 'IN_PROGRESS':
        return 'В работе';
      case 'COMPLETED':
        return 'Завершён';
      default:
        return this.status;
    }
  }

  get severity(): 'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast' {
    switch (this.status) {
      case 'IN_PROGRESS':
        return 'info';
      case 'COMPLETED':
        return 'success';
      default:
        return 'info';
    }
  }
}
