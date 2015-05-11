package forge.toolbox;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.StyleConstants;

import com.google.common.collect.ImmutableList;

import forge.assets.FSkinProp;
import forge.toolbox.FSkin.SkinImage;
import forge.view.FDialog;

/**
 * Class to replace JOptionPane using skinned dialogs
 *
 */
@SuppressWarnings("serial")
public class FOptionPane extends FDialog {
    public static final SkinImage QUESTION_ICON = FSkin.getIcon(FSkinProp.ICO_QUESTION);
    public static final SkinImage INFORMATION_ICON = FSkin.getIcon(FSkinProp.ICO_INFORMATION);
    public static final SkinImage WARNING_ICON = FSkin.getIcon(FSkinProp.ICO_WARNING);
    public static final SkinImage ERROR_ICON = FSkin.getIcon(FSkinProp.ICO_ERROR);

    public static void showMessageDialog(final String message) {
        showMessageDialog(message, "Forge", INFORMATION_ICON);
    }

    public static void showMessageDialog(final String message, final String title) {
        showMessageDialog(message, title, INFORMATION_ICON);
    }

    public static void showErrorDialog(final String message) {
        showMessageDialog(message, "Forge", ERROR_ICON);
    }

    public static void showErrorDialog(final String message, final String title) {
        showMessageDialog(message, title, ERROR_ICON);
    }

    public static void showMessageDialog(final String message, final String title, final SkinImage icon) {
        showOptionDialog(message, title, icon, ImmutableList.of("OK"), 0);
    }

    public static boolean showConfirmDialog(final String message) {
        return showConfirmDialog(message, "Forge");
    }

    public static boolean showConfirmDialog(final String message, final String title) {
        return showConfirmDialog(message, title, "Yes", "No", true);
    }

    public static boolean showConfirmDialog(final String message, final String title, final boolean defaultYes) {
        return showConfirmDialog(message, title, "Yes", "No", defaultYes);
    }

    public static boolean showConfirmDialog(final String message, final String title, final String yesButtonText, final String noButtonText) {
        return showConfirmDialog(message, title, yesButtonText, noButtonText, true);
    }

    public static boolean showConfirmDialog(final String message, final String title, final String yesButtonText, final String noButtonText, final boolean defaultYes) {
        final List<String> options = ImmutableList.of(yesButtonText, noButtonText);
        final int reply = FOptionPane.showOptionDialog(message, title, QUESTION_ICON, options, defaultYes ? 0 : 1);
        return (reply == 0);
    }

    public static int showOptionDialog(final String message, final String title, final SkinImage icon, final List<String> options) {
        return showOptionDialog(message, title, icon, options, 0);
    }

    public static int showOptionDialog(final String message, final String title, final SkinImage icon, final List<String> options, final int defaultOption) {
        final FOptionPane optionPane = new FOptionPane(message, title, icon, null, options, defaultOption);
        optionPane.setVisible(true);
        final int dialogResult = optionPane.result;
        optionPane.dispose();
        return dialogResult;
    }

    public static String showInputDialog(final String message, final String title) {
        return showInputDialog(message, title, null, "", null);
    }

    public static String showInputDialog(final String message, final String title, final SkinImage icon) {
        return showInputDialog(message, title, icon, "", null);
    }

    public static String showInputDialog(final String message, final String title, final SkinImage icon, final String initialInput) {
        return showInputDialog(message, title, icon, initialInput, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> T showInputDialog(final String message, final String title, final SkinImage icon, final String initialInput, final List<T> inputOptions) {
        final JComponent inputField;
        FTextField txtInput = null;
        FComboBox<T> cbInput = null;
        if (inputOptions == null) {
            txtInput = new FTextField.Builder().text(initialInput.toString()).build();
            inputField = txtInput;
        } else {
            cbInput = new FComboBox<T>(inputOptions);
            cbInput.setSelectedItem(initialInput);
            inputField = cbInput;
        }

        final FOptionPane optionPane = new FOptionPane(message, title, icon, inputField, ImmutableList.of("OK", "Cancel"), -1);
        optionPane.setDefaultFocus(inputField);
        inputField.addKeyListener(new KeyAdapter() { //hook so pressing Enter on field accepts dialog
            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    optionPane.setResult(0);
                }
            }
        });
        optionPane.setVisible(true);
        final int dialogResult = optionPane.result;
        optionPane.dispose();
        if (dialogResult == 0) {
            if (inputOptions == null) {
                return (T)txtInput.getText();
            } else {
                return cbInput.getSelectedItem();
            }
        }
        return null;
    }

    private int result = -1; //default result to -1, indicating dialog closed without choosing option
    private final FButton[] buttons;

