package forge.gamemodes.limited;

import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.item.SealedTemplate;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.FileSection;
import forge.util.FileUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;


public class CubeImporter {
    CubeHostingPlatform platform;
    String cubeId;
    URL cubecobraUrl;

    public CubeImporter(String inputCubeId) {
        if (inputCubeId == null) {
            throw new IllegalArgumentException("Invalid cube ID or URL");
        }

        //Add support and switching for other cube hosting platforms in the future
        platform = CubeHostingPlatform.fromUrl(inputCubeId);
        cubeId = parseCubeId(inputCubeId);
    }

    /**
     * Imports a cube fetching infos from a cube hosting platform using the cube ID.
     *
     * @return a CustomLimited instance representing the imported cube, or null if the import fails
     */
    public CustomLimited importCube() {
        try {
            return switch (platform) {
                case CUBECOBRA -> {
                    cubecobraUrl = new URI("https://cubecobra.com/cube/download/forge/" + cubeId).toURL();
                    yield parseFromURL(cubecobraUrl);
                }
                case CUBEARTISAN -> null; // Not implemented yet
            };
        } catch (Exception e) {
            System.err.println("Error importing cube: " + e.getMessage());
            return null;
        }
    }

    private CustomLimited parseFromURL(final URL url) {
        // Use a generic 15-cards booster template with no rarity slots
        // Nice to have: Infos about the slots and the draft format can be found on the platform and imported via JSON api.
        List<Pair<String, Integer>> slots = SealedTemplate.genericNoSlotBooster.getSlots();

        final Map<String, List<String>> sections = FileSection.parseSections(FileUtil.readFile(url));
        final Deck deckCube = DeckSerializer.fromSections(sections);
        if (deckCube == null) {
            throw new IllegalArgumentException("Failed to parse deck from URL: " + url);
        }
        final CustomLimited cd = new CustomLimited(deckCube.getName() + "_" +
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")),
                slots);
//        cd.landSetCode = "UNF";
        cd.setNumPacks(3);
        cd.setSingleton(true);
        cd.setCustomRankingsFile("rankings_cubecobra.txt");
        cd.setCardPool(deckCube.getMain());

        // Save the cube ID in preferences as the last imported cube
        FModel.getPreferences().setPref(ForgePreferences.FPref.LAST_IMPORTED_CUBE_ID, parseCubeId(url.toString()));
        FModel.getPreferences().save();

        return cd;
    }

    /**
     * Parse the cube ID from an input string
     * If case the inputStr is in the URL format like
     * "https://cubecobra.com/cube/overview/cubeid" or
     * "https://cubecobra.com/cube/list/cubeid",
     * it returns "cubeid".
     * Otherwise, it returns the string as is considering it as a plain cube ID.
     * @param inputStr the input string which can be a cube ID or a URL
     * @return string representing the cube ID
     */
    private String parseCubeId(String inputStr) {
        String parsedStr = switch (platform) {
            case CUBECOBRA -> {
                String[] parts = inputStr.trim().split("/");
                yield parts[parts.length - 1];
            }
            case CUBEARTISAN -> // Not implemented yet, but could be similar to CubeCobra
                null;
        };

        // Check if parsedStr is alphanumeric only, allow hyphens as well since full Cube IDs can contain them
        if (parsedStr != null && !parsedStr.matches("^[a-zA-Z0-9\\-]+$")) {
            throw new IllegalArgumentException("Cube ID must contain only alphanumeric characters");
        }
        return parsedStr;
    }
}

enum CubeHostingPlatform {
    CUBECOBRA("cubecobra.com/cube/"), // CubeCobra is the only supported platform for now
    CUBEARTISAN("cubeartisan.net/cube/"); // Not implemented yet, but could be similar to CubeCobra

    private final String domain;

    CubeHostingPlatform(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return domain;
    }

    // Determine the platform based on the URL
    public static CubeHostingPlatform fromUrl(String url) {
        for (CubeHostingPlatform platform : values()) {
            if (url.contains(platform.getDomain())) {
                return platform;
            }
        }
        return CUBECOBRA; // Infer default to CubeCobra if no match found, since it's the most common platform
                          // and probably the user passed a cube ID for Cube Cobra
    }
}