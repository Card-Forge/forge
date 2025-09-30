package forge.card;

import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.localinstance.skin.FSkinProp;

public class ColorSetImage implements FImage {
    private final ColorSet colorSet;
    private final int shardCount;

    public ColorSetImage(ColorSet colorSet0) {
        colorSet = colorSet0;
        shardCount = colorSet.getOrderedColors().size();
    }

    @Override
    public float getWidth() {
        return Forge.getAssets().images().get(FSkinProp.IMG_MANA_W).getWidth() * shardCount;
    }

    @Override
    public float getHeight() {
        return Forge.getAssets().images().get(FSkinProp.IMG_MANA_W).getHeight();
    }

    @Override
    public void draw(Graphics g, float x, float y, float w, float h) {
        float imageSize = w / shardCount;
        if (imageSize > h) {
            imageSize = h;
            float w0 = imageSize * shardCount;
            x += (w - w0) / 2;
            w = w0;
        }
        CardFaceSymbols.drawColorSet(g, colorSet, x, y, imageSize);
    }
}
