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
import { DropdownModule } from 'primeng/dropdown';
import { InputText } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { CatalogApiService } from '../../../core/catalog-api.service';
import { PerformerResponse, VehicleResponse } from '../../../core/catalog.models';
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
    DropdownModule,
  ],
  templateUrl: './vehicles.component.html',
  styleUrl: './vehicles.component.css',
})
export class VehiclesComponent implements OnInit {
  private readonly api = inject(CatalogApiService);
  private readonly fb = inject(FormBuilder);
  private readonly confirm = inject(ConfirmationService);

  items: VehicleResponse[] = [];
  performers: PerformerResponse[] = [];
  dialogVisible = false;
  editingId: number | null = null;
  saving = false;

  readonly form = this.fb.group({
    performerId: this.fb.control<number | null>(null, Validators.required),
    plateNumber: this.fb.nonNullable.control('', {
      validators: [Validators.required, plateValidator],
    }),
    brandModel: [''],
    type: [''],
    isDefault: [false],
  });

  ngOnInit(): void {
    this.reload();
    this.api.listPerformers().subscribe((p) => (this.performers = p));
  }

  reload(): void {
    this.api.listVehicles().subscribe((v) => (this.items = v));
  }

  performerLabel(id: number): string {
    return this.performers.find((p) => p.id === id)?.shortName ?? `#${id}`;
  }

  performerOptions(): { label: string; value: number | null }[] {
    return [
      { label: '—', value: null },
      ...this.performers.map((p) => ({ label: p.shortName, value: p.id })),
    ];
  }

  openCreate(): void {
    this.editingId = null;
    this.form.reset({
      performerId: null,
      plateNumber: '',
      brandModel: '',
      type: '',
      isDefault: false,
    });
    this.dialogVisible = true;
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
    this.dialogVisible = true;
  }

  save(): void {
    if (this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    if (v.performerId == null) {
      return;
    }
    const plate = v.plateNumber.replace(/\s+/g, '').toUpperCase();
    const body = {
      performerId: v.performerId,
      plateNumber: plate === '' ? null : plate,
      brandModel:
        (v.brandModel ?? '').trim() === '' ? null : (v.brandModel ?? '').trim(),
      type: (v.type ?? '').trim() === '' ? null : (v.type ?? '').trim(),
      isDefault: !!v.isDefault,
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
    return this.editingId != null ? 'Изменить ТС' : 'Новое ТС';
  }
}
