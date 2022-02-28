package forge.toolbox;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.google.common.collect.ImmutableList;

import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkin;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.card.CardRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.card.CardZoom;
import forge.game.card.CardView;
import forge.localinstance.skin.FSkinProp;
import forge.screens.match.views.VPrompt;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Callback;
import forge.util.Localizer;
import forge.util.Utils;
import forge.util.WaitCallback;

public class FOptionPane extends FDialog {
    public static final FSkinImage QUESTION_ICON = FSkinImage.QUESTION;
    public static final FSkinImage INFORMATION_ICON = FSkinImage.INFORMATION;
    public static final FSkinImage WARNING_ICON = FSkinImage.WARNING;
    public static final FSkinImage ERROR_ICON = FSkinImage.ERROR;

    public static final float PADDING = Utils.scale(10);

    public static float getMaxDisplayObjHeight() {
        return Forge.getScreenHeight() - VPrompt.HEIGHT - 2 * FDialog.MSG_HEIGHT;
    }

    public static void showMessageDialog(final String message) {
        showMessageDialog(message, "", INFORMATION_ICON);
    }

    public static void showMessageDialog(final String message, final String title) {
        showMessageDialog(message, title, INFORMATION_ICON);
    }

    public static void showErrorDialog(final String message) {
        showMessageDialog(message, "", ERROR_ICON);
    }

    public static void showErrorDialog(final String message, final String title) {
        showMessageDialog(message, title, ERROR_ICON);
    }

    public static void showMessageDialog(final String message, final String title, final FImage icon) {
        showOptionDialog(message, title, icon, ImmutableList.of(Localizer.getInstance().getMessage("lblOK")), 0, null);
    }

    public static void showMessageDialog(final String message, FSkinFont messageFont, final String title, final FImage icon) {
        showOptionDialog(message, messageFont, title, icon, ImmutableList.of(Localizer.getInstance().getMessage("lblOK")), 0, null);
    }

    public static void showMessageDialog(final String message, final String title, final FImage icon, final Callback<Integer> callback) {
        showOptionDialog(message, title, icon, ImmutableList.of(Localizer.getInstance().getMessage("lblOK")), 0, callback);
    }

    public static void showConfirmDialog(final String message, final Callback<Boolean> callback) {
        showConfirmDialog(message, "", callback);
    }

    public static void showConfirmDialog(final String message, final String title, final Callback<Boolean> callback) {
        showConfirmDialog(message, title, Localizer.getInstance().getMessage("lblYes"), Localizer.getInstance().getMessage("lblNo"), true, callback);
    }

    public static void showConfirmDialog(final String message, final String title, final boolean defaultYes, final Callback<Boolean> callback) {
        showConfirmDialog(message, title, Localizer.getInstance().getMessage("lblYes"), Localizer.getInstance().getMessage("lblNo"), defaultYes, callback);
    }

    public static void showConfirmDialog(final String message, final String title, final String yesButtonText, final String noButtonText, final Callback<Boolean> callback) {
        showConfirmDialog(message, title, yesButtonText, noButtonText, true, callback);
    }

    public static void showConfirmDialog(final String message, final String title, final String yesButtonText, final String noButtonText, final boolean defaultYes, final Callback<Boolean> callback) {
        final List<String> options = ImmutableList.of(yesButtonText, noButtonText);
        showOptionDialog(message, title, QUESTION_ICON, options, defaultYes ? 0 : 1, new Callback<Integer>() {
            @Override
            public void run(final Integer result) {
                callback.run(result == 0);
            }
        });
    }

    public static void showOptionDialog(final String message, final String title, final FImage icon, final List<String> options, final Callback<Integer> callback) {
        showOptionDialog(message, title, icon, options, 0, callback);
    }
    
    public static void showOptionDialog(final String message, final String title, final FImage icon, final List<String> options, final int defaultOption, final Callback<Integer> callback) {
        showOptionDialog(message, null, title, icon, options, defaultOption, callback);
    }

