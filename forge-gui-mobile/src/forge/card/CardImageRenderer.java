package forge.card;

import static forge.assets.FSkin.getDefaultSkinFile;
import static forge.card.CardRenderer.CROP_MULTIPLIER;
import static forge.card.CardRenderer.isModernFrame;

import java.util.ArrayList;
import java.util.List;

import forge.ImageKeys;
import forge.assets.*;
import forge.item.PaperCard;
import forge.util.*;
import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Align;
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
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.screens.match.MatchController;

public class CardImageRenderer {
    private static final float BASE_IMAGE_WIDTH = 360;
    private static final float BASE_IMAGE_HEIGHT = 504;
    private static float MANA_SYMBOL_SIZE, PT_BOX_WIDTH, HEADER_PADDING, BORDER_THICKNESS;
    private static FSkinFont NAME_FONT, TYPE_FONT, TEXT_FONT, PT_FONT;
    private static float prevImageWidth, prevImageHeight;
    private static final float BLACK_BORDER_THICKNESS_RATIO = 0.021f;
    public static final Color[] VEHICLE_PTBOX_COLOR = new Color[] { Color.valueOf("#A36C42") };
    public static final Color[] SPACECRAFT_PTBOX_COLOR = new Color[] { Color.valueOf("#6F6E6E") };

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
        PaperCard paperCard = null;
        try {
            paperCard = ImageUtil.getPaperCardFromImageKey(state.getImageKey());
        } catch (Exception e) {}
        String artist = "WOTC";
        if (paperCard != null && !paperCard.getArtist().isEmpty())
            artist = paperCard.getArtist();
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

