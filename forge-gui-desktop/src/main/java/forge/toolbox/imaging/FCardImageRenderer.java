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
    private static final float BASE_IMAGE_WIDTH = 480;
    private static final float BASE_IMAGE_HEIGHT = 680;
    private static final float BLACK_BORDER_THICKNESS_RATIO = 0.021f;
    private static final float NAME_BOX_TINT = 0.2f;
    private static final float TEXT_BOX_TINT = 0.1f;
    private static final float PT_BOX_TINT = 0.2f;
    private static float CARD_ART_RATIO;
    private static int PT_BOX_WIDTH, HEADER_PADDING, TYPE_PADDING, BLACK_BORDER_THICKNESS, BORDER_THICKNESS;
    private static Font NAME_FONT, TYPE_FONT, TEXT_FONT, REMINDER_FONT, PT_FONT;
    private static int NAME_SIZE, TYPE_SIZE, TEXT_SIZE, REMINDER_SIZE, PT_SIZE;
    private static Color TEXT_COLOR;

    private static BreakIterator boundary;
    private static Pattern linebreakPattern;
    private static Pattern reminderPattern;
    private static Pattern reminderHidePattern;
    private static Pattern symbolPattern;
    private static Map<Font, Font[]> shrinkFonts;

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

        shrinkFonts = new HashMap<>();
        shrinkFonts.put(NAME_FONT, new Font[NAME_FONT.getSize()]);
        shrinkFonts.put(TYPE_FONT, new Font[TYPE_FONT.getSize()]);
        shrinkFonts.put(TEXT_FONT, new Font[TEXT_FONT.getSize()]);
        shrinkFonts.put(REMINDER_FONT, new Font[REMINDER_FONT.getSize()]);

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
    private static Color C_COMMON = fromDetailColor(DetailColors.COMMON);
    private static Color C_UNCOMMON = fromDetailColor(DetailColors.UNCOMMON);
    private static Color C_RARE = fromDetailColor(DetailColors.RARE);
    private static Color C_MYTHIC = fromDetailColor(DetailColors.MYTHIC);
    private static Color C_SPECIAL = fromDetailColor(DetailColors.SPECIAL);
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

    private static Font getShrinkFont(Font orgFont, int newSize) {
        if (newSize == orgFont.getSize())
            return orgFont;
        Font font = shrinkFonts.get(orgFont)[newSize];
        if (font == null) {
            font = orgFont.deriveFont((float)newSize);
            shrinkFonts.get(orgFont)[newSize] = font;
        }
        return font;
    }

    public static void drawCardImage(Graphics2D g, CardView card, boolean altState, int width, int height) {
        if (!isInitialed)
            initialize();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        float ratio = Math.min((float)width / BASE_IMAGE_WIDTH, (float)height / BASE_IMAGE_HEIGHT);
        TEXT_COLOR = Color.BLACK;
        if (card.isSplitCard()) {
            boolean needTranslation = !"en-US".equals(FModel.getPreferences().getPref(FPref.UI_LANGUAGE));
            final CardStateView leftState = card.getLeftSplitState();
            final String leftText = needTranslation ? CardTranslation.getTranslatedOracle(leftState.getName()) : leftState.getOracleText();
            final CardStateView rightState = card.getRightSplitState();
            String rightText = needTranslation ? CardTranslation.getTranslatedOracle(rightState.getName()) : rightState.getOracleText();
            boolean isAftermath = (rightState.getKeywordKey().contains("Aftermath"));
            if (isAftermath) {
                int halfHeight = Math.round(380 * ratio);
                int halfWidth = Math.round((halfHeight - 10) * ratio);
                CARD_ART_RATIO = 2.68f;
                updateAreaSizes(ratio, ratio);
                drawCardStateImage(g, leftState, leftText, width, halfHeight);
                CARD_ART_RATIO = 1.66f;
                g.translate((double) width, (double)halfWidth);
                g.rotate(Math.PI / 2.);
                drawCardStateImage(g, rightState, rightText, height - halfWidth, width);
            } else {
                CARD_ART_RATIO = 1.36f;
                updateAreaSizes(ratio, (float)height / 2f / (float)width);
                AffineTransform tf = g.getTransform();
                g.translate(0., (double)height);
                g.rotate(-Math.PI / 2.);
                drawCardStateImage(g, leftState, leftText, height / 2, width);
                g.setTransform(tf);
                g.translate(0., (double)height / 2);
                g.rotate(-Math.PI / 2.);
                drawCardStateImage(g, rightState, rightText, height / 2, width);
            }
        } else if (card.isFlipCard()) {
            boolean needTranslation = !card.isToken() || !(card.getCloneOrigin() == null);
            final CardStateView state = card.getState(altState);
            final String text = card.getText(state, needTranslation ? CardTranslation.getTranslationTexts(state.getName(), "") : null);
            final CardStateView flipState = card.getState(!altState);
            final String flipText = card.getText(flipState, needTranslation ? CardTranslation.getTranslationTexts(flipState.getName(), "") : null);
            CARD_ART_RATIO = 1.72f;
            updateAreaSizes(ratio, ratio);
            drawFlipCardImage(g, state, text, flipState, flipText, width, height, altState);
        } else if (card.isAdventureCard()) {
            boolean needTranslation = !card.isToken() || !(card.getCloneOrigin() == null);
            final CardStateView state = card.getState(false);
            final String text = card.getText(state, needTranslation ? CardTranslation.getTranslationTexts(state.getName(), "") : null);
            final CardStateView advState = card.getState(true);
            final String advText = card.getText(advState, needTranslation ? CardTranslation.getTranslationTexts(advState.getName(), "") : null);
            CARD_ART_RATIO = 1.32f;
            updateAreaSizes(ratio, ratio);
            drawAdvCardImage(g, state, text, advState, advText, width, height);
        } else {
            boolean needTranslation = !card.isToken() || !(card.getCloneOrigin() == null);
            final CardStateView state = card.getState(altState);
            final String text = card.getText(state, needTranslation ? CardTranslation.getTranslationTexts(state.getName(), "") : null);
            CARD_ART_RATIO = 1.32f;
            updateAreaSizes(ratio, ratio);
            drawCardStateImage(g, state, text, width, height);
        }
    }

    private static void updateAreaSizes(float mainRatio, float subRatio) {
        NAME_SIZE = Math.round(NAME_FONT.getSize() * mainRatio);
        TYPE_SIZE = Math.round(TYPE_FONT.getSize() * subRatio);
        TEXT_SIZE = Math.round(TEXT_FONT.getSize() * mainRatio);
        REMINDER_SIZE = Math.round(REMINDER_FONT.getSize() * mainRatio);
        PT_SIZE = Math.round(PT_FONT.getSize() * mainRatio);
        PT_BOX_WIDTH = Math.round(75 * mainRatio);
        HEADER_PADDING = Math.round(7 * mainRatio);
        TYPE_PADDING = Math.round(7 * subRatio) + (mainRatio == subRatio ? (NAME_SIZE - TYPE_SIZE) / 2 : 0);
        BORDER_THICKNESS = Math.max(Math.round(2 * mainRatio), 1);
        BLACK_BORDER_THICKNESS = Math.round(10 * mainRatio);
    }

    private static void drawCardStateImage(Graphics2D g, CardStateView state, String text, int w, int h) {
        int x = 0, y = 0;
        g.setColor(Color.BLACK);
        g.fillRect(x, y, w, h);
        x += BLACK_BORDER_THICKNESS;
        y += BLACK_BORDER_THICKNESS;
        w -= 2 * BLACK_BORDER_THICKNESS;
        h -= 2 * BLACK_BORDER_THICKNESS;

        //determine colors for borders
        final List<DetailColors> borderColors = CardDetailUtil.getBorderColors(state, true);
        Color[] colors = fillColorBackground(g, borderColors, x, y, w, h);

        int artInset = Math.round(BLACK_BORDER_THICKNESS * 0.8f);
        int outerBorderThickness = 2 * BLACK_BORDER_THICKNESS - artInset;
        x += outerBorderThickness;
        y += outerBorderThickness;
        w -= 2 * outerBorderThickness;
        int headerHeight = NAME_SIZE + 2 * HEADER_PADDING;
        int typeBoxHeight = TYPE_SIZE + 2 * TYPE_PADDING;
        int ptBoxHeight = 0;
        if (state.isCreature() || state.isPlaneswalker() || state.getType().hasSubtype("Vehicle")) {
            //if P/T box needed, make room for it
            ptBoxHeight = NAME_SIZE + HEADER_PADDING;
        }

        int artWidth = w - 2 * artInset;
        int artHeight = Math.round(artWidth / CARD_ART_RATIO);
        int textBoxHeight = h - headerHeight - artHeight - typeBoxHeight - outerBorderThickness - artInset - PT_FONT.getSize() / 2;

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
            typeY = ptY - typeBoxHeight;
            if (!isDungeon)
                artWidth = artWidth / 2;
            artHeight = typeY - artY;
            textBoxHeight = artHeight;
            textY = artY;
        }

        //draw art box with Forge icon
        if (!isDungeon) {
            Color[] artBoxColors = tintColors(Color.DARK_GRAY, colors, NAME_BOX_TINT);
            int artX = x + artInset + (isSaga ? artWidth : 0);
            drawArt(g, artBoxColors, artX, artY, artWidth, artHeight);
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

            int textX = x + artInset;

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
            int textX = x + artInset + (isClass ? artWidth : 0);
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
        drawHeader(g, state, headerColors, x, y, w, headerHeight, true);

        //draw type line
        drawTypeLine(g, state, headerColors, x, typeY, w, typeBoxHeight, 0, true);
    }

    private static void drawFlipCardImage(Graphics2D g, CardStateView state, String text, CardStateView flipState, String flipText, int w, int h, boolean isFlipped) {
        int width = w, height = h;
        int x = 0, y = 0;
        g.setColor(Color.BLACK);
        g.fillRect(x, y, w, h);
        x += BLACK_BORDER_THICKNESS;
        y += BLACK_BORDER_THICKNESS;
        w -= 2 * BLACK_BORDER_THICKNESS;
        h -= 2 * BLACK_BORDER_THICKNESS;

        //determine colors for borders
        final List<DetailColors> borderColors = CardDetailUtil.getBorderColors(state, true);
        Color[] colors = fillColorBackground(g, borderColors, x, y, w, h);

        int artInset = Math.round(BLACK_BORDER_THICKNESS * 0.8f);
        int outerBorderThickness = 2 * BLACK_BORDER_THICKNESS - artInset;
        x += outerBorderThickness;
        y += outerBorderThickness;
        w -= 2 * outerBorderThickness;
        h -= 2 * outerBorderThickness;
        int headerHeight = NAME_SIZE + 2 * HEADER_PADDING;
        int typeBoxHeight = TYPE_SIZE + 2 * TYPE_PADDING;

        int artWidth = w - 2 * artInset;
        int artHeight = Math.round(artWidth / CARD_ART_RATIO);
        int textBoxHeight = (h - (headerHeight + typeBoxHeight) * 2 - artHeight) / 2;
        int ptBoxHeight = NAME_SIZE + HEADER_PADDING;

        int textY = y + headerHeight;
        int typeY = textY + textBoxHeight;
        int artY = typeY + typeBoxHeight;
        int ptY = typeY - 4;

        //draw art box with Forge icon
        if (!isFlipped) {
            Color[] artBoxColors = tintColors(Color.DARK_GRAY, colors, NAME_BOX_TINT);
            int artX = x + artInset;
            drawArt(g, artBoxColors, artX, artY, artWidth, artHeight);
        }

        //draw text box
        Color[] textBoxColors = tintColors(Color.WHITE, colors, TEXT_BOX_TINT);
        int textX = x + artInset;
        drawTextBox(g, state, text, textBoxColors, textX, textY, artWidth, textBoxHeight, 0);

        //draw header containing name and mana cost
        Color[] headerColors = tintColors(Color.WHITE, colors, NAME_BOX_TINT);
        drawHeader(g, state, headerColors, x, y, w, headerHeight, !isFlipped);

        //draw type line
        drawTypeLine(g, state, headerColors, x, typeY, w, typeBoxHeight, state.isCreature() ? PT_BOX_WIDTH : 0, !isFlipped);

        //draw P/T box
        if (state.isCreature()) {
            Color[] ptColors = tintColors(Color.WHITE, colors, PT_BOX_TINT);
            drawPTBox(g, state, null, ptColors, x, ptY, w, ptBoxHeight);
        }

        //flip the card
        g.translate(width, height);
        g.rotate(Math.PI);

        //draw art box with Forge icon
        if (isFlipped) {
            Color[] artBoxColors = tintColors(Color.DARK_GRAY, colors, NAME_BOX_TINT);
            int artX = x + artInset;
            drawArt(g, artBoxColors, artX, artY, artWidth, artHeight);
        }

        //draw text box
        drawTextBox(g, flipState, flipText, textBoxColors, textX, textY, artWidth, textBoxHeight, 0);

        //draw header containing name and mana cost
        drawHeader(g, flipState, headerColors, x, y, w, headerHeight, isFlipped);

        //draw type line
        drawTypeLine(g, flipState, headerColors, x, typeY, w, typeBoxHeight, flipState.isCreature() ? PT_BOX_WIDTH : 0, isFlipped);

        //draw P/T box
        if (flipState.isCreature()) {
            Color[] ptColors = tintColors(Color.WHITE, colors, PT_BOX_TINT);
            drawPTBox(g, flipState, null, ptColors, x, ptY, w, ptBoxHeight);
        }
    }

    private static void drawAdvCardImage(Graphics2D g, CardStateView state, String text, CardStateView advState, String advText, int w, int h) {
        int x = 0, y = 0;
        g.setColor(Color.BLACK);
        g.fillRect(x, y, w, h);
        x += BLACK_BORDER_THICKNESS;
        y += BLACK_BORDER_THICKNESS;
        w -= 2 * BLACK_BORDER_THICKNESS;
        h -= 2 * BLACK_BORDER_THICKNESS;

        //determine colors for borders
        final List<DetailColors> borderColors = CardDetailUtil.getBorderColors(state, true);
        Color[] colors = fillColorBackground(g, borderColors, x, y, w, h);

        int artInset = Math.round(BLACK_BORDER_THICKNESS * 0.8f);
        int outerBorderThickness = 2 * BLACK_BORDER_THICKNESS - artInset;
        x += outerBorderThickness;
        y += outerBorderThickness;
        w -= 2 * outerBorderThickness;
        int headerHeight = NAME_SIZE + 2 * HEADER_PADDING;
        int typeBoxHeight = TYPE_SIZE + 2 * TYPE_PADDING;
        int ptBoxHeight = NAME_SIZE + HEADER_PADDING;

        int artWidth = w - 2 * artInset;
        int artHeight = Math.round(artWidth / CARD_ART_RATIO);
        int textBoxWidth = artWidth / 2;
        int textBoxHeight = h - headerHeight - artHeight - typeBoxHeight - outerBorderThickness - artInset - PT_FONT.getSize() / 2;

        int artY = y + headerHeight;
        int typeY = artY + artHeight;
        int textY = typeY + typeBoxHeight;
        int ptY = textY + textBoxHeight;

        //draw art box with Forge icon
        Color[] artBoxColors = tintColors(Color.DARK_GRAY, colors, NAME_BOX_TINT);
        int artX = x + artInset;
        drawArt(g, artBoxColors, artX, artY, artWidth, artHeight);

        //draw text box
        Color[] textBoxColors = tintColors(Color.WHITE, colors, TEXT_BOX_TINT);
        int textX = x + artInset + textBoxWidth;
        drawTextBox(g, state, text, textBoxColors, textX, textY, textBoxWidth, textBoxHeight, 1);

        //draw header containing name and mana cost
        Color[] headerColors = tintColors(Color.WHITE, colors, NAME_BOX_TINT);
        drawHeader(g, state, headerColors, x, y, w, headerHeight, true);

        //draw type line
        drawTypeLine(g, state, headerColors, x, typeY, w, typeBoxHeight, 0, true);

        //draw P/T box
        Color[] ptColors = tintColors(Color.WHITE, colors, PT_BOX_TINT);
        drawPTBox(g, state, null, ptColors, x, ptY - ptBoxHeight / 2, w, ptBoxHeight);

        int advHeaderHeight = typeBoxHeight - 2;
        int advTypeHeight = advHeaderHeight - 1;
        NAME_SIZE = TYPE_SIZE - 2;
        TYPE_SIZE = NAME_SIZE - 1;
        textX = x + artInset;

        //draw header containing name and mana cost
        Color[] advheaderColors = tintColors(Color.GRAY, colors, 0.6f);
        TEXT_COLOR = Color.WHITE;
        drawHeader(g, advState, advheaderColors, textX, textY, textBoxWidth, advHeaderHeight, true);

        //draw type line
        Color[] advTypeColors = tintColors(Color.DARK_GRAY, colors, 0.6f);
        textY += advHeaderHeight;
        drawTypeLine(g, advState, advTypeColors, textX, textY, textBoxWidth, advTypeHeight, 0, false);

        //draw text box
        TEXT_COLOR = Color.BLACK;
        textY += advTypeHeight;
        textBoxHeight -= advHeaderHeight + advTypeHeight;
        drawTextBox(g, advState, advText, textBoxColors, textX, textY, textBoxWidth, textBoxHeight, 0);
    }

    private static Color[] fillColorBackground(Graphics2D g, List<DetailColors> backColors, int x, int y, int w, int h) {
        Color[] colors = new Color[backColors.size()];
        for (int i = 0; i < colors.length; i++) {
            DetailColors dc = backColors.get(i);
            colors[i] = new Color(dc.r, dc.g, dc.b);
        }
        fillColorBackground(g, colors, x, y, w, h);
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

    private static void drawVerticallyCenteredString(Graphics2D g, String text, Rectangle area, Font originalFont, int size) {
        Font font = getShrinkFont(originalFont, size);
        FontMetrics fontMetrics = g.getFontMetrics(font);

        // Shrink font if the text is too long
        while (fontMetrics.stringWidth(text) > area.width) {
            --size;
            font = getShrinkFont(originalFont, size);
            fontMetrics = g.getFontMetrics(font);
        }

        int x = area.x;
        int y = area.y + (area.height - fontMetrics.getHeight()) / 2 + fontMetrics.getAscent();

        g.setFont(font);
        g.setColor(TEXT_COLOR);
        g.drawString(text, x, y);
    }

    private static void drawHeader(Graphics2D g, CardStateView state, Color[] colors, int x, int y, int w, int h, boolean drawMana) {
        fillColorBackground(g, colors, x, y, w, h);
        g.setStroke(new BasicStroke(BORDER_THICKNESS));
        g.setColor(Color.BLACK);
        g.drawRect(x, y, w, h);

        int padding = h / 4;

        //draw mana cost for card
        if (drawMana) {
            ManaCost manaCost = state.getManaCost();
            int manaCostWidth = manaCost.getGlyphCount() * NAME_SIZE + HEADER_PADDING;
            CardFaceSymbols.draw(g, manaCost, x + w - manaCostWidth, y + (h - NAME_SIZE) / 2 + 1, NAME_SIZE);
            w -= padding + manaCostWidth;
        }

        //draw name for card
        x += padding;
        w -= 2 * padding;
        drawVerticallyCenteredString(g, CardTranslation.getTranslatedName(state.getName()),
            new Rectangle(x, y, w, h), NAME_FONT, NAME_SIZE);
    }


    private static void drawArt(Graphics2D g, Color[] colors, int x, int y, int w, int h) {
        fillColorBackground(g, colors, x, y, w, h);
        SkinIcon art = FSkin.getIcon(FSkinProp.ICO_LOGO);
        float artWidth = (float)art.getSizeForPaint(g).getWidth();
        float artHeight = (float)art.getSizeForPaint(g).getHeight();
        if (artWidth / artHeight >= (float)w / (float)h) {
            int newH = Math.round(w * (artHeight / artWidth));
            FSkin.drawImage(g, art, x, y + (h - newH) / 2, w, newH);
        } else {
            int newW = Math.round(h * (artWidth / artHeight));
            FSkin.drawImage(g, art, x + (w - newW) / 2, y, newW, h);
        }
        g.setStroke(new BasicStroke(BORDER_THICKNESS));
        g.setColor(Color.BLACK);
        g.drawRect(x, y, w, h);
    }

    private static void drawTypeLine(Graphics2D g, CardStateView state, Color[] colors, int x, int y, int w, int h, int adjust, boolean drawRarity) {
        fillColorBackground(g, colors, x, y, w, h);
        g.setStroke(new BasicStroke(BORDER_THICKNESS));
        g.setColor(Color.BLACK);
        g.drawRect(x, y, w, h);

        w -= adjust;
        int padding = h / 4;

        //draw square icon for rarity
        if (drawRarity) {
            int iconSize = Math.round(h * 0.55f);
            int iconPadding = (h - iconSize) / 2;
            w -= iconSize + iconPadding * 2;
            g.setColor(getRarityColor(state.getRarity()));
            g.fillRect(x + w + iconPadding, y + (h - iconSize) / 2, iconSize, iconSize);
        }

        //draw type
        x += padding;
        w -= padding;
        String typeLine = CardDetailUtil.formatCardType(state, true).replace(" - ", " — ");
        drawVerticallyCenteredString(g, typeLine, new Rectangle(x, y, w, h), TYPE_FONT, TYPE_SIZE);
    }

    /**
     * @param flagPTBox [0] bit: has PT box, [1] bit: leveler PT box, [2] bit: leveler Level box
     */
    private static void drawTextBox(Graphics2D g, CardStateView state, String text, Color[] colors,
            int x, int y, int w, int h, int flagPTBox) {
        fillColorBackground(g, colors, x, y, w, h);
        g.setStroke(new BasicStroke(BORDER_THICKNESS));
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
            CardFaceSymbols.drawSymbol(imageKey, g, x + (w - iconSize) / 2, y + (h - iconSize) / 2, iconSize);
        } else {
            if (StringUtils.isEmpty(text))
                return;

            int padding = TEXT_SIZE / 4;
            x += padding;
            w -= 2 * padding;
            if ((flagPTBox & 2) == 2)
                w -= PT_BOX_WIDTH;
            drawTextBoxText(g, text, x, y, w, h, flagPTBox);
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
            pieces.add(String.valueOf(state.getLoyalty()));
        }
        else if (state.getType().hasSubtype("Vehicle")) {
            pieces.add("[");
            pieces.add(String.valueOf(state.getPower()));
            pieces.add("/");
            pieces.add(String.valueOf(state.getToughness()));
            pieces.add("]");
        }
        else { return; }

        Font font = getShrinkFont(PT_FONT, PT_SIZE);
        FontMetrics metrics = g.getFontMetrics(font);
        int padding = PT_SIZE / 4;
        int totalPieceWidth = -padding;
        int[] pieceWidths = new int[pieces.size()];
        for (int i = 0; i < pieces.size(); i++) {
            int pieceWidth = metrics.stringWidth(pieces.get(i)) + padding;
            pieceWidths[i] = pieceWidth;
            totalPieceWidth += pieceWidth;
        }
        int boxHeight = metrics.getMaxAscent() + padding;

        int boxWidth = Math.max(PT_BOX_WIDTH, totalPieceWidth + 2 * padding);
        x += w - boxWidth;
        y += h - boxHeight;
        w = boxWidth;
        h = boxHeight;

        fillColorBackground(g, colors, x, y, w, h);
        g.setStroke(new BasicStroke(BORDER_THICKNESS));
        g.setColor(Color.BLACK);
        g.drawRect(x, y, w, h);

        x += (boxWidth - totalPieceWidth) / 2;
        for (int i = 0; i < pieces.size(); i++) {
            drawVerticallyCenteredString(g, pieces.get(i), new Rectangle(x, y - 2, w, h), PT_FONT, PT_SIZE);
            x += pieceWidths[i];
        }
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
        Font txFont = getShrinkFont(TEXT_FONT, TEXT_SIZE);
        Font rmFont = getShrinkFont(REMINDER_FONT, REMINDER_SIZE);
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
            txFont = getShrinkFont(TEXT_FONT, txFontSize);
            txMetrics = g.getFontMetrics(txFont);
            --rmFontSize;
            rmFont = getShrinkFont(REMINDER_FONT, rmFontSize);
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
                txFont = getShrinkFont(TEXT_FONT, txFontSize + 10);
                txMetrics = g.getFontMetrics(txFont);
                y -= paraSpacing;
            }
        }
    }
}
