package forge.screens.match.views;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.assets.ImageCache;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.player.LobbyPlayer;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.MagicStack;
import forge.menu.FDropDown;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FLabel;
import forge.utils.Utils;

public class VStack extends FDropDown {
    private static final float PADDING = 3;
    private static final float CARD_WIDTH = Utils.AVG_FINGER_WIDTH;
    private static final float CARD_HEIGHT = Math.round(CARD_WIDTH * FCardPanel.ASPECT_RATIO);
    private static final FSkinFont FONT = FSkinFont.get(11);
    private static final Color ALPHA_COMPOSITE = new Color(1, 1, 1, 0.5f);

    private final MagicStack stack;
    private final LobbyPlayer localPlayer;

    private int stackSize;

    public VStack(MagicStack stack0, LobbyPlayer localPlayer0) {
        stack = stack0;
        localPlayer = localPlayer0;
    }

    @Override
    protected boolean autoHide() {
        return false;
    }

    @Override
    public void update() {
        if (stackSize != stack.size()) {
            stackSize = stack.size();
            getMenuTab().setText("Stack (" + stackSize + ")");

            if (stackSize > 0) {
                if (!isVisible()) {
                    show();
                    return; //don't call super.update() since show handles this
                }
            }
            else {
                hide();
                return; //super.update() isn't needed if hidden
            }
        }
        super.update();
    }

    @Override
    protected ScrollBounds updateAndGetPaneSize(float maxWidth, float maxVisibleHeight) {
        clear();

        float height;
        float x = PADDING;
        float y = PADDING;
        float dy = PADDING - 1;
        float totalWidth = Math.round(maxWidth / 2);
        float width = totalWidth - 2 * PADDING;

        if (stack.isEmpty()) { //show label if stack empty
            FLabel label = add(new FLabel.Builder().text("[Empty]").fontSize(FONT.getSize()).align(HAlignment.CENTER).build());

            height = Math.round(label.getAutoSizeBounds().height) + 2 * PADDING;
            label.setBounds(x, y, width, height);
            return new ScrollBounds(totalWidth, y + height + PADDING);
        }
        else {
            StackInstanceDisplay display;
            boolean isTop = true;
            for (final SpellAbilityStackInstance stackInstance : stack) {
                display = add(new StackInstanceDisplay(stackInstance, isTop));
                height = display.getMinHeight(width);
                display.setBounds(x, y, width, height);
                y += height + dy;
                isTop = false;
            }
        }
        return new ScrollBounds(totalWidth, y + 1);
    }

    @Override
    protected void setScrollPositionsAfterLayout(float scrollLeft0, float scrollTop0) {
        super.setScrollPositionsAfterLayout(0, 0); //always scroll to top after layout
    }

    private class StackInstanceDisplay extends FDisplayObject {
        private final SpellAbilityStackInstance stackInstance;
        private final boolean isTop;
        private FSkinColor foreColor, backColor;
        private String text;

        private StackInstanceDisplay(SpellAbilityStackInstance stackInstance0, boolean isTop0) {
            stackInstance = stackInstance0;
            isTop = isTop0;
            Card card = stackInstance0.getSourceCard();

            text = stackInstance.getStackDescription();
            if (stackInstance.getSpellAbility().isOptionalTrigger() &&
                    card.getController().getController().getLobbyPlayer().equals(localPlayer)) {
                text = "(OPTIONAL) " + text;
            }

            if (stackInstance.getStackDescription().startsWith("Morph ")) {
                backColor = FSkinColor.getStandardColor(209, 156, 8);
                foreColor = FSkinColor.getStandardColor(Color.BLACK);
            }
            else if (CardUtil.getColors(card).isMulticolor()) {
                backColor = FSkinColor.getStandardColor(253, 175, 63);
                foreColor = FSkinColor.getStandardColor(Color.BLACK);
            }
            else if (card.isBlack()) {
                backColor = FSkinColor.getStandardColor(Color.BLACK);
                foreColor = FSkinColor.getStandardColor(Color.WHITE);
            }
            else if (card.isBlue()) {
                backColor = FSkinColor.getStandardColor(71, 108, 191);
                foreColor = FSkinColor.getStandardColor(Color.WHITE);
            }
            else if (card.isGreen()) {
                backColor = FSkinColor.getStandardColor(23, 95, 30);
                foreColor = FSkinColor.getStandardColor(Color.WHITE);
            }
            else if (card.isRed()) {
                backColor = FSkinColor.getStandardColor(214, 8, 8);
                foreColor = FSkinColor.getStandardColor(Color.WHITE);
            }
            else if (card.isWhite()) {
                backColor = FSkinColor.getStandardColor(Color.WHITE);
                foreColor = FSkinColor.getStandardColor(Color.BLACK);
            }
            else if (card.isArtifact() || card.isLand()) {
                backColor = FSkinColor.getStandardColor(111, 75, 43);
                foreColor = FSkinColor.getStandardColor(Color.WHITE);
            }
            else {
                backColor = FSkinColor.get(Colors.CLR_OVERLAY);
                foreColor = FSkinColor.get(Colors.CLR_TEXT);
            }

            if (!isTop) {
                backColor = backColor.alphaColor(ALPHA_COMPOSITE.a);
                foreColor = foreColor.alphaColor(ALPHA_COMPOSITE.a);
            }
        }

        private float getMinHeight(float width) {
            width -= CARD_WIDTH; //account for card picture
            width -= 3 * PADDING; //account for left and right insets and gap between picture and text
            float height = Math.max(CARD_HEIGHT, FONT.getFont().getWrappedBounds(text, width).height);
            height += 2 * PADDING;
            return Math.round(height);
        }

        @Override
        public void draw(Graphics g) {
            float w = getWidth();
            float h = getHeight();

            g.fillRect(backColor, 0, 0, w, h);

            float padding = PADDING;
            float cardWidth = CARD_WIDTH;
            float cardHeight = CARD_HEIGHT;
            float x = padding;
            float y = padding;

            if (!isTop) {
                g.setImageTint(ALPHA_COMPOSITE);
            }
            g.drawImage(ImageCache.getImage(stackInstance.getSourceCard()), x, y, cardWidth, cardHeight);
            if (!isTop) {
                g.clearImageTint();
            }

            x += cardWidth + padding;
            g.drawText(text, FONT, foreColor, x, y, w - x - padding, h - y - padding, true, HAlignment.LEFT, true);
        }
    }
}
