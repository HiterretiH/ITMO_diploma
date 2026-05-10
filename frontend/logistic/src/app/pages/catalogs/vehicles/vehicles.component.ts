import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { CatalogApiService } from '../../../core/catalog-api.service';
import { PerformerResponse, VehicleResponse } from '../../../core/catalog.models';
import { VehicleCatalogDialogComponent } from '../../../shared/catalog-edit-dialogs/vehicle-catalog-dialog.component';

@Component({
  selector: 'app-vehicles',
  standalone: true,
  imports: [CommonModule, TableModule, Button, VehicleCatalogDialogComponent],
  templateUrl: './vehicles.component.html',
  styleUrl: './vehicles.component.css',
})
export class VehiclesComponent implements OnInit {
  private readonly api = inject(CatalogApiService);
  private readonly confirm = inject(ConfirmationService);

  items: VehicleResponse[] = [];
  performers: PerformerResponse[] = [];

  ngOnInit(): void {
    this.reload();
    this.api.listPerformers().subscribe((p) => (this.performers = p));
  }

  reload(): void {
    this.api.listVehicles().subscribe((v) => (this.items = v));
  }

  performerLabel(id: number): string {
    return this.performers.find((p) => p.id === id)?.shortName ?? `#${id}`;
  }

  confirmDelete(row: VehicleResponse): void {
    this.confirm.confirm({
      message: `Удалить ТС «${row.plateNumber}»?`,
      header: 'Подтверждение',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () =>
        this.api.deleteVehicle(row.id).subscribe(() => this.reload()),
    });
  }
}
