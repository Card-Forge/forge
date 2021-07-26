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
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.gui.card.CardDetailUtil;
import forge.gui.card.CardDetailUtil.DetailColors;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.toolbox.CardFaceSymbols;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinIcon;
import forge.util.CardTranslation;

public class FCardImageRenderer {
    private static final float BASE_IMAGE_WIDTH = 480;
    private static final float BASE_IMAGE_HEIGHT = 680;
    private static float MANA_SYMBOL_SIZE, PT_BOX_WIDTH, HEADER_PADDING, BORDER_THICKNESS;
    private static Font NAME_FONT, TYPE_FONT, TEXT_FONT, REMINDER_FONT, PT_FONT;
    private static FontMetrics NAME_METRICS, TYPE_METRICS, TEXT_METRICS, REMINDER_METRICS, PT_METRICS;
    private static float prevImageWidth, prevImageHeight;
    private static final float BLACK_BORDER_THICKNESS_RATIO = 0.021f;
    private static final float NAME_BOX_TINT = 0.2f;
    private static final float TEXT_BOX_TINT = 0.1f;
    private static final float PT_BOX_TINT = 0.2f;
    private static final float CARD_ART_RATIO = 1.32f;

    private static Locale locale;
    private static BreakIterator boundary;
    private static Pattern linebreakPattern;
    private static Pattern reminderPattern;
    private static Pattern symbolPattern;
    private static final Map<Font, Font[]> shrinkFonts = new HashMap<>();

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
        Font font = shrinkFonts.get(orgFont)[newSize];
        if (font == null) {
            font = orgFont.deriveFont((float)newSize);
            shrinkFonts.get(orgFont)[newSize] = font;
        }
        return font;
    }

    private static void updateStaticFields(Graphics2D g, float w, float h) {
        if (w == prevImageWidth && h == prevImageHeight) {
            //for performance sake, only update static fields if card image size is different than previous rendered card
            return;
        }

        locale = new Locale(FModel.getPreferences().getPref(FPref.UI_LANGUAGE));
        boundary = BreakIterator.getLineInstance(locale);
        linebreakPattern = Pattern.compile("(\r\n\r\n)|(\n)");
        reminderPattern = Pattern.compile("\\((.+?)\\)");
        symbolPattern = Pattern.compile("\\{([A-Z0-9]+)\\}|\\{([A-Z0-9]+)/([A-Z0-9]+)\\}");

        float ratio = Math.min(w / BASE_IMAGE_WIDTH, h / BASE_IMAGE_HEIGHT);

        MANA_SYMBOL_SIZE = 26 * ratio;
        PT_BOX_WIDTH = 75 * ratio;
        HEADER_PADDING = 7 * ratio;
        NAME_FONT = new Font(Font.SERIF, Font.BOLD, Math.round(MANA_SYMBOL_SIZE));
        TYPE_FONT = new Font(Font.SERIF, Font.BOLD, Math.round(MANA_SYMBOL_SIZE * 0.8f));
        if ("ja-JP".equals(FModel.getPreferences().getPref(FPref.UI_LANGUAGE)) || "zh-CN".equals(FModel.getPreferences().getPref(FPref.UI_LANGUAGE))) {
            TEXT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, Math.round(MANA_SYMBOL_SIZE * 0.93f));
            REMINDER_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, Math.round(MANA_SYMBOL_SIZE * 0.93f) - 2);
        } else {
            TEXT_FONT = new Font(Font.SERIF, Font.PLAIN, Math.round(MANA_SYMBOL_SIZE * 0.93f));
            REMINDER_FONT = new Font(Font.SERIF, Font.ITALIC, Math.round(MANA_SYMBOL_SIZE * 0.93f));
        }
        PT_FONT = NAME_FONT;
        NAME_METRICS = g.getFontMetrics(NAME_FONT);
        TYPE_METRICS = g.getFontMetrics(TYPE_FONT);
        TEXT_METRICS = g.getFontMetrics(TEXT_FONT);
        REMINDER_METRICS = g.getFontMetrics(REMINDER_FONT);
        PT_METRICS = NAME_METRICS;
        BORDER_THICKNESS = Math.max(2 * ratio, 1f); //don't let border go below 1

        shrinkFonts.put(NAME_FONT, new Font[NAME_FONT.getSize()]);
        shrinkFonts.put(TYPE_FONT, new Font[TYPE_FONT.getSize()]);
        shrinkFonts.put(TEXT_FONT, new Font[TEXT_FONT.getSize()]);
        shrinkFonts.put(REMINDER_FONT, new Font[REMINDER_FONT.getSize()]);

        prevImageWidth = w;
        prevImageHeight = h;
    }

    public static void drawCardImage(Graphics2D g, PaperCard pc, boolean altState, int width, int height) {
        final CardView card = Card.getCardForUi(pc).getView();
        float x = 0, y = 0, w = width, h = height;
        updateStaticFields(g, w, h);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        float blackBorderThickness = w * BLACK_BORDER_THICKNESS_RATIO;
        g.setColor(Color.BLACK);
        g.fillRect((int)x, (int)y, (int)w, (int)h);
        x += blackBorderThickness;
        y += blackBorderThickness;
        w -= 2 * blackBorderThickness;
        h -= 2 * blackBorderThickness;

        final CardStateView state = card.getState(altState);
        //determine colors for borders
        final List<DetailColors> borderColors = CardDetailUtil.getBorderColors(state, true);
        Color[] colors = fillColorBackground(g, borderColors, x, y, w, h);

        float artInset = blackBorderThickness * 0.8f;
        float outerBorderThickness = 2 * blackBorderThickness - artInset;
        x += outerBorderThickness;
        y += outerBorderThickness;
        w -= 2 * outerBorderThickness;
        float headerHeight = MANA_SYMBOL_SIZE + 2 * HEADER_PADDING + 2;

        float artWidth = w - 2 * artInset;
        float artHeight = artWidth / CARD_ART_RATIO;
        float typeBoxHeight = TYPE_METRICS.getHeight() + HEADER_PADDING + 2;
        float ptBoxHeight = 0;
        float textBoxHeight = h - headerHeight - artHeight - typeBoxHeight - outerBorderThickness - artInset - PT_METRICS.getHeight() / 2f;
        if (state.isCreature() || state.isPlaneswalker() || state.getType().hasSubtype("Vehicle")) {
            //if P/T box needed, make room for it
            ptBoxHeight = MANA_SYMBOL_SIZE + HEADER_PADDING;
        }

        float artY = y + headerHeight;
        float typeY = artY + artHeight;
        float textY = typeY + typeBoxHeight;
        float ptY = textY + textBoxHeight;

        //draw art box with Forge icon
        Color[] artBoxColors = tintColors(Color.DARK_GRAY, colors, NAME_BOX_TINT);
        drawArt(g, artBoxColors, x + artInset, artY, artWidth, artHeight);

        //draw text box
        Color[] textBoxColors = tintColors(Color.WHITE, colors, TEXT_BOX_TINT);
        drawTextBox(g, card, state, textBoxColors, x + artInset, textY, w - 2 * artInset, textBoxHeight, ptBoxHeight > 0);

        //draw header containing name and mana cost
        Color[] headerColors = tintColors(Color.WHITE, colors, NAME_BOX_TINT);
        drawHeader(g, card, state, headerColors, x, y, w, headerHeight);

        //draw type line
        drawTypeLine(g, card, state, headerColors, x, typeY, w, typeBoxHeight);

        //draw P/T box
        if (ptBoxHeight > 0) {
            //only needed if on top since otherwise P/T will be hidden
            Color[] ptColors = tintColors(Color.WHITE, colors, PT_BOX_TINT);
            drawPtBox(g, card, state, ptColors, x, ptY - ptBoxHeight / 2f, w, ptBoxHeight);
        }
    }

    private static Color[] fillColorBackground(Graphics2D g, List<DetailColors> backColors, float x, float y, float w, float h) {
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

    private static void drawVerticallyCenteredString(Graphics2D g, String text, Rectangle area, Font font, FontMetrics fontMetrics) {
        Font originalFont = font;

        // Shrink font if the text is too long
        while (fontMetrics.stringWidth(text) > area.width) {
            int newSize = font.getSize() - 1;
            font = getShrinkFont(originalFont, newSize);
            fontMetrics = g.getFontMetrics(font);
        }

        int x = area.x;
        int y = area.y + (area.height - fontMetrics.getHeight()) / 2 + fontMetrics.getAscent();

        g.setFont(font);
        g.drawString(text, x, y);
    }

    private static void drawHeader(Graphics2D g, CardView card, CardStateView state, Color[] colors, float x, float y, float w, float h) {
        fillColorBackground(g, colors, x, y, w, h);
        g.setStroke(new BasicStroke(BORDER_THICKNESS));
        g.setColor(Color.BLACK);
        g.drawRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h));

        float padding = h / 8;

        //draw mana cost for card
        float manaCostWidth = 0;
        ManaCost mainManaCost = state.getManaCost();
        if (card.isSplitCard() && card.getAlternateState() != null) {
            //handle rendering both parts of split card
            mainManaCost = card.getLeftSplitState().getManaCost();
            ManaCost otherManaCost = card.getAlternateState().getManaCost();
            manaCostWidth = otherManaCost.getGlyphCount() * MANA_SYMBOL_SIZE + HEADER_PADDING;
            CardFaceSymbols.draw(g, otherManaCost, Math.round(x + w - manaCostWidth), Math.round(y + (h - MANA_SYMBOL_SIZE) / 2), Math.round(MANA_SYMBOL_SIZE));
            //draw "//" between two parts of mana cost
            manaCostWidth += NAME_METRICS.stringWidth("//") + HEADER_PADDING;
            drawVerticallyCenteredString(g, "//",
                new Rectangle(Math.round(x + w - manaCostWidth), Math.round(y), Math.round(w), Math.round(h)),
                NAME_FONT, NAME_METRICS);
        }
        manaCostWidth += mainManaCost.getGlyphCount() * MANA_SYMBOL_SIZE + HEADER_PADDING;
        CardFaceSymbols.draw(g, mainManaCost, Math.round(x + w - manaCostWidth), Math.round(y + (h - MANA_SYMBOL_SIZE) / 2), Math.round(MANA_SYMBOL_SIZE));

        //draw name for card
        x += padding;
        w -= 2 * padding;
        drawVerticallyCenteredString(g, CardTranslation.getTranslatedName(state.getName()),
            new Rectangle(Math.round(x), Math.round(y), Math.round(w - manaCostWidth - padding), Math.round(h)),
            NAME_FONT, NAME_METRICS);
    }


    private static void drawArt(Graphics2D g, Color[] colors, float x, float y, float w, float h) {
        fillColorBackground(g, colors, x, y, w, h);
        SkinIcon art = FSkin.getIcon(FSkinProp.ICO_LOGO);
        float artWidth = (float)art.getSizeForPaint(g).getWidth();
        float artHeight = (float)art.getSizeForPaint(g).getHeight();
        if (artWidth / artHeight >= w / h) {
            float newH = w * (artHeight / artWidth);
            FSkin.drawImage(g, art, Math.round(x), Math.round(y + (h - newH) / 2), Math.round(w), Math.round(newH));
        } else {
            float newW = h * (artWidth / artHeight);
            FSkin.drawImage(g, art, Math.round(x + (w - newW) / 2), Math.round(y), Math.round(newW), Math.round(h));
        }
        g.setStroke(new BasicStroke(BORDER_THICKNESS));
        g.setColor(Color.BLACK);
        g.drawRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
    }

    private static void drawTypeLine(Graphics2D g, CardView card, CardStateView state, Color[] colors, float x, float y, float w, float h) {
        fillColorBackground(g, colors, x, y, w, h);
        g.setStroke(new BasicStroke(BORDER_THICKNESS));
        g.setColor(Color.BLACK);
        g.drawRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h));

        float padding = h / 8;

        //draw square icon for rarity
        float iconSize = h * 0.55f;
        float iconPadding = (h - iconSize) / 2;
        w -= iconSize + iconPadding * 2;
        g.setColor(getRarityColor(state.getRarity()));
        g.fillRect(Math.round(x + w + iconPadding), Math.round(y + (h - iconSize) / 2), Math.round(iconSize), Math.round(iconSize));

        //draw type
        x += padding;
        g.setColor(Color.BLACK);
        drawVerticallyCenteredString(g, CardDetailUtil.formatCardType(state, true),
            new Rectangle(Math.round(x), Math.round(y), Math.round(w), Math.round(h)),
            TYPE_FONT, TYPE_METRICS);
    }

    private static void drawTextBox(Graphics2D g, CardView card, CardStateView state, Color[] colors, float x, float y, float w, float h, boolean hasPTBox) {
        fillColorBackground(g, colors, x, y, w, h);
        g.setStroke(new BasicStroke(BORDER_THICKNESS));
        g.setColor(Color.BLACK);
        g.drawRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h));

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
            CardFaceSymbols.drawSymbol(imageKey, g, Math.round(x + (w - iconSize) / 2), Math.round(y + (h - iconSize) / 2), iconSize);
        } else {
            boolean needTranslation = true;
            if (card.isToken()) {
                if (card.getCloneOrigin() == null)
                    needTranslation = false;
            }
            final String text = !card.isSplitCard() ?
                card.getText(state, needTranslation ? CardTranslation.getTranslationTexts(state.getName(), "") : null) :
                card.getText(state, needTranslation ? CardTranslation.getTranslationTexts(card.getLeftSplitState().getName(), card.getRightSplitState().getName()) : null );
            if (StringUtils.isEmpty(text))
                return;

            float padding = TEXT_METRICS.getAscent() * 0.25f;
            x += padding;
            w -= 2 * padding;
            drawTextBoxText(g, text, Math.round(x), Math.round(y), Math.round(w), Math.round(h), hasPTBox);
        }
    }

    private static void drawPtBox(Graphics2D g, CardView card, CardStateView state, Color[] colors, float x, float y, float w, float h) {
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
            pieces.add("[");
            pieces.add(String.valueOf(state.getPower()));
            pieces.add("/");
            pieces.add(String.valueOf(state.getToughness()));
            pieces.add("]");
        }
        else { return; }

        float padding = PT_METRICS.getAscent() / 4f;
        float totalPieceWidth = -padding;
        float[] pieceWidths = new float[pieces.size()];
        for (int i = 0; i < pieces.size(); i++) {
            float pieceWidth = PT_METRICS.stringWidth(pieces.get(i)) + padding;
            pieceWidths[i] = pieceWidth;
            totalPieceWidth += pieceWidth;
        }
        float boxHeight = PT_METRICS.getMaxAscent() + padding;

        float boxWidth = Math.max(PT_BOX_WIDTH, totalPieceWidth + 2 * padding);
        x += w - boxWidth;
        y += h - boxHeight;
        w = boxWidth;
        h = boxHeight;

        fillColorBackground(g, colors, x, y, w, h);
        g.setStroke(new BasicStroke(BORDER_THICKNESS));
        g.setColor(Color.BLACK);
        g.drawRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h));

        x += (boxWidth - totalPieceWidth) / 2;
        for (int i = 0; i < pieces.size(); i++) {
            drawVerticallyCenteredString(g, pieces.get(i),
                new Rectangle(Math.round(x), Math.round(y), Math.round(w), Math.round(h)),
                PT_FONT, PT_METRICS);
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
        public abstract void drawPrev(Graphics2D g, int x, int y, Font txFont, Font rmFont, FontMetrics txMetrics);
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

        public void drawPrev(Graphics2D g, int x, int y, Font txFont, Font rmFont, FontMetrics txMetrics) {
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

        public void drawPrev(Graphics2D g, int x, int y, Font txFont, Font rmFont, FontMetrics txMetrics) {
            int xoffset = Math.round(txMetrics.getAscent() * 0.8f);
            int yoffset = txMetrics.getAscent() - xoffset + 1;
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
                symbols.add(sbMatcher.group(1) != null ? sbMatcher.group(1) : sbMatcher.group(2) + sbMatcher.group(3));
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

        public int calculateLines(int width, FontMetrics txMetrics, FontMetrics rmMetrics, boolean hasPTBox) {
            int pos = 0;
            int lines = 1;
            for (Piece p : pieces) {
                p.restart();
                int w = p.getNextWidth(txMetrics, rmMetrics);
                while (w != -1) {
                    if (pos + w > width) {
                        ++lines;
                        pos = 0;
                    }
                    pos += w;
                    w = p.getNextWidth(txMetrics, rmMetrics);
                }
            }
            // If last line will overlapp with PT box, add one more line.
            if (hasPTBox && pos >= width - PT_BOX_WIDTH)
                ++lines;
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
                    if (pos + w > width) {
                        ++lines;
                        pos = 0;
                        y += lineHeight;
                    }
                    p.drawPrev(g, x + pos, y, txFont, rmFont, txMetrics);
                    pos += w;
                    w = p.getNextWidth(txMetrics, rmMetrics);
                }
            }
            return lines * lineHeight;
        }
    }

    private static void drawTextBoxText(Graphics2D g, final String text, int x, int y, int w, int h, boolean hasPTBox) {
        String [] paragraphs = linebreakPattern.split(text);
        List<Paragraph> pgList = new ArrayList<>();
        for (String pg : paragraphs) {
            pgList.add(new Paragraph(pg));
        }

        // Find font size that fit in the text box area
        Font txFont = TEXT_FONT, rmFont = REMINDER_FONT;
        FontMetrics txMetrics = TEXT_METRICS, rmMetrics = REMINDER_METRICS;
        int txFontSize = txFont.getSize(), rmFontSize = rmFont.getSize();
        int lineHeight, paraSpacing, lineSpacing, totalHeight;
        do {
            int totalLineSpacings = 0;
            totalHeight = 0;
            paraSpacing = txMetrics.getLeading() + txMetrics.getDescent();
            lineHeight = txMetrics.getAscent() + txMetrics.getDescent();
            lineSpacing = -2;
            for (int i = 0; i < pgList.size(); ++i) {
                boolean ptBox = (i < pgList.size() - 1) ? false : hasPTBox;
                Paragraph pg = pgList.get(i);
                totalHeight += paraSpacing;
                int lines = pg.calculateLines(w, txMetrics, rmMetrics, ptBox);
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
        y += (h - totalHeight - paraSpacing / 2) / 2;
        for (Paragraph pg : pgList) {
            y += pg.drawPieces(g, x, y, w, lineSpacing + lineHeight, txFont, txMetrics, rmFont, rmMetrics);
            y += paraSpacing - lineSpacing;
        }
    }
}
