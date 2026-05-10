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
import { InputTextarea } from 'primeng/inputtextarea';
import { TableModule } from 'primeng/table';
import { CatalogApiService } from '../../../core/catalog-api.service';
import { PerformerResponse } from '../../../core/catalog.models';
import { innValidator } from '../../../shared/forms/inn.validator';

@Component({
  selector: 'app-performers',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TableModule,
    Button,
    Dialog,
    InputText,
    InputTextarea,
  ],
  templateUrl: './performers.component.html',
  styleUrl: './performers.component.css',
})
export class PerformersComponent implements OnInit {
  private readonly api = inject(CatalogApiService);
  private readonly fb = inject(FormBuilder);
  private readonly confirm = inject(ConfirmationService);

  items: PerformerResponse[] = [];
  dialogVisible = false;
  editingId: number | null = null;
  saving = false;

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

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.listPerformers().subscribe((v) => (this.items = v));
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
    this.dialogVisible = true;
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
    this.dialogVisible = true;
  }

  save(): void {
    if (this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const emptyToNull = (s: string) =>
      s.trim() === '' ? null : s.trim();
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
      this.api.updatePerformer(this.editingId, body).subscribe(done);
    } else {
      this.api.createPerformer(body).subscribe(done);
    }
  }

  confirmDelete(row: PerformerResponse): void {
    this.confirm.confirm({
      message: `Удалить исполнителя «${row.shortName}»?`,
      header: 'Подтверждение',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () =>
        this.api.deletePerformer(row.id).subscribe(() => this.reload()),
    });
  }

  dialogHeader(): string {
    return this.editingId != null ? 'Изменить исполнителя' : 'Новый исполнитель';
  }
}
