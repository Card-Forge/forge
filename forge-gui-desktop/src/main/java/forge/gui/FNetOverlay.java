package forge.gui;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.ScrollPaneConstants;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;

import forge.model.FModel;
import forge.net.game.IRemote;
import forge.net.game.MessageEvent;
import forge.properties.ForgePreferences.FPref;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.toolbox.FTextArea;
import forge.toolbox.FTextField;
import forge.toolbox.SmartScroller;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum FNetOverlay {
    SINGLETON_INSTANCE;

    private final OverlayPanel pnl = new OverlayPanel();
    /** @return {@link javax.swing.JPanel} */
    public SkinnedPanel getPanel() {
        return this.pnl;
    }
    
    private final FTextArea txtLog = new FTextArea();
    private final FTextField txtInput = new FTextField.Builder().maxLength(60).build();
    private final FLabel cmdSend = new FLabel.ButtonBuilder().text("Send").build(); 

    //private boolean minimized = false;
    private int height = 120;
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
                remote.send(new MessageEvent(FModel.getPreferences().getPref(FPref.PLAYER_NAME), message));
            }
            // lobby.speak(ChatArea.Room, lobby.getGuiPlayer(), message);
        }
    };
    
    //private final int minimizedHeight = 30;
    
    /**
     * Semi-transparent overlay panel. Should be used with layered panes.
     */
    private FNetOverlay() {
        pnl.setOpaque(false);
        pnl.setVisible(false);
        pnl.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        pnl.setBorder(new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_BORDERS)));

        pnl.setLayout(new MigLayout("insets 0, gap 0, ax center, wrap 2"));
//        pnl.add(new FLabel.Builder().text("Loading new game...").fontSize(22).build(), "h 40px!, align center");

        // Block all input events below the overlay

        txtLog.setOpaque(true);
        txtLog.setFocusable(true);
        txtLog.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));

        FScrollPane _operationLogScroller = new FScrollPane(txtLog, false);
        _operationLogScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        new SmartScroller(_operationLogScroller);
        pnl.add(_operationLogScroller, "pushx, hmin 24, pushy, growy, growx, gap 2px 2px 2px 0, sx 2");

        txtInput.setBorder(new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
        pnl.add(txtInput, "pushx, growx, h 26px!, gap 2px 2px 2px 0");
        pnl.add(cmdSend, "w 60px!, h 28px!, gap 0 0 2px 0");
        
        txtInput.addActionListener(onSend);
        cmdSend.setCommand(new Runnable() { @Override public void run() { onSend.actionPerformed(null); } });
    }
    
    public void showUp(String message) { 
        txtLog.setText(message);
        pnl.setVisible(true);
    }

    private class OverlayPanel extends SkinnedPanel {
        private static final long serialVersionUID = -5056220798272120558L;

        /**
         * For some reason, the alpha channel background doesn't work properly on
         * Windows 7, so the paintComponent override is required for a
         * semi-transparent overlay.
         * 
         * @param g
         *            &emsp; Graphics object
         */
        @Override
        public void paintComponent(final Graphics g) {
            super.paintComponent(g);
            g.setColor(this.getBackground());
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
        }
    }

    /**
     * TODO: Write javadoc for this method.
     * @param mainBounds
     */
    public void containerResized(Rectangle mainBounds) {
        int w = Math.max(width, (int)(mainBounds.width * 0.25f));
        int x = mainBounds.width - w;
        int y = mainBounds.height - height;
        getPanel().setBounds(x, y, w, height);
        getPanel().validate();
    }

    private final static SimpleDateFormat inFormat = new SimpleDateFormat("HH:mm:ss");
    public void addMessage(final String origin, final String message) {
        final String toAdd;
        if (origin == null) {
            toAdd = String.format("%n[%s] %s: %s", inFormat.format(new Date()), origin, message);
        } else {
            toAdd = String.format("%n[%s] %s", inFormat.format(new Date()), message);
        }
        txtLog.append(toAdd);
    }
}
