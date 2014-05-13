package forge.toolbox;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;

import forge.Forge;
import forge.assets.FSkinImage;
import forge.screens.match.views.VPrompt;
import forge.toolbox.FEvent.*;
import forge.util.Callback;
import forge.util.Utils;

public class FOptionPane extends FDialog {
    public static final FSkinImage QUESTION_ICON = FSkinImage.QUESTION;
    public static final FSkinImage INFORMATION_ICON = FSkinImage.INFORMATION;
    public static final FSkinImage WARNING_ICON = FSkinImage.WARNING;
    public static final FSkinImage ERROR_ICON = FSkinImage.ERROR;

    public static final float PADDING = Utils.scaleMin(10);
    public static final float GAP_BELOW_BUTTONS = PADDING * 0.5f;
    public static final float BUTTON_HEIGHT = Utils.AVG_FINGER_HEIGHT * 0.75f;
    public static final float MIN_BUTTON_WIDTH = Utils.scaleX(120);

    public static float getMaxDisplayObjHeight() {
        return Forge.getCurrentScreen().getHeight() - 2 * VPrompt.HEIGHT - FDialog.TITLE_HEIGHT - 
               2 * PADDING - BUTTON_HEIGHT - GAP_BELOW_BUTTONS;
    }

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
        showOptionDialog(message, title, icon, new String[] {"OK"}, 0, null);
    }

    public static void showConfirmDialog(String message, final Callback<Boolean> callback) {
        showConfirmDialog(message, "Forge", callback);
    }

    public static void showConfirmDialog(String message, String title, final Callback<Boolean> callback) {
        showConfirmDialog(message, title, "Yes", "No", true, callback);
    }

    public static void showConfirmDialog(String message, String title, boolean defaultYes, final Callback<Boolean> callback) {
        showConfirmDialog(message, title, "Yes", "No", defaultYes, callback);
    }

    public static void showConfirmDialog(String message, String title, String yesButtonText, String noButtonText, final Callback<Boolean> callback) {
        showConfirmDialog(message, title, yesButtonText, noButtonText, true, callback);
    }

    public static void showConfirmDialog(String message, String title, String yesButtonText, String noButtonText, boolean defaultYes, final Callback<Boolean> callback) {
        String[] options = {yesButtonText, noButtonText};
        showOptionDialog(message, title, QUESTION_ICON, options, defaultYes ? 0 : 1, new Callback<Integer>() {
            @Override
            public void run(Integer result) {
                callback.run(result == 0);
            }
        });
    }

    public static void showOptionDialog(String message, String title, FSkinImage icon, String[] options, final Callback<Integer> callback) {
        showOptionDialog(message, title, icon, options, 0, callback);
    }

    public static void showOptionDialog(String message, String title, FSkinImage icon, String[] options, int defaultOption, final Callback<Integer> callback) {
        final FOptionPane optionPane = new FOptionPane(message, title, icon, null, options, defaultOption, callback);
        optionPane.show();
    }

    public static void showInputDialog(String message, String title, final Callback<String> callback) {
        showInputDialog(message, title, null, "", null, callback);
    }

    public static void showInputDialog(String message, String title, FSkinImage icon, final Callback<String> callback) {
        showInputDialog(message, title, icon, "", null, callback);
    }

    public static void showInputDialog(String message, String title, FSkinImage icon, String initialInput, final Callback<String> callback) {
        showInputDialog(message, title, icon, initialInput, null, callback);
    }

    public static <T> void showInputDialog(String message, String title, FSkinImage icon, T initialInput, T[] inputOptions, final Callback<T> callback) {
        final FDisplayObject inputField;
        final FTextField txtInput;
        final FComboBox<T> cbInput;
        if (inputOptions == null) {
            txtInput = new FTextField(initialInput.toString());
            cbInput = null;
            inputField = txtInput;
        }
        else {
            txtInput = null;
            cbInput = new FComboBox<T>(inputOptions);
            cbInput.setSelectedItem(initialInput);
            inputField = cbInput;
        }

        final FOptionPane optionPane = new FOptionPane(message, title, icon, inputField, new String[] {"OK", "Cancel"}, -1, new Callback<Integer>() {
            @SuppressWarnings("unchecked")
            @Override
            public void run(Integer result) {
                if (result == 0) {
                    if (txtInput != null) {
                        callback.run((T)txtInput.getText());
                    }
                    else {
                        callback.run((T)cbInput.getSelectedItem());
                    }
                }
                callback.run(null);
            }
        });
        optionPane.show();
    }

    private final FLabel lblIcon;
    private final FTextArea prompt;
    private final FDisplayObject displayObj;
    private final FButton[] buttons;
    private final Callback<Integer> callback;
    private final int defaultOption;

    public FOptionPane(String message, String title, FSkinImage icon, FDisplayObject displayObj0, String[] options, int defaultOption0, final Callback<Integer> callback0) {
        super(title);

        if (icon != null) {
            lblIcon = add(new FLabel.Builder().icon(icon).iconScaleAuto(false).insets(new Vector2(0, 0)).build());
        }
        else {
            lblIcon = null;
        }

        if (message != null) {
            prompt = add(new FTextArea(message));
            prompt.setFontSize(12);
        }
        else {
            prompt = null;
        }

        displayObj = displayObj0;
        if (displayObj != null) {
            add(displayObj);
        }

        callback = callback0;

        int optionCount = options.length;
        buttons = new FButton[optionCount];
        for (int i = 0; i < optionCount; i++) {
            final int option = i;
            buttons[i] = add(new FButton(options[i], new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    setResult(option);
                }
            }));
        }
        defaultOption = defaultOption0;
    }

    public void setResult(final int option) {
        hide();
        if (callback != null) {
            callback.run(option);
        }
    }

    public boolean isButtonEnabled(int index) {
        return buttons[index].isEnabled();
    }

    public void setButtonEnabled(int index, boolean enabled) {
        buttons[index].setEnabled(enabled);
    }

    @Override
    protected float layoutAndGetHeight(float width, float maxHeight) {
        float x = PADDING;
        float y = PADDING;

        float maxPromptHeight = maxHeight - PADDING - BUTTON_HEIGHT - GAP_BELOW_BUTTONS;
        if (displayObj != null) {
            maxPromptHeight -= displayObj.getHeight();
        }

        float promptHeight = 0;
        if (lblIcon != null) {
            float labelWidth = lblIcon.getIcon().getWidth();
            promptHeight = lblIcon.getIcon().getHeight();
            if (promptHeight > maxPromptHeight) {
                promptHeight = maxPromptHeight;
            }
            lblIcon.setBounds(x - Utils.scaleX(3), y, labelWidth, promptHeight);
            x += labelWidth;
        }
        if (prompt != null) {
            float promptWidth = width - x - PADDING;
            prompt.setBounds(x, y, promptWidth, prompt.getPreferredHeight(promptWidth));
            if (prompt.getHeight() < promptHeight) {
               //ensure prompt centered next to icon if less tall than icon
                prompt.setTop(y + (promptHeight - prompt.getHeight()) / 2);
            }
            else if (prompt.getHeight() > maxPromptHeight) {
                prompt.setHeight(maxPromptHeight);
            }
            if (prompt.getHeight() > promptHeight) {
                promptHeight = prompt.getHeight();
            }
        }

        x = PADDING;
        if (promptHeight > 0) {
            y += promptHeight + PADDING;
        }

        if (displayObj != null) {
            displayObj.setBounds(x, y, width - 2 * x, displayObj.getHeight());
            y += displayObj.getHeight() + PADDING;
        }

        //determine size for and position buttons
        float maxButtonWidth = 0;
        for (FButton btn : buttons) {
            float buttonWidth = btn.getAutoSizeBounds().width;
            if (buttonWidth > maxButtonWidth) {
                maxButtonWidth = buttonWidth;
            }
        }
        float gapBetween = Utils.scaleX(-2); //use negative so buttons closer together
        float buttonWidth = Math.max(maxButtonWidth, MIN_BUTTON_WIDTH); //account for margins and enforce minimum width
        float dx = buttonWidth + gapBetween;
        float totalButtonWidth = dx * buttons.length - gapBetween;

        x = (width - totalButtonWidth) / 2;
        if (x < PADDING) { //reduce button width if not enough room for buttons
            buttonWidth = (width - 2 * PADDING - (buttons.length - 1) * gapBetween) / (float)buttons.length;
            x = PADDING;
            dx = buttonWidth + gapBetween;
        }
        for (FButton btn : buttons) {
            btn.setBounds(x, y, buttonWidth, BUTTON_HEIGHT);
            x += dx;
        }

        return y + BUTTON_HEIGHT + GAP_BELOW_BUTTONS;
    }

    @Override
    public boolean keyDown(int keyCode) {
        switch (keyCode) {
        case Keys.ENTER:
        case Keys.SPACE:
            if (isButtonEnabled(defaultOption)) {
                setResult(defaultOption); //set result to default option on Enter/Space
            }
            return true;
        case Keys.ESCAPE:
        case Keys.BACK:
            if (Forge.endKeyInput()) { return true; }

            if (isButtonEnabled(buttons.length - 1)) {
                setResult(buttons.length - 1); //set result to final option on Escape or Back
            }
            return true;
        }
        return super.keyDown(keyCode);
    }
}
