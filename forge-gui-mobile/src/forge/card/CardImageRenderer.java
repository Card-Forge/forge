package forge.card;

import static forge.assets.FSkin.getDefaultSkinFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.github.tommyettinger.textra.TextraLabel;
import forge.ImageKeys;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.adventure.util.Reward;
import forge.adventure.util.RewardActor;
import forge.assets.*;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.util.*;
import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.TimeUtils;
import com.google.common.collect.ImmutableList;

import forge.CachedCardImage;
import forge.Forge;
import forge.Graphics;
import forge.card.CardRenderer.CardStackPosition;
import forge.card.mana.ManaCost;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.zone.ZoneType;
import forge.gui.card.CardDetailUtil;
import forge.gui.card.CardDetailUtil.DetailColors;
import forge.gui.card.CardTextRows;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.screens.match.MatchController;

public class CardImageRenderer {
    private static final float BASE_IMAGE_WIDTH = 360;
    private static final float BASE_IMAGE_HEIGHT = 504;
    private static float MANA_SYMBOL_SIZE, PT_BOX_WIDTH, HEADER_PADDING, BORDER_THICKNESS;
    private static FSkinFont NAME_FONT, TYPE_FONT, TEXT_FONT, PT_FONT, LEVEL_FONT; //LEVEL_FONT: the small "LEVEL" label of a Leveler level
    private static float prevImageWidth, prevImageHeight;
    private static final float BLACK_BORDER_THICKNESS_RATIO = 0.021f;
    public static final Color[] VEHICLE_PTBOX_COLOR = new Color[] { Color.valueOf("#A36C42") };
    public static final Color[] SPACECRAFT_PTBOX_COLOR = new Color[] { Color.valueOf("#6F6E6E") };
    private static final ArrayList<String> ptPieces = new ArrayList<>(8);
    private static final float[] ptWidths = new float[8];
    private static final String[] landTypesStrings = new String[0];
    private static float textBoxInset; //how far the text box is inset from the inner edge of the card
    private static final Color LOYALTY_BOX_COLOR = Color.BLACK;
    private static final List<TextRenderer> rowTextRenderers = new ArrayList<>();
    private static final TextRenderer classHeaderTextRenderer = new TextRenderer(true);

    private static Color fromDetailColor(DetailColors detailColor) {
        return FSkinColor.fromRGB(detailColor.r, detailColor.g, detailColor.b);
    }

    private static float getCapHeight(FSkinFont fSkinFont) {
        if (fSkinFont == null)
            return 0f;
        return fSkinFont.getCapHeight();
    }

    private static float getAscent(FSkinFont fSkinFont) {
        if (fSkinFont == null)
            return 0f;
        return fSkinFont.getAscent();
    }

    private static float getBoundsWidth(String sequence, FSkinFont fSkinFont) {
        if (fSkinFont == null)
            return 0f;
        return fSkinFont.getBounds(sequence).width;
    }

    public static void forceStaticFieldUpdate() {
        //force static fields to be updated the next time a card image is rendered
        prevImageWidth = 0;
        prevImageHeight = 0;
        forgeArt.clear();
        stretchedArt.clear();
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
        LEVEL_FONT = FSkinFont.forHeight(MANA_SYMBOL_SIZE * 0.5f);
        BORDER_THICKNESS = Math.max(1.5f * ratio, 1f); //don't let border go below 1

        prevImageWidth = w;
        prevImageHeight = h;
    }

    public static void drawFaceDownCard(CardView card, Graphics g, float x, float y, float w, float h) {
        // Try to draw the card sleeves first
        FImage sleeves = MatchController.getPlayerSleeve(card.getOwner());
        if (sleeves != null)
            g.drawImage(sleeves, x, y, w, h);
        else
            drawArt(null, g, x, y, w, h, false, true);
    }

    public static void drawCardImage(Graphics g, CardView card, boolean altState, float x, float y, float w, float h, CardStackPosition pos, boolean useCardBGTexture, boolean showArtist) {
        drawCardImage(g, card, altState, x, y, w, h, pos, useCardBGTexture, false, false, showArtist, true);
    }

    public static void drawCardImage(Graphics g, CardView card, boolean altState, float x, float y, float w, float h, CardStackPosition pos, boolean useCardBGTexture, boolean noText, boolean isChoiceList, boolean showArtist) {
        drawCardImage(g, card, altState, x, y, w, h, pos, useCardBGTexture, noText, isChoiceList, showArtist, true);
    }

    public static void drawCardImage(Graphics g, CardView card, boolean altState, float x, float y, float w, float h, CardStackPosition pos, boolean useCardBGTexture, boolean noText, boolean isChoiceList, boolean showArtist, boolean showArtBox) {
        drawCardImage(g, card, altState, x, y, w, h, pos, useCardBGTexture, noText, isChoiceList, showArtist, showArtBox, false);
    }

    public static void drawCardImage(Graphics g, CardView card, boolean altState, float x, float y, float w, float h, CardStackPosition pos, boolean useCardBGTexture, boolean noText, boolean isChoiceList, boolean showArtist, boolean showArtBox, boolean useEditionLabel) {
        updateStaticFields(w, h);

        float blackBorderThickness = w * BLACK_BORDER_THICKNESS_RATIO;
        g.fillRect(Color.BLACK, x, y, w, h);
        x += blackBorderThickness;
        y += blackBorderThickness;
        w -= 2 * blackBorderThickness;
        h -= 2 * blackBorderThickness;

        CardStateView state = altState
            ? card.getAlternateState()
            : isChoiceList && card.isSplitCard() 
                ? card.getLeftSplitState()
                : card.getCurrentState();
        final boolean isFaceDown = card.isFaceDown();
        final boolean canShow = MatchController.instance.mayView(card);
        //override
        if (isFaceDown && altState && card.isSplitCard())
            state = card.getLeftSplitState();
        boolean isSaga = state.getType().hasSubtype("Saga");
        boolean isClass = state.getType().hasSubtype("Class") || state.getType().hasSubtype("Case");
        boolean isDungeon = state.getType().isDungeon();
        boolean drawDungeon = isDungeon && CardRenderer.getCardArt(card) != null;

        if (!canShow) {
            drawFaceDownCard(card, g, x, y, w, h);
            return;
        }

        //determine colors for borders
        final List<DetailColors> borderColors;
        if (isFaceDown) {
            borderColors = !altState
                ? ImmutableList.of(DetailColors.FACE_DOWN)
                : !useCardBGTexture 
                    ? ImmutableList.of(DetailColors.FACE_DOWN)
                    : CardDetailUtil.getBorderColors(state, canShow);
        } else {
            borderColors = CardDetailUtil.getBorderColors(state, canShow);
        }
        Color[] colors = useCardBGTexture && Forge.allowCardBG ? drawCardBackgroundTexture(state, g, borderColors, x, y, w, h) : fillColorBackground(g, borderColors, x, y, w, h);

        float artInset = blackBorderThickness * 0.5f;
        textBoxInset = artInset;
        float outerBorderThickness = 2 * blackBorderThickness - artInset;
        x += outerBorderThickness;
        y += outerBorderThickness;
        w -= 2 * outerBorderThickness;
        float headerHeight = Math.max(MANA_SYMBOL_SIZE + 2 * HEADER_PADDING, 2 * getCapHeight(NAME_FONT)) + 2;

        //draw header containing name and mana cost
        Color[] headerColors = FSkinColor.tintColors(Color.WHITE, colors, CardRenderer.NAME_BOX_TINT);
        drawHeader(g, card, state, headerColors, x, y, w, headerHeight, isFaceDown && !altState, false);

        if (pos == CardStackPosition.BehindVert) {
            return;
        } //remaining rendering not needed if card is behind another card in a vertical stack
        boolean onTop = (pos == CardStackPosition.Top);

        y += headerHeight;

        float artWidth = w - 2 * artInset;
        float artHeight = !showArtBox ? 0f : artWidth / CardRenderer.CARD_ART_RATIO;
        float typeBoxHeight = 2 * getCapHeight(TYPE_FONT);
        float ptBoxHeight = 0;
        float textBoxHeight = h - headerHeight - artHeight - typeBoxHeight - outerBorderThickness - artInset;

        if (state.isCreature() || state.isPlaneswalker() || state.hasPrintedPT() || state.isBattle()) {
            ptBoxHeight = 2 * getCapHeight(PT_FONT);
        }
        //space for artist
        textBoxHeight -= 2 * getCapHeight(PT_FONT);
        // the artist is only needed (and only looked up in the card database) when it is actually shown
        String artist = showArtist ? getArtist(state.getImageKey()) : "WOTC";
        float minTextBoxHeight = 2 * headerHeight;
        if (textBoxHeight < minTextBoxHeight) {
            artHeight -= (minTextBoxHeight - textBoxHeight); //subtract from art height if text box not big enough otherwise
            textBoxHeight = minTextBoxHeight;
            if (artHeight < 0) {
                textBoxHeight += artHeight;
                artHeight = 0;
            }
        }

        //draw art box with Forge icon
        if (artHeight > 0) {
            if (isSaga)
                drawArt(card, g, x + artInset + (artWidth / 2), y, artWidth / 2, artHeight + textBoxHeight, altState, isFaceDown);
            else if (isClass)
                drawArt(card, g, x + artInset, y, artWidth / 2, artHeight + textBoxHeight, altState, isFaceDown);
            else if (isDungeon) {
                if (drawDungeon) {
                    drawArt(card, g, x + artInset, y, artWidth, artHeight + textBoxHeight, altState, isFaceDown);
                    y += textBoxHeight;
                }
            } else
                drawArt(card, g, x + artInset, y, artWidth, artHeight, altState, isFaceDown);
            y += artHeight;
        }

        if (showArtBox) { // if we don't check this the textbox will not expand its layout for Saga, Dungeon and Class card types
            if (isSaga) {
                //draw text box
                Color[] textBoxColors = FSkinColor.tintColors(Color.WHITE, colors, CardRenderer.TEXT_BOX_TINT);
                drawTextBox(g, card, state, textBoxColors, x + artInset, y - artHeight, (w - 2 * artInset) / 2, textBoxHeight + artHeight, onTop, useCardBGTexture, noText, altState, isFaceDown, canShow, isChoiceList, artHeight > 0);
                y += textBoxHeight;

                //draw type line
                drawTypeLine(g, state, canShow, headerColors, x, y, w, typeBoxHeight, noText, false, false, useEditionLabel || !showArtBox);
                y += typeBoxHeight;
            } else if (isClass) {
                //draw text box
                Color[] textBoxColors = FSkinColor.tintColors(Color.WHITE, colors, CardRenderer.TEXT_BOX_TINT);
                drawTextBox(g, card, state, textBoxColors, x + artInset + (artWidth / 2), y - artHeight, (w - 2 * artInset) / 2, textBoxHeight + artHeight, onTop, useCardBGTexture, noText, altState, isFaceDown, canShow, isChoiceList, artHeight > 0);
                y += textBoxHeight;

                //draw type line
                drawTypeLine(g, state, canShow, headerColors, x, y, w, typeBoxHeight, noText, false, false, useEditionLabel || !showArtBox);
                y += typeBoxHeight;
            } else if (isDungeon) {
                if (!drawDungeon) {
                    //draw textbox
                    Color[] textBoxColors = FSkinColor.tintColors(Color.WHITE, colors, CardRenderer.TEXT_BOX_TINT);
                    drawTextBox(g, card, state, textBoxColors, x + artInset, y - artHeight, (w - 2 * artInset), textBoxHeight + artHeight, onTop, useCardBGTexture, noText, altState, isFaceDown, canShow, isChoiceList, artHeight > 0);
                    y += textBoxHeight;
                }
                drawTypeLine(g, state, canShow, headerColors, x, y, w, typeBoxHeight, noText, false, false, useEditionLabel || !showArtBox);
                y += typeBoxHeight;
            } else {
                //draw type line
                drawTypeLine(g, state, canShow, headerColors, x, y, w, typeBoxHeight, noText, false, false, useEditionLabel || !showArtBox);
                y += typeBoxHeight;

                //draw text box
                Color[] textBoxColors = FSkinColor.tintColors(Color.WHITE, colors, CardRenderer.TEXT_BOX_TINT);
                drawTextBox(g, card, state, textBoxColors, x + artInset, y, w - 2 * artInset, textBoxHeight, onTop, useCardBGTexture, noText, altState, isFaceDown, canShow, isChoiceList, artHeight > 0);
                y += textBoxHeight;
            }
        } else {
            //draw type line
            drawTypeLine(g, state, canShow, headerColors, x, y, w, typeBoxHeight, noText, false, false, useEditionLabel || !showArtBox);
            y += typeBoxHeight;

            //draw text box
            Color[] textBoxColors = FSkinColor.tintColors(Color.WHITE, colors, CardRenderer.TEXT_BOX_TINT);
            drawTextBox(g, card, state, textBoxColors, x + artInset, y, w - 2 * artInset, textBoxHeight, onTop, useCardBGTexture, noText, altState, isFaceDown, canShow, isChoiceList, artHeight > 0);
            y += textBoxHeight;
        }

        //draw P/T box
        if (onTop && ptBoxHeight > 0) {
            //only needed if on top since otherwise P/T will be hidden
            Color[] ptColors = FSkinColor.tintColors(Color.WHITE, colors, CardRenderer.PT_BOX_TINT);
            drawPtBox(g, state, ptColors, x, y - 2 * artInset, w, ptBoxHeight, noText);
        }
        //draw artist
        if (showArtist)
            g.drawOutlinedText(artist, TEXT_FONT, Color.WHITE, Color.DARK_GRAY, x + (getCapHeight(TYPE_FONT) / 2), y + (getCapHeight(TYPE_FONT) / 2), w, h, false, Align.left, false);
    }
    // drawCardImage() ran a carddb search for every card on every frame just to find the artist's name, so cache it
    private static final ConcurrentHashMap<String, String> artistByImageKey = new ConcurrentHashMap<>();

