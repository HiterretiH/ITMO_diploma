package com.logistic.backend.document;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Russian monetary amounts in words (рубли и копейки), aligned with typical banking/legal wording.
 * External libs were avoided here so builds stay reproducible when Maven Central is unreachable.
 */
public final class RubMoneyWords {

    private RubMoneyWords() {}

    private static final String[] UNITS_M =
            {"", "один", "два", "три", "четыре", "пять", "шесть", "семь", "восемь", "девять"};
    private static final String[] UNITS_F =
            {"", "одна", "две", "три", "четыре", "пять", "шесть", "семь", "восемь", "девять"};
    private static final String[] TEENS = {
        "десять",
        "одиннадцать",
        "двенадцать",
        "тринадцать",
        "четырнадцать",
        "пятнадцать",
        "шестнадцать",
        "семнадцать",
        "восемнадцать",
        "девятнадцать",
    };
    private static final String[] TENS = {
        "",
        "",
        "двадцать",
        "тридцать",
        "сорок",
        "пятьдесят",
        "шестьдесят",
        "семьдесят",
        "восемьдесят",
        "девяносто",
    };
    private static final String[] HUNDREDS = {
        "",
        "сто",
        "двести",
        "триста",
        "четыреста",
        "пятьсот",
        "шестьсот",
        "семьсот",
        "восемьсот",
        "девятьсот",
    };

    /** Same as Python {@code "%.2f"} for non-null values (dot as decimal separator). */
    public static String formatMoneyTwoDecimals(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    /**
     * Full amount in words for documents, capitalized (first letter upper case). Empty when amount is
     * null.
     */
    public static String amountInWords(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        long totalKopecks = scaled.movePointRight(2).longValueExact();
        long rubles = totalKopecks / 100;
        int kopecks = (int) (totalKopecks % 100);

        String rub = rublesChunk(rubles);
        String kop = kopecksChunk(kopecks);
        String joined = (rub + " " + kop).trim();
        return capitalize(joined);
    }

    private static String rublesChunk(long rubles) {
        if (rubles == 0) {
            return "ноль " + plural(rubles, "рубль", "рубля", "рублей");
        }
        StringBuilder sb = new StringBuilder();
        long restAll = rubles;
        int gig = (int) (restAll / 1_000_000_000);
        restAll %= 1_000_000_000;
        int mil = (int) (restAll / 1_000_000);
        restAll %= 1_000_000;
        int thou = (int) (restAll / 1000);
        int rem = (int) (restAll % 1000);

        appendTriple(sb, gig, Scale.BILLIONS);
        appendTriple(sb, mil, Scale.MILLIONS);
        appendTriple(sb, thou, Scale.THOUSANDS);
        if (rem > 0) {
            appendWords(sb, triplet(rem, Gender.MASCULINE));
        }
        appendWords(sb, plural(rubles, "рубль", "рубля", "рублей"));
        return sb.toString().trim();
    }

    private static String kopecksChunk(int kopecks) {
        if (kopecks < 0 || kopecks > 99) {
            throw new IllegalArgumentException("kopecks");
        }
        if (kopecks == 0) {
            return "ноль " + plural(kopecks, "копейка", "копейки", "копеек");
        }
        return triplet(kopecks, Gender.FEMININE).trim()
                + " "
                + plural(kopecks, "копейка", "копейки", "копеек");
    }

    private enum Scale {
        BILLIONS(Gender.MASCULINE, "миллиард", "миллиарда", "миллиардов"),
        MILLIONS(Gender.MASCULINE, "миллион", "миллиона", "миллионов"),
        THOUSANDS(Gender.FEMININE, "тысяча", "тысячи", "тысяч");

        private final Gender tripletGender;
        private final String one;
        private final String few;
        private final String many;

        Scale(Gender tripletGender, String one, String few, String many) {
            this.tripletGender = tripletGender;
            this.one = one;
            this.few = few;
            this.many = many;
        }

        String plural(long n) {
            return RubMoneyWords.plural(n, one, few, many);
        }
    }

    private static void appendTriple(StringBuilder sb, int value, Scale scale) {
        if (value == 0) {
            return;
        }
        Gender g = scale.tripletGender;
        appendWords(sb, triplet(value, g));
        appendWords(sb, scale.plural(value));
    }

    private static void appendWords(StringBuilder sb, String chunk) {
        if (chunk == null || chunk.isBlank()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(' ');
        }
        sb.append(chunk.trim());
    }

    /** 1..999 words; gender affects units (один/одна/одно). */
    private static String triplet(int n, Gender gender) {
        if (n <= 0 || n > 999) {
            return "";
        }
        int h = n / 100;
        int t = n % 100;
        StringBuilder sb = new StringBuilder();
        if (h > 0) {
            sb.append(HUNDREDS[h]);
        }
        if (t > 0) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            if (t < 10) {
                sb.append(unit(t, gender));
            } else if (t < 20) {
                sb.append(TEENS[t - 10]);
            } else {
                sb.append(TENS[t / 10]);
                int u = t % 10;
                if (u != 0) {
                    sb.append(' ');
                    sb.append(unit(u, gender));
                }
            }
        }
        return sb.toString().trim();
    }

    private static String unit(int digit, Gender gender) {
        if (digit < 1 || digit > 9) {
            return "";
        }
        return gender == Gender.FEMININE ? UNITS_F[digit] : UNITS_M[digit];
    }

    private static String plural(long n, String one, String few, String many) {
        long nAbs = Math.abs(n) % 100;
        long n10 = nAbs % 10;
        if (nAbs >= 11 && nAbs <= 14) {
            return many;
        }
        if (n10 == 1) {
            return one;
        }
        if (n10 >= 2 && n10 <= 4) {
            return few;
        }
        return many;
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private enum Gender {
        MASCULINE,
        FEMININE,
    }
}
