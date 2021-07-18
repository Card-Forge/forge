package forge.card;

import static forge.card.CardRenderer.CROP_MULTIPLIER;
import static forge.card.CardRenderer.isModernFrame;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Align;
import com.google.common.collect.ImmutableList;

import forge.Forge;
import forge.Graphics;
import forge.assets.FBufferedImage;
import forge.assets.FImage;
import forge.assets.FSkin;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FSkinTexture;
import forge.assets.ImageCache;
import forge.assets.TextRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.card.mana.ManaCost;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.zone.ZoneType;
import forge.gui.card.CardDetailUtil;
import forge.gui.card.CardDetailUtil.DetailColors;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.screens.match.MatchController;
import forge.util.CardTranslation;
import forge.util.Utils;

public class CardImageRenderer {
    private static final float BASE_IMAGE_WIDTH = 360;
    private static final float BASE_IMAGE_HEIGHT = 504;
    private static float MANA_SYMBOL_SIZE, PT_BOX_WIDTH, HEADER_PADDING, BORDER_THICKNESS;
    private static FSkinFont NAME_FONT, TYPE_FONT, TEXT_FONT, PT_FONT;
    private static float prevImageWidth, prevImageHeight;
    private static final float BLACK_BORDER_THICKNESS_RATIO = 0.021f;

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

    public static void drawFaceDownCard(CardView card, Graphics g, float x, float y, float w, float h) {
        //try to draw the card sleeves first
        if (FSkin.getSleeves().get(card.getOwner()) != null)
            g.drawImage(FSkin.getSleeves().get(card.getOwner()), x, y, w, h);
        else
            drawArt(g, x, y, w, h);
    }

