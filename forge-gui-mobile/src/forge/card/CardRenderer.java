package forge.card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.ImageKeys;
import forge.localinstance.properties.ForgeConstants;
import forge.util.*;
import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import forge.CachedCardImage;
import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FImageComplex;
import forge.assets.FRotatedImage;
import forge.assets.FSkin;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FTextureRegionImage;
import forge.assets.ImageCache;
import forge.card.CardZoom.ActivateHandler;
import forge.card.mana.ManaCost;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.card.CounterType;
import forge.game.zone.ZoneType;
import forge.gui.FThreads;
import forge.gui.card.CardDetailUtil;
import forge.gui.card.CardDetailUtil.DetailColors;
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.localinstance.properties.ForgeConstants.CounterDisplayType;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.match.MatchController;
import forge.toolbox.FList;

public class CardRenderer {
    public enum CardStackPosition {
        Top,
        BehindHorz,
        BehindVert
    }

    // class that simplifies the callback logic of CachedCardImage
    static class RendererCachedCardImage extends CachedCardImage {
        boolean clearCardArtCache = false;

        public RendererCachedCardImage(CardView card, boolean clearArtCache) {
            super(card);
            this.clearCardArtCache = clearArtCache;
        }

        public RendererCachedCardImage(InventoryItem ii, boolean clearArtCache) {
            super(ii);
            this.clearCardArtCache = clearArtCache;
        }

        public RendererCachedCardImage(String key, boolean clearArtCache) {
            super(key);
            this.clearCardArtCache = clearArtCache;
        }

        @Override
        public void onImageFetched() {
            ImageCache.clear();
            if (clearCardArtCache) {
                cardArtCache.remove(key);
            }
        }
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
    public static final float CROP_MULTIPLIER = 0.96f;

    private static Map<Integer, BitmapFont> counterFonts = new HashMap<>();
    private static final Color counterBackgroundColor = new Color(0f, 0f, 0f, 0.9f);
    private static final Map<CounterType, Color> counterColorCache = new HashMap<>();
    private static final GlyphLayout layout = new GlyphLayout();

