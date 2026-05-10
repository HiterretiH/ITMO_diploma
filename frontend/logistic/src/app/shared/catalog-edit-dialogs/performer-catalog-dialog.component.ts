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
import { CatalogApiService } from '../../core/catalog-api.service';
import { PerformerResponse } from '../../core/catalog.models';
import { innValidator } from '../forms/inn.validator';
import { PerformerCatalogSaveEvent } from './catalog-save.models';

@Component({
  selector: 'app-performer-catalog-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    Dialog,
    Button,
    InputText,
    InputTextarea,
  ],
  templateUrl: './performer-catalog-dialog.component.html',
  styleUrl: './catalog-dialog-form.css',
})
export class PerformerCatalogDialogComponent {
  private readonly api = inject(CatalogApiService);
  private readonly fb = inject(FormBuilder);

  @Output() readonly saved = new EventEmitter<PerformerCatalogSaveEvent>();

  visible = false;
  saving = false;
  private editingId: number | null = null;

  readonly form = this.fb.nonNullable.group({
    shortName: ['', [Validators.required, Validators.minLength(1)]],
    fullName: [''],
    phone: [''],
    inn: ['', [innValidator]],
    bankName: [''],
    bik: [''],
    kpp: [''],
    paymentAccount: [''],
    corrAccount: [''],
    requisites: [''],
  });

  dialogHeader(): string {
    return this.editingId != null ? 'Изменить исполнителя' : 'Новый исполнитель';
  }

  openCreate(): void {
    this.editingId = null;
    this.form.reset({
      shortName: '',
      fullName: '',
      phone: '',
      inn: '',
      bankName: '',
      bik: '',
      kpp: '',
      paymentAccount: '',
      corrAccount: '',
      requisites: '',
    });
    this.visible = true;
  }

  openEdit(row: PerformerResponse): void {
    this.editingId = row.id;
    this.form.setValue({
      shortName: row.shortName,
      fullName: row.fullName ?? '',
      phone: row.phone ?? '',
      inn: row.inn ?? '',
      bankName: row.bankName ?? '',
      bik: row.bik ?? '',
      kpp: row.kpp ?? '',
      paymentAccount: row.paymentAccount ?? '',
      corrAccount: row.corrAccount ?? '',
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
    const emptyToNull = (s: string) => (s.trim() === '' ? null : s.trim());
    const body = {
      shortName: v.shortName.trim(),
      fullName: emptyToNull(v.fullName),
      phone: emptyToNull(v.phone),
      inn: emptyToNull(v.inn),
      bankName: emptyToNull(v.bankName),
      bik: emptyToNull(v.bik),
      kpp: emptyToNull(v.kpp),
      paymentAccount: emptyToNull(v.paymentAccount),
      corrAccount: emptyToNull(v.corrAccount),
      requisites: emptyToNull(v.requisites),
    };
    const wasCreate = this.editingId == null;
    this.saving = true;
    const obs =
      this.editingId != null
        ? this.api.updatePerformer(this.editingId, body)
        : this.api.createPerformer(body);
    obs.subscribe({
      next: (p) => {
        this.saving = false;
        this.visible = false;
        this.saved.emit({ entity: p, wasCreate });
      },
      error: () => {
        this.saving = false;
      },
    });
  }
}
