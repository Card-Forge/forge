package forge.screens.online;

import java.util.ArrayList;

import forge.screens.FScreen;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FChoiceList;
import forge.toolbox.FTextField;
import forge.util.Utils;

public class OnlineChatScreen extends FScreen {
    private static final float PADDING = Utils.scale(5);

    private final FChoiceList<String> lstLog = add(new FChoiceList<String>(new ArrayList<String>()));
    private final FTextField txtSendMessage = add(new FTextField());

    public OnlineChatScreen() {
        super(null, OnlineMenu.getMenu());

        txtSendMessage.setGhostText("Enter message to send");
        txtSendMessage.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                sendMessage();
            }
        });
    }
    
    private void sendMessage() {
        String message = txtSendMessage.getText();
        if (message.isEmpty()) { return; }

        txtSendMessage.setText("");

        lstLog.addItem(message);
        lstLog.scrollToBottom();
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float x = PADDING;
        float y = startY + PADDING;
        float w = width - 2 * PADDING;
        float h = height - y - txtSendMessage.getHeight() - 2 * PADDING;
        lstLog.setBounds(x, y, w, h);
        y += h + PADDING;
        txtSendMessage.setBounds(x, y, w, txtSendMessage.getHeight());
    }
}
