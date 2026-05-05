import { CommonModule } from '@angular/common';
import {
  Component,
  Input,
  OnDestroy,
  OnInit,
  inject,
} from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Subscription } from 'rxjs';
import {
  AutoComplete,
  AutoCompleteCompleteEvent,
  AutoCompleteSelectEvent,
} from 'primeng/autocomplete';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputText } from 'primeng/inputtext';
import { CatalogApiService } from '../../core/catalog-api.service';
import { PlaceResponse, PlaceType } from '../../core/catalog.models';

@Component({
  selector: 'app-place-input',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    AutoComplete,
    InputText,
    Button,
    Dialog,
    DropdownModule,
  ],
  templateUrl: './place-input.component.html',
  styleUrl: './place-input.component.css',
})
export class PlaceInputComponent implements OnInit, OnDestroy {
  private readonly catalog = inject(CatalogApiService);
  private readonly fb = inject(FormBuilder);

  /** Родительская форма (те же контролы, что и у trip-new / trip-edit). */
  @Input({ required: true }) form!: FormGroup;
  @Input({ required: true }) addressKey!: string;
  @Input({ required: true }) contactKey!: string;
  /** Фильтр каталога: LOAD или UNLOAD (BOTH подмешивается на бэкенде). */
  @Input({ required: true }) placeType!: 'LOAD' | 'UNLOAD';

  @Input() addressLabel = 'Адрес или объект';
  @Input() contactLabel = 'Контакт';
  @Input() inputId = 'placeAddr';
  @Input() contactInputId = 'placeCt';

  suggestions: PlaceResponse[] = [];
  lastSelectedPlace: PlaceResponse | null = null;

  saveDialogVisible = false;
  saveSaving = false;
  readonly saveTypeOptions: { label: string; value: PlaceType }[] = [
    { label: 'Погрузка', value: 'LOAD' },
    { label: 'Разгрузка', value: 'UNLOAD' },
    { label: 'Погрузка и разгрузка', value: 'BOTH' },
  ];

  private sub?: Subscription;

  /** Мини-форма «сохранить как место». */
  readonly saveDialogForm = this.fb.nonNullable.group({
    address: ['', [Validators.required, Validators.minLength(1)]],
    contact: [''],
    placeType: this.fb.nonNullable.control<PlaceType>('LOAD', Validators.required),
  });

  ngOnInit(): void {
    this.saveDialogForm.patchValue({ placeType: this.placeType });
    this.sub = this.addressControl.valueChanges.subscribe(() => {
      if (
        this.lastSelectedPlace &&
        (this.addressControl.value ?? '').trim() !==
          this.lastSelectedPlace.address.trim()
      ) {
        this.lastSelectedPlace = null;
      }
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  get addressControl(): FormControl<string> {
    return this.form.get(this.addressKey) as FormControl<string>;
  }

  get contactControl(): FormControl<string> {
    return this.form.get(this.contactKey) as FormControl<string>;
  }

  get showSaveAsPlace(): boolean {
    const addr = (this.addressControl.value ?? '').trim();
    if (!addr) {
      return false;
    }
    if (
      this.lastSelectedPlace &&
      this.lastSelectedPlace.address.trim() === addr
    ) {
      return false;
    }
    return true;
  }

  complete(ev: AutoCompleteCompleteEvent): void {
    const q = (ev.query ?? '').trim();
    this.catalog.places(this.placeType, q || undefined).subscribe({
      next: (list) => (this.suggestions = list),
      error: () => (this.suggestions = []),
    });
  }

  onSelect(ev: AutoCompleteSelectEvent): void {
    const v = ev.value;
    let p: PlaceResponse | undefined;
    if (v && typeof v === 'object' && 'id' in (v as object)) {
      p = v as PlaceResponse;
    } else {
      const addr = String(v ?? '').trim();
      p = this.suggestions.find((x) => x.address.trim() === addr);
    }
    if (!p) {
      return;
    }
    this.lastSelectedPlace = p;
    this.contactControl.patchValue(p.contact ?? '', { emitEvent: true });
  }

  openSaveDialog(): void {
    const addr = (this.addressControl.value ?? '').trim();
    const ct = (this.contactControl.value ?? '').trim();
    this.saveDialogForm.setValue({
      address: addr,
      contact: ct,
      placeType: this.placeType,
    });
    this.saveDialogVisible = true;
  }

  saveNewPlace(): void {
    const v = this.saveDialogForm.getRawValue();
    const address = v.address.trim();
    if (!address || this.saveSaving) {
      return;
    }
    this.saveSaving = true;
    this.catalog
      .createPlace({
        address,
        contact: v.contact.trim() === '' ? null : v.contact.trim(),
        placeType: v.placeType,
      })
      .subscribe({
        next: (created) => {
          this.saveSaving = false;
          this.saveDialogVisible = false;
          this.lastSelectedPlace = created;
          this.addressControl.patchValue(created.address, { emitEvent: true });
          this.contactControl.patchValue(created.contact ?? '', {
            emitEvent: true,
          });
        },
        error: () => {
          this.saveSaving = false;
        },
      });
  }
}
