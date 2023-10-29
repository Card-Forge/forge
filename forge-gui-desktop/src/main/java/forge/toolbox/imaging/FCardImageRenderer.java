package forge.toolbox.imaging;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import forge.card.CardRarity;
import forge.card.mana.ManaCost;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.gui.GuiBase;
import forge.gui.card.CardDetailUtil;
import forge.gui.card.CardDetailUtil.DetailColors;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.toolbox.CardFaceSymbols;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinIcon;
import forge.util.CardTranslation;

public class FCardImageRenderer {
    private static boolean isInitialed = false;
    private static final float BASE_IMAGE_WIDTH = 488;
    private static final float BASE_IMAGE_HEIGHT = 680;
    private static final float NAME_BOX_TINT = 0.2f;
    private static final float TEXT_BOX_TINT = 0.1f;
    private static final float PT_BOX_TINT = 0.2f;
    private static float CARD_ART_RATIO;
    private static int PT_BOX_WIDTH, HEADER_PADDING, TYPE_PADDING, BLACK_BORDER_THICKNESS, BOX_LINE_THICKNESS;
    private static int ART_INSET, OUTER_BORDER_THICKNESS;
    private static Font NAME_FONT, TYPE_FONT, TEXT_FONT, REMINDER_FONT, PT_FONT, ARTIST_FONT;
    private static int NAME_SIZE, TYPE_SIZE, TEXT_SIZE, REMINDER_SIZE, PT_SIZE, ARTIST_SIZE;
    private static Map<Font, Font[]> cachedFonts;
    private static Color TEXT_COLOR;

    private static BreakIterator boundary;
    private static Pattern linebreakPattern;
    private static Pattern reminderPattern;
    private static Pattern reminderHidePattern;
    private static Pattern symbolPattern;

