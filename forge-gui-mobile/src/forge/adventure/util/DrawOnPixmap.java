package forge.adventure.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;

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

    public static void drawText(Pixmap drawingMap, String itemText, int x, int y, float width, boolean bigText, Color color) {
        //used for big numbers on Gold/Life for reward...
        BitmapFont font = bigText ? Controls.getBitmapFont("big") : Controls.getBitmapFont("default");

        FrameBuffer frameBuffer = new FrameBuffer(Pixmap.Format.RGB888, drawingMap.getWidth(), drawingMap.getHeight(), false);
        SpriteBatch batch=new SpriteBatch();

        frameBuffer.begin();

        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Matrix4 matrix = new Matrix4();
        matrix.setToOrtho2D(0, drawingMap.getHeight(), drawingMap.getWidth(), -drawingMap.getHeight());
        batch.setProjectionMatrix(matrix);

        batch.begin();
        //Rendering ends here. Create a new Pixmap to Texture with mipmaps, otherwise will render as full black.
        Texture texture = new Texture(drawingMap);
        batch.draw(texture,0,0);
        font.setColor(color);
        font.draw(batch,itemText,x,y,width, Align.center,true);
        batch.end();
        drawingMap.drawPixmap(Pixmap.createFromFrameBuffer(0, 0, drawingMap.getWidth(), drawingMap.getHeight()),0,0);
        frameBuffer.end();
        texture.dispose();
        batch.dispose();

    }
}
