import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { DropdownModule } from 'primeng/dropdown';
import { Button } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { OrderApiService } from '../../core/order-api.service';
import { OrderResponse } from '../../core/order.models';
import { OrderStatusBadgeComponent } from '../../shared/layout/order-status-badge.component';
import { orderUiPhase } from '../../shared/order-ui';

export type OrderListFilter = 'all' | 'draft' | 'ready';

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
    OrderStatusBadgeComponent,
  ],
  templateUrl: './trip-list.component.html',
  styleUrl: './trip-list.component.css',
})
export class TripListComponent implements OnInit {
  private readonly api = inject(OrderApiService);
  private readonly router = inject(Router);

  orders: OrderResponse[] = [];
  /** На списке нет аудита по каждому заказу — завершённые не выделяем фильтром */
  filter: OrderListFilter | null = null;

  readonly filterOptions: { label: string; value: OrderListFilter | null }[] = [
    { label: 'Все', value: null },
    { label: 'Черновик', value: 'draft' },
    { label: 'Готов к завершению', value: 'ready' },
  ];

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.list().subscribe((rows) => (this.orders = rows));
  }

  get filteredOrders(): OrderResponse[] {
    const f = this.filter;
    if (f == null || f === 'all') {
      return this.orders;
    }
    return this.orders.filter((o) => {
      const phase = orderUiPhase(o, false);
      if (f === 'draft') {
        return phase === 'draft';
      }
      if (f === 'ready') {
        return phase === 'ready';
      }
      return true;
    });
  }

  openOrder(id: number): void {
    void this.router.navigate(['/orders', id]);
  }

  routeSnippet(o: OrderResponse): string {
    const a = (o.loadingPlace ?? '').trim();
    const b = (o.unloadingPlace ?? '').trim();
    const trunc = (s: string, n: number) =>
      s.length > n ? `${s.slice(0, n)}…` : s;
    if (!a && !b) {
      return '—';
    }
    return `${trunc(a, 36)} → ${trunc(b, 36)}`;
  }
}

