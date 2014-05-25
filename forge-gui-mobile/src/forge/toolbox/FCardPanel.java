package forge.toolbox;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.ImageCache;
import forge.card.CardCharacteristicName;
import forge.card.CardDetailUtil;
import forge.card.CardDetailUtil.DetailColors;
import forge.card.CardFaceSymbols;
import forge.card.CardRenderer;
import forge.card.mana.ManaCost;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.screens.match.FControl;
import forge.util.Utils;

public class FCardPanel extends FDisplayObject {
    public static final float TAPPED_ANGLE = -90;
    public static final float ASPECT_RATIO = 3.5f / 2.5f;
    public static final float PADDING = Utils.scaleMin(2);

    private Card card;
    private boolean tapped;
    private float tappedAngle = 0;
    private boolean highlighted;

    public FCardPanel() {
        this(null);
    }
    public FCardPanel(Card card0) {
        card = card0;
    }

    public Card getCard() {
        return card;
    }
    public void setCard(Card card0) {
        card = card0;
    }

    public boolean isHighlighted() {
        return highlighted;
    }
    public void setHighlighted(boolean highlighted0) {
        highlighted = highlighted0;
    }

    public boolean isTapped() {
        return tapped;
    }
    public void setTapped(final boolean tapped0) {
        tapped = tapped0;
    }

    public float getTappedAngle() {
        return tappedAngle;
    }
    public void setTappedAngle(float tappedAngle0) {
        tappedAngle = tappedAngle0;
    }

    protected boolean renderedCardContains(float x, float y) {
        float left = PADDING;
        float top = PADDING;
        float w = getWidth() - 2 * PADDING;
        float h = getHeight() - 2 * PADDING;
        if (w == h) { //adjust width if needed to make room for tapping
            w = h / ASPECT_RATIO;
        }

        if (tapped) { //rotate box if tapped
            top += h - w;
            float temp = w;
            w = h;
            h = temp;
        }

        return x >= left && x <= left + w && y >= top && y <= top + h;
    }

    @Override
    public void draw(Graphics g) {
        if (card == null) { return; }

        float x = PADDING;
        float y = PADDING;
        float w = getWidth() - 2 * PADDING;
        float h = getHeight() - 2 * PADDING;
        if (w == h) { //adjust width if needed to make room for tapping
            w = h / ASPECT_RATIO;
        }

        Texture image = ImageCache.getImage(card);
        if (tapped) {
            float edgeOffset = w / 2f;
            g.setRotateTransform(x + edgeOffset, y + h - edgeOffset, tappedAngle);
        }

        g.drawImage(image, x, y, w, h);
        drawOverlays(g, x, y, w, h);

        if (tapped) {
            g.clearTransform();
        }
    }

    private void drawOverlays(Graphics g, float x, float y, float w, float h) {
        drawFoilEffect(g, card, x, y, w, h);

        boolean needNameAndManaCostOverlays = (w < 200);
        if (showCardManaCostOverlay() && needNameAndManaCostOverlays) {
            float manaSymbolSize = w / 4;
            if (card.isSplitCard() && card.getCurState() == CardCharacteristicName.Original) {
                float dy = manaSymbolSize / 2 + Utils.scaleY(5);
                drawManaCost(g, card.getRules().getMainPart().getManaCost(), x, y + dy, w, h, manaSymbolSize);
                drawManaCost(g, card.getRules().getOtherPart().getManaCost(), x, y - dy, w, h, manaSymbolSize);
            }
            else {
                drawManaCost(g, card.getManaCost(), x, y, w, h, manaSymbolSize);
            }
        }

        int number = 0;
        for (final Integer i : card.getCounters().values()) {
            number += i.intValue();
        }

        final int counters = number;

        float countersSize = w / 2;
        final float xCounters = x - countersSize / 2;
        final float yCounters = y + h * 2 / 3 - countersSize;

        if (counters == 1) {
            CardFaceSymbols.drawSymbol("counters1", g, xCounters, yCounters, countersSize, countersSize);
        }
        else if (counters == 2) {
            CardFaceSymbols.drawSymbol("counters2", g, xCounters, yCounters, countersSize, countersSize);
        }
        else if (counters == 3) {
            CardFaceSymbols.drawSymbol("counters3", g, xCounters, yCounters, countersSize, countersSize);
        }
        else if (counters > 3) {
            CardFaceSymbols.drawSymbol("countersMulti", g, xCounters, yCounters, countersSize, countersSize);
        }

        float otherSymbolsSize = w / 2;
        final float combatXSymbols = (x + (w / 4)) - otherSymbolsSize / 2;
        final float stateXSymbols = (x + (w / 2)) - otherSymbolsSize / 2;
        final float ySymbols = (y + h) - (h / 8) - otherSymbolsSize / 2;

        Combat combat = card.getGame().getCombat();
        if (combat != null) {
            if (combat.isAttacking(card)) {
                CardFaceSymbols.drawSymbol("attack", g, combatXSymbols, ySymbols, otherSymbolsSize, otherSymbolsSize);
            }
            if (combat.isBlocking(card)) {
                CardFaceSymbols.drawSymbol("defend", g, combatXSymbols, ySymbols, otherSymbolsSize, otherSymbolsSize);
            }
        }

        if (card.isSick() && card.isInPlay()) {
            CardFaceSymbols.drawSymbol("summonsick", g, stateXSymbols, ySymbols, otherSymbolsSize, otherSymbolsSize);
        }

        if (card.isPhasedOut()) {
            CardFaceSymbols.drawSymbol("phasing", g, stateXSymbols, ySymbols, otherSymbolsSize, otherSymbolsSize);
        }

        if (FControl.isUsedToPay(card)) {
            float sacSymbolSize = otherSymbolsSize * 1.2f;
            CardFaceSymbols.drawSymbol("sacrifice", g, (x + (w / 2)) - sacSymbolSize / 2, (y + (h / 2)) - sacSymbolSize / 2, otherSymbolsSize, otherSymbolsSize);
        }

        float padding = w * 0.021f; //adjust for card border
        x += padding;
        y += padding;
        w -= 2 * padding;
        h -= 2 * padding;

        DetailColors borderColor = CardDetailUtil.getBorderColor(card, FControl.mayShowCard(card));
        Color color = FSkinColor.fromRGB(borderColor.r, borderColor.g, borderColor.b);
        color = FSkinColor.tintColor(Color.WHITE, color, CardRenderer.PT_BOX_TINT);

        if (showCardNameOverlay() && needNameAndManaCostOverlays) {
            g.drawOutlinedText(card.getName(), FSkinFont.get(12), Color.WHITE, Color.BLACK, x, y + padding, w, h * 0.4f, true, HAlignment.LEFT, false);
        }
        if (showCardIdOverlay()) {
            drawIdBox(g, color, x, y, w, h);
        }
        if (showCardPowerOverlay()) {
            drawPtBox(g, color, x, y, w, h);
        }
    }

