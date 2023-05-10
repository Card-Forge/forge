package forge.sound;

import java.io.File;
import java.io.FilenameFilter;

import forge.util.MyRandom;

public enum MusicPlaylist {
    BLACK   ("black/"),
    BLUE    ("blue/"),
    RED     ("red/"),
    GREEN   ("green/"),
    WHITE   ("white/"),
    CASTLE  ("castle/"),
    CAVE    ("cave/"),
    TOWN    ("town/"),
    BOSS    ("boss/"),
    MENUS   ("menus/"),
    MATCH   ("match/");

    private final String subDir;
    private int mostRecentTrackIdx = -1;
    private String[] filenames;
    private static boolean isInvalidated = false;

    MusicPlaylist(String subDir0) {
        subDir = subDir0;
    }

    public static void invalidateMusicPlaylist() {
        isInvalidated = true;
    }

    public String getRandomFilename() {
        if (filenames == null || isInvalidated) {
            try {
                FilenameFilter filter = new FilenameFilter(){
                    @Override
                    public boolean accept(File file, String name) {
                        return name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a");
                    }
                };
                filenames = new File(SoundSystem.instance.getMusicDirectory() + subDir).list(filter);
                if (filenames == null) filenames = new String[0];
            }
            catch (Exception e) {
                e.printStackTrace();
                filenames = new String[0];
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

        return SoundSystem.instance.getMusicDirectory() + subDir + filenames[mostRecentTrackIdx];
    }

    public String getNewRandomFilename() {
        String[] music;
        try {
            FilenameFilter filter = new FilenameFilter(){
                @Override
                public boolean accept(File file, String name) {
                    return name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a");
                }
            };
            music = new File(SoundSystem.instance.getMusicDirectory() + subDir).list(filter);
            if (music == null)
                return null;
        }
        catch (Exception e) {
            return null;
        }
        if (music.length == 0)
            return null;

        int index = MyRandom.getRandom().nextInt(music.length);
        return SoundSystem.instance.getMusicDirectory() + subDir + music[index];
    }
}
