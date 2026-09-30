package com.securetravels.crm.common.util;

import com.securetravels.crm.common.exception.BadRequestException;

/**
 * Structural + checksum validation of Indian GSTINs (15 chars).
 *
 * <p>Format: {@code [0-9]{2} [A-Z]{5} [0-9]{4} [A-Z] [0-9A-Z] Z [0-9A-Z]}:
 * state code (01-37, 38, 97, 99), PAN, entity code, entity number, the
 * mandatory {@code Z}, and a check digit.
 *
 * <p>Check digit: base-36 Luhn — the 14-digit prefix is scanned with
 * alternating weights 1 and 2; when {@code value * factor} reaches 36 the
 * digit sum of the two base-36 positions is used; the check equals
 * {@code (36 - sum % 36) % 36}.
 */
public final class GstinValidator {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final java.util.regex.Pattern FORMAT =
            java.util.regex.Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][0-9A-Z]Z[0-9A-Z]$");

    private GstinValidator() {
    }

    /**
     * Returns the uppercased trimmed GSTIN, or {@code null} when the input
     * is blank. Throws when a value is present but invalid — callers treat
     * GSTIN as optional, so {@code null} means "not supplied".
     */
    public static String normalizeOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim().toUpperCase(java.util.Locale.ROOT);
        if (!isValid(value)) {
            throw new BadRequestException("GSTIN is not valid: " + raw);
        }
        return value;
    }

    public static boolean isValid(String value) {
        if (value == null || value.length() != 15) {
            return false;
        }
        if (!FORMAT.matcher(value).matches()) {
            return false;
        }
        if (!validStateCode(value.substring(0, 2))) {
            return false;
        }
        return expectedCheckDigit(value.substring(0, 14)) == value.charAt(14);
    }

    private static boolean validStateCode(String two) {
        int code;
        try {
            code = Integer.parseInt(two);
        } catch (NumberFormatException e) {
            return false;
        }
        return (code >= 01 && code <= 37) || code == 38 || code == 97 || code == 99;
    }

    private static char expectedCheckDigit(String first14) {
        int sum = 0;
        for (int i = 0; i < 14; i++) {
            int factor = (i % 2 == 0) ? 1 : 2;
            int digit = ALPHABET.indexOf(first14.charAt(i)) * factor;
            if (digit >= 36) {
                digit = (digit / 36) + (digit % 36);
            }
            sum += digit;
        }
        return ALPHABET.charAt((36 - (sum % 36)) % 36);
    }
}