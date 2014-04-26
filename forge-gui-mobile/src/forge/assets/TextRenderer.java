package forge.assets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import forge.Forge.Graphics;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;

//Encodes text for drawing with symbols and reminder text
public class TextRenderer {
    private static final Map<String, FSkinImage> symbolLookup = new HashMap<String, FSkinImage>();
    static {
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
        symbolLookup.put("C", FSkinImage.CHAOS);
        symbolLookup.put("Q", FSkinImage.UNTAP);
        symbolLookup.put("S", FSkinImage.MANA_SNOW);
        symbolLookup.put("T", FSkinImage.TAP);
    }

    private final boolean parseReminderText;
    private String fullText = "";
    private float width, height, totalHeight;
    private BitmapFont baseBitmapFont;
    private FSkinFont baseFont, font;
    private boolean wrap, needClip;
    private List<Piece> pieces = new ArrayList<Piece>();

    public TextRenderer() {
        this(false);
    }
    public TextRenderer(boolean parseReminderText0) {
        parseReminderText = parseReminderText0;
    }

    //break text in pieces
    private void updatePieces(FSkinFont font0) {
        pieces.clear();
        font = font0;
        needClip = false;
        if (fullText.isEmpty()) { return; }

        BitmapFont bitmapFont = font.getFont();
        totalHeight = bitmapFont.getCapHeight();
        if (totalHeight > height) {
            //immediately try one font size smaller if no room for anything
            if (font.getSize() > FSkinFont.MIN_FONT_SIZE) {
                updatePieces(FSkinFont.get(font.getSize() - 1));
                return;
            }
            needClip = true;
        }

        boolean hideReminderText = FModel.getPreferences().getPrefBoolean(FPref.UI_HIDE_REMINDER_TEXT);

        char ch;
        float x = 0;
        float y = 0;
        float pieceWidth = 0;
        int lastSpaceIdx = -1;
        float lineHeight = bitmapFont.getLineHeight();
        String text = "";
        int inSymbolCount = 0;
        boolean atReminderTextEnd = false;
        int inReminderTextCount = 0;
        for (int i = 0; i < fullText.length(); i++) {
            atReminderTextEnd = false;
            ch = fullText.charAt(i);
            switch (ch) {
            case '\n':
                if (inSymbolCount > 0) {
                    inSymbolCount = 0;
                    text = "{" + text; //if not a symbol, render as text
                }
                if (!text.isEmpty()) {
                    addPiece(new TextPiece(text, inReminderTextCount > 0), x, y, pieceWidth, lineHeight);
                    pieceWidth = 0;
                    text = "";
                }
                x = 0;
                y += lineHeight;
                totalHeight += lineHeight;
                if (totalHeight > height) {
                    //try next font size down if out of space
                    if (font.getSize() > FSkinFont.MIN_FONT_SIZE) {
                        updatePieces(FSkinFont.get(font.getSize() - 1));
                        return;
                    }
                    needClip = true;
                }
                continue; //skip new line character
            case '{':
                if (inSymbolCount == 0 && !text.isEmpty()) { //add current text if just entering symbol
                    addPiece(new TextPiece(text, inReminderTextCount > 0), x, y, pieceWidth, lineHeight);
                    x += pieceWidth;
                    pieceWidth = 0;
                    text = "";
                }
                inSymbolCount++;
                continue; //skip '{' character
            case '}':
                if (inSymbolCount > 0) {
                    inSymbolCount--;
                    if (!text.isEmpty()) {
                        FSkinImage symbol = symbolLookup.get(text);
                        if (symbol != null) {
                            pieceWidth = lineHeight * 0.85f;
                            if (x + pieceWidth > width) {
                                if (wrap) {
                                    x = 0;
                                    y += lineHeight;
                                    totalHeight += lineHeight;
                                    if (totalHeight > height) {
                                        //try next font size down if out of space
                                        if (font.getSize() > FSkinFont.MIN_FONT_SIZE) {
                                            updatePieces(FSkinFont.get(font.getSize() - 1));
                                            return;
                                        }
                                        needClip = true;
                                    }
                                    //make previous consecutive symbols wrap too
                                    for (int j = pieces.size(); j >= 0; j--) {
                                        Piece piece = pieces.get(j);
                                        if (piece instanceof ImagePiece) {
                                            piece.x = x;
                                            piece.y += lineHeight;
                                            x += piece.w;
                                        }
                                        else { break; }
                                    }
                                }
                                else if (font.getSize() > FSkinFont.MIN_FONT_SIZE) {
                                    //try next font size down if out of space
                                    updatePieces(FSkinFont.get(font.getSize() - 1));
                                    return;
                                }
                                else {
                                    needClip = true;
                                }
                            }
                            addPiece(new ImagePiece(symbol, inReminderTextCount > 0), x, y - bitmapFont.getAscent() + (lineHeight - pieceWidth) / 2, pieceWidth, pieceWidth);
                            x += pieceWidth;
                            text = "";
                            continue; //skip '}' character
                        }
                    }
                    text = "{" + text; //if not a symbol, render as text
                    lastSpaceIdx++;
                }
                break;
            case '(':
                if (inSymbolCount > 0) {
                    inSymbolCount = 0;
                    text = "{" + text; //if not a symbol, render as text
                }
                if (parseReminderText) {
                    if (inReminderTextCount == 0 && !text.isEmpty()) { //add current text if just entering reminder text
                        addPiece(new TextPiece(text, false), x, y, pieceWidth, lineHeight);
                        x += pieceWidth;
                        pieceWidth = 0;
                        text = "";
                        lastSpaceIdx = -1;
                    }
                    inReminderTextCount++;
                }
                break;
            case ')':
                if (inSymbolCount > 0) {
                    inSymbolCount = 0;
                    text = "{" + text; //if not a symbol, render as text
                }
                if (inReminderTextCount > 0) {
                    inReminderTextCount--;
                    if (inReminderTextCount == 0) {
                        atReminderTextEnd = true;
                    }
                }
                break;
            case ' ':
                if (inSymbolCount > 0) {
                    inSymbolCount = 0;
                    text = "{" + text; //if not a symbol, render as text
                }
                lastSpaceIdx = text.length();
                break;
            }
            if (hideReminderText && (inReminderTextCount > 0 || atReminderTextEnd)) {
                continue;
            }
            text += ch;
            if (inSymbolCount == 0) {
                pieceWidth = bitmapFont.getBounds(text).width;
                if (x + pieceWidth > width) { //wrap or shrink if needed
                    if (wrap && lastSpaceIdx >= 0) {
                        String currentLineText = text.substring(0, lastSpaceIdx);
                        if (!currentLineText.isEmpty()) {
                            addPiece(new TextPiece(currentLineText, inReminderTextCount > 0 || atReminderTextEnd), x, y, pieceWidth, lineHeight);
                        }
                        text = text.substring(lastSpaceIdx + 1);
                        lastSpaceIdx = -1;
                        pieceWidth = text.isEmpty() ? 0 : bitmapFont.getBounds(text).width;
                        x = 0;
                        y += lineHeight;
                        totalHeight += lineHeight;
                        if (totalHeight > height) {
                            //try next font size down if out of space
                            if (font.getSize() > FSkinFont.MIN_FONT_SIZE) {
                                updatePieces(FSkinFont.get(font.getSize() - 1));
                                return;
                            }
                            needClip = true;
                        }
                    }
                    else if (font.getSize() > FSkinFont.MIN_FONT_SIZE) {
                        //try next font size down if out of space
                        updatePieces(FSkinFont.get(font.getSize() - 1));
                        return;
                    }
                    else {
                        needClip = true;
                    }
                }
                if (atReminderTextEnd && !text.isEmpty()) { //ensure final piece of reminder text added right away
                    addPiece(new TextPiece(text, true), x, y, pieceWidth, lineHeight);
                    x += pieceWidth;
                    pieceWidth = 0;
                    text = "";
                    lastSpaceIdx = -1;
                }
            }
        }

        if (!text.isEmpty()) {
            addPiece(new TextPiece(text, inReminderTextCount > 0), x, y, pieceWidth, lineHeight);
        }
    }

