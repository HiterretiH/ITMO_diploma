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

    public static OrderPrintSnapshot manualReviewDemo() {
        return new OrderPrintSnapshot(
                1001L,
                42,
                LocalDate.of(2026, 5, 15),
                "ООО Ромашка",
                "ООО Ромашка полное наименование",
                "+78120000000",
                "г. Москва, ул. Ленина, 1",
                "ИП Петров",
                "ИП Петров Петр Петрович",
                "+79001112233",
                "АО Банк",
                "000000000000",
                "044525225",
                "770101001",
                "40702810000000000001",
                "30101810400000000225",
                "г. Москва, ул. Ленина, 1",
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
