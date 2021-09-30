package forge.sound;

import java.io.File;
import java.io.FilenameFilter;

import forge.localinstance.properties.ForgeConstants;
import forge.util.MyRandom;

public enum MusicPlaylist {
    MENUS("menus/"),
    MATCH("match/");

    private final String subDir;
    private int mostRecentTrackIdx = -1;
    private String[] filenames;

    MusicPlaylist(String subDir0) {
        subDir = subDir0;
    }

    public String getRandomFilename() {
        if (filenames == null) {
            try {
                FilenameFilter filter = new FilenameFilter(){
                    @Override
                    public boolean accept(File file, String name) {
                        return name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a");
                    }
                };
                filenames = new File(ForgeConstants.MUSIC_DIR + subDir).list(filter);
                if (filenames == null) filenames = new String[0];
            }
            catch (Exception e) {
                e.printStackTrace();
                filenames = new String[0];
            }
        }

        if (filenames.length == 0) { return null; }

        if (filenames.length == 1) {
            mostRecentTrackIdx = 0;
        }
        else { //determine filename randomly from playlist
            int newIndex;
            do {
                newIndex = MyRandom.getRandom().nextInt(filenames.length);
            } while (newIndex == mostRecentTrackIdx); //ensure a different track is chosen than the last one

            mostRecentTrackIdx = newIndex;
        }

        return ForgeConstants.MUSIC_DIR + subDir + filenames[mostRecentTrackIdx];
    }
}
