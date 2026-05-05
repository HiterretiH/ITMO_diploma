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
import { PlaceResponse, PlaceType } from '../../../core/catalog.models';

@Component({
  selector: 'app-places',
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
  templateUrl: './places.component.html',
  styleUrl: './places.component.css',
})
export class PlacesComponent implements OnInit {
  private readonly api = inject(CatalogApiService);
  private readonly fb = inject(FormBuilder);
  private readonly confirm = inject(ConfirmationService);

  items: PlaceResponse[] = [];
  dialogVisible = false;
  editingId: number | null = null;
  saving = false;

  readonly typeOptions: { label: string; value: PlaceType }[] = [
    { label: 'Погрузка', value: 'LOAD' },
    { label: 'Разгрузка', value: 'UNLOAD' },
    { label: 'Погрузка и разгрузка', value: 'BOTH' },
  ];

  readonly form = this.fb.nonNullable.group({
    address: ['', [Validators.required, Validators.minLength(1)]],
    contact: [''],
    placeType: this.fb.nonNullable.control<PlaceType>('LOAD', Validators.required),
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.places().subscribe((v) => (this.items = v));
  }

  typeLabel(t: PlaceType): string {
    switch (t) {
      case 'LOAD':
        return 'Погрузка';
      case 'UNLOAD':
        return 'Разгрузка';
      default:
        return 'Оба';
    }
  }

  openCreate(): void {
    this.editingId = null;
    this.form.reset({
      address: '',
      contact: '',
      placeType: 'LOAD',
    });
    this.dialogVisible = true;
  }

  openEdit(row: PlaceResponse): void {
    this.editingId = row.id;
    this.form.setValue({
      address: row.address,
      contact: row.contact ?? '',
      placeType: row.placeType,
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
      address: v.address.trim(),
      contact: v.contact.trim() === '' ? null : v.contact.trim(),
      placeType: v.placeType,
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
      this.api.updatePlace(this.editingId, body).subscribe(done);
    } else {
      this.api.createPlace(body).subscribe(done);
    }
  }

  confirmDelete(row: PlaceResponse): void {
    this.confirm.confirm({
      message: `Удалить место «${row.address}»?`,
      header: 'Подтверждение',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.api.deletePlace(row.id).subscribe(() => this.reload()),
    });
  }

  dialogHeader(): string {
    return this.editingId != null ? 'Изменить место' : 'Новое место';
  }
}