    public static void drawCardImage(Graphics g, CardView card, boolean altState, float x, float y, float w, float h, CardStackPosition pos) {
        updateStaticFields(w, h);

        float blackBorderThickness = w * BLACK_BORDER_THICKNESS_RATIO;
        g.fillRect(Color.BLACK, x, y, w, h);
        x += blackBorderThickness;
        y += blackBorderThickness;
        w -= 2 * blackBorderThickness;
        h -= 2 * blackBorderThickness;

        final CardStateView state = card.getState(altState);
        final boolean canShow = MatchController.instance.mayView(card);

        if (!canShow) {
                drawFaceDownCard(card, g, x, y, w, h);
            return;
        }

        //determine colors for borders
        final List<DetailColors> borderColors;
        final boolean isFaceDown = card.isFaceDown();
        if (isFaceDown) {
            borderColors = ImmutableList.of(DetailColors.FACE_DOWN);
        }
        else {
            borderColors = CardDetailUtil.getBorderColors(state, canShow);
        }
        Color[] colors = fillColorBackground(g, borderColors, x, y, w, h);

        float artInset = blackBorderThickness * 0.5f;
        float outerBorderThickness = 2 * blackBorderThickness - artInset;
        x += outerBorderThickness;
        y += outerBorderThickness;
        w -= 2 * outerBorderThickness;
        float headerHeight = Math.max(MANA_SYMBOL_SIZE + 2 * HEADER_PADDING, 2 * NAME_FONT.getCapHeight()) + 2;

        //draw header containing name and mana cost
        Color[] headerColors = FSkinColor.tintColors(Color.WHITE, colors, CardRenderer.NAME_BOX_TINT);
        drawHeader(g, card, state, headerColors, x, y, w, headerHeight);

        if (pos == CardStackPosition.BehindVert) { return; } //remaining rendering not needed if card is behind another card in a vertical stack
        boolean onTop = (pos == CardStackPosition.Top);

        y += headerHeight;

        float artWidth = w - 2 * artInset;
        float artHeight = artWidth / CardRenderer.CARD_ART_RATIO;
        float typeBoxHeight = 2 * TYPE_FONT.getCapHeight();
        float ptBoxHeight = 0;
        float textBoxHeight = h - headerHeight - artHeight - typeBoxHeight - outerBorderThickness - artInset;
        if (state.isCreature() || state.isPlaneswalker() || state.getType().hasSubtype("Vehicle")) {
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
        drawTypeLine(g, card, state, canShow, headerColors, x, y, w, typeBoxHeight);
        y += typeBoxHeight;

        //draw text box
        Color[] textBoxColors = FSkinColor.tintColors(Color.WHITE, colors, CardRenderer.TEXT_BOX_TINT);
        drawTextBox(g, card, state, textBoxColors, x + artInset, y, w - 2 * artInset, textBoxHeight, onTop);
        y += textBoxHeight;

        //draw P/T box
        if (onTop && ptBoxHeight > 0) {
            //only needed if on top since otherwise P/T will be hidden
            Color[] ptColors = FSkinColor.tintColors(Color.WHITE, colors, CardRenderer.PT_BOX_TINT);
            drawPtBox(g, card, state, ptColors, x, y - 2 * artInset, w, ptBoxHeight);
        }
    }

    private static void drawHeader(Graphics g, CardView card, CardStateView state, Color[] colors, float x, float y, float w, float h) {
        fillColorBackground(g, colors, x, y, w, h);
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);

        float padding = h / 8;

        //draw mana cost for card
        float manaCostWidth = 0;
        ManaCost mainManaCost = state.getManaCost();
        if (card.isSplitCard() && card.getAlternateState() != null) {
            //handle rendering both parts of split card
            mainManaCost = card.getLeftSplitState().getManaCost();
            ManaCost otherManaCost = card.getAlternateState().getManaCost();
            manaCostWidth = CardFaceSymbols.getWidth(otherManaCost, MANA_SYMBOL_SIZE) + HEADER_PADDING;
            CardFaceSymbols.drawManaCost(g, otherManaCost, x + w - manaCostWidth, y + (h - MANA_SYMBOL_SIZE) / 2, MANA_SYMBOL_SIZE);
            //draw "//" between two parts of mana cost
            manaCostWidth += NAME_FONT.getBounds("//").width + HEADER_PADDING;
            g.drawText("//", NAME_FONT, Color.BLACK, x + w - manaCostWidth, y, w, h, false, Align.left, true);
        }
        manaCostWidth += CardFaceSymbols.getWidth(mainManaCost, MANA_SYMBOL_SIZE) + HEADER_PADDING;
        CardFaceSymbols.drawManaCost(g, mainManaCost, x + w - manaCostWidth, y + (h - MANA_SYMBOL_SIZE) / 2, MANA_SYMBOL_SIZE);

        //draw name for card
        x += padding;
        w -= 2 * padding;
        g.drawText(CardTranslation.getTranslatedName(state.getName()), NAME_FONT, Color.BLACK, x, y, w - manaCostWidth - padding, h, false, Align.left, true);
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

    private static void drawTypeLine(Graphics g, CardView card, CardStateView state, boolean canShow, Color[] colors, float x, float y, float w, float h) {
        fillColorBackground(g, colors, x, y, w, h);
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);

        float padding = h / 8;

        //draw square icon for rarity
        float iconSize = h * 0.55f;
        float iconPadding = (h - iconSize) / 2;
        w -= iconSize + iconPadding * 2;
        g.fillRect(CardRenderer.getRarityColor(state.getRarity()), x + w + iconPadding, y + (h - iconSize) / 2, iconSize, iconSize);

        //draw type
        x += padding;
        g.drawText(CardDetailUtil.formatCardType(state, canShow), TYPE_FONT, Color.BLACK, x, y, w, h, false, Align.left, true);
    }

    //use text renderer to handle mana symbols and reminder text
    private static final TextRenderer cardTextRenderer = new TextRenderer(true);

    private static void drawTextBox(Graphics g, CardView card, CardStateView state, Color[] colors, float x, float y, float w, float h, boolean onTop) {
        fillColorBackground(g, colors, x, y, w, h);
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);

        if (!onTop) { return; } //remaining rendering only needed if card on top

