import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { CatalogApiService } from '../../../core/catalog-api.service';
import { PerformerResponse } from '../../../core/catalog.models';
import { PerformerCatalogDialogComponent } from '../../../shared/catalog-edit-dialogs/performer-catalog-dialog.component';

@Component({
  selector: 'app-performers',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    Button,
    PerformerCatalogDialogComponent,
  ],
  templateUrl: './performers.component.html',
  styleUrl: './performers.component.css',
})
export class PerformersComponent implements OnInit {
  private readonly api = inject(CatalogApiService);
  private readonly confirm = inject(ConfirmationService);

  items: PerformerResponse[] = [];

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.listPerformers().subscribe((v) => (this.items = v));
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
}
