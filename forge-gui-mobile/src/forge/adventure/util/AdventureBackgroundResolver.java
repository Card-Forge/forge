package forge.adventure.util;

import forge.adventure.character.EnemySprite;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.scene.DuelScene;
import forge.adventure.scene.GameScene;
import forge.adventure.stage.MapStage;
import forge.assets.FSkinTexture;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        EnemySprite enemy = DuelScene.instance().getEnemy();
        String pointName = pointOfInterest == null ? null : pointOfInterest.getData().name;
        if (enemy != null) {
            addIfPresent(preferredFolders, enemy.battleBackground);
            addNamedFolder(preferredFolders, "encounter", pointName, enemy.getData().name);
            addIfPresent(preferredFolders, enemy.getData().battleBackground);
            addNamedFolder(preferredFolders, "enemy", enemy.getData().name);
        }

        MapStage mapStage = MapStage.getInstance();
        if (mapStage.isInMap()) {
            addIfPresent(preferredFolders, mapStage.getBattleBackground());
            String mapName = mapName(mapStage.getCurrentMap());
            addMapFolder(preferredFolders, pointName, mapName);
            addMapFolder(preferredFolders, null, mapName);
        }
        if (pointOfInterest != null) {
            addIfPresent(preferredFolders, pointOfInterest.getData().battleBackground);
            addNamedFolder(preferredFolders, "poi", pointName);
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

    private static void addNamedFolder(List<String> backgrounds, String type, String... names) {
        StringBuilder folder = new StringBuilder(type);
        for (String name : names) {
            String slug = slug(name);
            if (slug.isEmpty()) {
                return;
            }
            folder.append('/').append(slug);
        }
        backgrounds.add(folder.toString());
    }

    private static void addMapFolder(List<String> backgrounds, String pointName, String mapName) {
        if (mapName.isEmpty()) {
            return;
        }
        String pointSlug = slug(pointName);
        backgrounds.add("map/" + (pointSlug.isEmpty() ? "" : pointSlug + "/") + mapName);
    }

    private static String slug(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("['‘’ʼ]", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }

    private static String mapName(String path) {
        if (path == null) {
            return "";
        }
        String value = path.replace('\\', '/');
        int mapRoot = value.indexOf("maps/map/");
        if (mapRoot >= 0) {
            value = value.substring(mapRoot + "maps/map/".length());
        }
        value = value.replaceFirst("(?i)\\.tmx$", "");
        StringBuilder name = new StringBuilder();
        for (String segment : value.split("/")) {
            String slug = slug(segment);
            if (!slug.isEmpty()) {
                if (name.length() > 0) {
                    name.append('/');
                }
                name.append(slug);
            }
        }
        return name.toString();
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
