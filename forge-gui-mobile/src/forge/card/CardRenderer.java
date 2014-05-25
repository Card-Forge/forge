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
import forge.ImageKeys;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.ImageCache;
import forge.assets.TextRenderer;
import forge.card.CardDetailUtil.DetailColors;
import forge.card.mana.ManaCost;
import forge.game.card.Card;
import forge.item.PaperCard;
import forge.screens.match.FControl;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDialog;
import forge.toolbox.FList;
import forge.util.Utils;

public class CardRenderer {
    private static final FSkinFont NAME_FONT = FSkinFont.get(16);
    private static final FSkinFont TYPE_FONT = FSkinFont.get(14);
    private static final FSkinFont SET_FONT = TYPE_FONT;
    private static final FSkinFont TEXT_FONT = TYPE_FONT;
    private static final FSkinFont ID_FONT = TEXT_FONT;
    private static final FSkinFont PT_FONT = NAME_FONT;
    private static final float NAME_BOX_TINT = 0.2f;
    private static final float TEXT_BOX_TINT = 0.1f;
    public static final float PT_BOX_TINT = 0.2f;
    private static final float MANA_COST_PADDING = Utils.scaleMin(3);
    private static final float SET_BOX_MARGIN = Utils.scaleMin(1);
    private static final float MANA_SYMBOL_SIZE = FSkinImage.MANA_1.getNearestHQWidth(2 * (NAME_FONT.getFont().getCapHeight() - MANA_COST_PADDING));

    private static Color fromDetailColor(DetailColors detailColor) {
        return FSkinColor.fromRGB(detailColor.r, detailColor.g, detailColor.b);
    }

    public static void drawZoom(Graphics g, Card card, float width, float height) {
        float w = width - 2 * FDialog.INSETS;
        float h = height - 2 * FDialog.INSETS;

        final String key;
        if (FControl.mayShowCard(card) || FDialog.isDialogOpen()) { //support showing if card revealed in dialog
            key = card.getImageKey();
        }
        else { //only show card back if can't show card
            key = ImageKeys.TOKEN_PREFIX + ImageKeys.MORPH_IMAGE;
        }
        Texture image = ImageCache.getImage(key, true);
        float imageWidth = image.getWidth();
        float imageHeight = image.getHeight();

        if (imageWidth > w || imageHeight > h) {
            //scale down until image fits on screen
            float widthRatio = w / imageWidth;
            float heightRatio = h / imageHeight;

            if (widthRatio < heightRatio) {
                imageWidth *= widthRatio;
                imageHeight *= widthRatio;
            }
            else {
                imageWidth *= heightRatio;
                imageHeight *= heightRatio;
            }
        }
        else {
            //scale up as long as image fits on screen
            float minWidth = w / 2;
            float minHeight = h / 2;
            while (imageWidth < minWidth && imageHeight < minHeight) {
                imageWidth *= 2;
                imageHeight *= 2;
            }
        }

        g.drawImage(image, (width - imageWidth) / 2, (height - imageHeight) / 2, imageWidth, imageHeight);
    }

    public static void drawDetails(Graphics g, Card card, float width, float height) {
        float x = FDialog.INSETS;
        float y = FDialog.INSETS;
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

        boolean canShow = FControl.mayShowCard(card) || FDialog.isDialogOpen(); //support showing if card revealed in dialog

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
        float cardNameBoxHeight = Math.max(MANA_SYMBOL_SIZE + 2 * MANA_COST_PADDING, 2 * NAME_FONT.getFont().getCapHeight()) + 2 * TYPE_FONT.getFont().getCapHeight() + 2;

        //draw name/type box
        Color nameBoxColor1 = FSkinColor.tintColor(Color.WHITE, color1, NAME_BOX_TINT);
        Color nameBoxColor2 = color2 == null ? null : FSkinColor.tintColor(Color.WHITE, color2, NAME_BOX_TINT);
        drawCardNameBox(g, card, nameBoxColor1, nameBoxColor2, x, y, w, cardNameBoxHeight);

        float innerBorderThickness = outerBorderThickness / 2;
        float ptBoxHeight = 2 * PT_FONT.getFont().getCapHeight();
        float textBoxHeight = h - cardNameBoxHeight - ptBoxHeight - outerBorderThickness - 3 * innerBorderThickness; 

        y += cardNameBoxHeight + innerBorderThickness;
        Color textBoxColor1 = FSkinColor.tintColor(Color.WHITE, color1, TEXT_BOX_TINT);
        Color textBoxColor2 = color2 == null ? null : FSkinColor.tintColor(Color.WHITE, color2, TEXT_BOX_TINT);
        drawCardTextBox(g, card, canShow, textBoxColor1, textBoxColor2, x, y, w, textBoxHeight);

        y += textBoxHeight + innerBorderThickness;
        Color ptColor1 = FSkinColor.tintColor(Color.WHITE, color1, PT_BOX_TINT);
        Color ptColor2 = color2 == null ? null : FSkinColor.tintColor(Color.WHITE, color2, PT_BOX_TINT);
        drawCardIdAndPtBox(g, card, idForeColor, ptColor1, ptColor2, x, y, w, ptBoxHeight);
    }

