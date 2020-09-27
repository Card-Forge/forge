package forge.assets;

import java.io.File;
import java.util.ArrayList;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.google.common.cache.CacheLoader;
import forge.FThreads;

import forge.Forge;
import forge.ImageKeys;
import forge.util.TextUtil;

import static forge.assets.ImageCache.croppedBorderImage;

final class ImageLoader extends CacheLoader<String, Texture> {
    private static ArrayList<String> borderlessCardlistKey = new ArrayList<String>() {
        {//TODO: load the values from text list instead of hardcoded
            add("2XM/Academy Ruins2.fullborder");
            add("2XM/Atraxa, Praetors' Voice2.fullborder");
            add("2XM/Avacyn, Angel of Hope2.fullborder");
            add("2XM/Batterskull2.fullborder");
            add("2XM/Blightsteel Colossus2.fullborder");
            add("2XM/Blood Moon2.fullborder");
            add("2XM/Brainstorm2.fullborder");
            add("2XM/Chrome Mox2.fullborder");
            add("2XM/Council's Judgment2.fullborder");
            add("2XM/Crop Rotation2.fullborder");
            add("2XM/Cyclonic Rift2.fullborder");
            add("2XM/Dark Confidant2.fullborder");
            add("2XM/Doubling Season2.fullborder");
            add("2XM/Expedition Map2.fullborder");
            add("2XM/Exploration2.fullborder");
            add("2XM/Fatal Push2.fullborder");
            add("2XM/Force of Will2.fullborder");
            add("2XM/Goblin Guide2.fullborder");
            add("2XM/Jace, the Mind Sculptor2.fullborder");
            add("2XM/Kaalia of the Vast2.fullborder");
            add("2XM/Karn Liberated2.fullborder");
            add("2XM/Lightning Greaves2.fullborder");
            add("2XM/Mana Crypt2.fullborder");
            add("2XM/Meddling Mage2.fullborder");
            add("2XM/Mox Opal2.fullborder");
            add("2XM/Noble Hierarch2.fullborder");
            add("2XM/Phyrexian Metamorph2.fullborder");
            add("2XM/Sneak Attack2.fullborder");
            add("2XM/Stoneforge Mystic2.fullborder");
            add("2XM/Sword of Body and Mind2.fullborder");
            add("2XM/Sword of Feast and Famine2.fullborder");
            add("2XM/Sword of Fire and Ice2.fullborder");
            add("2XM/Sword of Light and Shadow2.fullborder");
            add("2XM/Sword of War and Peace2.fullborder");
            add("2XM/Thoughtseize2.fullborder");
            add("2XM/Toxic Deluge2.fullborder");
            add("2XM/Urza's Mine2.fullborder");
            add("2XM/Urza's Power Plant2.fullborder");
            add("2XM/Urza's Tower2.fullborder");
            add("2XM/Wurmcoil Engine2.fullborder");
            add("ELD/Garruk, Cursed Huntsman2.fullborder");
            add("ELD/Oko, Thief of Crowns2.fullborder");
            add("ELD/The Royal Scions2.fullborder");
            add("IKO/Brokkos, Apex of Forever2.fullborder");
            add("IKO/Brokkos, Apex of Forever3.fullborder");
            add("IKO/Crystalline Giant3.fullborder");
            add("IKO/Cubwarden2.fullborder");
            add("IKO/Dirge Bat2.fullborder");
            add("IKO/Dirge Bat3.fullborder");
            add("IKO/Everquill Phoenix2.fullborder");
            add("IKO/Everquill Phoenix3.fullborder");
            add("IKO/Gemrazer2.fullborder");
            add("IKO/Gemrazer3.fullborder");
            add("IKO/Gyruda, Doom of Depths3.fullborder");
            add("IKO/Huntmaster Liger2.fullborder");
            add("IKO/Huntmaster Liger3.fullborder");
            add("IKO/Illuna, Apex of Wishes2.fullborder");
            add("IKO/Illuna, Apex of Wishes3.fullborder");
            add("IKO/Indatha Triome2.fullborder");
            add("IKO/Ketria Triome2.fullborder");
            add("IKO/Lukka, Coppercoat Outcast2.fullborder");
            add("IKO/Luminous Broodmoth3.fullborder");
            add("IKO/Mysterious Egg2.fullborder");
            add("IKO/Narset of the Ancient Way2.fullborder");
            add("IKO/Nethroi, Apex of Death2.fullborder");
            add("IKO/Nethroi, Apex of Death3.fullborder");
            add("IKO/Pollywog Symbiote2.fullborder");
            add("IKO/Raugrin Triome2.fullborder");
            add("IKO/Savai Triome2.fullborder");
            add("IKO/Sea-Dasher Octopus2.fullborder");
            add("IKO/Snapdax, Apex of the Hunt2.fullborder");
            add("IKO/Snapdax, Apex of the Hunt3.fullborder");
            add("IKO/Sprite Dragon3.fullborder");
            add("IKO/Titanoth Rex2.fullborder");
            add("IKO/Vadrok, Apex of Thunder2.fullborder");
            add("IKO/Vadrok, Apex of Thunder3.fullborder");
            add("IKO/Vivien, Monsters' Advocate2.fullborder");
            add("IKO/Void Beckoner2.fullborder");
            add("IKO/Yidaro, Wandering Monster3.fullborder");
            add("IKO/Zagoth Triome2.fullborder");
            add("IKO/Zilortha, Strength Incarnate.fullborder");
            add("M21/Basri Ket2.fullborder");
            add("M21/Chandra, Heart of Fire2.fullborder");
            add("M21/Containment Priest2.fullborder");
            add("M21/Cultivate2.fullborder");
            add("M21/Garruk, Unleashed2.fullborder");
            add("M21/Grim Tutor2.fullborder");
            add("M21/Liliana, Waker of the Dead2.fullborder");
            add("M21/Massacre Wurm2.fullborder");
            add("M21/Scavenging Ooze2.fullborder");
            add("M21/Solemn Simulacrum2.fullborder");
            add("M21/Teferi, Master of Time2.fullborder");
            add("M21/Ugin, the Spirit Dragon2.fullborder");
            add("M21/Ugin, the Spirit Dragon3.fullborder");
            add("PLGS/Hangarback Walker.fullborder");
            add("SLD/Acidic Slime.fullborder");
            add("SLD/Captain Sisay.fullborder");
            add("SLD/Meren of Clan Nel Toth.fullborder");
            add("SLD/Narset, Enlightened Master.fullborder");
            add("SLD/Necrotic Ooze.fullborder");
            add("SLD/Oona, Queen of the Fae.fullborder");
            add("SLD/Saskia the Unyielding.fullborder");
            add("SLD/The Mimeoplasm.fullborder");
            add("SLD/Voidslime.fullborder");
            add("THB/Ashiok, Nightmare Muse2.fullborder");
            add("THB/Calix, Destiny's Hand2.fullborder");
            add("THB/Elspeth, Sun's Nemesis2.fullborder");
            add("UST/Forest.fullborder");
            add("UST/Island.fullborder");
            add("UST/Mountain.fullborder");
            add("UST/Plains.fullborder");
            add("UST/Swamp.fullborder");
            add("ZNR/Boulderloft Pathway2.fullborder");
            add("ZNR/Branchloft Pathway2.fullborder");
            add("ZNR/Brightclimb Pathway2.fullborder");
            add("ZNR/Clearwater Pathway2.fullborder");
            add("ZNR/Cragcrown Pathway2.fullborder");
            add("ZNR/Grimclimb Pathway2.fullborder");
            add("ZNR/Jace, Mirror Mage2.fullborder");
            add("ZNR/Lavaglide Pathway2.fullborder");
            add("ZNR/Murkwater Pathway2.fullborder");
            add("ZNR/Nahiri, Heir of the Ancients2.fullborder");
            add("ZNR/Needleverge Pathway2.fullborder");
            add("ZNR/Nissa of Shadowed Boughs2.fullborder");
            add("ZNR/Pillarverge Pathway2.fullborder");
            add("ZNR/Riverglide Pathway2.fullborder");
            add("ZNR/Timbercrown Pathway2.fullborder");
        }
    };

