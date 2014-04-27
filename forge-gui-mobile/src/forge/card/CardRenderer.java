package forge.card;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.ImageCache;
import forge.assets.TextRenderer;
import forge.card.CardDetailUtil.DetailColors;
import forge.card.mana.ManaCost;
import forge.game.card.Card;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.screens.match.FControl;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDialog;
import forge.toolbox.FList;

public class CardRenderer {
    private static final FSkinFont NAME_FONT = FSkinFont.get(16);
    private static final FSkinFont TYPE_FONT = FSkinFont.get(14);
    private static final FSkinFont SET_FONT = TYPE_FONT;
    private static final FSkinFont TEXT_FONT = TYPE_FONT;
    private static final FSkinFont ID_FONT = TEXT_FONT;
    private static final FSkinFont PT_FONT = NAME_FONT;
    private static final float NAME_BOX_TINT = 0.2f;
    private static final float TEXT_BOX_TINT = 0.1f;
    private static final float PT_BOX_TINT = 0.2f;
    private static final float MANA_COST_PADDING = 3;
    private static final float SET_BOX_MARGIN = 1;
    private static final float MANA_SYMBOL_SIZE = FSkinImage.MANA_1.getNearestHQWidth(2 * (NAME_FONT.getFont().getCapHeight() - MANA_COST_PADDING));

    private static Color fromDetailColor(DetailColors detailColor) {
        return FSkinColor.fromRGB(detailColor.r, detailColor.g, detailColor.b);
    }

    public static void drawZoom(Graphics g, Card card, float width, float height) {
        float x = FDialog.INSETS;
        float y = x;
        float w = width - 2 * x;
        float h = height - 2 * y;

        Texture image = ImageCache.getImage(card);

        float ratio = h / w;
        float imageRatio = (float)image.getHeight() / (float)image.getWidth(); //use image ratio rather than normal aspect ratio so it looks better

        if (ratio > imageRatio) {
            float oldHeight = h;
            h = w * imageRatio;
            y += (oldHeight - h) / 2;
        }
        else {
            float oldWidth = w;
            w = h / imageRatio;
            x += (oldWidth - w) / 2;
        }

        //prevent scaling image larger if preference turned off
        if (w > image.getWidth() || h > image.getHeight()) {
            if (!FModel.getPreferences().getPrefBoolean(FPref.UI_SCALE_LARGER)) {
                float oldWidth = w;
                float oldHeight = h;
                w = image.getWidth();
                h = image.getHeight();
                x += (oldWidth - w) / 2;
                y += (oldHeight - h) / 2;
            }
        }

        g.drawImage(image, x, y, w, h);
    }

    public static void drawDetails(Graphics g, Card card, float width, float height) {
        float x = FDialog.INSETS;
        float y = x;
        float w = width - 2 * x;
        float h = height - 2 * y;

        float ratio = h / w;
        if (ratio > FCardPanel.ASPECT_RATIO) {
            float oldHeight = h;
            h = w * FCardPanel.ASPECT_RATIO;
            y += (oldHeight - h) / 2;
        }
        else {
            float oldWidth = w;
            w = h / FCardPanel.ASPECT_RATIO;
            x += (oldWidth - w) / 2;
        }

        boolean canShow = !card.isFaceDown() && FControl.mayShowCard(card);

        float blackBorderThickness = w * 0.021f;
        g.fillRect(Color.BLACK, x, y, w, h);
        x += blackBorderThickness;
        y += blackBorderThickness;
        w -= 2 * blackBorderThickness;
        h -= 2 * blackBorderThickness;

        //determine colors for borders
        List<DetailColors> borderColors = CardDetailUtil.getBorderColors(card, canShow, true);
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

        Color idForeColor = FSkinColor.getHighContrastColor(color1);

        float outerBorderThickness = 2 * blackBorderThickness;
        x += outerBorderThickness;
        y += outerBorderThickness;
        w -= 2 * outerBorderThickness;
        h =  Math.max(MANA_SYMBOL_SIZE + 2 * MANA_COST_PADDING, 2 * NAME_FONT.getFont().getCapHeight()) + 2 * TYPE_FONT.getFont().getCapHeight() + 2;

        //draw name/type box
        Color nameBoxColor1 = FSkinColor.tintColor(Color.WHITE, color1, NAME_BOX_TINT);
        Color nameBoxColor2 = color2 == null ? null : FSkinColor.tintColor(Color.WHITE, color2, NAME_BOX_TINT);
        drawCardNameBox(g, card, nameBoxColor1, nameBoxColor2, x, y, w, h);

        float ptBoxHeight = 2 * PT_FONT.getFont().getCapHeight();

        float innerBorderThickness = outerBorderThickness / 2;
        y += h + innerBorderThickness;
        h = height - FDialog.INSETS - blackBorderThickness - ptBoxHeight - 2 * innerBorderThickness - y; 

        Color textBoxColor1 = FSkinColor.tintColor(Color.WHITE, color1, TEXT_BOX_TINT);
        Color textBoxColor2 = color2 == null ? null : FSkinColor.tintColor(Color.WHITE, color2, TEXT_BOX_TINT);
        drawCardTextBox(g, card, canShow, textBoxColor1, textBoxColor2, x, y, w, h);

        y += h + innerBorderThickness;
        h = ptBoxHeight;

        Color ptColor1 = FSkinColor.tintColor(Color.WHITE, color1, PT_BOX_TINT);
        Color ptColor2 = color2 == null ? null : FSkinColor.tintColor(Color.WHITE, color2, PT_BOX_TINT);
        drawCardIdAndPtBox(g, card, idForeColor, ptColor1, ptColor2, x, y, w, h);
    }

