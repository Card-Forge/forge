package forge.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.AbsoluteFileHandleResolver;
import com.badlogic.gdx.utils.Disposable;

public class Assets implements Disposable {
    public AssetManager manager = new AssetManager(new AbsoluteFileHandleResolver());
    @Override
    public void dispose() {
        manager.dispose();
    }
}
