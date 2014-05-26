package forge.assets;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.utils.Array;

import forge.FThreads;
import forge.util.Utils;

public class FSkinFont {
    public static final int MIN_FONT_SIZE = Math.round(8 / Utils.MAX_RATIO);
    public static final int MAX_FONT_SIZE = Math.round(72 / Utils.MAX_RATIO);

    private static final String TTF_FILE = "font1.ttf";
    private static final Map<Integer, FSkinFont> fonts = new HashMap<Integer, FSkinFont>();

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
            if (get(size).getLineHeight() > height) {
                return get(size - 1);
            }
            size++;
        }
    }

    //pre-load all supported font sizes
    public static void preloadAll() {
        for (int size = MIN_FONT_SIZE; size <= MAX_FONT_SIZE; size++) {
            get(size);
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

    // Expose methods from font that updates scale as needed
    public TextBounds getBounds(CharSequence str) {
        return font.getBounds(str);
    }
    public TextBounds getMultiLineBounds(CharSequence str) {
        return font.getMultiLineBounds(str);
    }
    public TextBounds getWrappedBounds(CharSequence str, float wrapWidth) {
        return font.getWrappedBounds(str, wrapWidth);
    }
    public float getAscent() {
        return font.getAscent();
    }
    public float getCapHeight() {
        return font.getCapHeight();
    }
    public float getLineHeight() {
        return font.getLineHeight();
    }

    public void draw(SpriteBatch batch, String text, Color color, float x, float y, float w, boolean wrap, HAlignment horzAlignment) {
        font.setColor(color);
        if (wrap) {
            font.drawWrapped(batch, text, x, y, w, horzAlignment);
        }
        else {
            font.drawMultiLine(batch, text, x, y, w, horzAlignment);
        }
    }

    private void updateFont() {
        int fontSize = (int)Utils.scaleMax(size);
        String fontName = "f" + fontSize;
        FileHandle fontFile = Gdx.files.absolute(FSkin.getFontDir() + fontName + ".fnt");
        if (fontFile.exists()) {
            final BitmapFontData data = new BitmapFontData(fontFile, false);
            FThreads.invokeInEdtNowOrLater(new Runnable() {
                @Override
                public void run() { //font must be initialized on UI thread
                    font = new BitmapFont(data, (TextureRegion)null, true);
                }
            });
        }
        else {
            FileHandle ttfFile = Gdx.files.absolute(FSkin.getDir() + TTF_FILE);
            generateFont(ttfFile, fontName, fontSize);
        }
    }

    private void generateFont(final FileHandle ttfFile, final String fontName, final int fontSize) {
        if (!ttfFile.exists()) { return; }

        final FreeTypeFontGenerator generator = new FreeTypeFontGenerator(ttfFile);

        //approximate optimal page size
        int pageSize;
        if (fontSize >= 28) {
            pageSize = 256;
        }
        else {
            pageSize = 128;
        }

        //only generate images for characters that could be used by Forge
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890\"!?'.,;:()[]{}<>|/@\\^$-%+=#_&*";

        final PixmapPacker packer = new PixmapPacker(pageSize, pageSize, Pixmap.Format.RGBA8888, 2, false);
        final FreeTypeFontGenerator.FreeTypeBitmapFontData fontData = generator.generateData(fontSize, chars, false, packer);
        final Array<PixmapPacker.Page> pages = packer.getPages();

        //finish generating font on UI thread
        FThreads.invokeInEdtNowOrLater(new Runnable() {
            @Override
            public void run() {
                TextureRegion[] textureRegions = new TextureRegion[pages.size];
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
                    textureRegions[i] = new TextureRegion(texture);
                }

                font = new BitmapFont(fontData, textureRegions, true);

                //create .fnt and .png files for font
                FileHandle fontFile = Gdx.files.absolute(FSkin.getFontDir() + fontName + ".fnt");
                FileHandle pixmapDir = Gdx.files.absolute(FSkin.getFontDir());
                BitmapFontWriter.setOutputFormat(BitmapFontWriter.OutputFormat.Text);

                String[] pageRefs = BitmapFontWriter.writePixmaps(packer.getPages(), pixmapDir, fontName);
                BitmapFontWriter.writeFont(font.getData(), pageRefs, fontFile, new BitmapFontWriter.FontInfo(fontName, fontSize), 1, 1);

                generator.dispose();
                packer.dispose();
            }
        });
    }
}