    public static float getCardListItemHeight() {
        return 2 * MANA_SYMBOL_SIZE + FSkinFont.get(12).getFont().getLineHeight() + 4 * FList.PADDING;
    }

    private static Map<PaperCard, TextureRegion> cardArtCache = new HashMap<PaperCard, TextureRegion>();

    //extract card art from the given card
    public static TextureRegion getCardArt(PaperCard card) {
        TextureRegion cardArt = cardArtCache.get(card);
        if (cardArt == null) {
            Texture image = ImageCache.getImage(card);
            int w = image.getWidth();
            int h = image.getHeight();
            int x = Math.round(w * 0.065f);
            int y = Math.round(h * 0.105f);
            w -= 2 * x;
            h *= 0.45f;
            cardArt = new TextureRegion(image, x, y, w, h);
            cardArtCache.put(card, cardArt);
        }
        return cardArt;
    }

    public static void drawCardListItem(Graphics g, FSkinFont font, FSkinColor foreColor, PaperCard card, int count, float x, float y, float w, float h) {
        TextureRegion cardArt = getCardArt(card);
        float cardArtHeight = h - MANA_SYMBOL_SIZE - FList.PADDING;
        float cardArtWidth = cardArtHeight * (float)cardArt.getRegionWidth() / (float)cardArt.getRegionHeight();
        g.drawImage(cardArt, x - FList.PADDING, y - FList.PADDING, cardArtWidth, cardArtHeight);
        x += cardArtWidth;

        ManaCost manaCost = card.getRules().getManaCost();
        float availableNameWidth = w - CardFaceSymbols.getWidth(manaCost, MANA_SYMBOL_SIZE) - cardArtWidth - FList.PADDING;
        g.drawText(card.getName(), font, foreColor, x, y, availableNameWidth, MANA_SYMBOL_SIZE, false, HAlignment.LEFT, true);
        x += availableNameWidth + FList.PADDING;
        CardFaceSymbols.drawManaCost(g, manaCost, x, y, MANA_SYMBOL_SIZE);
    }

