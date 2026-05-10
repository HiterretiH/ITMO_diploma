import { DocumentTypeName } from '../core/order.models';

/** Matches backend DocumentType.uiLabelRu */
export function documentTypeLabelRu(type: DocumentTypeName): string {
  switch (type) {
    case 'CONTRACT_APPLICATION':
      return 'Заявка на перевозку';
    case 'WAYBILL':
      return 'Товарно-транспортная накладная';
    case 'ACT_OF_WORK':
      return 'Акт выполненных работ';
    default:
      return type;
  }
}
