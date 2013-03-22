package forge.control.input;

import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class InputLockUI extends Input {
    private static final long serialVersionUID = 5777143577098597374L;
    
    public void showMessage() {
        ButtonUtil.disableAll();
        CMatchUI.SINGLETON_INSTANCE.showMessage("Waiting for actions...");
    }

}
