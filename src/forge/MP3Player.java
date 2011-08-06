
package forge;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javazoom.jl.player.Player;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;


/**
 * 
 * @author shaines
 */
public class MP3Player implements NewConstants {
    
    private Player      player;
    private InputStream is;
    private File        file;
    
    /** Creates a new instance of MP3Player */
    public MP3Player(String filename) {
        File base = ForgeProps.getFile(SOUND_BASE);
        file = new File(base, filename);
        if(GuiDisplay3.playsoundCheckboxForMenu.isSelected() && file.exists()) {
            try {
                // Create an InputStream to the file			
                is = new FileInputStream(file);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public void play() {
        if(GuiDisplay3.playsoundCheckboxForMenu.isSelected() && file.exists()) {
            try {
                player = new Player(is);
                PlayerThread pt = new PlayerThread();
                pt.start();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    class PlayerThread extends Thread {
        @Override
        public void run() {
            try {
                player.play();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}
