package com.portfolio.fts.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ContentSignatureVerifierTest {

    private static final byte[] PDF = "%PDF-1.7".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D
    };
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
    private static final byte[] OOXML = {0x50, 0x4B, 0x03, 0x04, 0x14, 0x00};
    private static final byte[] CSV = "col1,col2\n1,2".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PLAIN = "hello world".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NUL_BINARY = {0x48, 0x00, 0x69};
    private static final byte[] INVALID_UTF8 = {(byte) 0xC3, 0x28};
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final ContentSignatureVerifier verifier = new ContentSignatureVerifier();

    static Stream<Arguments> matchingHeads() {
        return Stream.of(
                arguments("application/pdf", PDF),
                arguments("image/png", PNG),
                arguments("image/jpeg", JPEG),
                arguments(XLSX, OOXML),
                arguments(DOCX, OOXML),
                arguments("text/csv", CSV),
                arguments("text/plain", PLAIN));
    }

    @ParameterizedTest
    @MethodSource("matchingHeads")
    @DisplayName("accepts each declared type when the head matches its signature")
    void matches_acceptsKnownSignatures(String contentType, byte[] head) {
        assertThat(verifier.matches(contentType, head)).isTrue();
    }

    static Stream<Arguments> mismatchedHeads() {
        return Stream.of(
                arguments("application/pdf", PNG),
                arguments("image/png", JPEG),
                arguments("image/jpeg", PDF),
                arguments(XLSX, PDF),
                arguments(DOCX, JPEG),
                arguments("text/csv", PNG),
                arguments("text/plain", NUL_BINARY));
    }

    @ParameterizedTest
    @MethodSource("mismatchedHeads")
    @DisplayName("rejects each declared type when the head has the wrong signature")
    void matches_rejectsWrongSignature(String contentType, byte[] head) {
        assertThat(verifier.matches(contentType, head)).isFalse();
    }

    @Test
    @DisplayName("rejects text that contains an invalid UTF-8 sequence")
    void matches_rejectsInvalidUtf8() {
        assertThat(verifier.matches("text/plain", INVALID_UTF8)).isFalse();
        assertThat(verifier.matches("text/csv", INVALID_UTF8)).isFalse();
    }

    @Test
    @DisplayName("accepts UTF-8 text whose last multi-byte character is truncated at the end of the head")
    void matches_acceptsTruncatedUtf8AtEnd() {
        byte[] euro = "€".getBytes(StandardCharsets.UTF_8);
        byte[] truncatedEuro = Arrays.copyOf(euro, euro.length - 1);
        byte[] prefix = "price ".getBytes(StandardCharsets.UTF_8);
        byte[] truncatedAtEnd = concat(prefix, truncatedEuro);

        assertThat(verifier.matches("text/plain", truncatedAtEnd)).isTrue();
        assertThat(verifier.matches("text/csv", truncatedEuro)).isTrue();
    }

    @Test
    @DisplayName("accepts UTF-8 text that starts with a BOM")
    void matches_acceptsUtf8Bom() {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] withBom = concat(bom, CSV);

        assertThat(verifier.matches("text/csv", withBom)).isTrue();
        assertThat(verifier.matches("text/plain", withBom)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "application/octet-stream",
            "application/zip",
            "image/gif",
            "application/json",
            "APPLICATION/PDF",
            "application/pdf; charset=binary"
    })
    @DisplayName("rejects unknown or non-normalized content types even when the head looks familiar")
    void matches_rejectsUnknownContentType(String contentType) {
        assertThat(verifier.matches(contentType, PDF)).isFalse();
        assertThat(verifier.matches(contentType, OOXML)).isFalse();
        assertThat(verifier.matches(contentType, PLAIN)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "application/pdf",
            "image/png",
            "image/jpeg",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/csv",
            "text/plain"
    })
    @DisplayName("rejects an empty head for every known type")
    void matches_rejectsEmptyHead(String contentType) {
        assertThat(verifier.matches(contentType, new byte[0])).isFalse();
    }

    private static byte[] concat(byte[] left, byte[] right) {
        byte[] out = Arrays.copyOf(left, left.length + right.length);
        System.arraycopy(right, 0, out, left.length, right.length);
        return out;
    }
}
