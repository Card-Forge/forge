package forge.assets;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.utils.Array;

import forge.util.Utils;

public class FSkinFont {
    public static final int MIN_FONT_SIZE = Math.round(8 / Utils.MAX_RATIO);

    private static final String TTF_FILE = "font1.ttf";
    private static final Map<Integer, FSkinFont> fonts = new HashMap<Integer, FSkinFont>();
    private static final int FONT_PAGE_SIZE = 256;
    private static final int MAX_FONT_SIZE = 72; //don't generate fonts larger than this, use scaling instead

    public static FSkinFont get(final int size0) {
        FSkinFont skinFont = fonts.get(size0);
        if (skinFont == null) {
            skinFont = new FSkinFont(size0);
            fonts.put(size0, skinFont);
        }
        return skinFont;
    }

    public static FSkinFont forHeight(final float height) {
        int size = MIN_FONT_SIZE + 1;
        while (true) {
            if (get(size).getFont().getLineHeight() > height) {
                return get(size - 1);
            }
            size++;
        }
    }

    public static void updateAll() {
        for (FSkinFont skinFont : fonts.values()) {
            skinFont.updateFont();
        }
    }

    private final int size;
    private BitmapFont font;

    private FSkinFont(final int size0) {
        size = size0;
        updateFont();
    }

    public int getSize() {
        return size;
    }

    public BitmapFont getFont() {
        return font;
    }

    private void updateFont() {
        float scale = 1;
        int fontSize = (int)Utils.scaleMax(size);
        try {
            if (fontSize > MAX_FONT_SIZE) { //scale if larger than max font size
                scale = (float)fontSize / (float)MAX_FONT_SIZE;
                fontSize = MAX_FONT_SIZE;
            }
            String fontName = "f" + fontSize;
            FileHandle fontFile = Gdx.files.absolute(FSkin.getFontDir() + fontName + ".fnt");
            if (fontFile.exists()) {
                font = new BitmapFont(fontFile);
            }
            else {
                FileHandle ttfFile = Gdx.files.absolute(FSkin.getDir() + TTF_FILE);
                font = generateFont(ttfFile, fontName, fontSize);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        if (font == null) {
            font = new BitmapFont(); //use scaled default font as fallback
            scale = (float)fontSize / 15; //default font has size 15
        }
        font.setUseIntegerPositions(true); //prevent parts of text getting cut off at times
        if (scale != 1) {
            font.setScale(scale);
        }
    }

    private BitmapFont generateFont(FileHandle ttfFile, String fontName, int fontSize) {
        if (!ttfFile.exists()) { return null; }

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(ttfFile);

        PixmapPacker packer = new PixmapPacker(FONT_PAGE_SIZE, FONT_PAGE_SIZE, Pixmap.Format.RGBA8888, 2, false);
        FreeTypeFontGenerator.FreeTypeBitmapFontData fontData = generator.generateData(fontSize, FreeTypeFontGenerator.DEFAULT_CHARS, false, packer);
        Array<PixmapPacker.Page> pages = packer.getPages();
        TextureRegion[] texRegions = new TextureRegion[pages.size];
        for (int i=0; i<pages.size; i++) {
            PixmapPacker.Page p = pages.get(i);
            Texture texture = new Texture(new PixmapTextureData(p.getPixmap(), p.getPixmap().getFormat(), false, false)) {
                @Override
                public void dispose() {
                    super.dispose();
                    getTextureData().consumePixmap().dispose();
                }
            };
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            texRegions[i] = new TextureRegion(texture);
        }

        BitmapFont font = new BitmapFont(fontData, texRegions, false);

        //create .fnt and .png files for font
        FileHandle fontFile = Gdx.files.absolute(FSkin.getFontDir() + fontName + ".fnt");
        FileHandle pixmapDir = Gdx.files.absolute(FSkin.getFontDir());
        BitmapFontWriter.setOutputFormat(BitmapFontWriter.OutputFormat.Text);

        String[] pageRefs = BitmapFontWriter.writePixmaps(packer.getPages(), pixmapDir, fontName);
        BitmapFontWriter.writeFont(font.getData(), pageRefs, fontFile, new BitmapFontWriter.FontInfo(fontName, fontSize), 1, 1);

        generator.dispose();
        packer.dispose();
        return font;
    }
}
