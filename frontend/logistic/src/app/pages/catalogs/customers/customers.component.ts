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
import { CustomerResponse } from '../../../core/catalog.models';

@Component({
  selector: 'app-customers',
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
  templateUrl: './customers.component.html',
  styleUrl: './customers.component.css',
})
export class CustomersComponent implements OnInit {
  private readonly api = inject(CatalogApiService);
  private readonly fb = inject(FormBuilder);
  private readonly confirm = inject(ConfirmationService);

  items: CustomerResponse[] = [];
  dialogVisible = false;
  editingId: number | null = null;
  saving = false;

  readonly form = this.fb.nonNullable.group({
    shortName: ['', [Validators.required, Validators.minLength(1)]],
    fullName: [''],
    phone: [''],
    requisites: [''],
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.listCustomers().subscribe((v) => (this.items = v));
  }

  openCreate(): void {
    this.editingId = null;
    this.form.reset({
      shortName: '',
      fullName: '',
      phone: '',
      requisites: '',
    });
    this.dialogVisible = true;
  }

  openEdit(row: CustomerResponse): void {
    this.editingId = row.id;
    this.form.setValue({
      shortName: row.shortName,
      fullName: row.fullName ?? '',
      phone: row.phone ?? '',
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
    const body = {
      shortName: v.shortName.trim(),
      fullName: v.fullName.trim() === '' ? null : v.fullName.trim(),
      phone: v.phone.trim() === '' ? null : v.phone.trim(),
      requisites: v.requisites.trim() === '' ? null : v.requisites.trim(),
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
      this.api.updateCustomer(this.editingId, body).subscribe(done);
    } else {
      this.api.createCustomer(body).subscribe(done);
    }
  }

  confirmDelete(row: CustomerResponse): void {
    this.confirm.confirm({
      message: `Удалить заказчика «${row.shortName}»?`,
      header: 'Подтверждение',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () =>
        this.api.deleteCustomer(row.id).subscribe(() => this.reload()),
    });
  }

  dialogHeader(): string {
    return this.editingId != null ? 'Изменить заказчика' : 'Новый заказчик';
  }
}
