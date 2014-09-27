package forge.card;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Graphics;
import forge.assets.FBufferedImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FSkinTexture;
import forge.assets.TextRenderer;
import forge.card.CardDetailUtil.DetailColors;
import forge.card.CardRenderer.CardStackPosition;
import forge.card.mana.ManaCost;
import forge.screens.FScreen;
import forge.view.CardView;
import forge.view.CardView.CardStateView;

public class CardImageRenderer {
    private static final float BASE_IMAGE_WIDTH = 360;
    private static final float BASE_IMAGE_HEIGHT = 504;
    private static float MANA_SYMBOL_SIZE, PT_BOX_WIDTH, HEADER_PADDING, BORDER_THICKNESS;
    private static FSkinFont NAME_FONT, TYPE_FONT, TEXT_FONT, PT_FONT;
    private static float prevImageWidth, prevImageHeight;

    public static void forceStaticFieldUpdate() {
        //force static fields to be updated the next time a card image is rendered
        prevImageWidth = 0;
        prevImageHeight = 0;
        forgeArt.clear();
    }

    private static void updateStaticFields(float w, float h) {
        if (w == prevImageWidth && h == prevImageHeight) {
            //for performance sake, only update static fields if card image size is different than previous rendered card
            return;
        }

        float ratio = Math.min(w / BASE_IMAGE_WIDTH, h / BASE_IMAGE_HEIGHT);

        MANA_SYMBOL_SIZE = 20 * ratio;
        PT_BOX_WIDTH = 56 * ratio;
        HEADER_PADDING = 5 * ratio;
        NAME_FONT = FSkinFont.forHeight(MANA_SYMBOL_SIZE);
        TYPE_FONT = FSkinFont.forHeight(MANA_SYMBOL_SIZE * 0.9f);
        TEXT_FONT = FSkinFont.forHeight(MANA_SYMBOL_SIZE * 0.95f);
        PT_FONT = NAME_FONT;
        BORDER_THICKNESS = Math.max(1.5f * ratio, 1f); //don't let border go below 1

        prevImageWidth = w;
        prevImageHeight = h;
    }

    public static void drawCardImage(Graphics g, CardView card, float x, float y, float w, float h, CardStackPosition pos) {
        updateStaticFields(w, h);
        final CardStateView state = card.getOriginal();

        float blackBorderThickness = w * CardRenderer.BLACK_BORDER_THICKNESS_RATIO;
        g.fillRect(Color.BLACK, x, y, w, h);
        x += blackBorderThickness;
        y += blackBorderThickness;
        w -= 2 * blackBorderThickness;
        h -= 2 * blackBorderThickness;

        //determine colors for borders
        final List<DetailColors> borderColors = CardDetailUtil.getBorderColors(card.getOriginal());
        DetailColors borderColor = borderColors.get(0);
        Color color1 = FSkinColor.fromRGB(borderColor.r, borderColor.g, borderColor.b);
        Color color2 = null;
        if (borderColors.size() > 1) {
            borderColor = borderColors.get(1);
            color2 = FSkinColor.fromRGB(borderColor.r, borderColor.g, borderColor.b);
        }
        if (color2 == null) {
            g.fillRect(color1, x, y, w, h);
        }
        else {
            g.fillGradientRect(color1, color2, false, x, y, w, h);
        }

        float artInset = blackBorderThickness * 0.5f;
        float outerBorderThickness = 2 * blackBorderThickness - artInset;
        x += outerBorderThickness;
        y += outerBorderThickness;
        w -= 2 * outerBorderThickness;
        float headerHeight = Math.max(MANA_SYMBOL_SIZE + 2 * HEADER_PADDING, 2 * NAME_FONT.getCapHeight()) + 2;

        //draw header containing name and mana cost
        Color headerColor1 = FSkinColor.tintColor(Color.WHITE, color1, CardRenderer.NAME_BOX_TINT);
        Color headerColor2 = color2 == null ? null : FSkinColor.tintColor(Color.WHITE, color2, CardRenderer.NAME_BOX_TINT);
        drawHeader(g, card, headerColor1, headerColor2, x, y, w, headerHeight);

        if (pos == CardStackPosition.BehindVert) { return; } //remaining rendering not needed if card is behind another card in a vertical stack
        boolean onTop = (pos == CardStackPosition.Top);

        y += headerHeight;

        float artWidth = w - 2 * artInset;
        float artHeight = artWidth / CardRenderer.CARD_ART_RATIO;
        float typeBoxHeight = 2 * TYPE_FONT.getCapHeight();
        float ptBoxHeight = 0;
        float textBoxHeight = h - headerHeight - artHeight - typeBoxHeight - outerBorderThickness - artInset;
        if (state.isCreature() || state.isPlaneswalker()) {
            //if P/T box needed, make room for it
            ptBoxHeight = 2 * PT_FONT.getCapHeight();
            textBoxHeight -= ptBoxHeight;
        }
        else {
            textBoxHeight -= 2 * artInset;
        }
        float minTextBoxHeight = 2 * headerHeight;
        if (textBoxHeight < minTextBoxHeight) {
            if (textBoxHeight < minTextBoxHeight) {
                artHeight -= (minTextBoxHeight - textBoxHeight); //subtract from art height if text box not big enough otherwise
                textBoxHeight = minTextBoxHeight;
                if (artHeight < 0) {
                    textBoxHeight += artHeight;
                    artHeight = 0;
                }
            }
        }

        //draw art box with Forge icon
        if (artHeight > 0) {
            drawArt(g, x + artInset, y, artWidth, artHeight);
            y += artHeight;
        }

        //draw type line
        drawTypeLine(g, card, headerColor1, headerColor2, x, y, w, typeBoxHeight);
        y += typeBoxHeight;

        //draw text box
        Color textBoxColor1 = FSkinColor.tintColor(Color.WHITE, color1, CardRenderer.TEXT_BOX_TINT);
        Color textBoxColor2 = color2 == null ? null : FSkinColor.tintColor(Color.WHITE, color2, CardRenderer.TEXT_BOX_TINT);
        drawTextBox(g, card, textBoxColor1, textBoxColor2, x + artInset, y, w - 2 * artInset, textBoxHeight, onTop);
        y += textBoxHeight;

        //draw P/T box
        if (onTop && ptBoxHeight > 0) {
            //only needed if on top since otherwise P/T will be hidden
            Color ptColor1 = FSkinColor.tintColor(Color.WHITE, color1, CardRenderer.PT_BOX_TINT);
            Color ptColor2 = color2 == null ? null : FSkinColor.tintColor(Color.WHITE, color2, CardRenderer.PT_BOX_TINT);
            drawPtBox(g, card, ptColor1, ptColor2, x, y - 2 * artInset, w, ptBoxHeight);
        }
    }