    private static void drawCardNameBox(Graphics g, Card card, Color color1, Color color2, float x, float y, float w, float h) {
        if (color2 == null) {
            g.fillRect(color1, x, y, w, h);
        }
        else {
            g.fillGradientRect(color1, color2, false, x, y, w, h);
        }
        g.drawRect(1, Color.BLACK, x, y, w, h);

        float padding = h / 8;

        //make sure name/mana cost row height is tall enough for both
        h = Math.max(MANA_SYMBOL_SIZE + 2 * MANA_COST_PADDING, 2 * NAME_FONT.getFont().getCapHeight());

        float manaCostWidth = CardFaceSymbols.getWidth(card.getManaCost(), MANA_SYMBOL_SIZE) + MANA_COST_PADDING;
        CardFaceSymbols.drawManaCost(g, card.getManaCost(), x + w - manaCostWidth, y + (h - MANA_SYMBOL_SIZE) / 2, MANA_SYMBOL_SIZE);

        x += padding;
        w -= 2 * padding;
        g.drawText(card.isFaceDown() ? "???" : card.getName(), NAME_FONT, Color.BLACK, x, y, w - manaCostWidth - padding, h, false, HAlignment.LEFT, true);

        y += h;
        h = 2 * TYPE_FONT.getFont().getCapHeight();

        String set = card.getCurSetCode();
        if (!StringUtils.isEmpty(set)) {
            float setWidth = getSetWidth(SET_FONT, set);
            drawSetLabel(g, SET_FONT, set, card.getRarity(), x + w + padding - setWidth - SET_BOX_MARGIN, y + SET_BOX_MARGIN, setWidth, h - SET_BOX_MARGIN);
            w -= setWidth; //reduce available width for type
        }

        g.drawText(CardDetailUtil.formatCardType(card), TYPE_FONT, Color.BLACK, x, y, w, h, false, HAlignment.LEFT, true);
    }

    public static float getSetWidth(FSkinFont font, String set) {
        return font.getFont().getBounds(set).width + font.getFont().getCapHeight();
    }

    public static void drawSetLabel(Graphics g, FSkinFont font, String set, CardRarity rarity, float x, float y, float w, float h) {
        Color backColor;
        switch(rarity) {
        case Uncommon:
            backColor = fromDetailColor(DetailColors.UNCOMMON);
            break;
        case Rare:
            backColor = fromDetailColor(DetailColors.RARE);
            break;
        case MythicRare:
            backColor = fromDetailColor(DetailColors.MYTHIC);
            break;
        case Special: //"Timeshifted" or other Special Rarity Cards
            backColor = fromDetailColor(DetailColors.SPECIAL);
            break;
        default: //case BasicLand: + case Common:
            backColor = fromDetailColor(DetailColors.COMMON);
            break;
        }

        Color foreColor = FSkinColor.getHighContrastColor(backColor);
        g.fillRect(backColor, x, y, w, h);
        g.drawText(set, font, foreColor, x, y, w, h, false, HAlignment.CENTER, true);
    }

    //use text renderer to handle mana symbols and reminder text
    private static final TextRenderer cardTextRenderer = new TextRenderer(true);

    private static void drawCardTextBox(Graphics g, Card card, boolean canShow, Color color1, Color color2, float x, float y, float w, float h) {
        if (color2 == null) {
            g.fillRect(color1, x, y, w, h);
        }
        else {
            g.fillGradientRect(color1, color2, false, x, y, w, h);
        }
        g.drawRect(1, Color.BLACK, x, y, w, h);

        float padX = TEXT_FONT.getFont().getCapHeight() / 2;
        float padY = padX + 2; //add a little more vertical padding
        x += padX;
        y += padY;
        w -= 2 * padX;
        h -= 2 * padY;
        cardTextRenderer.drawText(g, CardDetailUtil.composeCardText(card, canShow), TEXT_FONT, Color.BLACK, x, y, w, h, true, HAlignment.LEFT, false);
    }

    private static void drawCardIdAndPtBox(Graphics g, Card card, Color idForeColor, Color color1, Color color2, float x, float y, float w, float h) {
        String idText = CardDetailUtil.formatCardId(card);
        g.drawText(idText, ID_FONT, idForeColor, x, y + ID_FONT.getFont().getCapHeight() / 2, w, h, false, HAlignment.LEFT, false);

        String ptText = CardDetailUtil.formatPowerToughness(card);
        if (StringUtils.isEmpty(ptText)) { return; }

        float padding = PT_FONT.getFont().getCapHeight() / 2;
        float boxWidth = Math.min(PT_FONT.getFont().getBounds(ptText).width + 2 * padding,
                w - ID_FONT.getFont().getBounds(idText).width - padding); //prevent box overlapping ID
        x += w - boxWidth;
        w = boxWidth;

        if (color2 == null) {
            g.fillRect(color1, x, y, w, h);
        }
        else {
            g.fillGradientRect(color1, color2, false, x, y, w, h);
        }
        g.drawRect(1, Color.BLACK, x, y, w, h);
        g.drawText(ptText, PT_FONT, Color.BLACK, x, y, w, h, false, HAlignment.CENTER, true);
    }
}