    private static String getArtist(String imageKey) {
        if (imageKey == null) {
            return "WOTC";
        }
        final String cached = artistByImageKey.get(imageKey);
        if (cached != null) {
            return cached;
        }
        String artist = "WOTC";
        try {
            final PaperCard paperCard = ImageUtil.getPaperCardFromImageKey(imageKey);
            if (paperCard != null && !paperCard.getArtist().isEmpty()) {
                artist = paperCard.getArtist();
            }
        } catch (Exception e) {
            return artist; // not remembered, so a lookup that failed once is tried again
        }
        if (artistByImageKey.size() > 500) {
            artistByImageKey.clear(); // stays bounded
        }
        artistByImageKey.put(imageKey, artist);
        return artist;
    }

    /** The color of the outline that is drawn inside of the black border of a box, or null if the card has none. */
    private static Color getOutlineColor(ColorSet colors) {
        if (colors == null)
            return null;
        switch (colors.countColors()) {
            case 0:
                return Color.valueOf("#A0A6A4");
            case 1:
                if (colors.hasBlack())
                    return Color.valueOf("#48494a");
                else if (colors.hasGreen())
                    return Color.valueOf("#66cb35");
                else if (colors.hasBlue())
                    return Color.valueOf("#62b5f8");
                else if (colors.hasRed())
                    return Color.valueOf("#f6532d");
                else if (colors.hasWhite())
                    return Color.valueOf("#EEEBE1");
                return null;
            default:
                return Color.valueOf("#F9E084");
        }
    }

    private static void drawOutlineColor(Graphics g, ColorSet colors, float x, float y, float w, float h) {
        final Color outline = getOutlineColor(colors);
        if (outline != null)
            g.drawRect(BORDER_THICKNESS*2, outline, x, y, w, h);
    }
    private static void drawHeader(Graphics g, CardView card, CardStateView state, Color[] colors, float x, float y, float w, float h, boolean noText, boolean isAdventure) {
        float oldAlpha = g.getfloatAlphaComposite();
        if (isAdventure)
            g.setAlphaComposite(0.8f);
        fillColorBackground(g, colors, x, y, w, h);
        g.setAlphaComposite(oldAlpha);
        //draw outline color here
        if (state != null)
            drawOutlineColor(g, state.getColors(), x, y, w, h);
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);

        float padding = h / 8;
        float manaCostWidth = 0;
        float manaSymbolSize = isAdventure ? MANA_SYMBOL_SIZE * 0.75f : MANA_SYMBOL_SIZE;
        if (!noText && state != null) {
            //draw mana cost for card
            ManaCost mainManaCost = state.getOriginalManaCost();
            if (card.isSplitCard() && card.getAlternateState() != null && !card.isFaceDown() && card.getZone() != ZoneType.Stack && card.getZone() != ZoneType.Battlefield) {
                //handle rendering both parts of split card
                mainManaCost = card.getLeftSplitState().getManaCost();
                ManaCost otherManaCost = card.getRightSplitState().getManaCost();
                manaCostWidth = CardFaceSymbols.getWidth(otherManaCost, manaSymbolSize) + HEADER_PADDING;
                CardFaceSymbols.drawManaCost(g, otherManaCost, x + w - manaCostWidth, y + (h - manaSymbolSize) / 2, manaSymbolSize);
                //draw "//" between two parts of mana cost
                manaCostWidth += getBoundsWidth("//", NAME_FONT) + HEADER_PADDING;
                g.drawText("//", NAME_FONT, Color.BLACK, x + w - manaCostWidth, y, w, h, false, Align.left, true);
            }
            manaCostWidth += CardFaceSymbols.getWidth(mainManaCost, manaSymbolSize) + HEADER_PADDING;
            CardFaceSymbols.drawManaCost(g, mainManaCost, x + w - manaCostWidth, y + (h - manaSymbolSize) / 2, manaSymbolSize);
        }