        if (isSaga) {
            //draw text box
            Color[] textBoxColors = FSkinColor.tintColors(Color.WHITE, colors, CardRenderer.TEXT_BOX_TINT);
            drawTextBox(g, card, state, textBoxColors, x + artInset, y - artHeight, (w - 2 * artInset) / 2, textBoxHeight + artHeight, onTop, useCardBGTexture, noText, altState, isFaceDown, canShow, isChoiceList);
            y += textBoxHeight;

            //draw type line
            drawTypeLine(g, state, canShow, headerColors, x, y, w, typeBoxHeight, noText, false, false);
            y += typeBoxHeight;
        } else if (isClass) {
            //draw text box
            Color[] textBoxColors = FSkinColor.tintColors(Color.WHITE, colors, CardRenderer.TEXT_BOX_TINT);
            drawTextBox(g, card, state, textBoxColors, x + artInset + (artWidth / 2), y - artHeight, (w - 2 * artInset) / 2, textBoxHeight + artHeight, onTop, useCardBGTexture, noText, altState, isFaceDown, canShow, isChoiceList);
            y += textBoxHeight;

            //draw type line
            drawTypeLine(g, state, canShow, headerColors, x, y, w, typeBoxHeight, noText, false, false);
            y += typeBoxHeight;
        } else if (isDungeon) {
            if (!drawDungeon) {
                //draw textbox
                Color[] textBoxColors = FSkinColor.tintColors(Color.WHITE, colors, CardRenderer.TEXT_BOX_TINT);
                drawTextBox(g, card, state, textBoxColors, x + artInset, y - artHeight, (w - 2 * artInset), textBoxHeight + artHeight, onTop, useCardBGTexture, noText, altState, isFaceDown, canShow, isChoiceList);
                y += textBoxHeight;
            }
            drawTypeLine(g, state, canShow, headerColors, x, y, w, typeBoxHeight, noText, false, false);
            y += typeBoxHeight;
        } else {
            //draw type line
            drawTypeLine(g, state, canShow, headerColors, x, y, w, typeBoxHeight, noText, false, false);
            y += typeBoxHeight;

            //draw text box
            Color[] textBoxColors = FSkinColor.tintColors(Color.WHITE, colors, CardRenderer.TEXT_BOX_TINT);
            drawTextBox(g, card, state, textBoxColors, x + artInset, y, w - 2 * artInset, textBoxHeight, onTop, useCardBGTexture, noText, altState, isFaceDown, canShow, isChoiceList);
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
    private static void drawOutlineColor(Graphics g, ColorSet colors, float x, float y, float w, float h) {
        if (colors == null)
            return;
        switch (colors.countColors()) {
            case 0:
                g.drawRect(BORDER_THICKNESS*2, Color.valueOf("#A0A6A4"), x, y, w, h);
                break;
            case 1:
                if (colors.hasBlack())
                    g.drawRect(BORDER_THICKNESS*2, Color.valueOf("#48494a"), x, y, w, h);
                else if (colors.hasGreen())
                    g.drawRect(BORDER_THICKNESS*2, Color.valueOf("#66cb35"), x, y, w, h);
                else if (colors.hasBlue())
                    g.drawRect(BORDER_THICKNESS*2, Color.valueOf("#62b5f8"), x, y, w, h);
                else if (colors.hasRed())
                    g.drawRect(BORDER_THICKNESS*2, Color.valueOf("#f6532d"), x, y, w, h);
                else if (colors.hasWhite())
                    g.drawRect(BORDER_THICKNESS*2, Color.valueOf("#EEEBE1"), x, y, w, h);
                break;
            default:
                g.drawRect(BORDER_THICKNESS*2, Color.valueOf("#F9E084"), x, y, w, h);
                break;
        }
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
            ManaCost mainManaCost = state.getManaCost();
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
    }

    private static void drawArt(CardView cv, Graphics g, float x, float y, float w, float h, boolean altState, boolean isFaceDown) {
        boolean useStretchedArt = cv.getCurrentState().getType().hasSubtype("Saga")
                || cv.getCurrentState().getType().hasSubtype("Class")
                || cv.getCurrentState().getType().hasSubtype("Case")
                || cv.getCurrentState().getType().isDungeon();
        ColorSet colorSet = cv.getCurrentState().getColors();
        if (altState && cv.hasAlternateState()) {
            useStretchedArt = cv.getAlternateState().getType().hasSubtype("Saga")
                    || cv.getAlternateState().getType().hasSubtype("Class")
                    || cv.getAlternateState().getType().hasSubtype("Case")
                    || cv.getAlternateState().getType().isDungeon();
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
                g.drawImage(stretchedArt, x, y, w, h);
            } else {
                g.drawImage(forgeArt, x, y, w, h);
            }
            g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);
            return;
        }
        if (Forge.enableUIMask.equals("Art")) {
            FImageComplex cardArt = CardRenderer.getCardArt(cv);
            FImageComplex altArt = cardArt;
            boolean isHidden = (cv.getCurrentState().getImageKey().equals(ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD))
                    || cv.getCurrentState().getImageKey().equals(ImageKeys.getTokenKey(ImageKeys.FORETELL_IMAGE)));
            if (cardArt != null) {
                if (isHidden && !altState) {
                    if (useStretchedArt) {
                        g.drawImage(stretchedArt, x, y, w, h);
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
                    g.drawImage(stretchedArt, x, y, w, h);
                } else {
                    g.drawImage(forgeArt, x, y, w, h);
                }
            }
        } else {
            if (useStretchedArt) {
                g.drawImage(stretchedArt, x, y, w, h);
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

    private static void drawTypeLine(Graphics g, CardStateView state, boolean canShow, Color[] colors, float x, float y, float w, float h, boolean noText, boolean noRarity, boolean isAdventure) {
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

        //draw square icon for rarity
        if (!noRarity && state != null) {
            float iconSize = h * 0.9f;
            float iconPadding = (h - iconSize) / 2;
            w -= iconSize + iconPadding * 2;
            //g.fillRect(CardRenderer.getRarityColor(state.getRarity()), x + w + iconPadding, y + (h - iconSize) / 2, iconSize, iconSize);
            if (state.getRarity() == null) {
                g.drawImage(FSkinImage.SET_SPECIAL, x + w + iconPadding, y + (h - iconSize) / 2, iconSize, iconSize);
            } else if (state.getRarity() == CardRarity.Special) {
                g.drawImage(FSkinImage.SET_SPECIAL, x + w + iconPadding, y + (h - iconSize) / 2, iconSize, iconSize);
            } else if (state.getRarity() == CardRarity.MythicRare) {
                g.drawImage(FSkinImage.SET_MYTHIC, x + w + iconPadding, y + (h - iconSize) / 2, iconSize, iconSize);
            } else if (state.getRarity() == CardRarity.Rare) {
                g.drawImage(FSkinImage.SET_RARE, x + w + iconPadding, y + (h - iconSize) / 2, iconSize, iconSize);
            } else if (state.getRarity() == CardRarity.Uncommon) {
                g.drawImage(FSkinImage.SET_UNCOMMON, x + w + iconPadding, y + (h - iconSize) / 2, iconSize, iconSize);
            } else {
                g.drawImage(FSkinImage.SET_COMMON, x + w + iconPadding, y + (h - iconSize) / 2, iconSize, iconSize);
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

    private static void drawTextBox(Graphics g, CardView card, CardStateView state, Color[] colors, float x, float y, float w, float h, boolean onTop, boolean useCardBGTexture, boolean noText, boolean altstate, boolean isFacedown, boolean canShow, boolean isChoiceList) {
        if (card.hasSecondaryState()) {
            Color[] altcolors = FSkinColor.tintColors(Color.WHITE, fillColorBackground(g, CardDetailUtil.getBorderColors(card.getState(true), canShow) , x, y, w, h), CardRenderer.NAME_BOX_TINT);
            if ((isFacedown && !altstate) || card.getZone() == ZoneType.Stack || isChoiceList || altstate) {
                setTextBox(g, card, state, colors, x, y, w, h, onTop, useCardBGTexture, noText, 0f, 0f, false, altstate, isFacedown);
            } else {
                //left
                //float headerHeight = Math.max(MANA_SYMBOL_SIZE + 2 * HEADER_PADDING, 2 * TYPE_FONT.getCapHeight()) + 2;
                float typeBoxHeight = 2 * getCapHeight(TYPE_FONT);
                drawHeader(g, card, card.getState(true), altcolors, x, y, w - (w / 2), typeBoxHeight, noText, true);
                drawTypeLine(g, card.getState(true), canShow, altcolors, x, y + typeBoxHeight, w - (w / 2), typeBoxHeight, noText, true, true);
                float mod = (typeBoxHeight + typeBoxHeight);
                setTextBox(g, card, card.getState(true), altcolors, x, y + mod, w - (w / 2), h - mod, onTop, useCardBGTexture, noText, typeBoxHeight, typeBoxHeight, true, altstate, isFacedown);
                //right
                setTextBox(g, card, state, colors, x + w / 2, y, w - (w / 2), h, onTop, useCardBGTexture, noText, 0f, 0f, false, altstate, isFacedown);
            }
        } else {
            setTextBox(g, card, state, colors, x, y, w, h, onTop, useCardBGTexture, noText, 0f, 0f, false, altstate, isFacedown);
        }
    }

    private static void setTextBox(Graphics g, CardView card, CardStateView state, Color[] colors, float x, float y, float w, float h, boolean onTop, boolean useCardBGTexture, boolean noText, float adventureHeaderHeight, float adventureTypeHeight, boolean drawAdventure, boolean altstate, boolean isFaceDown) {
        boolean fakeDuals = false;
        //update land bg colors
        if (state != null && state.isLand()) {
            DetailColors modColors = DetailColors.WHITE;
            if (state.isBasicLand()) {
                if (state.isForest())
                    modColors = DetailColors.GREEN;
                else if (state.isIsland())
                    modColors = DetailColors.BLUE;
                else if (state.isMountain())
                    modColors = DetailColors.RED;
                else if (state.isSwamp())
                    modColors = DetailColors.BLACK;
                else if (state.isPlains())
                    modColors = DetailColors.LAND;
            }
            if (state.origCanProduceColoredMana() == 2) {
                //dual colors
                Color[] colorPairs = new Color[2];
                //init Color
                colorPairs[0] = fromDetailColor(DetailColors.WHITE);
                colorPairs[1] = fromDetailColor(DetailColors.WHITE);
                //override
                if (state.origProduceAnyMana()) {
                    colorPairs[0] = fromDetailColor(DetailColors.MULTICOLOR);
                    colorPairs[1] = fromDetailColor(DetailColors.MULTICOLOR);
                } else {
                    fakeDuals = true;
                    if (state.origProduceManaW() && state.origProduceManaU()) {
                        colorPairs[0] = fromDetailColor(DetailColors.LAND);
                        colorPairs[1] = fromDetailColor(DetailColors.BLUE);
                    } else if (state.origProduceManaW() && state.origProduceManaB()) {
                        colorPairs[0] = fromDetailColor(DetailColors.LAND);
                        colorPairs[1] = fromDetailColor(DetailColors.BLACK);
                    } else if (state.origProduceManaW() && state.origProduceManaR()) {
                        colorPairs[0] = fromDetailColor(DetailColors.LAND);
                        colorPairs[1] = fromDetailColor(DetailColors.RED);
                    } else if (state.origProduceManaW() && state.origProduceManaG()) {
                        colorPairs[0] = fromDetailColor(DetailColors.LAND);
                        colorPairs[1] = fromDetailColor(DetailColors.GREEN);
                    } else if (state.origProduceManaU() && state.origProduceManaB()) {
                        colorPairs[0] = fromDetailColor(DetailColors.BLUE);
                        colorPairs[1] = fromDetailColor(DetailColors.BLACK);
                    } else if (state.origProduceManaU() && state.origProduceManaR()) {
                        colorPairs[0] = fromDetailColor(DetailColors.BLUE);
                        colorPairs[1] = fromDetailColor(DetailColors.RED);
                    } else if (state.origProduceManaU() && state.origProduceManaG()) {
                        colorPairs[0] = fromDetailColor(DetailColors.BLUE);
                        colorPairs[1] = fromDetailColor(DetailColors.GREEN);
                    } else if (state.origProduceManaB() && state.origProduceManaR()) {
                        colorPairs[0] = fromDetailColor(DetailColors.BLACK);
                        colorPairs[1] = fromDetailColor(DetailColors.RED);
                    } else if (state.origProduceManaB() && state.origProduceManaG()) {
                        colorPairs[0] = fromDetailColor(DetailColors.BLACK);
                        colorPairs[1] = fromDetailColor(DetailColors.GREEN);
                    } else if (state.origProduceManaR() && state.origProduceManaG()) {
                        colorPairs[0] = fromDetailColor(DetailColors.RED);
                        colorPairs[1] = fromDetailColor(DetailColors.GREEN);
                    }
                }
                colorPairs = FSkinColor.tintColors(Color.WHITE, colorPairs, 0.3f);
                float oldAlpha = g.getfloatAlphaComposite();
                if (!useCardBGTexture)
                    fillColorBackground(g, colorPairs, x, y, w, h);
                else {
                    g.setAlphaComposite(0.95f);
                    fillColorBackground(g, colorPairs, x, y, w, h);
                    if (fakeDuals && state.countBasicLandTypes() == 2) {
                        g.setAlphaComposite(0.1f);
                        drawAlphaLines(g, x, y, w, h);
                    }
                    g.setAlphaComposite(oldAlpha);
                }
            } else {
                //override bg color
                if (state.origCanProduceColoredMana() > 2 || state.origProduceAnyMana()) {
                    modColors = DetailColors.MULTICOLOR;
                } else if (state.origCanProduceColoredMana() == 1) {
                    if (state.origProduceManaW())
                        modColors = DetailColors.LAND;
                    else if (state.origProduceManaB())
                        modColors = DetailColors.BLACK;
                    else if (state.origProduceManaG())
                        modColors = DetailColors.GREEN;
                    else if (state.origProduceManaR())
                        modColors = DetailColors.RED;
                    else if (state.origProduceManaU())
                        modColors = DetailColors.BLUE;
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
            //draw watermark
            FSkinImage image = null;
            if (state.origCanProduceColoredMana() == 1 && !state.origProduceManaC()) {
                if (state.isPlains())
                    image = FSkinImage.WATERMARK_W;
                else if (state.isIsland())
                    image = FSkinImage.WATERMARK_U;
                else if (state.isSwamp())
                    image = FSkinImage.WATERMARK_B;
                else if (state.isMountain())
                    image = FSkinImage.WATERMARK_R;
                else if (state.isForest())
                    image = FSkinImage.WATERMARK_G;
            } else if (state.origProduceManaC()) {
                image = FSkinImage.WATERMARK_C;
            }
            if (image != null) {
                float iconSize = h * 0.75f;
                g.drawImage(image, x + (w - iconSize) / 2, y + (h - iconSize) / 2, iconSize, iconSize);
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

    private static void drawPtBox(Graphics g, CardStateView state, Color[] colors, float x, float y, float w, float h, boolean noText) {
        List<String> pieces = new ArrayList<>();
        if (state.isCreature()) {
            pieces.add(String.valueOf(state.getPower()));
            pieces.add("/");
            pieces.add(String.valueOf(state.getToughness()));
        } else if (state.isPlaneswalker()) {
            pieces.add(String.valueOf(state.getLoyalty()));
        } else if (state.hasPrintedPT()) {
            pieces.add("[");
            pieces.add(String.valueOf(state.getPower()));
            pieces.add("/");
            pieces.add(String.valueOf(state.getToughness()));
            pieces.add("]");
        } else if (state.isBattle()) {
          pieces.add(String.valueOf(state.getDefense()));
        } else {
            return;
        }

        float padding = Math.round(getCapHeight(PT_FONT) / 4);
        float totalPieceWidth = -padding;
        float[] pieceWidths = new float[pieces.size()];
        for (int i = 0; i < pieces.size(); i++) {
            float pieceWidth = getBoundsWidth(pieces.get(i), PT_FONT) + padding;
            pieceWidths[i] = pieceWidth;
            totalPieceWidth += pieceWidth;
        }
        float boxHeight = getCapHeight(PT_FONT) + getAscent(PT_FONT) + 3 * padding;

        float boxWidth = Math.max(PT_BOX_WIDTH, totalPieceWidth + 2 * padding);
        x += w - boxWidth;
        y += h - boxHeight;
        w = boxWidth;
        h = boxHeight;

        fillColorBackground(g, state.isVehicle() ? VEHICLE_PTBOX_COLOR : state.isSpaceCraft() ? SPACECRAFT_PTBOX_COLOR : colors, x, y, w, h);
        //draw outline color here
        drawOutlineColor(g, state.getColors(), x, y, w, h);
        g.drawRect(BORDER_THICKNESS, Color.BLACK, x, y, w, h);

        if (noText)
            return;
        x += (boxWidth - totalPieceWidth) / 2;
        for (int i = 0; i < pieces.size(); i++) {
            g.drawText(pieces.get(i), PT_FONT, state.isVehicle() || state.isSpaceCraft() ? Color.WHITE : Color.BLACK, x, y, w, h, false, Align.left, true);
            x += pieceWidths[i];
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
    public static void drawZoom(Graphics g, CardView card, GameView gameView, boolean altState, float x, float y, float w, float h, float dispW, float dispH, boolean isCurrentCard) {
        drawZoom(g, card, gameView, altState, x, y, w, h, dispW, dispH, isCurrentCard, 1f);
    }
    public static void drawZoom(Graphics g, CardView card, GameView gameView, boolean altState, float x, float y, float w, float h, float dispW, float dispH, boolean isCurrentCard, float modR) {
        boolean canshow = MatchController.instance.mayView(card);
        String key = card.getState(altState).getImageKey();
        Texture image = new CachedCardImageRenderer(key).getImage();

        FImage sleeves = MatchController.getPlayerSleeve(card.getOwner());
        if (image == null) { //draw details if can't draw zoom
            drawDetails(g, card, gameView, altState, x, y, w, h);
            return;
        }
        if(card.isImmutable() && FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_DISABLE_IMAGES_EFFECT_CARDS)){
            drawDetails(g, card, gameView, altState, x, y, w, h);
            return;
        }

        if (image == ImageCache.getInstance().getDefaultImage() || Forge.enableUIMask.equals("Art")) { //support drawing card image manually if card image not found
            drawCardImage(g, card, altState, x, y, w, h, CardStackPosition.Top, true, true);
        } else {
            float radius = (h - w) / 8;
            float wh_Adj = ForgeConstants.isGdxPortLandscape && isCurrentCard ? 1.38f : 1.0f;
            float new_w = w * wh_Adj;
            float new_h = h * wh_Adj;
            float new_x = ForgeConstants.isGdxPortLandscape && isCurrentCard ? (dispW - new_w) / 2 : x;
            float new_y = ForgeConstants.isGdxPortLandscape && isCurrentCard ? (dispH - new_h) / 2 : y;
            float croppedArea = isModernFrame(card) ? CROP_MULTIPLIER : 0.97f;
            float minusxy = isModernFrame(card) ? 0.0f : 0.13f * radius;
            boolean displayFlipped = card.isFlipped();
            if (card.isFlipCard() && altState) {
                displayFlipped = !displayFlipped;
            }
            
            if (card.getCurrentState().getSetCode().equals("LEA") || card.getCurrentState().getSetCode().equals("LEB")) {
                croppedArea = 0.975f;
                minusxy = 0.135f * radius;
            }
            if (canshow && displayFlipped) {
                if (Forge.enableUIMask.equals("Full")) {
                    if (ImageCache.getInstance().isFullBorder(image))
                        g.drawCardRoundRect(image, new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, 180);
                    else {
                        g.drawRotatedImage(FSkin.getBorders().get(0), new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, 180);
                        g.drawRotatedImage(ImageCache.getInstance().croppedBorderImage(image), new_x + radius / 2 - minusxy, new_y + radius / 2 - minusxy, new_w * croppedArea, new_h * croppedArea, (new_x + radius / 2 - minusxy) + (new_w * croppedArea) / 2, (new_y + radius / 2 - minusxy) + (new_h * croppedArea) / 2, 180);
                        if (CardRendererUtils.drawFoil(card))
                            g.drawFoil(new_x, new_y, new_w, new_h, modR, false);
                    }
                } else if (Forge.enableUIMask.equals("Crop")) {
                    g.drawRotatedImage(ImageCache.getInstance().croppedBorderImage(image), new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, 180);
                } else {
                    g.drawRotatedImage(image, new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, 180);
                }
            } else if (canshow && CardRendererUtils.needsRotation(ForgePreferences.FPref.UI_ROTATE_PLANE_OR_PHENOMENON, card, altState)) {
                if (Forge.enableUIMask.equals("Full")) {
                    if (ImageCache.getInstance().isFullBorder(image))
                        g.drawCardRoundRect(image, new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, -90);
                    else {
                        g.drawRotatedImage(FSkin.getBorders().get(0), new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, -90);
                        g.drawRotatedImage(ImageCache.getInstance().croppedBorderImage(image), new_x + radius / 2 - minusxy, new_y + radius / 2 - minusxy, new_w * croppedArea, new_h * croppedArea, (new_x + radius / 2 - minusxy) + (new_w * croppedArea) / 2, (new_y + radius / 2 - minusxy) + (new_h * croppedArea) / 2, -90);
                        if (CardRendererUtils.drawFoil(card))
                            g.drawFoil(new_x, new_y, new_w, new_h, modR, true);
                    }
                } else if (Forge.enableUIMask.equals("Crop")) {
                    g.drawRotatedImage(ImageCache.getInstance().croppedBorderImage(image), new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, -90);
                } else
                    g.drawRotatedImage(image, new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, -90);
            } else if (canshow && CardRendererUtils.needsRotation(ForgePreferences.FPref.UI_ROTATE_SPLIT_CARDS, card, altState)) {
                boolean isAftermath = CardRendererUtils.hasAftermath(card);
                if (Forge.enableUIMask.equals("Full")) {
                    if (ImageCache.getInstance().isFullBorder(image))
                        g.drawCardRoundRect(image, new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, isAftermath ? 90 : -90, modR, CardRendererUtils.drawFoil(card));
                    else {
                        g.drawRotatedImage(FSkin.getBorders().get(ImageCache.getInstance().getFSkinBorders(card)), new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, isAftermath ? 90 : -90);
                        g.drawRotatedImage(ImageCache.getInstance().croppedBorderImage(image), new_x + radius / 2 - minusxy, new_y + radius / 2 - minusxy, new_w * croppedArea, new_h * croppedArea, (new_x + radius / 2 - minusxy) + (new_w * croppedArea) / 2, (new_y + radius / 2 - minusxy) + (new_h * croppedArea) / 2, isAftermath ? 90 : -90);
                    }
                } else if (Forge.enableUIMask.equals("Crop")) {
                    g.drawRotatedImage(ImageCache.getInstance().croppedBorderImage(image), new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, isAftermath ? 90 : -90);
                } else
                    g.drawRotatedImage(image, new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, isAftermath ? 90 : -90);
            } else {
                if (card.isFaceDown() && ZoneType.Exile.equals(card.getZone())) {
                    if (card.isForeTold() || altState) {
                        if (CardRendererUtils.needsRotation(ForgePreferences.FPref.UI_ROTATE_SPLIT_CARDS, card, altState) && isCurrentCard) {
                            boolean isAftermath = CardRendererUtils.hasAftermath(card);
                            if (Forge.enableUIMask.equals("Full")) {
                                if (ImageCache.getInstance().isFullBorder(image))
                                    g.drawCardRoundRect(image, new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, isAftermath ? 90 : -90);
                                else {
                                    g.drawRotatedImage(FSkin.getBorders().get(ImageCache.getInstance().getFSkinBorders(card)), new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, isAftermath ? 90 : -90);
                                    g.drawRotatedImage(ImageCache.getInstance().croppedBorderImage(image), new_x + radius / 2 - minusxy, new_y + radius / 2 - minusxy, new_w * croppedArea, new_h * croppedArea, (new_x + radius / 2 - minusxy) + (new_w * croppedArea) / 2, (new_y + radius / 2 - minusxy) + (new_h * croppedArea) / 2, isAftermath ? 90 : -90);
                                }
                            } else if (Forge.enableUIMask.equals("Crop")) {
                                g.drawRotatedImage(ImageCache.getInstance().croppedBorderImage(image), new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, isAftermath ? 90 : -90);
                            } else
                                g.drawRotatedImage(image, new_x, new_y, new_w, new_h, new_x + new_w / 2, new_y + new_h / 2, isAftermath ? 90 : -90);
                        } else {
                            if (Forge.enableUIMask.equals("Full")) {
                                if (ImageCache.getInstance().isFullBorder(image))
                                    g.drawCardRoundRect(image, null, x, y, w, h, false, false, CardRendererUtils.drawFoil(card));
                                else {
                                    g.drawImage(ImageCache.getInstance().getBorderImage(image.toString()), ImageCache.getInstance().borderColor(image), x, y, w, h);
                                    g.drawImage(ImageCache.getInstance().croppedBorderImage(image), x + radius / 2.4f - minusxy, y + radius / 2 - minusxy, w * croppedArea, h * croppedArea);
                                }
                            } else if (Forge.enableUIMask.equals("Crop")) {
                                g.drawImage(ImageCache.getInstance().croppedBorderImage(image), x, y, w, h);
                            } else {
                                g.drawImage(image, x, y, w, h);
                            }
                        }
                    } else {
                        //show sleeves instead
                        g.drawImage(sleeves, x, y, w, h);
                    }
                } else if (Forge.enableUIMask.equals("Full") && canshow) {
                    if (ImageCache.getInstance().isFullBorder(image))
                        g.drawCardRoundRect(image, null, x, y, w, h, false, false, CardRendererUtils.drawFoil(card));
                    else {
                        g.drawImage(ImageCache.getInstance().getBorderImage(image.toString()), ImageCache.getInstance().borderColor(image), x, y, w, h);
                        g.drawImage(ImageCache.getInstance().croppedBorderImage(image), x + radius / 2.4f - minusxy, y + radius / 2 - minusxy, w * croppedArea, h * croppedArea);
                    }
                } else if (Forge.enableUIMask.equals("Crop") && canshow) {
                    g.drawImage(ImageCache.getInstance().croppedBorderImage(image), x, y, w, h);
                } else {
                    if (canshow)
                        g.drawImage(image, x, y, w, h);
                    else // sleeve
                        g.drawImage(sleeves, x, y, w, h);
                }
            }
        }
        if (canshow && !Forge.enableUIMask.equals("Full") && CardRendererUtils.drawFoil(card))
            g.drawFoil(x, y, w, h, 0f, CardRendererUtils.needsRotation(card, altState));
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
