package forge.adventure.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/*
Class to draw directly on a pixmap
 */
public abstract class DrawOnPixmap {
    public static void draw(Pixmap on, TextureRegion from)
    {
        draw(on,0,0,from);

    }
    public static void draw(Pixmap on,int x,int y, TextureRegion from)
    {
        TextureData textureData = from.getTexture().getTextureData();
        if (!textureData.isPrepared()) {
            textureData.prepare();
        }
        on.drawPixmap( textureData.consumePixmap(), x, y,  from.getRegionX(), from.getRegionY(),  from.getRegionWidth(),  from.getRegionHeight() );

    }

    public static void drawText(Pixmap drawingMap, String text, int x, int y, float width) {

        BitmapFont font=Controls.bigFont();

        BitmapFont.BitmapFontData data=font.getData();
        Pixmap source=new Pixmap(Gdx.files.internal(data.imagePaths[0]));

        float totalLength=data.getGlyph('0').width*text.length();
        float xOffset=(width-totalLength)/2;
        xOffset+=x;
        for(char c:text.toCharArray())
        {

            drawingMap.drawPixmap(source, (int)xOffset, y,
                    data.getGlyph(c).srcX, data.getGlyph(c).srcY, data.getGlyph(c).width, data.getGlyph(c).height);
            xOffset+=data.getGlyph(c).width+1;
        }
        source.dispose();

    }
}