    private static void drawHeader(Graphics g, CardView card, Color color1, Color color2, float x, float y, float w, float h) {
        if (color2 == null) {
            g.fillRect(color1, x, y, w, h);
        }
        else {
            g.fillGradientRect(color1, color2, false, x, y, w, h);
        }
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);

        float padding = h / 8;

        final CardStateView state = card.getOriginal();
        //draw mana cost for card
        float manaCostWidth = 0;
        ManaCost mainManaCost = state.getManaCost();
        if (card.isSplitCard() && card.hasAltState()) {
            //handle rendering both parts of split card
            mainManaCost = state.getManaCost();
            ManaCost otherManaCost = card.getAlternate().getManaCost();
            manaCostWidth = CardFaceSymbols.getWidth(otherManaCost, MANA_SYMBOL_SIZE) + HEADER_PADDING;
            CardFaceSymbols.drawManaCost(g, otherManaCost, x + w - manaCostWidth, y + (h - MANA_SYMBOL_SIZE) / 2, MANA_SYMBOL_SIZE);
            //draw "//" between two parts of mana cost
            manaCostWidth += NAME_FONT.getBounds("//").width + HEADER_PADDING;
            g.drawText("//", NAME_FONT, Color.BLACK, x + w - manaCostWidth, y, w, h, false, HAlignment.LEFT, true);
        }
        manaCostWidth += CardFaceSymbols.getWidth(mainManaCost, MANA_SYMBOL_SIZE) + HEADER_PADDING;
        CardFaceSymbols.drawManaCost(g, mainManaCost, x + w - manaCostWidth, y + (h - MANA_SYMBOL_SIZE) / 2, MANA_SYMBOL_SIZE);

