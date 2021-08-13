package forge.assets;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.card.CardFaceSymbols;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.TextBounds;

//Encodes text for drawing with symbols and reminder text
public class TextRenderer {
    private static final Map<String, FSkinImage> symbolLookup = new HashMap<>(64);
    static {
        symbolLookup.put("C", FSkinImage.MANA_COLORLESS);
        symbolLookup.put("W", FSkinImage.MANA_W);
        symbolLookup.put("U", FSkinImage.MANA_U);
        symbolLookup.put("B", FSkinImage.MANA_B);
        symbolLookup.put("R", FSkinImage.MANA_R);
        symbolLookup.put("G", FSkinImage.MANA_G);
        symbolLookup.put("W/U", FSkinImage.MANA_HYBRID_WU);
        symbolLookup.put("U/B", FSkinImage.MANA_HYBRID_UB);
        symbolLookup.put("B/R", FSkinImage.MANA_HYBRID_BR);
        symbolLookup.put("R/G", FSkinImage.MANA_HYBRID_RG);
        symbolLookup.put("G/W", FSkinImage.MANA_HYBRID_GW);
        symbolLookup.put("W/B", FSkinImage.MANA_HYBRID_WB);
        symbolLookup.put("U/R", FSkinImage.MANA_HYBRID_UR);
        symbolLookup.put("B/G", FSkinImage.MANA_HYBRID_BG);
        symbolLookup.put("R/W", FSkinImage.MANA_HYBRID_RW);
        symbolLookup.put("G/U", FSkinImage.MANA_HYBRID_GU);
        symbolLookup.put("2/W", FSkinImage.MANA_2W);
        symbolLookup.put("2/U", FSkinImage.MANA_2U);
        symbolLookup.put("2/B", FSkinImage.MANA_2B);
        symbolLookup.put("2/R", FSkinImage.MANA_2R);
        symbolLookup.put("2/G", FSkinImage.MANA_2G);
        symbolLookup.put("P", FSkinImage.MANA_PHRYX);
        symbolLookup.put("P/W", FSkinImage.MANA_PHRYX_W);
        symbolLookup.put("P/U", FSkinImage.MANA_PHRYX_U);
        symbolLookup.put("P/B", FSkinImage.MANA_PHRYX_B);
        symbolLookup.put("P/R", FSkinImage.MANA_PHRYX_R);
        symbolLookup.put("P/G", FSkinImage.MANA_PHRYX_G);
        symbolLookup.put("W/P", FSkinImage.MANA_PHRYX_W);
        symbolLookup.put("U/P", FSkinImage.MANA_PHRYX_U);
        symbolLookup.put("B/P", FSkinImage.MANA_PHRYX_B);
        symbolLookup.put("R/P", FSkinImage.MANA_PHRYX_R);
        symbolLookup.put("G/P", FSkinImage.MANA_PHRYX_G);
        for (int i = 0; i <= 20; i++) {
            symbolLookup.put(String.valueOf(i), FSkinImage.valueOf("MANA_" + i));
        }
        symbolLookup.put("X", FSkinImage.MANA_X);
        symbolLookup.put("Y", FSkinImage.MANA_Y);
        symbolLookup.put("Z", FSkinImage.MANA_Z);
        symbolLookup.put("CHAOS", FSkinImage.CHAOS);
        symbolLookup.put("Q", FSkinImage.UNTAP);
        symbolLookup.put("S", FSkinImage.MANA_SNOW);
        symbolLookup.put("T", FSkinImage.TAP);
        symbolLookup.put("E", FSkinImage.ENERGY);
        symbolLookup.put("AE", FSkinImage.AETHER_SHARD);
        symbolLookup.put("PW", FSkinImage.PW_BADGE_COMMON);
        symbolLookup.put("CR", FSkinImage.QUEST_COINSTACK);
    }

    public static String startColor(Color color) {
        return "<clr " + Color.rgba8888(color) + ">";
    }
    public static String endColor() {
        return "</clr>";
    }