    private void drawIdBox(Graphics g, Color color, float x, float y, float w, float h) {
        if (card.getUniqueNumber() <= 0) { return; }

        String text = String.valueOf(card.getUniqueNumber());
        FSkinFont font = FSkinFont.forHeight(h * 0.12f);
        float padding = Math.round(font.getFont().getCapHeight() / 8);
        float boxWidth = font.getFont().getBounds(text).width + 2 * padding;
        float boxHeight = font.getFont().getCapHeight() + font.getFont().getAscent() + 2 * padding;

        y += h - boxHeight;
        w = boxWidth;
        h = boxHeight;

        g.fillRect(color, x, y, w, h);
        g.drawRect(Utils.scaleMin(1), Color.BLACK, x, y, w, h);
        g.drawText(text, font, Color.BLACK, Math.round(x) + padding, y, w, h, false, HAlignment.LEFT, true);
    }

    private void drawPtBox(Graphics g, Color color, float x, float y, float w, float h) {
        //use array of strings to render separately with a tiny amount of space in between
        //instead of using actual spaces which are too wide
        List<String> pieces = new ArrayList<String>();
        if (card.isCreature()) {
            pieces.add(String.valueOf(card.getNetAttack()));
            pieces.add("/");
            pieces.add(String.valueOf(card.getNetDefense()));
        }
        if (card.isPlaneswalker()) {
            if (pieces.isEmpty()) {
                pieces.add(String.valueOf(card.getCurrentLoyalty()));
            }
            else {
                pieces.add("(" + card.getCurrentLoyalty() + ")");
            }
        }
        if (pieces.isEmpty()) { return; }

        FSkinFont font = FSkinFont.forHeight(h * 0.15f);
        float padding = Math.round(font.getFont().getCapHeight() / 4);
        float boxWidth = padding;
        List<Float> pieceWidths = new ArrayList<Float>();
        for (String piece : pieces) {
            float pieceWidth = font.getFont().getBounds(piece).width + padding;
            pieceWidths.add(pieceWidth);
            boxWidth += pieceWidth;
        }
        float boxHeight = font.getFont().getCapHeight() + font.getFont().getAscent() + 2 * padding;

        x += w - boxWidth;
        y += h - boxHeight;
        w = boxWidth;
        h = boxHeight;

        g.fillRect(color, x, y, w, h);
        g.drawRect(Utils.scaleMin(1), Color.BLACK, x, y, w, h);

        x += padding;
        for (int i = 0; i < pieces.size(); i++) {
            g.drawText(pieces.get(i), font, Color.BLACK, x, y, w, h, false, HAlignment.LEFT, true);
            x += pieceWidths.get(i);
        }
    }

    private void drawManaCost(Graphics g, ManaCost cost, float x, float y, float w, float h, float manaSymbolSize) {
        float manaCostWidth = CardFaceSymbols.getWidth(cost, manaSymbolSize);
        CardFaceSymbols.drawManaCost(g, cost, x + (w - manaCostWidth) / 2, y + (h - manaSymbolSize) / 2, manaSymbolSize);
    }

    public static void drawFoilEffect(Graphics g, Card card, float x, float y, float w, float h) {
        if (isPreferenceEnabled(FPref.UI_OVERLAY_FOIL_EFFECT)) {
            int foil = card.getFoil();
            if (foil > 0) {
                CardFaceSymbols.drawOther(g, String.format("foil%02d", foil), x, y, w, h);
            }
        }
    }

    private static boolean isPreferenceEnabled(FPref preferenceName) {
        return FModel.getPreferences().getPrefBoolean(preferenceName);
    }

    private boolean isShowingOverlays() {
        return isPreferenceEnabled(FPref.UI_SHOW_CARD_OVERLAYS) && card != null && FControl.mayShowCard(card);
    }

    private boolean showCardNameOverlay() {
        return isShowingOverlays() && isPreferenceEnabled(FPref.UI_OVERLAY_CARD_NAME);
    }

    private boolean showCardPowerOverlay() {
        return isShowingOverlays() && isPreferenceEnabled(FPref.UI_OVERLAY_CARD_POWER);
    }

    private boolean showCardManaCostOverlay() {
        return isShowingOverlays() &&
                isPreferenceEnabled(FPref.UI_OVERLAY_CARD_MANA_COST) &&
                !getCard().isFaceDown();
    }

    private boolean showCardIdOverlay() {
        return isShowingOverlays() && isPreferenceEnabled(FPref.UI_OVERLAY_CARD_ID);
    }
}
