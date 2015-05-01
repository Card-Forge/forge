package forge.sound;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

public class AudioMusic implements IAudioMusic {
    private AdvancedPlayer musicPlayer;
    private FileInputStream fileStream;
    private BufferedInputStream bufferedStream;
    private boolean canResume;
    private String filename;
    private int total;
    private int stopped;
    private boolean valid;
    private Runnable onComplete;

    public AudioMusic(String filename0) {
        filename = filename0;
    }

    @Override
    public void play(final Runnable onComplete0) {
        onComplete = onComplete0;
        play(-1);
    }

    private boolean play(int pos) {
        valid = true;
        canResume = false;
        try {
            fileStream = new FileInputStream(filename);
            total = fileStream.available();
            if (pos > -1) {
                fileStream.skip(pos);
            }
            bufferedStream = new BufferedInputStream(fileStream);
            musicPlayer = new AdvancedPlayer(bufferedStream);
            musicPlayer.setPlayBackListener(new PlaybackListener() {
                @Override
                public void playbackFinished(PlaybackEvent evt) {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            });
            new Thread(new Runnable(){
                @Override public void run(){
                    try {
                        musicPlayer.play();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        valid = false;
                    }
                }
            }).start();
        }
        catch (Exception e) {
            e.printStackTrace();
            valid = false;
        }
        return valid;
    }

    @Override
    public void pause() {
        if (musicPlayer == null) { return; }

        try {
            stopped = fileStream.available();
            close();
            if (valid) {
                canResume = true;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void close() {
        musicPlayer.setPlayBackListener(null); //prevent firing if closed
        musicPlayer.close();
        fileStream = null;
        bufferedStream = null;
        musicPlayer = null;
    }

    @Override
    public void resume() {
        if (!canResume) { return; }

        if (play(total - stopped)) {
            canResume = false;
        }
    }

    @Override
    public void stop() {
        if (musicPlayer == null) { return; }

        musicPlayer.setPlayBackListener(null); //prevent firing if stopped manually
        musicPlayer.stop();
    }

    @Override
    public void dispose() {
        if (musicPlayer == null) { return; }

        close();
        canResume = false;
    }
}
