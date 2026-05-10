import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { CatalogApiService } from '../../../core/catalog-api.service';
import { CustomerResponse } from '../../../core/catalog.models';
import { CustomerCatalogDialogComponent } from '../../../shared/catalog-edit-dialogs/customer-catalog-dialog.component';

@Component({
  selector: 'app-customers',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    Button,
    CustomerCatalogDialogComponent,
  ],
  templateUrl: './customers.component.html',
  styleUrl: './customers.component.css',
})
export class CustomersComponent implements OnInit {
  private readonly api = inject(CatalogApiService);
  private readonly confirm = inject(ConfirmationService);

  items: CustomerResponse[] = [];

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.listCustomers().subscribe((v) => (this.items = v));
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
}
