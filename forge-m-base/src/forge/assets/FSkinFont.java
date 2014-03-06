package forge.assets;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public class FSkinFont {
    private static final String TTF_FILE = "font1.ttf";
    private static final int defaultFontSize = 12;
    private static final Map<Integer, FSkinFont> fonts = new HashMap<Integer, FSkinFont>();

    public static FSkinFont get() {
        return get(defaultFontSize);
    }

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
        this.size = size0;
        this.updateFont();
    }

    public int getSize() {
        return this.size;
    }

    public BitmapFont getFont() {
        return font;
    }

    private void updateFont() {
        String dir = FSkin.getDir();

        //generate .fnt and .png files from .ttf if needed
        FileHandle ttfFile = Gdx.files.internal(dir + TTF_FILE);
        if (ttfFile.exists()) {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(ttfFile);
            font = generator.generateFont(this.size);
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