    private final boolean parseReminderText;
    private String fullText = "";
    private float width, height, totalHeight;
    private FSkinFont baseFont, font;
    private boolean wrap, needClip;
    private List<Piece> pieces = new ArrayList<>();
    private List<Float> lineWidths = new ArrayList<>();
    private BreakIterator boundary = BreakIterator.getLineInstance(new Locale(Forge.locale));

    public TextRenderer() {
        this(false);
    }
    public TextRenderer(boolean parseReminderText0) {
        parseReminderText = parseReminderText0;
    }

    //break text in pieces
    private void updatePieces(FSkinFont font0) {
        pieces.clear();
        lineWidths.clear();
        font = font0;
        needClip = false;
        if (fullText == null || fullText.isEmpty()) { return; }

        totalHeight = font.getCapHeight();
        if (totalHeight > height) {
            //immediately try one font size smaller if no room for anything
            if (font.canShrink()) {
                updatePieces(font.shrink());
                return;
            }
            needClip = true;
        }

        boundary.setText(fullText);
        ForgePreferences prefs = FModel.getPreferences();
        boolean hideReminderText = prefs != null && prefs.getPrefBoolean(FPref.UI_HIDE_REMINDER_TEXT);

        char ch;
        float x = 0;
        float y = 0;
        float pieceWidth = 0;
        float lineHeight = font.getLineHeight();
        int nextSpaceIdx = boundary.first();
        int lastSpaceIdx = -1;
        int lineNum = 0;
        StringBuilder text = new StringBuilder();
        int inSymbolCount = 0;
        int consecutiveSymbols = 0;
        int inKeywordCount = 0;
        boolean atReminderTextEnd = false;
        int inReminderTextCount = 0;
        Color colorOverride = null;

        for (int i = 0; i < fullText.length(); i++) {
            atReminderTextEnd = false;
            if (i == nextSpaceIdx) {
                lastSpaceIdx = text.length();
                nextSpaceIdx = boundary.next();
            }
            ch = fullText.charAt(i);
            switch (ch) {
            case '\r':
                continue; //skip '\r' character
            case '\n':
                if (inSymbolCount > 0) {
                    inSymbolCount = 0;
                    text.insert(0, '{'); //if not a symbol, render as text
                }
                lineWidths.add(x + pieceWidth);
                if (text.length() > 0) {
                    addPiece(new TextPiece(text.toString(), colorOverride, inReminderTextCount > 0), lineNum, x, y, pieceWidth, lineHeight);
                    pieceWidth = 0;
                    text.setLength(0);
                    consecutiveSymbols = 0;
                }
                lastSpaceIdx = -1;
                x = 0;
                y += lineHeight;
                totalHeight += lineHeight;
                lineNum++;
                if (totalHeight > height) {
                    //try next font size down if out of space
                    if (font.canShrink()) {
                        updatePieces(font.shrink());
                        return;
                    }
                    needClip = true;
                }
                continue; //skip new line character
            case '{':
                if (inSymbolCount == 0 && text.length() > 0) { //add current text if just entering symbol
                    addPiece(new TextPiece(text.toString(), colorOverride,inReminderTextCount > 0), lineNum, x, y, pieceWidth, lineHeight);
                    x += pieceWidth;
                    pieceWidth = 0;
                    text.setLength(0);
                    lastSpaceIdx = -1;
                    consecutiveSymbols = 0;
                }
                inSymbolCount++;
                continue; //skip '{' character
            case '}':
                if (inSymbolCount > 0) {
                    inSymbolCount--;
                    if (text.length() > 0) {
                        FSkinImage symbol = symbolLookup.get(text.toString());
                        if (symbol != null) {
                            pieceWidth = lineHeight * CardFaceSymbols.FONT_SIZE_FACTOR;
                            if (x + pieceWidth > width) {
                                if (wrap) {
                                    y += lineHeight;
                                    totalHeight += lineHeight;
                                    lineNum++;
                                    if (totalHeight > height) {
                                        //try next font size down if out of space
                                        if (font.canShrink()) {
                                            updatePieces(font.shrink());
                                            return;
                                        }
                                        needClip = true;
                                    }
                                    if (consecutiveSymbols == 0) {
                                        lineWidths.add(x);
                                        x = 0;
                                    }
                                    else { //make previous consecutive symbols wrap too if needed
                                        x = 0;
                                        int startSymbolIdx = pieces.size() - consecutiveSymbols;
                                        lineWidths.add(pieces.get(startSymbolIdx).x);
                                        for (int j = startSymbolIdx; j < pieces.size(); j++) {
                                            Piece piece = pieces.get(j);
                                            piece.x = x;
                                            piece.y += lineHeight;
                                            piece.lineNum++;
                                            x += piece.w;
                                        }
                                    }
                                }
                                else if (font.canShrink()) {
                                    //try next font size down if out of space
                                    updatePieces(font.shrink());
                                    return;
                                }
                                else {
                                    needClip = true;
                                }
                            }
                            addPiece(new SymbolPiece(symbol, inReminderTextCount > 0), lineNum, x, y - font.getAscent() + (lineHeight - pieceWidth) / 2, pieceWidth, pieceWidth);
                            x += pieceWidth;
                            pieceWidth = 0;
                            text.setLength(0);
                            lastSpaceIdx = -1;
                            consecutiveSymbols++;
                            continue; //skip '}' character
                        }
                    }
                    if (!hideReminderText || inReminderTextCount == 0) {
                        text.insert(0, '{'); //if not a symbol, render as text
                        if (lastSpaceIdx >= 0) {
                            lastSpaceIdx++;
                        }
                    }
                }
                break;
            case '<':
                if (inSymbolCount > 0) {
                    inSymbolCount = 0;
                    text.insert(0, '{'); //if not a symbol, render as text
                    if (lastSpaceIdx >= 0) {
                        lastSpaceIdx++;
                    }
                }
                if (inKeywordCount == 0 && text.length() > 0) { //add current text if starting a keyword
                    addPiece(new TextPiece(text.toString(), colorOverride,false), lineNum, x, y, pieceWidth, lineHeight);
                    x += pieceWidth;
                    pieceWidth = 0;
                    text.setLength(0);
                    lastSpaceIdx = -1;
                    consecutiveSymbols = 0;
                }
                inKeywordCount++;
                break;
            case '>':
                if (inSymbolCount > 0) {
                    inSymbolCount = 0;
                    text.insert(0, '{'); //if not a symbol, render as text
                    if (lastSpaceIdx >= 0) {
                        lastSpaceIdx++;
                    }
                }
                if (inKeywordCount > 0) {
                    inKeywordCount--;
                    if (inKeywordCount == 0 && text.length() > 0) {
                        String keyword, value;
                        text.deleteCharAt(0); //trim leading '<'
                        if (text.charAt(0) == '/') {
                            keyword = text.substring(1);
                            value = null;
                        }
                        else {
                            int idx = text.indexOf(" ");
                            if (idx != -1) {
                                keyword = text.substring(0, idx);
                                value = text.substring(idx + 1);
                            }
                            else {
                                keyword = text.toString();
                                value = null;
                            }
                        }
                        boolean validKeyword = true;
                        switch (keyword) {
                        case "clr":
                            colorOverride = value != null ? new Color(Integer.parseInt(value)) : null;
                            break;
                        case "span":
                            // <span style="color:gray;">
                            if (value != null && value.contains("color:")) {
                                int startIdx = value.indexOf(':') + 1;
                                int endIdx = value.indexOf(';');
                                String colorName = value.substring(startIdx, endIdx);
                                if (colorName.equals("gray")) {
                                    colorOverride = Color.GRAY;
                                }
                            } else {
                                colorOverride = null;
                            }
                            break;
                        default:
                            validKeyword = false;
                            break;
                        }
                        if (validKeyword) {
                            text.setLength(0);
                            lastSpaceIdx = -1;
                            continue; //skip '>' character
                        }
                    }
                }
                break;
            case '(':
                if (inSymbolCount > 0) {
                    inSymbolCount = 0;
                    text.insert(0, '{'); //if not a symbol, render as text
                    if (lastSpaceIdx >= 0) {
                        lastSpaceIdx++;
                    }
                }
                if (parseReminderText) {
                    if (inReminderTextCount == 0 && text.length() > 0) { //add current text if just entering reminder text
                        addPiece(new TextPiece(text.toString(), colorOverride,false), lineNum, x, y, pieceWidth, lineHeight);
                        x += pieceWidth;
                        pieceWidth = 0;
                        text.setLength(0);
                        lastSpaceIdx = -1;
                        consecutiveSymbols = 0;
                    }
                    inReminderTextCount++;
                }
                break;
            case ')':
                if (inSymbolCount > 0) {
                    inSymbolCount = 0;
                    text.insert(0, '{'); //if not a symbol, render as text
                    if (lastSpaceIdx >= 0) {
                        lastSpaceIdx++;
                    }
                }
                if (inReminderTextCount > 0) {
                    inReminderTextCount--;
                    if (inReminderTextCount == 0) {
                        atReminderTextEnd = true;
                    }
                }
                break;
            case ' ':
                if (inKeywordCount == 0 && inSymbolCount > 0) {
                    inSymbolCount = 0;
                    text.insert(0, '{'); //if not a symbol, render as text
                    if (lastSpaceIdx >= 0) {
                        lastSpaceIdx++;
                    }
                }
                break;
            }
            if (hideReminderText && (inReminderTextCount > 0 || atReminderTextEnd)) {
                continue;
            }
            text.append(ch);
            if (inSymbolCount == 0 && inKeywordCount == 0) {
                pieceWidth = font.getBounds(text).width;
                if (x + pieceWidth > width) { //wrap or shrink if needed
                    if (wrap && (lastSpaceIdx >= 0 || consecutiveSymbols > 0)) {
                        if (lastSpaceIdx < 0) {
                            //no space between symbols and end of line, wrap those symbols along with text
                            x = 0;
                            int startSymbolIdx = pieces.size() - consecutiveSymbols;
                            lineWidths.add(pieces.get(startSymbolIdx).x);
                            for (int j = startSymbolIdx; j < pieces.size(); j++) {
                                Piece piece = pieces.get(j);
                                piece.x = x;
                                piece.y += lineHeight;
                                piece.lineNum++;
                                x += piece.w;
                            }
                        }
                        else {
                            int endIdx = lastSpaceIdx;
                            if (lastSpaceIdx > 0 && text.charAt(lastSpaceIdx - 1) == ' ') {
                                endIdx = lastSpaceIdx - 1;
                            }
                            String currentLineText = text.substring(0, endIdx);
                            if (!currentLineText.isEmpty()) {
                                pieceWidth = font.getBounds(currentLineText).width;
                                addPiece(new TextPiece(currentLineText, colorOverride,inReminderTextCount > 0 || atReminderTextEnd), lineNum, x, y, pieceWidth, lineHeight);
                                consecutiveSymbols = 0;
                            }
                            else {
                                pieceWidth = 0;
                            }
                            lineWidths.add(x + pieceWidth);
                            text.delete(0, lastSpaceIdx);
                            x = 0;
                        }
                        lastSpaceIdx = -1;
                        pieceWidth = text.length() == 0 ? 0 : font.getBounds(text).width;
                        y += lineHeight;
                        totalHeight += lineHeight;
                        lineNum++;
                        if (totalHeight > height) {
                            //try next font size down if out of space
                            if (font.canShrink()) {
                                updatePieces(font.shrink());
                                return;
                            }
                            needClip = true;
                        }
                    }
                    else if (x > 0 && pieceWidth <= width) {
                        //if current piece starting past beginning of line and no spaces found,
                        //wrap current piece being built up along with part of previous pieces as needed
                        int lastPieceIdx;
                        for (lastPieceIdx = pieces.size() - 1; lastPieceIdx >= 0; lastPieceIdx--) {
                            Piece lastPiece = pieces.get(lastPieceIdx);
                            if (lastPiece.lineNum < lineNum) {
                                lastPieceIdx = pieces.size() - 1; //don't re-wrap anything if reached previous line
                                break;
                            }
                            if (lastPiece instanceof TextPiece) {
                                TextPiece textPiece = (TextPiece)lastPiece;
                                int index = textPiece.text.lastIndexOf(' ');
                                if (index != -1) {
                                    if (index == 0) {
                                        textPiece.text = textPiece.text.substring(1);
                                        textPiece.w = font.getBounds(textPiece.text).width;
                                        lastPieceIdx--;
                                    }
                                    else if (index == textPiece.text.length() - 1) {
                                        textPiece.text = textPiece.text.substring(0, textPiece.text.length() - 1);
                                        textPiece.w = font.getBounds(textPiece.text).width;
                                    }
                                    else {
                                        TextPiece splitPiece = new TextPiece(textPiece.text.substring(index + 1), textPiece.colorOverride, textPiece.inReminderText);
                                        textPiece.text = textPiece.text.substring(0, index);
                                        textPiece.w = font.getBounds(textPiece.text).width;
                                        splitPiece.x = textPiece.x + textPiece.w;
                                        splitPiece.y = textPiece.y;
                                        splitPiece.w = font.getBounds(splitPiece.text).width;
                                        splitPiece.h = textPiece.h;
                                    }
                                    break;
                                }
                            }
                        }
                        if (lastPieceIdx >= 0) {
                            Piece lastPiece = pieces.get(lastPieceIdx);
                            lineWidths.add(lastPiece.x + lastPiece.w);
                            x = 0;
                            for (int j = lastPieceIdx + 1; j < pieces.size(); j++) {
                                Piece piece = pieces.get(j);
                                piece.x = x;
                                piece.y += lineHeight;
                                piece.lineNum++;
                                x += piece.w;
                            }
                            y += lineHeight;
                            totalHeight += lineHeight;
                            lineNum++;
                            if (totalHeight > height) {
                                //try next font size down if out of space
                                if (font.canShrink()) {
                                    updatePieces(font.shrink());
                                    return;
                                }
                                needClip = true;
                            }
                        } else {
                            if (font.canShrink()) {
                                //try next font size down if out of space
                                updatePieces(font.shrink());
                                return;
                            }
                            needClip = true;
                        }
                    }
                    else {
                        if (font.canShrink()) {
                            //try next font size down if out of space
                            updatePieces(font.shrink());
                            return;
                        }
                        needClip = true;
                    }
                }
                if (atReminderTextEnd && text.length() > 0) { //ensure final piece of reminder text added right away
                    addPiece(new TextPiece(text.toString(), colorOverride, true), lineNum, x, y, pieceWidth, lineHeight);
                    x += pieceWidth;
                    pieceWidth = 0;
                    text.setLength(0);
                    lastSpaceIdx = -1;
                    consecutiveSymbols = 0;
                }
            }
        }

        lineWidths.add(x + pieceWidth);
        if (text.length() > 0) {
            addPiece(new TextPiece(text.toString(), colorOverride, inReminderTextCount > 0), lineNum, x, y, pieceWidth, lineHeight);
            consecutiveSymbols = 0;
        }
    }

