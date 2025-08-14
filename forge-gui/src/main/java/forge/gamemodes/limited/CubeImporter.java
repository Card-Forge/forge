package forge.gamemodes.limited;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class CubeImporter {

    public CustomLimited importFromCubeCobra(String urlInputString) {
        // Extract cube ID from URL (e.g., https://cubecobra.com/cube/overview/cubeid)
        //                           or    https://cubecobra.com/cube/list/cubeid
        String cubeId = extractCubeId(urlInputString);
        if (cubeId == null) {
            return null;
        }

        try {
            URL cubecobraUrl;
            try {
                cubecobraUrl = new URI("https://cubecobra.com/cube/download/forge/" + cubeId).toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

            return CustomLimited.parseFromURL(cubecobraUrl);

        } catch (Exception e) {
            System.err.println("Error importing cube: " + e.getMessage());
            return null;
        }
    }

    // Extracts the cube ID from a CubeCobra URL.
    private String extractCubeId(String url) {
        if (url.contains("cubecobra.com")) {
            String[] parts = url.split("/");
            return parts[parts.length - 1];
        }
        return url;
    }
}
