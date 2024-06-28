package forge.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import io.github.zumikua.webploader.common.WebPLoaderFactory;
import io.github.zumikua.webploader.common.WebPLoaderNativeInterface;
import io.github.zumikua.webploader.common.WebPTextureFactory;
import io.github.zumikua.webploader.common.WebPTextureLoader;

/**
 *
 */
public class WebpLoader  {

    private final WebPTextureFactory textureFactory;

    /**
     *
     * @param nativeInterface
     */
    public WebpLoader(WebPLoaderNativeInterface nativeInterface) {
        WebPLoaderFactory mFactory = new WebPLoaderFactory(nativeInterface);
        textureFactory = mFactory.getTextureFactory();
    }

    /**
     * Load the webp file into Texture file.
     * @param filename Webp image file
     * @return Texture file load
     */
    public Texture load(String filename) {
        AssetManager mAssetManager = new AssetManager();
        mAssetManager.setLoader(Texture.class, ".webp", new WebPTextureLoader(mAssetManager.getFileHandleResolver(), textureFactory));
        mAssetManager.load(filename, Texture.class);
        mAssetManager.finishLoading();
        return mAssetManager.get(filename, Texture.class);
    }
}