    private void addPiece(Piece piece, int lineNum, float x, float y, float w, float h) {
        piece.lineNum = lineNum;
        piece.x = x;
        piece.y = y;
        piece.w = w;
        piece.h = h;
        pieces.add(piece);
    }

    private void setProps(String text, FSkinFont skinFont, float w, float h, boolean wrap0) {
        boolean needUpdate = false;
        if (!fullText.equals(text)) {
            fullText = text;
            needUpdate = true;
        }
        if (skinFont != baseFont) {
            baseFont = skinFont;
            needUpdate = true;
        }
        if (width != w) {
            width = w;
            needUpdate = true;
        }
        if (height != h) {
            height = h;
            needUpdate = true;
        }
        if (wrap != wrap0) {
            wrap = wrap0;
            needUpdate = true;
        }
        if (needUpdate) {
            updatePieces(baseFont);
        }
    }

    private TextBounds getCurrentBounds() {
        float maxWidth = 0;
        for (Float lineWidth : lineWidths) {
            if (lineWidth > maxWidth) {
                maxWidth = lineWidth;
            }
        }
        TextBounds bounds = new TextBounds();
        bounds.width = maxWidth;
        bounds.height = totalHeight;
        return bounds;
    }

    public TextBounds getBounds(String text, FSkinFont skinFont) {
        setProps(text, skinFont, Float.MAX_VALUE, Float.MAX_VALUE, false);
        return getCurrentBounds();
    }
    public TextBounds getWrappedBounds(String text, FSkinFont skinFont, float maxWidth) {
        setProps(text, skinFont, maxWidth, Float.MAX_VALUE, true);
        return getCurrentBounds();
    }

