/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package forge.sound;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 *
 * @author agetian
 */
class AsyncSoundRegistry {
    static Map<String, Integer> soundsPlayed = new HashMap<String, Integer>();

    public static void registerSound(String soundName) {
	if (soundsPlayed.containsKey(soundName)) {
	    soundsPlayed.put(soundName, soundsPlayed.get(soundName) + 1);
	} else {
	    soundsPlayed.put(soundName, 1);
	}
    }

    public static void unregisterSound(String soundName) {
	if (soundsPlayed.containsKey(soundName) && soundsPlayed.get(soundName) != 1) {
	    soundsPlayed.put(soundName, soundsPlayed.get(soundName) - 1);
	} else {
	    soundsPlayed.remove(soundName);
	}
    }

    public static boolean isRegistered(String soundName) {
	return soundsPlayed.containsKey(soundName);
    }

    public static int getNumIterations(String soundName) {
	return soundsPlayed.containsKey(soundName) ? soundsPlayed.get(soundName) : 0;
    }
}

public class AsyncSoundPlayer extends Thread { 
 
    private String filename;
    private boolean isSync;
 
    private final int EXTERNAL_BUFFER_SIZE = 524288;
    private final int MAX_SOUND_ITERATIONS = 5;
 
    public AsyncSoundPlayer(String wavfile, boolean synced) { 
        filename = wavfile;
	isSync = synced;
    } 
 
    public void run() { 
	if (isSync && AsyncSoundRegistry.isRegistered(filename)) {
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
	    AsyncSoundRegistry.unregisterSound(filename);
        } 
    } 
} 
