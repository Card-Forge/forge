package forge.gui;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.ScrollPaneConstants;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;

import forge.Singletons;
import forge.gui.framework.SDisplayUtil;
import forge.model.FModel;
import forge.net.ChatMessage;
import forge.net.IOnlineChatInterface;
import forge.net.IRemote;
import forge.net.event.MessageEvent;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.home.online.OnlineMenu;
import forge.toolbox.FLabel;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FTextArea;
import forge.toolbox.FTextField;
import forge.toolbox.SmartScroller;
import forge.view.FDialog;
import forge.view.FFrame;


public enum FNetOverlay implements IOnlineChatInterface {
    SINGLETON_INSTANCE;

    private static final String COORD_DELIM = ",";
    private final ForgePreferences prefs = FModel.getPreferences();
    private boolean hasBeenShown, locLoaded;

    @SuppressWarnings("serial")
    private final FDialog window = new FDialog(false, true, "0") {
        @Override
        public void setLocationRelativeTo(Component c) {
            //don't change location this way if dialog has already been shown or location was loaded from preferences
            if (hasBeenShown || locLoaded) { return; }
            super.setLocationRelativeTo(c);
        }

        @Override
        public void setVisible(boolean b0) {
            if (isVisible() == b0) { return; }
            if (!b0 && hasBeenShown) {
                //update preference before hiding window, as otherwise its location will be 0,0
                prefs.setPref(FPref.CHAT_WINDOW_LOC,
                        getX() + COORD_DELIM + getY() + COORD_DELIM +
                        getWidth() + COORD_DELIM + getHeight());
                prefs.save();
            }
            super.setVisible(b0);
            if (b0) {
                hasBeenShown = true;
            }
            OnlineMenu.chatItem.setState(b0);
        }
    };
    public FDialog getWindow() {
        return this.window;
    }
    
    private final FTextArea txtLog = new FTextArea();
    private final FTextField txtInput = new FTextField.Builder().maxLength(60).build();
    private final FLabel cmdSend = new FLabel.ButtonBuilder().text("Send").build(); 

    //private boolean minimized = false;
    private int height = 140;
    private int width = 400;

    private IRemote remote = null;
    public void setGameClient(final IRemote remote) {
        this.remote = remote;
    }

    private final ActionListener onSend = new ActionListener() {
        @Override public final void actionPerformed(final ActionEvent e) {
            final String message = txtInput.getText();
            txtInput.setText("");
            if (StringUtils.isBlank(message)) {
                return;
            }

            if (remote != null) {
                remote.send(new MessageEvent(prefs.getPref(FPref.PLAYER_NAME), message));
            }
        }
    };

    /**
     * Semi-transparent overlay panel. Should be used with layered panes.
     */
    private FNetOverlay() {
        window.setTitle("Chat");
        window.setVisible(false);
        window.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        window.setBorder(new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_BORDERS)));

        window.setLayout(new MigLayout("insets 0, gap 0, ax center, wrap 2"));

        // Block all input events below the overlay
        txtLog.setOpaque(true);
        txtLog.setFocusable(true);
        txtLog.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));

        FScrollPane _operationLogScroller = new FScrollPane(txtLog, false);
        _operationLogScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        new SmartScroller(_operationLogScroller).attach();
        window.add(_operationLogScroller, "pushx, hmin 24, pushy, growy, growx, gap 2px 2px 2px 0, sx 2");

        //txtInput.setBorder(new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
        window.add(txtInput, "pushx, growx, h 26px!, gap 2px 2px 2px 0");
        window.add(cmdSend, "w 60px!, h 28px!, gap 0 0 2px 0");
        
        txtInput.addActionListener(onSend);
        cmdSend.setCommand(new Runnable() { @Override public void run() { onSend.actionPerformed(null); } });
    }

    public void hide() {
        window.setVisible(false);
    }

    public void show() {
        show(null);
    }
    public void show(final ChatMessage message) { 
        if (!hasBeenShown) {
            hasBeenShown = true;
            loadLocation();
            window.getTitleBar().addMouseListener(new FMouseAdapter() {
                @Override
                public void onLeftDoubleClick(MouseEvent e) {
                    window.setVisible(false); //hide window if titlebar double-clicked
                }
            });
        }
        if (message != null) {
            txtLog.setText(message.getFormattedMessage());
        }
        window.setVisible(true);
    }

    private void loadLocation() {
        String value = prefs.getPref(FPref.CHAT_WINDOW_LOC);
        if (value.length() > 0) {
            String[] coords = value.split(COORD_DELIM);
            if (coords.length == 4) {
                try {
                    int x = Integer.parseInt(coords[0]);
                    int y = Integer.parseInt(coords[1]);
                    int w = Integer.parseInt(coords[2]);
                    int h = Integer.parseInt(coords[3]);

                    //ensure the window is accessible
                    int centerX = x + w / 2;
                    int centerY = y + h / 2;
                    Rectangle screenBounds = SDisplayUtil.getScreenBoundsForPoint(new Point(centerX, centerY)); 
                    if (centerX < screenBounds.x) {
                        x = screenBounds.x;
                    }
                    else if (centerX > screenBounds.x + screenBounds.width) {
                        x = screenBounds.x + screenBounds.width - w;
                        if (x < screenBounds.x) {
                            x = screenBounds.x;
                        }
                    }
                    if (centerY < screenBounds.y) {
                        y = screenBounds.y;
                    }
                    else if (centerY > screenBounds.y + screenBounds.height) {
                        y = screenBounds.y + screenBounds.height - h;
                        if (y < screenBounds.y) {
                            y = screenBounds.y;
                        }
                    }
                    window.setBounds(x, y, w, h);
                    locLoaded = true;
                    return;
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            prefs.setPref(FPref.CHAT_WINDOW_LOC, ""); //clear value if invalid
            prefs.save();
        }

        //fallback default size
        FFrame mainFrame = Singletons.getView().getFrame();
        int w = Math.max(width, (int)(mainFrame.getWidth() * 0.25f));
        int x = mainFrame.getWidth() - w;
        int y = mainFrame.getHeight() - height;
        window.setBounds(x, y, w, height);
    }

    @Override
    public void addMessage(final ChatMessage message) {
        txtLog.append("\n" + message.getFormattedMessage());
    }
}
