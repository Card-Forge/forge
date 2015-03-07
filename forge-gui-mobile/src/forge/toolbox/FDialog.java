package forge.toolbox;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinTexture;
import forge.assets.FSkinColor.Colors;
import forge.screens.FScreen;
import forge.screens.match.MatchController;
import forge.screens.match.views.VPrompt;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Utils;

public abstract class FDialog extends FOverlay {
    public static final FSkinFont MSG_FONT = FSkinFont.get(12);
    public static final FSkinColor MSG_FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT).alphaColor(0.9f);
    public static final FSkinColor MSG_BACK_COLOR = FScreen.Header.BACK_COLOR.alphaColor(0.75f);
    public static final float MSG_HEIGHT = MSG_FONT.getCapHeight() * 2.5f;
    private static final FSkinColor BORDER_COLOR = FSkinColor.get(Colors.CLR_BORDERS);
    private static float BORDER_THICKNESS = Utils.scale(1);

    private static int openDialogCount = 0;

    public static boolean isDialogOpen() {
        return openDialogCount > 0;
    }

    private final VPrompt prompt;
    private final int buttonCount;
    private float totalHeight;
    private boolean hidden;

    protected FDialog(String title, int buttonCount0) {
        title = title.replaceAll(" - ", "\n"); //breakup into lines
        buttonCount = buttonCount0;
        prompt = add(new VPrompt("", "", null, null));
        prompt.setMessage(title);
    }

    @Override
    protected final void doLayout(float width, float height) {
        float contentHeight = layoutAndGetHeight(width, height - VPrompt.HEIGHT - 2 * MSG_HEIGHT);
        totalHeight = contentHeight + VPrompt.HEIGHT;

        prompt.setBounds(0, height - VPrompt.HEIGHT, width, VPrompt.HEIGHT);

        //shift all children into position
        float dy = height - totalHeight;
        for (FDisplayObject child : getChildren()) {
            if (child != prompt) {
                child.setTop(child.getTop() + dy);
            }
        }
    }

    @Override
    public void setVisible(boolean visible0) {
        if (this.isVisible() == visible0) { return; }

        if (visible0) {
            openDialogCount++;
            MatchController.instance.cancelAwaitNextInput(); //ensure "Waiting for opponent..." prompt doesn't appear while dialog awaiting input
        }
        else if (openDialogCount > 0) {
            openDialogCount--;
        }
        super.setVisible(visible0);
    }

    protected abstract float layoutAndGetHeight(float width, float maxHeight);
    
    protected void initButton(int index, String text, FEventHandler command) {
        FButton button = getButton(index);
        if (button == null) { return; }

        button.setText(text);
        button.setCommand(command);
        button.setEnabled(true);
    }

    protected FButton getButton(int index) {
        switch (index) {
        case 0:
            return prompt.getBtnOk();
        case 1:
            return prompt.getBtnCancel();
        default:
            return null;
        }
    }

    public boolean isButtonEnabled(int index) {
        FButton button = getButton(index);
        return button != null ? button.isEnabled() : false;
    }

    public void setButtonEnabled(int index, boolean enabled) {
        FButton button = getButton(index);
        if (button != null) {
            button.setEnabled(enabled);
        }
    }

    private boolean isPointWithinDialog(float x, float y) {
        return y >= getHeight() - totalHeight;
    }

    @Override
    public boolean longPress(float x, float y) {
        return !isPointWithinDialog(x, y); //don't handle long press by default if pressed inside dialog
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);

        float x = 0;
        float y = getHeight() - totalHeight;
        float w = getWidth();
        float h = totalHeight - VPrompt.HEIGHT;
        g.drawImage(FSkinTexture.BG_TEXTURE, x, y, w, h);
        g.fillRect(FScreen.TEXTURE_OVERLAY_COLOR, x, y, w, h);
    }

    @Override
    public void drawOverlay(Graphics g) {
        float w = getWidth();
        String message;
        if (hidden) {
            message = "Swipe up to show prompt again";
        }
        else {
            message = "Swipe down to temporarily hide prompt";
        }
        g.fillRect(FDialog.MSG_BACK_COLOR, 0, 0, w, MSG_HEIGHT);
        g.drawText(message, FDialog.MSG_FONT, FDialog.MSG_FORE_COLOR, 0, 0, w, MSG_HEIGHT, false, HAlignment.CENTER, true);

        float y = getHeight() - totalHeight;
        g.drawLine(BORDER_THICKNESS, BORDER_COLOR, 0, y, w, y);
        y = prompt.getTop();
        g.drawLine(BORDER_THICKNESS, BORDER_COLOR, 0, y, w, y);
    }
}
