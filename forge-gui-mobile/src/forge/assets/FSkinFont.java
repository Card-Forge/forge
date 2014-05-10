package forge.assets;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

import forge.util.Utils;

public class FSkinFont {
    public static final int MIN_FONT_SIZE = Math.round(8 / Utils.MAX_RATIO);

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
        String dir = FSkin.getDir();

        //generate .fnt and .png files from .ttf if needed
        FileHandle ttfFile = Gdx.files.absolute(dir + TTF_FILE);
        if (ttfFile.exists()) {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(ttfFile);
            font = generator.generateFont((int)Utils.scaleMax(size)); //scale font based on size
            font.setUseIntegerPositions(true); //prevent parts of text getting cut off at times
            generator.dispose();
        }
    }

    public static void updateAll() {
        for (FSkinFont skinFont : fonts.values()) {
            skinFont.updateFont();
        }
    }
}
