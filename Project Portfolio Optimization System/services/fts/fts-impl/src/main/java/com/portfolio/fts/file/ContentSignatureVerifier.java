package com.portfolio.fts.file;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

@Component
public class ContentSignatureVerifier {

    public static final int HEAD_SIZE = 8192;

    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-'};
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] OOXML = {0x50, 0x4B, 0x03, 0x04};

    private static final Map<String, Predicate<byte[]>> SIGNATURES = Map.ofEntries(
            Map.entry("application/pdf", head -> startsWith(head, PDF)),
            Map.entry("image/png", head -> startsWith(head, PNG)),
            Map.entry("image/jpeg", head -> startsWith(head, JPEG)),
            Map.entry(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    head -> startsWith(head, OOXML)),
            Map.entry(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    head -> startsWith(head, OOXML)),
            Map.entry("text/csv", ContentSignatureVerifier::isUtf8Text),
            Map.entry("text/plain", ContentSignatureVerifier::isUtf8Text));

    public boolean matches(String contentType, byte[] head) {
        if (contentType == null || head == null || head.length == 0) {
            return false;
        }
        Predicate<byte[]> signature = SIGNATURES.get(contentType);
        return signature != null && signature.test(head);
    }

    private static boolean startsWith(byte[] head, byte[] magic) {
        return head.length >= magic.length
                && Arrays.equals(head, 0, magic.length, magic, 0, magic.length);
    }

    private static boolean isUtf8Text(byte[] head) {
        for (byte b : head) {
            if (b == 0) {
                return false;
            }
        }
        CharsetDecoder decoder = StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer in = ByteBuffer.wrap(head);
        CharBuffer out = CharBuffer.allocate(head.length);
        CoderResult result = decoder.decode(in, out, false);
        return result.isUnderflow();
    }
}