    static {
        try {
            for (int fontSize = 8; fontSize <= 22; fontSize++) {
                generateFontForCounters(fontSize);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Color fromDetailColor(DetailColors detailColor) {
        return FSkinColor.fromRGB(detailColor.r, detailColor.g, detailColor.b);
    }

    public static Color getRarityColor(CardRarity rarity) {
        if (rarity == null)// NPE from Rarity weird...
            return Color.CLEAR;
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

    public static boolean isModernFrame(IPaperCard c) {
        if (c == null)
            return false;

        CardEdition ed = FModel.getMagicDb().getEditions().get(c.getEdition());
        if (ed != null) {
            switch (ed.getCode()) {
                case "MED":
                case "ME2":
                case "ME3":
                case "ME4":
                case "TSB":
                    return false;
                default:
                    return ed.isModern();
            }
        }

        return false;
    }

    public static boolean isModernFrame(CardView c) {
        if (c == null)
            return false;

        CardView.CardStateView state = c.getCurrentState();
        CardEdition ed = FModel.getMagicDb().getEditions().get(state.getSetCode());
        if (ed != null) {
            switch (ed.getCode()) {
                case "MED":
                case "ME2":
                case "ME3":
                case "ME4":
                case "TSB":
                    return false;
                default:
                    return ed.isModern();
            }
        }

        return false;
    }

    public static float getCardListItemHeight(boolean compactMode) {
        if (compactMode) {
            return MANA_SYMBOL_SIZE + 2 * FList.PADDING;
        }
        return Math.round(MANA_SYMBOL_SIZE + FSkinFont.get(12).getLineHeight() + 3 * FList.PADDING + 1);
    }

    private static final Map<String, FImageComplex> cardArtCache = new HashMap<>(1024);
    public static final float CARD_ART_RATIO = 1.302f;
    public static final float CARD_ART_HEIGHT_PERCENTAGE = 0.43f;
    private static List<String> classicModuleCardtoCrop = FileUtil.readFile(ForgeConstants.CLASSIC_MODULE_CARD_TO_CROP_FILE);

    public static void clearcardArtCache(){
        cardArtCache.clear();
    }

    //extract card art from the given card
    public static FImageComplex getCardArt(IPaperCard pc) {
        return getCardArt(pc, false);
    }

    public static FImageComplex getCardArt(IPaperCard pc, boolean backFace) {
        CardType type = pc.getRules().getType();
        return getCardArt(pc.getImageKey(backFace), pc.getRules().getSplitType() == CardSplitType.Split,
                type.isPlane() || type.isPhenomenon(),pc.getRules().getOracleText().contains("Aftermath"),
                type.hasSubtype("Saga"), type.hasSubtype("Class"), type.isDungeon(), CardSplitType.Flip.equals(pc.getRules().getSplitType()),
                type.isPlaneswalker(), isModernFrame(pc));
    }

    public static FImageComplex getCardArt(CardView card) {
        CardTypeView type = card.getCurrentState().getType();
        return getCardArt(card.getCurrentState().getImageKey(), card.isSplitCard(), type.isPlane() || type.isPhenomenon(),
                card.getText().contains("Aftermath"), type.hasSubtype("Saga"), type.hasSubtype("Class"), type.isDungeon(),
                card.isFlipCard(), type.isPlaneswalker(), isModernFrame(card));
    }

    public static FImageComplex getCardArt(String imageKey, boolean isSplitCard, boolean isHorizontalCard, boolean isAftermathCard, boolean isSaga, boolean isClass, boolean isDungeon, boolean isFlipCard, boolean isPlanesWalker, boolean isModernFrame) {
        FImageComplex cardArt = cardArtCache.get(imageKey);
        boolean isClassicModule = imageKey != null && imageKey.length() > 2 && classicModuleCardtoCrop.contains(imageKey.substring(ImageKeys.CARD_PREFIX.length()).replace(".jpg", "").replace(".png", ""));
        if (cardArt == null) {
            Texture image = new RendererCachedCardImage(imageKey, true).getImage();
            if (image != null) {
                if (image == ImageCache.defaultImage) {
                    cardArt = CardImageRenderer.forgeArt;
                }
                else {
                    float x, y;
                    float w = image.getWidth();
                    float h = image.getHeight();
                     if (isClassicModule) {
                        x = w * 0.09f;
                        y = h * 0.2f;
                        w -= 2f * x;
                        h -= 3f * y;
                    }else if (isPlanesWalker) {
                        x = w * 0.09f;
                        y = h * 0.11f;
                        w -= 2f * x;
                        h -= 5.71f * y;
                    } else if (isFlipCard) {
                        x = w * 0.09f;
                        y = h * 0.32f;
                        w -= 2f * x;
                        h -= 2.1f * y;
                    } else if (isDungeon) {
                        x = w * 0.09f;
                        y = h * 0.1f;
                        w -= 2f * x;
                        h -= 2.2f * y;
                    } else if (isClass) {
                        x = w * 0.09f;
                        y = h * 0.11f;
                        w -= 1.1f * x + w / 2;
                        h -= 2.45f * y;
                    } else if (isSaga) {
                        x = (w * 0.1f) + (w * 0.8f / 2);
                        y = h * 0.11f;
                        w -= 1.16f * x;
                        h -= 2.45f * y;
                    } else if (isSplitCard && !isAftermathCard) { //allow rotated image for split cards
                        x = w * 33f / 250f;
                        y = 0; //delay adjusting y and h until drawn
                        w *= 106f / 250f;
                    } else if (isHorizontalCard) { //allow rotated image for horizontal cards
                        float artX = 40f, artY = 40f;
                        float artW = 350f, artH = 156f;
                        float srcW = 430f, srcH = 300f;
                        if (w > h) {
                            x = w * 40f / 430f;
                            y = h * 40f / srcH;
                            w *= artW / srcW;
                            h *= artH / srcH;
                        } else { //rotate art clockwise if its not the correct orientation
                            x = w * artY / srcH;
                            y = h * (srcW - artW - artX) / srcW;
                            w *= artH / srcH;
                            h *= artW / srcW;
                            cardArt = new FRotatedImage(image, Math.round(x), Math.round(y), Math.round(w), Math.round(h), true);
                            cardArtCache.put(imageKey, cardArt);
                            return cardArt;
                        }
                    } else {
                         //adjust smaller crop
                        x = isModernFrame ? w * 0.1f :w * 0.12f;
                        y = isModernFrame ? h * 0.12f : h * 0.11f;
                        w -= isModernFrame ? 2 * x : 2.1f * x;
                        h *= CARD_ART_HEIGHT_PERCENTAGE;
                        float ratioRatio = w / h / CARD_ART_RATIO;
                        if (ratioRatio > 1) { //if too wide, shrink width
                            float dw = w * (ratioRatio - 1);
                            w -= dw;
                            x += dw / 2;
                        } else { //if too tall, shrink height
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
        //fix display for effect
        if (imageKey == "" && cardArt == null)
            return CardImageRenderer.forgeArt;
        return cardArt;
    }

    public static FImageComplex getAftermathSecondCardArt(final String imageKey) {
        FImageComplex cardArt = cardArtCache.get("Aftermath_second_"+imageKey);
        if (cardArt == null) {
            Texture image = new CachedCardImage(imageKey) {
                @Override
                public void onImageFetched() {
                    ImageCache.clear();
                    cardArtCache.remove("Aftermath_second_" + imageKey);
                }
            }.getImage();
            if (image != null) {
                if (image == ImageCache.defaultImage) {
                    cardArt = CardImageRenderer.forgeArt;
                } else {
                    float x, y;
                    float w = image.getWidth();
                    float h = image.getHeight();
                    //allow rotated image for split cards
                    x = w * 138f / 250f;
                    y = h * 210f / 370f; //delay adjusting y and h until drawn
                    w *= 68f / 250f;
                    h *= 128f / 370f;

                    cardArt = new FTextureRegionImage(new TextureRegion(image, Math.round(x), Math.round(y), Math.round(w), Math.round(h)));

                }
                cardArtCache.put("Aftermath_second_"+imageKey, cardArt);
            }
        }
        return cardArt;
    }

    public static FImageComplex getAlternateCardArt(final String imageKey, boolean isPlanesWalker) {
        FImageComplex cardArt = cardArtCache.get("Alternate_"+imageKey);
        if (cardArt == null) {
            Texture image = new CachedCardImage(imageKey) {
                @Override
                public void onImageFetched() {
                    ImageCache.clear();
                    cardArtCache.remove("Alternate_" + imageKey);
                }
            }.getImage();
            if (image != null) {
                if (image == ImageCache.defaultImage) {
                    cardArt = CardImageRenderer.forgeArt;
                } else {
                    float x, y;
                    float w = image.getWidth();
                    float h = image.getHeight();
                    if (isPlanesWalker) {
                        x = w * 0.09f;
                        y = h * 0.11f;
                        w -= 2f * x;
                        h -= 5.71f * y;
                    } else {
                        x = w * 0.1f;
                        y = h * 0.11f;
                        w -= 2 * x;
                        h *= CARD_ART_HEIGHT_PERCENTAGE;
                        float ratioRatio = w / h / CARD_ART_RATIO;
                        if (ratioRatio > 1) { //if too wide, shrink width
                            float dw = w * (ratioRatio - 1);
                            w -= dw;
                            x += dw / 2;
                        } else { //if too tall, shrink height
                            float dh = h * (1 - ratioRatio);
                            h -= dh;
                            y += dh / 2;
                        }
                    }
                    cardArt = new FTextureRegionImage(new TextureRegion(image, Math.round(x), Math.round(y), Math.round(w), Math.round(h)));
                }
                cardArtCache.put("Alternate_"+imageKey, cardArt);
            }
        }
        return cardArt;
    }

    public static FImageComplex getMeldCardParts(final String imageKey, boolean bottom) {
        FImageComplex cardArt;
        if (!bottom) {
            cardArt = cardArtCache.get("Meld_primary_"+imageKey);
        } else {
            cardArt = cardArtCache.get("Meld_secondary_"+imageKey);
        }

        if (cardArt == null) {
            Texture image = new CachedCardImage(imageKey) {
                @Override
                public void onImageFetched() {
                    ImageCache.clear();
                    cardArtCache.remove("Meld_primary_" + imageKey);
                    cardArtCache.remove("Meld_secondary_" + imageKey);
                }
            }.getImage();
            if (image != null) {
                if (image == ImageCache.defaultImage) {
                    cardArt = CardImageRenderer.forgeArt;
                } else {
                    float x = 0;
                    float w = image.getWidth();
                    float h = image.getHeight()/2f;
                    float y = bottom ? h : 0;
                    cardArt = new FTextureRegionImage(new TextureRegion(image, Math.round(x), Math.round(y), Math.round(w), Math.round(h)));

                }
                if (!bottom)
                    cardArtCache.put("Meld_primary_"+imageKey, cardArt);
                else
                    cardArtCache.put("Meld_secondary_"+imageKey, cardArt);
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
            String name = CardTranslation.getTranslatedName(state.getName());
            if (count > 0) { //preface name with count if applicable
                name = count + " " + name;
            }
            if (suffix != null) {
                name += suffix;
            }
            g.drawText(name, font, foreColor, x, y, w, h, false, Align.center, true);
        }
    }

    public static void drawCardListItem(Graphics g, FSkinFont font, FSkinColor foreColor, IPaperCard pc, int count, String suffix, float x, float y, float w, float h, boolean compactMode) {
        final CardView card = CardView.getCardForUi(pc);
        final CardStateView state = card.getCurrentState();
        drawCardListItem(g, font, foreColor, getCardArt(pc), card, pc.getEdition(),
                pc.getRarity(), state.getPower(), state.getToughness(),
                state.getLoyalty(), count, suffix, x, y, w, h, compactMode);
    }

    public static void drawCardListItem(Graphics g, FSkinFont font, FSkinColor foreColor, FImageComplex cardArt, CardView card, String set, CardRarity rarity, int power, int toughness, String loyalty, int count, String suffix, float x, float y, float w, float h, boolean compactMode) {
        float cardArtHeight = h + 2 * FList.PADDING;
        float cardArtWidth = cardArtHeight * CARD_ART_RATIO;
        if (cardArt != null) {
            float artX = x - FList.PADDING;
            float artY = y - FList.PADDING;

            if (card.isSplitCard() && !card.getText().contains("Aftermath")) {
                //draw split art with proper orientation
                float srcY = cardArt.getHeight() * 13f / 354f;
                float srcHeight = cardArt.getHeight() * 150f / 354f;
                float dh = srcHeight * (1 - cardArt.getWidth() / srcHeight / CARD_ART_RATIO);
                srcHeight -= dh;
                srcY += dh / 2;
                g.drawRotatedImage(cardArt.getTexture(), artX, artY, cardArtHeight, cardArtWidth / 2, artX + cardArtWidth / 2, artY + cardArtWidth / 2, cardArt.getRegionX(), (int)srcY, (int)cardArt.getWidth(), (int)srcHeight, -90);
                g.drawRotatedImage(cardArt.getTexture(), artX, artY + cardArtWidth / 2, cardArtHeight, cardArtWidth / 2, artX + cardArtWidth / 2, artY + cardArtWidth / 2, cardArt.getRegionX(), (int)cardArt.getHeight() - (int)(srcY + srcHeight), (int)cardArt.getWidth(), (int)srcHeight, -90);
            }
            else if (card.getText().contains("Aftermath")) {
                FImageComplex secondArt = CardRenderer.getAftermathSecondCardArt(card.getCurrentState().getImageKey());
                g.drawRotatedImage(cardArt.getTexture(), artX, artY, cardArtWidth, cardArtHeight / 2, artX + cardArtWidth, artY + cardArtHeight / 2, cardArt.getRegionX(), cardArt.getRegionY(), (int)cardArt.getWidth(), (int)cardArt.getHeight() /2, 0);
                g.drawRotatedImage(secondArt.getTexture(), artX - cardArtHeight / 2 , artY + cardArtHeight / 2, cardArtHeight /2, cardArtWidth, artX, artY + cardArtHeight / 2, secondArt.getRegionX(), secondArt.getRegionY(), (int)secondArt.getWidth(), (int)secondArt.getHeight(), 90);
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
            mainManaCost = card.getLeftSplitState().getManaCost();
            ManaCost otherManaCost = card.getAlternateState().getManaCost();
            manaCostWidth = CardFaceSymbols.getWidth(otherManaCost, MANA_SYMBOL_SIZE) + MANA_COST_PADDING;
            CardFaceSymbols.drawManaCost(g, otherManaCost, x + w - manaCostWidth + MANA_COST_PADDING, y, MANA_SYMBOL_SIZE);
            //draw "//" between two parts of mana cost
            manaCostWidth += font.getBounds("//").width + MANA_COST_PADDING;
            g.drawText("//", font, foreColor, x + w - manaCostWidth + MANA_COST_PADDING, y, w, MANA_SYMBOL_SIZE, false, Align.left, true);
        }
        manaCostWidth += CardFaceSymbols.getWidth(mainManaCost, MANA_SYMBOL_SIZE);
        CardFaceSymbols.drawManaCost(g, mainManaCost, x + w - manaCostWidth, y, MANA_SYMBOL_SIZE);

        x += cardArtWidth;
        String name = CardTranslation.getTranslatedName(card.getCurrentState().getName());
        if (count > 0) { //preface name with count if applicable
            name = count + " " + name;
        }
        if (suffix != null) {
            name += suffix;
        }
        g.drawText(name, font, foreColor, x, y, w - manaCostWidth - cardArtWidth - FList.PADDING, MANA_SYMBOL_SIZE, false, Align.left, true);

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
        else if (card.getCurrentState().getType().hasSubtype("Vehicle")) {
            type += String.format(" [%s / %s]", power, toughness);
        }
        g.drawText(type, typeFont, foreColor, x, y, availableTypeWidth, lineHeight, false, Align.left, true);
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
        g.drawText(set, font, foreColor, x, y, w, h, false, Align.center, true);
    }

    public static void drawCard(Graphics g, IPaperCard pc, float x, float y, float w, float h, CardStackPosition pos) {
        Texture image = new RendererCachedCardImage(pc, false).getImage();
        float radius = (h - w)/8;
        float croppedArea = isModernFrame(pc) ? CROP_MULTIPLIER : 0.97f;
        float minusxy = isModernFrame(pc) ? 0.0f : 0.13f*radius;
        if (pc.getEdition().equals("LEA")||pc.getEdition().equals("LEB")) {
            croppedArea = 0.975f;
            minusxy = 0.135f*radius;
        }
        if (image != null) {
            if (image == ImageCache.defaultImage || Forge.enableUIMask.equals("Art")) {
                CardImageRenderer.drawCardImage(g, CardView.getCardForUi(pc), false, x, y, w, h, pos, true, true);
            } else {
                if (Forge.enableUIMask.equals("Full")) {
                    if (ImageCache.isBorderlessCardArt(image))
                        g.drawImage(image, x, y, w, h);
                    else {
                        //tint the border
                        g.drawImage(ImageCache.getBorderImage(image.toString()), ImageCache.borderColor(image), x, y, w, h);
                        g.drawImage(ImageCache.croppedBorderImage(image), x + radius / 2.4f-minusxy, y + radius / 2-minusxy, w * croppedArea, h * croppedArea);
                    }
                } else if (Forge.enableUIMask.equals("Crop")) {
                    g.drawImage(ImageCache.croppedBorderImage(image), x, y, w, h);
                } else
                    g.drawImage(image, x, y, w, h);
            }
            if (pc.isFoil()) { //draw foil effect if needed
                final CardView card = CardView.getCardForUi(pc);
                if (card.getCurrentState().getFoilIndex() == 0) { //if foil finish not yet established, assign a random one
                    card.getCurrentState().setFoilIndexOverride(-1);
                }
                drawFoilEffect(g, card, x, y, w, h, false);
            }
        } else {
            //if card has invalid or no texture due to sudden changes in ImageCache, draw CardImageRenderer instead and wait for it to refresh automatically
            CardImageRenderer.drawCardImage(g, CardView.getCardForUi(pc), false, x, y, w, h, pos, Forge.enableUIMask.equals("Art"), true);
        }
    }
    public static void drawCard(Graphics g, CardView card, float x, float y, float w, float h, CardStackPosition pos, boolean rotate) {
        drawCard(g, card, x, y, w, h, pos, rotate, false, false);
    }
    public static void drawCard(Graphics g, CardView card, float x, float y, float w, float h, CardStackPosition pos, boolean rotate, boolean showAltState, boolean isChoiceList) {
        boolean canshow = MatchController.instance.mayView(card);
        boolean showsleeves = card.isFaceDown() && card.isInZone(EnumSet.of(ZoneType.Exile)); //fix facedown card image ie gonti lord of luxury
        Texture image = new RendererCachedCardImage(card, false).getImage( showAltState ? card.getAlternateState().getImageKey() : card.getCurrentState().getImageKey());
        TextureRegion crack_overlay = FSkin.getCracks().get(card.getCrackOverlayInt());
        FImage sleeves = MatchController.getPlayerSleeve(card.getOwner());
        float radius = (h - w)/8;
        float croppedArea = isModernFrame(card) ? CROP_MULTIPLIER : 0.97f;
        float minusxy = isModernFrame(card) ? 0.0f : 0.13f*radius;
        if (card.getCurrentState().getSetCode().equals("LEA")||card.getCurrentState().getSetCode().equals("LEB")) {
            croppedArea = 0.975f;
            minusxy = 0.135f*radius;
        }
        if (image != null) {
            if (image == ImageCache.defaultImage || Forge.enableUIMask.equals("Art")) {
                float oldAlpha = g.getfloatAlphaComposite();
                if (card.isPhasedOut())
                    g.setAlphaComposite(0.2f);
                CardImageRenderer.drawCardImage(g, card, showAltState, x, y, w, h, pos, true, false, isChoiceList, !showCardIdOverlay(card));
                g.setAlphaComposite(oldAlpha);
            } else if (showsleeves) {
                if (!card.isForeTold())
                    g.drawCardImage(sleeves, crack_overlay, x, y, w, h, card.wasDestroyed(), card.getDamage() > 0);
                else
                    g.drawCardImage(image, crack_overlay, x, y, w, h, card.wasDestroyed(), card.getDamage() > 0);
            } else {
                if(FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_ROTATE_PLANE_OR_PHENOMENON)
                        && (card.getCurrentState().isPhenomenon() || card.getCurrentState().isPlane()) && rotate){
                    if (Forge.enableUIMask.equals("Full")) {
                        if (ImageCache.isBorderlessCardArt(image))
                            g.drawRotatedImage(image, x, y, w, h, x + w / 2, y + h / 2, -90);
                        else {
                            g.drawRotatedImage(FSkin.getBorders().get(0), x, y, w, h, x + w / 2, y + h / 2, -90);
                            g.drawRotatedImage(ImageCache.croppedBorderImage(image), x+radius/2.3f-minusxy, y+radius/2-minusxy, w*croppedArea, h*croppedArea, (x+radius/2.3f-minusxy) + (w*croppedArea) / 2, (y+radius/2-minusxy) + (h*croppedArea) / 2, -90);
                        }
                    } else if (Forge.enableUIMask.equals("Crop")) {
                        g.drawRotatedImage(ImageCache.croppedBorderImage(image),x, y, w, h, x + w / 2, y + h / 2, -90);
                    } else
                        g.drawRotatedImage(image, x, y, w, h, x + w / 2, y + h / 2, -90);
                } else {
                    if (Forge.enableUIMask.equals("Full") && canshow) {
                        if (ImageCache.isBorderlessCardArt(image))
                            g.drawCardImage(image, crack_overlay, x, y, w, h, card.wasDestroyed(), card.getDamage() > 0);
                        else {
                            boolean t = (card.getCurrentState().getOriginalColors() != card.getCurrentState().getColors()) || card.getCurrentState().hasChangeColors();
                            g.drawBorderImage(ImageCache.getBorderImage(image.toString(), canshow), ImageCache.borderColor(image), ImageCache.getTint(card, image), x, y, w, h, t); //tint check for changed colors
                            g.drawCardImage(ImageCache.croppedBorderImage(image), crack_overlay, x + radius / 2.4f-minusxy, y + radius / 2-minusxy, w * croppedArea, h * croppedArea, card.wasDestroyed(), card.getDamage() > 0);
                        }
                    } else if (Forge.enableUIMask.equals("Crop") && canshow) {
                        g.drawCardImage(ImageCache.croppedBorderImage(image), crack_overlay, x, y, w, h, card.wasDestroyed(), card.getDamage() > 0);
                    } else {
                        if (canshow)
                            g.drawCardImage(image, crack_overlay, x, y, w, h, card.wasDestroyed(), card.getDamage() > 0);
                        else // draw card back sleeves
                            g.drawCardImage(sleeves, crack_overlay, x, y, w, h, card.wasDestroyed(), card.getDamage() > 0);
                    }
                }
            }
            drawFoilEffect(g, card, x, y, w, h, false);
        } else {
            //if card has invalid or no texture due to sudden changes in ImageCache, draw CardImageRenderer instead and wait for it to refresh automatically
            float oldAlpha = g.getfloatAlphaComposite();
            if (card.isPhasedOut())
                g.setAlphaComposite(0.2f);
            CardImageRenderer.drawCardImage(g, card, showAltState, x, y, w, h, pos, Forge.enableUIMask.equals("Art"), false, isChoiceList, !showCardIdOverlay(card));
            g.setAlphaComposite(oldAlpha);
        }
    }

    public static void drawCardWithOverlays(Graphics g, CardView card, float x, float y, float w, float h, CardStackPosition pos) {
        drawCardWithOverlays(g, card, x, y, w, h, pos, false, false, false);
    }
    public static void drawCardWithOverlays(Graphics g, CardView card, float x, float y, float w, float h, CardStackPosition pos, boolean stackview, boolean showAltState, boolean isChoiceList) {
        boolean canShow = MatchController.instance.mayView(card);
        float oldAlpha = g.getfloatAlphaComposite();
        boolean unselectable = !MatchController.instance.isSelectable(card) && MatchController.instance.isSelecting();
        float cx, cy, cw, ch;
        cx = x; cy = y; cw = w; ch = h;
        drawCard(g, card, x, y, w, h, pos, false, showAltState, isChoiceList);

        float padding = w * PADDING_MULTIPLIER; //adjust for card border
        x += padding;
        y += padding;
        w -= 2 * padding;
        h -= 2 * padding;

        // TODO: A hacky workaround is currently used to make the game not leak the color information for Morph cards.
        final CardStateView details = showAltState ? card.getAlternateState() : isChoiceList && card.isSplitCard() ? card.getLeftSplitState() : card.getCurrentState();
        final boolean isFaceDown = card.isFaceDown();
        final DetailColors borderColor = isFaceDown ? CardDetailUtil.DetailColors.FACE_DOWN : CardDetailUtil.getBorderColor(details, canShow); // canShow doesn't work here for face down Morphs
        Color color = FSkinColor.fromRGB(borderColor.r, borderColor.g, borderColor.b);
        color = FSkinColor.tintColor(Color.WHITE, color, CardRenderer.PT_BOX_TINT);

        //card name && manacost original position is here moved at the bottom...

        if (stackview)
            return; //override
        if (pos == CardStackPosition.BehindVert) { return; } //remaining rendering not needed if card is behind another card in a vertical stack
        boolean onTop = (pos == CardStackPosition.Top);

        if (canShow && showCardIdOverlay(card)) {
            FSkinFont idFont = FSkinFont.forHeight(h * 0.11f);
            float idHeight = idFont.getCapHeight();
            g.drawOutlinedText(String.valueOf(card.getId()), idFont, Color.WHITE, Color.BLACK, x + padding, y + h - idHeight - padding, w, h, false, Align.left, false);
        }

        if (card.getCounters() != null && !card.getCounters().isEmpty()) {

            switch (CounterDisplayType.from(FModel.getPreferences().getPref(FPref.UI_CARD_COUNTER_DISPLAY_TYPE))) {
                case OLD_WHEN_SMALL:
                case TEXT:
                    drawCounterTabs(card, g, x, y, w, h);
                    break;
                case IMAGE:
                    drawCounterImage(card, g, x, y, w, h);
                    break;
                case HYBRID:
                    drawCounterImage(card, g, x, y, w, h);
                    drawCounterTabs(card, g, x, y, w, h);
                    break;
            }

        }

        if (card.getCurrentRoom() != null && !card.getCurrentRoom().isEmpty()) {
            List<String> markers = new ArrayList<>();
            markers.add("In Room:");
            markers.add(card.getCurrentRoom());
            drawMarkersTabs(markers, g, x, y, w, h);
        }

        float otherSymbolsSize = w / 4f;
        final float combatXSymbols = (x + (w / 4)) - otherSymbolsSize / 2 - 10;
        final float stateXSymbols = (x + (w / 2)) - otherSymbolsSize / 2 - 10;
        final float ySymbols = (y + h) - (h / 12) - otherSymbolsSize / 2;

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
        //Darken unselectable cards
        if (unselectable){
            g.setAlphaComposite(0.6f);
            g.fillRect(Color.BLACK, cx, cy, cw, ch);
            g.setAlphaComposite(oldAlpha);
        }
        //Magenta outline when card is chosen
        if (MatchController.instance.isUsedToPay(card)){
            g.drawRect(BORDER_THICKNESS, Color.MAGENTA, cx, cy, cw, ch);
        }
        //Ability Icons
        boolean onbattlefield = ZoneType.Battlefield.equals(card.getZone());
        if (unselectable){ g.setAlphaComposite(0.6f); }
        if (onbattlefield && onTop && showAbilityIcons(card)) {
            drawAbilityIcons(g,card, cx, cy, cw, cx + ((cw*2)/2.3f), cy, cw / 5.5f, cw / 5.7f);
        } else if (canShow && !onbattlefield && showAbilityIcons(card)) {
            //draw indicator for flash or can be cast at instant speed, enabled if show ability icons is enabled
            String keywordKey = card.getCurrentState().getKeywordKey();
            String abilityText = card.getCurrentState().getAbilityText();
            if ((keywordKey.indexOf("Flash") != -1)
                    || ((abilityText.indexOf("May be played by") != -1)
                    && (abilityText.indexOf("and as though it has flash") != -1))){
                if (keywordKey.indexOf("Flashback") == -1)
                    CardFaceSymbols.drawSymbol("flash", g, cx + ((cw*2)/2.3f), cy, cw / 5.5f, cw / 5.5f);
            }
        }
        //draw name and mana cost overlays if card is small or default card image being used
        if (h <= NAME_COST_THRESHOLD && canShow) {
            if (showCardNameOverlay(card)) {
                float multiplier;
                switch (Forge.extrawide) {
                    case "default":
                        multiplier = 0.145f; //good for tablets with 16:10 or similar
                        break;
                    case "wide":
                        multiplier = 0.150f;
                        break;
                    case "extrawide":
                        multiplier = 0.155f; //good for tall phones with 21:9 or similar
                        break;
                    default:
                        multiplier = 0.150f;
                        break;
                }
                g.drawOutlinedText(CardTranslation.getTranslatedName(details.getName()), FSkinFont.forHeight(h * multiplier), Color.WHITE, Color.BLACK, x + padding -1f, y + padding, w - 2 * padding, h * 0.4f, true, Align.left, false, true);
            }
            if (showCardManaCostOverlay(card)) {
                float manaSymbolSize = w / 4.5f;
                if (card.isSplitCard() && card.hasAlternateState()) {
                    if (!card.isFaceDown()) { // no need to draw mana symbols on face down split cards (e.g. manifested)
                        if (isChoiceList) {
                            if (card.getRightSplitState().getName().equals(details.getName()))
                                drawManaCost(g, card.getRightSplitState().getManaCost(), x - padding, y, w + 2 * padding, h, manaSymbolSize);
                            else
                                drawManaCost(g, card.getLeftSplitState().getManaCost(), x - padding, y, w + 2 * padding, h, manaSymbolSize);
                        } else {
                            drawManaCost(g, card.getCurrentState().getManaCost(), x - padding, y, w + 2 * padding, h, manaSymbolSize);
                        }
                    }
                }
                else {
                    drawManaCost(g, showAltState ? card.getAlternateState().getManaCost() : card.getCurrentState().getManaCost(), x - padding, y, w + 2 * padding, h, manaSymbolSize);
                }
            }
        }
        //reset alpha
        g.setAlphaComposite(oldAlpha);
    }

    public static void drawAbilityIcons(Graphics g, CardView card, float cx, float cy, float cw, float abiX, float abiY, float abiScale, float abiSpace) {
        float abiCount = 0;
        if (card.isToken()){
            CardFaceSymbols.drawSymbol("token", g, abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
            abiCount += 1;
        }
        if (card.isCommander()) {
            CardFaceSymbols.drawSymbol("commander", g, abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
            abiCount += 1;
        }
        if (card.getCurrentState().hasFlying()) {
            CardFaceSymbols.drawSymbol("flying", g, abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
            abiCount += 1;
        }
        if (card.getCurrentState().hasHaste()) {
            CardFaceSymbols.drawSymbol("haste", g, abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
            abiCount += 1;
        }
        if (card.getCurrentState().hasDoubleStrike()) {
            CardFaceSymbols.drawSymbol("doublestrike", g, abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
            abiCount += 1;
        }
        else if (card.getCurrentState().hasFirstStrike()) {
            CardFaceSymbols.drawSymbol("firststrike", g, abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
            abiCount += 1;
        }
        if (card.getCurrentState().hasDeathtouch()) {
            if (abiCount > 5 ) { abiY = cy + (abiSpace * (abiCount - 6)); abiX = cx + ((cw*2)/1.92f); }
            CardFaceSymbols.drawSymbol("deathtouch", g, abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
            abiCount += 1;
        }
        if (card.getCurrentState().hasIndestructible()) {
            if (abiCount > 5 ) { abiY = cy + (abiSpace * (abiCount - 6)); abiX = cx + ((cw*2)/1.92f); }
            CardFaceSymbols.drawSymbol("indestructible", g, abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
            abiCount += 1;
        }
        if (card.getCurrentState().hasMenace()) {
            if (abiCount > 5 ) { abiY = cy + (abiSpace * (abiCount - 6)); abiX = cx + ((cw*2)/1.92f); }
            CardFaceSymbols.drawSymbol("menace", g, abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
            abiCount += 1;
        }
        if (card.getCurrentState().hasFear()) {
            if (abiCount > 5 ) { abiY = cy + (abiSpace * (abiCount - 6)); abiX = cx + ((cw*2)/1.92f); }
            CardFaceSymbols.drawSymbol("fear", g, abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
            abiCount += 1;
        }
        if (card.getCurrentState().hasIntimidate()) {
            if (abiCount > 5 ) { abiY = cy + (abiSpace * (abiCount - 6)); abiX = cx + ((cw*2)/1.92f); }
            CardFaceSymbols.drawSymbol("intimidate", g, abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
            abiCount += 1;
        }
        if (card.getCurrentState().hasShadow()) {
            if (abiCount > 5 ) { abiY = cy + (abiSpace * (abiCount - 6)); abiX = cx + ((cw*2)/1.92f); }
            CardFaceSymbols.drawSymbol("shadow", g, abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
            abiCount += 1;
        }
        if (card.getCurrentState().hasHorsemanship()) {
            if (abiCount > 5 ) { abiY = cy + (abiSpace * (abiCount - 6)); abiX = cx + ((cw*2)/1.92f); }
            CardFaceSymbols.drawSymbol("horsemanship", g, abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
            abiCount += 1;
        }
        if (card.getCurrentState().hasLandwalk()) {
            CardFaceSymbols.drawSymbol("landwalk", g, abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
            abiCount += 1;
        }
        if (card.getCurrentState().hasHexproof()) {
            if (abiCount > 5 ) { abiY = cy + (abiSpace * (abiCount - 6)); abiX = cx + ((cw*2)/1.92f); }
            if (!card.getCurrentState().getHexproofKey().isEmpty()){
                String[] splitK = card.getCurrentState().getHexproofKey().split(":");
                List<String> listHK = Arrays.asList(splitK);
                if (listHK.contains("generic")) {
                    CardFaceSymbols.drawSymbol("hexproof", g, abiX, abiY, abiScale, abiScale);
                    abiY += abiSpace;
                    abiCount += 1;
                }
                if (listHK.contains("R")) {
                    CardFaceSymbols.drawSymbol("hexproofR", g, abiX, abiY, abiScale, abiScale);
                    abiY += abiSpace;
                    abiCount += 1;
                }
                if (listHK.contains("B")) {
                    CardFaceSymbols.drawSymbol("hexproofB", g, abiX, abiY, abiScale, abiScale);
                    abiY += abiSpace;
                    abiCount += 1;
                }
                if (listHK.contains("U")) {
                    CardFaceSymbols.drawSymbol("hexproofU", g, abiX, abiY, abiScale, abiScale);
                    abiY += abiSpace;
                    abiCount += 1;
                }
                if (listHK.contains("G")) {
                    CardFaceSymbols.drawSymbol("hexproofG", g, abiX, abiY, abiScale, abiScale);
                    abiY += abiSpace;
                    abiCount += 1;
                }
                if (listHK.contains("W")) {
                    CardFaceSymbols.drawSymbol("hexproofW", g, abiX, abiY, abiScale, abiScale);
                    abiY += abiSpace;
                    abiCount += 1;
                }
                if (listHK.contains("monocolored")) {
                    CardFaceSymbols.drawSymbol("hexproofC", g, abiX, abiY, abiScale, abiScale);
                    abiY += abiSpace;
                    abiCount += 1;
                }
            } else {
                CardFaceSymbols.drawSymbol("hexproof", g, abiX, abiY, abiScale, abiScale);
                abiY += abiSpace;
                abiCount += 1;
            }
        }
        else if (card.getCurrentState().hasShroud()) {
            if (abiCount > 5 ) { abiY = cy + (abiSpace * (abiCount - 6)); abiX = cx + ((cw*2)/1.92f); }
            CardFaceSymbols.drawSymbol("shroud", g, abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
            abiCount += 1;
        }
        if (card.getCurrentState().hasVigilance()) {
            if (abiCount > 5 ) { abiY = cy + (abiSpace * (abiCount - 6)); abiX = cx + ((cw*2)/1.92f); }
            CardFaceSymbols.drawSymbol("vigilance", g, abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
            abiCount += 1;
        }
        if (card.getCurrentState().hasTrample()) {
            if (abiCount > 5 ) { abiY = cy + (abiSpace * (abiCount - 6)); abiX = cx + ((cw*2)/1.92f); }
            CardFaceSymbols.drawSymbol("trample", g, abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
            abiCount += 1;
        }
        if (card.getCurrentState().hasReach()) {
            if (abiCount > 5 ) { abiY = cy + (abiSpace * (abiCount - 6)); abiX = cx + ((cw*2)/1.92f); }
            CardFaceSymbols.drawSymbol("reach", g, abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
            abiCount += 1;
        }
        if (card.getCurrentState().hasLifelink()) {
            if (abiCount > 5 ) { abiY = cy + (abiSpace * (abiCount - 6)); abiX = cx + ((cw*2)/1.92f); }
            CardFaceSymbols.drawSymbol("lifelink", g, abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
            abiCount += 1;
        }
        if (card.getCurrentState().hasDefender()) {
            if (abiCount > 5 ) { abiY = cy + (abiSpace * (abiCount - 6)); abiX = cx + ((cw*2)/1.92f); }
            CardFaceSymbols.drawSymbol("defender", g, abiX, abiY, abiScale, abiScale);
            abiY += abiSpace;
            abiCount += 1;
        }
        //Protection Icons
        if (!card.getCurrentState().getProtectionKey().isEmpty()){
            if (abiCount > 5 ) { abiY = cy + (abiSpace * (abiCount - 6)); abiX = cx + ((cw*2)/1.92f); }
            if (card.getCurrentState().getProtectionKey().contains("everything") || card.getCurrentState().getProtectionKey().contains("allcolors")) {
                CardFaceSymbols.drawSymbol("protectAll", g, abiX, abiY, abiScale, abiScale);
                abiY += abiSpace;
                abiCount += 1;
            }
            else if (card.getCurrentState().getProtectionKey().contains("coloredspells")) {
                CardFaceSymbols.drawSymbol("protectColoredSpells", g, abiX, abiY, abiScale, abiScale);
                abiY += abiSpace;
                abiCount += 1;
            }
            else if (card.getCurrentState().getProtectionKey().equals("R")) {
                CardFaceSymbols.drawSymbol("protectR", g, abiX, abiY, abiScale, abiScale);
                abiY += abiSpace;
                abiCount += 1;
            }
            else if (card.getCurrentState().getProtectionKey().equals("G")) {
                CardFaceSymbols.drawSymbol("protectG", g, abiX, abiY, abiScale, abiScale);
                abiY += abiSpace;
                abiCount += 1;
            }
            else if (card.getCurrentState().getProtectionKey().equals("B")) {
                CardFaceSymbols.drawSymbol("protectB", g, abiX, abiY, abiScale, abiScale);
                abiY += abiSpace;
                abiCount += 1;
            }
            else if (card.getCurrentState().getProtectionKey().equals("U")) {
                CardFaceSymbols.drawSymbol("protectU", g, abiX, abiY, abiScale, abiScale);
                abiY += abiSpace;
                abiCount += 1;
            }
            else if (card.getCurrentState().getProtectionKey().equals("W")) {
                CardFaceSymbols.drawSymbol("protectW", g, abiX, abiY, abiScale, abiScale);
                abiY += abiSpace;
                abiCount += 1;
            }
            else if (card.getCurrentState().getProtectionKey().equals("RG")||card.getCurrentState().getProtectionKey().equals("GR")) {
                CardFaceSymbols.drawSymbol("protectRG", g, abiX, abiY, abiScale, abiScale);
                abiY += abiSpace;
                abiCount += 1;
            }
            else if (card.getCurrentState().getProtectionKey().equals("RB")||card.getCurrentState().getProtectionKey().equals("BR")) {
                CardFaceSymbols.drawSymbol("protectRB", g, abiX, abiY, abiScale, abiScale);
                abiY += abiSpace;
                abiCount += 1;
            }
            else if (card.getCurrentState().getProtectionKey().equals("RU")||card.getCurrentState().getProtectionKey().equals("UR")) {
                CardFaceSymbols.drawSymbol("protectRU", g, abiX, abiY, abiScale, abiScale);
                abiY += abiSpace;
                abiCount += 1;
            }
            else if (card.getCurrentState().getProtectionKey().equals("RW")||card.getCurrentState().getProtectionKey().equals("WR")) {
                CardFaceSymbols.drawSymbol("protectRW", g, abiX, abiY, abiScale, abiScale);
                abiY += abiSpace;
                abiCount += 1;
            }
            else if (card.getCurrentState().getProtectionKey().equals("GB")||card.getCurrentState().getProtectionKey().equals("BG")) {
                CardFaceSymbols.drawSymbol("protectGB", g, abiX, abiY, abiScale, abiScale);
                abiY += abiSpace;
                abiCount += 1;
            }
            else if (card.getCurrentState().getProtectionKey().equals("GU")||card.getCurrentState().getProtectionKey().equals("UG")) {
                CardFaceSymbols.drawSymbol("protectGU", g, abiX, abiY, abiScale, abiScale);
                abiY += abiSpace;
                abiCount += 1;
            }
            else if (card.getCurrentState().getProtectionKey().equals("GW")||card.getCurrentState().getProtectionKey().equals("WG")) {
                CardFaceSymbols.drawSymbol("protectGW", g, abiX, abiY, abiScale, abiScale);
                abiY += abiSpace;
                abiCount += 1;
            }
            else if (card.getCurrentState().getProtectionKey().equals("BU")||card.getCurrentState().getProtectionKey().equals("UB")) {
                CardFaceSymbols.drawSymbol("protectBU", g, abiX, abiY, abiScale, abiScale);
                abiY += abiSpace;
                abiCount += 1;
            }
            else if (card.getCurrentState().getProtectionKey().equals("BW")||card.getCurrentState().getProtectionKey().equals("WB")) {
                CardFaceSymbols.drawSymbol("protectBW", g, abiX, abiY, abiScale, abiScale);
                abiY += abiSpace;
                abiCount += 1;
            }
            else if (card.getCurrentState().getProtectionKey().equals("UW")||card.getCurrentState().getProtectionKey().equals("WU")) {
                CardFaceSymbols.drawSymbol("protectUW", g, abiX, abiY, abiScale, abiScale);
                abiY += abiSpace;
                abiCount += 1;
            }
            else if (card.getCurrentState().getProtectionKey().contains("generic") || card.getCurrentState().getProtectionKey().length() > 2) {
                CardFaceSymbols.drawSymbol("protectGeneric", g, abiX, abiY, abiScale, abiScale);
                abiY += abiSpace;
                abiCount += 1;
            }
        }
    }
    private static void drawCounterTabs(final CardView card, final Graphics g, final float x, final float y, final float w, final float h) {

        int fontSize = Math.max(11, Math.min(22, (int) (h * 0.08)));
        BitmapFont font = counterFonts.get(fontSize);

        final float additionalXOffset = 3f * ((fontSize - 11) / 11f);
        final float variableWidth = ((fontSize - 11) / 11f) * 44f;

        float otherSymbolsSize = w / 3.5f;
        final float ySymbols = (h / 12) - otherSymbolsSize / 2;

        final float counterBoxHeight = 9 + fontSize;
        final float counterBoxBaseWidth = 50 + variableWidth + additionalXOffset * 2;
        final float counterBoxSpacing = -4;

        final float spaceFromTopOfCard = y + h - counterBoxHeight - counterBoxSpacing - otherSymbolsSize + ySymbols;

        int currentCounter = 0;

        if (CounterDisplayType.from(FModel.getPreferences().getPref(FPref.UI_CARD_COUNTER_DISPLAY_TYPE)) == CounterDisplayType.OLD_WHEN_SMALL) {

            int maxCounters = 0;
            for (Integer numberOfCounters : card.getCounters().values()) {
                maxCounters = Math.max(maxCounters, numberOfCounters);
            }

            //if (counterBoxBaseWidth + font.getBounds(String.valueOf(maxCounters)).width > w) {
            if(font != null && !String.valueOf(maxCounters).isEmpty()){
                layout.setText(font, String.valueOf(maxCounters));
                if (counterBoxBaseWidth + layout.width > w) {
                    drawCounterImage(card, g, x, y, w, h);
                    return;
                }
            }
        }

        for (Map.Entry<CounterType, Integer> counterEntry : card.getCounters().entrySet()) {
            final CounterType counter = counterEntry.getKey();
            final int numberOfCounters = counterEntry.getValue();
            //final float counterBoxRealWidth = counterBoxBaseWidth + font.getBounds(String.valueOf(numberOfCounters)).width + 4;
            if(font != null && !String.valueOf(numberOfCounters).isEmpty()){
                layout.setText(font, String.valueOf(numberOfCounters));
                final float counterBoxRealWidth = counterBoxBaseWidth + layout.width + 4;

                final float counterYOffset = spaceFromTopOfCard - (currentCounter++ * (counterBoxHeight + counterBoxSpacing));

                g.fillRect(counterBackgroundColor, x - 3, counterYOffset, counterBoxRealWidth, counterBoxHeight);

                if (!counterColorCache.containsKey(counter)) {
                    counterColorCache.put(counter, new Color(counter.getRed() / 255.0f, counter.getGreen() / 255.0f, counter.getBlue() / 255.0f, 1.0f));
                }

                Color counterColor = counterColorCache.get(counter);

                drawText(g, counter.getCounterOnCardDisplayName(), font, counterColor, x + 2 + additionalXOffset, counterYOffset, counterBoxRealWidth, counterBoxHeight, Align.left);
                drawText(g, String.valueOf(numberOfCounters), font, counterColor, x + counterBoxBaseWidth - 4f - additionalXOffset, counterYOffset, counterBoxRealWidth, counterBoxHeight, Align.left);
            }
        }
    }

    private static final int GL_BLEND = GL20.GL_BLEND;

    private static void drawText(Graphics g, String text, BitmapFont font, Color color, float x, float y, float w, float h, int horizontalAlignment) {

        if (color.a < 1) { //enable blending so alpha colored shapes work properly
            Gdx.gl.glEnable(GL_BLEND);
        }
        if(font != null && !text.isEmpty()) {
            layout.setText(font, text);
            TextBounds textBounds = new TextBounds(layout.width, layout.height);

            float textHeight = textBounds.height;
            if (h > textHeight) {
                y += (h - textHeight) / 2;
            }

            font.setColor(color);
            font.draw(g.getBatch(), text, g.adjustX(x), g.adjustY(y, 0), w, horizontalAlignment, true);

            if (color.a < 1) {
                Gdx.gl.glDisable(GL_BLEND);
            }
        }
    }

    private static void drawCounterImage(final CardView card, final Graphics g, final float x, final float y, final float w, final float h) {

        int number = 0;
        if (card.getCounters() != null) {
            for (final Integer i : card.getCounters().values()) {
                number += i;
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

    }

    private static void drawMarkersTabs(final List<String> markers, final Graphics g, final float x, final float y, final float w, final float h) {

        int fontSize = Math.max(8, Math.min(22, (int) (h * 0.05)));
        BitmapFont font = counterFonts.get(fontSize);

        final float additionalXOffset = 3f * ((fontSize - 8) / 8f);

        float otherSymbolsSize = w / 3.5f;
        final float ySymbols = (h / 12) - otherSymbolsSize / 2;

        final float markerBoxHeight = 9 + fontSize;
        final float markerBoxBaseWidth = 8 + additionalXOffset * 2;
        final float markerBoxSpacing = -4;

        final float spaceFromTopOfCard = y + h - markerBoxHeight - markerBoxSpacing - otherSymbolsSize + ySymbols;

        int markerCounter = markers.size() - 1;

        for (String marker : markers) {
            layout.setText(font, marker);
            final float markerBoxRealWidth = markerBoxBaseWidth + layout.width + 4;

            final float markerYOffset = spaceFromTopOfCard - (markerCounter-- * (markerBoxHeight + markerBoxSpacing));

            g.fillRect(counterBackgroundColor, x - 3, markerYOffset, markerBoxRealWidth, markerBoxHeight);

            Color markerColor = new Color(200.0f / 255.0f, 200.0f / 255.0f, 200.0f / 255.0f, 1.0f);

            drawText(g, marker, font, markerColor, x + 2 + additionalXOffset, markerYOffset, markerBoxRealWidth, markerBoxHeight, Align.left);
        }
    }

    private static void drawPtBox(Graphics g, CardView card, CardStateView details, Color color, float x, float y, float w, float h) {
        //use array of strings to render separately with a tiny amount of space in between
        //instead of using actual spaces which are too wide
        List<String> pieces = new ArrayList<>();
        if (details.isCreature()) {
            pieces.add(String.valueOf(details.getPower()));
            pieces.add("/");
            pieces.add(String.valueOf(details.getToughness()));
        }
        else if (details.getType().hasSubtype("Vehicle")) {
            pieces.add("[");
            pieces.add(String.valueOf(details.getPower()));
            pieces.add("/");
            pieces.add(String.valueOf(details.getToughness()));
            pieces.add("]");
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
        List<Float> pieceWidths = new ArrayList<>();
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
            g.drawOutlinedText(">" + card.getDamage() + "<", font, Color.RED, Color.WHITE, x, y - h + padding, w, h, false, Align.center, true);
        }

        g.fillRect(color, x, y, w, h);
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);

        x += padding;
        for (int i = 0; i < pieces.size(); i++) {
            g.drawText(pieces.get(i), font, Color.BLACK, x, y, w, h, false, Align.left, true);
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

    public static void drawFoilEffect(Graphics g, CardView card, float x, float y, float w, float h, boolean inZoomer) {
        float new_x = x; float new_y = y; float new_w = w; float new_h = h; float radius = (h - w)/8;
        float croppedArea = isModernFrame(card) ? CROP_MULTIPLIER : 0.97f;
        float minusxy = isModernFrame(card) ? 0.0f : 0.13f*radius;
        if (card.getCurrentState().getSetCode().equals("LEA")||card.getCurrentState().getSetCode().equals("LEB")) {
            croppedArea = 0.975f;
            minusxy = 0.135f*radius;
        }
        if (Forge.enableUIMask.equals("Full")) {
            new_x += radius/2.4f-minusxy; new_y += radius/2-minusxy; new_w = w * croppedArea; new_h = h * croppedArea;
        }
        if (isPreferenceEnabled(FPref.UI_OVERLAY_FOIL_EFFECT) && MatchController.instance.mayView(card)) {
            boolean rotateSplit = isPreferenceEnabled(FPref.UI_ROTATE_SPLIT_CARDS) && card.isSplitCard() && inZoomer;
            int foil = card.getCurrentState().getFoilIndex();
            if (foil > 0) {
                CardFaceSymbols.drawOther(g, String.format("foil%02d", foil), new_x, new_y, new_w, new_h, rotateSplit);
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

    public static boolean showAbilityIcons(CardView card) {
        return isShowingOverlays(card) && isPreferenceEnabled(FPref.UI_OVERLAY_ABILITY_ICONS);
    }

    private static boolean showCardIdOverlay(CardView card) {
        return card.getId() > 0 && isShowingOverlays(card) && isPreferenceEnabled(FPref.UI_OVERLAY_CARD_ID);
    }

    //TODO Make FSkinFont accept more than one kind of font and merge this with it
    private static void generateFontForCounters(final int fontSize) {

        FileHandle ttfFile = Gdx.files.absolute(ForgeConstants.COMMON_FONTS_DIR).child("Roboto-Bold.ttf");

        if (!ttfFile.exists()) { return; }

        final FreeTypeFontGenerator generator = new FreeTypeFontGenerator(ttfFile);

        //approximate optimal page size
        int pageSize = 128;

        //only generate images for characters that could be used by Forge
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890/-+:'";

        final PixmapPacker packer = new PixmapPacker(pageSize, pageSize, Pixmap.Format.RGBA8888, 2, false);
        final FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.characters = chars;
        parameter.size = fontSize;
        parameter.packer = packer;
        final FreeTypeFontGenerator.FreeTypeBitmapFontData fontData = generator.generateData(parameter);
        final Array<PixmapPacker.Page> pages = packer.getPages();

        //TODO Cache this
        //finish generating font on UI thread
        FThreads.invokeInEdtNowOrLater(new Runnable() {
            @Override
            public void run() {

                //TextureRegion[] textureRegions = new TextureRegion[pages.size];
                Array<TextureRegion> textureRegions = new Array<>();
                for (int i = 0; i < pages.size; i++) {
                    PixmapPacker.Page p = pages.get(i);
                    Texture texture = new Texture(new PixmapTextureData(p.getPixmap(), p.getPixmap().getFormat(), false, false)) {
                        @Override
                        public void dispose() {
                            super.dispose();
                            getTextureData().consumePixmap().dispose();
                        }
                    };
                    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                    //textureRegions[i] = new TextureRegion(texture);
                    textureRegions.add(new TextureRegion(texture));
                }

                counterFonts.put(fontSize, new BitmapFont(fontData, textureRegions, true));

                generator.dispose();
                packer.dispose();

            }
        });

    }

}
