package forge.sound;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

/**
 *
 * @author agetian
 */
class AsyncSoundRegistry {
    static Map<String, Integer> soundsPlayed = new HashMap<>();

    public synchronized static void registerSound(String soundName) {
        if (soundsPlayed.containsKey(soundName)) {
            soundsPlayed.put(soundName, soundsPlayed.get(soundName) + 1);
        } else {
            soundsPlayed.put(soundName, 1);
        }
        //System.out.println("Register: Count for " + soundName + " = " + soundsPlayed.get(soundName));
    }

    public synchronized static void unregisterSound(String soundName) {
        if (soundsPlayed.containsKey(soundName) && soundsPlayed.get(soundName) > 1) {
            soundsPlayed.put(soundName, soundsPlayed.get(soundName) - 1);
        } else {
            soundsPlayed.remove(soundName);
        }
        //System.out.println("Unregister: Count for " + soundName + " = " + soundsPlayed.get(soundName));
    }

    public synchronized static boolean isRegistered(String soundName) {
        return soundsPlayed.containsKey(soundName);
    }

    public synchronized static int getNumIterations(String soundName) {
        return soundsPlayed.getOrDefault(soundName, 0);
    }
}

public class AltSoundSystem extends Thread {

    private String filename;
    private boolean isSync;

    private final int EXTERNAL_BUFFER_SIZE = 524288;
    private final int MAX_SOUND_ITERATIONS = 5;

    public AltSoundSystem(String wavfile, boolean synced) {
        filename = wavfile;
        isSync = synced;
    }

    @Override
    public void run() {
        if (isSync && AsyncSoundRegistry.getNumIterations(filename) >= 1) {
            return;
        }
        if (AsyncSoundRegistry.getNumIterations(filename) >= MAX_SOUND_ITERATIONS) {
            return;
        }

        File soundFile = new File(filename);
        if (!soundFile.exists()) {
            return;
        }

        AudioInputStream audioInputStream = null;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(AudioClip.getAudioClips(soundFile));
            audioInputStream = AudioSystem.getAudioInputStream(bis);
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
            return;
        }

        AudioFormat format = audioInputStream.getFormat();
        SourceDataLine audioLine = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        Mixer.Info selectedMixer = null;

        try {
            for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(info)) {
                    selectedMixer = mixerInfo;
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage()); // print a warning but don't crash
            return;
        }

        if (selectedMixer == null)
            return;

        try {
            audioLine = AudioSystem.getSourceDataLine(format, selectedMixer);
            audioLine.open(format);
        } catch (Exception e) {
            return;
        }

        if (audioLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            float vol = FModel.getPreferences().getPrefInt(FPref.UI_VOL_SOUNDS) / 100f;
            FloatControl gain = (FloatControl) audioLine.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float) (20.0 * Math.log10(Math.max(vol, 0.0001)));
            dB = Math.max(dB, gain.getMinimum());
            dB = Math.min(dB, gain.getMaximum());
            gain.setValue(dB);
        }

        audioLine.start();
        AsyncSoundRegistry.registerSound(filename);

        int nBytesRead = 0;
        byte[] audioBufData = new byte[EXTERNAL_BUFFER_SIZE];

        try {
            while (nBytesRead != -1) {
                nBytesRead = audioInputStream.read(audioBufData, 0, audioBufData.length);
                if (nBytesRead >= 0)
                    audioLine.write(audioBufData, 0, nBytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } finally {
            audioLine.drain();
            audioLine.close();
            try {
                audioInputStream.close();
            } catch (IOException e) {
                // Can't do much if closing it fails.
            }
            AsyncSoundRegistry.unregisterSound(filename);
        }
    }
}
