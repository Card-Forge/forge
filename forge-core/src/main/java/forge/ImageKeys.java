package forge;

import forge.item.PaperCard;
import forge.util.FileUtil;
import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;

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
    public static final String FORETELL_IMAGE        = "foretell";

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
        if (StringUtils.isEmpty(key))
            return null;

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

        // AE -> Ae and Ae -> AE for older cards with different file names 
        // on case-sensitive file systems
        if (filename.contains("Ae")) {
            file = findFile(dir, TextUtil.fastReplace(filename, "Ae", "AE"));
            if (file != null) { return file; }
        } else if (filename.contains("AE")) {
            file = findFile(dir, TextUtil.fastReplace(filename, "AE", "Ae"));
            if (file != null) { return file; }
        }
        //try fullborder...
        if (filename.contains(".full")) {
            String fullborderFile = TextUtil.fastReplace(filename, ".full", ".fullborder");
            file = findFile(dir, fullborderFile);
            if (file != null) { return file; }
            // if there's a 1st art variant try without it for .fullborder images
            file = findFile(dir, TextUtil.fastReplace(fullborderFile, "1.fullborder", ".fullborder"));
            if (file != null) { return file; }
            // if there's a 1st art variant try with it for .fullborder images
            file = findFile(dir, fullborderFile.replaceAll("[0-9]*.fullborder", "1.fullborder"));
            if (file != null) { return file; }
            // if there's an art variant try without it for .full images
            file = findFile(dir, filename.replaceAll("[0-9].full",".full"));
            if (file != null) { return file; }
            // if there's a 1st art variant try with it for .full images
            file = findFile(dir, filename.replaceAll("[0-9]*.full", "1.full"));
            if (file != null) { return file; }
            //setlookup
            if (!StaticData.instance().getSetLookup().isEmpty()) {
                for (String setKey : StaticData.instance().getSetLookup().keySet()) {
                    if (filename.startsWith(setKey)) {
                        for (String setLookup : StaticData.instance().getSetLookup().get(setKey)) {
                            //.fullborder lookup
                            file = findFile(dir, TextUtil.fastReplace(fullborderFile, setKey, getSetFolder(setLookup)));
                            if (file != null) { return file; }
                            file = findFile(dir, TextUtil.fastReplace(fullborderFile, setKey, getSetFolder(setLookup)).replaceAll("[0-9]*.fullborder", "1.fullborder"));
                            if (file != null) { return file; }
                            //.full lookup
                            file = findFile(dir, TextUtil.fastReplace(filename, setKey, getSetFolder(setLookup)));
                            if (file != null) { return file; }
                            file = findFile(dir, TextUtil.fastReplace(filename, setKey, getSetFolder(setLookup)).replaceAll("[0-9]*.full", "1.full"));
                            if (file != null) { return file; }
                        }
                    }
                }
            }
        }
        //if an image, like phenomenon or planes is missing .full in their filenames but you have an existing images that have .full/.fullborder
        if (!filename.contains(".full")) {
            file = findFile(dir, TextUtil.addSuffix(filename,".full"));
            if (file != null) { return file; }
            file = findFile(dir, TextUtil.addSuffix(filename,".fullborder"));
            if (file != null) { return file; }
        }

        if (dir.equals(CACHE_TOKEN_PICS_DIR)) {
            int index = filename.lastIndexOf('_');
            if (index != -1) {
                String setlessFilename = filename.substring(0, index);
                String setCode = filename.substring(index + 1);
                // try with upper case set
                file = findFile(dir, setlessFilename + "_" + setCode.toUpperCase());
                if (file != null) { return file; }
                // try with lower case set
                file = findFile(dir, setlessFilename + "_" + setCode.toLowerCase());
                if (file != null) { return file; }
                // try without set name
                file = findFile(dir, setlessFilename);
                if (file != null) { return file; }
                // if there's an art variant try without it
                if (setlessFilename.matches(".*[0-9]*$")) {
                    file = findFile(dir, setlessFilename.replaceAll("[0-9]*$", ""));
                    if (file != null) { return file; }
                }
            }
        } else if (filename.contains("/")) {
            String setlessFilename = filename.substring(filename.indexOf('/') + 1);
            file = findFile(dir, setlessFilename);
            if (file != null) { return file; }

            if (setlessFilename.contains(".full")) {
            	//try fullborder
                String fullborderFile = TextUtil.fastReplace(setlessFilename, ".full", ".fullborder");
                file = findFile(dir, fullborderFile);
                if (file != null) { return file; }
                // try lowering the art index to the minimum for regular cards
                file = findFile(dir, setlessFilename.replaceAll("[0-9]*[.]full", "1.full"));
                if (file != null) { return file; }
            }
        }

        // System.out.println("File not found, no image created: " + key);

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
                if (file.isDirectory()) {
                    file.delete();
                    continue;
                }
                return file;
            }
        }
        return null;
    }

    //shortcut for determining if a card image exists for a given card
    //should only be called from PaperCard.hasImage()
    static HashMap<String, HashSet<String>> cachedContent=new HashMap<>();
    public static boolean hasImage(PaperCard pc) {
        Boolean editionHasImage = editionImageLookup.get(pc.getEdition());
        if (editionHasImage == null) {
            String setFolder = getSetFolder(pc.getEdition());
            editionHasImage = FileUtil.isDirectoryWithFiles(CACHE_CARD_PICS_DIR + setFolder);
            editionImageLookup.put(pc.getEdition(), editionHasImage);
            if (editionHasImage){
                File f = new File(CACHE_CARD_PICS_DIR + setFolder);  // no need to check this, otherwise editionHasImage would be false!
                HashSet<String> setFolderContent = new HashSet<>();
                for (String filename : Arrays.asList(f.list())) {
                    // TODO: should this use FILE_EXTENSIONS ?
                    if (!filename.endsWith(".jpg") && !filename.endsWith(".png"))
                        continue;  // not image - not interested
                    setFolderContent.add(filename.split("\\.")[0]);  // get rid of any full or fullborder
                }
                cachedContent.put(setFolder, setFolderContent);
            }
        }
        String[] keyParts = StringUtils.split(pc.getCardImageKey(), "//");
        if (keyParts.length != 2)
            return false;
        HashSet<String> content = cachedContent.getOrDefault(keyParts[0], null);
        //avoid checking for file if edition doesn't have any images
        return editionHasImage && hitCache(content, keyParts[1]);
    }

    private static boolean hitCache(HashSet<String> cache, String filename){
        if (cache == null || cache.isEmpty())
            return false;
        final String keyPrefix = filename.split("\\.")[0];
        return cache.contains(keyPrefix);
    }
}