        //draw name for card
        x += padding;
        w -= 2 * padding;
        g.drawText(state.getName(), NAME_FONT, Color.BLACK, x, y, w - manaCostWidth - padding, h, false, HAlignment.LEFT, true);
    }

    public static final FBufferedImage forgeArt;
    static {
        final float logoWidth = FSkinImage.LOGO.getWidth();
        final float logoHeight = FSkinImage.LOGO.getHeight();
        float h = logoHeight * 1.1f;
        float w = h * CardRenderer.CARD_ART_RATIO;
        forgeArt = new FBufferedImage(w, h) {
            @Override
            protected void draw(Graphics g, float w, float h) {
                g.drawImage(FSkinTexture.BG_TEXTURE, 0, 0, w, h);
                g.fillRect(FScreen.TEXTURE_OVERLAY_COLOR, 0, 0, w, h);
                g.drawImage(FSkinImage.LOGO, (w - logoWidth) / 2, (h - logoHeight) / 2, logoWidth, logoHeight);
            }
        };
    }

    private static void drawArt(Graphics g, float x, float y, float w, float h) {
        g.drawImage(forgeArt, x, y, w, h);
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);
    }

    private static void drawTypeLine(Graphics g, CardView card, Color color1, Color color2, float x, float y, float w, float h) {
        if (color2 == null) {
            g.fillRect(color1, x, y, w, h);
        }
        else {
            g.fillGradientRect(color1, color2, false, x, y, w, h);
        }
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);

        float padding = h / 8;

        //draw square icon for rarity
        float iconSize = h * 0.55f;
        float iconPadding = (h - iconSize) / 2;
        w -= iconSize + iconPadding * 2;
        g.fillRect(CardRenderer.getRarityColor(card.getRarity()), x + w + iconPadding, y + (h - iconSize) / 2, iconSize, iconSize);

        //draw type
        x += padding;
        g.drawText(CardDetailUtil.formatCardType(card.getOriginal()), TYPE_FONT, Color.BLACK, x, y, w, h, false, HAlignment.LEFT, true);
    }

    //use text g to handle mana symbols and reminder text
    private static final TextRenderer cardTextRenderer = new TextRenderer(true);

    private static void drawTextBox(Graphics g, CardView card, Color color1, Color color2, float x, float y, float w, float h, boolean onTop) {
        if (color2 == null) {
            g.fillRect(color1, x, y, w, h);
        }
        else {
            g.fillGradientRect(color1, color2, false, x, y, w, h);
        }
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);

        if (!onTop) { return; } //remaining rendering only needed if card on top

        final CardStateView state = card.getOriginal();
        if (state.isBasicLand()) {
            //draw icons for basic lands
            FSkinImage image;
            switch (state.getName()) {
            case "Plains":
                image = FSkinImage.MANA_W;
                break;
            case "Island":
                image = FSkinImage.MANA_U;
                break;
            case "Swamp":
                image = FSkinImage.MANA_B;
                break;
            case "Mountain":
                image = FSkinImage.MANA_R;
                break;
            default:
                image = FSkinImage.MANA_G;
                break;
            }
            float iconSize = h * 0.75f;
            g.drawImage(image, x + (w - iconSize) / 2, y + (h - iconSize) / 2, iconSize, iconSize);
        }
        else {
            final String text = card.getOriginal().getText();
            if (StringUtils.isEmpty(text)) { return; }

            float padding = TEXT_FONT.getCapHeight() * 0.75f;
            x += padding;
            y += padding;
            w -= 2 * padding;
            h -= 2 * padding;
            cardTextRenderer.drawText(g, text, TEXT_FONT, Color.BLACK, x, y, w, h, y, h, true, HAlignment.LEFT, true);
        }
    }

    private static void drawPtBox(Graphics g, CardView card, Color color1, Color color2, float x, float y, float w, float h) {
        final CardStateView state = card.getOriginal();
        List<String> pieces = new ArrayList<String>();
        if (state.isCreature()) {
            pieces.add(String.valueOf(state.getPower()));
            pieces.add("/");
            pieces.add(String.valueOf(state.getToughness()));
        }
        else if (state.isPlaneswalker()) {
            pieces.add(String.valueOf(state.getLoyalty()));
        }
        else { return; }

        float padding = Math.round(PT_FONT.getCapHeight() / 4);
        float totalPieceWidth = -padding;
        List<Float> pieceWidths = new ArrayList<Float>();
        for (String piece : pieces) {
            float pieceWidth = PT_FONT.getBounds(piece).width + padding;
            pieceWidths.add(pieceWidth);
            totalPieceWidth += pieceWidth;
        }
        float boxHeight = PT_FONT.getCapHeight() + PT_FONT.getAscent() + 3 * padding;

        float boxWidth = Math.max(PT_BOX_WIDTH, totalPieceWidth + 2 * padding);
        x += w - boxWidth;
        y += h - boxHeight;
        w = boxWidth;
        h = boxHeight;

        if (color2 == null) {
            g.fillRect(color1, x, y, w, h);
        }
        else {
            g.fillGradientRect(color1, color2, false, x, y, w, h);
        }
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);

        x += (boxWidth - totalPieceWidth) / 2;
        for (int i = 0; i < pieces.size(); i++) {
            g.drawText(pieces.get(i), PT_FONT, Color.BLACK, x, y, w, h, false, HAlignment.LEFT, true);
            x += pieceWidths.get(i);
        }
    }
}
