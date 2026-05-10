import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { InputTextarea } from 'primeng/inputtextarea';
import { CatalogApiService } from '../../core/catalog-api.service';
import {
  CustomerResponse,
  DriverResponse,
  PerformerResponse,
  VehicleResponse,
} from '../../core/catalog.models';
import { innValidator } from '../forms/inn.validator';
import { plateValidator } from '../forms/plate.validator';

@Component({
  selector: 'app-order-catalog-dialogs',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    Dialog,
    Button,
    InputText,
    InputTextarea,
  ],
  templateUrl: './order-catalog-dialogs.component.html',
})
export class OrderCatalogDialogsComponent {
  private readonly catalog = inject(CatalogApiService);
  private readonly fb = inject(FormBuilder);

  /** Форма заказа (родитель): customerId, performerId, driverId, vehicleId. */
  @Input({ required: true }) form!: FormGroup;

  @Output() readonly catalogSaved = new EventEmitter<void>();
  /** Сообщение для баннера родителя (например «Сначала выберите исполнителя»). */
  @Output() readonly catalogHint = new EventEmitter<string>();

  customerDialogVisible = false;
  customerSaving = false;
  customerEditingId: number | null = null;
  readonly customerForm = this.fb.nonNullable.group({
    shortName: ['', [Validators.required, Validators.minLength(1)]],
    fullName: [''],
    phone: [''],
    requisites: [''],
  });

  performerDialogVisible = false;
  performerSaving = false;
  performerEditingId: number | null = null;
  readonly performerForm = this.fb.nonNullable.group({
    shortName: ['', [Validators.required, Validators.minLength(1)]],
    fullName: [''],
    phone: [''],
    inn: ['', [innValidator]],
  });

