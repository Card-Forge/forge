package forge;

import forge.card.CardEdition;
import forge.item.PaperCard;
import forge.util.FileUtil;
import forge.util.TextUtil;
import forge.util.ThreadUtil;
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
    public static final String ADVENTURECARD_PREFIX = "a:";

    public static final String HIDDEN_CARD           = "hidden";
    public static final String MORPH_IMAGE           = "morph";
    public static final String MANIFEST_IMAGE        = "manifest";
    public static final String CLOAKED_IMAGE         = "cloaked";
    public static final String FORETELL_IMAGE        = "foretell";
    public static final String BLESSING_IMAGE        = "blessing";
    public static final String INITIATIVE_IMAGE      = "initiative";
    public static final String MONARCH_IMAGE         = "monarch";
    public static final String THE_RING_IMAGE        = "the_ring";
    public static final String RADIATION_IMAGE       = "radiation";
    public static final String SPEED_IMAGE           = "speed";
    public static final String MAX_SPEED_IMAGE       = "max_speed";

    public static final String BACKFACE_POSTFIX  = "$alt";
    public static final String SPECFACE_W = "$wspec";
    public static final String SPECFACE_U = "$uspec";
    public static final String SPECFACE_B = "$bspec";
    public static final String SPECFACE_R = "$rspec";
    public static final String SPECFACE_G = "$gspec";

    private static String CACHE_CARD_PICS_DIR, CACHE_TOKEN_PICS_DIR, CACHE_ICON_PICS_DIR, CACHE_BOOSTER_PICS_DIR,
        CACHE_FATPACK_PICS_DIR, CACHE_BOOSTERBOX_PICS_DIR, CACHE_PRECON_PICS_DIR, CACHE_TOURNAMENTPACK_PICS_DIR;
    public static String ADVENTURE_CARD_PICS_DIR;
    private static Map<String, String> CACHE_CARD_PICS_SUBDIR;

    private static Map<String, Boolean> editionImageLookup = new HashMap<>();

    private static Map<String, Set<String>> editionAlias = new HashMap<>();
    private static Set<String> toFind = new HashSet<>();

    private static boolean isLibGDXPort = false;

    /**
     * Private constructor to prevent instantiation.
     */
    private ImageKeys() {
    }

    public static void setIsLibGDXPort(boolean value) {
        isLibGDXPort = value;
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

    private static final Map<String, File> cachedCards = new HashMap<>(50000);
    public static HashSet<String> missingCards = new HashSet<>();
    public static void clearMissingCards() {
        missingCards.clear();
    }
    public static File getCachedCardsFile(String key) {
        return cachedCards.get(key);
    }
    public static File getImageFile(String key) {
        if (StringUtils.isEmpty(key))
            return null;

        final String dir;
        final String filename;
        String[] tempdata = null;
        if (key.startsWith(ImageKeys.TOKEN_PREFIX)) {
            tempdata = key.substring(ImageKeys.TOKEN_PREFIX.length()).split("\\|");
            String tokenname = tempdata[0];
            if (tempdata.length > 1) {
                tokenname += "_" + tempdata[1];
            }
            if (tempdata.length > 2) {
                tokenname += "_" + tempdata[2];
            }
            filename = tokenname;

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
        } else if (key.startsWith(ImageKeys.ADVENTURECARD_PREFIX)) {
            filename = key.substring(ImageKeys.ADVENTURECARD_PREFIX.length());
            dir = ADVENTURE_CARD_PICS_DIR;
        }else {
            filename = key;
            dir = CACHE_CARD_PICS_DIR;
        }
        if (toFind.contains(filename))
            return null;
        if (missingCards.contains(filename))
            return null;

        File cachedFile = cachedCards.get(filename);
        if (cachedFile != null) {
            return cachedFile;
        } else {
            File file = findFile(dir, filename);
            if (file != null) {
                cachedCards.put(filename, file);
                return file;
            }
            if (dir.equals(CACHE_TOKEN_PICS_DIR)) {
                String setlessFilename = tempdata[0];
                String setCode = tempdata.length > 1 ? tempdata[1] : "";
                String collectorNumber = tempdata.length > 2 ? tempdata[2] : "";
                if (!setCode.isEmpty()) {
                    if (!collectorNumber.isEmpty()) {
                        file = findFile(dir, setCode + "/" + collectorNumber + "_" + setlessFilename);
                        if (file != null) {
                            cachedCards.put(filename, file);
                            return file;
                        }
                    }
                    file = findFile(dir, setCode + "/" + setlessFilename);
                    if (file != null) {
                        cachedCards.put(filename, file);
                        return file;
                    }
                }
                file = findFile(dir, setlessFilename);
                if (file != null) {
                    cachedCards.put(filename, file);
                    return file;
                }
            }

            // AE -> Ae and Ae -> AE for older cards with different file names
            // on case-sensitive file systems
            if (filename.contains("Ae")) {
                file = findFile(dir, TextUtil.fastReplace(filename, "Ae", "AE"));
                if (file != null) {
                    cachedCards.put(filename, file);
                    return file;
                }
            } else if (filename.contains("AE")) {
                file = findFile(dir, TextUtil.fastReplace(filename, "AE", "Ae"));
                if (file != null) {
                    cachedCards.put(filename, file);
                    return file;
                }
            }
            //try fullborder...
            if (filename.contains(".full")) {
                String fullborderFile = TextUtil.fastReplace(filename, ".full", ".fullborder");
                file = findFile(dir, fullborderFile);
                if (file != null) {
                    cachedCards.put(filename, file);
                    return file;
                }
                // if there's a 1st art variant try without it for .fullborder images
                file = findFile(dir, TextUtil.fastReplace(fullborderFile, "1.fullborder", ".fullborder"));
                if (file != null) {
                    cachedCards.put(filename, file);
                    return file;
                }
                // if there's an art variant try without it for .full images
                file = findFile(dir, filename.replaceAll("[0-9].full",".full"));
                if (file != null) {
                    cachedCards.put(filename, file);
                    return file;
                }
                //setlookup
                if (hasSetLookup(filename)) {
                    toFind.add(filename);
                    try {
                        ThreadUtil.getServicePool().submit(() -> {
                            File f = setLookUpFile(filename, fullborderFile);
                            if (f != null)
                                cachedCards.put(filename, f);
                            else //is null
                                missingCards.add(filename);
                            toFind.remove(filename);
                        });
                    } catch (Exception e) {
                        toFind.remove(filename);
                    }
                }
                String setCode = filename.contains("/") ? filename.substring(0, filename.indexOf("/")) : "";
                if (!setCode.isEmpty() && editionAlias.containsKey(setCode)) {
                    for (String alias : editionAlias.get(setCode)) {
                        file = findFile(dir, TextUtil.fastReplace(filename, setCode + "/", alias + "/"));
                        if (file != null) {
                            cachedCards.put(filename, file);
                            return file;
                        }
                        file = findFile(dir, TextUtil.fastReplace(fullborderFile, setCode + "/", alias + "/"));
                        if (file != null) {
                            cachedCards.put(filename, file);
                            return file;
                        }
                    }
                }
            }
            //if an image, like phenomenon or planes is missing .full in their filenames but you have an existing images that have .full/.fullborder
            if (!filename.contains(".full")) {
                file = findFile(dir, TextUtil.addSuffix(filename,".full"));
                if (file != null) {
                    cachedCards.put(filename, file);
                    return file;
                }
                file = findFile(dir, TextUtil.addSuffix(filename,".fullborder"));
                if (file != null) {
                    cachedCards.put(filename, file);
                    return file;
                }
            }
            if (filename.contains("/")) {
                String setlessFilename = filename.substring(filename.indexOf('/') + 1);
                file = findFile(dir, setlessFilename);
                if (file != null) {
                    cachedCards.put(filename, file);
                    return file;
                }

                if (setlessFilename.contains(".full")) {
                    //try fullborder
                    String fullborderFile = TextUtil.fastReplace(setlessFilename, ".full", ".fullborder");
                    file = findFile(dir, fullborderFile);
                    if (file != null) {
                        cachedCards.put(filename, file);
                        return file;
                    }
                    // try lowering the art index to the minimum for regular cards
                    file = findFile(dir, setlessFilename.replaceAll("[0-9]*[.]full", "1.full"));
                    if (file != null) {
                        cachedCards.put(filename, file);
                        return file;
                    }
                }
                //lookup other cards like planechase/phenomenon
                if (!filename.contains(".full")) {
                    String newFilename = TextUtil.addSuffix(filename,".full");
                    file = findFile(dir, newFilename);
                    if (file != null) {
                        cachedCards.put(filename, file);
                        return file;
                    }
                    String newFilename2 = TextUtil.addSuffix(filename,".fullborder");
                    file = findFile(dir, newFilename2);
                    if (file != null) {
                        cachedCards.put(filename, file);
                        return file;
                    }
                    String setCode = filename.substring(0, filename.indexOf("/"));
                    if (!setCode.isEmpty() && editionAlias.containsKey(setCode)) {
                        for (String alias : editionAlias.get(setCode)) {
                            file = findFile(dir, TextUtil.fastReplace(newFilename, setCode + "/", alias + "/"));
                            if (file != null) {
                                cachedCards.put(filename, file);
                                return file;
                            }
                            file = findFile(dir, TextUtil.fastReplace(newFilename2, setCode + "/", alias + "/"));
                            if (file != null) {
                                cachedCards.put(filename, file);
                                return file;
                            }
                        }
                    }
                }
            }
        }

        // System.out.println("File not found, no image created: " + key);
        // add missing cards - disable for desktop version for compatibility reasons with autodownloader
        if (isLibGDXPort && !hasSetLookup(filename)) //missing cards with setlookup is handled differently
            missingCards.add(filename);
        return null;
    }

    public static String getSetFolder(String edition) {
        return  !CACHE_CARD_PICS_SUBDIR.containsKey(edition)
                ? StaticData.instance().getEditions().getCode2ByCode(edition) // by default 2-letter codes from MWS are used
                : CACHE_CARD_PICS_SUBDIR.get(edition); // may use custom paths though
    }
    public static boolean hasSetLookup(String filename) {
        if (filename == null)
            return false;
        if (!StaticData.instance().getSetLookup().isEmpty()) {
            return StaticData.instance().getSetLookup().keySet().stream().anyMatch(filename::startsWith);
        }

        return false;
    }
    public static File setLookUpFile(String filename, String fullborderFile) {
        if (!StaticData.instance().getSetLookup().isEmpty()) {
            for (String setKey : StaticData.instance().getSetLookup().keySet()) {
                if (filename.startsWith(setKey)) {
                    for (String setLookup : StaticData.instance().getSetLookup().get(setKey)) {
                        String lookupDirectory = CACHE_CARD_PICS_DIR + setLookup;
                        File f = new File(lookupDirectory);
                        if (f.exists() && f.isDirectory()) {
                            for (String ext : FILE_EXTENSIONS) {
                                if (ext.isEmpty())
                                    continue;
                                File placeholder;
                                String fb1 = fullborderFile.replace(setKey+"/","")+ext;
                                placeholder = new File(lookupDirectory+"/"+fb1);
                                if (placeholder.exists()) {
                                    return placeholder;
                                }
                                String fb2 = fullborderFile.replace(setKey+"/","").replaceAll("[0-9]*.fullborder", "1.fullborder")+ext;
                                placeholder = new File(lookupDirectory+"/"+fb2);
                                if (placeholder.exists()) {
                                    return placeholder;
                                }
                                String f1 = filename.replace(setKey+"/","")+ext;
                                placeholder = new File(lookupDirectory+"/"+f1);
                                if (placeholder.exists()) {
                                    return placeholder;
                                }
                                String f2 = filename.replace(setKey+"/","").replaceAll("[0-9]*.full", "1.full")+ext;
                                placeholder = new File(lookupDirectory+"/"+f2);
                                if (placeholder.exists()) {
                                    return placeholder;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
    private static File findFile(String dir, String filename) {
        if (dir.equals(CACHE_CARD_PICS_DIR)) {
            for (String ext : FILE_EXTENSIONS) {
                if (ext.isEmpty())
                    continue;

                File f = new File(dir, filename + ext);
                if (f.exists()) {
                    return f;
                }
            }
        } else {
            //old method for tokens and others
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
        }
        return null;
    }

    //shortcut for determining if a card image exists for a given card
    //should only be called from PaperCard.hasImage()
    static HashMap<String, HashSet<String>> cachedContent=new HashMap<>(50000);
    public static boolean hasImage(PaperCard pc) {
        return hasImage(pc, false);
    }
    public static boolean hasImage(PaperCard pc, boolean update) {
        Boolean editionHasImage = editionImageLookup.get(pc.getEdition());
        if (editionHasImage == null) {
            String setFolder = getSetFolder(pc.getEdition());
            CardEdition ed = StaticData.instance().getEditions().get(setFolder);
            if (ed != null && !editionAlias.containsKey(setFolder)) {
                String alias = ed.getAlias();
                Set<String> aliasSet = new HashSet<>();
                if (alias != null) {
                    if (!alias.equalsIgnoreCase(setFolder))
                        aliasSet.add(alias);
                }
                String code = ed.getCode();
                if (code != null) {
                    if (!code.equalsIgnoreCase(setFolder))
                        aliasSet.add(code);
                }
                if (!aliasSet.isEmpty())
                    editionAlias.put(setFolder, aliasSet);
            }
            // Build list of all folders that contain images for this set
            // This handles mixed setups where images may be split across legacy (Code2),
            // modern code, and alias folders (e.g., some in DS/, some in DST/)
            List<String> foldersWithImages = new ArrayList<>();

            // Check Code2 folder first (legacy - highest priority for duplicates)
            if (FileUtil.isDirectoryWithFiles(CACHE_CARD_PICS_DIR + setFolder)) {
                foldersWithImages.add(setFolder);
            }

            // Check modern code and alias folders
            if (ed != null) {
                String code = ed.getCode();
                if (code != null && !code.equalsIgnoreCase(setFolder)) {
                    if (FileUtil.isDirectoryWithFiles(CACHE_CARD_PICS_DIR + code)) {
                        foldersWithImages.add(code);
                    }
                }
                String alias = ed.getAlias();
                if (alias != null && !alias.equalsIgnoreCase(setFolder)
                        && (code == null || !alias.equalsIgnoreCase(code))) {
                    if (FileUtil.isDirectoryWithFiles(CACHE_CARD_PICS_DIR + alias)) {
                        foldersWithImages.add(alias);
                    }
                }
            }

            editionHasImage = !foldersWithImages.isEmpty();
            editionImageLookup.put(pc.getEdition(), editionHasImage);
            if (editionHasImage) {
                HashSet<String> setFolderContent = new HashSet<>();
                // Process all folders, merging their contents
                // First folder in list wins for duplicates (legacy Code2 has priority)
                for (String folder : foldersWithImages) {
                    File f = new File(CACHE_CARD_PICS_DIR + folder);
                    String[] files = f.list();
                    if (files == null) continue;
                    for (String filename : files) {
                        // TODO: should this use FILE_EXTENSIONS ?
                        if (!filename.endsWith(".jpg") && !filename.endsWith(".png"))
                            continue;  // not image - not interested
                        String cardName = filename.split("\\.")[0];  // get rid of any full or fullborder
                        setFolderContent.add(cardName);
                        //preload cachedCards at startUp
                        // Use setFolder in key (for lookup) but actual folder in path
                        String key = setFolder + "/" + filename.replace(".fullborder", ".full").replace(".jpg", "").replace(".png", "");
                        // Only cache if not already present (first folder wins)
                        if (!cachedCards.containsKey(key)) {
                            File value = new File(CACHE_CARD_PICS_DIR + folder + "/" + filename);
                            cachedCards.put(key, value);
                        }
                    }
                }
                cachedContent.put(setFolder, setFolderContent);
            }
        }
        String[] keyParts = StringUtils.split(pc.getCardImageKey(), "//");
        if (keyParts.length != 2)
            return false;
        if (update && editionHasImage) {
            try {
                cachedContent.get(getSetFolder(pc.getEdition())).add(pc.getName());
            } catch (Exception e) {
                System.err.println(e);
            }
        }
        HashSet<String> content = cachedContent.getOrDefault(keyParts[0], null);
        //avoid checking for file if edition doesn't have any images
        return editionHasImage && hitCache(content, keyParts[1]);
    }

    private static boolean hitCache(HashSet<String> cache, String filename) {
        if (cache == null || cache.isEmpty())
            return false;
        final String keyPrefix = filename.split("\\.")[0];
        return cache.contains(keyPrefix);
    }
}
