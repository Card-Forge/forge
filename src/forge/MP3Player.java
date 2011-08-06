package forge;

import javazoom.jl.player.*;

//Import the Java classes
import java.io.*;

import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * 
 * @author shaines
 */
public class MP3Player implements NewConstants{

	private Player player;
	private InputStream is;

	/** Creates a new instance of MP3Player */
	public MP3Player(String filename) {
		try {
			// Create an InputStream to the file
			File base = ForgeProps.getFile(SOUND_BASE);
			File file = new File(base, filename);
			is = new FileInputStream(file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void play() {
		try {
			player = new Player(is);
			PlayerThread pt = new PlayerThread();
			pt.start();			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	class PlayerThread extends Thread {
		public void run() {
			try {
				player.play();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
