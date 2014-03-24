package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;

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
import forge.utils.Utils;

public class VStack extends FDropDown {
    private static final float PADDING = Utils.AVG_FINGER_WIDTH * 0.1f;
    private static final float CARD_WIDTH = Utils.AVG_FINGER_WIDTH;
    private static final float CARD_HEIGHT = CARD_WIDTH * FCardPanel.ASPECT_RATIO;
    private static final FSkinFont FONT = FSkinFont.get(11);

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

        float y = 0;
        float width = maxWidth / 2;
        StackInstanceDisplay display;
        float height = 0;
        float scale = 1; //scale size down as you go from top to bottom
        List<StackInstanceDisplay> displays = new ArrayList<StackInstanceDisplay>();

        for (final SpellAbilityStackInstance stackInstance : stack) {
            if (y > 0) {
                y -= CARD_HEIGHT * scale / 4; //allow partial overlap between layers of stack
            }
            display = new StackInstanceDisplay(stackInstance);
            display.scale = scale;
            height = display.getMinHeight(width);
            display.setBounds(width - width * scale, y, width * scale, height);
            y += height;
            scale *= 0.85f;
            displays.add(display);
        }
        //add in reverse order so top of stack appears on top
        for (int i = displays.size() - 1; i >= 0; i--) {
            add(displays.get(i));
        }
        return new ScrollBounds(width, y);
    }

    private class StackInstanceDisplay extends FDisplayObject {
        private final SpellAbilityStackInstance stackInstance;
        private FSkinColor foreColor, backColor;
        private float scale;
        private String text;

        private StackInstanceDisplay(SpellAbilityStackInstance stackInstance0) {
            stackInstance = stackInstance0;
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

            foreColor = foreColor.alphaColor(0.9f);
            backColor = backColor.alphaColor(0.9f);
        }

        private float getMinHeight(float width) {
            width -= CARD_WIDTH; //account for card picture
            width -= 3 * PADDING; //account for left and right insets and gap between picture and text
            float height = Math.max(CARD_HEIGHT, FONT.getFont().getWrappedBounds(text, width).height);
            height += 2 * PADDING;
            return height * scale;
        }

        @Override
        public void draw(Graphics g) {
            float w = getWidth();
            float h = getHeight();
            g.startClip(0, 0, w, h);

            g.fillRect(backColor, 0, 0, w, h);
            g.drawRect(2, foreColor, 0, 0, w, h);

            float padding = PADDING * scale;
            float cardWidth = CARD_WIDTH * scale;
            float cardHeight = CARD_HEIGHT * scale;

            float x = padding;
            float y = padding;
            g.drawImage(ImageCache.getImage(stackInstance.getSourceCard()), x, y, cardWidth, cardHeight);
            
            x += cardWidth + padding;
            w -= x + padding;
            h -= y + padding;

            g.drawText(text, FONT, foreColor, x, y, w, h, true, HAlignment.LEFT, true);

            g.endClip();
        }
    }
}
