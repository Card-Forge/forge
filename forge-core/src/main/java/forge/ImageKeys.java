package forge;

import forge.card.CardDb;
import forge.item.*;
import forge.util.FileUtil;
import forge.util.ImageUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class ImageKeys {
    public static final String CARD_PREFIX           = "c:";
    public static final String TOKEN_PREFIX          = "t:";
    public static final String ICON_PREFIX           = "i:";
    public static final String BOOSTER_PREFIX        = "b:";
    public static final String FATPACK_PREFIX        = "f:";
    public static final String BOOSTERBOX_PREFIX     = "x:";
    public static final String PRECON_PREFIX         = "p:";
    public static final String TOURNAMENTPACK_PREFIX = "o:";

    public static final String HIDDEN_CARD           = "hidden";
    public static final String MORPH_IMAGE           = "morph";
    public static final String MANIFEST_IMAGE        = "manifest";

    public static final String BACKFACE_POSTFIX  = "$alt";

    private static String CACHE_CARD_PICS_DIR, CACHE_TOKEN_PICS_DIR, CACHE_ICON_PICS_DIR, CACHE_BOOSTER_PICS_DIR,
        CACHE_FATPACK_PICS_DIR, CACHE_BOOSTERBOX_PICS_DIR, CACHE_PRECON_PICS_DIR, CACHE_TOURNAMENTPACK_PICS_DIR;
    private static Map<String, String> CACHE_CARD_PICS_SUBDIR;

    private static Map<String, Boolean> editionImageLookup = new HashMap<>();

    /**
     * Private constructor to prevent instantiation.
     */
    private ImageKeys() {
    }

    public static void initializeDirs(String cards, Map<String, String> cardsSub, String tokens, String icons, String boosters,
            String fatPacks, String boosterBoxes, String precons, String tournamentPacks) {
        CACHE_CARD_PICS_DIR = cards;
        CACHE_CARD_PICS_SUBDIR = cardsSub;
        CACHE_TOKEN_PICS_DIR = tokens;
        CACHE_ICON_PICS_DIR = icons;
        CACHE_BOOSTER_PICS_DIR = boosters;
        CACHE_FATPACK_PICS_DIR = fatPacks;
        CACHE_BOOSTERBOX_PICS_DIR = boosterBoxes;
        CACHE_PRECON_PICS_DIR = precons;
        CACHE_TOURNAMENTPACK_PICS_DIR = tournamentPacks;
    }

    // image file extensions for various formats in order of likelihood
    // the last, empty, string is for keys that come in with an extension already in place
    private static final String[] FILE_EXTENSIONS = { ".jpg", ".png", "" };

    public static String getImageKey(PaperCard pc, boolean altState) {
        return ImageKeys.CARD_PREFIX + pc.getName() + CardDb.NameSetSeparator + pc.getEdition() + CardDb.NameSetSeparator + pc.getArtIndex() + (altState ? BACKFACE_POSTFIX : "");
    }

    // Inventory items don't have to know how a certain client should draw them.
    // That's why this method is not encapsulated and overloaded in the InventoryItem descendants
    public static String getImageKey(InventoryItem ii, boolean altState) {
        if (ii instanceof PaperCard) {
            return getImageKey((PaperCard)ii, altState);
        }
        if (ii instanceof TournamentPack) {
            return ImageKeys.TOURNAMENTPACK_PREFIX + ((TournamentPack)ii).getEdition();
        }
        if (ii instanceof BoosterPack) {
            BoosterPack bp = (BoosterPack)ii;
            if (SealedProduct.specialSets.contains(bp.getEdition()) || bp.getEdition().equals("?")) {
                return "b:" + bp.getName().substring(0, bp.getName().indexOf(bp.getItemType()) - 1);
            }
            int cntPics = StaticData.instance().getEditions().get(bp.getEdition()).getCntBoosterPictures();
            String suffix = (1 >= cntPics) ? "" : ("_" + bp.getArtIndex());
            return ImageKeys.BOOSTER_PREFIX + bp.getEdition() + suffix;
        }
        if (ii instanceof FatPack) {
            return ImageKeys.FATPACK_PREFIX + ((FatPack)ii).getEdition();
        }
        if (ii instanceof BoosterBox) {
            return ImageKeys.BOOSTERBOX_PREFIX + ((BoosterBox)ii).getEdition();
        }
        if (ii instanceof PreconDeck) {
            return ImageKeys.PRECON_PREFIX + ((PreconDeck)ii).getImageFilename();
        }
        if (ii instanceof PaperToken) {
            return ImageKeys.TOKEN_PREFIX + ((PaperToken)ii).getImageFilename();
        }
        return null;
    }

    public static String getTokenKey(String tokenName) {
        return ImageKeys.TOKEN_PREFIX + tokenName;
    }

    public static String getTokenImageName(String tokenKey) {
        if (!tokenKey.startsWith(ImageKeys.TOKEN_PREFIX)) {
            return null;
        }
        return tokenKey.substring(ImageKeys.TOKEN_PREFIX.length());
    }

    public static File getImageFile(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }

        final String dir;
        final String filename;
        if (key.startsWith(ImageKeys.TOKEN_PREFIX)) {
            filename = key.substring(ImageKeys.TOKEN_PREFIX.length());
            dir = CACHE_TOKEN_PICS_DIR;
        } else if (key.startsWith(ImageKeys.ICON_PREFIX)) {
            filename = key.substring(ImageKeys.ICON_PREFIX.length());
            dir = CACHE_ICON_PICS_DIR;
        } else if (key.startsWith(ImageKeys.BOOSTER_PREFIX)) {
            filename = key.substring(ImageKeys.BOOSTER_PREFIX.length());
            dir = CACHE_BOOSTER_PICS_DIR;
        } else if (key.startsWith(ImageKeys.FATPACK_PREFIX)) {
            filename = key.substring(ImageKeys.FATPACK_PREFIX.length());
            dir = CACHE_FATPACK_PICS_DIR;
        } else if (key.startsWith(ImageKeys.BOOSTERBOX_PREFIX)) {
            filename = key.substring(ImageKeys.BOOSTERBOX_PREFIX.length());
            dir = CACHE_BOOSTERBOX_PICS_DIR;
        } else if (key.startsWith(ImageKeys.PRECON_PREFIX)) {
            filename = key.substring(ImageKeys.PRECON_PREFIX.length());
            dir = CACHE_PRECON_PICS_DIR;
        } else if (key.startsWith(ImageKeys.TOURNAMENTPACK_PREFIX)) {
            filename = key.substring(ImageKeys.TOURNAMENTPACK_PREFIX.length());
            dir = CACHE_TOURNAMENTPACK_PICS_DIR;
        } else {
            filename = key;
            dir = CACHE_CARD_PICS_DIR;
        }

        File file = findFile(dir, filename);
        if (file != null) { return file; }

        // some S00 cards are really part of 6ED
        String s2kAlias = getSetFolder("S00");
        if (filename.startsWith(s2kAlias)) {
            file = findFile(dir, filename.replace(s2kAlias, getSetFolder("6ED")));
            if (file != null) { return file; }
        }

        // try without set name
        if (dir.equals(CACHE_TOKEN_PICS_DIR)) {
            int index = filename.lastIndexOf('_');
            if (index != -1) {
                String setlessFilename = filename.substring(0, index);
                file = findFile(dir, setlessFilename);
                if (file != null) { return file; }
            }
        } else if (filename.contains("/")) {
            String setlessFilename = filename.substring(filename.indexOf('/') + 1);
            file = findFile(dir, setlessFilename);
            if (file != null) { return file; }

            // try lowering the art index to the minimum for regular cards
            if (setlessFilename.contains(".full")) {
                file = findFile(dir, setlessFilename.replaceAll("[0-9]*[.]full", "1.full"));
                if (file != null) { return file; }
            }
        }

        System.out.println("File not found, no image created: " + key);

        return null;
    }

    public static String getSetFolder(String edition) {
        return  !CACHE_CARD_PICS_SUBDIR.containsKey(edition)
                ? StaticData.instance().getEditions().getCode2ByCode(edition) // by default 2-letter codes from MWS are used
                : CACHE_CARD_PICS_SUBDIR.get(edition); // may use custom paths though
    }

    private static File findFile(String dir, String filename) {
        for (String ext : FILE_EXTENSIONS) {
            File file = new File(dir, filename + ext);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    //shortcut for determining if a card image exists for a given card
    //should only be called from PaperCard.hasImage()
    public static boolean hasImage(PaperCard pc) {
        Boolean editionHasImage = editionImageLookup.get(pc.getEdition());
        if (editionHasImage == null) {
            String setFolder = getSetFolder(pc.getEdition());
            editionHasImage = FileUtil.isDirectoryWithFiles(CACHE_CARD_PICS_DIR + setFolder);
            editionImageLookup.put(pc.getEdition(), editionHasImage);
        }
        //avoid checking for file if edition doesn't have any images
        return editionHasImage && findFile(CACHE_CARD_PICS_DIR, ImageUtil.getImageKey(pc, false, true)) != null;
    }
}