    private static void initialize() {
        Locale locale = new Locale(FModel.getPreferences().getPref(FPref.UI_LANGUAGE));
        boundary = BreakIterator.getLineInstance(locale);
        linebreakPattern = Pattern.compile("(\r\n\r\n)|(\n)");
        reminderPattern = Pattern.compile("\\((.+?)\\)");
        reminderHidePattern = Pattern.compile(" \\((.+?)\\)");
        symbolPattern = Pattern.compile("\\{([A-Z0-9]+)\\}|\\{([A-Z0-9]+)/([A-Z0-9]+)\\}");

        NAME_FONT = new Font(Font.SERIF, Font.BOLD, 26);
        TYPE_FONT = new Font(Font.SERIF, Font.BOLD, 22);
        if ("ja-JP".equals(FModel.getPreferences().getPref(FPref.UI_LANGUAGE)) || "zh-CN".equals(FModel.getPreferences().getPref(FPref.UI_LANGUAGE))) {
            TEXT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 24);
            REMINDER_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 22);
        } else {
            if (FModel.getPreferences().getPrefBoolean(FPref.UI_CARD_IMAGE_RENDER_USE_SANS_SERIF_FONT)) {
                TEXT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 24);
                REMINDER_FONT = new Font(Font.SANS_SERIF, Font.ITALIC, 24);
            } else {
                TEXT_FONT = new Font(Font.SERIF, Font.PLAIN, 24);
                REMINDER_FONT = new Font(Font.SERIF, Font.ITALIC, 24);
            }
        }
        PT_FONT = NAME_FONT;
        ARTIST_FONT = new Font(Font.SERIF, Font.BOLD, 20);

        float screenScale = GuiBase.getInterface().getScreenScale();
        int arrayMultiplier = Math.round(2 * screenScale);

        cachedFonts = new HashMap<>();
        cachedFonts.put(NAME_FONT, new Font[NAME_FONT.getSize() * arrayMultiplier]);
        cachedFonts.put(TYPE_FONT, new Font[TYPE_FONT.getSize() * arrayMultiplier]);
        cachedFonts.put(TEXT_FONT, new Font[TEXT_FONT.getSize() * arrayMultiplier]);
        cachedFonts.put(REMINDER_FONT, new Font[REMINDER_FONT.getSize() * arrayMultiplier]);
        cachedFonts.put(ARTIST_FONT, new Font[ARTIST_FONT.getSize() * arrayMultiplier]);

        isInitialed = true;
    }

    private static Color tintColor(Color source, Color tint, float alpha) {
        float r = (tint.getRed() - source.getRed()) * alpha + source.getRed();
        float g = (tint.getGreen() - source.getGreen()) * alpha + source.getGreen();
        float b = (tint.getBlue() - source.getBlue()) * alpha + source.getBlue();
        return new Color(r / 255f, g / 255f, b / 255f, 1f);
    }

    private static Color[] tintColors(Color source, Color[] tints, float alpha) {
        Color[] tintedColors = new Color[tints.length];
        for (int i = 0; i < tints.length; i++) {
            tintedColors[i] = tintColor(source, tints[i], alpha);
        }
        return tintedColors;
    }

    private static Color fromDetailColor(DetailColors detailColor) {
        return new Color(detailColor.r, detailColor.g, detailColor.b);
    }
    private static final Color C_COMMON = fromDetailColor(DetailColors.COMMON);
    private static final Color C_UNCOMMON = fromDetailColor(DetailColors.UNCOMMON);
    private static final Color C_RARE = fromDetailColor(DetailColors.RARE);
    private static final Color C_MYTHIC = fromDetailColor(DetailColors.MYTHIC);
    private static final Color C_SPECIAL = fromDetailColor(DetailColors.SPECIAL);
    private static Color getRarityColor(CardRarity rarity) {
        if (rarity == null)// NPE from Rarity weird...
            return Color.MAGENTA;
        switch(rarity) {
        case Uncommon:
            return C_UNCOMMON;
        case Rare:
            return C_RARE;
        case MythicRare:
            return C_MYTHIC;
        case Special: //"Timeshifted" or other Special Rarity Cards
            return C_SPECIAL;
        default: //case BasicLand: + case Common:
            return C_COMMON;
        }
    }

    private static Font getFontBySize(Font orgFont, int newSize) {
        if (newSize == orgFont.getSize())
            return orgFont;
        Font font = cachedFonts.get(orgFont)[newSize];
        if (font == null) {
            font = orgFont.deriveFont((float)newSize);
            cachedFonts.get(orgFont)[newSize] = font;
        }
        return font;
    }

    private static void updateAreaSizes(float mainRatio, float subRatio) {
        NAME_SIZE = Math.round(NAME_FONT.getSize() * mainRatio);
        TYPE_SIZE = Math.round(TYPE_FONT.getSize() * subRatio);
        TEXT_SIZE = Math.round(TEXT_FONT.getSize() * mainRatio);
        REMINDER_SIZE = Math.round(REMINDER_FONT.getSize() * mainRatio);
        PT_SIZE = Math.round(PT_FONT.getSize() * mainRatio);
        ARTIST_SIZE = Math.round(ARTIST_FONT.getSize() * mainRatio);
        PT_BOX_WIDTH = Math.round(75 * mainRatio);
        HEADER_PADDING = Math.round(7 * mainRatio);
        TYPE_PADDING = Math.round(7 * subRatio) + (mainRatio == subRatio ? (NAME_SIZE - TYPE_SIZE) / 2 : 0);
        BOX_LINE_THICKNESS = Math.max(Math.round(2 * mainRatio), 1);
        BLACK_BORDER_THICKNESS = Math.round(10 * mainRatio);
        ART_INSET = Math.round(BLACK_BORDER_THICKNESS * 0.6f);
        OUTER_BORDER_THICKNESS = Math.round(1.2f * BLACK_BORDER_THICKNESS) - ART_INSET;
    }

    public static void drawCardImage(Graphics2D g, CardView card, boolean altState, int width, int height, BufferedImage art, String legalString) {
        if (!isInitialed) {
            initialize();
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        float ratio = Math.min((float)width / BASE_IMAGE_WIDTH, (float)height / BASE_IMAGE_HEIGHT);
        BLACK_BORDER_THICKNESS = Math.round(10 * ratio);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        if (legalString != null) {
            TEXT_COLOR = Color.LIGHT_GRAY;
            int x = BLACK_BORDER_THICKNESS * 3;
            int y = height - BLACK_BORDER_THICKNESS * 3;
            int w = width;
            boolean hasPTBox = false;
            if (!card.isSplitCard() && !card.isFlipCard()) {
                final CardStateView state = card.getState(card.isAdventureCard() ? false : altState);
                if ((state.isCreature() && !state.getKeywordKey().contains("Level up"))
                        || state.isPlaneswalker() || state.isBattle() || state.isVehicle())
                    hasPTBox = true;
            }
            if (hasPTBox) {
                w -= PT_BOX_WIDTH + BLACK_BORDER_THICKNESS * 5;
            } else {
                w -= BLACK_BORDER_THICKNESS * 6;
            }
            int h = BLACK_BORDER_THICKNESS * 3;
            drawVerticallyCenteredString(g, legalString, new Rectangle(x, y, w, h), ARTIST_FONT, ARTIST_SIZE);
        }

        width -= 2 * BLACK_BORDER_THICKNESS;
        height -= 2 * BLACK_BORDER_THICKNESS;
        g.translate(BLACK_BORDER_THICKNESS, BLACK_BORDER_THICKNESS);
        TEXT_COLOR = Color.BLACK;
        if (card.isSplitCard()) {
            boolean needTranslation = !"en-US".equals(FModel.getPreferences().getPref(FPref.UI_LANGUAGE));
            final CardStateView leftState = card.getLeftSplitState();
            final String leftText = needTranslation ? CardTranslation.getTranslatedOracle(leftState.getName()) : leftState.getOracleText();
            final CardStateView rightState = card.getRightSplitState();
            String rightText = needTranslation ? CardTranslation.getTranslatedOracle(rightState.getName()) : rightState.getOracleText();
            boolean isAftermath = (rightState.getKeywordKey().contains("Aftermath"));
            BufferedImage leftArt = null;
            BufferedImage rightArt = null;
            if (isAftermath) {
                if (art != null) {
                    int leftWidth = Math.round(art.getWidth() * 0.61328125f);
                    leftArt = art.getSubimage(0, 0, leftWidth, art.getHeight());
                    rightArt = art.getSubimage(leftWidth, 0, art.getWidth() - leftWidth, art.getHeight());
                }
                int halfHeight = Math.round(370 * ratio);
                int halfWidth = Math.round(360 * ratio);
                CARD_ART_RATIO = 2.719f;
                updateAreaSizes(ratio, ratio);
                drawCardStateImage(g, leftState, leftText, width, halfHeight, leftArt);
                CARD_ART_RATIO = 1.714f;
                int widthAdjust = OUTER_BORDER_THICKNESS;
                int heightAdjust = OUTER_BORDER_THICKNESS + PT_SIZE / 2;
                g.translate((double) width - widthAdjust, (double)halfWidth);
                g.rotate(Math.PI / 2.);
                drawCardStateImage(g, rightState, rightText, height - halfWidth - heightAdjust, width, rightArt);
            } else {
                if (art != null) {
                    leftArt = art.getSubimage(0, 0, art.getWidth() / 2, art.getHeight());
                    rightArt = art.getSubimage(art.getWidth() / 2, 0, art.getWidth() / 2, art.getHeight());
                }
                CARD_ART_RATIO = 1.443f;
                updateAreaSizes(ratio, ((float)height / 2f / (float)width) * ratio);
                int widthAdjust = OUTER_BORDER_THICKNESS + PT_SIZE / 2;
                int heightAdjust = height - widthAdjust - BLACK_BORDER_THICKNESS;
                AffineTransform tf = g.getTransform();
                g.translate(0., (double)(height - widthAdjust));
                g.rotate(-Math.PI / 2.);
                drawCardStateImage(g, leftState, leftText, heightAdjust / 2, width + widthAdjust, leftArt);
                g.setTransform(tf);
                g.translate(0., (double)(heightAdjust / 2));
                g.rotate(-Math.PI / 2.);
                drawCardStateImage(g, rightState, rightText, heightAdjust / 2, width + widthAdjust, rightArt);
            }
        } else if (card.isFlipCard()) {
            boolean needTranslation = !card.isToken() || !(card.getCloneOrigin() == null);
            final CardStateView state = card.getState(false);
            final String text = card.getText(state, needTranslation ? CardTranslation.getTranslationTexts(state.getName(), "") : null);
            final CardStateView flipState = card.getState(true);
            final String flipText = card.getText(flipState, needTranslation ? CardTranslation.getTranslationTexts(flipState.getName(), "") : null);
            CARD_ART_RATIO = 1.728f;
            updateAreaSizes(ratio, ratio);
            int heightAdjust = OUTER_BORDER_THICKNESS + PT_SIZE / 2;
            if (altState) {
                g.translate(width, height);
                g.rotate(Math.PI);
            }
            drawFlipCardImage(g, state, text, flipState, flipText, width, height - heightAdjust, art);
        } else if (card.isAdventureCard()) {
            boolean needTranslation = !card.isToken() || !(card.getCloneOrigin() == null);
            final CardStateView state = card.getState(false);
            final String text = card.getText(state, needTranslation ? CardTranslation.getTranslationTexts(state.getName(), "") : null);
            final CardStateView advState = card.getState(true);
            final String advText = card.getText(advState, needTranslation ? CardTranslation.getTranslationTexts(advState.getName(), "") : null);
            CARD_ART_RATIO = 1.37f;
            updateAreaSizes(ratio, ratio);
            drawAdvCardImage(g, state, text, advState, advText, width, height, art);
        } else {
            boolean needTranslation = !card.isToken() || !(card.getCloneOrigin() == null);
            final CardStateView state = card.getState(altState);
            final String text = card.getText(state, needTranslation ? CardTranslation.getTranslationTexts(state.getName(), "") : null);
            CARD_ART_RATIO = 1.37f;
            if (art != null && Math.abs((float)art.getWidth() / (float)art.getHeight() - CARD_ART_RATIO) > 0.1f) {
                CARD_ART_RATIO = (float)art.getWidth() / (float)art.getHeight();
            }
            updateAreaSizes(ratio, ratio);
            drawCardStateImage(g, state, text, width, height, art);
        }
        g.dispose();
    }

    private static void drawCardStateImage(Graphics2D g, CardStateView state, String text, int w, int h, BufferedImage art) {
        int x = 0, y = 0;

        //determine colors for borders
        final List<DetailColors> borderColors = CardDetailUtil.getBorderColors(state, true);
        Color[] colors = fillColorBackground(g, borderColors, x, y, w, h, BLACK_BORDER_THICKNESS);

        x += OUTER_BORDER_THICKNESS;
        y += OUTER_BORDER_THICKNESS;
        w -= 2 * OUTER_BORDER_THICKNESS;
        int headerHeight = NAME_SIZE + 2 * HEADER_PADDING;
        int typeBoxHeight = TYPE_SIZE + 2 * TYPE_PADDING;
        int ptBoxHeight = 0;
        if (state.isCreature() || state.isPlaneswalker() | state.isBattle() || state.isVehicle()) {
            //if P/T box needed, make room for it
            ptBoxHeight = headerHeight;
        }

        int artWidth = w - 2 * ART_INSET;
        int artHeight = Math.round(artWidth / CARD_ART_RATIO);
        int textBoxHeight = h - headerHeight - artHeight - typeBoxHeight - OUTER_BORDER_THICKNESS - ART_INSET - PT_SIZE / 2;

        int artY = y + headerHeight;
        int typeY = artY + artHeight;
        int textY = typeY + typeBoxHeight;
        int ptY = textY + textBoxHeight;

        // Adjust layout for Saga, Class and Dungeon cards
        boolean isSaga = state.getType().hasSubtype("Saga");
        boolean isClass = state.getType().hasSubtype("Class");
        boolean isDungeon = state.getType().isDungeon();
        if (isSaga || isClass || isDungeon) {
            // Move type line to the bottom
            typeY = ptY - Math.round(typeBoxHeight * 1.2f);
            if (!isDungeon)
                artWidth = artWidth / 2;
            artHeight = typeY - artY;
            textBoxHeight = artHeight;
            textY = artY;
        }

        //draw art box with Forge icon
        if (!isDungeon) {
            Color[] artBoxColors = tintColors(Color.DARK_GRAY, colors, NAME_BOX_TINT);
            int artX = x + ART_INSET + (isSaga ? artWidth : 0);
            drawArt(g, artBoxColors, artX, artY, artWidth, artHeight, art);
        }

        //handle leveler cards
        boolean isLevelup = (state.getKeywordKey().contains("Level up"));
        if (isLevelup) {
            int textBoxHeightDiv3 = Math.round(textBoxHeight / 3f);
            String [] paragraphs = linebreakPattern.split(text);
            StringBuilder sb = new StringBuilder();
            String text1 = "", text2 = "", text3 = "";
            String level2 = "", level3 = "";
            String ptOverride2 = null, ptOverride3 = null;
            for (String pg : paragraphs) {
                if (pg.matches("(.*[0-9]+-[0-9]+)|(.*[0-9]+\\+)")) {
                    //add space before numbers in case there is no space.
                    pg = pg.replaceAll("([^0-9 ]+)(([0-9]+-[0-9]+)|([0-9]+\\+))", "$1 $2");
                    if (level2.isEmpty()) {
                        text1 = sb.toString();
                        level2 = pg;
                    } else {
                        text2 = sb.toString();
                        level3 = pg;
                    }
                    sb.setLength(0);
                    continue;
                }
                if (pg.matches("[0-9]+/[0-9]+")) {
                    if (ptOverride2 == null)
                        ptOverride2 = pg;
                    else
                        ptOverride3 = pg;
                    continue;
                }
                if (sb.length() > 0)
                    sb.append("\n");
                sb.append(pg);
            }
            text3 = sb.toString();
            //handle the case that translated text doesn't contains P/T info.
            if (ptOverride2 == null) {
                paragraphs = linebreakPattern.split(state.getOracleText());
                for (String pg : paragraphs) {
                    if (pg.matches("[0-9]+/[0-9]+")) {
                        if (ptOverride2 == null)
                            ptOverride2 = pg;
                        else
                            ptOverride3 = pg;
                    }
                }
            }

            int textX = x + ART_INSET;

            //draw text box
            Color[] textBox1Colors = tintColors(Color.WHITE, colors, TEXT_BOX_TINT);
            drawTextBox(g, state, text1, textBox1Colors, textX, textY, artWidth, textBoxHeightDiv3, 2);

            //draw P/T box
            Color[] pt1Colors = tintColors(Color.WHITE, colors, PT_BOX_TINT);
            ptY = textY + (textBoxHeightDiv3 - ptBoxHeight) / 2;
            drawPTBox(g, state, null, pt1Colors, x, ptY, w, ptBoxHeight);

            textY += textBoxHeightDiv3;
            ptY += textBoxHeightDiv3;
            int orgTextSize = TEXT_SIZE;
            int levelBoxWitdh = PT_BOX_WIDTH * 3 / 4;
            //draw text box
            Color lighterGray = new Color(224, 224, 224);
            Color[] textBox2Colors = tintColors(lighterGray, colors, TEXT_BOX_TINT + 0.15f);
            TEXT_SIZE = orgTextSize - 10;
            drawTextBox(g, state, level2, textBox2Colors, textX, textY, levelBoxWitdh, textBoxHeightDiv3, 4);
            TEXT_SIZE = orgTextSize;
            drawTextBox(g, state, text2, textBox2Colors, textX + levelBoxWitdh, textY, artWidth - levelBoxWitdh, textBoxHeightDiv3, 2);

            //draw P/T box
            Color[] pt2Colors = tintColors(lighterGray, colors, PT_BOX_TINT + 0.15f);
            drawPTBox(g, state, ptOverride2, pt2Colors, x, ptY, w, ptBoxHeight);

            textY += textBoxHeightDiv3;
            ptY += textBoxHeightDiv3;
            textBoxHeightDiv3 = textBoxHeight - textBoxHeightDiv3 * 2;
            //draw text box
            Color lightGray = new Color(160, 160, 160);
            Color[] textBox3Colors = tintColors(lightGray, colors, TEXT_BOX_TINT + 0.3f);
            TEXT_SIZE = orgTextSize - 10;
            drawTextBox(g, state, level3, textBox3Colors, textX, textY, levelBoxWitdh, textBoxHeightDiv3, 4);
            TEXT_SIZE = orgTextSize;
            drawTextBox(g, state, text3, textBox3Colors, textX + levelBoxWitdh, textY, artWidth - levelBoxWitdh, textBoxHeightDiv3, 2);

            //draw P/T box
            Color[] pt3Colors = tintColors(lightGray, colors, PT_BOX_TINT + 0.3f);
            drawPTBox(g, state, ptOverride3, pt3Colors, x, ptY, w, ptBoxHeight);
        } else {
            //draw text box
            Color[] textBoxColors = tintColors(Color.WHITE, colors, TEXT_BOX_TINT);
            int textX = x + ART_INSET + (isClass ? artWidth : 0);
            drawTextBox(g, state, text, textBoxColors, textX, textY, artWidth, textBoxHeight, ptBoxHeight > 0 ? 1 : 0);

            //draw P/T box
            if (ptBoxHeight > 0) {
                Color[] ptColors = tintColors(Color.WHITE, colors, PT_BOX_TINT);
                ptY -= ptBoxHeight / 2;
                drawPTBox(g, state, null, ptColors, x, ptY, w, ptBoxHeight);
            }
        }

        //draw header containing name and mana cost
        Color[] headerColors = tintColors(Color.WHITE, colors, NAME_BOX_TINT);
        drawHeader(g, state, headerColors, x, y, w, headerHeight, true, true);

        //draw type line
        drawTypeLine(g, state, headerColors, x, typeY, w, typeBoxHeight, 0, true, true);
    }

    private static void drawFlipCardImage(Graphics2D g, CardStateView state, String text, CardStateView flipState, String flipText, int w, int h, BufferedImage art) {
        int width = w, height = h;
        int x = 0, y = 0;

        //determine colors for borders
        final List<DetailColors> borderColors = CardDetailUtil.getBorderColors(state, true);
        Color[] colors = fillColorBackground(g, borderColors, x, y, w, h, 0);

        x += OUTER_BORDER_THICKNESS;
        y += OUTER_BORDER_THICKNESS;
        w -= 2 * OUTER_BORDER_THICKNESS;
        h -= 2 * OUTER_BORDER_THICKNESS;
        int headerHeight = NAME_SIZE + 2 * HEADER_PADDING;
        int typeBoxHeight = TYPE_SIZE + 2 * TYPE_PADDING;

        int artWidth = w - 2 * ART_INSET;
        int artHeight = Math.round(artWidth / CARD_ART_RATIO);
        int textBoxHeight = (h - (headerHeight + typeBoxHeight) * 2 - artHeight) / 2;
        int ptBoxHeight = headerHeight;

        int textY = y + headerHeight;
        int typeY = textY + textBoxHeight;
        int artY = typeY + typeBoxHeight;
        int ptY = typeY + 1;

        //draw art box with Forge icon
        Color[] artBoxColors = tintColors(Color.DARK_GRAY, colors, NAME_BOX_TINT);
        int artX = x + ART_INSET;
        drawArt(g, artBoxColors, artX, artY, artWidth, artHeight, art);

        //draw text box
        Color[] textBoxColors = tintColors(Color.WHITE, colors, TEXT_BOX_TINT);
        int textX = x + ART_INSET;
        drawTextBox(g, state, text, textBoxColors, textX, textY, artWidth, textBoxHeight, 0);

        //draw header containing name and mana cost
        Color[] headerColors = tintColors(Color.WHITE, colors, NAME_BOX_TINT);
        drawHeader(g, state, headerColors, x, y, w, headerHeight, true, true);

        //draw type line
        drawTypeLine(g, state, headerColors, x, typeY, w, typeBoxHeight, state.isCreature() ? PT_BOX_WIDTH : 0, true, true);

        //draw P/T box
        if (state.isCreature()) {
            Color[] ptColors = tintColors(Color.WHITE, colors, PT_BOX_TINT);
            drawPTBox(g, state, null, ptColors, x, ptY, w, ptBoxHeight);
        }

        //flip the card
        g.translate(width, height);
        g.rotate(Math.PI);

        //draw text box
        drawTextBox(g, flipState, flipText, textBoxColors, textX, textY, artWidth, textBoxHeight, 0);

        //draw header containing name and mana cost
        drawHeader(g, flipState, headerColors, x, y, w, headerHeight, false, true);

        //draw type line
        drawTypeLine(g, flipState, headerColors, x, typeY, w, typeBoxHeight, flipState.isCreature() ? PT_BOX_WIDTH : 0, false, true);

        //draw P/T box
        if (flipState.isCreature()) {
            Color[] ptColors = tintColors(Color.WHITE, colors, PT_BOX_TINT);
            drawPTBox(g, flipState, null, ptColors, x, ptY, w, ptBoxHeight);
        }
    }

    private static void drawAdvCardImage(Graphics2D g, CardStateView state, String text, CardStateView advState, String advText, int w, int h, BufferedImage art) {
        int x = 0, y = 0;

        //determine colors for borders
        final List<DetailColors> borderColors = CardDetailUtil.getBorderColors(state, true);
        Color[] colors = fillColorBackground(g, borderColors, x, y, w, h, BLACK_BORDER_THICKNESS);

        x += OUTER_BORDER_THICKNESS;
        y += OUTER_BORDER_THICKNESS;
        w -= 2 * OUTER_BORDER_THICKNESS;
        int headerHeight = NAME_SIZE + 2 * HEADER_PADDING;
        int typeBoxHeight = TYPE_SIZE + 2 * TYPE_PADDING;
        int ptBoxHeight = headerHeight;

        int artWidth = w - 2 * ART_INSET;
        int artHeight = Math.round(artWidth / CARD_ART_RATIO);
        int textBoxWidth = artWidth / 2;
        int textBoxHeight = h - headerHeight - artHeight - typeBoxHeight - OUTER_BORDER_THICKNESS - ART_INSET - PT_SIZE / 2;

        int artY = y + headerHeight;
        int typeY = artY + artHeight;
        int textY = typeY + typeBoxHeight;
        int ptY = textY + textBoxHeight;

        //draw art box with Forge icon
        Color[] artBoxColors = tintColors(Color.DARK_GRAY, colors, NAME_BOX_TINT);
        int artX = x + ART_INSET;
        drawArt(g, artBoxColors, artX, artY, artWidth, artHeight, art);

        //draw text box
        Color[] textBoxColors = tintColors(Color.WHITE, colors, TEXT_BOX_TINT);
        int textX = x + ART_INSET + textBoxWidth;
        drawTextBox(g, state, text, textBoxColors, textX, textY, textBoxWidth, textBoxHeight, 1);

        //draw header containing name and mana cost
        Color[] headerColors = tintColors(Color.WHITE, colors, NAME_BOX_TINT);
        drawHeader(g, state, headerColors, x, y, w, headerHeight, true, true);

        //draw type line
        drawTypeLine(g, state, headerColors, x, typeY, w, typeBoxHeight, 0, true, true);

        //draw P/T box
        Color[] ptColors = tintColors(Color.WHITE, colors, PT_BOX_TINT);
        drawPTBox(g, state, null, ptColors, x, ptY - ptBoxHeight / 2, w, ptBoxHeight);

        int advHeaderHeight = typeBoxHeight - 2;
        int advTypeHeight = advHeaderHeight - 1;
        NAME_SIZE = TYPE_SIZE - 2;
        TYPE_SIZE = NAME_SIZE - 1;
        textX = x + ART_INSET;

        // refresh if adventure has different color
        colors = fillColorBackground(g, CardDetailUtil.getBorderColors(advState, true), 0, 0, 0, 0, BLACK_BORDER_THICKNESS);
        textBoxColors = tintColors(Color.WHITE, colors, TEXT_BOX_TINT);

        //draw header containing name and mana cost
        Color[] advheaderColors = tintColors(Color.GRAY, colors, 0.6f);
        TEXT_COLOR = Color.WHITE;
        drawHeader(g, advState, advheaderColors, textX, textY, textBoxWidth, advHeaderHeight, true, false);

        //draw type line
        Color[] advTypeColors = tintColors(Color.DARK_GRAY, colors, 0.6f);
        drawTypeLine(g, advState, advTypeColors, textX, textY + advHeaderHeight, textBoxWidth, advTypeHeight, 0, false, false);

        //draw text box
        TEXT_COLOR = Color.BLACK;
        int yAdjust = advHeaderHeight + advTypeHeight;
        drawTextBox(g, advState, advText, textBoxColors, textX, textY, textBoxWidth, textBoxHeight, (yAdjust << 16));
    }

    private static Color[] fillColorBackground(Graphics2D g, List<DetailColors> backColors, int x, int y, int w, int h, int borderThickness) {
        Color[] colors = new Color[backColors.size()];
        for (int i = 0; i < colors.length; i++) {
            DetailColors dc = backColors.get(i);
            colors[i] = new Color(dc.r, dc.g, dc.b);
        }
        fillRoundColorBackground(g, colors, x, y, w, h - 2 * borderThickness, borderThickness * 12, borderThickness * 10);
        fillColorBackground(g, colors, x, y, w, 10 * borderThickness);
        return colors;
    }
    private static void fillColorBackground(Graphics2D g, Color[] colors, float x, float y, float w, float h) {
        Paint oldPaint = g.getPaint();
        switch (colors.length) {
        case 1:
            g.setColor(colors[0]);
            g.fillRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
            break;
        case 2:
            GradientPaint gradient = new GradientPaint(x, y, colors[0], x + w, y, colors[1]);
            g.setPaint(gradient);
            g.fillRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
            break;
        case 3:
            float halfWidth = w / 2;
            GradientPaint gradient1 = new GradientPaint(x, y, colors[0], x + halfWidth, y, colors[1]);
            g.setPaint(gradient1);
            g.fillRect(Math.round(x), Math.round(y), Math.round(halfWidth), Math.round(h));
            GradientPaint gradient2 = new GradientPaint(x + halfWidth, y, colors[1], x + w, y, colors[2]);
            g.setPaint(gradient2);
            g.fillRect(Math.round(x + halfWidth), Math.round(y), Math.round(halfWidth), Math.round(h));
            break;
        }
        g.setPaint(oldPaint);
    }
    private static void fillRoundColorBackground(Graphics2D g, Color[] colors, float x, float y, float w, float h, float arcWidth, float arcHeight) {
        Paint oldPaint = g.getPaint();
        switch (colors.length) {
        case 1:
            g.setColor(colors[0]);
            g.fillRoundRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h), Math.round(arcWidth), Math.round(arcHeight));
            break;
        case 2:
            GradientPaint gradient = new GradientPaint(x, y, colors[0], x + w, y, colors[1]);
            g.setPaint(gradient);
            g.fillRoundRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h), Math.round(arcWidth), Math.round(arcHeight));
            break;
        case 3:
            float halfWidth = w / 2;
            GradientPaint gradient1 = new GradientPaint(x, y, colors[0], x + halfWidth, y, colors[1]);
            g.setPaint(gradient1);
            g.fillRoundRect(Math.round(x), Math.round(y), Math.round(halfWidth), Math.round(h), Math.round(arcWidth), Math.round(arcHeight));
            g.fillRect(Math.round(x + halfWidth - arcWidth), Math.round(y), Math.round(arcWidth), Math.round(h));
            GradientPaint gradient2 = new GradientPaint(x + halfWidth, y, colors[1], x + w, y, colors[2]);
            g.setPaint(gradient2);
            g.fillRoundRect(Math.round(x + halfWidth), Math.round(y), Math.round(halfWidth), Math.round(h), Math.round(arcWidth), Math.round(arcHeight));
            g.fillRect(Math.round(x + halfWidth), Math.round(y), Math.round(arcWidth), Math.round(h));
            break;
        }
        g.setPaint(oldPaint);
    }

    private static void drawVerticallyCenteredString(Graphics2D g, String text, Rectangle area, Font originalFont, int size) {
        Font font = getFontBySize(originalFont, size);
        FontMetrics fontMetrics = g.getFontMetrics(font);

        // Shrink font if the text is too long
        while (fontMetrics.stringWidth(text) > area.width) {
            --size;
            font = getFontBySize(originalFont, size);
            fontMetrics = g.getFontMetrics(font);
        }

        int x = area.x;
        int y = area.y + (area.height - fontMetrics.getHeight()) / 2 + fontMetrics.getAscent();

        g.setFont(font);
        g.setColor(TEXT_COLOR);
        g.drawString(text, x, y);
    }

    private static void drawHeader(Graphics2D g, CardStateView state, Color[] colors, int x, int y, int w, int h, boolean drawMana, boolean drawRoundRect) {
        int padding = h / 3;
        fillRoundColorBackground(g, colors, x, y, w, h, drawRoundRect ? padding : 0, drawRoundRect ? h : 0);
        if (drawRoundRect) {
            g.setStroke(new BasicStroke(BOX_LINE_THICKNESS));
            g.setColor(Color.BLACK);
            g.drawRoundRect(x, y, w, h, padding, h);
        }

        //draw mana cost for card
        if (drawMana) {
            ManaCost manaCost = state.getManaCost();
            int manaCostWidth = manaCost.getGlyphCount() * NAME_SIZE + HEADER_PADDING;
            CardFaceSymbols.draw(g, manaCost, x + w - manaCostWidth, y + (h - NAME_SIZE) / 2 + 1, NAME_SIZE - 1);
            w -= padding + manaCostWidth;
        }

        //draw name for card
        x += padding;
        w -= 2 * padding;
        drawVerticallyCenteredString(g, CardTranslation.getTranslatedName(state.getName()),
            new Rectangle(x, y, w, h), NAME_FONT, NAME_SIZE);
    }


    private static void drawArt(Graphics2D g, Color[] colors, int x, int y, int w, int h, BufferedImage art) {
        if (art != null) {
            int artWidth = art.getWidth();
            int artHeight = art.getHeight();
            if ((float)artWidth / (float)artHeight >= (float)w / (float)h) {
                int newW = Math.round(artHeight * ((float)w / (float)h));
                int newX = (artWidth - newW) / 2;
                g.drawImage(art, x, y, x + w, y + h, newX, 0, newX + newW, art.getHeight(), null);
            } else {
                int newH = Math.round(artWidth * ((float)h / (float)w));
                int newY = (artHeight - newH) / 2;
                g.drawImage(art, x, y, x + w, y + h, 0, newY, art.getWidth(), newY + newH, null);
            }
        } else {
            fillColorBackground(g, colors, x, y, w, h);
            SkinIcon icon = FSkin.getIcon(FSkinProp.ICO_LOGO);
            float artWidth = (float)icon.getSizeForPaint(g).getWidth();
            float artHeight = (float)icon.getSizeForPaint(g).getHeight();
            if (artWidth / artHeight >= (float)w / (float)h) {
                int newH = Math.round(w * (artHeight / artWidth));
                FSkin.drawImage(g, icon, x, y + (h - newH) / 2, w, newH);
            } else {
                int newW = Math.round(h * (artWidth / artHeight));
                FSkin.drawImage(g, icon, x + (w - newW) / 2, y, newW, h);
            }
        }
        g.setStroke(new BasicStroke(BOX_LINE_THICKNESS));
        g.setColor(Color.BLACK);
        g.drawRect(x, y, w, h);
    }

    private static void drawTypeLine(Graphics2D g, CardStateView state, Color[] colors, int x, int y, int w, int h, int adjust, boolean drawRarity, boolean drawRoundRect) {
        int padding = h / 3;
        fillRoundColorBackground(g, colors, x, y, w, h, drawRoundRect ? padding : 0, drawRoundRect ? h : 0);
        if (drawRoundRect) {
            g.setStroke(new BasicStroke(BOX_LINE_THICKNESS));
            g.setColor(Color.BLACK);
            g.drawRoundRect(x, y, w, h, padding, h);
        }

        w -= adjust;

        //draw square icon for rarity
        if (drawRarity) {
            int iconSize = Math.round(h * 0.9f);
            int iconPadding = (h - iconSize) / 2;
            w -= iconSize + iconPadding * 2;
            if (state.getRarity() == null) {
                FSkin.drawImage(g, FSkin.getImage(FSkinProp.IMG_SETLOGO_SPECIAL), x + w + iconPadding, y + (h - iconSize + 1) / 2, iconSize, iconSize);
            } else if (state.getRarity() == CardRarity.Special ) {
                FSkin.drawImage(g, FSkin.getImage(FSkinProp.IMG_SETLOGO_SPECIAL), x + w + iconPadding, y + (h - iconSize + 1) / 2, iconSize, iconSize);
            } else if (state.getRarity() == CardRarity.MythicRare) {
                FSkin.drawImage(g, FSkin.getImage(FSkinProp.IMG_SETLOGO_MYTHIC), x + w + iconPadding, y + (h - iconSize + 1) / 2, iconSize, iconSize);
            } else if (state.getRarity() == CardRarity.Rare) {
                FSkin.drawImage(g, FSkin.getImage(FSkinProp.IMG_SETLOGO_RARE), x + w + iconPadding, y + (h - iconSize + 1) / 2, iconSize, iconSize);
            } else if (state.getRarity() == CardRarity.Uncommon) {
                FSkin.drawImage(g, FSkin.getImage(FSkinProp.IMG_SETLOGO_UNCOMMON), x + w + iconPadding, y + (h - iconSize + 1) / 2, iconSize, iconSize);
            } else {
                FSkin.drawImage(g, FSkin.getImage(FSkinProp.IMG_SETLOGO_COMMON), x + w + iconPadding, y + (h - iconSize + 1) / 2, iconSize, iconSize);
            }
        }

        //draw type
        x += padding;
        w -= padding;
        String typeLine = CardDetailUtil.formatCardType(state, true).replace(" - ", " — ");
        drawVerticallyCenteredString(g, typeLine, new Rectangle(x, y, w, h), TYPE_FONT, TYPE_SIZE);
    }

    /**
     * @param textBoxFlags [0] bit: has PT box, [1] bit: leveler PT box, [2] bit: leveler Level box, [16~31] bit: y adjust
     */
    private static void drawTextBox(Graphics2D g, CardStateView state, String text, Color[] colors,
            int x, int y, int w, int h, int textBoxFlags) {
        int yAdjust = (textBoxFlags >> 16);
        if (state.isLand()) {
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
            Color bgColor = fromDetailColor(modColors);
            bgColor = tintColor(Color.WHITE, bgColor, NAME_BOX_TINT);
            Paint oldPaint = g.getPaint();
            g.setColor(bgColor);
            g.fillRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
            g.setPaint(oldPaint);
        } else {
            fillColorBackground(g, colors, x, y + yAdjust, w, h - yAdjust);
        }
        g.setStroke(new BasicStroke(BOX_LINE_THICKNESS));
        g.setColor(Color.BLACK);
        g.drawRect(x, y, w, h);

        if (state.isBasicLand()) {
            //draw icons for basic lands
            String imageKey;
            switch (state.getName().replaceFirst("^Snow-Covered ", "")) {
            case "Plains":
                imageKey = "W";
                break;
            case "Island":
                imageKey = "U";
                break;
            case "Swamp":
                imageKey = "B";
                break;
            case "Mountain":
                imageKey = "R";
                break;
            case "Forest":
                imageKey = "G";
                break;
            default:
                imageKey = "C";
                break;
            }
            int iconSize = Math.round(h * 0.75f);
            CardFaceSymbols.drawWatermark(imageKey, g, x + (w - iconSize) / 2, y + (h - iconSize) / 2, iconSize);
        } else {
            if (StringUtils.isEmpty(text))
                return;

            int padding = TEXT_SIZE / 4;
            x += padding;
            w -= 2 * padding;
            if ((textBoxFlags & 2) == 2)
                w -= PT_BOX_WIDTH;
            drawTextBoxText(g, text, x, y + yAdjust, w, h - yAdjust, textBoxFlags);
        }
    }

    private static void drawPTBox(Graphics2D g, CardStateView state, String ptOverride, Color[] colors, int x, int y, int w, int h) {
        List<String> pieces = new ArrayList<>();
        if (state.isCreature()) {
            if (ptOverride != null) {
                String [] pt = ptOverride.split("/");
                pieces.add(pt[0]);
                pieces.add("/");
                pieces.add(pt[1]);
            } else {
                pieces.add(String.valueOf(state.getPower()));
                pieces.add("/");
                pieces.add(String.valueOf(state.getToughness()));
            }
        }
        else if (state.isPlaneswalker()) {
            Color [] pwColor = { Color.BLACK };
            colors = pwColor;
            TEXT_COLOR = Color.WHITE;
            pieces.add(String.valueOf(state.getLoyalty()));
        }
        else if (state.isBattle()) {
            Color [] pwColor = { Color.BLACK };
            colors = pwColor;
            TEXT_COLOR = Color.WHITE;
            pieces.add(String.valueOf(state.getDefense()));
        }
        else if (state.isVehicle()) {
            Color [] vhColor = { new Color(128, 96, 64) };
            colors = vhColor;
            TEXT_COLOR = Color.WHITE;
            pieces.add(String.valueOf(state.getPower()));
            pieces.add("/");
            pieces.add(String.valueOf(state.getToughness()));
        }
        else { return; }

        Font font = getFontBySize(PT_FONT, PT_SIZE);
        FontMetrics metrics = g.getFontMetrics(font);
        int padding = PT_SIZE / 4;
        int totalPieceWidth = -padding;
        int[] pieceWidths = new int[pieces.size()];
        for (int i = 0; i < pieces.size(); i++) {
            int pieceWidth = metrics.stringWidth(pieces.get(i)) + padding;
            pieceWidths[i] = pieceWidth;
            totalPieceWidth += pieceWidth;
        }
        x += w - PT_BOX_WIDTH;
        w = PT_BOX_WIDTH;

        int arcWidth = h / 3;
        fillRoundColorBackground(g, colors, x, y, w, h, arcWidth, h);
        g.setStroke(new BasicStroke(BOX_LINE_THICKNESS));
        g.setColor(state.isPlaneswalker() || state.isBattle() ? Color.WHITE : Color.BLACK);
        g.drawRoundRect(x, y, w, h, arcWidth, h);

        x += (PT_BOX_WIDTH - totalPieceWidth) / 2;
        for (int i = 0; i < pieces.size(); i++) {
            drawVerticallyCenteredString(g, pieces.get(i), new Rectangle(x, y - 2, w, h), PT_FONT, PT_SIZE);
            x += pieceWidths[i];
        }
        TEXT_COLOR = Color.BLACK;
    }

    private static abstract class Piece {
        protected final boolean isReminder;

        protected Piece(boolean isReminder) {
            this.isReminder = isReminder;
        }

        public abstract void restart();
        public abstract int getNextWidth(FontMetrics txMetrics, FontMetrics rmMetrics);
        public abstract void drawCurrent(Graphics2D g, int x, int y, Font txFont, Font rmFont, FontMetrics txMetrics);
    }

    private static class TextPiece extends Piece {
        private String text;
        private int index;
        private List<Integer> boundaryList;

        private void buildBoundaryList() {
            boundaryList = new ArrayList<>();
            boundary.setText(text);
            boundaryList.add(boundary.first());
            for (int next = boundary.next(); next != BreakIterator.DONE; next = boundary.next()) {
                boundaryList.add(next);
            }
        }

        public TextPiece(String text, boolean isReminder) {
            super(isReminder);
            this.text = text;
            buildBoundaryList();
        }

        public void restart()
        {
            index = 0;
        }

        public int getNextWidth(FontMetrics txMetrics, FontMetrics rmMetrics) {
            ++index;
            if (index == boundaryList.size()) {
                return -1;
            }
            String subtext = text.substring(boundaryList.get(index - 1), boundaryList.get(index));
            if (isReminder) {
                return rmMetrics.stringWidth(subtext);
            }
            return txMetrics.stringWidth(subtext);
        }

        public void drawCurrent(Graphics2D g, int x, int y, Font txFont, Font rmFont, FontMetrics txMetrics) {
            int ascent = txMetrics.getAscent();
            String subtext = text.substring(boundaryList.get(index - 1), boundaryList.get(index));
            if (isReminder) {
                g.setFont(rmFont);
            } else {
                g.setFont(txFont);
            }
            g.drawString(subtext, x, y + ascent);
        }
    }

    private static class SymbolPiece extends Piece {
        private List<String> symbols;
        private boolean restarted;

        public SymbolPiece(List<String> symbols) {
            super(false);
            this.symbols = symbols;
            restarted = false;
        }

        public void restart() {
            restarted = true;
        }

        public int getNextWidth(FontMetrics txMetrics, FontMetrics rmMetrics) {
            if (restarted) {
                int offset = Math.round(txMetrics.getAscent() * 0.8f);
                restarted = false;
                return offset * symbols.size();
            }
            return -1;
        }

        public void drawCurrent(Graphics2D g, int x, int y, Font txFont, Font rmFont, FontMetrics txMetrics) {
            int xoffset = Math.round(txMetrics.getAscent() * 0.8f);
            int yoffset = txMetrics.getAscent() - xoffset + 2;
            for (String s : symbols) {
                CardFaceSymbols.drawSymbol(s, g, x, y + yoffset, xoffset - 1);
                x += xoffset;
            }
        }
    }

    private static class Paragraph {
        private String text;
        private List<Piece> pieces;

        private void parseSymbols(String subtext, boolean isReminder) {
            List<String> symbols = new ArrayList<>();
            Matcher sbMatcher = symbolPattern.matcher(subtext);
            int parsed = 0;
            while (sbMatcher.find()) {
                if (sbMatcher.start() > parsed) {
                    if (!symbols.isEmpty()) {
                        pieces.add(new SymbolPiece(symbols));
                        symbols = new ArrayList<>();
                    }
                    pieces.add(new TextPiece(subtext.substring(parsed, sbMatcher.start()), isReminder));
                }
                String symbol = sbMatcher.group(1) != null ? sbMatcher.group(1) :
                    // switch position of "P" and mana color for phyrexian mana symbol.
                    "P".equals(sbMatcher.group(3)) ? sbMatcher.group(3) + sbMatcher.group(2) :
                    sbMatcher.group(2) + sbMatcher.group(3);
                symbols.add(symbol);
                parsed = sbMatcher.end();
            }
            if (!symbols.isEmpty()) {
                pieces.add(new SymbolPiece(symbols));
            }
            if (parsed < subtext.length())
                pieces.add(new TextPiece(subtext.substring(parsed, subtext.length()), isReminder));
        }

        private void buildPieceList() {
            pieces = new ArrayList<>();
            Matcher rmMatcher = reminderPattern.matcher(text);
            int parsed = 0;
            while (rmMatcher.find()) {
                // Non-reminder text
                if (rmMatcher.start() > parsed) {
                    parseSymbols(text.substring(parsed, rmMatcher.start()), false);
                }
                parseSymbols(text.substring(rmMatcher.start(), rmMatcher.end()), true);
                parsed = rmMatcher.end();
            }
            // Remaining text
            if (parsed < text.length())
                parseSymbols(text.substring(parsed, text.length()), false);
        }

        public Paragraph(String text) {
            this.text = text;
            buildPieceList();
        }

        public int getTotalWidth(FontMetrics txMetrics, FontMetrics rmMetrics) {
            int width = 0;
            for (Piece p : pieces) {
                p.restart();
                int w = p.getNextWidth(txMetrics, rmMetrics);
                while (w != -1) {
                    width += w;
                    w = p.getNextWidth(txMetrics, rmMetrics);
                }
            }
            return width;
        }

        public int calculateLines(int width, FontMetrics txMetrics, FontMetrics rmMetrics, int flagPTBox) {
            int pos = 0;
            int lines = 1;
            for (Piece p : pieces) {
                p.restart();
                int w = p.getNextWidth(txMetrics, rmMetrics);
                while (w != -1) {
                    if (pos + w > width && pos > 0) {
                        ++lines;
                        pos = 0;
                    }
                    pos += w;
                    w = p.getNextWidth(txMetrics, rmMetrics);
                }
            }
            boolean hasPTBox = (flagPTBox & 1) == 1;
            boolean hasMultipleParagraph = (flagPTBox & 2) == 2;
            // If last line will overlapp with PT box, add one more line.
            if (hasPTBox && pos >= width - PT_BOX_WIDTH) {
                if (lines > 1 || hasMultipleParagraph)
                    ++lines;
            }
            return lines;
        }

        public int drawPieces(Graphics2D g, int x, int y, int width, int lineHeight,
                Font txFont, FontMetrics txMetrics, Font rmFont, FontMetrics rmMetrics) {
            int pos = 0;
            int lines = 1;
            for (Piece p : pieces) {
                p.restart();
                int w = p.getNextWidth(txMetrics, rmMetrics);
                while (w != -1) {
                    if (pos + w > width && pos > 0) {
                        ++lines;
                        pos = 0;
                        y += lineHeight;
                    }
                    p.drawCurrent(g, x + pos, y, txFont, rmFont, txMetrics);
                    pos += w;
                    w = p.getNextWidth(txMetrics, rmMetrics);
                }
            }
            return lines * lineHeight;
        }
    }

    private static void drawTextBoxText(Graphics2D g, String text, int x, int y, int w, int h, int flagPTBox) {
        boolean hasPTBox = (flagPTBox & 1) == 1;
        boolean isLevelup = (flagPTBox & 2) == 2;
        boolean isLevelBox = (flagPTBox & 4) == 4;
        if (FModel.getPreferences().getPrefBoolean(FPref.UI_CARD_IMAGE_RENDER_HIDE_REMINDER_TEXT))
            text = reminderHidePattern.matcher(text).replaceAll("");
        String [] paragraphs = isLevelBox ? text.split(" ") : linebreakPattern.split(text);
        List<Paragraph> pgList = new ArrayList<>();
        for (String pg : paragraphs) {
            pgList.add(new Paragraph(pg));
        }

        // Find font size that fit in the text box area
        Font txFont = getFontBySize(TEXT_FONT, TEXT_SIZE);
        Font rmFont = getFontBySize(REMINDER_FONT, REMINDER_SIZE);
        FontMetrics txMetrics = g.getFontMetrics(txFont);
        FontMetrics rmMetrics = g.getFontMetrics(rmFont);
        int txFontSize = txFont.getSize(), rmFontSize = rmFont.getSize();
        int lineHeight, paraSpacing, lineSpacing, totalHeight, totalLines;
        do {
            int totalLineSpacings = 0;
            totalHeight = 0;
            totalLines = 0;
            paraSpacing = txMetrics.getLeading() + txMetrics.getDescent();
            lineHeight = txMetrics.getAscent() + txMetrics.getDescent();
            lineSpacing = -2;
            for (int i = 0; i < pgList.size(); ++i) {
                // [0] bit: hasPTBox or not, [1] bit: has multiple paragraph or not.
                flagPTBox = (i < pgList.size() - 1) ? 0 : (hasPTBox ? 1 : 0) + (i > 0 ? 2 : 0);
                Paragraph pg = pgList.get(i);
                totalHeight += paraSpacing;
                int lines = pg.calculateLines(w, txMetrics, rmMetrics, flagPTBox);
                totalLines += lines;
                totalLineSpacings += lines - 1;
                totalHeight += lines * lineHeight + (lines - 1) * lineSpacing;
            }
            while (totalHeight > h && lineSpacing > -txMetrics.getDescent()) {
                --lineSpacing;
                totalHeight -= totalLineSpacings;
            }
            if (totalHeight <= h)
                break;
            //Shrink font and do again
            --txFontSize;
            txFont = getFontBySize(TEXT_FONT, txFontSize);
            txMetrics = g.getFontMetrics(txFont);
            --rmFontSize;
            rmFont = getFontBySize(REMINDER_FONT, rmFontSize);
            rmMetrics = g.getFontMetrics(rmFont);
        } while (txFontSize >= 8 && rmFontSize >= 8);

        // Draw text
        // Center text is there is only one line
        if (totalLines == 1 && !isLevelup) {
            Paragraph pg = pgList.get(0);
            int width = pg.getTotalWidth(txMetrics, rmMetrics);
            x += (w - width) / 2;
        }
        y += (h - totalHeight - paraSpacing / 2) / 2;
        for (Paragraph pg : pgList) {
            int xoffset = isLevelBox ? (w - pg.getTotalWidth(txMetrics, rmMetrics)) / 2 : 0;
            y += pg.drawPieces(g, x + xoffset, y, w, lineSpacing + lineHeight, txFont, txMetrics, rmFont, rmMetrics);
            y += paraSpacing - lineSpacing;
            if (isLevelBox) {
                txFont = getFontBySize(TEXT_FONT, txFontSize + 10);
                txMetrics = g.getFontMetrics(txFont);
                y -= paraSpacing;
            }
        }
    }
}
