package forge.screens.match.views;

import com.badlogic.gdx.utils.Align;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.card.CardZoom;
import forge.game.card.CardView;
import forge.menu.FMagnifyView;
import forge.toolbox.FButton;
import forge.toolbox.FButton.Corner;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.TextBounds;
import forge.util.Utils;
import org.apache.commons.lang3.StringUtils;

public class VPrompt extends FContainer {
    public static final float HEIGHT = Utils.AVG_FINGER_HEIGHT;
    public static final float BTN_WIDTH = HEIGHT * 1.5f;
    public static final float PADDING = Utils.scale(2);
    public static final FSkinFont FONT = FSkinFont.get(14);
    public static final FSkinColor BACK_COLOR = FSkinColor.get(Colors.CLR_THEME2);
    public static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);

    private final FButton btnOk, btnCancel;
    private final MessageLabel lblMessage;
    private String message;
    private CardView card = null ; 

    public void setCardView(final CardView card) {
        this.card = card ;
    }

    public VPrompt(String okText, String cancelText, FEventHandler okCommand, FEventHandler cancelCommand) {
        lblMessage = add(new MessageLabel());
        lblMessage.setLeft(BTN_WIDTH);
        lblMessage.setHeight(HEIGHT);
        btnOk = add(new FButton(okText, okCommand));
        btnCancel = add(new FButton(cancelText, cancelCommand));
        btnOk.setSize(BTN_WIDTH, HEIGHT);
        btnCancel.setSize(BTN_WIDTH, HEIGHT);
        btnOk.setCorner(Corner.BottomLeft);
        btnCancel.setCorner(Corner.BottomRight);
        btnOk.setEnabled(false); //disable buttons until first input queued
        btnCancel.setEnabled(false);
    }

    public FButton getBtnOk() {
        return btnOk;
    }

    public FButton getBtnCancel() {
        return btnCancel;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message0) {
        message = message0;
        card = null ;
    }
    public void setMessage(String message0, CardView card0) {
        message = message0;
        card = card0;
    }

    /** Flashes animation on input panel if play is currently waiting on input. */
    public void remind() {
        //SDisplayUtil.remind(view);
    }

    @Override
    protected void doLayout(float width, float height) {
        lblMessage.setWidth(width - 2 * BTN_WIDTH);
        btnCancel.setLeft(lblMessage.getRight());
    }

    @Override
    protected void drawBackground(Graphics g) {
        g.fillRect(BACK_COLOR, 0, 0, getWidth(), getHeight());
    }
    
    private class MessageLabel extends FDisplayObject {
        private final TextRenderer renderer = new TextRenderer();

        @Override
        public boolean tap(float x, float y, int count) {
            //if not enough room for prompt at given size, show magnify view
            float maxWidth = getWidth() - 2 * PADDING;
            float maxHeight = getHeight() - 2 * PADDING;
            TextBounds textBounds = renderer.getWrappedBounds(message, FONT, maxWidth);
            if (textBounds.height > maxHeight) {
                FMagnifyView.show(this, message, FORE_COLOR, BACK_COLOR, FONT, false);
            }
            return true;
        }

        @Override
        public boolean fling(float x, float y) {
            if ( card != null ) { 
                CardZoom.show(card);
            }
            return true;
        }

        @Override
        public boolean longPress(float x, float y) {
            if ( card != null ) { 
                CardZoom.show(card);
            }
            return true;
        }

        @Override
        public void draw(Graphics g) {
            if (!StringUtils.isEmpty(message)) {
                float x = PADDING;
                float y = PADDING;
                float w = getWidth() - 2 * PADDING;
                float h = getHeight() - 2 * PADDING;
                renderer.drawText(g, message, FONT, FORE_COLOR, x, y, w, h, y, h, true, Align.center, true);
            }
        }
    }
}
