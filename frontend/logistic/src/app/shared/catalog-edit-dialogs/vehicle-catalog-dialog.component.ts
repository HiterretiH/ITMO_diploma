import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Button } from 'primeng/button';
import { Checkbox } from 'primeng/checkbox';
import { Dialog } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputText } from 'primeng/inputtext';
import { CatalogApiService } from '../../core/catalog-api.service';
import { PerformerResponse, VehicleResponse } from '../../core/catalog.models';
import { plateValidator } from '../forms/plate.validator';
import { VehicleCatalogSaveEvent } from './catalog-save.models';

@Component({
  selector: 'app-vehicle-catalog-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    Dialog,
    Button,
    Checkbox,
    InputText,
    DropdownModule,
  ],
  templateUrl: './vehicle-catalog-dialog.component.html',
  styleUrl: './catalog-dialog-form.css',
})
export class VehicleCatalogDialogComponent {
  private readonly api = inject(CatalogApiService);
  private readonly fb = inject(FormBuilder);

  @Input() performers: PerformerResponse[] = [];
  @Input() performerLockedId: number | null = null;

  @Output() readonly saved = new EventEmitter<VehicleCatalogSaveEvent>();

  visible = false;
  saving = false;
  editingId: number | null = null;

  readonly form = this.fb.group({
    performerId: this.fb.control<number | null>(null, Validators.required),
    plateNumber: this.fb.nonNullable.control('', {
      validators: [Validators.required, plateValidator],
    }),
    brandModel: [''],
    type: [''],
    isDefault: [false],
  });

  performerOptions(): { label: string; value: number | null }[] {
    return [
      { label: '—', value: null },
      ...this.performers.map((p) => ({ label: p.shortName, value: p.id })),
    ];
  }

  dialogHeader(): string {
    return this.editingId != null ? 'Изменить ТС' : 'Новое ТС';
  }

  openCreate(): void {
    this.editingId = null;
    const lock = this.performerLockedId;
    const pid = lock ?? null;

    const finish = (defaultChecked: boolean): void => {
      this.form.reset({
        performerId: pid,
        plateNumber: '',
        brandModel: '',
        type: '',
        isDefault: defaultChecked,
      });
      this.visible = true;
    };

    if (pid != null) {
      this.api.listVehicles().subscribe({
        next: (vehicles) => {
          const n = vehicles.filter((v) => v.performerId === pid).length;
          finish(n === 0);
        },
        error: () => finish(false),
      });
    } else {
      finish(false);
    }
  }

  openEdit(row: VehicleResponse): void {
    this.editingId = row.id;
    this.form.setValue({
      performerId: row.performerId,
      plateNumber: row.plateNumber ?? '',
      brandModel: row.brandModel ?? '',
      type: row.type ?? '',
      isDefault: row.isDefault,
    });
    this.visible = true;
  }

  close(): void {
    this.visible = false;
  }

  save(): void {
    if (this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    let performerId = v.performerId;
    if (this.performerLockedId != null) {
      performerId = this.performerLockedId;
    }
    if (performerId == null) {
      return;
    }
    const plate = v.plateNumber.replace(/\s+/g, '').toUpperCase();
    const body = {
      performerId,
      plateNumber: plate === '' ? null : plate,
      brandModel:
        (v.brandModel ?? '').trim() === '' ? null : (v.brandModel ?? '').trim(),
      type: (v.type ?? '').trim() === '' ? null : (v.type ?? '').trim(),
      isDefault: !!v.isDefault,
    };
    const wasCreate = this.editingId == null;
    this.saving = true;
    const obs =
      this.editingId != null
        ? this.api.updateVehicle(this.editingId, body)
        : this.api.createVehicle(body);
    obs.subscribe({
      next: (ve) => {
        this.saving = false;
        this.visible = false;
        this.saved.emit({ entity: ve, wasCreate });
      },
      error: () => {
        this.saving = false;
      },
    });
  }
}