    Texture n;
    @Override
    public Texture load(String key) {
        boolean extendedArt = isBorderless(key);
        boolean textureFilter = Forge.isTextureFilteringEnabled();
        File file = ImageKeys.getImageFile(key);
        if (file != null) {
            FileHandle fh = new FileHandle(file);
            try {
                Texture t = new Texture(fh, textureFilter);
                //update
                ImageCache.updateBorders(t, extendedArt ? false: isCloserToWhite(getpixelColor(t)));
                if (textureFilter)
                    t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
                if (extendedArt)
                    return generateTexture(fh, t, textureFilter);
                return t;
            }
            catch (Exception ex) {
                Forge.log("Could not read image file " + fh.path() + "\n\nException:\n" + ex.toString());
            }
        }
        return null;
    }

    public Texture generateTexture(FileHandle fh, Texture t, boolean textureFilter) {
        if (t == null || fh == null)
            return t;
        FThreads.invokeInEdtNowOrLater(new Runnable() {
            @Override
            public void run() {
                Pixmap pImage = new Pixmap(fh);
                int w = pImage.getWidth();
                int h = pImage.getHeight();
                int radius = (h - w) / 8;
                Pixmap pMask = createRoundedRectangle(w, h, radius, Color.RED);
                drawPixelstoMask(pImage, pMask);
                TextureData textureData = new PixmapTextureData(
                        pMask, //pixmap to use
                        Pixmap.Format.RGBA8888,
                        textureFilter, //use mipmaps
                        false, true);
                n = new Texture(textureData);
                if (textureFilter)
                    n.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
                pImage.dispose();
                pMask.dispose();
            }
        });
        return n;
    }
    public Pixmap createRoundedRectangle(int width, int height, int cornerRadius, Color color) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        Pixmap ret = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        //round corners
        pixmap.fillCircle(cornerRadius, cornerRadius, cornerRadius);
        pixmap.fillCircle(width - cornerRadius - 1, cornerRadius, cornerRadius);
        pixmap.fillCircle(cornerRadius, height - cornerRadius - 1, cornerRadius);
        pixmap.fillCircle(width - cornerRadius - 1, height - cornerRadius - 1, cornerRadius);
        //two rectangle parts
        pixmap.fillRectangle(cornerRadius, 0, width - cornerRadius * 2, height);
        pixmap.fillRectangle(0, cornerRadius, width, height - cornerRadius * 2);
        //draw rounded rectangle
        ret.setColor(color);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (pixmap.getPixel(x, y) != 0) ret.drawPixel(x, y);
            }
        }
        pixmap.dispose();
        return ret;
    }
    public void drawPixelstoMask(Pixmap pixmap, Pixmap mask){
        int pixmapWidth = mask.getWidth();
        int pixmapHeight = mask.getHeight();
        Color pixelColor = new Color();
        for (int x=0; x<pixmapWidth; x++){
            for (int y=0; y<pixmapHeight; y++){
                if (mask.getPixel(x, y) != 0) {
                    Color.rgba8888ToColor(pixelColor, pixmap.getPixel(x, y));
                    mask.setColor(pixelColor);
                    mask.drawPixel(x, y);
                }
            }
        }
    }

    public boolean isBorderless(String imagekey) {
        if (imagekey.length() > 7) {
            if ((!imagekey.substring(0, 7).contains("MPS_KLD"))&&(imagekey.substring(0, 4).contains("MPS_"))) //MPS_ sets except MPD_KLD
                return true;
        }
        return borderlessCardlistKey.contains(TextUtil.fastReplace(imagekey,".full",".fullborder"));
    }

    public static boolean isBorderless(Texture t) {
        //generated texture/pixmap?
        if (t.toString().contains("com.badlogic.gdx.graphics.Texture@"))
            return true;
        for (String key : borderlessCardlistKey) {
            if (t.toString().contains(key))
                return true;
        }
        return false;
    }

    public static String getpixelColor(Texture i) {
        if (!i.getTextureData().isPrepared()) {
            i.getTextureData().prepare(); //prepare texture
        }
        //get pixmap from texture data
        Pixmap pixmap = i.getTextureData().consumePixmap();
        //get pixel color from x,y texture coordinate based on the image fullborder or not
        Color color = new Color(pixmap.getPixel(croppedBorderImage(i).getRegionX()+1, croppedBorderImage(i).getRegionY()+1));
        pixmap.dispose();
        return color.toString();
    }
    public static boolean isCloserToWhite(String c){
        if (c == null || c == "")
            return false;
        int c_r = Integer.parseInt(c.substring(0,2),16);
        int c_g = Integer.parseInt(c.substring(2,4),16);
        int c_b = Integer.parseInt(c.substring(4,6),16);
        int brightness = ((c_r * 299) + (c_g * 587) + (c_b * 114)) / 1000;
        return brightness > 155;
    }
}
