package forge.control.input;

import forge.view.ButtonUtil;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class InputLockUI extends InputBase {
    private static final long serialVersionUID = 5777143577098597374L;
    
    public void showMessage() {
        ButtonUtil.disableAll();
        //showMessage("Waiting for actions...");
    }
    
    @Override
    public String toString() {
        return "lockUI"; 
    }

}
