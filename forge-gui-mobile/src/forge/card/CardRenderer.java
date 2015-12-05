package forge.card;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import forge.Graphics;
import forge.ImageKeys;
import forge.assets.FImageComplex;
import forge.assets.FRotatedImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FTextureRegionImage;
import forge.assets.ImageCache;
import forge.card.CardDetailUtil.DetailColors;
import forge.card.CardZoom.ActivateHandler;
import forge.card.mana.ManaCost;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.item.IPaperCard;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.screens.match.MatchController;
import forge.toolbox.FList;
import forge.util.Utils;

public class CardRenderer {
    public enum CardStackPosition {
        Top,
        BehindHorz,
        BehindVert
    }

    private static final FSkinFont NAME_FONT = FSkinFont.get(16);
    public static final float NAME_BOX_TINT = 0.2f;
    public static final float TEXT_BOX_TINT = 0.1f;
    public static final float PT_BOX_TINT = 0.2f;
    private static final float MANA_COST_PADDING = Utils.scale(3);
    public static final float SET_BOX_MARGIN = Utils.scale(1);
    public static final float MANA_SYMBOL_SIZE = FSkinImage.MANA_1.getNearestHQWidth(2 * (NAME_FONT.getCapHeight() - MANA_COST_PADDING));
    private static final float NAME_COST_THRESHOLD = Utils.scale(200);
    private static final float BORDER_THICKNESS = Utils.scale(1);
    public static final float PADDING_MULTIPLIER = 0.021f;

    private static Color fromDetailColor(DetailColors detailColor) {
        return FSkinColor.fromRGB(detailColor.r, detailColor.g, detailColor.b);
    }

    public static Color getRarityColor(CardRarity rarity) {
        switch(rarity) {
        case Uncommon:
            return fromDetailColor(DetailColors.UNCOMMON);
        case Rare:
            return fromDetailColor(DetailColors.RARE);
        case MythicRare:
            return fromDetailColor(DetailColors.MYTHIC);
        case Special: //"Timeshifted" or other Special Rarity Cards
            return fromDetailColor(DetailColors.SPECIAL);
        default: //case BasicLand: + case Common:
            return fromDetailColor(DetailColors.COMMON);
        }
    }

    public static float getCardListItemHeight(boolean compactMode) {
        if (compactMode) {
            return MANA_SYMBOL_SIZE + 2 * FList.PADDING;
        }
        return Math.round(MANA_SYMBOL_SIZE + FSkinFont.get(12).getLineHeight() + 3 * FList.PADDING + 1);
    }

    private static final Map<String, FImageComplex> cardArtCache = new HashMap<String, FImageComplex>();
    public static final float CARD_ART_RATIO = 1.302f;

    //extract card art from the given card
    public static FImageComplex getCardArt(IPaperCard pc) {
        CardType type = pc.getRules().getType();
        return getCardArt(ImageKeys.getImageKey(pc, false), pc.getRules().getSplitType() == CardSplitType.Split, type.isPlane() || type.isPhenomenon());
    }
    public static FImageComplex getCardArt(CardView card) {
        CardTypeView type = card.getCurrentState().getType();
        return getCardArt(card.getCurrentState().getImageKey(), card.isSplitCard(), type.isPlane() || type.isPhenomenon());
    }
    public static FImageComplex getCardArt(String imageKey, boolean isSplitCard, boolean isHorizontalCard) {
        FImageComplex cardArt = cardArtCache.get(imageKey);
        if (cardArt == null) {
            Texture image = ImageCache.getImage(imageKey, true);
            if (image != null) {
                if (image == ImageCache.defaultImage) {
                    cardArt = CardImageRenderer.forgeArt;
                }
                else {
                    float x, y;
                    float w = image.getWidth();
                    float h = image.getHeight();
                    if (isSplitCard) { //allow rotated image for split cards
                        x = w * 33f / 250f;
                        y = 0; //delay adjusting y and h until drawn
                        w *= 106f / 250f;
                    }
                    else if (isHorizontalCard) { //allow rotated image for horizontal cards
                        float artX = 40f, artY = 40f;
                        float artW = 350f, artH = 156f;
                        float srcW = 430f, srcH = 300f;
                        if (w > h) {
                            x = w * 40f / 430f;
                            y = h * 40f / srcH;
                            w *= artW / srcW;
                            h *= artH / srcH;
                        }
                        else { //rotate art clockwise if its not the correct orientation
                            x = w * artY / srcH;
                            y = h * (srcW - artW - artX) / srcW;
                            w *= artH / srcH;
                            h *= artW / srcW;
                            cardArt = new FRotatedImage(image, Math.round(x), Math.round(y), Math.round(w), Math.round(h), true);
                            cardArtCache.put(imageKey, cardArt);
                            return cardArt;
                        }
                    }
                    else {
                        x = w * 0.1f;
                        y = h * 0.11f;
                        w -= 2 * x;
                        h *= 0.43f;
                        float ratioRatio = w / h / CARD_ART_RATIO;
                        if (ratioRatio > 1) { //if too wide, shrink width
                            float dw = w * (ratioRatio - 1);
                            w -= dw;
                            x += dw / 2;
                        }
                        else { //if too tall, shrink height
                            float dh = h * (1 - ratioRatio);
                            h -= dh;
                            y += dh / 2;
                        }
                    }
                    cardArt = new FTextureRegionImage(new TextureRegion(image, Math.round(x), Math.round(y), Math.round(w), Math.round(h)));
                }
                cardArtCache.put(imageKey, cardArt);
            }
        }
        return cardArt;
    }

