package com.logistic.backend.document;

public enum DocumentType {
    CONTRACT_APPLICATION("Заявка на перевозку", "Заявка"),
    WAYBILL("Товарно-транспортная накладная", "ТТН"),
    ACT_OF_WORK("Акт выполненных работ", "Акт");

    private final String uiLabelRu;
    private final String fileStemRu;

    DocumentType(String uiLabelRu, String fileStemRu) {
        this.uiLabelRu = uiLabelRu;
        this.fileStemRu = fileStemRu;
    }

    /** Full label for UI tables / ZIP listings */
    public String uiLabelRu() {
        return uiLabelRu;
    }

    /** Short token used inside generated filenames */
    public String fileStemRu() {
        return fileStemRu;
    }
}