    public static float getCardListItemHeight() {
        return Math.round(MANA_SYMBOL_SIZE + FSkinFont.get(12).getFont().getLineHeight() + 3 * FList.PADDING + 1);
    }

    private static final Map<String, TextureRegion> cardArtCache = new HashMap<String, TextureRegion>();
    public static final float CARD_ART_RATIO = 1.302f;

    //extract card art from the given card
    public static TextureRegion getCardArt(PaperCard paperCard) {
        return getCardArt(ImageKeys.getImageKey(paperCard, false));
    }
    public static TextureRegion getCardArt(Card card) {
        return getCardArt(card.getImageKey());
    }
    public static TextureRegion getCardArt(String imageKey) {
        TextureRegion cardArt = cardArtCache.get(imageKey);
        if (cardArt == null) {
            Texture image = ImageCache.getImage(imageKey, true);
            float w = image.getWidth();
            float h = image.getHeight();
            float x = w * 0.1f;
            float y = h * 0.11f;
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
            cardArt = new TextureRegion(image, Math.round(x), Math.round(y), Math.round(w), Math.round(h));
            cardArtCache.put(imageKey, cardArt);
        }
        return cardArt;
    }

    public static void drawCardListItem(Graphics g, FSkinFont font, FSkinColor foreColor, Card card, int count, float x, float y, float w, float h) {
        drawCardListItem(g, font, foreColor, getCardArt(card), card.getRules(), card.getCurSetCode(),
                card.getRarity(), card.getNetAttack(), card.getNetDefense(),
                card.getCurrentLoyalty(), count, x, y, w, h);
    }
    public static void drawCardListItem(Graphics g, FSkinFont font, FSkinColor foreColor, PaperCard paperCard, int count, float x, float y, float w, float h) {
        CardRules cardRules = paperCard.getRules();
        drawCardListItem(g, font, foreColor, getCardArt(paperCard), cardRules, paperCard.getEdition(),
                paperCard.getRarity(), cardRules.getIntPower(), cardRules.getIntToughness(),
                cardRules.getInitialLoyalty(), count, x, y, w, h);
    }
    public static void drawCardListItem(Graphics g, FSkinFont font, FSkinColor foreColor, TextureRegion cardArt, CardRules cardRules, String set, CardRarity rarity, int power, int toughness, int loyalty, int count, float x, float y, float w, float h) {
        float cardArtHeight = h + 2 * FList.PADDING;
        float cardArtWidth = cardArtHeight * CARD_ART_RATIO;
        g.drawImage(cardArt, x - FList.PADDING, y - FList.PADDING, cardArtWidth, cardArtHeight);
        x += cardArtWidth;

        String name = cardRules.getName();
        ManaCost manaCost = cardRules.getManaCost();
        float availableNameWidth = w - CardFaceSymbols.getWidth(manaCost, MANA_SYMBOL_SIZE) - cardArtWidth - FList.PADDING;
        if (count > 0) { //preface name with count if applicable
            name = count + " " + name;
        }
        g.drawText(name, font, foreColor, x, y, availableNameWidth, MANA_SYMBOL_SIZE, false, HAlignment.LEFT, true);
        x += availableNameWidth + FList.PADDING;
        CardFaceSymbols.drawManaCost(g, manaCost, x, y, MANA_SYMBOL_SIZE);

        x -= availableNameWidth + FList.PADDING;
        y += MANA_SYMBOL_SIZE + FList.PADDING + 1;

        FSkinFont typeFont = FSkinFont.get(12);
        float availableTypeWidth = w - cardArtWidth;
        float lineHeight = typeFont.getFont().getLineHeight();
        if (!StringUtils.isEmpty(set)) {
            float setWidth = getSetWidth(typeFont, set);
            availableTypeWidth -= setWidth;
            drawSetLabel(g, typeFont, set, rarity, x + availableTypeWidth + SET_BOX_MARGIN, y - SET_BOX_MARGIN, setWidth, lineHeight + 2 * SET_BOX_MARGIN);
        }
        String type = cardRules.getType().toString();
        if (cardRules.getType().isCreature()) { //include P/T or Loyalty at end of type
            type += " (" + power + " / " + toughness + ")";
        }
        else if (cardRules.getType().isPlaneswalker()) {
            type += " (" + loyalty + ")";
        }
        g.drawText(type, typeFont, foreColor, x, y, availableTypeWidth, lineHeight, false, HAlignment.LEFT, true);
    }

    public static boolean cardListItemTap(Card card, float x, float y, int count) {
        if (x <= getCardListItemHeight() * CARD_ART_RATIO) {
            CardZoom.show(card);
            return true;
        }
        return false;
    }
    public static boolean cardListItemTap(PaperCard paperCard, float x, float y, int count) {
        float cardArtHeight = getCardListItemHeight();
        float cardArtWidth = cardArtHeight * CARD_ART_RATIO;
        if (x <= cardArtWidth && y <= cardArtHeight) {
            CardZoom.show(Card.getCardForUi(paperCard));
            return true;
        }
        return false;
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
        g.drawRect(Utils.scaleMin(1), Color.BLACK, x, y, w, h);
        g.drawText(ptText, PT_FONT, Color.BLACK, x, y, w, h, false, HAlignment.CENTER, true);
    }
}