    public static void drawCardListItem(Graphics g, FSkinFont font, FSkinColor foreColor, CardView card, int count, String suffix, float x, float y, float w, float h, boolean compactMode) {
        final CardStateView state = card.getCurrentState();
        if (card.getId() > 0) {
            drawCardListItem(g, font, foreColor, getCardArt(card), card, state.getSetCode(),
                    state.getRarity(), state.getPower(), state.getToughness(),
                    state.getLoyalty(), count, suffix, x, y, w, h, compactMode);
        }
        else { //if fake card, just draw card name centered
            String name = state.getName();
            if (count > 0) { //preface name with count if applicable
                name = count + " " + name;
            }
            if (suffix != null) {
                name += suffix;
            }
            g.drawText(name, font, foreColor, x, y, w, h, false, HAlignment.CENTER, true);
        }
    }
    public static void drawCardListItem(Graphics g, FSkinFont font, FSkinColor foreColor, IPaperCard pc, int count, String suffix, float x, float y, float w, float h, boolean compactMode) {
        final CardView card = CardView.getCardForUi(pc);
        final CardStateView state = card.getCurrentState();
        drawCardListItem(g, font, foreColor, getCardArt(pc), card, pc.getEdition(),
                pc.getRarity(), state.getPower(), state.getToughness(),
                state.getLoyalty(), count, suffix, x, y, w, h, compactMode);
    }
    public static void drawCardListItem(Graphics g, FSkinFont font, FSkinColor foreColor, FImageComplex cardArt, CardView card, String set, CardRarity rarity, int power, int toughness, int loyalty, int count, String suffix, float x, float y, float w, float h, boolean compactMode) {
        float cardArtHeight = h + 2 * FList.PADDING;
        float cardArtWidth = cardArtHeight * CARD_ART_RATIO;
        if (cardArt != null) {
            float artX = x - FList.PADDING;
            float artY = y - FList.PADDING;
            if (card.isSplitCard()) {
                //draw split art with proper orientation
                float srcY = cardArt.getHeight() * 13f / 354f;
                float srcHeight = cardArt.getHeight() * 150f / 354f;
                float dh = srcHeight * (1 - cardArt.getWidth() / srcHeight / CARD_ART_RATIO);
                srcHeight -= dh;
                srcY += dh / 2;
                g.drawRotatedImage(cardArt.getTexture(), artX, artY, cardArtHeight, cardArtWidth / 2, artX + cardArtWidth / 2, artY + cardArtWidth / 2, cardArt.getRegionX(), (int)srcY, (int)cardArt.getWidth(), (int)srcHeight, -90);
                g.drawRotatedImage(cardArt.getTexture(), artX, artY + cardArtWidth / 2, cardArtHeight, cardArtWidth / 2, artX + cardArtWidth / 2, artY + cardArtWidth / 2, cardArt.getRegionX(), (int)cardArt.getHeight() - (int)(srcY + srcHeight), (int)cardArt.getWidth(), (int)srcHeight, -90);
            }
            else {
                g.drawImage(cardArt, artX, artY, cardArtWidth, cardArtHeight);
            }
        }

        //render card name and mana cost on first line
        float manaCostWidth = 0;
        ManaCost mainManaCost = card.getCurrentState().getManaCost();
        if (card.isSplitCard()) {
            //handle rendering both parts of split card
            ManaCost otherManaCost = card.getAlternateState().getManaCost();
            manaCostWidth = CardFaceSymbols.getWidth(otherManaCost, MANA_SYMBOL_SIZE) + MANA_COST_PADDING;
            CardFaceSymbols.drawManaCost(g, otherManaCost, x + w - manaCostWidth + MANA_COST_PADDING, y, MANA_SYMBOL_SIZE);
            //draw "//" between two parts of mana cost
            manaCostWidth += font.getBounds("//").width + MANA_COST_PADDING;
            g.drawText("//", font, foreColor, x + w - manaCostWidth + MANA_COST_PADDING, y, w, MANA_SYMBOL_SIZE, false, HAlignment.LEFT, true);
        }
        manaCostWidth += CardFaceSymbols.getWidth(mainManaCost, MANA_SYMBOL_SIZE);
        CardFaceSymbols.drawManaCost(g, mainManaCost, x + w - manaCostWidth, y, MANA_SYMBOL_SIZE);

        x += cardArtWidth;
        String name = card.getCurrentState().getName();
        if (count > 0) { //preface name with count if applicable
            name = count + " " + name;
        }
        if (suffix != null) {
            name += suffix;
        }
        g.drawText(name, font, foreColor, x, y, w - manaCostWidth - cardArtWidth - FList.PADDING, MANA_SYMBOL_SIZE, false, HAlignment.LEFT, true);

        if (compactMode) {
            return; //skip second line if rendering in compact mode
        }

        y += MANA_SYMBOL_SIZE + FList.PADDING + SET_BOX_MARGIN;

        //render card type, p/t, and set box on second line
        FSkinFont typeFont = FSkinFont.get(12);
        float availableTypeWidth = w - cardArtWidth;
        float lineHeight = typeFont.getLineHeight();
        if (!StringUtils.isEmpty(set)) {
            float setWidth = getSetWidth(typeFont, set);
            availableTypeWidth -= setWidth;
            drawSetLabel(g, typeFont, set, rarity, x + availableTypeWidth + SET_BOX_MARGIN, y - SET_BOX_MARGIN, setWidth, lineHeight + 2 * SET_BOX_MARGIN);
        }
        String type = CardDetailUtil.formatCardType(card.getCurrentState(), true);
        if (card.getCurrentState().isCreature()) { //include P/T or Loyalty at end of type
            type += " (" + power + " / " + toughness + ")";
        }
        else if (card.getCurrentState().isPlaneswalker()) {
            type += " (" + loyalty + ")";
        }
        g.drawText(type, typeFont, foreColor, x, y, availableTypeWidth, lineHeight, false, HAlignment.LEFT, true);
    }

