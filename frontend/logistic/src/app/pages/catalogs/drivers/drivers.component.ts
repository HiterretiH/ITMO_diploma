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
import { InputText } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { CatalogApiService } from '../../../core/catalog-api.service';
import { DriverResponse } from '../../../core/catalog.models';

@Component({
  selector: 'app-drivers',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TableModule,
    Button,
    Dialog,
    InputText,
  ],
  templateUrl: './drivers.component.html',
  styleUrl: './drivers.component.css',
})
export class DriversComponent implements OnInit {
  private readonly api = inject(CatalogApiService);
  private readonly fb = inject(FormBuilder);
  private readonly confirm = inject(ConfirmationService);

  items: DriverResponse[] = [];
  dialogVisible = false;
  editingId: number | null = null;
  saving = false;

  readonly form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(1)]],
    licenseNumber: ['', [Validators.required, Validators.minLength(1)]],
    licenseCategory: [''],
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.drivers().subscribe((v) => (this.items = v));
  }

  openCreate(): void {
    this.editingId = null;
    this.form.reset({
      fullName: '',
      licenseNumber: '',
      licenseCategory: '',
    });
    this.dialogVisible = true;
  }

  openEdit(row: DriverResponse): void {
    this.editingId = row.id;
    this.form.setValue({
      fullName: row.fullName,
      licenseNumber: row.licenseNumber,
      licenseCategory: row.licenseCategory ?? '',
    });
    this.dialogVisible = true;
  }

  save(): void {
    if (this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const body = {
      fullName: v.fullName.trim(),
      licenseNumber: v.licenseNumber.trim(),
      licenseCategory:
        v.licenseCategory.trim() === '' ? null : v.licenseCategory.trim(),
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
      this.api.updateDriver(this.editingId, body).subscribe(done);
    } else {
      this.api.createDriver(body).subscribe(done);
    }
  }

  confirmDelete(row: DriverResponse): void {
    this.confirm.confirm({
      message: `Удалить водителя «${row.fullName}»?`,
      header: 'Подтверждение',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.api.deleteDriver(row.id).subscribe(() => this.reload()),
    });
  }

  dialogHeader(): string {
    return this.editingId != null ? 'Водитель' : 'Новый водитель';
  }
}
