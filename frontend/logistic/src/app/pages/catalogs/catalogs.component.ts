import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CatalogApiService } from '../../core/catalog-api.service';
import {
  CounterpartyResponse,
  DriverResponse,
  VehicleResponse,
} from '../../core/catalog.models';

@Component({
  selector: 'app-catalogs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './catalogs.component.html',
  styleUrl: './catalogs.component.css',
})
export class CatalogsComponent implements OnInit {
  private readonly api = inject(CatalogApiService);

  counterparties: CounterpartyResponse[] = [];
  drivers: DriverResponse[] = [];
  vehicles: VehicleResponse[] = [];

  cpName = '';
  cpInn = '';
  cpAddr = '';
  cpPhone = '';

  drName = '';
  drLic = '';
  drCat = '';

  vPlate = '';
  vModel = '';
  vCap: number | null = null;

  error: string | null = null;

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.error = null;
    this.api.counterparties().subscribe({
      next: (v) => (this.counterparties = v),
      error: () => (this.error = 'Не удалось загрузить контрагентов'),
    });
    this.api.drivers().subscribe({
      next: (v) => (this.drivers = v),
      error: () => (this.error = 'Не удалось загрузить водителей'),
    });
    this.api.vehicles().subscribe({
      next: (v) => (this.vehicles = v),
      error: () => (this.error = 'Не удалось загрузить транспорт'),
    });
  }

  addCp(): void {
    this.api
      .createCounterparty({
        name: this.cpName,
        inn: this.cpInn || null,
        legalAddress: this.cpAddr || null,
        phone: this.cpPhone || null,
      })
      .subscribe({
        next: () => {
          this.cpName = '';
          this.cpInn = '';
          this.cpAddr = '';
          this.cpPhone = '';
          this.reload();
        },
        error: () => (this.error = 'Не удалось создать контрагента'),
      });
  }

  addDr(): void {
    this.api
      .createDriver({
        fullName: this.drName,
        licenseNumber: this.drLic,
        licenseCategory: this.drCat || null,
      })
      .subscribe({
        next: () => {
          this.drName = '';
          this.drLic = '';
          this.drCat = '';
          this.reload();
        },
        error: () => (this.error = 'Не удалось создать водителя'),
      });
  }

  addV(): void {
    this.api
      .createVehicle({
        plateNumber: this.vPlate,
        model: this.vModel || null,
        loadCapacityKg: this.vCap,
      })
      .subscribe({
        next: () => {
          this.vPlate = '';
          this.vModel = '';
          this.vCap = null;
          this.reload();
        },
        error: () => (this.error = 'Не удалось создать ТС'),
      });
  }
}
