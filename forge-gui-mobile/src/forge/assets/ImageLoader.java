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
                    t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);

                    /* // Optional experimental feature: Anisotropic filtering
                    GL20 gl = Gdx.gl20;
                    if (gl != null && Gdx.graphics.supportsExtension("GL_EXT_texture_filter_anisotropic")) {
                        FloatBuffer buffer = BufferUtils.newFloatBuffer(16);
                        gl.glGetFloatv(GL20.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, buffer);
                        float maxAniso = buffer.get(0);

                        t.bind();
                        gl.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAniso);
                    } */
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