    public static boolean cardListItemTap(List<?> cards, int selectedIndex, ActivateHandler activateHandler, float x, float y, int count, boolean compactMode) {
        if (x <= getCardListItemHeight(compactMode) * CARD_ART_RATIO) {
            CardZoom.show(cards, selectedIndex, activateHandler);
            return true;
        }
        return false;
    }
    public static boolean paperCardListItemTap(List<?> cards, int selectedIndex, ActivateHandler activateHandler, float x, float y, int count, boolean compactMode) {
        float cardArtHeight = getCardListItemHeight(compactMode);
        float cardArtWidth = cardArtHeight * CARD_ART_RATIO;
        if (x <= cardArtWidth && y <= cardArtHeight) {
            CardZoom.show(cards, selectedIndex, activateHandler);
            return true;
        }
        return false;
    }

    public static float getSetWidth(FSkinFont font, String set) {
        return font.getBounds(set).width + font.getCapHeight();
    }

    public static void drawSetLabel(Graphics g, FSkinFont font, String set, CardRarity rarity, float x, float y, float w, float h) {
        Color backColor = getRarityColor(rarity);
        Color foreColor = FSkinColor.getHighContrastColor(backColor);
        g.fillRect(backColor, x, y, w, h);
        g.drawText(set, font, foreColor, x, y, w, h, false, HAlignment.CENTER, true);
    }

    public static void drawCard(Graphics g, IPaperCard pc, float x, float y, float w, float h, CardStackPosition pos) {
        Texture image = ImageCache.getImage(pc);
        if (image != null) {
            if (image == ImageCache.defaultImage) {
                CardImageRenderer.drawCardImage(g, CardView.getCardForUi(pc), false, x, y, w, h, pos);
            }
            else {
                g.drawImage(image, x, y, w, h);
            }
            if (pc.isFoil()) { //draw foil effect if needed
                final CardView card = CardView.getCardForUi(pc);
                if (card.getCurrentState().getFoilIndex() == 0) { //if foil finish not yet established, assign a random one
                    card.getCurrentState().setFoilIndexOverride(-1);
                }
                drawFoilEffect(g, card, x, y, w, h);
            }
        }
        else { //draw cards without textures as just a black rectangle
            g.fillRect(Color.BLACK, x, y, w, h);
        }
    }
    public static void drawCard(Graphics g, CardView card, float x, float y, float w, float h, CardStackPosition pos) {
        Texture image = ImageCache.getImage(card);
        if (image != null) {
            if (image == ImageCache.defaultImage) {
                CardImageRenderer.drawCardImage(g, card, false, x, y, w, h, pos);
            }
            else {
                g.drawImage(image, x, y, w, h);
            }
            drawFoilEffect(g, card, x, y, w, h);
        }
        else { //draw cards without textures as just a black rectangle
            g.fillRect(Color.BLACK, x, y, w, h);
        }
    }

