package forge.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Helpers for card-art deck sleeves: deriving a cache filename from a card image key, and
 * encoding card image keys for storage in comma-joined, '='-parsed preference values.
 */
public final class SleeveArt {
    private SleeveArt() {}

    // Suffix is ".artcrop.jpg" so the image fetchers' scryfall path munging leaves the
    // destination untouched (it only rewrites paths lacking ".full"/".artcrop")
    public static String cacheFileName(final String imageKey) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-1");
            final byte[] h = md.digest(imageKey.getBytes(StandardCharsets.UTF_8));
            final StringBuilder sb = new StringBuilder(h.length * 2);
            for (final byte b : h) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.append(".artcrop.jpg").toString();
        } catch (final Exception e) {
            throw new IllegalStateException("SHA-1 unavailable", e);
        }
    }

    // URL-safe Base64 without padding: no '=' to confuse the KEY=VALUE pref parser and no
    // ',' so comma-joined lists stay safe
    public static String encode(final String imageKey) {
        if (imageKey == null || imageKey.isEmpty()) {
            return "";
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(imageKey.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(final String token) {
        if (token == null || token.isEmpty()) {
            return "";
        }
        try {
            return new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        } catch (final IllegalArgumentException e) {
            return "";
        }
    }

    public static List<String> parseList(final String pref) {
        final List<String> keys = new ArrayList<>();
        if (pref == null || pref.isEmpty()) {
            return keys;
        }
        for (final String token : pref.split(",")) {
            final String key = decode(token.trim());
            if (!key.isEmpty() && !keys.contains(key)) {
                keys.add(key);
            }
        }
        return keys;
    }

    public static String formatList(final List<String> keys) {
        final StringBuilder sb = new StringBuilder();
        for (final String key : keys) {
            final String enc = encode(key);
            if (enc.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(enc);
        }
        return sb.toString();
    }
}