    public static void showOptionDialog(final String message, final FSkinFont messageFont, final String title, final FImage icon, final List<String> options, final int defaultOption, final Callback<Integer> callback) {
        final FOptionPane optionPane = new FOptionPane(message, messageFont, title, icon, null, options, defaultOption, callback);
        optionPane.show();
    }

    public static int showCardOptionDialog(final CardView card, final String message, final String title, final FSkinProp icon, final List<String> options, final int defaultOption) {
        return new WaitCallback<Integer>() {
            @Override
            public void run() {
                FOptionPane.showCardOptionDialog(card, message, title, icon == null ? null : FSkin.getImages().get(icon), options, defaultOption, this);
            }
        }.invokeAndWait();
    }

    public static void showCardOptionDialog(final CardView card, String message, String title, FImage icon, final List<String> options, final int defaultOption, final Callback<Integer> callback) {
        final FDisplayObject cardDisplay;
        if (card != null) {
            cardDisplay = new FDisplayObject() {
                @Override
                public boolean tap(final float x, final float y, final int count) {
                    CardZoom.show(card);
                    return true;
                }
                @Override
                public boolean longPress(final float x, final float y) {
                    CardZoom.show(card);
                    return true;
                }
                @Override
                public void draw(final Graphics g) {
                    final float h = getHeight();
                    final float w = h / FCardPanel.ASPECT_RATIO;
                    final float x = (getWidth() - w) / 2;
                    final float y = 0;

                    CardRenderer.drawCard(g, card, x, y, w, h, CardStackPosition.Top, true);
                }
            };
            cardDisplay.setHeight(Forge.getScreenHeight() / 2);
        }
        else {
            cardDisplay = null;
        }

        //if title not specified and message is a single line, show message as title
        if (StringUtils.isEmpty(title) && !message.contains("\n")) {
            title = message;
            message = null;
            icon = null;
        }

        final FOptionPane optionPane = new FOptionPane(message, null, title, icon, cardDisplay, options, defaultOption, callback);
        optionPane.show();
    }

    public static void showInputDialog(final String title, final Callback<String> callback) {
        showInputDialog(null, title, "", null, callback);
    }
    public static <T> void showInputDialog(final String title, final T initialInput, final Callback<T> callback) {
        showInputDialog(null, title, initialInput, null, callback);
    }
    public static <T> void showInputDialog(final String message, final String title, final T initialInput, final List<T> inputOptions, final Callback<T> callback) {
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
            cbInput = new FComboBox<>(inputOptions);
            cbInput.setSelectedItem(initialInput);
            inputField = cbInput;
        }

        final float padTop = message == null ? PADDING : 0;

        //use container to add padding above and below field
        final FContainer container = new FContainer() {
            @Override
            protected void doLayout(final float width, final float height) {
                inputField.setBounds(0, padTop, width, inputField.getHeight());
            }
        };
        container.add(inputField);
        container.setHeight(inputField.getHeight() + padTop + PADDING);

