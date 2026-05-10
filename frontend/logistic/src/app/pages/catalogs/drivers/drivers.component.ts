import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { CatalogApiService } from '../../../core/catalog-api.service';
import { DriverResponse, PerformerResponse } from '../../../core/catalog.models';
import { DriverCatalogDialogComponent } from '../../../shared/catalog-edit-dialogs/driver-catalog-dialog.component';

@Component({
  selector: 'app-drivers',
  standalone: true,
  imports: [CommonModule, TableModule, Button, DriverCatalogDialogComponent],
  templateUrl: './drivers.component.html',
  styleUrl: './drivers.component.css',
})
export class DriversComponent implements OnInit {
  private readonly api = inject(CatalogApiService);
  private readonly confirm = inject(ConfirmationService);

  items: DriverResponse[] = [];
  performers: PerformerResponse[] = [];

  ngOnInit(): void {
    this.reload();
    this.api.listPerformers().subscribe((p) => (this.performers = p));
  }

  reload(): void {
    this.api.listDrivers().subscribe((v) => (this.items = v));
  }

  performerLabel(id: number): string {
    return this.performers.find((p) => p.id === id)?.shortName ?? `#${id}`;
  }

  confirmDelete(row: DriverResponse): void {
    this.confirm.confirm({
      message: `Удалить водителя «${row.fullName}»?`,
      header: 'Подтверждение',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () =>
        this.api.deleteDriver(row.id).subscribe(() => this.reload()),
    });
  }
}
