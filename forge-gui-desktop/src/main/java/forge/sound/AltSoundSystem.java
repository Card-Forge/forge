package forge.sound;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author agetian
 */
class AsyncSoundRegistry {
    static Map<String, Integer> soundsPlayed = new HashMap<String, Integer>();

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
        return soundsPlayed.containsKey(soundName) ? soundsPlayed.get(soundName) : 0;
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
            audioInputStream = AudioSystem.getAudioInputStream(soundFile);
        } catch (UnsupportedAudioFileException e) { 
            e.printStackTrace();
            return;
        } catch (IOException e) { 
            e.printStackTrace();
            return;
        } 

        AudioFormat format = audioInputStream.getFormat();
        SourceDataLine audioLine = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        try { 
            audioLine = (SourceDataLine) AudioSystem.getLine(info);
            audioLine.open(format);
        } catch (Exception e) { 
            return;
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
