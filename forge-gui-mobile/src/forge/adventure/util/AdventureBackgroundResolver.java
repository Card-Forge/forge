package forge.adventure.util;

import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.scene.DuelScene;
import forge.adventure.scene.GameScene;
import forge.assets.FSkinTexture;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves Adventure battle backgrounds from the most specific configured folder
 * to the existing category and single-image fallbacks.
 */
public final class AdventureBackgroundResolver {
    private AdventureBackgroundResolver() {
    }

    public static FSkinTexture getBattleBackground() {
        GameScene gameScene = GameScene.instance();
        PointOfInterest pointOfInterest = gameScene.getMapPOI();
        String location = gameScene.getAdventurePlayerLocation(false, true);
        String biome = pointOfInterest == null ? null
                : gameScene.getBiomeByPosition(pointOfInterest.getPosition());

        List<String> preferredFolders = new ArrayList<>();
        addIfPresent(preferredFolders, DuelScene.instance().getBattleBackground());
        if (pointOfInterest != null) {
            addIfPresent(preferredFolders, pointOfInterest.getData().battleBackground);
        }

        return getLocationBackground(location).getRandomAdventureBackground(
                "waste".equals(biome) ? "colorless" : biome,
                preferredFolders);
    }

    private static void addIfPresent(List<String> backgrounds, String background) {
        if (background != null && !background.isEmpty()) {
            backgrounds.add(background);
        }
    }

    private static FSkinTexture getLocationBackground(String location) {
        return switch (location) {
            case "green" -> FSkinTexture.ADV_BG_FOREST;
            case "black" -> FSkinTexture.ADV_BG_SWAMP;
            case "red" -> FSkinTexture.ADV_BG_MOUNTAIN;
            case "blue" -> FSkinTexture.ADV_BG_ISLAND;
            case "white" -> FSkinTexture.ADV_BG_PLAINS;
            case "waste" -> FSkinTexture.ADV_BG_WASTE;
            case "cave" -> FSkinTexture.ADV_BG_CAVE;
            case "dungeon" -> FSkinTexture.ADV_BG_DUNGEON;
            case "castle" -> FSkinTexture.ADV_BG_CASTLE;
            default -> FSkinTexture.ADV_BG_COMMON;
        };
    }
}
