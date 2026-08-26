package forge.assets;

import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;

import forge.Forge;
import forge.Graphics;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgeConstants;
import forge.util.Aggregates;
import forge.util.ImageFetcher;

public enum FSkinTexture implements FImage {
    BG_TEXTURE(ForgeConstants.TEXTURE_BG_FILE, true, false),
    BG_MATCH(ForgeConstants.MATCH_BG_FILE, false, false),
    BG_MATCH_DAY(ForgeConstants.MATCH_BG_DAY_FILE, false, false),
    BG_MATCH_NIGHT(ForgeConstants.MATCH_BG_NIGHT_FILE, false, false),
    BG_SPACE(ForgeConstants.SPACE_BG_FILE, false, false),
    BG_CHAOS_WHEEL(ForgeConstants.CHAOS_WHEEL_IMG_FILE, false, false),
    //Adventure textures
    ADV_BG_TEXTURE(ForgeConstants.ADV_TEXTURE_BG_FILE, true, false),
    ADV_BG_MATCH(ForgeConstants.ADV_MATCH_BG_FILE, false, false),
    ADV_BG_MATCH_DAY(ForgeConstants.ADV_MATCH_BG_DAY_FILE, false, false),
    ADV_BG_MATCH_NIGHT(ForgeConstants.ADV_MATCH_BG_NIGHT_FILE, false, false),
    ADV_BG_SWAMP(ForgeConstants.ADV_BG_SWAMP_FILE, false, false),
    ADV_BG_FOREST(ForgeConstants.ADV_BG_FOREST_FILE, false, false),
    ADV_BG_MOUNTAIN(ForgeConstants.ADV_BG_MOUNTAIN_FILE, false, false),
    ADV_BG_ISLAND(ForgeConstants.ADV_BG_ISLAND_FILE, false, false),
    ADV_BG_PLAINS(ForgeConstants.ADV_BG_PLAINS_FILE, false, false),
    ADV_BG_WASTE(ForgeConstants.ADV_BG_WASTE_FILE, false, false),
    ADV_BG_COMMON(ForgeConstants.ADV_BG_COMMON_FILE, false, false),
    ADV_BG_CAVE(ForgeConstants.ADV_BG_CAVE_FILE, false, false),
    ADV_BG_DUNGEON(ForgeConstants.ADV_BG_DUNGEON_FILE, false, false),
    ADV_BG_CASTLE(ForgeConstants.ADV_BG_CASTLE_FILE, false, false),
    //CARD BG
    CARDBG_A(ForgeConstants.IMG_CARDBG_A, false, false),
    CARDBG_B(ForgeConstants.IMG_CARDBG_B, false, false),
    CARDBG_BG(ForgeConstants.IMG_CARDBG_BG, false, false),
    CARDBG_BR(ForgeConstants.IMG_CARDBG_BR, false, false),
    CARDBG_C(ForgeConstants.IMG_CARDBG_C, false, false),
    CARDBG_G(ForgeConstants.IMG_CARDBG_G, false, false),
    CARDBG_L(ForgeConstants.IMG_CARDBG_L, false, false),
    CARDBG_M(ForgeConstants.IMG_CARDBG_M, false, false),
    CARDBG_R(ForgeConstants.IMG_CARDBG_R, false, false),
    CARDBG_RG(ForgeConstants.IMG_CARDBG_RG, false, false),
    CARDBG_U(ForgeConstants.IMG_CARDBG_U, false, false),
    CARDBG_UB(ForgeConstants.IMG_CARDBG_UB, false, false),
    CARDBG_UG(ForgeConstants.IMG_CARDBG_UG, false, false),
    CARDBG_UR(ForgeConstants.IMG_CARDBG_UR, false, false),
    CARDBG_V(ForgeConstants.IMG_CARDBG_V, false, false),
    CARDBG_W(ForgeConstants.IMG_CARDBG_W, false, false),
    CARDBG_WB(ForgeConstants.IMG_CARDBG_WB, false, false),
    CARDBG_WG(ForgeConstants.IMG_CARDBG_WG, false, false),
    CARDBG_WR(ForgeConstants.IMG_CARDBG_WR, false, false),
    CARDBG_WU(ForgeConstants.IMG_CARDBG_WU, false, false),
    PWBG_B(ForgeConstants.IMG_PWBG_B, false, false),
    PWBG_BG(ForgeConstants.IMG_PWBG_BG, false, false),
    PWBG_BR(ForgeConstants.IMG_PWBG_BR, false, false),
    PWBG_C(ForgeConstants.IMG_PWBG_C, false, false),
    PWBG_G(ForgeConstants.IMG_PWBG_G, false, false),
    PWBG_M(ForgeConstants.IMG_PWBG_M, false, false),
    PWBG_R(ForgeConstants.IMG_PWBG_R, false, false),
    PWBG_RG(ForgeConstants.IMG_PWBG_RG, false, false),
    PWBG_U(ForgeConstants.IMG_PWBG_U, false, false),
    PWBG_UB(ForgeConstants.IMG_PWBG_UB, false, false),
    PWBG_UG(ForgeConstants.IMG_PWBG_UG, false, false),
    PWBG_UR(ForgeConstants.IMG_PWBG_UR, false, false),
    PWBG_W(ForgeConstants.IMG_PWBG_W, false, false),
    PWBG_WB(ForgeConstants.IMG_PWBG_WB, false, false),
    PWBG_WG(ForgeConstants.IMG_PWBG_WG, false, false),
    PWBG_WR(ForgeConstants.IMG_PWBG_WR, false, false),
    PWBG_WU(ForgeConstants.IMG_PWBG_WU, false, false),
    NYX_B(ForgeConstants.IMG_NYX_B, false, false),
    NYX_G(ForgeConstants.IMG_NYX_G, false, false),
    NYX_M(ForgeConstants.IMG_NYX_M, false, false),
    NYX_R(ForgeConstants.IMG_NYX_R, false, false),
    NYX_U(ForgeConstants.IMG_NYX_U, false, false),
    NYX_W(ForgeConstants.IMG_NYX_W, false, false),
    NYX_C(ForgeConstants.IMG_NYX_C, false, false),
    GENERIC_PLANE("", false, true);


