package forge.assets;

import java.io.File;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import org.cache2k.integration.CacheLoader;

import forge.Forge;
import forge.ImageKeys;

final class ImageLoader extends CacheLoader<String, Texture> {
    @Override
    public Texture load(String key) {
        boolean mask = key.contains("#drawroundcorner#");
        boolean alphaCard = false;
        boolean textureFilter = Forge.isTextureFilteringEnabled();
        if (key.length() > 4){
            if ((key.substring(0,4).contains("LEA/")) || (key.substring(0,2).contains("A/")))
                alphaCard = true;
            //TODO dont add border on some sets???
        }
        File file = ImageKeys.getImageFile(key.replaceAll("#drawroundcorner#",""));
        if (file != null) {
            FileHandle fh = new FileHandle(file);
            try {
                Texture t;
                if (mask) {
                    Pixmap pImage = new Pixmap(fh);
                    int w = pImage.getWidth();
                    int h = pImage.getHeight();
                    int radius = alphaCard ? (h - w) / 6 : (h - w) / 8;
                    Pixmap pMask = createRoundedRectangle(w, h, radius, Color.RED);
                    drawPixelstoMask(pImage, pMask);
                    TextureData textureData = new PixmapTextureData(
                            pMask, //pixmap to use
                            Pixmap.Format.RGBA8888,
                            textureFilter, //use mipmaps
                            false, true);
                    if (textureFilter)
                    {
                        t = new Texture(textureData);
                        t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
                    } else {
                        t = new Texture(textureData);
                    }
                    pImage.dispose();
                    pMask.dispose();
                    return t;
                } else {
                    if (textureFilter) {
                        t = new Texture(fh, true);
                        t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
                        return t;
                    }
                    else
                        return new Texture(fh);
                }
            }
            catch (Exception ex) {
                Forge.log("Could not read image file " + fh.path() + "\n\nException:\n" + ex.toString());
            }
        }
        return null;
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
    public void blendPixmaps(Pixmap pixmap, Pixmap mask, Pixmap model){
        int pixmapWidth = pixmap.getWidth();
        int pixmapHeight = pixmap.getHeight();
        Color pixelColor = new Color();
        Color maskPixelColor = new Color();

        for (int x=0; x<pixmapWidth; x++){
            for (int y=0; y<pixmapHeight; y++){
                Color.rgba8888ToColor(pixelColor, pixmap.getPixel(x, y));
                Color.rgba8888ToColor(maskPixelColor, mask.getPixel(x, y));

                pixelColor.a = pixelColor.a * maskPixelColor.a;
                model.setColor(pixelColor);
                model.drawPixel(x, y);
            }
        }
    }
}