        //draw name for card
        x += padding;
        w -= 2 * padding;
        if (!noText && state != null)
            g.drawText(CardTranslation.getTranslatedName(state.getName()), NAME_FONT, Color.BLACK, x, y, w - manaCostWidth - padding, h, false, Align.left, true);
    }

    public static final FBufferedImage forgeArt;
    private static final FBufferedImage stretchedArt;
    private static final FBufferedImage dungeonArt;

    static {
        final float logoWidth = FSkinImage.CARDART.getWidth();
        final float logoHeight = FSkinImage.CARDART.getHeight();
        float h = logoHeight * 1.1f;
        float w = h * CardRenderer.CARD_ART_RATIO;
        forgeArt = new FBufferedImage(w, h) {
            @Override
            protected void draw(Graphics g, float w, float h) {
                g.drawImage(Forge.isMobileAdventureMode ? FSkinTexture.ADV_BG_TEXTURE : FSkinTexture.BG_TEXTURE, 0, 0, w, h);
                g.fillRect(FScreen.getTextureOverlayColor(), 0, 0, w, h);
                g.drawImage(FSkinImage.CARDART, (w - logoWidth) / 2, (h - logoHeight) / 2, logoWidth, logoHeight);
            }
        };
        stretchedArt = new FBufferedImage(w, h) {
            @Override
            protected void draw(Graphics g, float w, float h) {
                g.drawImage(Forge.isMobileAdventureMode ? FSkinTexture.ADV_BG_TEXTURE : FSkinTexture.BG_TEXTURE, 0, 0, w, h);
                g.fillRect(FScreen.getTextureOverlayColor(), 0, 0, w, h);
                int newW = Math.round((h * (logoWidth / logoHeight)) * 1.5f);
                int newH = Math.round(logoHeight / 2);
                g.drawImage(FSkinImage.CARDART, (w - newW) /2, (h - newH) / 2, newW, newH);
            }
        };
        dungeonArt = new FBufferedImage(w, h) {
            @Override
            protected void draw(Graphics g, float w, float h) {
                g.drawImage(Forge.isMobileAdventureMode ? FSkinTexture.ADV_BG_TEXTURE : FSkinTexture.BG_TEXTURE, 0, 0, w, h);
                g.fillRect(FScreen.getTextureOverlayColor(), 0, 0, w, h);
                int newW = Math.round((h * (logoWidth / logoHeight)) * 1.2f);
                int newH = Math.round(logoHeight * 0.8f);
                g.drawImage(FSkinImage.CARDART, (w - newW) /2, (h - newH) / 2, newW, newH);
            }
        };
    }

    private static void drawArt(CardView cv, Graphics g, float x, float y, float w, float h, boolean altState, boolean isFaceDown) {
        boolean isDungeon = cv.getCurrentState().getType().isDungeon();
        boolean useStretchedArt = cv.getCurrentState().getType().hasSubtype("Saga")
                || cv.getCurrentState().getType().hasSubtype("Class")
                || cv.getCurrentState().getType().hasSubtype("Case")
                || isDungeon;
        ColorSet colorSet = cv.getCurrentState().getColors();
        if (altState && cv.hasAlternateState()) {
            isDungeon = cv.getAlternateState().getType().isDungeon();
            useStretchedArt = cv.getAlternateState().getType().hasSubtype("Saga")
                    || cv.getAlternateState().getType().hasSubtype("Class")
                    || cv.getAlternateState().getType().hasSubtype("Case")
                    || isDungeon;
            colorSet = cv.getAlternateState().getColors();
        }
        if (cv == null) {
            if (isFaceDown) {
                Texture cardBack = ImageCache.getInstance().getImage(ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD), false);
                if (cardBack != null) {
                    g.drawImage(cardBack, x, y, w, h);
                    return;
                }
            }
            //fallback
            if (useStretchedArt) {
                g.drawImage(isDungeon ? dungeonArt : stretchedArt, x, y, w, h);
            } else {
                g.drawImage(forgeArt, x, y, w, h);
            }
            g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);
            return;
        }
        if (Forge.enableUIMask.equals("Art") || cv.useCardArt()) {
            FImageComplex cardArt = CardRenderer.getCardArt(cv);
            FImageComplex altArt = cardArt;
            boolean isHidden = (cv.getCurrentState().getImageKey().equals(ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD))
                    || cv.getCurrentState().getImageKey().equals(ImageKeys.getTokenKey(ImageKeys.FORETELL_IMAGE)));
            if (cardArt != null) {
                if (isHidden && !altState) {
                    if (useStretchedArt) {
                        g.drawImage(isDungeon ? dungeonArt : stretchedArt, x, y, w, h);
                    } else {
                        g.drawImage(forgeArt, x, y, w, h);
                    }
                } else if (cv.getCurrentState().getImageKey().equals(ImageKeys.getTokenKey(ImageKeys.MANIFEST_IMAGE)) && !altState) {
                    altArt = CardRenderer.getAlternateCardArt(ImageKeys.getTokenKey(ImageKeys.MANIFEST_IMAGE), false);
                    g.drawImage(altArt, x, y, w, h);
                } else if (cv.getCurrentState().getImageKey().equals(ImageKeys.getTokenKey(ImageKeys.MORPH_IMAGE)) && !altState) {
                    altArt = CardRenderer.getAlternateCardArt(ImageKeys.getTokenKey(ImageKeys.MORPH_IMAGE), false);
                    g.drawImage(altArt, x, y, w, h);
                } else {
                    if (cv.hasAlternateState()) {
                        if (altState) {
                            if (cv.getAlternateState().isPlaneswalker())
                                altArt = CardRenderer.getAlternateCardArt(cv.getAlternateState().getImageKey(), cv.getAlternateState().isPlaneswalker());
                            else {
                                altArt = CardRenderer.getCardArt(cv.getAlternateState().getImageKey(), cv.isSplitCard(), cv.getAlternateState().isPlane() || cv.getAlternateState().isPhenomenon(), cv.getText().contains("Aftermath"),
                                        cv.getAlternateState().getType().hasSubtype("Saga"), cv.getAlternateState().getType().hasSubtype("Class") || cv.getAlternateState().getType().hasSubtype("Case"), cv.getAlternateState().getType().isDungeon(),
                                        cv.isFlipCard(), cv.getAlternateState().isPlaneswalker(), CardRenderer.isModernFrame(cv), cv.getAlternateState().getType().isBattle());
                            }
                        }
                    }
                    if (cv.isSplitCard()) {
                        drawSplitCard(cv, altArt, g, x, y, w, h, altState, isFaceDown);
                    } else if (cv.isFlipCard()) {
                        drawFlipCard(isFaceDown ? altArt : cardArt, g, x, y, w, h, altState);
                    } else {
                        g.drawImage(altArt, x, y, w, h);
                    }
                }
            } else {
                if (useStretchedArt) {
                    g.drawImage(isDungeon ? dungeonArt : stretchedArt, x, y, w, h);
                } else {
                    g.drawImage(forgeArt, x, y, w, h);
                }
            }
        } else {
            if (useStretchedArt) {
                g.drawImage(isDungeon ? dungeonArt : stretchedArt, x, y, w, h);
            } else {
                g.drawImage(forgeArt, x, y, w, h);
            }
        }
        //draw outline color here
        drawOutlineColor(g, colorSet, x, y, w, h);
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);
    }

    private static void drawSplitCard(CardView card, FImageComplex cardArt, Graphics g, float x, float y, float w, float h, boolean altState, boolean isFaceDown) {
        if (cardArt.getTexture() == forgeArt.getTexture()) {
            g.drawImage(cardArt, x, y, w, h);
            return;
        }
        CardView alt = card.getBackup();
        if (alt == null)
            alt = card.getAlternateState().getCard();
        CardView cv = altState && isFaceDown ? alt : card;
        boolean isAftermath = altState ? cv.getAlternateState().hasAftermath() : cv.getRightSplitState().hasAftermath();
        if (!isAftermath) {
            CardEdition ed = FModel.getMagicDb().getEditions().get(cv.getCurrentState().getSetCode());
            boolean isOldFrame = ed != null && !ed.isModern();
            float modH = isOldFrame ? cardArt.getHeight() / 12f : 0f;
            float modW = !isOldFrame ? cardArt.getWidth() / 12f : 0f;
            float modW2 = !isOldFrame ? cardArt.getWidth() / 6f : 0f;
            float srcY = cardArt.getHeight() * 13f / 354f;
            float srcHeight = cardArt.getHeight() * 190f / 354f;
            float dh = srcHeight * (1 - cardArt.getWidth() / srcHeight / CardRenderer.CARD_ART_RATIO);
            srcHeight -= dh;
            srcY += dh / 2;
            g.drawRotatedImage(cardArt.getTexture(), x, y, h + modH, w / 2, x + w / 2, y + w / 2, cardArt.getRegionX() + (int) modW, (int) srcY, (int) (cardArt.getWidth() - modW2), (int) srcHeight, -90);
            g.drawRotatedImage(cardArt.getTexture(), x, y + w / 2, h + modH, w / 2, x + w / 2, y + w / 2, cardArt.getRegionX() + (int) modW, (int) cardArt.getHeight() - (int) (srcY + srcHeight), (int) (cardArt.getWidth() - modW2), (int) srcHeight, -90);
            g.drawLine(BORDER_THICKNESS, Color.BLACK, x + w / 2, y, x + w / 2, y + h);
        } else {
            FImageComplex secondArt = CardRenderer.getAftermathSecondCardArt(cv.getCurrentState().getImageKey());
            g.drawRotatedImage(cardArt.getTexture(), x, y, w, h / 2, x + w, y + h / 2, cardArt.getRegionX(), cardArt.getRegionY(), (int) cardArt.getWidth(), (int) cardArt.getHeight() / 2, 0);
            g.drawRotatedImage(secondArt.getTexture(), x - h / 2, y + h / 2, h / 2, w, x, y + h / 2, secondArt.getRegionX(), secondArt.getRegionY(), (int) secondArt.getWidth(), (int) secondArt.getHeight(), 90);
            g.drawLine(BORDER_THICKNESS, Color.BLACK, x, y + h / 2, x + w, y + h / 2);
        }
    }

    private static void drawFlipCard(FImageComplex cardArt, Graphics g, float x, float y, float w, float h, boolean altState) {
        if (altState)
            g.drawRotatedImage(cardArt.getTextureRegion(), x, y, w, h, x + w / 2, y + h / 2, 180);
        else
            g.drawImage(cardArt, x, y, w, h);
    }

    private static void drawTypeLine(Graphics g, CardStateView state, boolean canShow, Color[] colors, float x, float y, float w, float h, boolean noText, boolean noRarity, boolean isAdventure, boolean useEditionLabel) {
        float oldAlpha = g.getfloatAlphaComposite();
        if (isAdventure)
            g.setAlphaComposite(0.6f);
        fillColorBackground(g, colors, x, y, w, h);
        g.setAlphaComposite(oldAlpha);
        //draw outline color here
        if (state != null)
            drawOutlineColor(g, state.getColors(), x, y, w, h);
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);

        float padding = h / 8;

        //draw rarity: edition text box (deck-editor style) or classic anvil set icons
        if (!noRarity && state != null) {
            if (useEditionLabel) {
                String set = canShow ? state.getSetCode() : CardEdition.UNKNOWN_CODE;
                CardRarity rarity = canShow ? state.getRarity() : CardRarity.Unknown;
                if (rarity == null)
                    rarity = CardRarity.Unknown;
                if (!StringUtils.isEmpty(set)) {
                    float setWidth = CardRenderer.getSetWidth(TYPE_FONT, set);
                    float setHeight = h - 2 * CardRenderer.SET_BOX_MARGIN;
                    float setX = x + w - setWidth - CardRenderer.SET_BOX_MARGIN;
                    float setY = y + CardRenderer.SET_BOX_MARGIN;
                    CardRenderer.drawSetLabel(g, TYPE_FONT, set, rarity, setX, setY, setWidth, setHeight);
                    w -= setWidth + CardRenderer.SET_BOX_MARGIN;
                }
            } else {
                float iconSize = h * 0.9f;
                float iconPadding = (h - iconSize) / 2;
                w -= iconSize + iconPadding * 2;
                CardRarity rarity = state.getRarity();
                FSkinImage image = rarity == null ? FSkinImage.SET_SPECIAL : switch (rarity) {
                    case Special -> FSkinImage.SET_SPECIAL;
                    case MythicRare -> FSkinImage.SET_MYTHIC;
                    case Rare -> FSkinImage.SET_RARE;
                    case Uncommon -> FSkinImage.SET_UNCOMMON;
                    default -> FSkinImage.SET_COMMON;
                };
                g.drawImage(image, x + w + iconPadding, y + (h - iconSize) / 2, iconSize, iconSize);
            }
        }

        //draw type
        if (noText)
            return;
        x += padding;
        if (state != null)
            g.drawText(CardDetailUtil.formatCardType(state, canShow), TYPE_FONT, Color.BLACK, x, y, w, h, false, Align.left, true);
    }

    //use text renderer to handle mana symbols and reminder text
    private static final TextRenderer cardTextRenderer = new TextRenderer(true);

    private static void drawTextBox(Graphics g, CardView card, CardStateView state, Color[] colors, float x, float y, float w, float h, boolean onTop, boolean useCardBGTexture, boolean noText, boolean altstate, boolean isFacedown, boolean canShow, boolean isChoiceList, boolean isArtVisible) {
        if (card.hasSecondaryState() || card.hasPreparedSpell()) {
            Color[] altcolors = FSkinColor.tintColors(Color.WHITE, fillColorBackground(g, CardDetailUtil.getBorderColors(card.getState(true), canShow) , x, y, w, h), CardRenderer.NAME_BOX_TINT);
            if ((isFacedown && !altstate) || card.getZone() == ZoneType.Stack && !card.hasPreparedSpell() || isChoiceList || altstate) {
                setTextBox(g, card, state, colors, x, y, w, h, onTop, useCardBGTexture, noText, 0f, 0f, false, altstate, isFacedown, isArtVisible);
            } else {
                float leftX = x, rightX = x + w / 2, width = w - (w / 2);
                CardStateView rightState = state, leftState = card.getState(true);
                if (card.hasPreparedSpell()) {
                    leftX = x + w / 2;
                    rightX = x;
                }
                //left
                //float headerHeight = Math.max(MANA_SYMBOL_SIZE + 2 * HEADER_PADDING, 2 * TYPE_FONT.getCapHeight()) + 2;
                float typeBoxHeight = 2 * getCapHeight(TYPE_FONT);
                drawHeader(g, card, leftState, altcolors, leftX, y, width, typeBoxHeight, noText, true);
                drawTypeLine(g, leftState, canShow, altcolors, leftX, y + typeBoxHeight, width, typeBoxHeight, noText, true, true, false);
                float mod = (typeBoxHeight + typeBoxHeight);
                setTextBox(g, card, leftState, altcolors, leftX, y + mod, width, h - mod, onTop, useCardBGTexture, noText, typeBoxHeight, typeBoxHeight, true, altstate, isFacedown, isArtVisible);
                //right
                setTextBox(g, card, rightState, colors, rightX, y, width, h, onTop, useCardBGTexture, noText, 0f, 0f, false, altstate, isFacedown, isArtVisible);
            }
        } else {
            setTextBox(g, card, state, colors, x, y, w, h, onTop, useCardBGTexture, noText, 0f, 0f, false, altstate, isFacedown, isArtVisible);
        }
    }

    private static void setTextBox(Graphics g, CardView card, CardStateView state, Color[] colors, float x, float y, float w, float h, boolean onTop, boolean useCardBGTexture, boolean noText, float adventureHeaderHeight, float adventureTypeHeight, boolean drawAdventure, boolean altstate, boolean isFaceDown, boolean isArtVisible) {
        boolean fakeDuals = false;
        //update land bg colors
        FSkinProp imageProp = null;
        ColorSet origColors = null;
        if (state != null && state.isLand()) {
            origColors = state.origProduceMana();
            DetailColors modColors = DetailColors.LAND;
            long landTypeCount = 0;
            if (state.getType() != null && state.getType().getLandTypes() != null) {
                Set<String> landTypesSet = state.getType().getLandTypes();
                String[] activeArray = landTypesSet.toArray(landTypesStrings);
                int typeCount = landTypesSet.size();
                for (int i = 0; i < typeCount; i++) {
                    String s = activeArray[i];
                    if (s != null && CardType.isABasicLandType(s)) {
                        landTypeCount++;
                    }
                    activeArray[i] = null;
                }
            }
            if (origColors != null && origColors.countColors() == 2) {
                //dual colors
                Color[] colorPairs = new Color[2];
                //override
                if (state.origProduceAnyMana()) {
                    colorPairs[0] = fromDetailColor(DetailColors.MULTICOLOR);
                    colorPairs[1] = fromDetailColor(DetailColors.MULTICOLOR);
                } else {
                    fakeDuals = true;
                    List<DetailColors> detailColors = CardDetailUtil.getBorderColors(origColors);
                    colorPairs[0] = fromDetailColor(detailColors.get(0));
                    colorPairs[1] = fromDetailColor(detailColors.get(1));
                }
                colorPairs = FSkinColor.tintColors(Color.WHITE, colorPairs, 0.3f);
                float oldAlpha = g.getfloatAlphaComposite();
                if (!useCardBGTexture)
                    fillColorBackground(g, colorPairs, x, y, w, h);
                else {
                    g.setAlphaComposite(0.95f);
                    fillColorBackground(g, colorPairs, x, y, w, h);
                    if (fakeDuals && landTypeCount == 2 && isArtVisible) {
                        g.setAlphaComposite(0.1f);
                        drawAlphaLines(g, x, y, w, h);
                    }
                    g.setAlphaComposite(oldAlpha);
                }
            } else {
                //override bg color
                if (state.origProduceAnyMana() || (origColors != null && origColors.countColors() > 2)) {
                    modColors = DetailColors.MULTICOLOR;
                } else if (origColors != null && origColors.countColors() == 1) {
                    modColors = CardDetailUtil.getColor(origColors.iterator().next());
                }
                Color bgColor = fromDetailColor(modColors);
                bgColor = FSkinColor.tintColor(Color.WHITE, bgColor, CardRenderer.NAME_BOX_TINT);
                float oldAlpha = g.getfloatAlphaComposite();
                if (!useCardBGTexture)
                    g.fillRect(bgColor, x, y, w, h);
                else {
                    g.setAlphaComposite(0.95f);
                    g.fillRect(bgColor, x, y, w, h);
                    g.setAlphaComposite(oldAlpha);
                }
            }
        } else {
            float oldAlpha = g.getfloatAlphaComposite();
            if (!useCardBGTexture)
                fillColorBackground(g, colors, x, y, w, h);
            else {
                g.setAlphaComposite(0.95f);
                fillColorBackground(g, colors, x, y, w, h);
                g.setAlphaComposite(oldAlpha);
            }
        }
        //draw outline color here
        if (state != null)
            drawOutlineColor(g, state.getColors(), x, y, w, h);
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);

        if (!onTop) {
            return;
        } //remaining rendering only needed if card on top

        if (state != null && state.isBasicLand()) {
            // set the correct watermark from color
            if (imageProp == null && origColors.countColors() == 1) {
                for (MagicColor.Color c : MagicColor.Color.values()) {
                    String str = c.getBasicLandType();
                    if (str != null && state.getType().hasSubtype(str)) {
                        imageProp = FSkinProp.watermarkFromColor(c);
                    }
                }
            }
            if (imageProp == null)
                imageProp = FSkinProp.IMG_WATERMARK_C;
            //draw watermark
            if (imageProp != null) {
                float iconSize = h * 0.75f;
                g.drawImage(FSkin.getImages().get(imageProp), x + (w - iconSize) / 2, y + (h - iconSize) / 2, iconSize, iconSize);
            }
        } else {
            boolean needTranslation = true;
            String text = "";
            if (card.isToken()) {
                if (card.getCloneOrigin() == null)
                    needTranslation = false;
            }
            if (drawAdventure) {
                // draw left textbox text
                if (noText)
                    return;
                if (card.hasSecondaryState()) {
                    CardView cv = card.getBackup();
                    if (cv == null || isFaceDown)
                        cv = card;
                    CardStateView csv = cv.getState(true);
                    if (csv == null) { // backup may not have adventure state (e.g. clone of adventure)
                        cv = card;
                        csv = cv.getState(true);
                    }
                    text = cv.getText(csv, needTranslation && csv != null ? CardTranslation.getTranslationTexts(csv) : null);

                } else {
                    text = !card.isSplitCard() ?
                            card.getText(state, needTranslation ? state == null ? null : CardTranslation.getTranslationTexts(state) : null) :
                            card.getText(state, needTranslation ? CardTranslation.getTranslationTexts(card.getLeftSplitState(), card.getRightSplitState()) : null);
                }
            } else {
                if (noText)
                    return;
                if (card.hasSecondaryState()) {
                    CardView cv = card.getBackup();
                    if (cv == null || isFaceDown)
                        cv = card;
                    CardStateView csv = cv.getState(false);
                    text = cv.getText(csv, needTranslation ? CardTranslation.getTranslationTexts(csv) : null);

                } else {
                    text = !card.isSplitCard() ?
                            card.getText(state, needTranslation ? state == null ? null : CardTranslation.getTranslationTexts(state) : null) :
                            card.getText(state, needTranslation ? CardTranslation.getTranslationTexts(card.getLeftSplitState(), card.getRightSplitState()) : null);
                }
            }
            if (StringUtils.isEmpty(text)) {
                return;
            }

            // Station levels, loyalty abilities, Saga chapters, Class levels and Leveler ranges get a badge on the left of their text
            if (!drawAdventure && state != null) {
                List<CardTextRows.Row> rows = CardTextRows.parse(state, text);
                if (!rows.isEmpty()) {
                    boolean hasBottomPtBox = state.isCreature() || state.isPlaneswalker() || state.hasPrintedPT() || state.isBattle();
                    drawTextRows(g, state, rows, x, y, w, h, hasBottomPtBox);
                    return;
                }
            }

            float padding = getCapHeight(TEXT_FONT) * 0.75f;
            x += padding;
            y += padding;
            w -= 2 * padding;
            h -= 2 * padding;
            cardTextRenderer.drawText(g, text, TEXT_FONT, Color.BLACK, x, y, w, h, y, h, true, Align.left, true);
        }
    }

    private static void drawAlphaLines(Graphics g, float x, float y, float w, float h) {
        g.drawImage(Forge.getAssets().getTexture(getDefaultSkinFile("overlay_alpha.png")), x, y, w, h);
    }

    private static TextRenderer getRowTextRenderer(int index) {
        while (rowTextRenderers.size() <= index) {
            rowTextRenderers.add(new TextRenderer(true));
        }
        return rowTextRenderers.get(index);
    }

    /** True if the row has a badge box on the left. The level of a Class is not a badge, it is the header of its row. */
    private static boolean hasBadgeBox(CardTextRows.Row row) {
        return row.hasBadge() && row.kind != CardTextRows.Kind.CLASS;
    }

    /** Width of the badge of a row: a circle (Station) and a square (Saga chapter) are as wide as they are high. */
    private static float getBadgeWidth(CardTextRows.Row row, float boxHeight, float boxPadding) {
        float textWidth = 0;
        for (String part : row.badgeParts()) {
            textWidth = Math.max(textWidth, getBoundsWidth(part, PT_FONT));
        }
        textWidth += 2 * boxPadding;
        if (row.isRoundBadge() || row.kind == CardTextRows.Kind.SAGA) {
            return Math.max(boxHeight, textWidth);
        }
        return Math.max(PT_BOX_WIDTH * 0.8f, textWidth);
    }

    /** Height of the badge(s) of a row: a Saga row has a badge for every chapter, stacked with a gap between them. */
    private static float getBadgeHeight(CardTextRows.Row row, float badgeWidth, float boxHeight, float gap) {
        if (row.kind == CardTextRows.Kind.SAGA) {
            final int parts = row.badgeParts().size();
            return parts * badgeWidth + (parts - 1) * gap;
        }
        return row.isRoundBadge() ? badgeWidth : boxHeight;
    }

    /**
     * Draws the badge of a row, vertically centered on centerY: a gray circle with white text like the P/T box of a
     * Spacecraft for a Station level, an independent square for every chapter of a Saga, otherwise a rectangle in
     * the colors of the P/T box of the card.
     */
    private static void drawRowBadge(Graphics g, CardStateView state, CardTextRows.Row row, Color[] colors, float x, float centerY, float width, float boxHeight, float gap) {
        if (row.kind == CardTextRows.Kind.SAGA) {
            final List<String> chapters = row.badgeParts();
            final float top = centerY - getBadgeHeight(row, width, boxHeight, gap) / 2;
            for (int i = 0; i < chapters.size(); i++) {
                drawRowBox(g, state, chapters.get(i), false, colors, Color.BLACK, false, x, top + i * (width + gap) + width / 2, width, width);
            }
        } else if (row.kind == CardTextRows.Kind.STATION) {
            drawRowBox(g, state, row.badge, true, SPACECRAFT_PTBOX_COLOR, Color.WHITE, true, x, centerY, width, width);
        } else {
            drawRowBox(g, state, row.badge, false, colors, Color.BLACK, false, x, centerY, width, boxHeight);
        }
    }

    /**
     * Draws the small header of a Class level, like the header of an Adventure: the cost on the left, the level on
     * the right. It is as wide as the text box and only has a line on top, so it connects to the frame like on the
     * original card.
     */
    private static void drawClassHeader(Graphics g, String cost, String level, Color[] colors, float x, float y, float w, float h) {
        // the gradient does not cover the lines of the frame on both sides
        final float inset = 2 * BORDER_THICKNESS;
        fillColorBackground(g, colors, x + inset, y, w - 2 * inset, h);
        g.drawLine(BORDER_THICKNESS, Color.BLACK, x, y, x + w, y);
        final float pad = h / 4;
        final float levelWidth = getBoundsWidth(level, TYPE_FONT);
        classHeaderTextRenderer.drawText(g, cost, TYPE_FONT, Color.BLACK, x + pad, y, Math.max(w - levelWidth - 3 * pad, 1), h, y, h, false, Align.left, true);
        g.drawText(level, TYPE_FONT, Color.BLACK, x + pad, y, w - 2 * pad, h, false, Align.right, true);
    }

    private static boolean hasLevelerRows(List<CardTextRows.Row> rows) {
        for (CardTextRows.Row row : rows) {
            if (row.kind == CardTextRows.Kind.LEVEL) {
                return true;
            }
        }
        return false;
    }

    /** The colors of the frame of the card, the P/T box and the header boxes are tinted from them. */
    private static Color[] getFrameColors(CardStateView state) {
        final List<DetailColors> borderColors = CardDetailUtil.getBorderColors(state, true);
        final Color[] frameColors = new Color[borderColors.size()];
        for (int i = 0; i < frameColors.length; i++) {
            frameColors[i] = fromDetailColor(borderColors.get(i));
        }
        return frameColors;
    }

    private static Color[] tintFrom(Color base, Color[] frameColors, float tint) {
        return frameColors.length == 0 ? new Color[] { base } : FSkinColor.tintColors(base, frameColors, tint);
    }

    /**
     * Draws the text of a Leveler like the desktop does: a band for the text before the first level and a band for
     * every level, every band darker than the one before. A level has its name and range in a column on the left and
     * its P/T box on the right. The P/T box of the card itself stays at the bottom right of the text box, the last
     * level keeps clear of it.
     */
    private static void drawLevelerRows(Graphics g, CardStateView state, List<CardTextRows.Row> rows, float x, float y, float w, float h, boolean hasBottomPtBox) {
        final float padding = getCapHeight(TEXT_FONT) * 0.75f;
        final float boxPadding = Math.round(getCapHeight(PT_FONT) / 4);
        final float boxHeight = getCapHeight(PT_FONT) + getAscent(PT_FONT) + 3 * boxPadding;
        final float columnWidth = PT_BOX_WIDTH * 0.75f;
        final float labelHeight = 2 * getCapHeight(LEVEL_FONT);
        final float rangeHeight = 2 * getCapHeight(TYPE_FONT);
        final float ptOverlap = PT_BOX_WIDTH - textBoxInset; //how far a P/T box reaches into the text box
        final float ptRight = x + w + textBoxInset; //right side of the card's inner area, where the P/T box of a card ends
        // how far the P/T box of the card reaches up into the text box
        final float cardPtOverlap = Math.max(2 * textBoxInset - 2 * getCapHeight(PT_FONT) + boxHeight, 0);

        // the bands: a LEVEL row for a level, null for the text before the first level
        final List<CardTextRows.Row> bands = new ArrayList<>();
        final List<String> bandTexts = new ArrayList<>();
        final StringBuilder intro = new StringBuilder();
        for (CardTextRows.Row row : rows) {
            if (row.kind == CardTextRows.Kind.LEVEL) {
                if (intro.length() > 0) {
                    bands.add(null);
                    bandTexts.add(intro.toString());
                    intro.setLength(0);
                }
                bands.add(row);
                bandTexts.add(row.text);
            } else {
                if (intro.length() > 0) {
                    intro.append('\n');
                }
                intro.append(row.text);
            }
        }
        if (intro.length() > 0) {
            bands.add(null);
            bandTexts.add(intro.toString());
        }
        final int count = bands.size();

        final Color[] frameColors = getFrameColors(state);
        final Color lighterGray = Color.valueOf("#E0E0E0");
        final Color lightGray = Color.valueOf("#A0A0A0");
        final Color[][] bandColors = {
                null,
                tintFrom(lighterGray, frameColors, CardRenderer.TEXT_BOX_TINT + 0.15f),
                tintFrom(lightGray, frameColors, CardRenderer.TEXT_BOX_TINT + 0.3f)
        };
        final Color[][] ptColors = {
                tintFrom(Color.WHITE, frameColors, CardRenderer.PT_BOX_TINT),
                tintFrom(lighterGray, frameColors, CardRenderer.PT_BOX_TINT + 0.15f),
                tintFrom(lightGray, frameColors, CardRenderer.PT_BOX_TINT + 0.3f)
        };

        // measure the bands
        final int[] shades = new int[count];
        final String[] pts = new String[count];
        final float[] textX = new float[count];
        final float[] textW = new float[count];
        final float[] needed = new float[count];
        float total = 0;
        int levelNo = 0;
        for (int i = 0; i < count; i++) {
            final CardTextRows.Row level = bands.get(i);
            shades[i] = level == null ? 0 : Math.min(++levelNo, 2);
            pts[i] = level != null ? level.pt : null;
            final float left = x + padding + (level != null ? columnWidth : 0);
            float right = x + w - padding;
            if (pts[i] != null || (hasBottomPtBox && i == count - 1)) {
                right -= ptOverlap;
            }
            textX[i] = left;
            textW[i] = Math.max(right - left, 1);
            float content = bandTexts.get(i).isEmpty() ? 0 : getRowTextRenderer(i).getWrappedBounds(bandTexts.get(i), TEXT_FONT, textW[i]).height;
            if (pts[i] != null) {
                content = Math.max(content, boxHeight);
                if (hasBottomPtBox && i == count - 1) {
                    content = Math.max(content, boxHeight + cardPtOverlap + boxPadding);
                }
            }
            if (level != null) {
                content = Math.max(content, labelHeight + rangeHeight);
            }
            needed[i] = content + padding;
            total += needed[i];
        }

        // the bands fill the whole text box
        final float scale = total > h ? h / total : 1;
        final float extra = total > h ? 0 : (h - total) / count;
        final float[] centers = new float[count];
        float top = y;
        for (int i = 0; i < count; i++) {
            final CardTextRows.Row level = bands.get(i);
            final float slot = needed[i] * scale + extra;
            final float centerY = top + slot / 2;
            centers[i] = centerY;
            if (shades[i] > 0) {
                fillColorBackground(g, bandColors[shades[i]], x, top, w, slot);
            }
            if (i > 0) {
                g.drawLine(BORDER_THICKNESS, Color.BLACK, x, top, x + w, top);
            }
            if (level != null) {
                g.drawLine(BORDER_THICKNESS, Color.BLACK, x + columnWidth, top, x + columnWidth, top + slot);
                final float labelTop = centerY - (labelHeight + rangeHeight) / 2;
                g.drawText("LEVEL", LEVEL_FONT, Color.BLACK, x, labelTop, columnWidth, labelHeight, false, Align.center, true);
                g.drawText(level.badge, TYPE_FONT, Color.BLACK, x, labelTop + labelHeight, columnWidth, rangeHeight, false, Align.center, true);
            }
            if (!bandTexts.get(i).isEmpty()) {
                getRowTextRenderer(i).drawText(g, bandTexts.get(i), TEXT_FONT, Color.BLACK, textX[i], top, textW[i], slot, top, slot, true, Align.left, true);
            }
            top += slot;
        }
        // the frame of the text box on top of the bands
        drawOutlineColor(g, state.getColors(), x, y, w, h);
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);
        // the P/T boxes on top of the frame, they stick out of the right side of the text box
        for (int i = 0; i < count; i++) {
            if (pts[i] != null) {
                final float ptWidth = Math.max(PT_BOX_WIDTH, getBoundsWidth(pts[i], PT_FONT) + 2 * boxPadding);
                float ptCenterY = centers[i];
                if (hasBottomPtBox && i == count - 1) {
                    ptCenterY = Math.min(ptCenterY, y + h - cardPtOverlap - boxPadding - boxHeight / 2);
                }
                drawRowBox(g, state, pts[i], false, ptColors[shades[i]], Color.BLACK, false, ptRight - ptWidth, ptCenterY, ptWidth, boxHeight);
            }
        }
    }

    /**
     * Draws the text of a card with rows that have a badge: Station levels, loyalty abilities, Saga chapters,
     * Leveler ranges and Class levels. Every row is centered on its own slot, so the badge on the left, the text
     * and the P/T box on the right of a row are aligned with each other. The badge sticks out of the left side of
     * the text box, the P/T box of a Station level or a Leveler range out of the right side. The level of a Class
     * is not a badge but a small header above the text of its row. The badges have the colors of the P/T box of a
     * card, a Station level the gray of a Spacecraft.
     *
     * @param hasBottomPtBox true if the P/T box of the card itself overlaps the bottom right of the text box
     */
    private static void drawTextRows(Graphics g, CardStateView state, List<CardTextRows.Row> rows, float x, float y, float w, float h, boolean hasBottomPtBox) {
        if (hasLevelerRows(rows)) {
            drawLevelerRows(g, state, rows, x, y, w, h, hasBottomPtBox);
            return;
        }
        final int count = rows.size();
        final float padding = getCapHeight(TEXT_FONT) * 0.75f;
        final float boxPadding = Math.round(getCapHeight(PT_FONT) / 4);
        final float boxHeight = getCapHeight(PT_FONT) + getAscent(PT_FONT) + 3 * boxPadding;
        final float headerHeight = 2 * getCapHeight(TYPE_FONT); // header of a Class level, as high as a type line

        // a small gap above and below the rows, so the mana symbols of the first and the last row stay inside of the text box
        final float verticalGap = padding / 2;
        y += verticalGap;
        h -= 2 * verticalGap;

        // all badges have the same width, so they line up
        float badgeWidth = 0;
        for (CardTextRows.Row row : rows) {
            if (hasBadgeBox(row)) {
                badgeWidth = Math.max(badgeWidth, getBadgeWidth(row, boxHeight, boxPadding));
            }
        }
        final float badgeOverhang = badgeWidth * 0.4f;
        final float badgeLeft = x - badgeOverhang;
        final float ptOverlap = PT_BOX_WIDTH - textBoxInset; //how far a P/T box reaches into the text box
        final float ptRight = x + w + textBoxInset; //right side of the card's inner area, where the P/T box of a card ends

        // the colors of the P/T box of the card, and of its header
        final Color[] frameColors = getFrameColors(state);
        final Color[] badgeColors = frameColors.length == 0 ? new Color[] { Color.WHITE } : FSkinColor.tintColors(Color.WHITE, frameColors, CardRenderer.PT_BOX_TINT);
        final Color[] headerColors = frameColors.length == 0 ? new Color[] { Color.WHITE } : FSkinColor.tintColors(Color.WHITE, frameColors, CardRenderer.NAME_BOX_TINT);

        // measure the rows
        String[][] headers = new String[count][];
        String[] bodies = new String[count];
        float[] textX = new float[count];
        float[] textW = new float[count];
        float[] needed = new float[count];
        final float headerGap = padding / 2; // between the header of a Class level and its text
        final float headerPadding = 2 * padding; // above and below the header and the text of a Class level
        float total;
        FSkinFont rowFont = TEXT_FONT;
        for (float fit = 1; ; fit -= 0.07f) {
            total = 0;
            for (int i = 0; i < count; i++) {
                CardTextRows.Row row = rows.get(i);
                headers[i] = row.classHeader();
                bodies[i] = headers[i] != null ? headers[i][2] : row.text;
                float left = hasBadgeBox(row) ? badgeLeft + badgeWidth + padding : x + padding;
                float right = x + w - padding;
                if (row.pt != null || (hasBottomPtBox && i == count - 1)) {
                    right -= ptOverlap;
                }
                textX[i] = left;
                textW[i] = Math.max(right - left, 1);
                float textHeight = bodies[i].isEmpty() ? 0 : getRowTextRenderer(i).getWrappedBounds(bodies[i], rowFont, textW[i]).height;
                if (headers[i] != null) {
                    needed[i] = headerHeight + (bodies[i].isEmpty() ? 0 : headerGap + textHeight) + headerPadding;
                } else {
                    needed[i] = textHeight + padding;
                    float boxNeeded = hasBadgeBox(row) ? getBadgeHeight(row, badgeWidth, boxHeight, boxPadding) : 0;
                    if (row.pt != null) {
                        boxNeeded = Math.max(boxNeeded, boxHeight);
                    }
                    if (boxNeeded > 0) {
                        needed[i] = Math.max(needed[i], boxNeeded + padding);
                    }
                }
                total += needed[i];
            }
            // the text is drawn in a smaller font if it does not fit
            if (total <= h || fit <= 0.65f) {
                break;
            }
            rowFont = FSkinFont.forHeight(MANA_SYMBOL_SIZE * 0.95f * (fit - 0.07f));
        }

        // spread the space that is left evenly over the rows, or squeeze the rows if there is not enough
        final float scale = total > h ? h / total : 1;
        final float extra = total > h ? 0 : (h - total) / count;
        float rowTop = y;
        for (int i = 0; i < count; i++) {
            CardTextRows.Row row = rows.get(i);
            float slot = needed[i] * scale + extra;
            float centerY = rowTop + slot / 2;
            if (headers[i] != null) {
                // the header on top, the text of the level below it, both centered in the slot
                float top = rowTop + Math.max((slot - (needed[i] - headerPadding)) / 2, 0);
                drawClassHeader(g, headers[i][0], headers[i][1], headerColors, x, top, w, headerHeight);
                if (!bodies[i].isEmpty()) {
                    float bodyTop = top + headerHeight + headerGap;
                    float bodyHeight = Math.max(rowTop + slot - bodyTop, 1);
                    getRowTextRenderer(i).drawText(g, bodies[i], rowFont, Color.BLACK, textX[i], bodyTop, textW[i], bodyHeight, bodyTop, bodyHeight, true, Align.left, false);
                }
                rowTop += slot;
                continue;
            }
            getRowTextRenderer(i).drawText(g, bodies[i], rowFont, Color.BLACK, textX[i], rowTop, textW[i], slot, rowTop, slot, true, Align.left, true);
            if (hasBadgeBox(row)) {
                drawRowBadge(g, state, row, badgeColors, badgeLeft, centerY, badgeWidth, boxHeight, boxPadding);
            }
            if (row.pt != null) {
                float ptWidth = Math.max(PT_BOX_WIDTH, getBoundsWidth(row.pt, PT_FONT) + 2 * boxPadding);
                // like the P/T box of the card: gray for a Spacecraft, the color of the card otherwise
                if (row.isStation) {
                    drawRowBox(g, state, row.pt, false, SPACECRAFT_PTBOX_COLOR, Color.WHITE, true, ptRight - ptWidth, centerY, ptWidth, boxHeight);
                } else {
                    drawRowBox(g, state, row.pt, false, badgeColors, Color.BLACK, false, ptRight - ptWidth, centerY, ptWidth, boxHeight);
                }
            }
            rowTop += slot;
        }
    }

    /**
     * Draws a box with text, vertically centered on centerY: a circle for the threshold of a Station level,
     * otherwise a rectangle like the P/T box of a card.
     *
     * @param outlineWithCardColor the outline has the color of the card inside of the black outline, as on the P/T box of a Spacecraft
     */
    private static void drawRowBox(Graphics g, CardStateView state, String text, boolean circle, Color[] fill, Color textColor,
                                   boolean outlineWithCardColor, float x, float centerY, float w, float h) {
        float y = centerY - h / 2;
        if (circle) {
            final float cx = x + w / 2;
            final float radius = Math.min(w, h) / 2;
            g.fillCircle(fill[0], cx, centerY, radius);
            if (outlineWithCardColor) {
                final Color outline = getOutlineColor(state.getColors());
                if (outline != null) {
                    g.drawCircle(BORDER_THICKNESS * 2, outline, cx, centerY, radius);
                }
            }
            g.drawCircle(BORDER_THICKNESS, Color.BLACK, cx, centerY, radius);
        } else {
            fillColorBackground(g, fill, x, y, w, h);
            if (outlineWithCardColor) {
                drawOutlineColor(g, state.getColors(), x, y, w, h);
            }
            g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);
        }
        g.drawText(text, PT_FONT, textColor, x, y, w, h, false, Align.center, true);
    }

    private static void drawPtBox(Graphics g, CardStateView state, Color[] colors, float x, float y, float w, float h, boolean noText) {
        ptPieces.clear();

        if (state.isCreature()) {
            ptPieces.add(String.valueOf(state.getPower()));
            ptPieces.add("/");
            ptPieces.add(String.valueOf(state.getToughness()));
        } else if (state.isPlaneswalker()) {
            ptPieces.add(String.valueOf(state.getLoyalty()));
        } else if (state.hasPrintedPT()) {
            ptPieces.add("[");
            ptPieces.add(String.valueOf(state.getPower()));
            ptPieces.add("/");
            ptPieces.add(String.valueOf(state.getToughness()));
            ptPieces.add("]");
        } else if (state.isBattle()) {
            ptPieces.add(String.valueOf(state.getDefense()));
        } else {
            return;
        }

        float padding = Math.round(getCapHeight(PT_FONT) / 4);
        float totalPieceWidth = -padding;

        int piecesSize = ptPieces.size();
        for (int i = 0; i < piecesSize; i++) {
            float pieceWidth = getBoundsWidth(ptPieces.get(i), PT_FONT) + padding;
            ptWidths[i] = pieceWidth;
            totalPieceWidth += pieceWidth;
        }
        float boxHeight = getCapHeight(PT_FONT) + getAscent(PT_FONT) + 3 * padding;

        float boxWidth = Math.max(PT_BOX_WIDTH, totalPieceWidth + 2 * padding);
        x += w - boxWidth;
        y += h - boxHeight;
        w = boxWidth;
        h = boxHeight;

        //loyalty and defense boxes are black with white text and outline, like on the desktop
        final boolean isDarkBox = !state.isCreature() && (state.isPlaneswalker() || state.isBattle());
        fillColorBackground(g, state.isVehicle() ? VEHICLE_PTBOX_COLOR : state.isSpaceCraft() ? SPACECRAFT_PTBOX_COLOR : isDarkBox ? new Color[] { LOYALTY_BOX_COLOR } : colors, x, y, w, h);
        if (isDarkBox) {
            g.drawRect(BORDER_THICKNESS, Color.WHITE, x, y, w, h);
        } else {
            //draw outline color here
            drawOutlineColor(g, state.getColors(), x, y, w, h);
            g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);
        }

        if (noText)
            return;
        final Color ptTextColor = state.isVehicle() || state.isSpaceCraft() || isDarkBox ? Color.WHITE : Color.BLACK;
        x += (boxWidth - totalPieceWidth) / 2;
        for (int i = 0; i < piecesSize; i++) {
            g.drawText(ptPieces.get(i), PT_FONT, ptTextColor, x, y, w, h, false, Align.left, true);
            x += ptWidths[i];
        }
    }
    static class CachedCardImageRenderer extends CachedCardImage {

        public CachedCardImageRenderer(String key) {
            super(key);
        }

        @Override
        public void onImageFetched() {
            ImageCache.getInstance().clear();
        }
    }
    private static CardView scrollingCard;
    private static long scrollingSince, scrollingDrawn;

    /** Draws an item description inside the box between the icon and the corner ornaments, scrolling it when it overflows. */
    private static void drawScrollingDescription(Graphics g, TextraLabel label, CardView card, float x, float top, float w, float bottom, float px) {
        boolean current = CardZoom.isCurrent(card);
        float arrowY = bottom + 3 * px;
        label.setAlignment(Align.topLeft);
        label.layout();
        float textHeight = label.getPrefHeight();
        // overflowing text leaves room below it for the more-text arrow
        if (textHeight > bottom - top + 2f)
            bottom -= 6 * px;
        float boxHeight = bottom - top;
        float overflow = textHeight - boxHeight;
        // restart when the card changes or the zoom view was closed and reopened
        if (current && (card != scrollingCard || TimeUtils.timeSinceMillis(scrollingDrawn) > 500)) {
            scrollingCard = card;
            scrollingSince = TimeUtils.millis();
        }
        if (current)
            scrollingDrawn = TimeUtils.millis();
        float elapsed = current ? TimeUtils.timeSinceMillis(scrollingSince) / 1000f : 0;
        float offset = overflow > 2f ? RewardActor.autoScrollOffset(elapsed, overflow, boxHeight) : 0;
        label.setSize(w, textHeight);
        label.setPosition(g.adjustX(x), g.adjustY(top, 0) - textHeight + offset);
        g.startClip(x, top, w, boxHeight);
        label.draw(g.getBatch(), g.getfloatAlphaComposite());
        g.endClip();
        if (overflow > 2f && offset < overflow)
            RewardActor.drawMoreArrow(g.getBatch(), g.adjustX(x + w / 2), g.adjustY(arrowY, 0), px, elapsed, g.getfloatAlphaComposite());
    }

    public static void drawZoom(Graphics g, CardView card, GameView gameView, boolean altState, float x, float y, float w, float h, float dispW, float dispH, boolean isCurrentCard) {
        drawZoom(g, card, gameView, altState, x, y, w, h, dispW, dispH, isCurrentCard, 1f);
    }
    public static void drawZoom(Graphics g, CardView card, GameView gameView, boolean altState, float x, float y, float w, float h, float dispW, float dispH, boolean isCurrentCard, float modR) {
        boolean canshow = MatchController.instance.mayView(card);
        String key = card.getState(altState).getImageKey();
        Texture image = new CachedCardImageRenderer(key).getImage();
        if (image == null) {
            //try if its Reward Actor object
            if (card.getObject() instanceof RewardActor actor) {
                if (Reward.Type.Card == actor.getReward().getType() || Reward.Type.CardPack == actor.getReward().getType()) {
                    image = actor.getImage(Reward.Type.CardPack != actor.getReward().getType());
                } else {
                    updateStaticFields(w, h);
                    //draw cardBack
                    g.drawImage(Config.instance().getItemSprite("CardBack"), x, y, w, h);
                    // Draw Sprite
                    float itemY = y + h / 4f;
                    float itemW = w / 3f;
                    float itemX = x + itemW;
                    boolean center = false;
                    String name = "";
                    switch (actor.getReward().getType()) {
                        case Item -> {
                            name = actor.getReward().getItem().name;
                            g.drawImage(actor.getReward().getItem().sprite(), itemX, itemY, itemW, itemW);
                        }
                        case Life, Shards, Gold -> {
                            name = actor.getReward().getType().toString();
                            center = true;
                            g.drawImage(Config.instance().getItemSprite(actor.getReward().getType().toString()), itemX, itemY, itemW, itemW);
                        }
                    }

                    String header = isCurrentCard ? "[%300]" : "[%240]";
                    float inset = isCurrentCard ? MANA_SYMBOL_SIZE * 2.4f : MANA_SYMBOL_SIZE * 2.3f;

                    // Item Name
                    TextraLabel itemName = Controls.newTextraLabel(header + name);
                    itemName.setWidth(w - (inset * 2));
                    itemName.setAlignment(1);
                    itemName.setPosition(x + inset, y + h / 1.15f);
                    itemName.draw(g.getBatch(), g.getfloatAlphaComposite());

                    // Description
                    TextraLabel itemDescription = Controls.newTextraLabel(header + TextUtil.fastReplace(card.getCurrentState().getOracleText(), "{M}", "[+Shards]").stripTrailing());
                    itemDescription.setWidth(w - (inset * 2));
                    itemDescription.setWrap(true);
                    if (center) {
                        itemDescription.setPosition(x + inset, y + h / 2.5f);
                        itemDescription.setAlignment(1);
                        itemDescription.draw(g.getBatch(), g.getfloatAlphaComposite());
                        return;
                    }
                    drawScrollingDescription(g, itemDescription, card, x + inset, itemY + itemW + h / 64f, w - inset * 2, y + h - h * 8f / 64f, w / 122f);
                    return;
                }
            } else if (card.getObject() instanceof InventoryItem item) {
                image = ImageCache.getInstance().getImage(item);
            }
        }
        FImage sleeves = MatchController.getPlayerSleeve(card.getOwner());
        if (card.isImmutable() && FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_DISABLE_IMAGES_EFFECT_CARDS)){
            drawDetails(g, card, gameView, altState, x, y, w, h);
            return;
        }
        // when image is not available draw the card renders
        if (image == null || image == ImageCache.getInstance().getDefaultImage() || (Forge.enableUIMask.equals("Art") || card.useCardArt())) { //support drawing card image manually if card image not found
            drawCardImage(g, card, altState, x, y, w, h, CardStackPosition.Top, true, true);
        } else {
            float wh_Adj = ForgeConstants.isGdxPortLandscape && isCurrentCard ? 1.38f : 1.0f;
            float new_w = w * wh_Adj;
            float new_h = h * wh_Adj;
            float new_x = ForgeConstants.isGdxPortLandscape && isCurrentCard ? (dispW - new_w) / 2 : x;
            float new_y = ForgeConstants.isGdxPortLandscape && isCurrentCard ? (dispH - new_h) / 2 : y;
            boolean displayFlipped = card.isFlipped();
            if (card.isFlipCard() && altState) {
                displayFlipped = !displayFlipped;
            }

            if (canshow && displayFlipped) {
                if (Forge.enableUIMask.equals("Full")) {
                    g.drawCardRoundRect(image, new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, 180, 1f, CardRendererUtils.getFoilIndex(card));
                } else if (Forge.enableUIMask.equals("Crop")) {
                    g.drawCardRoundRect(ImageCache.getInstance().croppedBorderImage(image), new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, 180, 0f, CardRendererUtils.getFoilIndex(card));
                } else {
                    g.drawCardRoundRect(image, new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, 180, 0f, CardRendererUtils.getFoilIndex(card));
                }
            } else {
                if (card.isFaceDown() && ZoneType.Exile.equals(card.getZone())) {
                    if (card.isForeTold() || altState) {
                        if (Forge.enableUIMask.equals("Full")) {
                            g.drawCardRoundRect(image, null, x, y, w, h, false, false, CardRendererUtils.getFoilIndex(card));
                        } else if (Forge.enableUIMask.equals("Crop")) {
                            g.drawImage(ImageCache.getInstance().croppedBorderImage(image), x, y, w, h, CardRendererUtils.getFoilIndex(card));
                        } else {
                            g.drawImage(image, x, y, w, h, CardRendererUtils.getFoilIndex(card));
                        }
                    } else {
                        //show sleeves instead
                        g.drawImage(sleeves, x, y, w, h);
                    }
                } else if (Forge.enableUIMask.equals("Full") && canshow) {
                    g.drawCardRoundRect(image, null, x, y, w, h, false, false, CardRendererUtils.getFoilIndex(card));
                } else if (Forge.enableUIMask.equals("Crop") && canshow) {
                    g.drawImage(ImageCache.getInstance().croppedBorderImage(image), x, y, w, h, CardRendererUtils.getFoilIndex(card));
                } else {
                    if (canshow)
                        g.drawImage(image, x, y, w, h, CardRendererUtils.getFoilIndex(card));
                    else // sleeve
                        g.drawImage(sleeves, x, y, w, h);
                }
            }
        }
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
        } else {
            borderColors = CardDetailUtil.getBorderColors(state, canShow);
        }
        Color[] colors = Forge.allowCardBG ? drawCardBackgroundTexture(state, g, !altState ? borderColors : CardDetailUtil.getBorderColors(state, canShow), x, y, w, h) : fillColorBackground(g, borderColors, x, y, w, h);

        Color idForeColor = FSkinColor.getHighContrastColor(colors[0]);

        float outerBorderThickness = 2 * blackBorderThickness;
        x += outerBorderThickness;
        y += outerBorderThickness;
        w -= 2 * outerBorderThickness;
        float cardNameBoxHeight = Math.max(MANA_SYMBOL_SIZE + 2 * HEADER_PADDING, 2 * getCapHeight(NAME_FONT)) + 2 * getCapHeight(TYPE_FONT) + 2;

        //draw name/type box
        Color[] nameBoxColors = FSkinColor.tintColors(Color.WHITE, colors, CardRenderer.NAME_BOX_TINT);
        drawDetailsNameBox(g, card, state, canShow, nameBoxColors, x, y, w, cardNameBoxHeight);

        float innerBorderThickness = outerBorderThickness / 2;
        float ptBoxHeight = 2 * getCapHeight(PT_FONT);
        float textBoxHeight = h - cardNameBoxHeight - ptBoxHeight - outerBorderThickness - 3 * innerBorderThickness;

        y += cardNameBoxHeight + innerBorderThickness;
        Color[] textBoxColors = FSkinColor.tintColors(Color.WHITE, colors, CardRenderer.TEXT_BOX_TINT);
        drawDetailsTextBox(g, state, gameView, canShow, textBoxColors, x, y, w, textBoxHeight);

        y += textBoxHeight + innerBorderThickness;
        Color[] ptColors = FSkinColor.tintColors(Color.WHITE, colors, CardRenderer.PT_BOX_TINT);
        drawDetailsIdAndPtBox(g, state, canShow, idForeColor, ptColors, x, y, w, ptBoxHeight);
    }

    public static Color[] fillColorBackground(Graphics g, List<DetailColors> backColors, float x, float y, float w, float h) {
        Color[] colors = new Color[backColors.size()];
        for (int i = 0; i < colors.length; i++) {
            DetailColors dc = backColors.get(i);
            colors[i] = fromDetailColor(dc);
        }
        fillColorBackground(g, colors, x, y, w, h);
        return colors;
    }

    public static Color[] drawCardBackgroundTexture(CardStateView state, Graphics g, List<DetailColors> backColors, float x, float y, float w, float h) {
        boolean isHybrid = state.getManaCost().hasMultiColor();
        boolean isPW = state.isPlaneswalker();
        Color[] colors = new Color[backColors.size()];
        for (int i = 0; i < colors.length; i++) {
            DetailColors dc = backColors.get(i);
            colors[i] = fromDetailColor(dc);
        }
        switch (backColors.size()) {
            case 1:
                if (backColors.get(0) == DetailColors.FACE_DOWN) {
                    g.drawImage(FSkinTexture.CARDBG_C, x, y, w, h);
                } else if (backColors.get(0) == DetailColors.LAND) {
                    g.drawImage(isPW ? FSkinTexture.PWBG_C : FSkinTexture.CARDBG_L, x, y, w, h);
                } else if (backColors.get(0) == DetailColors.MULTICOLOR) {
                    if (state.isVehicle())
                        g.drawImage(FSkinTexture.CARDBG_V, x, y, w, h);
                    else if (state.isEnchantment())
                        g.drawImage(FSkinTexture.NYX_M, x, y, w, h);
                    else if (state.isArtifact() && !isPW)
                        g.drawImage(FSkinTexture.CARDBG_A, x, y, w, h);
                    else
                        g.drawImage(isPW ? FSkinTexture.PWBG_M : FSkinTexture.CARDBG_M, x, y, w, h);
                } else if (backColors.get(0) == DetailColors.COLORLESS) {
                    if (state.isVehicle())
                        g.drawImage(FSkinTexture.CARDBG_V, x, y, w, h);
                    else if (isPW)
                        g.drawImage(FSkinTexture.PWBG_C, x, y, w, h);
                    else if (state.isEnchantment())
                        g.drawImage(FSkinTexture.NYX_C, x, y, w, h);
                    else if (state.isArtifact())
                        g.drawImage(FSkinTexture.CARDBG_A, x, y, w, h);
                    else
                        g.drawImage(FSkinTexture.CARDBG_C, x, y, w, h);
                } else if (backColors.get(0) == DetailColors.GREEN) {
                    if (state.isVehicle())
                        g.drawImage(FSkinTexture.CARDBG_V, x, y, w, h);
                    else if (state.isEnchantment())
                        g.drawImage(FSkinTexture.NYX_G, x, y, w, h);
                    else if (state.isArtifact() && !isPW)
                        g.drawImage(FSkinTexture.CARDBG_A, x, y, w, h);
                    else
                        g.drawImage(isPW ? FSkinTexture.PWBG_G : FSkinTexture.CARDBG_G, x, y, w, h);
                } else if (backColors.get(0) == DetailColors.RED) {
                    if (state.isVehicle())
                        g.drawImage(FSkinTexture.CARDBG_V, x, y, w, h);
                    else if (state.isEnchantment())
                        g.drawImage(FSkinTexture.NYX_R, x, y, w, h);
                    else if (state.isArtifact() && !isPW)
                        g.drawImage(FSkinTexture.CARDBG_A, x, y, w, h);
                    else
                        g.drawImage(isPW ? FSkinTexture.PWBG_R : FSkinTexture.CARDBG_R, x, y, w, h);
                } else if (backColors.get(0) == DetailColors.BLACK) {
                    if (state.isVehicle())
                        g.drawImage(FSkinTexture.CARDBG_V, x, y, w, h);
                    else if (state.isEnchantment())
                        g.drawImage(FSkinTexture.NYX_B, x, y, w, h);
                    else if (state.isArtifact() && !isPW)
                        g.drawImage(FSkinTexture.CARDBG_A, x, y, w, h);
                    else
                        g.drawImage(isPW ? FSkinTexture.PWBG_B : FSkinTexture.CARDBG_B, x, y, w, h);
                } else if (backColors.get(0) == DetailColors.BLUE) {
                    if (state.isVehicle())
                        g.drawImage(FSkinTexture.CARDBG_V, x, y, w, h);
                    else if (state.isEnchantment())
                        g.drawImage(FSkinTexture.NYX_U, x, y, w, h);
                    else if (state.isArtifact() && !isPW)
                        g.drawImage(FSkinTexture.CARDBG_A, x, y, w, h);
                    else
                        g.drawImage(isPW ? FSkinTexture.PWBG_U : FSkinTexture.CARDBG_U, x, y, w, h);
                } else if (backColors.get(0) == DetailColors.WHITE) {
                    if (state.isVehicle())
                        g.drawImage(FSkinTexture.CARDBG_V, x, y, w, h);
                    else if (state.isEnchantment())
                        g.drawImage(FSkinTexture.NYX_W, x, y, w, h);
                    else if (state.isArtifact() && !isPW)
                        g.drawImage(FSkinTexture.CARDBG_A, x, y, w, h);
                    else
                        g.drawImage(isPW ? FSkinTexture.PWBG_W : FSkinTexture.CARDBG_W, x, y, w, h);
                }
                break;
            case 2:
                if (state.isVehicle())
                    g.drawImage(FSkinTexture.CARDBG_V, x, y, w, h);
                else if (state.isEnchantment())
                    g.drawImage(FSkinTexture.NYX_M, x, y, w, h);
                else if (state.isArtifact() && !isPW)
                    g.drawImage(FSkinTexture.CARDBG_A, x, y, w, h);
                else {
                    if (!isHybrid) {
                        g.drawImage(isPW ? FSkinTexture.PWBG_M : FSkinTexture.CARDBG_M, x, y, w, h);
                    } else if (backColors.contains(DetailColors.WHITE) && backColors.contains(DetailColors.BLUE)) {
                        g.drawImage(isPW ? FSkinTexture.PWBG_WU : FSkinTexture.CARDBG_WU, x, y, w, h);
                    } else if (backColors.contains(DetailColors.WHITE) && backColors.contains(DetailColors.BLACK)) {
                        g.drawImage(isPW ? FSkinTexture.PWBG_WB : FSkinTexture.CARDBG_WB, x, y, w, h);
                    } else if (backColors.contains(DetailColors.WHITE) && backColors.contains(DetailColors.RED)) {
                        g.drawImage(isPW ? FSkinTexture.PWBG_WR : FSkinTexture.CARDBG_WR, x, y, w, h);
                    } else if (backColors.contains(DetailColors.WHITE) && backColors.contains(DetailColors.GREEN)) {
                        g.drawImage(isPW ? FSkinTexture.PWBG_WG : FSkinTexture.CARDBG_WG, x, y, w, h);
                    } else if (backColors.contains(DetailColors.BLUE) && backColors.contains(DetailColors.BLACK)) {
                        g.drawImage(isPW ? FSkinTexture.PWBG_UB : FSkinTexture.CARDBG_UB, x, y, w, h);
                    } else if (backColors.contains(DetailColors.BLUE) && backColors.contains(DetailColors.RED)) {
                        g.drawImage(isPW ? FSkinTexture.PWBG_UR : FSkinTexture.CARDBG_UR, x, y, w, h);
                    } else if (backColors.contains(DetailColors.BLUE) && backColors.contains(DetailColors.GREEN)) {
                        g.drawImage(isPW ? FSkinTexture.PWBG_UG : FSkinTexture.CARDBG_UG, x, y, w, h);
                    } else if (backColors.contains(DetailColors.BLACK) && backColors.contains(DetailColors.RED)) {
                        g.drawImage(isPW ? FSkinTexture.PWBG_BR : FSkinTexture.CARDBG_BR, x, y, w, h);
                    } else if (backColors.contains(DetailColors.BLACK) && backColors.contains(DetailColors.GREEN)) {
                        g.drawImage(isPW ? FSkinTexture.PWBG_BG : FSkinTexture.CARDBG_BG, x, y, w, h);
                    } else if (backColors.contains(DetailColors.RED) && backColors.contains(DetailColors.GREEN)) {
                        g.drawImage(isPW ? FSkinTexture.PWBG_RG : FSkinTexture.CARDBG_RG, x, y, w, h);
                    }
                }
                break;
            case 3:
                if (state.isVehicle())
                    g.drawImage(FSkinTexture.CARDBG_V, x, y, w, h);
                else if (state.isEnchantment())
                    g.drawImage(FSkinTexture.NYX_M, x, y, w, h);
                else if (state.isArtifact() && !isPW)
                    g.drawImage(FSkinTexture.CARDBG_A, x, y, w, h);
                else
                    g.drawImage(isPW ? FSkinTexture.PWBG_M : FSkinTexture.CARDBG_M, x, y, w, h);
                break;
            default:
                if (state.isVehicle())
                    g.drawImage(FSkinTexture.CARDBG_V, x, y, w, h);
                else if (state.isEnchantment())
                    g.drawImage(FSkinTexture.NYX_C, x, y, w, h);
                else if (state.isArtifact() && !isPW)
                    g.drawImage(FSkinTexture.CARDBG_A, x, y, w, h);
                else
                    g.drawImage(isPW ? FSkinTexture.PWBG_C : FSkinTexture.CARDBG_C, x, y, w, h);
                break;
        }
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
        h = Math.max(MANA_SYMBOL_SIZE + 2 * HEADER_PADDING, 2 * getCapHeight(NAME_FONT));

        //draw mana cost for card
        float manaCostWidth = 0;
        if (canShow) {
            ManaCost mainManaCost = state.getManaCost();
            if (card.isSplitCard() && card.hasAlternateState() && !card.isFaceDown() && card.getZone() != ZoneType.Stack && card.getZone() != ZoneType.Battlefield) { //only display current state's mana cost when on stack
                //handle rendering both parts of split card
                mainManaCost = card.getLeftSplitState().getManaCost();
                ManaCost otherManaCost = card.getAlternateState().getManaCost();
                manaCostWidth = CardFaceSymbols.getWidth(otherManaCost, MANA_SYMBOL_SIZE) + HEADER_PADDING;
                CardFaceSymbols.drawManaCost(g, otherManaCost, x + w - manaCostWidth, y + (h - MANA_SYMBOL_SIZE) / 2, MANA_SYMBOL_SIZE);
                //draw "//" between two parts of mana cost
                manaCostWidth += getBoundsWidth("//", NAME_FONT) + HEADER_PADDING;
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
        h = 2 * getCapHeight(TYPE_FONT);

        String set = state.getSetCode();
        CardRarity rarity = state.getRarity();
        if (!canShow) {
            set = CardEdition.UNKNOWN_CODE;
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

        float padX = getCapHeight(TEXT_FONT) / 2;
        float padY = padX + Utils.scale(2); //add a little more vertical padding
        x += padX;
        y += padY;
        w -= 2 * padX;
        h -= 2 * padY;
        cardTextRenderer.drawText(g, CardDetailUtil.composeCardText(state, gameView, canShow), TEXT_FONT, Color.BLACK, x, y, w, h, y, h, true, Align.left, false);
    }

    private static void drawDetailsIdAndPtBox(Graphics g, CardStateView state, boolean canShow, Color idForeColor, Color[] colors, float x, float y, float w, float h) {
        float idWidth = 0;
        if (canShow) {
            String idText = CardDetailUtil.formatCardId(state);
            g.drawText(idText, TYPE_FONT, idForeColor, x, y + getCapHeight(TYPE_FONT) / 2, w, h, false, Align.left, false);
            idWidth = getBoundsWidth(idText, TYPE_FONT);
        }

        String ptText = CardDetailUtil.formatPrimaryCharacteristic(state, canShow);
        if (StringUtils.isEmpty(ptText)) {
            return;
        }

        TextBounds bounds = cardTextRenderer.getBounds(ptText, PT_FONT);
        float padding = getCapHeight(PT_FONT) / 2;
        float boxWidth = Math.min(bounds.width + 2 * padding,
                w - idWidth - padding); //prevent box overlapping ID
        x += w - boxWidth;
        w = boxWidth;

        fillColorBackground(g, state.isVehicle() ? VEHICLE_PTBOX_COLOR : state.isSpaceCraft() ? SPACECRAFT_PTBOX_COLOR : colors, x, y, w, h);
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);
        cardTextRenderer.drawText(g, ptText, PT_FONT, state.isVehicle() || state.isSpaceCraft() ? Color.WHITE : Color.BLACK, x, y, w, h, y, h, false, Align.center, true);
    }
}
