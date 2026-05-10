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
import { DriverResponse, PerformerResponse } from '../../core/catalog.models';
import { DriverCatalogSaveEvent } from './catalog-save.models';

/**
 * @param performerLockedId если задан (например из формы заказа), выпадающий список исполнителя скрыт.
 */
@Component({
  selector: 'app-driver-catalog-dialog',
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
  templateUrl: './driver-catalog-dialog.component.html',
  styleUrl: './catalog-dialog-form.css',
})
export class DriverCatalogDialogComponent {
  private readonly api = inject(CatalogApiService);
  private readonly fb = inject(FormBuilder);

  /** Справочник исполнителей для выпадающего списка (страница каталога). */
  @Input() performers: PerformerResponse[] = [];
  /** Фиксированный исполнитель (форма заказа); при задании список не показывается. */
  @Input() performerLockedId: number | null = null;

  @Output() readonly saved = new EventEmitter<DriverCatalogSaveEvent>();

  visible = false;
  saving = false;
  /** null — режим создания (доступно в шаблоне для подписи кнопки). */
  editingId: number | null = null;

  readonly form = this.fb.nonNullable.group({
    performerId: this.fb.control<number | null>(null, Validators.required),
    fullName: ['', [Validators.required, Validators.minLength(1)]],
    phone: [''],
    isDefault: [false],
  });

  performerOptions(): { label: string; value: number | null }[] {
    return [
      { label: '—', value: null },
      ...this.performers.map((p) => ({ label: p.shortName, value: p.id })),
    ];
  }

  dialogHeader(): string {
    return this.editingId != null ? 'Изменить водителя' : 'Новый водитель';
  }

  openCreate(): void {
    this.editingId = null;
    const lock = this.performerLockedId;
    const pid = lock ?? null;

    const finish = (defaultChecked: boolean): void => {
      this.form.reset({
        performerId: pid,
        fullName: '',
        phone: '',
        isDefault: defaultChecked,
      });
      this.visible = true;
    };

    if (pid != null) {
      this.api.listDrivers().subscribe({
        next: (drivers) => {
          const n = drivers.filter((d) => d.performerId === pid).length;
          finish(n === 0);
        },
        error: () => finish(false),
      });
    } else {
      finish(false);
    }
  }

  openEdit(row: DriverResponse): void {
    this.editingId = row.id;
    this.form.setValue({
      performerId: row.performerId,
      fullName: row.fullName,
      phone: row.phone ?? '',
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
    const body = {
      performerId,
      fullName: v.fullName.trim(),
      phone: v.phone.trim() === '' ? null : v.phone.trim(),
      isDefault: v.isDefault,
    };
    const wasCreate = this.editingId == null;
    this.saving = true;
    const obs =
      this.editingId != null
        ? this.api.updateDriver(this.editingId, body)
        : this.api.createDriver(body);
    obs.subscribe({
      next: (d) => {
        this.saving = false;
        this.visible = false;
        this.saved.emit({ entity: d, wasCreate });
      },
      error: () => {
        this.saving = false;
      },
    });
  }
}
