package forge.assets;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import forge.Forge;
import forge.Graphics;
import forge.localinstance.skin.FSkinProp;
import forge.util.ImageUtil;

public class FSkinImageImpl implements FSkinImageInterface {
    private final int x, y, w, h;
    FSkinProp skinProp;
    private TextureRegion textureRegion;

	FSkinImageImpl(FSkinProp skinProp0) {
        int[] coords = skinProp0.getCoords();
        x = coords[0];
        y = coords[1];
        w = coords[2];
        h = coords[3];
        skinProp = skinProp0;
	}
	

    public void load(Pixmap preferredIcons) {
        FSkinProp.PropType type = skinProp.getType();
        String filename = type.getFilename();
        if (filename == null) {
        	return;
        }
        boolean is2D = type == FSkinProp.PropType.ADVENTURE;
        FileHandle preferredFile = type == FSkinProp.PropType.MANAICONS ? FSkin.getDefaultSkinFile(filename) : FSkin.getSkinFile(filename);
        Texture texture = Forge.getAssets().getTexture(preferredFile, is2D, false);
        if (texture == null) {
            if (preferredFile.exists()) {
                try {
                    texture = Forge.getAssets().getTexture(preferredFile, is2D, false);
                }
                catch (final Exception e) {
                    System.err.println("Failed to load skin file: " + preferredFile);
                    //e.printStackTrace();
                }
            }
        }
        if (texture != null) {
            if (!(type == FSkinProp.PropType.ABILITY || type == FSkinProp.PropType.IMAGE || type == FSkinProp.PropType.ICON || type == FSkinProp.PropType.MANAICONS)) { //just return region for preferred file if not icons file
                textureRegion = new TextureRegion(texture, x, y, w, h);
                return;
            }

            int fullWidth = texture.getWidth();
            int fullHeight = texture.getHeight();

            // Test if requested sub-image in inside bounds of preferred sprite.
            // (Height and width of preferred sprite were set in loadFontAndImages.)
            if (x + w <= fullWidth && y + h <= fullHeight) {
                // Test if various points of requested sub-image are transparent.
                // If any return true, image exists.
                int x0 = 0, y0 = 0;
                Color c;

                // Center
                x0 = (x + w / 2);
                y0 = (y + h / 2);
                c = new Color(preferredIcons.getPixel(x0, y0));
                if (c.a != 0) {
                    textureRegion = new TextureRegion(texture, x, y, w, h);
                    return;
                }

                x0 += 2;
                y0 += 2;
                c = new Color(preferredIcons.getPixel(x0, y0));
                if (c.a != 0) {
                    textureRegion = new TextureRegion(texture, x, y, w, h);
                    return;
                }

                x0 -= 4;
                c = new Color(preferredIcons.getPixel(x0, y0));
                if (c.a != 0) {
                    textureRegion = new TextureRegion(texture, x, y, w, h);
                    return;
                }

                y0 -= 4;
                c = new Color(preferredIcons.getPixel(x0, y0));
                if (c.a != 0) {
                    textureRegion = new TextureRegion(texture, x, y, w, h);
                    return;
                }

                x0 += 4;
                c = new Color(preferredIcons.getPixel(x0, y0));
                if (c.a != 0) {
                    textureRegion = new TextureRegion(texture, x, y, w, h);
                    return;
                }
            }
        }

        //use default file if can't use preferred file
        FileHandle defaultFile = FSkin.getDefaultSkinFile(filename);
        texture = Forge.getAssets().getTexture(defaultFile, is2D, false);
        if (texture == null) {
            if (defaultFile.exists()) {
                try {
                    texture = Forge.getAssets().getTexture(defaultFile, is2D, false);
                }
                catch (final Exception e) {
                    System.err.println("Failed to load skin file: " + defaultFile);
                    //e.printStackTrace();
                }
            }
        }
        if (texture != null) {
            textureRegion = new TextureRegion(texture, x, y, w, h);
        }
    }

    @Override
    public float getWidth() {
        return w;
    }

    @Override
    public float getHeight() {
        return h;
    }

    @Override
    public TextureRegion getTextureRegion() {
        return textureRegion;
    }

    @Override
    public float getNearestHQWidth(float baseWidth) {
        return ImageUtil.getNearestHQSize(baseWidth, w);
    }

    @Override
    public float getNearestHQHeight(float baseHeight) {
        return ImageUtil.getNearestHQSize(baseHeight, h);
    }

    @Override
    public void draw(Graphics g, float x, float y, float w, float h) {
        g.drawImage(textureRegion, x, y, w, h);
    }
}