    public void drawText(Graphics g, String text, FSkinFont skinFont, FSkinColor skinColor, float x, float y, float w, float h, float visibleStartY, float visibleHeight, boolean wrap0, int horzAlignment, boolean centerVertically) {
        drawText(g, text, skinFont, skinColor.getColor(), x, y, w, h, visibleStartY, visibleHeight, wrap0, horzAlignment, centerVertically);
    }
    public void drawText(Graphics g, String text, FSkinFont skinFont, Color color, float x, float y, float w, float h, float visibleStartY, float visibleHeight, boolean wrap0, int horzAlignment, boolean centerVertically) {
        setProps(text, skinFont, w, h, wrap0);
        if (needClip) { //prevent text flowing outside region if couldn't shrink it to fit
            g.startClip(x, y, w, h);
        }
        if (height > totalHeight && centerVertically) {
            y += (height - totalHeight) / 2;
        }
        float[] alignmentOffsets = new float[lineWidths.size()];
        for (int i = 0; i < lineWidths.size(); i++) {
            switch (horzAlignment) {
            case Align.left:
                alignmentOffsets[i] = 0;
                break;
            case Align.center:
                alignmentOffsets[i] = Math.max((width - lineWidths.get(i)) / 2, 0);
                break;
            case Align.right:
                alignmentOffsets[i] = Math.max(width - lineWidths.get(i), 0);
                break;
            }
        }

        visibleStartY -= y; //subtract y to make calculation quicker
        float visibleEndY = visibleStartY + visibleHeight;

        for (Piece piece : pieces) {
            if (piece.y + piece.h < visibleStartY) {
                continue;
            }
            if (piece.y >= visibleEndY) {
                break;
            }
            piece.draw(g, color, x + alignmentOffsets[piece.lineNum], y);
        }
        if (needClip) {
            g.endClip();
        }
    }

