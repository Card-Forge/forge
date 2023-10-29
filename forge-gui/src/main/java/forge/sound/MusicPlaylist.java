package forge.sound;

import forge.gui.GuiBase;
import forge.localinstance.properties.ForgeConstants;
import forge.util.MyRandom;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.FilenameFilter;

public enum MusicPlaylist {
    BLACK       ("black/"),
    BLUE        ("blue/"),
    RED         ("red/"),
    GREEN       ("green/"),
    WHITE       ("white/"),
    COLORLESS   ("colorless/"),
    CASTLE      ("castle/"),
    CAVE        ("cave/"),
    TOWN        ("town/"),
    BOSS        ("boss/"),
    MENUS       ("menus/"),
    MATCH       ("match/");

    private final String subDir;
    private int mostRecentTrackIdx = -1;
    private File[] filenames;
    private static boolean isInvalidated = false;

    MusicPlaylist(String subDir0) {
        subDir = subDir0;
    }

    public static void invalidateMusicPlaylist() {
        isInvalidated = true;
    }

    public String getRandomFilename() {
        String path = SoundSystem.instance.getMusicDirectory() + subDir;
        if (filenames == null || isInvalidated) {
            try {
                FilenameFilter filter = (file, name) -> name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a");
                filenames = new File(path).listFiles(filter);
                if (GuiBase.isAdventureMode() && (filenames == null || ArrayUtils.isEmpty(filenames))) {
                    path = ForgeConstants.ADVENTURE_COMMON_MUSIC_DIR + subDir;
                    filenames = new File(path).listFiles(filter);
                }
                if (filenames == null)
                    filenames = new File[0];
            }
            catch (Exception e) {
                e.printStackTrace();
                filenames = new File[0];
            }
            isInvalidated = false;
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

        return filenames[mostRecentTrackIdx].getPath();
    }

    public String getNewRandomFilename() {
        File[] music;
        String path = SoundSystem.instance.getMusicDirectory() + subDir;
        try {
            FilenameFilter filter = (file, name) -> name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a");
            music = new File(path).listFiles(filter);
            if (GuiBase.isAdventureMode() && (music == null || ArrayUtils.isEmpty(music))) {
                path = ForgeConstants.ADVENTURE_COMMON_MUSIC_DIR + subDir;
                music = new File(path).listFiles(filter);
            }
            if (music == null)
               return null;
        }
        catch (Exception e) {
            return null;
        }
        if (music.length == 0)
            return null;

        int index = MyRandom.getRandom().nextInt(music.length);
        System.out.println("Looking up " +path + ForgeConstants.PATH_SEPARATOR + music[index]);
        return music[index].getPath();
    }
}
