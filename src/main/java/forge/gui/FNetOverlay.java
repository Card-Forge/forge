package forge.gui;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import org.apache.commons.lang3.StringUtils;

import net.miginfocom.swing.MigLayout;
import forge.Command;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FTextArea;
import forge.gui.toolbox.FTextField;
import forge.gui.toolbox.SmartScroller;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum FNetOverlay {
    SINGLETON_INSTANCE;

    private final JPanel pnl = new OverlayPanel();
    /** @return {@link javax.swing.JPanel} */
    public JPanel getPanel() {
        return this.pnl;
    }
    
    private final JTextArea txtLog = new FTextArea();
    private final JTextField txtInput = new FTextField.Builder().maxLength(60).build();
    private final FLabel cmdSend = new FLabel.ButtonBuilder().text("Send").build(); 

    
    private boolean minimized = false;
    private int height = 120;
    private int width = 400;

    private final ActionListener onSend = new ActionListener() {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            String message = txtInput.getText();
            txtInput.setText("");
            if ( StringUtils.isBlank(message) )
                return;
            addMessage(message);
        }
    };
    
    private final int minimizedHeight = 30;
    
    /**
     * Semi-transparent overlay panel. Should be used with layered panes.
     */
    private FNetOverlay() {
        pnl.setOpaque(false);
        pnl.setVisible(false);
        pnl.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        pnl.setBorder(BorderFactory.createLineBorder(FSkin.getColor(FSkin.Colors.CLR_BORDERS)));

        pnl.setLayout(new MigLayout("insets 0, gap 0, ax center, wrap 2"));
//        pnl.add(new FLabel.Builder().text("Loading new game...").fontSize(22).build(), "h 40px!, align center");

        // Block all input events below the overlay
        pnl.addMouseListener(new MouseAdapter() { });
        pnl.addMouseMotionListener(new MouseMotionAdapter() { });
        pnl.addKeyListener(new KeyAdapter() { });
        
        txtLog.setOpaque(true);
        txtLog.setFocusable(true);
        txtLog.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        txtLog.setText("This is Forge chat window\n");

        JScrollPane _operationLogScroller = new JScrollPane(txtLog);
        _operationLogScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        _operationLogScroller.setBorder(null);
        new SmartScroller(_operationLogScroller);
        pnl.add(_operationLogScroller, "pushx, hmin 24, growy, growx, gap 2px 2px 2px 0, sx 2");

        txtInput.setBorder(BorderFactory.createLineBorder(FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
        pnl.add(txtInput, "pushx, growx, h 26px!, gap 2px 2px 2px 0");
        pnl.add(cmdSend, "w 60px!, h 28px!, gap 0 0 2px 0");
        
        txtInput.addActionListener(onSend);
        cmdSend.setCommand(new Runnable() { @Override public void run() { onSend.actionPerformed(null); } });
    }

    private class OverlayPanel extends JPanel {
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
    
    SimpleDateFormat inFormat = new SimpleDateFormat("HH:mm:ss");
    public void addMessage(String message) {
        String toAdd = String.format("[%s]: %s%n", inFormat.format(new Date()), message);
        txtLog.append(toAdd);
    }
}
