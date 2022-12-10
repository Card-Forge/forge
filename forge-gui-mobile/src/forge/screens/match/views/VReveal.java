package forge.screens.match.views;

import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.card.CardRenderer;
import forge.card.CardZoom;
import forge.game.card.CardView;
import forge.gui.card.CardDetailUtil;
import forge.menu.FDropDown;
import forge.screens.match.MatchController;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FLabel;
import forge.trackable.TrackableCollection;
import forge.util.CardTranslation;
import forge.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class VReveal extends FDropDown {
    public static final float MARGINS = Utils.scale(4);
    private static final FSkinFont FONT = FSkinFont.get(11);
    public static final float PADDING = Utils.scale(3);
    private TrackableCollection<CardView> revealed;

    private static FSkinColor getAltRowColor() {
        if (Forge.isMobileAdventureMode)
            return FSkinColor.get(FSkinColor.Colors.ADV_CLR_ZEBRA);
        return FSkinColor.get(FSkinColor.Colors.CLR_ZEBRA);
    }

    private static FSkinColor getRowColor() {
        return getAltRowColor().darker();
    }

    private static FSkinColor getForeColor() {
        if (Forge.isMobileAdventureMode)
            return FSkinColor.get(FSkinColor.Colors.ADV_CLR_TEXT);
        return FSkinColor.get(FSkinColor.Colors.CLR_TEXT);
    }

    private final TextRenderer renderer = new TextRenderer(false);

    @Override
    protected boolean autoHide() {
        return true;
    }

    @Override
    protected void drawBackground(Graphics g) {
        float w = getWidth();
        float h = getHeight();
        g.fillRect(getRowColor(), 0, 0, w, h); //can fill background with main row color since drop down will never be taller than number of rows
    }

    @Override
    protected ScrollBounds updateAndGetPaneSize(float maxWidth, float maxVisibleHeight) {
        float x = MARGINS;
        float y = MARGINS;
        float totalWidth = Forge.getScreenWidth();
        float width = totalWidth - 2 * MARGINS;
        float entryHeight;
        float entryWidth = totalWidth;
        float minWidth = 4 * Utils.AVG_FINGER_WIDTH;
        if (entryWidth < minWidth) {
            entryWidth = minWidth;
        }
        revealed = MatchController.instance.getGameView().getRevealedCollection();
        if (revealed == null || revealed.isEmpty()) {
            FLabel label = add(new FLabel.Builder().text("[" + Forge.getLocalizer().getMessage("lblEmpty") + "]").font(FSkinFont.get(11)).align(Align.center).build());

            float height = Math.round(label.getAutoSizeBounds().height) + 2 * PADDING;
            label.setBounds(x, y, width, height);

            return new ScrollBounds(totalWidth, y + height + MARGINS);
        } else {
            clear();
            boolean isAltRow = false;
            RevealEntryDisplay revealEntryDisplay;
            x = getMenuTab().screenPos.x;
            y = 1;
            for (CardView c : revealed) {
                revealEntryDisplay = add(new RevealEntryDisplay(c, isAltRow));
                isAltRow = !isAltRow;
                entryHeight = revealEntryDisplay.getMinHeight(entryWidth) + MARGINS;
                revealEntryDisplay.setBounds(0, y, entryWidth, entryHeight);
                y += entryHeight;
            }
        }
        return new ScrollBounds(totalWidth, y + MARGINS);
    }

    private class RevealEntryDisplay extends FDisplayObject {
        CardView card;
        boolean altRow;
        String text;
        FImage cardArt;

        private RevealEntryDisplay(CardView cardView, boolean isAltRow) {
            card = cardView;
            altRow = isAltRow;
            text = CardTranslation.getTranslatedName(card.getCurrentState().getName()) + "\n" + formatType();
            cardArt = CardRenderer.getCardArt(cardView);
        }

        private float getMinHeight(float width) {
            width -= 2 * PADDING; //account for left and right insets
            float height = renderer.getWrappedBounds("\n", FONT, width).height;
            height += 2 * PADDING;
            return Math.round(height);
        }

        @Override
        public boolean tap(float x, float y, int count) {
            try {
                List<CardView> cardViewList = new ArrayList<>(revealed);
                int index = cardViewList.indexOf(card);
                CardZoom.show(cardViewList, index, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        public void draw(Graphics g) {
            float w = getWidth();
            float h = getHeight();
            float cardArtWidth = h * 1.302f;

            if (altRow) {
                g.fillRect(getAltRowColor(), 0, 0, w, h);
            }
            if (isHovered()) {
                g.fillRect(getForeColor().brighter().alphaColor(0.3f), 0, 0, w, h);
            }
            g.drawImage(cardArt, 0, 0, cardArtWidth, h);
            //use full height without padding so text not scaled down
            renderer.drawText(g, text, FONT, getForeColor(), cardArtWidth + PADDING, PADDING, w - (2 * PADDING + cardArtWidth), h, 0, h, false, Align.left, false);
        }

        private String formatType() {
            String type = CardDetailUtil.formatCardType(card.getCurrentState(), true);
            if (card.getCurrentState().isCreature()) { //include P/T or Loyalty at end of type
                type += " (" + card.getCurrentState().getPower() + " / " + card.getCurrentState().getToughness() + ")";
            } else if (card.getCurrentState().isPlaneswalker()) {
                type += " (" + card.getCurrentState().getLoyalty() + ")";
            } else if (card.getCurrentState().getType().hasSubtype("Vehicle")) {
                type += String.format(" [%s / %s]", card.getCurrentState().getPower(), card.getCurrentState().getToughness());
            }
            return type;
        }
    }
}
