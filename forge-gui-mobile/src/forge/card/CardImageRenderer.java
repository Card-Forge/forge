package forge.card;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.TextureRenderer;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.card.CardDetailUtil.DetailColors;
import forge.card.mana.ManaCost;
import forge.game.card.Card;

public class CardImageRenderer {
    private static final float IMAGE_WIDTH = 360;
    private static final float IMAGE_HEIGHT = 504;
    private static final float MANA_SYMBOL_SIZE = 20;
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
        float blackBorderThickness = w * 0.021f;
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

        Color idForeColor = FSkinColor.getHighContrastColor(color1);

        float outerBorderThickness = 2 * blackBorderThickness;
        x += outerBorderThickness;
        y += outerBorderThickness;
        w -= 2 * outerBorderThickness;
        float headerHeight = Math.max(MANA_SYMBOL_SIZE + 2 * HEADER_PADDING, 2 * NAME_FONT.getCapHeight()) + 2;
        float typeBoxHeight = 2 * TYPE_FONT.getCapHeight();

        //draw header containing name and mana cost
        Color nameBoxColor1 = FSkinColor.tintColor(Color.WHITE, color1, CardRenderer.NAME_BOX_TINT);
        Color nameBoxColor2 = color2 == null ? null : FSkinColor.tintColor(Color.WHITE, color2, CardRenderer.NAME_BOX_TINT);
        drawHeader(renderer, card, true, nameBoxColor1, nameBoxColor2, x, y, w, headerHeight);

        float innerBorderThickness = outerBorderThickness / 2;
        float ptBoxHeight = 2 * PT_FONT.getCapHeight();
        float textBoxHeight = h - headerHeight - ptBoxHeight - outerBorderThickness - 3 * innerBorderThickness; 

        y += headerHeight + innerBorderThickness;
        Color textBoxColor1 = FSkinColor.tintColor(Color.WHITE, color1, CardRenderer.TEXT_BOX_TINT);
        Color textBoxColor2 = color2 == null ? null : FSkinColor.tintColor(Color.WHITE, color2, CardRenderer.TEXT_BOX_TINT);
        drawTextBox(renderer, card, true, textBoxColor1, textBoxColor2, x, y, w, textBoxHeight);

        y += textBoxHeight + innerBorderThickness;
        Color ptColor1 = FSkinColor.tintColor(Color.WHITE, color1, CardRenderer.PT_BOX_TINT);
        Color ptColor2 = color2 == null ? null : FSkinColor.tintColor(Color.WHITE, color2, CardRenderer.PT_BOX_TINT);
        drawPtBox(renderer, card, idForeColor, ptColor1, ptColor2, x, y, w, ptBoxHeight);

        return renderer.finish();
    }

    private static void drawHeader(TextureRenderer renderer, Card card, boolean canShow, Color color1, Color color2, float x, float y, float w, float h) {
        if (color2 == null) {
            renderer.fillRect(color1, x, y, w, h);
        }
        else {
            renderer.fillGradientRect(color1, color2, false, x, y, w, h);
        }
        renderer.drawRect(1, Color.BLACK, x, y, w, h);

        float padding = h / 8;

        //make sure name/mana cost row height is tall enough for both
        h = Math.max(MANA_SYMBOL_SIZE + 2 * HEADER_PADDING, 2 * NAME_FONT.getCapHeight());

        //draw mana cost for card
        float manaCostWidth = 0;
        if (canShow) {
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
        }

        //draw name for card
        x += padding;
        w -= 2 * padding;
        renderer.drawText(!canShow || card.isFaceDown() ? "???" : card.getName(), NAME_FONT, Color.BLACK, x, y, w - manaCostWidth - padding, h, false, HAlignment.LEFT, true);
    }

    //use text renderer to handle mana symbols and reminder text
    private static final TextRenderer cardTextRenderer = new TextRenderer(true);

    private static void drawTextBox(TextureRenderer renderer, Card card, boolean canShow, Color color1, Color color2, float x, float y, float w, float h) {
        if (color2 == null) {
            renderer.fillRect(color1, x, y, w, h);
        }
        else {
            renderer.fillGradientRect(color1, color2, false, x, y, w, h);
        }
        renderer.drawRect(1, Color.BLACK, x, y, w, h);

        float padX = TEXT_FONT.getCapHeight() / 2;
        float padY = padX + 2; //add a little more vertical padding
        x += padX;
        y += padY;
        w -= 2 * padX;
        h -= 2 * padY;
        cardTextRenderer.drawText(renderer, CardDetailUtil.composeCardText(card, canShow), TEXT_FONT, Color.BLACK, x, y, w, h, y, h, true, HAlignment.LEFT, false);
    }

    private static void drawPtBox(TextureRenderer renderer, Card card, Color idForeColor, Color color1, Color color2, float x, float y, float w, float h) {
        String ptText = CardDetailUtil.formatPowerToughness(card);
        if (StringUtils.isEmpty(ptText)) { return; }

        float padding = PT_FONT.getCapHeight() / 2;
        float boxWidth = PT_FONT.getBounds(ptText).width + 2 * padding;
        x += w - boxWidth;
        w = boxWidth;

        if (color2 == null) {
            renderer.fillRect(color1, x, y, w, h);
        }
        else {
            renderer.fillGradientRect(color1, color2, false, x, y, w, h);
        }
        renderer.drawRect(1, Color.BLACK, x, y, w, h);
        renderer.drawText(ptText, PT_FONT, Color.BLACK, x, y, w, h, false, HAlignment.CENTER, true);
    }
}
