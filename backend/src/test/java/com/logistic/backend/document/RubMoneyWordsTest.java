package com.logistic.backend.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RubMoneyWordsTest {

    @Test
    void formatsMoneyLikePythonPercentF() {
        assertThat(RubMoneyWords.formatMoneyTwoDecimals(new BigDecimal("98500")))
                .isEqualTo("98500.00");
        assertThat(RubMoneyWords.formatMoneyTwoDecimals(new BigDecimal("1"))).isEqualTo("1.00");
        assertThat(RubMoneyWords.formatMoneyTwoDecimals(null)).isEmpty();
    }

    @Test
    void spellsWholeRublesWithoutFractionalTests() {
        assertThat(RubMoneyWords.amountInWords(new BigDecimal("1.00")))
                .startsWith("Один рубль ");
        assertThat(RubMoneyWords.amountInWords(new BigDecimal("2.00")))
                .startsWith("Два рубля ");
        assertThat(RubMoneyWords.amountInWords(new BigDecimal("98500.00")))
                .startsWith("Девяносто восемь тысяч пятьсот рублей ");
        assertThat(RubMoneyWords.amountInWords(null)).isEmpty();
    }
}
