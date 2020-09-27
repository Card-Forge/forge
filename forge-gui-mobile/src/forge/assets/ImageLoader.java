package forge.assets;

import java.io.File;
import java.util.List;

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
import forge.properties.ForgeConstants;
import forge.util.FileUtil;
import forge.util.TextUtil;

import static forge.assets.ImageCache.croppedBorderImage;

final class ImageLoader extends CacheLoader<String, Texture> {
    private static List<String> borderlessCardlistKey = FileUtil.readFile(ForgeConstants.BORDERLESS_CARD_LIST_FILE);

    Texture n;
    @Override
    public Texture load(String key) {
        boolean extendedArt = isBorderless(key);
        boolean textureFilter = Forge.isTextureFilteringEnabled();
        File file = ImageKeys.getImageFile(key);
        if (file != null) {
            FileHandle fh = new FileHandle(file);
            try {
                Texture t = new Texture(fh, textureFilter);
                //update
                ImageCache.updateBorders(t, extendedArt ? false: isCloserToWhite(getpixelColor(t)));
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

    public boolean isBorderless(String imagekey) {
        if(borderlessCardlistKey.isEmpty())
            return false;
        if (imagekey.length() > 7) {
            if ((!imagekey.substring(0, 7).contains("MPS_KLD"))&&(imagekey.substring(0, 4).contains("MPS_"))) //MPS_ sets except MPD_KLD
                return true;
        }
        return borderlessCardlistKey.contains(TextUtil.fastReplace(imagekey,".full",".fullborder"));
    }

    public static boolean isBorderless(Texture t) {
        if(borderlessCardlistKey.isEmpty())
            return false;
        //generated texture/pixmap?
        if (t.toString().contains("com.badlogic.gdx.graphics.Texture@"))
            return true;
        for (String key : borderlessCardlistKey) {
            if (t.toString().contains(key))
                return true;
        }
        return false;
    }

    public static String getpixelColor(Texture i) {
        if (!i.getTextureData().isPrepared()) {
            i.getTextureData().prepare(); //prepare texture
        }
        //get pixmap from texture data
        Pixmap pixmap = i.getTextureData().consumePixmap();
        //get pixel color from x,y texture coordinate based on the image fullborder or not
        Color color = new Color(pixmap.getPixel(croppedBorderImage(i).getRegionX()+1, croppedBorderImage(i).getRegionY()+1));
        pixmap.dispose();
        return color.toString();
    }
    public static boolean isCloserToWhite(String c){
        if (c == null || c == "")
            return false;
        int c_r = Integer.parseInt(c.substring(0,2),16);
        int c_g = Integer.parseInt(c.substring(2,4),16);
        int c_b = Integer.parseInt(c.substring(4,6),16);
        int brightness = ((c_r * 299) + (c_g * 587) + (c_b * 114)) / 1000;
        return brightness > 155;
    }
}
