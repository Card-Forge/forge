package forge.web.images;

import forge.ImageKeys;
import forge.StaticData;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.util.ImageUtil;
import forge.util.SleeveArt;

import java.io.File;
import java.util.Locale;
import java.util.regex.Pattern;

/** Where the page's card pictures and sleeve art are saved, and which keys are safe to look up. */
public final class ImageFiles {
    private static final Pattern PARENT = Pattern.compile("(^|[/\\\\:|])\\.\\.([/\\\\|]|$)");

    private ImageFiles() {
    }

    /** Refuses a key with a ".." path segment. Forge joins a key onto a folder path unchanged, and deletes a folder found where an image should be. */
    public static boolean safeKey(final String key) {
        return !PARENT.matcher(key).find();
    }

    private static boolean isImage(final File file) {
        final String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
    }

    /** The saved picture for a key, or null when it has not been downloaded. */
    public static File card(final String imageKey) {
        if (!safeKey(imageKey)) {
            return null;
        }
        String key = imageKey;
        final boolean backFace = key.endsWith(ImageKeys.BACKFACE_POSTFIX);
        if (backFace) {
            key = key.substring(0, key.length() - ImageKeys.BACKFACE_POSTFIX.length());
        }
        if (key.startsWith(ImageKeys.CARD_PREFIX)) {
            final PaperCard card = ImageUtil.getPaperCardFromImageKey(key);
            if (card == null) {
                return null;
            }
            key = backFace ? card.getCardAltImageKey() : card.getCardImageKey();
        }
        final File file = ImageKeys.getImageFile(key);
        return file != null && file.isFile() && isImage(file) ? file : null;
    }

    /** The key of the art that a card is drawn with. A rebalanced card has no picture, so it uses the key of the card it rebalances. */
    public static String artKey(final String imageKey) {
        if (!imageKey.startsWith(ImageKeys.CARD_PREFIX) || !safeKey(imageKey)) {
            return imageKey;
        }
        final PaperCard card = ImageUtil.getPaperCardFromImageKey(imageKey);
        if (card == null || !card.isRebalanced() || !card.getName().startsWith("A-")) {
            return imageKey;
        }
        final PaperCard original = StaticData.instance().getCommonCards().getCard(card.getName().substring(2), card.getEdition());
        return original == null ? imageKey : original.getImageKey(imageKey.endsWith(ImageKeys.BACKFACE_POSTFIX));
    }

    /** The art crop the shared fetcher has cached for a card, or null when it has not been downloaded yet. */
    public static File sleeveArt(final String imageKey) {
        final File f = new File(ForgeConstants.CACHE_SLEEVE_PICS_DIR, SleeveArt.cacheFileName(imageKey));
        return f.isFile() ? f : null;
    }

    public static String type(final File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".png") ? "image/png" : "image/jpeg";
    }
}
