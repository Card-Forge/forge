package forge.toolbox;

import forge.assets.FSkinImage;

public class FOptionPane extends FOverlay {
    public static final FSkinImage QUESTION_ICON = FSkinImage.QUESTION;
    public static final FSkinImage INFORMATION_ICON = FSkinImage.INFORMATION;
    public static final FSkinImage WARNING_ICON = FSkinImage.WARNING;
    public static final FSkinImage ERROR_ICON = FSkinImage.ERROR;

    public static void showMessageDialog(String message) {
        showMessageDialog(message, "Forge", INFORMATION_ICON);
    }

    public static void showMessageDialog(String message, String title) {
        showMessageDialog(message, title, INFORMATION_ICON);
    }

    public static void showErrorDialog(String message) {
        showMessageDialog(message, "Forge", ERROR_ICON);
    }

    public static void showErrorDialog(String message, String title) {
        showMessageDialog(message, title, ERROR_ICON);
    }

    public static void showMessageDialog(String message, String title, FSkinImage icon) {
        showOptionDialog(message, title, icon, new String[] {"OK"}, 0);
    }

    public static boolean showConfirmDialog(String message) {
        return showConfirmDialog(message, "Forge");
    }

    public static boolean showConfirmDialog(String message, String title) {
        return showConfirmDialog(message, title, "Yes", "No", true);
    }

    public static boolean showConfirmDialog(String message, String title, boolean defaultYes) {
        return showConfirmDialog(message, title, "Yes", "No", defaultYes);
    }

    public static boolean showConfirmDialog(String message, String title, String yesButtonText, String noButtonText) {
        return showConfirmDialog(message, title, yesButtonText, noButtonText, true);
    }

    public static boolean showConfirmDialog(String message, String title, String yesButtonText, String noButtonText, boolean defaultYes) {
        String[] options = {yesButtonText, noButtonText};
        int reply = FOptionPane.showOptionDialog(message, title, QUESTION_ICON, options, defaultYes ? 0 : 1);
        return (reply == 0);
    }

    public static int showOptionDialog(String message, String title, FSkinImage icon, String[] options) {
        return showOptionDialog(message, title, icon, options, 0);
    }

    public static int showOptionDialog(String message, String title, FSkinImage icon, String[] options, int defaultOption) {
        final FOptionPane optionPane = new FOptionPane(message, title, icon, null, options, defaultOption);
        optionPane.setVisible(true);
        int dialogResult = optionPane.result;
        optionPane.setVisible(false);
        return dialogResult;
    }

    public static String showInputDialog(String message, String title) {
        return showInputDialog(message, title, null, "", null);
    }

    public static String showInputDialog(String message, String title, FSkinImage icon) {
        return showInputDialog(message, title, icon, "", null);
    }

    public static String showInputDialog(String message, String title, FSkinImage icon, String initialInput) {
        return showInputDialog(message, title, icon, initialInput, null);
    }

    public static <T> T showInputDialog(String message, String title, FSkinImage icon, T initialInput, T[] inputOptions) {
        /*final JComponent inputField;
        FTextField txtInput = null;
        FComboBox<T> cbInput = null;
        if (inputOptions == null) {
            txtInput = new FTextField.Builder().text(initialInput.toString()).build();
            inputField = txtInput;
        }
        else {
            cbInput = new FComboBox<T>(inputOptions);
            cbInput.setSelectedItem(initialInput);
            inputField = cbInput;
        }

        final FOptionPane optionPane = new FOptionPane(message, title, icon, inputField, new String[] {"OK", "Cancel"}, -1);
        optionPane.setDefaultFocus(inputField);
        inputField.addKeyListener(new KeyAdapter() { //hook so pressing Enter on field accepts dialog
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    optionPane.setResult(0);
                }
            }
        });
        optionPane.setVisible(true);
        int dialogResult = optionPane.result;
        optionPane.dispose();
        if (dialogResult == 0) {
            if (inputOptions == null) {
                return (T)txtInput.getText();
            }
            else {
                return (T)cbInput.getSelectedItem();
            }
        }*/
        return null;
    }

    private int result = -1; //default result to -1, indicating dialog closed without choosing option
    private final FButton[] buttons;

    public FOptionPane(String message, String title, FSkinImage icon, FDisplayObject displayObj, String[] options, int defaultOption) {
        buttons = new FButton[options.length]; //TODO: Remove this line when below uncommented
        /*this.setTitle(title);

        float padding = 10;
        float x = padding;
        float gapAboveButtons = padding * 3 / 2;
        float gapBottom = displayObj == null ? gapAboveButtons: padding;

        if (icon != null) {
            FLabel lblIcon = new FLabel.Builder().icon(icon).build();
            float labelWidth = icon.getWidth();
            this.add(lblIcon, "x " + (x - 3) + ", ay top, w " + labelWidth + ", h " + icon.getHeight() + ", gapbottom " + gapBottom);
            x += labelWidth;
        }
        if (message != null) {
            FTextArea prompt = new FTextArea(message);
            prompt.setFont(FSkin.getFont(14));
            prompt.setAutoSize(true);
            Dimension parentSize = JOptionPane.getRootFrame().getSize();
            prompt.setMaximumSize(new Dimension(parentSize.width / 2, parentSize.height - 100));
            this.add(prompt, "x " + x + ", ay top, wrap, gaptop " + (icon == null ? 0 : 7) + ", gapbottom " + gapBottom);
            x = padding;
        }
        if (displayObj != null) {
            this.add(displayObj, "x " + x + ", w 100%-" + (x + padding) + ", wrap, gapbottom " + gapAboveButtons);
        }

        //determine size of buttons
        int optionCount = options.length;
        FButton btnMeasure = new FButton(); //use blank button to aid in measurement
        FontMetrics metrics = JOptionPane.getRootFrame().getGraphics().getFontMetrics(btnMeasure.getFont());

        int maxTextWidth = 0;
        buttons = new FButton[optionCount];
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
        if (x < padding) {
            width = totalButtonWidth + 2 * padding; //increase width to make room for buttons
            x = padding;
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
                this.setDefaultFocus(btn);
            }
            this.add(btn, "x " + x + ", w " + buttonWidth + ", h " + buttonHeight);
            x += dx;
        }

        this.setSize(width, this.getHeight() + buttonHeight); //resize dialog again to account for buttons
*/    }

    @Override
    public void setVisible(boolean visible) {
        if (this.isVisible() == visible) { return; }

        if (visible) {
            result = -1; //default result to -1 when shown, indicating dialog closed without choosing option
        }
        super.setVisible(visible);
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result0) {
        this.result = result0;
        /*SwingUtilities.invokeLater(new Runnable() { //delay hiding so action can finish first
            @Override
            public void run() {
                setVisible(false);
            }
        });*/
    }

    public boolean isButtonEnabled(int index) {
        return buttons[index].isEnabled();
    }

    public void setButtonEnabled(int index, boolean enabled) {
        buttons[index].setEnabled(enabled);
    }
}
