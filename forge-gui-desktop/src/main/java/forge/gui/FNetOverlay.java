package forge.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.apache.commons.lang3.StringUtils;

import forge.Singletons;
import forge.gamemodes.net.ChatMessage;
import forge.gamemodes.net.IOnlineChatInterface;
import forge.gamemodes.net.IRemote;
import forge.gamemodes.net.event.MessageEvent;
import forge.gui.framework.SDisplayUtil;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.home.online.OnlineMenu;
import forge.toolbox.FLabel;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FTextField;
import forge.toolbox.SmartScroller;
import forge.util.Localizer;
import forge.view.FDialog;
import forge.view.FFrame;
import net.miginfocom.swing.MigLayout;


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
    
    // JTextPane with StyledDocument for colored system messages
    private final JTextPane txtLog = new JTextPane();
    private StyledDocument doc;
    private SimpleAttributeSet systemStyle;
    private SimpleAttributeSet playerStyle;

    private final FTextField txtInput = new FTextField.Builder().maxLength(255).build();
    private final FLabel cmdSend = new FLabel.ButtonBuilder().text(Localizer.getInstance().getMessage("lblSend")).build(); 

    //private boolean minimized = false;
    private int height = 140;
    private int width = 400;

    private IRemote remote = null;
    public void setGameClient(final IRemote remote) {
        this.remote = remote;
    }

    private final ActionListener onSend = e -> {
        final String message = txtInput.getText();
        txtInput.setText("");
        if (StringUtils.isBlank(message)) {
            return;
        }

        if (remote != null) {
            remote.send(new MessageEvent(prefs.getPref(FPref.PLAYER_NAME), message));
        }
    };

    public FTextField getTxtInput(){
        return txtInput;
    }

    /**
     * Semi-transparent overlay panel. Should be used with layered panes.
     */
    FNetOverlay() {
        // Initialize styled document for colored messages
        doc = txtLog.getStyledDocument();
        systemStyle = new SimpleAttributeSet();
        playerStyle = new SimpleAttributeSet();

        // Configure system message style: light blue RGB(100, 150, 255) - matches mobile implementation
        StyleConstants.setForeground(systemStyle, new Color(100, 150, 255));

        // Configure player message style (default foreground color from skin, with fallback)
        FSkin.SkinColor skinTextColor = FSkin.getColor(FSkin.Colors.CLR_TEXT);
        Color playerColor = (skinTextColor != null) ? skinTextColor.getColor() : Color.WHITE;
        StyleConstants.setForeground(playerStyle, playerColor);

        window.setTitle(Localizer.getInstance().getMessage("lblChat"));
        window.setVisible(false);
        window.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        window.setBorder(new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_BORDERS)));

        window.setLayout(new MigLayout("insets 0, gap 0, ax center, wrap 2"));

        // Configure JTextPane as read-only chat log
        txtLog.setOpaque(true);
        txtLog.setFocusable(true);
        txtLog.setEditable(false);
        FSkin.SkinColor skinZebraColor = FSkin.getColor(FSkin.Colors.CLR_ZEBRA);
        txtLog.setBackground((skinZebraColor != null) ? skinZebraColor.getColor() : Color.DARK_GRAY);

        FScrollPane _operationLogScroller = new FScrollPane(txtLog, false);
        _operationLogScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        new SmartScroller(_operationLogScroller).attach();
        window.add(_operationLogScroller, "pushx, hmin 24, pushy, growy, growx, gap 2px 2px 2px 0, sx 2");

        //txtInput.setBorder(new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
        window.add(txtInput, "pushx, growx, h 26px!, gap 2px 2px 2px 0");
        window.add(cmdSend, "w 60px!, h 28px!, gap 0 0 2px 0");
        
        txtInput.addActionListener(onSend);
        cmdSend.setCommand((Runnable) () -> onSend.actionPerformed(null));
    }

    public void reset() {
        setGameClient(null);
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException e) {
            // Ignore - document is being cleared
        }
        hide();
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
            try {
                doc.remove(0, doc.getLength());
                SimpleAttributeSet style = message.isSystemMessage() ? systemStyle : playerStyle;
                doc.insertString(0, message.getFormattedMessage(), style);
            } catch (BadLocationException e) {
                // Fallback to plain text if styled insert fails
                txtLog.setText(message.getFormattedMessage());
            }
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
        try {
            // Choose style based on message type
            SimpleAttributeSet style = message.isSystemMessage() ? systemStyle : playerStyle;
            String text = "\n" + message.getFormattedMessage();
            doc.insertString(doc.getLength(), text, style);
        } catch (BadLocationException e) {
            // Fallback - should not occur in normal operation
            e.printStackTrace();
        }
    }
}
