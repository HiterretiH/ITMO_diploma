import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output, inject } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { InputTextarea } from 'primeng/inputtextarea';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { CatalogApiService } from '../../core/catalog-api.service';
import { CustomerResponse } from '../../core/catalog.models';
import { CustomerCatalogSaveEvent } from './catalog-save.models';

@Component({
  selector: 'app-customer-catalog-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    Dialog,
    Button,
    InputText,
    InputTextarea,
    OverlayPanelModule,
  ],
  templateUrl: './customer-catalog-dialog.component.html',
  styleUrl: './catalog-dialog-form.css',
})
export class CustomerCatalogDialogComponent {
  private readonly api = inject(CatalogApiService);
  private readonly fb = inject(FormBuilder);

  @Output() readonly saved = new EventEmitter<CustomerCatalogSaveEvent>();

  visible = false;
  saving = false;
  private editingId: number | null = null;

  readonly form = this.fb.nonNullable.group({
    shortName: ['', [Validators.required, Validators.minLength(1)]],
    fullName: [''],
    phone: [''],
    requisites: [''],
  });

  dialogHeader(): string {
    return this.editingId != null ? 'Изменить заказчика' : 'Новый заказчик';
  }

  openCreate(): void {
    this.editingId = null;
    this.form.reset({
      shortName: '',
      fullName: '',
      phone: '',
      requisites: '',
    });
    this.visible = true;
  }

  openEdit(row: CustomerResponse): void {
    this.editingId = row.id;
    this.form.setValue({
      shortName: row.shortName,
      fullName: row.fullName ?? '',
      phone: row.phone ?? '',
      requisites: row.requisites ?? '',
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
    const body = {
      shortName: v.shortName.trim(),
      fullName: v.fullName.trim() === '' ? null : v.fullName.trim(),
      phone: v.phone.trim() === '' ? null : v.phone.trim(),
      requisites: v.requisites.trim() === '' ? null : v.requisites.trim(),
    };
    const wasCreate = this.editingId == null;
    this.saving = true;
    const obs =
      this.editingId != null
        ? this.api.updateCustomer(this.editingId, body)
        : this.api.createCustomer(body);
    obs.subscribe({
      next: (c) => {
        this.saving = false;
        this.visible = false;
        this.saved.emit({ entity: c, wasCreate });
      },
      error: () => {
        this.saving = false;
      },
    });
  }
}