    public static void drawCardWithOverlays(Graphics g, CardView card, float x, float y, float w, float h, CardStackPosition pos) {
        drawCard(g, card, x, y, w, h, pos);

        float padding = w * PADDING_MULTIPLIER; //adjust for card border
        x += padding;
        y += padding;
        w -= 2 * padding;
        h -= 2 * padding;

        boolean canShow = MatchController.instance.mayView(card);

        // TODO: A hacky workaround is currently used to make the game not leak the color information for Morph cards.
        final CardStateView details = card.getCurrentState();
        final boolean isFaceDown = details.getState() == CardStateName.FaceDown;
        final DetailColors borderColor = isFaceDown ? CardDetailUtil.DetailColors.FACE_DOWN : CardDetailUtil.getBorderColor(details, canShow); // canShow doesn't work here for face down Morphs
        Color color = FSkinColor.fromRGB(borderColor.r, borderColor.g, borderColor.b);
        color = FSkinColor.tintColor(Color.WHITE, color, CardRenderer.PT_BOX_TINT);

        //draw name and mana cost overlays if card is small or default card image being used
        if (h <= NAME_COST_THRESHOLD && canShow) {
            if (showCardNameOverlay(card)) {
                g.drawOutlinedText(details.getName(), FSkinFont.forHeight(h * 0.18f), Color.WHITE, Color.BLACK, x + padding, y + padding, w - 2 * padding, h * 0.4f, true, HAlignment.LEFT, false);
            }
            if (showCardManaCostOverlay(card)) {
                float manaSymbolSize = w / 4;
                if (card.isSplitCard() && card.hasAlternateState()) {
                    float dy = manaSymbolSize / 2 + Utils.scale(5);
                    drawManaCost(g, card.getCurrentState().getManaCost(), x - padding, y + dy, w + 2 * padding, h, manaSymbolSize);
                    drawManaCost(g, card.getAlternateState().getManaCost(), x - padding, y - dy, w + 2 * padding, h, manaSymbolSize);
                }
                else {
                    drawManaCost(g, card.getCurrentState().getManaCost(), x - padding, y, w + 2 * padding, h, manaSymbolSize);
                }
            }
        }

        if (pos == CardStackPosition.BehindVert) { return; } //remaining rendering not needed if card is behind another card in a vertical stack
        boolean onTop = (pos == CardStackPosition.Top);

        if (canShow && showCardIdOverlay(card)) {
            FSkinFont idFont = FSkinFont.forHeight(h * 0.12f);
            float idHeight = idFont.getCapHeight();
            g.drawOutlinedText(String.valueOf(card.getId()), idFont, Color.WHITE, Color.BLACK, x + padding, y + h - idHeight - padding, w, h, false, HAlignment.LEFT, false);
        }

        int number = 0;
        if (card.getCounters() != null) {
            for (final Integer i : card.getCounters().values()) {
                number += i.intValue();
            }
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

        if (card.isAttacking()) {
            CardFaceSymbols.drawSymbol("attack", g, combatXSymbols, ySymbols, otherSymbolsSize, otherSymbolsSize);
        }
        else if (card.isBlocking()) {
            CardFaceSymbols.drawSymbol("defend", g, combatXSymbols, ySymbols, otherSymbolsSize, otherSymbolsSize);
        }

        if (onTop && card.isSick()) {
            //only needed if on top since otherwise symbol will be hidden
            CardFaceSymbols.drawSymbol("summonsick", g, stateXSymbols, ySymbols, otherSymbolsSize, otherSymbolsSize);
        }

        if (card.isPhasedOut()) {
            CardFaceSymbols.drawSymbol("phasing", g, stateXSymbols, ySymbols, otherSymbolsSize, otherSymbolsSize);
        }

        if (MatchController.instance.isUsedToPay(card)) {
            float sacSymbolSize = otherSymbolsSize * 1.2f;
            CardFaceSymbols.drawSymbol("sacrifice", g, (x + (w / 2)) - sacSymbolSize / 2, (y + (h / 2)) - sacSymbolSize / 2, otherSymbolsSize, otherSymbolsSize);
        }

        if (onTop && showCardPowerOverlay(card) && (canShow || card.isFaceDown())) { //make sure card p/t box appears on top
            //only needed if on top since otherwise P/T will be hidden
            drawPtBox(g, card, details, color, x, y, w, h);
        }
    }

    private static void drawPtBox(Graphics g, CardView card, CardStateView details, Color color, float x, float y, float w, float h) {
        //use array of strings to render separately with a tiny amount of space in between
        //instead of using actual spaces which are too wide
        List<String> pieces = new ArrayList<String>();
        if (details.isCreature()) {
            pieces.add(String.valueOf(details.getPower()));
            pieces.add("/");
            pieces.add(String.valueOf(details.getToughness()));
        }
        if (details.isPlaneswalker()) {
            if (pieces.isEmpty()) {
                pieces.add(String.valueOf(details.getLoyalty()));
            }
            else {
                pieces.add("(" + details.getLoyalty() + ")");
            }
        }
        if (pieces.isEmpty()) { return; }

        FSkinFont font = FSkinFont.forHeight(h * 0.15f);
        float padding = Math.round(font.getCapHeight() / 4);
        float boxWidth = padding;
        List<Float> pieceWidths = new ArrayList<Float>();
        for (String piece : pieces) {
            float pieceWidth = font.getBounds(piece).width + padding;
            pieceWidths.add(pieceWidth);
            boxWidth += pieceWidth;
        }
        float boxHeight = font.getCapHeight() + font.getAscent() + 2 * padding;

        x += w - boxWidth;
        y += h - boxHeight;
        w = boxWidth;
        h = boxHeight;

        //draw card damage above P/T box if needed
        if (card.getDamage() > 0) {
            g.drawOutlinedText(">" + card.getDamage() + "<", font, Color.RED, Color.WHITE, x, y - h + padding, w, h, false, HAlignment.CENTER, true);
        }

        g.fillRect(color, x, y, w, h);
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);

        x += padding;
        for (int i = 0; i < pieces.size(); i++) {
            g.drawText(pieces.get(i), font, Color.BLACK, x, y, w, h, false, HAlignment.LEFT, true);
            x += pieceWidths.get(i);
        }
    }