    private void addPiece(Piece piece, float x, float y, float w, float h) {
        piece.x = x;
        piece.y = y;
        piece.w = w;
        piece.h = h;
        pieces.add(piece);
    }

    public void drawText(Graphics g, String text0, FSkinFont skinFont, FSkinColor skinColor, float x, float y, float w, float h, boolean wrap0, HAlignment horzAlignment, boolean centerVertically) {
        drawText(g, text0, skinFont, skinColor.getColor(), x, y, w, h, wrap, horzAlignment, centerVertically);
    }
    public void drawText(Graphics g, String text, FSkinFont skinFont, Color color, float x, float y, float w, float h, boolean wrap0, HAlignment horzAlignment, boolean centerVertically) {
        boolean needUpdate = false;
        if (!fullText.equals(text)) {
            fullText = text;
            needUpdate = true;
        }
        if (skinFont != baseFont || skinFont.getFont() != baseBitmapFont) {
            baseFont = skinFont;
            baseBitmapFont = skinFont.getFont(); //cache baseBitmapFont separate to handle skin changes
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
        if (needClip) { //prevent text flowing outside region if couldn't shrink it to fit
            g.startClip(x, y, w, h);
        }
        if (height > totalHeight && centerVertically) {
            y += (height - totalHeight) / 2;
        }
        for (Piece piece : pieces) {
            piece.draw(g, color, x, y);
        }
        if (needClip) {
            g.endClip();
        }
    }

    private abstract class Piece {
        protected float x, y, w, h;
        protected final boolean inReminderText;
        protected final static float ALPHA_COMPOSITE = 0.5f;

        protected Piece(boolean inReminderText0) {
            inReminderText = inReminderText0;
        }

        public abstract void draw(Graphics g, Color color, float offsetX, float offsetY);
    }

    private class TextPiece extends Piece {
        private final String text;

        private TextPiece(String text0, boolean inReminderText0) {
            super(inReminderText0);
            text = text0;
        }

        @Override
        public void draw(Graphics g, Color color, float offsetX, float offsetY) {
            g.drawText(text, font, inReminderText ? FSkinColor.alphaColor(color, ALPHA_COMPOSITE) : color, x + offsetX, y + offsetY, w, h, false, HAlignment.LEFT, false);
        }
    }

    private class ImagePiece extends Piece {
        private FSkinImage image;

        private ImagePiece(FSkinImage image0, boolean inReminderText0) {
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
