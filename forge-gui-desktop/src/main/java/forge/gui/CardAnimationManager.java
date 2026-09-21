package forge.gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.Timer;

import forge.localinstance.properties.ForgeConstants;

/**
 * Manages animated card art loops for cards that have animation frame assets.
 */
public final class CardAnimationManager {

    private static final CardAnimationManager INSTANCE = new CardAnimationManager();

    private final Map<String, BufferedImage[]> cardAnimations = new ConcurrentHashMap<>();
    private final Set<JComponent> activeComponents = Collections.newSetFromMap(new WeakHashMap<>());
    private int currentFrameIndex = 0;
    private Timer animationTimer;
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

        if (!cardAnimations.isEmpty()) {
            System.out.println("[CardAnimationManager] Loaded animated cards: " + cardAnimations.keySet());

            // 24 FPS timer (~41ms per tick)
            animationTimer = new Timer(41, e -> {
                currentFrameIndex++;
                synchronized (activeComponents) {
                    for (JComponent comp : activeComponents) {
                        if (comp != null && comp.isShowing()) {
                            comp.repaint();
                        }
                    }
                }
            });
            animationTimer.start();
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
            if (cardAnimations.containsKey(cardName) || cardAnimations.containsKey(normKey)) {
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
            BufferedImage[] frames = new BufferedImage[frameFiles.length];
            int loaded = 0;

            for (int i = 0; i < frameFiles.length; i++) {
                try {
                    frames[i] = ImageIO.read(frameFiles[i]);
                    if (frames[i] != null) {
                        loaded++;
                    }
                } catch (Exception ex) {
                    System.err.println("[CardAnimationManager] Error loading frame " + frameFiles[i].getName() + ": " + ex.getMessage());
                }
            }

            if (loaded > 0) {
                cardAnimations.put(cardName, frames);
                cardAnimations.put(normKey, frames);
                System.out.println("[CardAnimationManager] Registered animation for card '" + folder.getName() + "' with " + loaded + " frames.");
            }
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
        return INSTANCE.cardAnimations.containsKey(key) || INSTANCE.cardAnimations.containsKey(normalize(key));
    }

    public static BufferedImage getCurrentFrame(String cardName) {
        if (cardName == null) {
            return null;
        }
        if (!INSTANCE.initialized) {
            INSTANCE.initialize();
        }
        String key = cardName.toLowerCase().trim();
        BufferedImage[] frames = INSTANCE.cardAnimations.get(key);
        if (frames == null) {
            frames = INSTANCE.cardAnimations.get(normalize(key));
        }
        if (frames == null || frames.length == 0) {
            return null;
        }
        int idx = Math.floorMod(INSTANCE.currentFrameIndex, frames.length);
        return frames[idx];
    }

    public static void register(JComponent component, String cardName) {
        if (component == null) {
            return;
        }
        if (!INSTANCE.initialized) {
            INSTANCE.initialize();
        }
        if (hasAnimation(cardName)) {
            synchronized (INSTANCE.activeComponents) {
                INSTANCE.activeComponents.add(component);
            }
        }
    }

    public static void unregister(JComponent component) {
        if (component == null) {
            return;
        }
        synchronized (INSTANCE.activeComponents) {
            INSTANCE.activeComponents.remove(component);
        }
    }
}
