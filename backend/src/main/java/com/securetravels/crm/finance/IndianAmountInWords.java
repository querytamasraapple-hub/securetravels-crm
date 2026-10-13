package com.securetravels.crm.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Indian numbering system amount-in-words for GST invoices. */
final class IndianAmountInWords {

    private static final String[] ONES = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
    };
    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private IndianAmountInWords() {}

    static String of(BigDecimal amount) {
        if (amount == null) return "";
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        long rupees = scaled.longValue();
        int paise = scaled.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        String words = "Rupees " + convert(rupees) + " Only";
        if (paise > 0) {
            words = "Rupees " + convert(rupees) + " and Paise " + convert(paise) + " Only";
        }
        return words;
    }

    private static String convert(long n) {
        if (n == 0) return "Zero";
        StringBuilder sb = new StringBuilder();
        long crore = n / 10000000;
        n %= 10000000;
        long lakh = n / 100000;
        n %= 100000;
        long thousand = n / 1000;
        n %= 1000;
        long hundred = n / 100;
        n %= 100;
        if (crore > 0) sb.append(twoDigits((int) crore)).append(" Crore ");
        if (lakh > 0) sb.append(twoDigits((int) lakh)).append(" Lakh ");
        if (thousand > 0) sb.append(twoDigits((int) thousand)).append(" Thousand ");
        if (hundred > 0) sb.append(ONES[(int) hundred]).append(" Hundred ");
        if (n > 0) {
            if (sb.length() > 0) sb.append("and ");
            sb.append(twoDigits((int) n)).append(' ');
        }
        return sb.toString().trim();
    }

    private static String twoDigits(int n) {
        if (n < 20) return ONES[n];
        return (TENS[n / 10] + (n % 10 == 0 ? "" : " " + ONES[n % 10])).trim();
    }
}
