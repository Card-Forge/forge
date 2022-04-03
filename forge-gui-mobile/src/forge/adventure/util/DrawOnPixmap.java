package forge.adventure.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import forge.util.TextUtil;

/*
Class to draw directly on a pixmap
 */
public abstract class DrawOnPixmap {
    public static void draw(Pixmap on, TextureRegion from) {
        draw(on, 0, 0, from);

    }

    public static void draw(Pixmap on, int x, int y, TextureRegion from) {
        TextureData textureData = from.getTexture().getTextureData();
        if (!textureData.isPrepared()) {
            textureData.prepare();
        }
        on.drawPixmap(textureData.consumePixmap(), x, y, from.getRegionX(), from.getRegionY(), from.getRegionWidth(), from.getRegionHeight());

    }

    public static void drawText(Pixmap drawingMap, String itemText, int x, int y, float width, boolean bigText) {
        //used for big numbers on Gold/Life for reward...
        BitmapFont font = bigText ? Controls.getBitmapFont("MiKrollFantasyBig") : Controls.getBitmapFont("default");

        BitmapFont.BitmapFontData data = font.getData();
        Pixmap source = new Pixmap(Gdx.files.absolute(data.getImagePath(0)));

        String[] split = TextUtil.split(itemText, ' ');
        for (int i = 0; i < split.length; i++) {
            String text = split[i];
            float totalLength =0;
            for (char c : text.toCharArray()) {
                totalLength += data.getGlyph(c).width;
            }
            float xOffset = (width - totalLength) / 2;
            float yOffset = y;
            xOffset += x;
            for (char c : text.toCharArray()) {
                drawingMap.drawPixmap(source, (int) xOffset, (int) yOffset*(i+1),
                        data.getGlyph(c).srcX, data.getGlyph(c).srcY, data.getGlyph(c).width, data.getGlyph(c).height);
                xOffset += data.getGlyph(c).width + 1;
            }
            if (!bigText) {
                yOffset += data.getGlyph(' ').height + 1;
            }
        }
        source.dispose();

    }
}