        final FOptionPane optionPane = new FOptionPane(message, null, title, null, container, ImmutableList.of(Localizer.getInstance().getMessage("lblOK"), Localizer.getInstance().getMessage("lblCancel")), 0, new Callback<Integer>() {
            @SuppressWarnings("unchecked")
            @Override
            public void run(final Integer result) {
                if (result == 0) {
                    if (txtInput != null) {
                        callback.run((T)txtInput.getText());
                    } else {
                        callback.run(cbInput.getSelectedItem());
                    }
                } else {
                    callback.run(null);
                }
            }
        }) {
            @Override
            protected float getBottomMargin() {
                return Forge.getScreenHeight() * 0.4f; //account for keyboard
            }

            @Override
            protected boolean padAboveAndBelow() {
                return false; //let container around text field handle padding
            }

            @Override
            protected boolean centerPrompt() {
                return true;
            }
        };
        optionPane.show();
        if (txtInput != null) {
            txtInput.startEdit();
        }
    }

    private final FLabel lblIcon;
    private final FTextArea prompt;
    protected final FDisplayObject displayObj;
    private final Callback<Integer> callback;
    private final int defaultOption;
    private final boolean centerIcon;

    public FOptionPane(final String message, final FSkinFont messageFont, final String title, final FImage icon, final FDisplayObject displayObj0, final List<String> options, final int defaultOption0, final Callback<Integer> callback0) {
        super(title, options.size());

        if (icon != null) {
            centerIcon = icon.getWidth() >= 100; //for large icon, center in dialog
            lblIcon = add(new FLabel.Builder().icon(icon).iconScaleFactor(1).insets(new Vector2(0, 0)).iconInBackground(centerIcon).build());
            if (centerIcon) {
                lblIcon.setAlignment(Align.center);
            }
        }
        else {
            lblIcon = null;
            centerIcon = false;
        }

        if (message != null) {
            prompt = add(new FTextArea(true, message));
            prompt.setFont(messageFont != null ? messageFont : FSkinFont.get(12));
            if (centerIcon || centerPrompt()) {
                prompt.setAlignment(Align.center);
            }
        }
        else {
            prompt = null;
        }

        displayObj = displayObj0;
        if (displayObj != null) {
            add(displayObj);
        }

        callback = callback0;

        final int optionsSize = options.size();
        for (int i = 0; i < optionsSize; i++) {
            final int option = i;
            initButton(i, options.get(i), new FEventHandler() {
                @Override
                public void handleEvent(final FEvent e) {
                    setResult(option);
                }
            });
        }
        defaultOption = defaultOption0;
    }

    public void setResult(final int option) {
        hide();
        if (callback != null) {
            callback.run(option);
        }
    }

    protected boolean padAboveAndBelow() {
        return true;
    }

    protected boolean centerPrompt() {
        return false;
    }

    @Override
    protected float layoutAndGetHeight(final float width, final float maxHeight) {
        float x = PADDING;
        float y = PADDING;

        float maxPromptHeight = maxHeight - 2 * PADDING;
        if (displayObj != null) {
            maxPromptHeight -= displayObj.getHeight();
        }

        float promptHeight = 0;
        if (lblIcon != null) {
            final float labelWidth = Utils.scale(lblIcon.getIcon().getWidth());
            promptHeight = lblIcon.getIcon().getHeight() * labelWidth / lblIcon.getIcon().getWidth();
            if (promptHeight > maxPromptHeight) {
                promptHeight = maxPromptHeight;
            }
            if (centerIcon) {
                lblIcon.setBounds(x, y, width - 2 * PADDING, promptHeight);
                y += promptHeight + PADDING;
            }
            else {
                lblIcon.setBounds(x - Utils.scale(3), y, labelWidth, promptHeight);
                x += labelWidth;
            }
        }
        if (prompt != null) {
            final float promptWidth = width - x - PADDING;
            prompt.setBounds(x, y, promptWidth, prompt.getPreferredHeight(promptWidth));
            if (prompt.getHeight() < promptHeight && !centerIcon) {
                //ensure prompt centered next to icon if less tall than icon
                prompt.setTop(y + (promptHeight - prompt.getHeight()) / 2);
            }
            else if (prompt.getHeight() > maxPromptHeight) {
                prompt.setHeight(maxPromptHeight);
            }
            if (prompt.getHeight() > promptHeight || centerIcon) {
                promptHeight = prompt.getHeight();
            }
        }

        if (promptHeight > 0) {
            y += promptHeight + PADDING;
        }

        if (displayObj != null) {
            if (!padAboveAndBelow()) {
                y -= PADDING;
            }
            displayObj.setBounds(0, y, width, displayObj.getHeight());
            y += displayObj.getHeight();
            if (padAboveAndBelow()) {
                y += PADDING;
            }
        }
        return y;
    }

    @Override
    public boolean keyDown(final int keyCode) {
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

            if (isButtonEnabled(1)) {
                setResult(isButtonEnabled(2) ? 2 : 1); //set result to final option on Escape or Back
            }
            return true;
        }
        return super.keyDown(keyCode);
    }
}
