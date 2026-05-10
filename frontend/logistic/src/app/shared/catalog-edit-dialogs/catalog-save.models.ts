import {
  CustomerResponse,
  DriverResponse,
  PerformerResponse,
  VehicleResponse,
} from '../../core/catalog.models';

export type CustomerCatalogSaveEvent = {
  entity: CustomerResponse;
  wasCreate: boolean;
};

export type PerformerCatalogSaveEvent = {
  entity: PerformerResponse;
  wasCreate: boolean;
};

export type DriverCatalogSaveEvent = {
  entity: DriverResponse;
  wasCreate: boolean;
};

export type VehicleCatalogSaveEvent = {
  entity: VehicleResponse;
  wasCreate: boolean;
};
