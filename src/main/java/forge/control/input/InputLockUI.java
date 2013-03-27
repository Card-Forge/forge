package forge.control.input;

import java.util.concurrent.atomic.AtomicInteger;

import forge.FThreads;
import forge.view.ButtonUtil;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class InputLockUI extends InputBase {
    private static final long serialVersionUID = 5777143577098597374L;
    
    private final AtomicInteger iCall = new AtomicInteger();
    
    public void showMessage() {
        int ixCall = 1 + iCall.getAndIncrement();
        FThreads.delay(500, new InputUpdater(ixCall));
    }
    
    @Override
    public String toString() {
        return "lockUI"; 
    }
    
    private class InputUpdater implements Runnable {
        final int ixCall;
        
        public InputUpdater(final int idxCall) {
            ixCall = idxCall;
        }
        
        @Override
        public void run() {
            if ( ixCall != iCall.get() || !isActive()) // cancel the message if it's not from latest call or input is gone already 
                return;
            FThreads.invokeInEDT(showMessageFromEdt);
        }
    };
    
    private final Runnable showMessageFromEdt = new Runnable() {
        
        @Override
        public void run() {
            ButtonUtil.disableAll();
            showMessage("Waiting for actions...");
        }
    };

}
