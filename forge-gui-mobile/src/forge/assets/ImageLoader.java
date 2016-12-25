package forge.assets;

import java.io.File;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.google.common.cache.CacheLoader;

import forge.Forge;
import forge.ImageKeys;

final class ImageLoader extends CacheLoader<String, Texture> {
    @Override
    public Texture load(String key) {
        File file = ImageKeys.getImageFile(key);
        if (file != null) {
            FileHandle fh = new FileHandle(file);
            try {
                if (Forge.isTextureFilteringEnabled()) {
                    Texture t = new Texture(fh, true);
                    t.setFilter(Texture.TextureFilter.MipMapLinearNearest, Texture.TextureFilter.MipMapNearestNearest);
                    return t;
                } else {
                    return new Texture(fh);
                }
            }
            catch (Exception ex) {
                Forge.log("Could not read image file " + fh.path() + "\n\nException:\n" + ex.toString());
            }
        }
        return null;
    }
}