    private String filename;
    private final boolean repeat;
    private Texture texture;
    private final boolean isPlanechaseBG;
    private static List<String> planechaseString;
    private boolean isloaded = false;
    private boolean hasError = false;
    private FileHandle[] adventureBackgroundFiles;
    private FileHandle adventureBackgroundFile;
    private String adventureBackgroundFolderKey;
    private static final FileHandle[] NO_FILES = new FileHandle[0];
    private static final FilenameFilter IMAGE_FILES = (dir, name) -> {
        String lowerCaseName = name.toLowerCase(Locale.ROOT);
        return lowerCaseName.endsWith(".jpg") || lowerCaseName.endsWith(".jpeg") || lowerCaseName.endsWith(".png");
    };

    FSkinTexture(String filename0, boolean repeat0, boolean isPlanechaseBG0) {
        filename = filename0;
        repeat = repeat0;
        isPlanechaseBG = isPlanechaseBG0;
    }

    public static List<String> getValues() {
        if (planechaseString == null) {
            planechaseString = new ArrayList<>();
            for (FSkinTexture fskinTexture : FSkinTexture.values()) {
                if (fskinTexture.isPlanechaseBG)
                    planechaseString.add(fskinTexture.filename
                            .replace(".jpg", "")
                            .replace("'", "")
                            .replace("-", ""));
            }
        }
        return Collections.unmodifiableList(planechaseString);
    }

    public void load() {
        load("");
    }

    private static final EnumSet<FSkinTexture> ADVENTURE_BACKGROUNDS = EnumSet.of(
            ADV_BG_TEXTURE, ADV_BG_MATCH, ADV_BG_MATCH_DAY, ADV_BG_MATCH_NIGHT,
            ADV_BG_SWAMP, ADV_BG_FOREST, ADV_BG_MOUNTAIN, ADV_BG_ISLAND, ADV_BG_PLAINS,
            ADV_BG_WASTE, ADV_BG_COMMON, ADV_BG_CAVE, ADV_BG_DUNGEON, ADV_BG_CASTLE
    );

    private boolean isAdventureBackground() {
        return ADVENTURE_BACKGROUNDS.contains(this);
    }

    public static void invalidateAdventureTextures() {
        for (FSkinTexture texture : ADVENTURE_BACKGROUNDS) {
            texture.unloadAdventureBackground();
        }
        invalidateAdventureBackgroundFiles();
    }

    public static void invalidateAdventureBackgroundFiles() {
        for (FSkinTexture texture : ADVENTURE_BACKGROUNDS) {
            texture.adventureBackgroundFiles = null;
            texture.adventureBackgroundFolderKey = null;
        }
    }