    public FOptionPane(final String message, final String title, final SkinImage icon, final Component comp, final List<String> options, final int defaultOption) {
        this.setTitle(title);

        final int padding = 10;
        int x = padding;
        final int gapAboveButtons = padding * 3 / 2;
        final int gapBottom = comp == null ? gapAboveButtons : padding;
        FLabel centeredLabel = null;
        FTextPane centeredPrompt = null;

        if (icon != null) {
            if (icon.getWidth() < 100) {
                final FLabel lblIcon = new FLabel.Builder().icon(icon).build();
                this.add(lblIcon, "x " + (x - 3) + ", ay top, w " + icon.getWidth() + ", h " + icon.getHeight() + ", gapbottom " + gapBottom);
                x += icon.getWidth();
            }
            else {
                final FLabel lblIcon = new FLabel.Builder().icon(icon).iconInBackground(true).iconScaleFactor(1).iconAlignX(SwingConstants.CENTER).build();
                lblIcon.setMinimumSize(new Dimension(icon.getWidth() * 2, icon.getHeight()));
                this.add(lblIcon, "x " + x + ", ay top, wrap, gapbottom " + gapBottom);
                centeredLabel = lblIcon;
            }
        }
        if (message != null) {
            if (centeredLabel == null) {
                final FTextArea prompt = new FTextArea(message);
                prompt.setFont(FSkin.getFont(14));
                prompt.setAutoSize(true);
                final Dimension parentSize = JOptionPane.getRootFrame().getSize();
                prompt.setMaximumSize(new Dimension(parentSize.width / 2, parentSize.height - 100));
                this.add(prompt, "x " + x + ", ay top, wrap, gaptop " + (icon == null ? 0 : 7) + ", gapbottom " + gapBottom);
            }
            else {
                final FTextPane prompt = new FTextPane(message);
                prompt.setFont(FSkin.getFont(14));
                prompt.setTextAlignment(StyleConstants.ALIGN_CENTER);
                final Dimension parentSize = JOptionPane.getRootFrame().getSize();
                prompt.setMaximumSize(new Dimension(parentSize.width / 2, parentSize.height - 100));
                this.add(prompt, "x " + x + ", ay top, wrap, gapbottom " + gapBottom);
                centeredPrompt = prompt;
            }
            x = padding;
        }
        if (comp != null) {
            this.add(comp, "x " + x + ", w 100%-" + (x + padding) + ", wrap, gapbottom " + gapAboveButtons);
        }

        //determine size of buttons
        final int optionCount = options.size();
        final FButton btnMeasure = new FButton(); //use blank button to aid in measurement
        final FontMetrics metrics = JOptionPane.getRootFrame().getGraphics().getFontMetrics(btnMeasure.getFont());

        int maxTextWidth = 0;
        buttons = new FButton[optionCount];
        for (int i = 0; i < optionCount; i++) {
            final String option = options.get(i);
            final int textWidth = metrics.stringWidth(option);
            if (textWidth > maxTextWidth) {
                maxTextWidth = textWidth;
            }
            buttons[i] = new FButton(option);
        }

        this.pack(); //resize dialog to fit component and title to help determine button layout

        int width = this.getWidth();
        final int gapBetween = 3;
        final int buttonHeight = 26;
        final int buttonWidth = Math.max(maxTextWidth + btnMeasure.getMargin().left + btnMeasure.getMargin().right, 120); //account for margins and enforce minimum width
        final int dx = buttonWidth + gapBetween;
        final int totalButtonWidth = dx * optionCount - gapBetween;
        final int lastOption = optionCount - 1;

        //add buttons
        x = (width - totalButtonWidth) / 2;
        if (x < padding) {
            width = totalButtonWidth + 2 * padding; //increase width to make room for buttons
            x = padding;
        }
        for (int i = 0; i < optionCount; i++) {
            final int option = i;
            final FButton btn = buttons[i];
            btn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent arg0) {
                    FOptionPane.this.result = option;
                    FOptionPane.this.setVisible(false);
                }
            });
            btn.addKeyListener(new KeyAdapter() { //hook certain keys to move focus between buttons
                @Override
                public void keyPressed(final KeyEvent e) {
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
                this.setDefaultFocus(btn);
            }
            this.add(btn, "x " + x + ", w " + buttonWidth + ", h " + buttonHeight);
            x += dx;
        }

        if (centeredLabel != null) {
            centeredLabel.setPreferredSize(new Dimension(width - 2 * padding, centeredLabel.getMinimumSize().height));
            centeredPrompt.setPreferredSize(new Dimension(width - 2 * padding, centeredPrompt.getPreferredSize().height));
        }

        this.setSize(width, this.getHeight() + buttonHeight); //resize dialog again to account for buttons
    }

    @Override
    public void setVisible(final boolean visible) {
        if (this.isVisible() == visible) { return; }

        if (visible) {
            result = -1; //default result to -1 when shown, indicating dialog closed without choosing option
        }
        super.setVisible(visible);
    }

    public int getResult() {
        return result;
    }

    public void setResult(final int result0) {
        this.result = result0;
        SwingUtilities.invokeLater(new Runnable() { //delay hiding so action can finish first
            @Override
            public void run() {
                setVisible(false);
            }
        });
    }

    public boolean isButtonEnabled(final int index) {
        return buttons[index].isEnabled();
    }

    public void setButtonEnabled(final int index, final boolean enabled) {
        buttons[index].setEnabled(enabled);
    }
}
