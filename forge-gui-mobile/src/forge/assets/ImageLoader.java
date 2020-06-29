package forge.assets;

import java.io.File;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.google.common.cache.CacheLoader;
import forge.FThreads;

import forge.Forge;
import forge.ImageKeys;

final class ImageLoader extends CacheLoader<String, Texture> {
    Texture n;
    @Override
    public Texture load(String key) {
        boolean extendedArt = false;
        boolean textureFilter = Forge.isTextureFilteringEnabled();
        if (key.length() > 4){
            if ((key.substring(0,4).contains("MPS_"))) //MPS_ sets
                extendedArt = true;
            else if ((key.substring(0,3).contains("UST"))) //Unstable Set
                extendedArt = true;
        }
        File file = ImageKeys.getImageFile(key);
        if (file != null) {
            FileHandle fh = new FileHandle(file);
            try {
                Texture t = new Texture(fh, textureFilter);
                if (textureFilter)
                    t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
                if (extendedArt)
                    return generateTexture(fh, t, textureFilter);
                return t;
            }
            catch (Exception ex) {
                Forge.log("Could not read image file " + fh.path() + "\n\nException:\n" + ex.toString());
            }
        }
        return null;
    }

    public Texture generateTexture(FileHandle fh, Texture t, boolean textureFilter) {
        if (t == null || fh == null)
            return t;
        FThreads.invokeInEdtNowOrLater(new Runnable() {
            @Override
            public void run() {
                Pixmap pImage = new Pixmap(fh);
                int w = pImage.getWidth();
                int h = pImage.getHeight();
                int radius = (h - w) / 8;
                Pixmap pMask = createRoundedRectangle(w, h, radius, Color.RED);
                drawPixelstoMask(pImage, pMask);
                TextureData textureData = new PixmapTextureData(
                        pMask, //pixmap to use
                        Pixmap.Format.RGBA8888,
                        textureFilter, //use mipmaps
                        false, true);
                n = new Texture(textureData);
                if (textureFilter)
                    n.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
                pImage.dispose();
                pMask.dispose();
            }
        });
        return n;
    }
    public Pixmap createRoundedRectangle(int width, int height, int cornerRadius, Color color) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        Pixmap ret = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        //round corners
        pixmap.fillCircle(cornerRadius, cornerRadius, cornerRadius);
        pixmap.fillCircle(width - cornerRadius - 1, cornerRadius, cornerRadius);
        pixmap.fillCircle(cornerRadius, height - cornerRadius - 1, cornerRadius);
        pixmap.fillCircle(width - cornerRadius - 1, height - cornerRadius - 1, cornerRadius);
        //two rectangle parts
        pixmap.fillRectangle(cornerRadius, 0, width - cornerRadius * 2, height);
        pixmap.fillRectangle(0, cornerRadius, width, height - cornerRadius * 2);
        //draw rounded rectangle
        ret.setColor(color);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (pixmap.getPixel(x, y) != 0) ret.drawPixel(x, y);
            }
        }
        pixmap.dispose();
        return ret;
    }
    public void drawPixelstoMask(Pixmap pixmap, Pixmap mask){
        int pixmapWidth = mask.getWidth();
        int pixmapHeight = mask.getHeight();
        Color pixelColor = new Color();
        for (int x=0; x<pixmapWidth; x++){
            for (int y=0; y<pixmapHeight; y++){
                if (mask.getPixel(x, y) != 0) {
                    Color.rgba8888ToColor(pixelColor, pixmap.getPixel(x, y));
                    mask.setColor(pixelColor);
                    mask.drawPixel(x, y);
                }
            }
        }
    }
}
