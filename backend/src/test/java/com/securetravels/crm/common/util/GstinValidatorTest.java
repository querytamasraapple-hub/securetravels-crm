package com.securetravels.crm.common.util;

import com.securetravels.crm.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class GstinValidatorTest {

    /** Known-valid real GSTINs (structurally and checksum-correct). */
    private static final String[] VALID = {
            "27AAPFU0939F1ZV",  // Maharashtra
            "29AAECI9765K1ZR",  // Karnataka
            "33AAACC1206D1ZN",  // Tamil Nadu
            "09AAAUP8175A1ZG"   // Uttarakhand
    };

    @Test
    void acceptsKnownValidGstins() {
        for (String gstin : VALID) {
            assertThat(GstinValidator.isValid(gstin)).as(gstin).isTrue();
        }
    }

    @Test
    void generatedCheckDigitMatchesTheSample() {
        for (String gstin : VALID) {
            assertThat(GstinValidator.isValid(gstin)).isTrue();
        }
    }

    @Test
    void normalizesUpperAndTrim() {
        assertThat(GstinValidator.normalizeOrNull("  29aaECI9765K1ZR ")).isEqualTo("29AAECI9765K1ZR");
    }

    @Test
    void blankIsOptionalNotInvalid() {
        assertThat(GstinValidator.normalizeOrNull(null)).isNull();
        assertThat(GstinValidator.normalizeOrNull("  ")).isNull();
    }

    @Test
    void rejectsWrongChecksum() {
        String broken = "27AAPFU0939F1ZW";   // last char flipped
        assertThat(GstinValidator.isValid(broken)).isFalse();
    }

    @Test
    void rejectsBrokenPanShape() {
        // A digit inside the PAN letters makes the PAN part fail the regex.
        assertThat(GstinValidator.isValid("27AAP0U0939F1ZV")).isFalse();
    }

    @Test
    void rejectsMissingZMarker() {
        String broken = "27AAPFU0939F11V";    // char 12 must be Z
        assertThat(GstinValidator.isValid(broken)).isFalse();
    }

    @Test
    void rejectsWrongLength() {
        assertThat(GstinValidator.isValid("27AAPFU0939F1Z")).isFalse();
        assertThat(GstinValidator.isValid("27AAPFU0939F1ZVV")).isFalse();
    }

    @Test
    void rejectsInvalidStateCode() {
        assertThat(GstinValidator.isValid("00AAPFU0939F1ZG")).isFalse();
        assertThat(GstinValidator.isValid("39AAPFU0939F1ZG")).isFalse();
    }

    @Test
    void normalizeRejectsPresentButInvalid() {
        assertThatExceptionOfType(BadRequestException.class)
                .isThrownBy(() -> GstinValidator.normalizeOrNull("27AAPFU0939F1ZW"));
    }

    @Test
    void rejectsGarbage() {
        assertThat(GstinValidator.isValid("not-a-gstin")).isFalse();
        assertThat(GstinValidator.isValid("")).isFalse();
    }
}