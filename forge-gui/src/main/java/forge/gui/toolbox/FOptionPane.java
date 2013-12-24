package forge.gui.toolbox;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import forge.gui.toolbox.FSkin.SkinImage;
import forge.view.FDialog;

/**
 * Class to replace JOptionPane using skinned dialogs
 *
 */
@SuppressWarnings("serial")
public class FOptionPane extends FDialog {
    private int result;

    public static int showOptionDialog(String message, String title, SkinImage icon, String[] options, int defaultOption) {
        FTextArea txtMessage = new FTextArea(message);
        FSkin.get(txtMessage).setFont(FSkin.getFont(14));
        txtMessage.setAutoSize(true);
        Dimension parentSize = JOptionPane.getRootFrame().getSize();
        txtMessage.setMaximumSize(new Dimension(parentSize.width / 2, parentSize.height - 100));

        FOptionPane optionPane = new FOptionPane(title, icon, txtMessage, options, defaultOption);
        optionPane.result = options.length - 1; //default result to final option in case dialog closed
        optionPane.setVisible(true);
        int dialogResult = optionPane.result;
        optionPane.dispose();
        return dialogResult;
    }

    private FOptionPane(String title, SkinImage icon, JComponent comp, String[] options, int defaultOption) {
        this.setTitle(title);

        int padX = 10;
        int gapBottom = 2 * padX;
        int x = padX;

        if (icon != null) {
            FLabel lblIcon = new FLabel.Builder().icon(icon).build();
            int labelWidth = icon.getWidth();
            this.add(lblIcon, "x " + (x - 3) + ", ay top, w " + labelWidth + ", h " + icon.getHeight() + ", gapbottom " + gapBottom);
            x += labelWidth;
        }
        this.add(comp, "x " + x + ", wrap, gapbottom " + gapBottom);

        //determine size of buttons
        int optionCount = options.length;
        FButton btnMeasure = new FButton(); //use blank button to aid in measurement
        FontMetrics metrics = JOptionPane.getRootFrame().getGraphics().getFontMetrics(btnMeasure.getFont());

        int maxTextWidth = 0;
        final FButton[] buttons = new FButton[optionCount];
        for (int i = 0; i < optionCount; i++) {
            int textWidth = metrics.stringWidth(options[i]);
            if (textWidth > maxTextWidth) {
                maxTextWidth = textWidth;
            }
            buttons[i] = new FButton(options[i]);
        }

        this.pack(); //resize dialog to fit component and title to help determine button layout

        int width = this.getWidth();
        int gapBetween = 3;
        int buttonHeight = 26;
        int buttonWidth = Math.max(maxTextWidth + btnMeasure.getMargin().left + btnMeasure.getMargin().right, 120); //account for margins and enfore minimum width
        int dx = buttonWidth + gapBetween;
        int totalButtonWidth = dx * optionCount - gapBetween;
        final int lastOption = optionCount - 1;

        //add buttons
        x = (width - totalButtonWidth) / 2;
        if (x < padX) {
            width = totalButtonWidth + 2 * padX; //increase width to make room for buttons
            x = padX;
        }
        for (int i = 0; i < optionCount; i++) {
            final int option = i;
            final FButton btn = buttons[i];
            btn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    FOptionPane.this.result = option;
                    FOptionPane.this.setVisible(false);
                }
            });
            btn.addKeyListener(new KeyAdapter() { //hook certain keys to move focus between buttons
                @Override
                public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        if (option > 0) {
                            buttons[option - 1].requestFocusInWindow();
                        }
                        break;
                    case KeyEvent.VK_RIGHT:
                        if (option < lastOption) {
                            buttons[option + 1].requestFocusInWindow();
                        }
                        break;
                    case KeyEvent.VK_HOME:
                        if (option > 0) {
                            buttons[0].requestFocusInWindow();
                        }
                        break;
                    case KeyEvent.VK_END:
                        if (option < lastOption) {
                            buttons[lastOption].requestFocusInWindow();
                        }
                        break;
                    }
                }
            });
            if (option == defaultOption) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        btn.requestFocusInWindow();
                    }
                });
            }
            this.add(btn, "x " + x + ", w " + buttonWidth + ", h " + buttonHeight);
            x += dx;
        }

        this.setSize(width, this.getHeight() + buttonHeight); //resize dialog again to account for buttons
    }
}
