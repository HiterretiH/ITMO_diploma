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
      case 'DRAFT':
        return 'Черновик';
      case 'PENDING_APPROVAL':
        return 'На согласовании';
      case 'APPROVED':
        return 'Утверждён';
      case 'ARCHIVED':
        return 'Архив';
      default:
        return this.status;
    }
  }

  get severity(): 'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast' {
    switch (this.status) {
      case 'DRAFT':
        return 'secondary';
      case 'PENDING_APPROVAL':
        return 'warn';
      case 'APPROVED':
        return 'success';
      case 'ARCHIVED':
        return 'contrast';
      default:
        return 'info';
    }
  }
}
