package forge.card;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

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
    private static final float IMAGE_WIDTH = 360;
    private static final float IMAGE_HEIGHT = 504;
    private static final float MANA_SYMBOL_SIZE = 20;
    private static final float PT_BOX_WIDTH = 56;
    private static final float HEADER_PADDING = 4;
    private static final FSkinFont NAME_FONT = FSkinFont.forHeight(MANA_SYMBOL_SIZE);
    private static final FSkinFont TYPE_FONT = FSkinFont.forHeight(MANA_SYMBOL_SIZE * 0.9f);
    private static final FSkinFont TEXT_FONT = FSkinFont.forHeight(MANA_SYMBOL_SIZE * 0.95f);
    private static final FSkinFont PT_FONT = NAME_FONT;

    public static Texture createCardImage(Card card) {
        float w = IMAGE_WIDTH;
        float h = IMAGE_HEIGHT;

        TextureRenderer renderer = new TextureRenderer(w, h);

        float x = 0;
        float y = 0;
        float blackBorderThickness = w * CardRenderer.BLACK_BORDER_THICKNESS_RATIO;
        renderer.fillRect(Color.BLACK, x, y, w, h);
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
            renderer.fillRect(color1, x, y, w, h);
        }
        else {
            renderer.fillGradientRect(color1, color2, false, x, y, w, h);
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
        drawHeader(renderer, card, headerColor1, headerColor2, x, y, w, headerHeight);
        y += headerHeight;

        //draw art box with Forge icon
        float artWidth = w - 2 * artInset;
        float artHeight = artWidth / CardRenderer.CARD_ART_RATIO;
        drawArt(renderer, x + artInset, y, artWidth, artHeight);
        y += artHeight;

        float typeBoxHeight = 2 * TYPE_FONT.getCapHeight();
        drawTypeLine(renderer, card, headerColor1, headerColor2, x, y, w, typeBoxHeight);
        y += typeBoxHeight;

        float ptBoxHeight = 2 * PT_FONT.getCapHeight();
        float textBoxHeight = h - headerHeight - artHeight - typeBoxHeight - ptBoxHeight - outerBorderThickness; 

        Color textBoxColor1 = FSkinColor.tintColor(Color.WHITE, color1, CardRenderer.TEXT_BOX_TINT);
        Color textBoxColor2 = color2 == null ? null : FSkinColor.tintColor(Color.WHITE, color2, CardRenderer.TEXT_BOX_TINT);
        drawTextBox(renderer, card, textBoxColor1, textBoxColor2, x + artInset, y, w - 2 * artInset, textBoxHeight);

        y += textBoxHeight - 2 * artInset;
        Color ptColor1 = FSkinColor.tintColor(Color.WHITE, color1, CardRenderer.PT_BOX_TINT);
        Color ptColor2 = color2 == null ? null : FSkinColor.tintColor(Color.WHITE, color2, CardRenderer.PT_BOX_TINT);
        drawPtBox(renderer, card, ptColor1, ptColor2, x, y, w, ptBoxHeight);

        return renderer.finish();
    }

    private static void drawHeader(TextureRenderer renderer, Card card, Color color1, Color color2, float x, float y, float w, float h) {
        if (color2 == null) {
            renderer.fillRect(color1, x, y, w, h);
        }
        else {
            renderer.fillGradientRect(color1, color2, false, x, y, w, h);
        }
        renderer.drawRect(1, Color.BLACK, x, y, w, h);

        float padding = h / 8;

        //draw mana cost for card
        float manaCostWidth = 0;
        ManaCost mainManaCost = card.getManaCost();
        if (card.isSplitCard() && card.getCurState() == CardCharacteristicName.Original) {
            //handle rendering both parts of split card
            mainManaCost = card.getRules().getMainPart().getManaCost();
            ManaCost otherManaCost = card.getRules().getOtherPart().getManaCost();
            manaCostWidth = CardFaceSymbols.getWidth(otherManaCost, MANA_SYMBOL_SIZE) + HEADER_PADDING;
            CardFaceSymbols.drawManaCost(renderer, otherManaCost, x + w - manaCostWidth, y + (h - MANA_SYMBOL_SIZE) / 2, MANA_SYMBOL_SIZE);
            //draw "//" between two parts of mana cost
            manaCostWidth += NAME_FONT.getBounds("//").width + HEADER_PADDING;
            renderer.drawText("//", NAME_FONT, Color.BLACK, x + w - manaCostWidth, y, w, h, false, HAlignment.LEFT, true);
        }
        manaCostWidth += CardFaceSymbols.getWidth(mainManaCost, MANA_SYMBOL_SIZE) + HEADER_PADDING;
        CardFaceSymbols.drawManaCost(renderer, mainManaCost, x + w - manaCostWidth, y + (h - MANA_SYMBOL_SIZE) / 2, MANA_SYMBOL_SIZE);

        //draw name for card
        x += padding;
        w -= 2 * padding;
        renderer.drawText(card.getName(), NAME_FONT, Color.BLACK, x, y, w - manaCostWidth - padding, h, false, HAlignment.LEFT, true);
    }

    private static void drawArt(TextureRenderer renderer, float x, float y, float w, float h) {
        float imageHeight = h * 0.9f;
        float imageWidth = imageHeight * FSkinImage.LOGO.getWidth() / FSkinImage.LOGO.getHeight();
        renderer.drawImage(FSkinTexture.BG_TEXTURE, x, y, w, h);
        renderer.fillRect(FScreen.TEXTURE_OVERLAY_COLOR, x, y, w, h);
        renderer.drawImage(FSkinImage.LOGO, x + (w - imageWidth) / 2, y + (h - imageHeight) / 2, imageWidth, imageHeight);
        renderer.drawRect(1, Color.BLACK, x, y, w, h);
    }

    private static void drawTypeLine(TextureRenderer renderer, Card card, Color color1, Color color2, float x, float y, float w, float h) {
        if (color2 == null) {
            renderer.fillRect(color1, x, y, w, h);
        }
        else {
            renderer.fillGradientRect(color1, color2, false, x, y, w, h);
        }
        renderer.drawRect(1, Color.BLACK, x, y, w, h);

        float padding = h / 8;

        //draw square icon for rarity
        float iconSize = h * 0.55f;
        float iconPadding = (h - iconSize) / 2;
        w -= iconSize + iconPadding * 2;
        renderer.fillRect(CardRenderer.getRarityColor(card.getRarity()), x + w + iconPadding, y + (h - iconSize) / 2, iconSize, iconSize);

        //draw type
        x += padding;
        renderer.drawText(CardDetailUtil.formatCardType(card), TYPE_FONT, Color.BLACK, x, y, w, h, false, HAlignment.LEFT, true);
    }

    //use text renderer to handle mana symbols and reminder text
    private static final TextRenderer cardTextRenderer = new TextRenderer(true);

    private static void drawTextBox(TextureRenderer renderer, Card card, Color color1, Color color2, float x, float y, float w, float h) {
        if (color2 == null) {
            renderer.fillRect(color1, x, y, w, h);
        }
        else {
            renderer.fillGradientRect(color1, color2, false, x, y, w, h);
        }
        renderer.drawRect(1, Color.BLACK, x, y, w, h);

        float padX = TEXT_FONT.getCapHeight() * 0.75f;
        float padY = padX + 2; //add a little more vertical padding
        x += padX;
        y += padY;
        w -= 2 * padX;
        h -= 2 * padY;
        String text = card.getRules().getOracleText();
        text = text.replace("\\n", "\n"); //replace new line placeholders with actual new line characters
        cardTextRenderer.drawText(renderer, text, TEXT_FONT, Color.BLACK, x, y, w, h, y, h, true, HAlignment.LEFT, true);
    }

    private static void drawPtBox(TextureRenderer renderer, Card card, Color color1, Color color2, float x, float y, float w, float h) {
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

        x += w - PT_BOX_WIDTH;
        y += h - boxHeight;
        w = PT_BOX_WIDTH;
        h = boxHeight;

        if (color2 == null) {
            renderer.fillRect(color1, x, y, w, h);
        }
        else {
            renderer.fillGradientRect(color1, color2, false, x, y, w, h);
        }
        renderer.drawRect(1, Color.BLACK, x, y, w, h);

        x += (PT_BOX_WIDTH - totalPieceWidth) / 2;
        for (int i = 0; i < pieces.size(); i++) {
            renderer.drawText(pieces.get(i), PT_FONT, Color.BLACK, x, y, w, h, false, HAlignment.LEFT, true);
            x += pieceWidths.get(i);
        }
    }
}
