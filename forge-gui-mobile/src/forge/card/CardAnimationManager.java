package forge.card;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;

import forge.localinstance.properties.ForgeConstants;

/**
 * Manages animated card art loops for cards in the mobile/adventure LibGDX UI.
 */
public final class CardAnimationManager {

    private static final CardAnimationManager INSTANCE = new CardAnimationManager();

    private final Map<String, File[]> animationFiles = new ConcurrentHashMap<>();
    private final Map<String, Texture[]> animationTextures = new ConcurrentHashMap<>();
    private final Set<Texture> allTextures = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private boolean initialized = false;

    private CardAnimationManager() {
    }

    public static CardAnimationManager getInstance() {
        return INSTANCE;
    }

    public synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Scan potential locations for animated card folders
        scanDirectory(new File(ForgeConstants.RES_DIR, "animated_cards"));
        scanDirectory(new File(ForgeConstants.CACHE_CARD_PICS_DIR, "animated_cards"));
        scanDirectory(new File(ForgeConstants.CACHE_DIR, "animated_cards"));
        scanDirectory(new File("res/animated_cards"));
        scanDirectory(new File("../res/animated_cards"));
        scanDirectory(new File("D:/projects/forge/res/animated_cards"));
        scanDirectory(new File("D:/projects/forge/forge-installer/target/forge-installer-2.0.15-SNAPSHOT/res/animated_cards"));

        if (!animationFiles.isEmpty()) {
            System.out.println("[CardAnimationManager-Mobile] Discovered animated cards: " + animationFiles.keySet());
        }
    }

    private static String normalize(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private void scanDirectory(File animDir) {
        if (animDir == null || !animDir.exists() || !animDir.isDirectory()) {
            return;
        }

        File[] cardFolders = animDir.listFiles(File::isDirectory);
        if (cardFolders == null) {
            return;
        }

        for (File folder : cardFolders) {
            String cardName = folder.getName().toLowerCase().trim();
            String normKey = normalize(folder.getName());
            if (animationFiles.containsKey(cardName) || animationFiles.containsKey(normKey)) {
                continue;
            }

            File[] frameFiles = folder.listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".jpeg");
            });

            if (frameFiles == null || frameFiles.length == 0) {
                continue;
            }

            Arrays.sort(frameFiles, Comparator.comparing(File::getName));
            animationFiles.put(cardName, frameFiles);
            animationFiles.put(normKey, frameFiles);
            System.out.println("[CardAnimationManager-Mobile] Registered animation files for card '" + folder.getName() + "' with " + frameFiles.length + " frames.");
        }
    }

    public static boolean hasAnimation(String cardName) {
        if (cardName == null) {
            return false;
        }
        if (!INSTANCE.initialized) {
            INSTANCE.initialize();
        }
        String key = cardName.toLowerCase().trim();
        return INSTANCE.animationFiles.containsKey(key) || INSTANCE.animationFiles.containsKey(normalize(key));
    }

    public static boolean isAnimationTexture(Texture texture) {
        return texture != null && INSTANCE.allTextures.contains(texture);
    }

    public static Texture getCurrentFrame(String cardName) {
        if (cardName == null) {
            return null;
        }
        if (!INSTANCE.initialized) {
            INSTANCE.initialize();
        }

        String key = cardName.toLowerCase().trim();
        Texture[] textures = INSTANCE.animationTextures.get(key);
        if (textures == null) {
            textures = INSTANCE.animationTextures.get(normalize(key));
        }

        if (textures == null) {
            File[] files = INSTANCE.animationFiles.get(key);
            if (files == null) {
                files = INSTANCE.animationFiles.get(normalize(key));
            }
            if (files == null || files.length == 0) {
                return null;
            }

            textures = loadTextures(files);
            if (textures == null || textures.length == 0) {
                return null;
            }

            INSTANCE.animationTextures.put(key, textures);
            INSTANCE.animationTextures.put(normalize(key), textures);
        }

        if (textures.length == 0) {
            return null;
        }

        // 24 FPS calculation
        long now = System.currentTimeMillis();
        int idx = (int) Math.floorMod(now * 24L / 1000L, textures.length);
        Texture frame = textures[idx];

        // Request next frame rendering so the animation loops continuously
        if (Gdx.graphics != null) {
            Gdx.graphics.requestRendering();
        }

        return frame;
    }

    private static synchronized Texture[] loadTextures(File[] files) {
        Texture[] textures = new Texture[files.length];
        int loaded = 0;
        for (int i = 0; i < files.length; i++) {
            try {
                FileHandle fh = Gdx.files.absolute(files[i].getAbsolutePath());
                Texture t = new Texture(fh);
                t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                textures[i] = t;
                INSTANCE.allTextures.add(t);
                loaded++;
            } catch (Exception ex) {
                System.err.println("[CardAnimationManager-Mobile] Error loading texture frame " + files[i].getName() + ": " + ex.getMessage());
            }
        }

        if (loaded == 0) {
            return null;
        }
        System.out.println("[CardAnimationManager-Mobile] Successfully loaded " + loaded + " texture frames into GPU.");
        return textures;
    }
}
