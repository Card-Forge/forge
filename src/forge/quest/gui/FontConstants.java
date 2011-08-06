/**
 * FontConstants.java
 * 
 * Created on 10.02.2011
 */

package forge.quest.gui;


import java.awt.Font;


/**
 * <p>
 * The class FontConstants.
 * </p>
 * 
 * @version V0.0 10.02.2011
 * @author Clemens Koza
 */
public class FontConstants {
    public static final String DIALOG, DIALOG_INPUT, MONOSPACED, SANS_SERIF, SERIF;
    
    static {
        String dialog = "Dialog";
        String dialogInput = "DialogInput";
        String monospaced = "Monospaced";
        String sansSerif = "SansSerif";
        String serif = "Serif";
        try {
            dialog = Font.DIALOG;
            dialogInput = Font.DIALOG_INPUT;
            monospaced = Font.MONOSPACED;
            sansSerif = Font.SANS_SERIF;
            serif = Font.SERIF;
        } catch(NoSuchFieldError ex) {}
        
        DIALOG = dialog;
        DIALOG_INPUT = dialogInput;
        MONOSPACED = monospaced;
        SANS_SERIF = sansSerif;
        SERIF = serif;
    }
}
