package forge.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData;
import com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntSet;
import forge.Forge;
import forge.gui.FThreads;
import forge.localinstance.properties.ForgeConstants;
import forge.util.FileUtil;
import forge.util.LineReader;
import forge.util.TextBounds;
import forge.util.Utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class FSkinFont {
    private static final int MIN_FONT_SIZE = 8;
    private static int MAX_FONT_SIZE = 72;

    private static final int MAX_FONT_SIZE_LESS_GLYPHS = 72;
    private static final int MAX_FONT_SIZE_MANY_GLYPHS = 36;

    private static final String TTF_FILE = "font1.ttf";
    private static HashMap<String, String> langUniqueCharacterSet = new HashMap<>();

    static {
        FileUtil.ensureDirectoryExists(ForgeConstants.FONTS_DIR);
    }
    public static FSkinFont get(final int unscaledSize) {
        return _get((int)Utils.scale(unscaledSize));
    }
    public static FSkinFont _get(final int scaledSize) {
        FSkinFont skinFont = Forge.getAssets().fonts().get(scaledSize);
        if (skinFont == null) {
            skinFont = new FSkinFont(scaledSize);
            Forge.getAssets().fonts().put(scaledSize, skinFont);
        }
        return skinFont;
    }

    public static FSkinFont forHeight(final float height) {
        int size = MIN_FONT_SIZE + 1;
        while (true) {
            FSkinFont f = _get(size);
            if (f != null && f.getLineHeight() > height) {
                return _get(size - 1);
            }
            size++;
        }
    }

    //pre-load all supported font sizes
    public static void preloadAll(String language) {
        //todo:really check the language glyph is a lot
        MAX_FONT_SIZE = (language.equals("zh-CN") || language.equals("ja-JP")) ? MAX_FONT_SIZE_MANY_GLYPHS : MAX_FONT_SIZE_LESS_GLYPHS;
        for (int size = MIN_FONT_SIZE; size <= MAX_FONT_SIZE; size++) {
            _get(size);
        }
    }

    //delete all cached font files
    public static void deleteCachedFiles() {
        final FileHandle dir = Gdx.files.absolute(ForgeConstants.FONTS_DIR);
        for (FileHandle fontFile : dir.list()) {
            String name = fontFile.name();
            if (name.endsWith(".fnt") || name.endsWith(".png")) {
                fontFile.delete();
            }
        }
    }

    public static void updateAll() {
        for (FSkinFont skinFont : Forge.getAssets().fonts().values()) {
            skinFont.updateFont();
        }
    }

    private final int fontSize;
    private final float scale;
    BitmapFont font;

    private FSkinFont(int fontSize0) {
        if (fontSize0 > MAX_FONT_SIZE) {
            scale = (float)fontSize0 / MAX_FONT_SIZE;
        }
        else if (fontSize0 < MIN_FONT_SIZE) {
            scale = (float)fontSize0 / MIN_FONT_SIZE;
        }
        else {
            scale = 1;
        }
        fontSize = fontSize0;
        updateFont();
    }
    static int indexOf (CharSequence text, char ch, int start) {
        final int n = text.length();
        for (; start < n; start++)
            if (text.charAt(start) == ch) return start;
        return n;

    }
    public int computeVisibleGlyphs (CharSequence str, int start, int end, float availableWidth) {
        if (font == null)
            return 0;
        BitmapFontData data = font.getData();
        int index = start;
        float width = 0;
        Glyph lastGlyph = null;
        availableWidth /= data.scaleX;

        for (; index < end; index++) {
            char ch = str.charAt(index);
            if (ch == '[' && data.markupEnabled) {
                index++;
                if (!(index < end && str.charAt(index) == '[')) { // non escaped '['
                    while (index < end && str.charAt(index) != ']')
                        index++;
                    continue;
                }
            }

            Glyph g = data.getGlyph(ch);

            if (g != null) {
                if (lastGlyph != null) width += lastGlyph.getKerning(ch);
                if ((width + g.xadvance) - availableWidth > 0.001f) break;
                width += g.xadvance;
                lastGlyph = g;
            }
        }

        return index - start;
    }
    public boolean isBreakChar (char c) {
        BitmapFontData data = font.getData();
        if (data.breakChars == null) return false;
        for (char br : data.breakChars)
            if (c == br) return true;
        return false;
    }
    static boolean isWhitespace (char c) {
        switch (c) {
            case '\n':
            case '\r':
            case '\t':
            case ' ':
                return true;
            default:
                return false;
        }
    }
    // Expose methods from font that updates scale as needed
    public TextBounds getBounds(CharSequence str) {
        updateScale(); //must update scale before measuring text
        return getBounds(str, 0, str.length());
    }
    public TextBounds getBounds(CharSequence str, int start, int end) {
        if (font == null) {
            return new TextBounds(0f, 0f);
        }
        BitmapFontData data = font.getData();
        //int start = 0;
        //int end = str.length();
        int width = 0;
        Glyph lastGlyph = null;

        while (start < end) {
            char ch = str.charAt(start++);
            if (ch == '[' && data.markupEnabled) {
                if (!(start < end && str.charAt(start) == '[')) { // non escaped '['
                    while (start < end && str.charAt(start) != ']')
                        start++;
                    start++;
                    continue;
                }
                start++;
            }
            lastGlyph = data.getGlyph(ch);
            if (lastGlyph != null) {
                width = lastGlyph.xadvance;
                break;
            }
        }
        while (start < end) {
            char ch = str.charAt(start++);
            if (ch == '[' && data.markupEnabled) {
                if (!(start < end && str.charAt(start) == '[')) { // non escaped '['
                    while (start < end && str.charAt(start) != ']')
                        start++;
                    start++;
                    continue;
                }
                start++;
            }

            Glyph g = data.getGlyph(ch);
            if (g != null) {
                width += lastGlyph.getKerning(ch);
                lastGlyph = g;
                width += g.xadvance;
            }
        }

        return new TextBounds(width * data.scaleX, data.capHeight);

    }
    public TextBounds getMultiLineBounds(CharSequence str) {
        updateScale();
        if (font == null) {
            return new TextBounds(0f, 0f);
        }
        BitmapFontData data = font.getData();
        int start = 0;
        float maxWidth = 0;
        int numLines = 0;
        int length = str.length();

        while (start < length) {
            int lineEnd = indexOf(str, '\n', start);
            float lineWidth = getBounds(str, start, lineEnd).width;
            maxWidth = Math.max(maxWidth, lineWidth);
            start = lineEnd + 1;
            numLines++;
        }

        return new TextBounds(maxWidth, data.capHeight + (numLines - 1) * data.lineHeight);

    }
    public TextBounds getWrappedBounds(CharSequence str, float wrapWidth) {
        updateScale();
        if (font == null) {
            return new TextBounds(0f, 0f);
        }
        BitmapFontData data = font.getData();
        if (wrapWidth <= 0) wrapWidth = Integer.MAX_VALUE;
        int start = 0;
        int numLines = 0;
        int length = str.length();
        float maxWidth = 0;
        while (start < length) {
            int newLine = indexOf(str, '\n', start);
            int lineEnd = start + computeVisibleGlyphs(str, start, newLine, wrapWidth);
            int nextStart = lineEnd + 1;
            if (lineEnd < newLine) {
                // Find char to break on.
                while (lineEnd > start) {
                    if (isWhitespace(str.charAt(lineEnd))) break;
                    if (isBreakChar(str.charAt(lineEnd - 1))) break;
                    lineEnd--;
                }

                if (lineEnd == start) {

                    if (nextStart > start + 1) nextStart--;

                    lineEnd = nextStart; // If no characters to break, show all.

                } else {
                    nextStart = lineEnd;

                    // Eat whitespace at start of wrapped line.

                    while (nextStart < length) {
                        char c = str.charAt(nextStart);
                        if (!isWhitespace(c)) break;
                        nextStart++;
                        if (c == '\n') break; // Eat only the first wrapped newline.
                    }

                    // Eat whitespace at end of line.
                    while (lineEnd > start) {

                        if (!isWhitespace(str.charAt(lineEnd - 1))) break;
                        lineEnd--;
                    }
                }
            }

            if (lineEnd > start) {
                float lineWidth = getBounds(str, start, lineEnd).width;
                maxWidth = Math.max(maxWidth, lineWidth);
            }
            start = nextStart;
            numLines++;
        }

        return new TextBounds(maxWidth, data.capHeight + (numLines - 1) * data.lineHeight);
    }
    public float getAscent() {
        if (font == null)
            return 0f;
        updateScale();
        return font.getAscent();
    }
    public float getCapHeight() {
        if (font == null)
            return 0f;
        updateScale();
        return font.getCapHeight();
    }
    public float getLineHeight() {
        if (font == null)
            return 0f;
        updateScale();
        return font.getLineHeight();
    }

    public void draw(SpriteBatch batch, String text, Color color, float x, float y, float w, boolean wrap, int horzAlignment) {
        updateScale();
        font.setColor(color);
        font.draw(batch, text, x, y, w, horzAlignment, wrap);
    }

    //update scale of font if needed
    private void updateScale() {
        try {
            if (font.getScaleX() != scale) {
                font.getData().setScale(scale);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean canShrink() {
        return fontSize > MIN_FONT_SIZE;
    }

    public boolean canIncrease() {
        return MAX_FONT_SIZE - fontSize > 2;
    }

    public FSkinFont shrink() {
        return _get(fontSize - 1);
    }

    public FSkinFont increase() {
        return _get(fontSize + 3);
    }

    public String getCharacterSet(String langCode) {
        if (langUniqueCharacterSet.containsKey(langCode)) {
            return langUniqueCharacterSet.get(langCode);
        }
        StringBuilder characters = new StringBuilder(FreeTypeFontGenerator.DEFAULT_CHARS);
        IntSet characterSet = new IntSet();
        for (int offset = 0; offset < FreeTypeFontGenerator.DEFAULT_CHARS.length();) {
            final int codePoint = FreeTypeFontGenerator.DEFAULT_CHARS.codePointAt(offset);
            characterSet.add(codePoint);
            offset += Character.charCount(codePoint);
        }
        String[] translationFilePaths = { ForgeConstants.LANG_DIR + "cardnames-" + langCode + ".txt",
                ForgeConstants.LANG_DIR + langCode + ".properties" };
        for (int i = 0; i < translationFilePaths.length; i++) {
            try (LineReader translationFile = new LineReader(new FileInputStream(translationFilePaths[i]),
                    StandardCharsets.UTF_8)) {
                for (String fileLine : translationFile.readLines()) {
                    final int stringLength = fileLine.length();
                    for (int offset = 0; offset < stringLength;) {
                        final int codePoint = fileLine.codePointAt(offset);
                        if (!characterSet.contains(codePoint)) {
                            characterSet.add(codePoint);
                            characters.append(Character.toChars(codePoint));
                        }
                        offset += Character.charCount(codePoint);
                    }
                }
                translationFile.close();
            } catch (IOException e) {
                if (!"en-US".equalsIgnoreCase(langCode))
                    System.err.println("Error reading translation file: " + translationFilePaths[i]);
            }
        }
        langUniqueCharacterSet.put(langCode, characters.toString());

        return characters.toString();
    }

    private void updateFont() {
        String fontName = "f";
        if (scale != 1) {
            if (fontSize > MAX_FONT_SIZE)
                fontName += MAX_FONT_SIZE;
            else
                fontName += MIN_FONT_SIZE;
        } else {
            fontName += fontSize;
        }
        if (Forge.locale.equals("zh-CN") || Forge.locale.equals("ja-JP") && !Forge.forcedEnglishonCJKMissing) {
            fontName += Forge.locale;
        }
        FileHandle fontFile = Gdx.files.absolute(ForgeConstants.FONTS_DIR + fontName + ".fnt");
        final boolean[] found = {false};
        if (fontFile != null && fontFile.exists()) {
            FThreads.invokeInEdtNowOrLater(() -> { //font must be initialized on UI thread
                try {
                    if (!Forge.getAssets().manager().contains(fontFile.path(), BitmapFont.class)) {
                        Forge.getAssets().manager().load(fontFile.path(), BitmapFont.class);
                        Forge.getAssets().manager().finishLoadingAsset(fontFile.path());
                    }
                    font = Forge.getAssets().manager().get(fontFile.path(), BitmapFont.class);
                    found[0] = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    found[0] = false;
                }
            });
        }
        if (found[0])
            return;
        //not found generate
        if (Forge.locale.equals("zh-CN") || Forge.locale.equals("ja-JP") && !Forge.forcedEnglishonCJKMissing) {
            String ttfName = Forge.CJK_Font;
            FileHandle ttfFile = Gdx.files.absolute(ForgeConstants.FONTS_DIR + ttfName + ".ttf");
            if (ttfFile != null && ttfFile.exists()) {
                generateFont(ttfFile, fontName, fontSize);
            }
        } else {
            generateFont(FSkin.getSkinFile(TTF_FILE), fontName, fontSize);
        }
    }

    private void generateFont(final FileHandle ttfFile, final String fontName, final int fontSize) {
        if (!ttfFile.exists()) { return; }

        final FreeTypeFontGenerator generator = new FreeTypeFontGenerator(ttfFile);

        //approximate optimal page size
        int pageSize;
        if (fontSize >= 50) {
          pageSize = 1024;
        } else if (fontSize >= 20) {
            pageSize = 512;
        } else {
            pageSize = 256;
        }
        if (Forge.locale.equals("zh-CN") || Forge.locale.equals("ja-JP") && !Forge.forcedEnglishonCJKMissing) {
            pageSize = 1024;
        }

        final PixmapPacker packer = new PixmapPacker(pageSize, pageSize, Pixmap.Format.RGBA8888, 2, false);
        final FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.characters = getCharacterSet(Forge.locale);
        parameter.size = fontSize;
        parameter.packer = packer;
        final FreeTypeFontGenerator.FreeTypeBitmapFontData fontData = generator.generateData(parameter);
        final Array<PixmapPacker.Page> pages = packer.getPages();

        //finish generating font on UI thread
        FThreads.invokeInEdtNowOrLater(new Runnable() {
            @Override
            public void run() {
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
                    textureRegions.addAll(new TextureRegion(texture));
                }

                BitmapFont temp = new BitmapFont(fontData, textureRegions, true);

                //create .fnt and .png files for font
                FileHandle pixmapDir = Gdx.files.absolute(ForgeConstants.FONTS_DIR);
                if (pixmapDir != null) {
                    FileHandle fontFile = pixmapDir.child(fontName + ".fnt");
                    BitmapFontWriter.setOutputFormat(BitmapFontWriter.OutputFormat.Text);

                    String[] pageRefs = BitmapFontWriter.writePixmaps(packer.getPages(), pixmapDir, fontName);
                    BitmapFontWriter.writeFont(temp.getData(), pageRefs, fontFile, new BitmapFontWriter.FontInfo(fontName, fontSize), 1, 1);
                    //load to assetManager
                    Forge.getAssets().manager().load(fontFile.path(), BitmapFont.class);
                    Forge.getAssets().manager().finishLoadingAsset(fontFile.path());
                    font = Forge.getAssets().manager().get(fontFile.path(), BitmapFont.class);
                }

                generator.dispose();
                packer.dispose();
                temp.dispose();
            }
        });
    }

    public static Iterable<String> getAllCJKFonts() {
        final Array<String> allCJKFonts = new Array<>();

        allCJKFonts.add("None");
        final FileHandle dir = Gdx.files.absolute(ForgeConstants.FONTS_DIR);
        for (FileHandle fontFile : dir.list()) {
            String fontName = fontFile.name();
            if (!fontName.endsWith(".ttf")) { continue; }
            allCJKFonts.add(fontName.replace(".ttf", ""));
        }

        return allCJKFonts;
    }
}