    private void unloadAdventureBackground() {
        if (adventureBackgroundFile != null) {
            String path = adventureBackgroundFile.path();
            if (Forge.getAssets().manager().isLoaded(path)) {
                Forge.getAssets().manager().unload(path);
            } else {
                Texture cached = Forge.getAssets().fallback_skins().remove(path);
                if (cached != null) {
                    cached.dispose();
                }
            }
        }
        adventureBackgroundFile = null;
        texture = null;
        isloaded = false;
        hasError = false;
    }

    private String getAdventureBackgroundName() {
        int extensionIndex = filename.lastIndexOf('.');
        return extensionIndex < 0 || !filename.startsWith("adv_bg_") ? ""
                : filename.substring("adv_bg_".length(), extensionIndex);
    }

    private static String normalizeAdventureBackgroundFolder(String folder) {
        if (folder == null) {
            return null;
        }
        String normalized = folder.trim().replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty() || normalized.startsWith("/")) {
            return null;
        }
        for (String part : normalized.split("/")) {
            if (part.equals("..")) {
                return null;
            }
        }
        return normalized;
    }

    private List<String> getAdventureBackgroundFolders(String subdirectory, List<String> preferredFolders) {
        String backgroundName = getAdventureBackgroundName();
        if (backgroundName.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> folders = new ArrayList<>();
        if (preferredFolders != null) {
            for (String preferredFolder : preferredFolders) {
                String folder = normalizeAdventureBackgroundFolder(preferredFolder);
                if (folder != null && !folders.contains(folder)) {
                    folders.add(folder);
                }
            }
        }
        String normalizedSubdirectory = normalizeAdventureBackgroundFolder(subdirectory);
        if (normalizedSubdirectory != null) {
            folders.add(backgroundName + ForgeConstants.PATH_SEPARATOR + normalizedSubdirectory);
        }
        if (!folders.contains(backgroundName)) {
            folders.add(backgroundName);
        }
        return folders;
    }

    private void loadAdventureBackgroundFiles(List<String> backgroundFolders) {
        String adventureDirectory = GuiBase.getAdventureDirectory();
        if (backgroundFolders.isEmpty() || adventureDirectory == null || adventureDirectory.isEmpty()) {
            adventureBackgroundFiles = NO_FILES;
            return;
        }

        for (String folder : backgroundFolders) {
            String relativePath = ForgeConstants.SKIN_DIR + "battle_backgrounds"
                    + ForgeConstants.PATH_SEPARATOR + folder;
            for (String root : new String[]{GuiBase.getAdventureCacheDirectory(),
                    adventureDirectory, ForgeConstants.ADVENTURE_COMMON_DIR}) {
                if (root == null || root.isEmpty()) {
                    continue;
                }
                FileHandle[] files = Assets.getFileHandle(root + relativePath).list(IMAGE_FILES);
                if (files.length > 0) {
                    adventureBackgroundFiles = files;
                    return;
                }
            }
        }
        adventureBackgroundFiles = NO_FILES;
    }

    /**
     * Tries each preferred folder before the category's biome and generic folders.
     */
    public FSkinTexture getRandomAdventureBackground(String subdirectory, List<String> preferredFolders) {
        List<String> backgroundFolders = getAdventureBackgroundFolders(subdirectory, preferredFolders);
        String backgroundFolderKey = String.join("\n", backgroundFolders);
        if (!backgroundFolderKey.equals(adventureBackgroundFolderKey)) {
            if (adventureBackgroundFolderKey != null) {
                unloadAdventureBackground();
            }
            adventureBackgroundFolderKey = backgroundFolderKey;
            adventureBackgroundFiles = null;
        }
        if (adventureBackgroundFiles == null) {
            loadAdventureBackgroundFiles(backgroundFolders);
        }
        if (adventureBackgroundFiles.length == 0) {
            return this;
        }

        FileHandle selectedFile;
        do {
            selectedFile = Aggregates.random(adventureBackgroundFiles);
        } while (adventureBackgroundFiles.length > 1 && adventureBackgroundFile != null
                && selectedFile.equals(adventureBackgroundFile));
        if (selectedFile.equals(adventureBackgroundFile)) {
            return this;
        }

        unloadAdventureBackground();
        try {
            texture = Forge.getAssets().getTexture(selectedFile, false);
            if (texture != null) {
                adventureBackgroundFile = selectedFile;
                isloaded = true;
            }
        } catch (final Exception e) {
            System.err.println("Failed to load adventure background: " + selectedFile);
            e.printStackTrace();
        }
        return this;
    }

    private FileHandle getAdventureBackgroundFile() {
        String adventureDirectory = GuiBase.getAdventureDirectory();
        if (adventureDirectory == null || adventureDirectory.isEmpty()) {
            return null;
        }

        String adventureCacheDirectory = GuiBase.getAdventureCacheDirectory();
        if (adventureCacheDirectory != null && !adventureCacheDirectory.isEmpty()) {
            FileHandle cachedFile = Assets.getFileHandle(adventureCacheDirectory + ForgeConstants.SKIN_DIR + filename);
            if (cachedFile.exists()) {
                return cachedFile;
            }
        }

        // Check adventure-specific skin directory first
        String adventureSkinPath = adventureDirectory + ForgeConstants.SKIN_DIR + filename;
        FileHandle adventureFile = Assets.getFileHandle(adventureSkinPath);
        if (adventureFile.exists()) {
            return adventureFile;
        }

        // Check common adventure skin directory
        String commonSkinPath = ForgeConstants.ADVENTURE_COMMON_DIR + ForgeConstants.SKIN_DIR + filename;
        FileHandle commonFile = Assets.getFileHandle(commonSkinPath);
        if (commonFile.exists()) {
            return commonFile;
        }

        return null;
    }

    public boolean load(String planeName) {
        if (hasError)
            return false;
        if (!planeName.isEmpty()) {
            texture = null; //reset
            this.filename = ImageFetcher.getPlanechaseFilename(planeName);
        }

        FileHandle preferredFile;
        if (isPlanechaseBG) {
            preferredFile = FSkin.getCachePlanechaseFile(filename);
        } else if (isAdventureBackground()) {
            // For adventure backgrounds, check adventure directories first
            preferredFile = getAdventureBackgroundFile();
            if (preferredFile == null) {
                // Fall back to skin directories
                preferredFile = FSkin.getSkinFile(filename);
            }
        } else {
            preferredFile = FSkin.getSkinFile(filename);
        }

        if (preferredFile.exists()) {
            try {
                texture = Forge.getAssets().getTexture(preferredFile, false);
                if (texture != null)
                    isloaded = true;
            } catch (final Exception e) {
                System.err.println("Failed to load skin file: " + preferredFile);
                e.printStackTrace();
                isloaded = false;
                hasError = true;
            }
        }
        if (texture == null) {
            //use default file if can't use preferred file
            FileHandle defaultFile = FSkin.getDefaultSkinFile(filename);
            if (isPlanechaseBG) {
                ImageFetcher fetcher = GuiBase.getInterface().getImageFetcher();
                fetcher.fetchImage("PLANECHASEBG:" + planeName, () -> {
                    hasError = false;
                    load();
                });
                defaultFile = FSkin.getSkinFile(ForgeConstants.MATCH_BG_FILE);
                if (!defaultFile.exists())
                    defaultFile = FSkin.getDefaultSkinFile(ForgeConstants.MATCH_BG_FILE);
            }

            if (defaultFile.exists()) {
                try {
                    texture = Forge.getAssets().getTexture(defaultFile);
                    isloaded = true;
                } catch (final Exception e) {
                    System.err.println("Failed to load skin file: " + defaultFile);
                    e.printStackTrace();
                    isloaded = false;
                    hasError = true;
                    return false;
                }
            } else {
                System.err.println("Failed to load skin file: " + defaultFile);
                isloaded = false;
                hasError = true;
                return false;
            }
        }
        if (repeat) {
            if (texture != null)
                texture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
        }
        return true;
    }

    @Override
    public float getWidth() {
        if (!isloaded)
            load();
        if (hasError)
            return 0f;
        return texture.getWidth();
    }

    @Override
    public float getHeight() {
        if (!isloaded)
            load();
        if (hasError)
            return 0f;
        return texture.getHeight();
    }

    @Override
    public void draw(Graphics g, float x, float y, float w, float h) {
        if (!isloaded)
            load();
        if (hasError)
            return;
        if (repeat) {
            g.drawRepeatingImage(texture, x, y, w, h);
        } else {
            g.drawImage(texture, x, y, w, h);
        }
    }

    public void drawRotated(Graphics g, float x, float y, float w, float h, float rotation) {
        if (!isloaded)
            load();
        if (hasError)
            return;
        g.drawRotatedImage(texture, x, y, w, h, x + w / 2, y + h / 2, rotation);
    }

    public void drawFlipped(Graphics g, float x, float y, float w, float h) {
        if (!isloaded)
            load();
        if (hasError)
            return;
        g.drawFlippedImage(texture, x, y, w, h);
    }
}
