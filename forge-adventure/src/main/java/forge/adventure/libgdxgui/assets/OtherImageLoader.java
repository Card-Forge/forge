package forge.adventure.libgdxgui.assets;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.google.common.cache.CacheLoader;
import forge.adventure.libgdxgui.Forge;
import forge.ImageKeys;

import java.io.File;

final class OtherImageLoader extends CacheLoader<String, Texture> {
    @Override
    public Texture load(String key) {
        File file = ImageKeys.getImageFile(key);
        if (file != null) {
            FileHandle fh = new FileHandle(file);
            try {
                if (Forge.isTextureFilteringEnabled()) {
                    Texture t = new Texture(fh, true);
                    t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
                    return t;
                } else {
                    return new Texture(fh);
                }
            }
            catch (Exception ex) {
                Forge.log("Could not read image file " + fh.path() + "\n\nException:\n" + ex);
                return null;
            }
        }
        return null;
    }
}

