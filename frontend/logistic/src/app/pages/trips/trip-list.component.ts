import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DropdownModule } from 'primeng/dropdown';
import { TableModule } from 'primeng/table';
import { TabsModule } from 'primeng/tabs';
import { finalize } from 'rxjs/operators';
import { OrderApiService } from '../../core/order-api.service';
import { OrderResponse } from '../../core/order.models';
import {
  orderReadyForBackendComplete,
  orderUiPhase,
} from '../../shared/order-ui';
import {
  CompletedTripsStats,
  filterCompletedTrips,
  summarizeCompletedTrips,
} from './trip-list-filters';

@Component({
  selector: 'app-trip-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    TableModule,
    TabsModule,
    Button,
    DropdownModule,
    DatePickerModule,
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
  /** While completing a trip, button shows spinner; other rows stay clickable after reload. */
  completingOrderId: number | null = null;

  /** Completed-tab filter: empty string = all customers (by short name). */
  doneCustomer = '';
  doneDateFrom: Date | null = null;
  doneDateTo: Date | null = null;

  onListTabChange(value: string | number): void {
    this.listTab = value === 'done' ? 'done' : 'active';
  }

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.list().subscribe((rows) => (this.orders = rows));
  }

  completedCustomerOptions(): { label: string; value: string }[] {
    const names = new Set<string>();
    for (const o of this.orders) {
      if (!o.completed) {
        continue;
      }
      const n = (o.customerShortName ?? '').trim();
      if (n) {
        names.add(n);
      }
    }
    return [
      { label: 'Все заказчики', value: '' },
      ...[...names]
        .sort((a, b) => a.localeCompare(b, 'ru'))
        .map((v) => ({ label: v, value: v })),
    ];
  }

  private toLocalIsoDate(d: Date | null | undefined): string | null {
    if (!d) {
      return null;
    }
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  get tabOrders(): OrderResponse[] {
    const base =
      this.listTab === 'active'
        ? this.orders.filter((o) => !o.completed)
        : this.orders.filter((o) => o.completed);
    if (this.listTab !== 'done') {
      return base;
    }
    return filterCompletedTrips(base, this.doneCustomer, {
      dateFromInclusive: this.toLocalIsoDate(this.doneDateFrom),
      dateToInclusive: this.toLocalIsoDate(this.doneDateTo),
    });
  }

  doneStats(): CompletedTripsStats {
    if (this.listTab !== 'done') {
      return { count: 0, totalPriceSum: null };
    }
    return summarizeCompletedTrips(this.tabOrders);
  }

  clearDoneFilters(): void {
    this.doneCustomer = '';
    this.doneDateFrom = null;
    this.doneDateTo = null;
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
      orderUiPhase(o, o.completed) === 'ready'
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
      message: 'Завершить рейс?',
      header: 'Подтверждение',
      icon: 'pi pi-check-circle',
      accept: () => {
        this.completingOrderId = o.id;
        this.api
          .complete(o.id)
          .pipe(finalize(() => (this.completingOrderId = null)))
          .subscribe(() => this.reload());
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