    private abstract class Piece {
        protected static final float ALPHA_COMPOSITE = 0.5f;

        protected final boolean inReminderText;
        protected float x, y, w, h;
        protected int lineNum;

        protected Piece(boolean inReminderText0) {
            inReminderText = inReminderText0;
        }

        public abstract void draw(Graphics g, Color color, float offsetX, float offsetY);
    }

    private class TextPiece extends Piece {
        private String text;
        private Color colorOverride;

        private TextPiece(String text0, Color colorOverride0, boolean inReminderText0) {
            super(inReminderText0);
            text = text0;
            colorOverride = colorOverride0;
        }

        @Override
        public void draw(Graphics g, Color color, float offsetX, float offsetY) {
            if (colorOverride != null) {
                color = colorOverride;
            }
            else if (inReminderText) {
                color = FSkinColor.alphaColor(color, ALPHA_COMPOSITE);
            }
            g.drawText(text, font, color, x + offsetX, y + offsetY, w, h, false, Align.left, false);
        }
    }

    private class SymbolPiece extends Piece {
        private FSkinImage image;

        private SymbolPiece(FSkinImage image0, boolean inReminderText0) {
            super(inReminderText0);
            image = image0;
        }

        @Override
        public void draw(Graphics g, Color color, float offsetX, float offsetY) {
            if (inReminderText) {
                g.setAlphaComposite(ALPHA_COMPOSITE);
            }
            g.drawImage(image, x + offsetX, y + offsetY, w, h);
            if (inReminderText) {
                g.resetAlphaComposite();
            }
        }
    }
}
