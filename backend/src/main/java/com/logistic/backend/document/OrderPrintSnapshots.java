package com.logistic.backend.document;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Shared {@link OrderPrintSnapshot} instances for tests, {@link DocumentFixtureGenerator}, and timing reports.
 *
 * <p>{@link #manualReviewDemo()} matches the trip-form demo constants in the Angular app
 * ({@code orderPrintDemoTripFormPatch} in {@code order-print-demo.fixture.ts}): same date, order number, route,
 * contacts, single leg, and money fields.
 */
public final class OrderPrintSnapshots {

    private OrderPrintSnapshots() {}

    /** Полный блок реквизитов заказчика (ООО) для DOCX/PDF и ручной проверки верстки. */
    private static final String DEMO_CUSTOMER_REQUISITES =
            """
            ООО «Ромашка» (полное наименование: ООО Ромашка полное наименование)
            ИНН 7701234567, КПП 770101001, ОГРН 1197746123456
            Юр. адрес: 125047, г. Москва, ул. Лесная, д. 5, пом. 12Н
            Почтовый адрес: 125047, г. Москва, ул. Лесная, д. 5, пом. 12Н
            Р/с 40702810938000012345 в ПАО Сбербанк, БИК 044525225, к/с 30101810400000000225
            Тел.: +78120000000, e-mail: buh@romashka.example"""
                    .stripIndent()
                    .strip();

    /**
     * Полный блок реквизитов исполнителя (ИП); банковские реквизиты совпадают с отдельными полями снимка
     * (performerBankName, performerInn, …).
     */
    private static final String DEMO_PERFORMER_REQUISITES =
            """
            ИП Петров Петр Петрович
            ИНН 000000000000, ОГРНИП 320774600456789
            Адрес регистрации: 141080, Московская обл., г. Королёв, ул. Калинина, д. 10, кв. 5
            Банк: АО Банк
            БИК 044525225, р/с 40702810000000000001, к/с 30101810400000000225
            КПП 770101001
            Тел.: +79001112233"""
                    .stripIndent()
                    .strip();

    public static OrderPrintSnapshot manualReviewDemo() {
        return new OrderPrintSnapshot(
                1001L,
                42,
                LocalDate.of(2026, 5, 15),
                "ООО Ромашка",
                "ООО Ромашка полное наименование",
                "+78120000000",
                DEMO_CUSTOMER_REQUISITES,
                "ИП Петров",
                "ИП Петров Петр Петрович",
                "+79001112233",
                "АО Банк",
                "000000000000",
                "044525225",
                "770101001",
                "40702810000000000001",
                "30101810400000000225",
                DEMO_PERFORMER_REQUISITES,
                "Volvo FH",
                "А123ВС178",
                "тягач с прицепом",
                "Иванов Иван Иванович",
                "+79002223344",
                "Москва",
                "Контакт погрузки +7 900 123 45 67",
                "Санкт-Петербург",
                "Контакт разгрузки +7 900 765 43 21",
                1,
                new BigDecimal("98500.00"),
                new BigDecimal("98500.00"));
    }
}
