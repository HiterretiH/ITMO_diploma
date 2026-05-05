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
import { CounterpartyResponse } from '../../../core/catalog.models';
import { innValidator } from '../../../shared/forms/inn.validator';

@Component({
  selector: 'app-counterparties',
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
  templateUrl: './counterparties.component.html',
  styleUrl: './counterparties.component.css',
})
export class CounterpartiesComponent implements OnInit {
  private readonly api = inject(CatalogApiService);
  private readonly fb = inject(FormBuilder);
  private readonly confirm = inject(ConfirmationService);

  items: CounterpartyResponse[] = [];
  dialogVisible = false;
  editingId: number | null = null;
  saving = false;

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(1)]],
    inn: ['', [innValidator]],
    legalAddress: [''],
    phone: [''],
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.counterparties().subscribe((v) => (this.items = v));
  }

  openCreate(): void {
    this.editingId = null;
    this.form.reset({
      name: '',
      inn: '',
      legalAddress: '',
      phone: '',
    });
    this.dialogVisible = true;
  }

  openEdit(row: CounterpartyResponse): void {
    this.editingId = row.id;
    this.form.setValue({
      name: row.name,
      inn: row.inn ?? '',
      legalAddress: row.legalAddress ?? '',
      phone: row.phone ?? '',
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
      name: v.name.trim(),
      inn: v.inn.trim() === '' ? null : v.inn.trim(),
      legalAddress: v.legalAddress.trim() === '' ? null : v.legalAddress.trim(),
      phone: v.phone.trim() === '' ? null : v.phone.trim(),
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
      this.api.updateCounterparty(this.editingId, body).subscribe(done);
    } else {
      this.api.createCounterparty(body).subscribe(done);
    }
  }

  confirmDelete(row: CounterpartyResponse): void {
    this.confirm.confirm({
      message: `Удалить контрагента «${row.name}»?`,
      header: 'Подтверждение',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () =>
        this.api.deleteCounterparty(row.id).subscribe(() => this.reload()),
    });
  }

  dialogHeader(): string {
    return this.editingId != null ? 'Контрагент' : 'Новый контрагент';
  }
}
