package forge.sound;

import forge.util.MyRandom;

import java.io.File;

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

    public String getSubDir() {
        return this.subDir;
    }

    public String getRandomFilename() {
        if (filenames == null || isInvalidated) {
            try {
                File musicDir = SoundSystem.findMusicDirectory(this);
                if(musicDir != null)
                    filenames = musicDir.listFiles(SoundSystem.PLAYABLE_AUDIO);
                else
                    filenames = new File[0];

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
        else if (filenames.length == 1) {
            mostRecentTrackIdx = 0;
        }
        else {
            int index = MyRandom.getRandom().nextInt(filenames.length - 1);
            if(index >= mostRecentTrackIdx)
                index += 1;
            mostRecentTrackIdx = index;
        }

        return filenames[mostRecentTrackIdx].getPath();
    }
}
