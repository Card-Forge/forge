package forge.assets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;

import forge.Graphics;
import forge.localinstance.properties.ForgeConstants;

public enum FSkinTexture implements FImage {
    BG_TEXTURE(ForgeConstants.TEXTURE_BG_FILE, true, false),
    BG_MATCH(ForgeConstants.MATCH_BG_FILE, false, false),
    BG_MATCH_DAY(ForgeConstants.MATCH_BG_DAY_FILE, false, false),
    BG_MATCH_NIGHT(ForgeConstants.MATCH_BG_NIGHT_FILE, false, false),
    BG_SPACE(ForgeConstants.SPACE_BG_FILE, false, false),
    BG_CHAOS_WHEEL(ForgeConstants.CHAOS_WHEEL_IMG_FILE, false, false),
    Academy_at_Tolaria_West(ForgeConstants.BG_1, false, true),
    Agyrem(ForgeConstants.BG_2, false, true),
    Akoum(ForgeConstants.BG_3, false, true),
    Aretopolis(ForgeConstants.BG_4, false, true),
    Astral_Arena(ForgeConstants.BG_5, false, true),
    Bant(ForgeConstants.BG_6, false, true),
    Bloodhill_Bastion(ForgeConstants.BG_7, false, true),
    Cliffside_Market(ForgeConstants.BG_8, false, true),
    Edge_of_Malacol(ForgeConstants.BG_9, false, true),
    Eloren_Wilds(ForgeConstants.BG_10, false, true),
    Feeding_Grounds(ForgeConstants.BG_11, false, true),
    Fields_of_Summer(ForgeConstants.BG_12, false, true),
    Furnace_Layer(ForgeConstants.BG_13, false, true),
    Gavony(ForgeConstants.BG_14, false, true),
    Glen_Elendra(ForgeConstants.BG_15, false, true),
    Glimmervoid_Basin(ForgeConstants.BG_16, false, true),
    Goldmeadow(ForgeConstants.BG_17, false, true),
    Grand_Ossuary(ForgeConstants.BG_18, false, true),
    Grixis(ForgeConstants.BG_19, false, true),
    Grove_of_the_Dreampods(ForgeConstants.BG_20, false, true),
    Hedron_Fields_of_Agadeem(ForgeConstants.BG_21, false, true),
    Immersturm(ForgeConstants.BG_22, false, true),
    Isle_of_Vesuva(ForgeConstants.BG_23, false, true),
    Izzet_Steam_Maze(ForgeConstants.BG_24, false, true),
    Jund(ForgeConstants.BG_25, false, true),
    Kessig(ForgeConstants.BG_26, false, true),
    Kharasha_Foothills(ForgeConstants.BG_27, false, true),
    Kilnspire_District(ForgeConstants.BG_28, false, true),
    Krosa(ForgeConstants.BG_29, false, true),
    Lair_of_the_Ashen_Idol(ForgeConstants.BG_30, false, true),
    Lethe_Lake(ForgeConstants.BG_31, false, true),
    Llanowar(ForgeConstants.BG_32, false, true),
    Minamo(ForgeConstants.BG_33, false, true),
    Mount_Keralia(ForgeConstants.BG_34, false, true),
    Murasa(ForgeConstants.BG_35, false, true),
    Naar_Isle(ForgeConstants.BG_36, false, true),
    Naya(ForgeConstants.BG_37, false, true),
    Nephalia(ForgeConstants.BG_38, false, true),
    Norns_Dominion(ForgeConstants.BG_39, false, true),
    Onakke_Catacomb(ForgeConstants.BG_40, false, true),
    Orochi_Colony(ForgeConstants.BG_41, false, true),
    Orzhova(ForgeConstants.BG_42, false, true),
    Otaria(ForgeConstants.BG_43, false, true),
    Panopticon(ForgeConstants.BG_44, false, true),
    Pools_of_Becoming(ForgeConstants.BG_45, false, true),
    Prahv(ForgeConstants.BG_46, false, true),
    Quicksilver_Sea(ForgeConstants.BG_47, false, true),
    Ravens_Run(ForgeConstants.BG_48, false, true),
    Sanctum_of_Serra(ForgeConstants.BG_49, false, true),
    Sea_of_Sand(ForgeConstants.BG_50, false, true),
    Selesnya_Loft_Gardens(ForgeConstants.BG_51, false, true),
    Shiv(ForgeConstants.BG_52, false, true),
    Skybreen(ForgeConstants.BG_53, false, true),
    Sokenzan(ForgeConstants.BG_54, false, true),
    Stairs_to_Infinity(ForgeConstants.BG_55, false, true),
    Stensia(ForgeConstants.BG_56, false, true),
    Stronghold_Furnace(ForgeConstants.BG_57, false, true),
    Takenuma(ForgeConstants.BG_58, false, true),
    Tazeem(ForgeConstants.BG_59, false, true),
    The_Aether_Flues(ForgeConstants.BG_60, false, true),
    The_Dark_Barony(ForgeConstants.BG_61, false, true),
    The_Eon_Fog(ForgeConstants.BG_62, false, true),
    The_Fourth_Sphere(ForgeConstants.BG_63, false, true),
    The_Great_Forest(ForgeConstants.BG_64, false, true),
    The_Hippodrome(ForgeConstants.BG_65, false, true),
    The_Maelstrom(ForgeConstants.BG_66, false, true),
    The_Zephyr_Maze(ForgeConstants.BG_67, false, true),
    Trail_of_the_MageRings(ForgeConstants.BG_68, false, true),
    Truga_Jungle(ForgeConstants.BG_69, false, true),
    Turri_Island(ForgeConstants.BG_70, false, true),
    Undercity_Reaches(ForgeConstants.BG_71, false, true),
    Velis_Vel(ForgeConstants.BG_72, false, true),
    Windriddle_Palaces(ForgeConstants.BG_73, false, true),
    Tember_City(ForgeConstants.BG_74, false, true),
    Celestine_Reef(ForgeConstants.BG_75, false, true),
    Horizon_Boughs(ForgeConstants.BG_76, false, true),
    Mirrored_Depths(ForgeConstants.BG_77, false, true),
    Talon_Gates(ForgeConstants.BG_78, false, true);