        if (state.isBasicLand()) {
            //draw icons for basic lands
            FSkinImage image;
            switch (state.getName().replaceFirst("^Snow-Covered ", "")) {
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
            case "Forest":
                image = FSkinImage.MANA_G;
                break;
            default:
                image = FSkinImage.MANA_COLORLESS;
                break;
            }
            float iconSize = h * 0.75f;
            g.drawImage(image, x + (w - iconSize) / 2, y + (h - iconSize) / 2, iconSize, iconSize);
        }
        else {
            boolean needTranslation = true;
            if (card.isToken()) {
                if (card.getCloneOrigin() == null)
                    needTranslation = false;
            }
            final String text = !card.isSplitCard() ?
                card.getText(state, needTranslation ? CardTranslation.getTranslationTexts(state.getName(), "") : null) :
                card.getText(state, needTranslation ? CardTranslation.getTranslationTexts(card.getLeftSplitState().getName(), card.getRightSplitState().getName()) : null );
            if (StringUtils.isEmpty(text)) { return; }

            float padding = TEXT_FONT.getCapHeight() * 0.75f;
            x += padding;
            y += padding;
            w -= 2 * padding;
            h -= 2 * padding;
            cardTextRenderer.drawText(g, text, TEXT_FONT, Color.BLACK, x, y, w, h, y, h, true, Align.left, true);
        }
    }

    private static void drawPtBox(Graphics g, CardView card, CardStateView state, Color[] colors, float x, float y, float w, float h) {
        List<String> pieces = new ArrayList<>();
        if (state.isCreature()) {
            pieces.add(String.valueOf(state.getPower()));
            pieces.add("/");
            pieces.add(String.valueOf(state.getToughness()));
        }
        else if (state.isPlaneswalker()) {
            pieces.add(String.valueOf(state.getLoyalty()));
        }
        else if (state.getType().hasSubtype("Vehicle")) {
            // TODO Invert color box for Vehicles?
            pieces.add("[");
            pieces.add(String.valueOf(state.getPower()));
            pieces.add("/");
            pieces.add(String.valueOf(state.getToughness()));
            pieces.add("]");
        }
        else { return; }

        float padding = Math.round(PT_FONT.getCapHeight() / 4);
        float totalPieceWidth = -padding;
        float[] pieceWidths = new float[pieces.size()];
        for (int i = 0; i < pieces.size(); i++) {
            float pieceWidth = PT_FONT.getBounds(pieces.get(i)).width + padding;
            pieceWidths[i] = pieceWidth;
            totalPieceWidth += pieceWidth;
        }
        float boxHeight = PT_FONT.getCapHeight() + PT_FONT.getAscent() + 3 * padding;

        float boxWidth = Math.max(PT_BOX_WIDTH, totalPieceWidth + 2 * padding);
        x += w - boxWidth;
        y += h - boxHeight;
        w = boxWidth;
        h = boxHeight;

        fillColorBackground(g, colors, x, y, w, h);
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);

        x += (boxWidth - totalPieceWidth) / 2;
        for (int i = 0; i < pieces.size(); i++) {
            g.drawText(pieces.get(i), PT_FONT, Color.BLACK, x, y, w, h, false, Align.left, true);
            x += pieceWidths[i];
        }
    }

    public static void drawZoom(Graphics g, CardView card, GameView gameView, boolean altState, float x, float y, float w, float h, float dispW, float dispH, boolean isCurrentCard) {
        boolean canshow = MatchController.instance.mayView(card);
        Texture image = null;
        try {
            image = ImageCache.getImage(card.getState(altState).getImageKey(), true);
        } catch (Exception ex) {
            //System.err.println(card.toString()+" : " +ex.getMessage());
            //TODO: don't know why this is needed, needs further investigation...
            if (!card.hasAlternateState()) {
                altState = false;
                image = ImageCache.getImage(card.getState(altState).getImageKey(), true);
            }
        }
        FImage sleeves = MatchController.getPlayerSleeve(card.getOwner());
        if (image == null) { //draw details if can't draw zoom
            drawDetails(g, card, gameView, altState, x, y, w, h);
            return;
        }
        if(card.isImmutable() && FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_DISABLE_IMAGES_EFFECT_CARDS)){
            drawDetails(g, card, gameView, altState, x, y, w, h);
            return;
        }

        if (image == ImageCache.defaultImage) { //support drawing card image manually if card image not found
            drawCardImage(g, card, altState, x, y, w, h, CardStackPosition.Top);
        } else {
            float radius = (h - w)/8;
            float wh_Adj = ForgeConstants.isGdxPortLandscape && isCurrentCard ? 1.38f:1.0f;
            float new_w = w*wh_Adj;
            float new_h = h*wh_Adj;
            float new_x = ForgeConstants.isGdxPortLandscape && isCurrentCard ? (dispW - new_w) / 2:x;
            float new_y = ForgeConstants.isGdxPortLandscape && isCurrentCard ? (dispH - new_h) / 2:y;
            float new_xRotate = (dispW - new_h) /2;
            float new_yRotate = (dispH - new_w) /2;
            boolean rotateSplit = FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_ROTATE_SPLIT_CARDS);
            boolean rotatePlane = FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_ROTATE_PLANE_OR_PHENOMENON);
            float croppedArea = isModernFrame(card) ? CROP_MULTIPLIER : 0.97f;
            float minusxy = isModernFrame(card) ? 0.0f : 0.13f*radius;
            if (card.getCurrentState().getSetCode().equals("LEA")||card.getCurrentState().getSetCode().equals("LEB")) {
                croppedArea = 0.975f;
                minusxy = 0.135f*radius;
            }
            if (rotatePlane && (card.getCurrentState().isPhenomenon() || card.getCurrentState().isPlane())) {
                if (Forge.enableUIMask.equals("Full")){
                    if (ImageCache.isBorderlessCardArt(image))
                        g.drawRotatedImage(image, new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, -90);
                    else {
                        g.drawRotatedImage(FSkin.getBorders().get(0), new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, -90);
                        g.drawRotatedImage(ImageCache.croppedBorderImage(image), new_x+radius/2-minusxy, new_y+radius/2-minusxy, new_w*croppedArea, new_h*croppedArea, (new_x+radius/2-minusxy) + (new_w*croppedArea) / 2, (new_y+radius/2-minusxy) + (new_h*croppedArea) / 2, -90);
                    }
                } else if (Forge.enableUIMask.equals("Crop")) {
                    g.drawRotatedImage(ImageCache.croppedBorderImage(image), new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, -90);
                } else
                    g.drawRotatedImage(image, new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, -90);
            } else if (rotateSplit && isCurrentCard && card.isSplitCard() && canshow) {
                boolean isAftermath = card.getText().contains("Aftermath") || card.getAlternateState().getOracleText().contains("Aftermath");
                if (Forge.enableUIMask.equals("Full")) {
                    if (ImageCache.isBorderlessCardArt(image))
                        g.drawRotatedImage(image, new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, isAftermath ? 90 : -90);
                    else {
                        g.drawRotatedImage(FSkin.getBorders().get(ImageCache.getFSkinBorders(card)), new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, isAftermath ? 90 : -90);
                        g.drawRotatedImage(ImageCache.croppedBorderImage(image), new_x + radius / 2-minusxy, new_y + radius / 2-minusxy, new_w * croppedArea, new_h * croppedArea, (new_x + radius / 2-minusxy) + (new_w * croppedArea) / 2, (new_y + radius / 2-minusxy) + (new_h * croppedArea) / 2, isAftermath ? 90 : -90);
                    }
                } else if (Forge.enableUIMask.equals("Crop")) {
                    g.drawRotatedImage(ImageCache.croppedBorderImage(image), new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, isAftermath ? 90 : -90);
                } else
                    g.drawRotatedImage(image, new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, isAftermath ? 90 : -90);
            } else {
                if (Forge.enableUIMask.equals("Full") && canshow) {
                    if (ImageCache.isBorderlessCardArt(image))
                        g.drawImage(image, x, y, w, h);
                    else {
                        g.drawImage(ImageCache.getBorderImage(image.toString()), ImageCache.borderColor(image), x, y, w, h);
                        g.drawImage(ImageCache.croppedBorderImage(image), x + radius / 2.4f-minusxy, y + radius / 2-minusxy, w * croppedArea, h * croppedArea);
                    }
                } else if (Forge.enableUIMask.equals("Crop") && canshow) {
                    g.drawImage(ImageCache.croppedBorderImage(image), x, y, w, h);
                } else {
                    if (canshow)
                        g.drawImage(image, x, y, w, h);
                    else // sleeve
                        g.drawImage(sleeves, x, y, w, h);
                }
            }
        }
        CardRenderer.drawFoilEffect(g, card, x, y, w, h, isCurrentCard && canshow && image != ImageCache.defaultImage);
    }

    public static void drawDetails(Graphics g, CardView card, GameView gameView, boolean altState, float x, float y, float w, float h) {
        updateStaticFields(w, h);

        float blackBorderThickness = w * BLACK_BORDER_THICKNESS_RATIO;
        g.fillRect(Color.BLACK, x, y, w, h);
        x += blackBorderThickness;
        y += blackBorderThickness;
        w -= 2 * blackBorderThickness;
        h -= 2 * blackBorderThickness;

        final CardStateView state = card.getState(altState);
        final boolean canShow = MatchController.instance.mayView(card);

        //determine colors for borders
        final List<DetailColors> borderColors;
        final boolean isFaceDown = card.isFaceDown();
        if (isFaceDown) {
            borderColors = ImmutableList.of(DetailColors.FACE_DOWN);
        }
        else {
            borderColors = CardDetailUtil.getBorderColors(state, canShow);
        }
        Color[] colors = fillColorBackground(g, borderColors, x, y, w, h);

        Color idForeColor = FSkinColor.getHighContrastColor(colors[0]);

        float outerBorderThickness = 2 * blackBorderThickness;
        x += outerBorderThickness;
        y += outerBorderThickness;
        w -= 2 * outerBorderThickness;
        float cardNameBoxHeight = Math.max(MANA_SYMBOL_SIZE + 2 * HEADER_PADDING, 2 * NAME_FONT.getCapHeight()) + 2 * TYPE_FONT.getCapHeight() + 2;

        //draw name/type box
        Color[] nameBoxColors = FSkinColor.tintColors(Color.WHITE, colors, CardRenderer.NAME_BOX_TINT);
        drawDetailsNameBox(g, card, state, canShow, nameBoxColors, x, y, w, cardNameBoxHeight);

        float innerBorderThickness = outerBorderThickness / 2;
        float ptBoxHeight = 2 * PT_FONT.getCapHeight();
        float textBoxHeight = h - cardNameBoxHeight - ptBoxHeight - outerBorderThickness - 3 * innerBorderThickness; 

        y += cardNameBoxHeight + innerBorderThickness;
        Color[] textBoxColors = FSkinColor.tintColors(Color.WHITE, colors, CardRenderer.TEXT_BOX_TINT);
        drawDetailsTextBox(g, state, gameView, canShow, textBoxColors, x, y, w, textBoxHeight);

        y += textBoxHeight + innerBorderThickness;
        Color[] ptColors = FSkinColor.tintColors(Color.WHITE, colors, CardRenderer.PT_BOX_TINT);
        drawDetailsIdAndPtBox(g, card, state, canShow, idForeColor, ptColors, x, y, w, ptBoxHeight);
    }

    public static Color[] fillColorBackground(Graphics g, List<DetailColors> backColors, float x, float y, float w, float h) {
        Color[] colors = new Color[backColors.size()];
        for (int i = 0; i < colors.length; i++) {
            DetailColors dc = backColors.get(i);
            colors[i] = FSkinColor.fromRGB(dc.r, dc.g, dc.b);
        }
        fillColorBackground(g, colors, x, y, w, h);
        return colors;
    }
    public static void fillColorBackground(Graphics g, Color[] colors, float x, float y, float w, float h) {
        switch (colors.length) {
        case 1:
            g.fillRect(colors[0], x, y, w, h);
            break;
        case 2:
            g.fillGradientRect(colors[0], colors[1], false, x, y, w, h);
            break;
        case 3:
            float halfWidth = w / 2;
            g.fillGradientRect(colors[0], colors[1], false, x, y, halfWidth, h);
            g.fillGradientRect(colors[1], colors[2], false, x + halfWidth, y, halfWidth, h);
            break;
        }
    }

    private static void drawDetailsNameBox(Graphics g, CardView card, CardStateView state, boolean canShow, Color[] colors, float x, float y, float w, float h) {
        fillColorBackground(g, colors, x, y, w, h);
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);

        float padding = h / 8;

        //make sure name/mana cost row height is tall enough for both
        h = Math.max(MANA_SYMBOL_SIZE + 2 * HEADER_PADDING, 2 * NAME_FONT.getCapHeight());

        //draw mana cost for card
        float manaCostWidth = 0;
        if (canShow) {
            ManaCost mainManaCost = state.getManaCost();
            if (card.isSplitCard() && card.hasAlternateState() && !card.isFaceDown() && card.getZone() != ZoneType.Stack) { //only display current state's mana cost when on stack
                //handle rendering both parts of split card
                mainManaCost = card.getLeftSplitState().getManaCost();
                ManaCost otherManaCost = card.getAlternateState().getManaCost();
                manaCostWidth = CardFaceSymbols.getWidth(otherManaCost, MANA_SYMBOL_SIZE) + HEADER_PADDING;
                CardFaceSymbols.drawManaCost(g, otherManaCost, x + w - manaCostWidth, y + (h - MANA_SYMBOL_SIZE) / 2, MANA_SYMBOL_SIZE);
                //draw "//" between two parts of mana cost
                manaCostWidth += NAME_FONT.getBounds("//").width + HEADER_PADDING;
                g.drawText("//", NAME_FONT, Color.BLACK, x + w - manaCostWidth, y, w, h, false, Align.left, true);
            }
            manaCostWidth += CardFaceSymbols.getWidth(mainManaCost, MANA_SYMBOL_SIZE) + HEADER_PADDING;
            CardFaceSymbols.drawManaCost(g, mainManaCost, x + w - manaCostWidth, y + (h - MANA_SYMBOL_SIZE) / 2, MANA_SYMBOL_SIZE);
        }

        //draw name for card
        x += padding;
        w -= 2 * padding;
        g.drawText(CardDetailUtil.formatCardName(card, canShow, state == card.getAlternateState()), NAME_FONT, Color.BLACK, x, y, w - manaCostWidth - padding, h, false, Align.left, true);

        //draw type and set label for card
        y += h;
        h = 2 * TYPE_FONT.getCapHeight();

        String set = state.getSetCode();
        CardRarity rarity = state.getRarity();
        if (!canShow) {
            set = CardEdition.UNKNOWN.getCode();
            rarity = CardRarity.Unknown;
        }
        if (!StringUtils.isEmpty(set)) {
            float setWidth = CardRenderer.getSetWidth(TYPE_FONT, set);
            CardRenderer.drawSetLabel(g, TYPE_FONT, set, rarity, x + w + padding - setWidth - HEADER_PADDING + CardRenderer.SET_BOX_MARGIN, y + CardRenderer.SET_BOX_MARGIN, setWidth, h - CardRenderer.SET_BOX_MARGIN);
            w -= setWidth; //reduce available width for type
        }

        g.drawText(CardDetailUtil.formatCardType(state, canShow), TYPE_FONT, Color.BLACK, x, y, w, h, false, Align.left, true);
    }

    private static void drawDetailsTextBox(Graphics g, CardStateView state, GameView gameView, boolean canShow, Color[] colors, float x, float y, float w, float h) {
        fillColorBackground(g, colors, x, y, w, h);
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);

        float padX = TEXT_FONT.getCapHeight() / 2;
        float padY = padX + Utils.scale(2); //add a little more vertical padding
        x += padX;
        y += padY;
        w -= 2 * padX;
        h -= 2 * padY;
        cardTextRenderer.drawText(g, CardDetailUtil.composeCardText(state, gameView, canShow), TEXT_FONT, Color.BLACK, x, y, w, h, y, h, true, Align.left, false);
    }

    private static void drawDetailsIdAndPtBox(Graphics g, CardView card, CardStateView state, boolean canShow, Color idForeColor, Color[] colors, float x, float y, float w, float h) {
        float idWidth = 0;
        if (canShow) {
            String idText = CardDetailUtil.formatCardId(state);
            g.drawText(idText, TYPE_FONT, idForeColor, x, y + TYPE_FONT.getCapHeight() / 2, w, h, false, Align.left, false);
            idWidth = TYPE_FONT.getBounds(idText).width;
        }

        String ptText = CardDetailUtil.formatPowerToughness(state, canShow);
        if (StringUtils.isEmpty(ptText)) { return; }

        float padding = PT_FONT.getCapHeight() / 2;
        float boxWidth = Math.min(PT_FONT.getBounds(ptText).width + 2 * padding,
                w - idWidth - padding); //prevent box overlapping ID
        x += w - boxWidth;
        w = boxWidth;

        fillColorBackground(g, colors, x, y, w, h);
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);
        g.drawText(ptText, PT_FONT, Color.BLACK, x, y, w, h, false, Align.center, true);
    }
}
