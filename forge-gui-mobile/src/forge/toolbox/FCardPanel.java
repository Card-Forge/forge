package forge.toolbox;

import com.badlogic.gdx.graphics.Texture;

import forge.Forge.Graphics;
import forge.assets.ImageCache;
import forge.card.CardCharacteristicName;
import forge.card.CardFaceSymbols;
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
        if (showCardManaCostOverlay() && w < 200) {
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

        drawFoilEffect(g, card, x, y, w, h);
    }

    private void drawManaCost(final Graphics g, ManaCost cost, float x, float y, float w, float h, float manaSymbolSize) {
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