    private final String filename;
    private final boolean repeat;
    private Texture texture;
    private final boolean isPlane;
    private static List<String> PlanesValue;

    FSkinTexture(String filename0, boolean repeat0, boolean isPlane0) {
        filename = filename0;
        repeat = repeat0;
        isPlane = isPlane0;
    }

    static {
        PlanesValue = new ArrayList<>();
        for (FSkinTexture PlanesEnum : FSkinTexture.values()) {
                PlanesValue.add(PlanesEnum.filename
                        .replace(".jpg", "")
                        .replace("'", "")
                        .replace("-", ""));
        }
    }

    public static List<String> getValues() {
        return Collections.unmodifiableList(PlanesValue);
    }

    public void load() {
        FileHandle preferredFile = isPlane ? FSkin.getCachePlanechaseFile(filename) : FSkin.getSkinFile(filename);
        if (preferredFile.exists()) {
            try {
                texture = new Texture(preferredFile);
            }
            catch (final Exception e) {
                System.err.println("Failed to load skin file: " + preferredFile);
                e.printStackTrace();
            }
        }
        if (texture == null) {
            //use default file if can't use preferred file
            FileHandle defaultFile = FSkin.getDefaultSkinFile(filename);
            if(isPlane) {
                defaultFile = FSkin.getSkinFile(ForgeConstants.MATCH_BG_FILE);
                if(!defaultFile.exists())
                    defaultFile = FSkin.getDefaultSkinFile(ForgeConstants.MATCH_BG_FILE);
            }

            if (defaultFile.exists()) {
                try {
                    texture = new Texture(defaultFile);
                }
                catch (final Exception e) {
                    System.err.println("Failed to load skin file: " + defaultFile);
                    e.printStackTrace();
                    return;
                }
            }
            else {
                System.err.println("Failed to load skin file: " + defaultFile);
                return;
            }
        }
        if (repeat) {
            texture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
        }
    }

    @Override
    public float getWidth() {
        return texture.getWidth();
    }

    @Override
    public float getHeight() {
        return texture.getHeight();
    }

    @Override
    public void draw(Graphics g, float x, float y, float w, float h) {
        if (repeat) {
            g.drawRepeatingImage(texture, x, y, w, h);
        }
        else {
            g.drawImage(texture, x, y, w, h);
        }
    }

    public void drawRotated(Graphics g, float x, float y, float w, float h, float rotation) {
        g.drawRotatedImage(texture, x, y, w, h, x + w / 2, y + h / 2, rotation);
    }

    public void drawFlipped(Graphics g, float x, float y, float w, float h) {
        g.drawFlippedImage(texture, x, y, w, h);
    }
}