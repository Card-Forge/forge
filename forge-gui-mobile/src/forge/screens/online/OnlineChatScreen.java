package forge.screens.online;

import java.util.ArrayList;

import forge.model.FModel;
import forge.net.IOnlineChatInterface;
import forge.net.IRemote;
import forge.net.event.MessageEvent;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.FScreen;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FChoiceList;
import forge.toolbox.FTextField;
import forge.util.Utils;

public class OnlineChatScreen extends FScreen implements IOnlineChatInterface {
    private static final float PADDING = Utils.scale(5);

    private IRemote gameClient;
    private final ForgePreferences prefs = FModel.getPreferences();
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

        addMessage(message);

        if (gameClient != null) {
            gameClient.send(new MessageEvent(prefs.getPref(FPref.PLAYER_NAME), message));
        }
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

    @Override
    public void setGameClient(IRemote gameClient0) {
        gameClient = gameClient0;
    }

    @Override
    public void addMessage(String message) {
        lstLog.addItem(message);
        lstLog.scrollToBottom();
    }
}
