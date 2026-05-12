package com.logistic.backend.catalog;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

public final class CustomerPlaceKeys {

    private CustomerPlaceKeys() {}

    /** Normalized text used only for stable key hashing (not for display). */
    public static String normalizeForKey(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.strip();
        if (t.isEmpty()) {
            return "";
        }
        String nfkc = Normalizer.normalize(t, Normalizer.Form.NFKC);
        String lower = nfkc.toLowerCase(Locale.ROOT);
        return lower.replaceAll("\\s+", " ").strip();
    }

    public static Optional<String> addressKeySha256(String rawAddress) {
        String n = normalizeForKey(rawAddress);
        if (n.isEmpty()) {
            return Optional.empty();
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(n.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return Optional.of(sb.toString());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
