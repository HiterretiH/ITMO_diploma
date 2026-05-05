import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { CatalogApiService } from '../../../core/catalog-api.service';
import { VehicleResponse } from '../../../core/catalog.models';
import { plateValidator } from '../../../shared/forms/plate.validator';

@Component({
  selector: 'app-vehicles',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TableModule,
    Button,
    Dialog,
    InputText,
    InputNumber,
  ],
  templateUrl: './vehicles.component.html',
  styleUrl: './vehicles.component.css',
})
export class VehiclesComponent implements OnInit {
  private readonly api = inject(CatalogApiService);
  private readonly fb = inject(FormBuilder);
  private readonly confirm = inject(ConfirmationService);

  items: VehicleResponse[] = [];
  dialogVisible = false;
  editingId: number | null = null;
  saving = false;

  readonly form = this.fb.group({
    plateNumber: this.fb.nonNullable.control('', {
      validators: [Validators.required, plateValidator],
    }),
    model: [''],
    loadCapacityKg: this.fb.control<number | null>(null, {
      validators: [Validators.min(0)],
    }),
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.vehicles().subscribe((v) => (this.items = v));
  }

  openCreate(): void {
    this.editingId = null;
    this.form.reset({
      plateNumber: '',
      model: '',
      loadCapacityKg: null,
    });
    this.dialogVisible = true;
  }

  openEdit(row: VehicleResponse): void {
    this.editingId = row.id;
    this.form.setValue({
      plateNumber: row.plateNumber,
      model: row.model ?? '',
      loadCapacityKg: row.loadCapacityKg,
    });
    this.dialogVisible = true;
  }

  save(): void {
    if (this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const plate = v.plateNumber.replace(/\s+/g, '').toUpperCase();
    const modelTrim = (v.model ?? '').trim();
    const body = {
      plateNumber: plate,
      model: modelTrim === '' ? null : modelTrim,
      loadCapacityKg:
        v.loadCapacityKg === null || v.loadCapacityKg === undefined
          ? null
          : Math.floor(Number(v.loadCapacityKg)),
    };
    this.saving = true;
    const done = {
      next: () => {
        this.saving = false;
        this.dialogVisible = false;
        this.reload();
      },
      error: () => {
        this.saving = false;
      },
    };
    if (this.editingId != null) {
      this.api.updateVehicle(this.editingId, body).subscribe(done);
    } else {
      this.api.createVehicle(body).subscribe(done);
    }
  }

  confirmDelete(row: VehicleResponse): void {
    this.confirm.confirm({
      message: `Удалить ТС «${row.plateNumber}»?`,
      header: 'Подтверждение',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () =>
        this.api.deleteVehicle(row.id).subscribe(() => this.reload()),
    });
  }

  dialogHeader(): string {
    return this.editingId != null ? 'Транспортное средство' : 'Новое ТС';
  }
}
