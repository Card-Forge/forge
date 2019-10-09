package forge.assets;

import java.io.File;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import org.cache2k.integration.CacheLoader;

import forge.Forge;
import forge.ImageKeys;

final class ImageLoader extends CacheLoader<String, Texture> {
    @Override
    public Texture load(String key) {
        boolean textureFilter = Forge.isTextureFilteringEnabled();
        File file = ImageKeys.getImageFile(key);
        if (file != null) {
            FileHandle fh = new FileHandle(file);
            try {
                Texture t = new Texture(fh, textureFilter);
                if (textureFilter)
                    t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
                return t;
            }
            catch (Exception ex) {
                Forge.log("Could not read image file " + fh.path() + "\n\nException:\n" + ex.toString());
            }
        }
        return null;
    }
}
