package forge.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.AbsoluteFileHandleResolver;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import forge.gui.GuiBase;

import java.util.HashMap;

public class Assets implements Disposable {
    public AssetManager cards = new AssetManager(new AbsoluteFileHandleResolver());
    public AssetManager others = new AssetManager(new AbsoluteFileHandleResolver());
    public HashMap<Integer, FSkinFont> fonts = new HashMap<>();
    public ObjectMap<Integer, BitmapFont> counterFonts = new ObjectMap<>();
    public ObjectMap<String, Texture> generatedCards = new ObjectMap<>(512);
    public ObjectMap<Integer, Texture> fallback_skins = new ObjectMap<>();
    public ObjectMap<String, Texture> tmxMap = new ObjectMap<String, Texture>();
    public Assets() {
        //init titlebg fallback
        fallback_skins.put(0, new Texture(GuiBase.isAndroid()
                ? Gdx.files.internal("fallback_skin").child("title_bg_lq.png")
                : Gdx.files.classpath("fallback_skin").child("title_bg_lq.png")));
        //init transition fallback
        fallback_skins.put(1, new Texture(GuiBase.isAndroid()
                ? Gdx.files.internal("fallback_skin").child("transition.png")
                : Gdx.files.classpath("fallback_skin").child("transition.png")));
    }
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
        for (Texture texture : fallback_skins.values())
            texture.dispose();
        for (Texture texture : tmxMap.values())
            texture.dispose();
    }
}
