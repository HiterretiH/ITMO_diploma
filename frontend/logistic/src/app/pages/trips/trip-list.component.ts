import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TabsModule } from 'primeng/tabs';
import { OrderApiService } from '../../core/order-api.service';
import { OrderResponse } from '../../core/order.models';
import { OrderStatusBadgeComponent } from '../../shared/layout/order-status-badge.component';
import {
  orderReadyForBackendComplete,
  orderUiPhase,
} from '../../shared/order-ui';

@Component({
  selector: 'app-trip-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    TableModule,
    TabsModule,
    Button,
    OrderStatusBadgeComponent,
  ],
  templateUrl: './trip-list.component.html',
  styleUrl: './trip-list.component.css',
})
export class TripListComponent implements OnInit {
  private readonly api = inject(OrderApiService);
  private readonly router = inject(Router);
  private readonly confirm = inject(ConfirmationService);

  orders: OrderResponse[] = [];
  listTab: 'active' | 'done' = 'active';

  onListTabChange(value: string | number): void {
    this.listTab = value === 'done' ? 'done' : 'active';
  }

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.list().subscribe((rows) => (this.orders = rows));
  }

  get tabOrders(): OrderResponse[] {
    return this.listTab === 'active'
      ? this.orders.filter((o) => !o.completed)
      : this.orders.filter((o) => o.completed);
  }

  tabSubtitle(): string {
    return this.listTab === 'active'
      ? 'Рейсы в работе'
      : 'Завершённые рейсы';
  }

  canCompleteRow(o: OrderResponse): boolean {
    return (
      this.listTab === 'active' &&
      !o.completed &&
      orderUiPhase(o, false) === 'ready'
    );
  }

  openOrder(id: number): void {
    void this.router.navigate(['/orders', id]);
  }

  completeOrder(o: OrderResponse): void {
    if (!orderReadyForBackendComplete(o)) {
      return;
    }
    this.confirm.confirm({
      message: 'Завершить рейс? Будут сгенерированы документы.',
      header: 'Подтверждение',
      icon: 'pi pi-check-circle',
      accept: () => {
        this.api.complete(o.id).subscribe(() => this.reload());
      },
    });
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
