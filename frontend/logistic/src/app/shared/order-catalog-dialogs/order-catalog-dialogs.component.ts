import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { FormGroup } from '@angular/forms';
import {
  CustomerResponse,
  DriverResponse,
  PerformerResponse,
  VehicleResponse,
} from '../../core/catalog.models';
import { CustomerCatalogDialogComponent } from '../catalog-edit-dialogs/customer-catalog-dialog.component';
import {
  CustomerCatalogSaveEvent,
  DriverCatalogSaveEvent,
  PerformerCatalogSaveEvent,
  VehicleCatalogSaveEvent,
} from '../catalog-edit-dialogs/catalog-save.models';
import { DriverCatalogDialogComponent } from '../catalog-edit-dialogs/driver-catalog-dialog.component';
import { PerformerCatalogDialogComponent } from '../catalog-edit-dialogs/performer-catalog-dialog.component';
import { VehicleCatalogDialogComponent } from '../catalog-edit-dialogs/vehicle-catalog-dialog.component';

@Component({
  selector: 'app-order-catalog-dialogs',
  standalone: true,
  imports: [
    CommonModule,
    CustomerCatalogDialogComponent,
    PerformerCatalogDialogComponent,
    DriverCatalogDialogComponent,
    VehicleCatalogDialogComponent,
  ],
  templateUrl: './order-catalog-dialogs.component.html',
})
export class OrderCatalogDialogsComponent {
  /** Форма заказа (родитель): customerId, performerId, driverId, vehicleId. */
  @Input({ required: true }) form!: FormGroup;

  @Output() readonly catalogSaved = new EventEmitter<void>();
  /** Сообщение для баннера родителя (например «Сначала выберите исполнителя»). */
  @Output() readonly catalogHint = new EventEmitter<string>();

  @ViewChild(CustomerCatalogDialogComponent)
  private customerDlg!: CustomerCatalogDialogComponent;
  @ViewChild(PerformerCatalogDialogComponent)
  private performerDlg!: PerformerCatalogDialogComponent;
  @ViewChild(DriverCatalogDialogComponent)
  private driverDlg!: DriverCatalogDialogComponent;
  @ViewChild(VehicleCatalogDialogComponent)
  private vehicleDlg!: VehicleCatalogDialogComponent;

  /** Исполнитель из формы заказа — фиксируется для водителя/ТС. */
  orderPerformerId(): number | null {
    const id = this.form.getRawValue().performerId as number | null;
    return id == null ? null : id;
  }

  openCustomerCreate(): void {
    this.customerDlg.openCreate();
  }

  openCustomerEdit(customers: CustomerResponse[]): void {
    const id = this.form.getRawValue().customerId as number | null;
    if (id == null) {
      return;
    }
    const c = customers.find((x) => x.id === id);
    if (!c) {
      return;
    }
    this.customerDlg.openEdit(c);
  }

  openPerformerCreate(): void {
    this.performerDlg.openCreate();
  }

  openPerformerEdit(performers: PerformerResponse[]): void {
    const id = this.form.getRawValue().performerId as number | null;
    if (id == null) {
      return;
    }
    const p = performers.find((x) => x.id === id);
    if (!p) {
      return;
    }
    this.performerDlg.openEdit(p);
  }

  openDriverCreate(): void {
    if (this.form.getRawValue().performerId == null) {
      this.catalogHint.emit('Сначала выберите исполнителя.');
      return;
    }
    this.driverDlg.openCreate();
  }

  openDriverEdit(drivers: DriverResponse[]): void {
    const id = this.form.getRawValue().driverId as number | null;
    if (id == null) {
      return;
    }
    const d = drivers.find((x) => x.id === id);
    if (!d) {
      return;
    }
    this.driverDlg.openEdit(d);
  }

  openVehicleCreate(): void {
    if (this.form.getRawValue().performerId == null) {
      this.catalogHint.emit('Сначала выберите исполнителя.');
      return;
    }
    this.vehicleDlg.openCreate();
  }

  openVehicleEdit(vehicles: VehicleResponse[]): void {
    const id = this.form.getRawValue().vehicleId as number | null;
    if (id == null) {
      return;
    }
    const v = vehicles.find((x) => x.id === id);
    if (!v) {
      return;
    }
    this.vehicleDlg.openEdit(v);
  }

  onCustomerSaved(ev: CustomerCatalogSaveEvent): void {
    if (ev.wasCreate) {
      this.form.patchValue({ customerId: ev.entity.id });
    }
    this.catalogSaved.emit();
  }

  onPerformerSaved(ev: PerformerCatalogSaveEvent): void {
    if (ev.wasCreate) {
      this.form.patchValue({ performerId: ev.entity.id });
    }
    this.catalogSaved.emit();
  }

  onDriverSaved(ev: DriverCatalogSaveEvent): void {
    if (ev.wasCreate) {
      this.form.patchValue({ driverId: ev.entity.id });
    }
    this.catalogSaved.emit();
  }

  onVehicleSaved(ev: VehicleCatalogSaveEvent): void {
    if (ev.wasCreate) {
      this.form.patchValue({ vehicleId: ev.entity.id });
    }
    this.catalogSaved.emit();
  }
}