  driverDialogVisible = false;
  driverEditingId: number | null = null;
  driverSaving = false;
  readonly driverForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(1)]],
    phone: [''],
    isDefault: [false],
  });

  vehicleDialogVisible = false;
  vehicleEditingId: number | null = null;
  vehicleSaving = false;
  readonly vehicleForm = this.fb.group({
    plateNumber: this.fb.nonNullable.control('', {
      validators: [Validators.required, plateValidator],
    }),
    brandModel: [''],
    type: [''],
    isDefault: this.fb.control(false),
  });

  openCustomerCreate(): void {
    this.customerEditingId = null;
    this.customerForm.reset({
      shortName: '',
      fullName: '',
      phone: '',
      requisites: '',
    });
    this.customerDialogVisible = true;
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
    this.customerEditingId = c.id;
    this.customerForm.setValue({
      shortName: c.shortName,
      fullName: c.fullName ?? '',
      phone: c.phone ?? '',
      requisites: c.requisites ?? '',
    });
    this.customerDialogVisible = true;
  }

  openPerformerCreate(): void {
    this.performerEditingId = null;
    this.performerForm.reset({
      shortName: '',
      fullName: '',
      phone: '',
      inn: '',
    });
    this.performerDialogVisible = true;
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
    this.performerEditingId = p.id;
    this.performerForm.setValue({
      shortName: p.shortName,
      fullName: p.fullName ?? '',
      phone: p.phone ?? '',
      inn: p.inn ?? '',
    });
    this.performerDialogVisible = true;
  }

  openDriverCreate(): void {
    if (this.form.getRawValue().performerId == null) {
      this.catalogHint.emit('Сначала выберите исполнителя.');
      return;
    }
    this.driverEditingId = null;
    this.driverForm.reset({
      fullName: '',
      phone: '',
      isDefault: false,
    });
    this.driverDialogVisible = true;
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
    this.driverEditingId = d.id;
    this.driverForm.setValue({
      fullName: d.fullName,
      phone: d.phone ?? '',
      isDefault: d.isDefault,
    });
    this.driverDialogVisible = true;
  }

  openVehicleCreate(): void {
    if (this.form.getRawValue().performerId == null) {
      this.catalogHint.emit('Сначала выберите исполнителя.');
      return;
    }
    this.vehicleEditingId = null;
    this.vehicleForm.reset({
      plateNumber: '',
      brandModel: '',
      type: '',
      isDefault: false,
    });
    this.vehicleDialogVisible = true;
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
    this.vehicleEditingId = v.id;
    this.vehicleForm.setValue({
      plateNumber: v.plateNumber ?? '',
      brandModel: v.brandModel ?? '',
      type: v.type ?? '',
      isDefault: v.isDefault,
    });
    this.vehicleDialogVisible = true;
  }

  saveCustomerDialog(): void {
    if (this.customerForm.invalid || this.customerSaving) {
      this.customerForm.markAllAsTouched();
      return;
    }
    const v = this.customerForm.getRawValue();
    const body = {
      shortName: v.shortName.trim(),
      fullName: v.fullName.trim() === '' ? null : v.fullName.trim(),
      phone: v.phone.trim() === '' ? null : v.phone.trim(),
      requisites: v.requisites.trim() === '' ? null : v.requisites.trim(),
    };
    this.customerSaving = true;
    const obs =
      this.customerEditingId != null
        ? this.catalog.updateCustomer(this.customerEditingId, body)
        : this.catalog.createCustomer(body);
    obs.subscribe({
      next: (c) => {
        if (this.customerEditingId == null) {
          this.form.patchValue({ customerId: c.id });
        }
        this.customerSaving = false;
        this.customerDialogVisible = false;
        this.catalogSaved.emit();
      },
      error: () => {
        this.customerSaving = false;
      },
    });
  }

  savePerformerDialog(): void {
    if (this.performerForm.invalid || this.performerSaving) {
      this.performerForm.markAllAsTouched();
      return;
    }
    const v = this.performerForm.getRawValue();
    const body = {
      shortName: v.shortName.trim(),
      fullName: v.fullName.trim() === '' ? null : v.fullName.trim(),
      phone: v.phone.trim() === '' ? null : v.phone.trim(),
      inn: v.inn.trim() === '' ? null : v.inn.trim(),
    };
    this.performerSaving = true;
    const obs =
      this.performerEditingId != null
        ? this.catalog.updatePerformer(this.performerEditingId, body)
        : this.catalog.createPerformer(body);
    obs.subscribe({
      next: (p) => {
        if (this.performerEditingId == null) {
          this.form.patchValue({ performerId: p.id });
        }
        this.performerSaving = false;
        this.performerDialogVisible = false;
        this.catalogSaved.emit();
      },
      error: () => {
        this.performerSaving = false;
      },
    });
  }

  saveDriverDialog(): void {
    const performerId = this.form.getRawValue().performerId as number | null;
    if (performerId == null || this.driverForm.invalid || this.driverSaving) {
      this.driverForm.markAllAsTouched();
      return;
    }
    const v = this.driverForm.getRawValue();
    const body = {
      performerId,
      fullName: v.fullName.trim(),
      phone: v.phone.trim() === '' ? null : v.phone.trim(),
      isDefault: v.isDefault,
    };
    this.driverSaving = true;
    const obs =
      this.driverEditingId != null
        ? this.catalog.updateDriver(this.driverEditingId, body)
        : this.catalog.createDriver(body);
    obs.subscribe({
      next: (d) => {
        if (this.driverEditingId == null) {
          this.form.patchValue({ driverId: d.id });
        }
        this.driverSaving = false;
        this.driverDialogVisible = false;
        this.catalogSaved.emit();
      },
      error: () => {
        this.driverSaving = false;
      },
    });
  }

  saveVehicleDialog(): void {
    const performerId = this.form.getRawValue().performerId as number | null;
    if (performerId == null || this.vehicleForm.invalid || this.vehicleSaving) {
      this.vehicleForm.markAllAsTouched();
      return;
    }
    const v = this.vehicleForm.getRawValue();
    const plate = v.plateNumber.replace(/\s+/g, '').toUpperCase();
    const body = {
      performerId,
      plateNumber: plate === '' ? null : plate,
      brandModel:
        (v.brandModel ?? '').trim() === '' ? null : (v.brandModel ?? '').trim(),
      type: (v.type ?? '').trim() === '' ? null : (v.type ?? '').trim(),
      isDefault: !!v.isDefault,
    };
    this.vehicleSaving = true;
    const obs =
      this.vehicleEditingId != null
        ? this.catalog.updateVehicle(this.vehicleEditingId, body)
        : this.catalog.createVehicle(body);
    obs.subscribe({
      next: (ve) => {
        if (this.vehicleEditingId == null) {
          this.form.patchValue({ vehicleId: ve.id });
        }
        this.vehicleSaving = false;
        this.vehicleDialogVisible = false;
        this.catalogSaved.emit();
      },
      error: () => {
        this.vehicleSaving = false;
      },
    });
  }
}
