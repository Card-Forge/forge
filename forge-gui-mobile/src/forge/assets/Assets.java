package forge.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.AbsoluteFileHandleResolver;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

import java.util.HashMap;

public class Assets implements Disposable {
    public AssetManager cards = new AssetManager(new AbsoluteFileHandleResolver());
    public AssetManager others = new AssetManager(new AbsoluteFileHandleResolver());
    public HashMap<Integer, FSkinFont> fonts = new HashMap<>();
    public ObjectMap<Integer, BitmapFont> counterFonts = new ObjectMap<>();
    public ObjectMap<String, Texture> generatedCards = new ObjectMap<>(512);
    @Override
    public void dispose() {
        cards.dispose();
        others.dispose();
        for (BitmapFont bitmapFont : counterFonts.values())
            bitmapFont.dispose();
        for (Texture texture : generatedCards.values())
            texture.dispose();
        for (FSkinFont fSkinFont : fonts.values())
            fSkinFont.font.dispose();
    }
}
