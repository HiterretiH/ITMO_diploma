import { Component, Input } from '@angular/core';
import { Tag } from 'primeng/tag';
import { OrderResponse } from '../../core/order.models';
import { orderUiPhase } from '../order-ui';

@Component({
  selector: 'app-order-status-badge',
  standalone: true,
  imports: [Tag],
  template: ` <p-tag [severity]="severity" [value]="label" [rounded]="true" /> `,
})
export class OrderStatusBadgeComponent {
  @Input({ required: true }) order!: OrderResponse;
  /** From audit ORDER_COMPLETED or known completion state */
  @Input() completed = false;

  get label(): string {
    switch (orderUiPhase(this.order, this.completed)) {
      case 'completed':
        return 'Завершён';
      case 'ready':
        return 'Готов к завершению';
      default:
        return 'Черновик';
    }
  }

  get severity(): 'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast' {
    switch (orderUiPhase(this.order, this.completed)) {
      case 'completed':
        return 'success';
      case 'ready':
        return 'warn';
      default:
        return 'secondary';
    }
  }
}
