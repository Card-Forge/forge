package forge.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.AbsoluteFileHandleResolver;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import forge.gui.GuiBase;
import forge.localinstance.skin.FSkinProp;

import java.util.HashMap;

public class Assets implements Disposable {
    public AssetManager manager = new AssetManager(new AbsoluteFileHandleResolver());
    public HashMap<Integer, FSkinFont> fonts = new HashMap<>();
    public HashMap<String, FImageComplex> cardArtCache = new HashMap<>(1024);
    public HashMap<String, FImage> avatarImages = new HashMap<>();
    public HashMap<String, FSkinImage> MANA_IMAGES = new HashMap<>(128);
    public HashMap<String, FSkinImage> symbolLookup = new HashMap<>(64);
    public HashMap<FSkinProp, FSkinImage> images = new HashMap<>(512);
    public HashMap<Integer, TextureRegion> avatars = new HashMap<>(150);
    public HashMap<Integer, TextureRegion> sleeves = new HashMap<>(64);
    public HashMap<Integer, TextureRegion> cracks = new HashMap<>(16);
    public HashMap<Integer, TextureRegion> borders = new HashMap<>();
    public HashMap<Integer, TextureRegion> deckbox = new HashMap<>();
    public HashMap<Integer, TextureRegion> cursor = new HashMap<>();
    public ObjectMap<Integer, BitmapFont> counterFonts = new ObjectMap<>();
    public ObjectMap<String, Texture> generatedCards = new ObjectMap<>(512);
    public ObjectMap<Integer, Texture> fallback_skins = new ObjectMap<>();
    public ObjectMap<String, Texture> tmxMap = new ObjectMap<String, Texture>();
    public Skin skin;
    public BitmapFont advDefaultFont, advBigFont;
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
        manager.dispose();
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
        if (advDefaultFont != null)
            advDefaultFont.dispose();
        if (advBigFont != null)
            advBigFont.dispose();
        if (skin != null)
            skin.dispose();
    }
}