    private static void drawManaCost(Graphics g, ManaCost cost, float x, float y, float w, float h, float manaSymbolSize) {
        float manaCostWidth = CardFaceSymbols.getWidth(cost, manaSymbolSize);
        if (manaCostWidth > w) {
            manaCostWidth = w;
            manaSymbolSize = w / cost.getGlyphCount();
        }
        CardFaceSymbols.drawManaCost(g, cost, x + (w - manaCostWidth) / 2, y + (h - manaSymbolSize) / 2, manaSymbolSize);
    }

    public static void drawFoilEffect(Graphics g, CardView card, float x, float y, float w, float h) {
        if (isPreferenceEnabled(FPref.UI_OVERLAY_FOIL_EFFECT) && MatchController.instance.mayView(card)) {
            int foil = card.getCurrentState().getFoilIndex();
            if (foil > 0) {
                CardFaceSymbols.drawOther(g, String.format("foil%02d", foil), x, y, w, h);
            }
        }
    }

    private static boolean isPreferenceEnabled(FPref preferenceName) {
        return FModel.getPreferences().getPrefBoolean(preferenceName);
    }

    private static boolean isShowingOverlays(CardView card) {
        return isPreferenceEnabled(FPref.UI_SHOW_CARD_OVERLAYS) && card != null;
    }

    private static boolean showCardNameOverlay(CardView card) {
        return isShowingOverlays(card) && isPreferenceEnabled(FPref.UI_OVERLAY_CARD_NAME);
    }

    private static boolean showCardPowerOverlay(CardView card) {
        return isShowingOverlays(card) && isPreferenceEnabled(FPref.UI_OVERLAY_CARD_POWER);
    }

    private static boolean showCardManaCostOverlay(CardView card) {
        return isShowingOverlays(card) &&
                isPreferenceEnabled(FPref.UI_OVERLAY_CARD_MANA_COST);
    }

    private static boolean showCardIdOverlay(CardView card) {
        return card.getId() > 0 && isShowingOverlays(card) && isPreferenceEnabled(FPref.UI_OVERLAY_CARD_ID);
    }
}
