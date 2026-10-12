package forge.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helpers for card-art deck sleeves: deriving a cache filename from a card image key, and
 * encoding card image keys for storage in comma-joined, '='-parsed preference values.
 */
public final class SleeveArt {
    private SleeveArt() {}

    // Crop offset along the slack axis: 0 = left/top edge, 1000 = right/bottom edge, 500 = centre
    public static final int DEFAULT_OFFSET = 500;

    public static int clampOffset(final int offset) {
        return Math.max(0, Math.min(1000, offset));
    }

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

    // Library entries pair a key with its crop offset as "b64key:offset". ':' never occurs in
    // url-safe base64, so it's a safe separator
    public static LinkedHashMap<String, Integer> parseLibrary(final String pref) {
        final LinkedHashMap<String, Integer> entries = new LinkedHashMap<>();
        if (pref == null || pref.isEmpty()) {
            return entries;
        }
        for (final String raw : pref.split(",")) {
            final String token = raw.trim();
            if (token.isEmpty()) {
                continue;
            }
            final int colon = token.indexOf(':');
            final String key = decode(colon < 0 ? token : token.substring(0, colon));
            if (key.isEmpty()) {
                continue;
            }
            int offset = DEFAULT_OFFSET;
            if (colon >= 0) {
                try {
                    offset = clampOffset(Integer.parseInt(token.substring(colon + 1)));
                } catch (final NumberFormatException e) {
                    offset = DEFAULT_OFFSET;
                }
            }
            entries.putIfAbsent(key, offset);
        }
        return entries;
    }

    public static String formatLibrary(final Map<String, Integer> entries) {
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, Integer> e : entries.entrySet()) {
            final String enc = encode(e.getKey());
            if (enc.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(enc).append(':').append(clampOffset(e.getValue() == null ? DEFAULT_OFFSET : e.getValue()));
        }
        return sb.toString();
    }
}
