package forge.card;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Graphics;
import forge.TextureRenderer;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FSkinTexture;
import forge.assets.TextRenderer;
import forge.card.CardDetailUtil.DetailColors;
import forge.card.mana.ManaCost;
import forge.game.card.Card;
import forge.screens.FScreen;

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

    public static Texture createCardImage(Card card) {
        TextureRenderer g = new TextureRenderer(BASE_IMAGE_WIDTH, BASE_IMAGE_HEIGHT);
        drawCardImage(g, card, 0, 0, BASE_IMAGE_WIDTH, BASE_IMAGE_HEIGHT);
        return g.finish();
    }

    public static void drawCardImage(Graphics g, Card card, float x, float y, float w, float h) {
        updateStaticFields(w, h);

        float blackBorderThickness = w * CardRenderer.BLACK_BORDER_THICKNESS_RATIO;
        g.fillRect(Color.BLACK, x, y, w, h);
        x += blackBorderThickness;
        y += blackBorderThickness;
        w -= 2 * blackBorderThickness;
        h -= 2 * blackBorderThickness;

        //determine colors for borders
        List<DetailColors> borderColors = CardDetailUtil.getBorderColors(card, true, true);
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
        y += headerHeight;

        float artWidth = w - 2 * artInset;
        float artHeight = artWidth / CardRenderer.CARD_ART_RATIO;
        float typeBoxHeight = 2 * TYPE_FONT.getCapHeight();
        float ptBoxHeight = 0;
        float textBoxHeight = h - headerHeight - artHeight - typeBoxHeight - outerBorderThickness - artInset;
        if (card.isCreature() || card.isPlaneswalker()) {
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
        drawTextBox(g, card, textBoxColor1, textBoxColor2, x + artInset, y, w - 2 * artInset, textBoxHeight);
        y += textBoxHeight;

        //draw P/T box
        if (ptBoxHeight > 0) {
            Color ptColor1 = FSkinColor.tintColor(Color.WHITE, color1, CardRenderer.PT_BOX_TINT);
            Color ptColor2 = color2 == null ? null : FSkinColor.tintColor(Color.WHITE, color2, CardRenderer.PT_BOX_TINT);
            drawPtBox(g, card, ptColor1, ptColor2, x, y - 2 * artInset, w, ptBoxHeight);
        }
    }

    private static void drawHeader(Graphics g, Card card, Color color1, Color color2, float x, float y, float w, float h) {
        if (color2 == null) {
            g.fillRect(color1, x, y, w, h);
        }
        else {
            g.fillGradientRect(color1, color2, false, x, y, w, h);
        }
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);

        float padding = h / 8;

        //draw mana cost for card
        float manaCostWidth = 0;
        ManaCost mainManaCost = card.getManaCost();
        if (card.isSplitCard() && card.getCurState() == CardCharacteristicName.Original) {
            //handle rendering both parts of split card
            mainManaCost = card.getRules().getMainPart().getManaCost();
            ManaCost otherManaCost = card.getRules().getOtherPart().getManaCost();
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
        g.drawText(card.getName(), NAME_FONT, Color.BLACK, x, y, w - manaCostWidth - padding, h, false, HAlignment.LEFT, true);
    }

    private static void drawArt(Graphics g, float x, float y, float w, float h) {
        float imageHeight = h * 0.9f;
        float imageWidth = imageHeight * FSkinImage.LOGO.getWidth() / FSkinImage.LOGO.getHeight();
        g.drawImage(FSkinTexture.BG_TEXTURE, x, y, w, h);
        g.fillRect(FScreen.TEXTURE_OVERLAY_COLOR, x, y, w, h);
        g.drawImage(FSkinImage.LOGO, x + (w - imageWidth) / 2, y + (h - imageHeight) / 2, imageWidth, imageHeight);
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);
    }

    private static void drawTypeLine(Graphics g, Card card, Color color1, Color color2, float x, float y, float w, float h) {
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
        g.drawText(CardDetailUtil.formatCardType(card), TYPE_FONT, Color.BLACK, x, y, w, h, false, HAlignment.LEFT, true);
    }

    //use text g to handle mana symbols and reminder text
    private static final TextRenderer cardTextRenderer = new TextRenderer(true);

    private static void drawTextBox(Graphics g, Card card, Color color1, Color color2, float x, float y, float w, float h) {
        if (color2 == null) {
            g.fillRect(color1, x, y, w, h);
        }
        else {
            g.fillGradientRect(color1, color2, false, x, y, w, h);
        }
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);

        if (card.isBasicLand()) {
            //draw icons for basic lands
            FSkinImage image;
            switch (card.getName()) {
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
            float padding = TEXT_FONT.getCapHeight() * 0.75f;
            x += padding;
            y += padding;
            w -= 2 * padding;
            h -= 2 * padding;
            String text = card.getRules().getOracleText();
            text = text.replace("\\n", "\n"); //replace new line placeholders with actual new line characters
            cardTextRenderer.drawText(g, text, TEXT_FONT, Color.BLACK, x, y, w, h, y, h, true, HAlignment.LEFT, true);
        }
    }

    private static void drawPtBox(Graphics g, Card card, Color color1, Color color2, float x, float y, float w, float h) {
        List<String> pieces = new ArrayList<String>();
        if (card.isCreature()) {
            pieces.add(String.valueOf(card.getBaseAttack()));
            pieces.add("/");
            pieces.add(String.valueOf(card.getBaseDefense()));
        }
        else if (card.isPlaneswalker()) {
            pieces.add(String.valueOf(card.getBaseLoyalty()));
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
