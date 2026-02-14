package forge.sound;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.JavaSoundAudioDevice;
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
    private volatile boolean isPlaying = false;
    private VolumeAudioDevice volumeDevice;

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
            volumeDevice = new VolumeAudioDevice();
            musicPlayer = new AdvancedPlayer(bufferedStream, volumeDevice);
            musicPlayer.setPlayBackListener(new PlaybackListener() {
                @Override
                public void playbackFinished(PlaybackEvent evt) {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            });
            new Thread(() -> {
                try {
                    isPlaying = true;
                    musicPlayer.play();
                }
                catch (Exception e){
                    e.printStackTrace();
                    valid = false;
                    isPlaying = false;
                }
            }, "Audio Music").start();
        }
        catch (Exception e) {
            e.printStackTrace();
            valid = false;
            isPlaying = false;
        }
        return valid;
    }

    @Override
    public void pause() {
        if (musicPlayer == null) { return; }

        try {
            stopped = fileStream.available();
            close();
            isPlaying = false;
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
        volumeDevice = null;
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

    @Override
    public void setVolume(float value) {
        if (volumeDevice != null) {
            volumeDevice.setVolume(value);
        }
    }

    @Override
    public boolean isPlaying() {
        return this.isPlaying;
    }

    /**
     * Audio device that scales PCM samples to apply volume control.
     * Extends JavaSoundAudioDevice so all standard audio output is preserved;
     * only the sample data is scaled before being written to the sound line.
     */
    static class VolumeAudioDevice extends JavaSoundAudioDevice {
        private volatile float volume = 1.0f;

        void setVolume(float v) {
            this.volume = v;
        }

        @Override
        protected void writeImpl(short[] samples, int offs, int len) throws JavaLayerException {
            if (volume != 1.0f) {
                for (int i = offs; i < offs + len; i++) {
                    samples[i] = (short) Math.max(Short.MIN_VALUE,
                            Math.min(Short.MAX_VALUE, (int) (samples[i] * volume)));
                }
            }
            super.writeImpl(samples, offs, len);
        }
    }
}
