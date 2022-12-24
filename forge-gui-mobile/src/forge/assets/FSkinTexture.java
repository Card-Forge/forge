package forge.assets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;

import forge.Forge;
import forge.Graphics;
import forge.localinstance.properties.ForgeConstants;

public enum FSkinTexture implements FImage {
    BG_TEXTURE(ForgeConstants.TEXTURE_BG_FILE, true, false),
    BG_MATCH(ForgeConstants.MATCH_BG_FILE, false, false),
    BG_MATCH_DAY(ForgeConstants.MATCH_BG_DAY_FILE, false, false),
    BG_MATCH_NIGHT(ForgeConstants.MATCH_BG_NIGHT_FILE, false, false),
    BG_SPACE(ForgeConstants.SPACE_BG_FILE, false, false),
    BG_CHAOS_WHEEL(ForgeConstants.CHAOS_WHEEL_IMG_FILE, false, false),
    //Adventure textures
    ADV_BG_TEXTURE(ForgeConstants.ADV_TEXTURE_BG_FILE, true, false),
    ADV_BG_MATCH(ForgeConstants.ADV_MATCH_BG_FILE, false, false),
    ADV_BG_MATCH_DAY(ForgeConstants.ADV_MATCH_BG_DAY_FILE, false, false),
    ADV_BG_MATCH_NIGHT(ForgeConstants.ADV_MATCH_BG_NIGHT_FILE, false, false),
    ADV_BG_SWAMP(ForgeConstants.ADV_BG_SWAMP_FILE, false, false),
    ADV_BG_FOREST(ForgeConstants.ADV_BG_FOREST_FILE, false, false),
    ADV_BG_MOUNTAIN(ForgeConstants.ADV_BG_MOUNTAIN_FILE, false, false),
    ADV_BG_ISLAND(ForgeConstants.ADV_BG_ISLAND_FILE, false, false),
    ADV_BG_PLAINS(ForgeConstants.ADV_BG_PLAINS_FILE, false, false),
    ADV_BG_WASTE(ForgeConstants.ADV_BG_WASTE_FILE, false, false),
    ADV_BG_COMMON(ForgeConstants.ADV_BG_COMMON_FILE, false, false),
    ADV_BG_CAVE(ForgeConstants.ADV_BG_CAVE_FILE, false, false),
    ADV_BG_DUNGEON(ForgeConstants.ADV_BG_DUNGEON_FILE, false, false),
    ADV_BG_CASTLE(ForgeConstants.ADV_BG_CASTLE_FILE, false, false),
    //CARD BG
    CARDBG_A (ForgeConstants.IMG_CARDBG_A, false, false),
    CARDBG_B (ForgeConstants.IMG_CARDBG_B, false, false),
    CARDBG_BG (ForgeConstants.IMG_CARDBG_BG, false, false),
    CARDBG_BR (ForgeConstants.IMG_CARDBG_BR, false, false),
    CARDBG_C (ForgeConstants.IMG_CARDBG_C, false, false),
    CARDBG_G (ForgeConstants.IMG_CARDBG_G, false, false),
    CARDBG_L (ForgeConstants.IMG_CARDBG_L, false, false),
    CARDBG_M (ForgeConstants.IMG_CARDBG_M, false, false),
    CARDBG_R (ForgeConstants.IMG_CARDBG_R, false, false),
    CARDBG_RG (ForgeConstants.IMG_CARDBG_RG, false, false),
    CARDBG_U (ForgeConstants.IMG_CARDBG_U, false, false),
    CARDBG_UB (ForgeConstants.IMG_CARDBG_UB, false, false),
    CARDBG_UG (ForgeConstants.IMG_CARDBG_UG, false, false),
    CARDBG_UR (ForgeConstants.IMG_CARDBG_UR, false, false),
    CARDBG_V (ForgeConstants.IMG_CARDBG_V, false, false),
    CARDBG_W (ForgeConstants.IMG_CARDBG_W, false, false),
    CARDBG_WB (ForgeConstants.IMG_CARDBG_WB, false, false),
    CARDBG_WG (ForgeConstants.IMG_CARDBG_WG, false, false),
    CARDBG_WR (ForgeConstants.IMG_CARDBG_WR, false, false),
    CARDBG_WU (ForgeConstants.IMG_CARDBG_WU, false, false),
    PWBG_B (ForgeConstants.IMG_PWBG_B, false, false),
    PWBG_BG (ForgeConstants.IMG_PWBG_BG, false, false),
    PWBG_BR (ForgeConstants.IMG_PWBG_BR, false, false),
    PWBG_C (ForgeConstants.IMG_PWBG_C, false, false),
    PWBG_G (ForgeConstants.IMG_PWBG_G, false, false),
    PWBG_M (ForgeConstants.IMG_PWBG_M, false, false),
    PWBG_R (ForgeConstants.IMG_PWBG_R, false, false),
    PWBG_RG (ForgeConstants.IMG_PWBG_RG, false, false),
    PWBG_U (ForgeConstants.IMG_PWBG_U, false, false),
    PWBG_UB (ForgeConstants.IMG_PWBG_UB, false, false),
    PWBG_UG (ForgeConstants.IMG_PWBG_UG, false, false),
    PWBG_UR (ForgeConstants.IMG_PWBG_UR, false, false),
    PWBG_W (ForgeConstants.IMG_PWBG_W, false, false),
    PWBG_WB (ForgeConstants.IMG_PWBG_WB, false, false),
    PWBG_WG (ForgeConstants.IMG_PWBG_WG, false, false),
    PWBG_WR (ForgeConstants.IMG_PWBG_WR, false, false),
    PWBG_WU (ForgeConstants.IMG_PWBG_WU, false, false),
    NYX_B (ForgeConstants.IMG_NYX_B, false, false),
    NYX_G (ForgeConstants.IMG_NYX_G, false, false),
    NYX_M (ForgeConstants.IMG_NYX_M, false, false),
    NYX_R (ForgeConstants.IMG_NYX_R, false, false),
    NYX_U (ForgeConstants.IMG_NYX_U, false, false),
    NYX_W (ForgeConstants.IMG_NYX_W, false, false),
    NYX_C (ForgeConstants.IMG_NYX_C, false, false),

    //Planechase
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
    Talon_Gates(ForgeConstants.BG_78, false, true),
    Planewide_Disaster(ForgeConstants.BG_79, false, true),
    Reality_Shaping(ForgeConstants.BG_80, false, true),
    Spatial_Merging(ForgeConstants.BG_81, false, true),
    Chaotic_Aether(ForgeConstants.BG_82, false, true),
    Interplanar_Tunnel(ForgeConstants.BG_83, false, true),
    Morphic_Tide(ForgeConstants.BG_84, false, true),
    Mutual_Epiphany(ForgeConstants.BG_85, false, true),
    Time_Distortion(ForgeConstants.BG_86, false, true);

    private final String filename;
    private final boolean repeat;
    private Texture texture;
    private final boolean isPlanechaseBG;
    private static List<String> PlanesValue;
    private boolean isloaded = false;
    private boolean hasError = false;

    FSkinTexture(String filename0, boolean repeat0, boolean isPlanechaseBG0) {
        filename = filename0;
        repeat = repeat0;
        isPlanechaseBG = isPlanechaseBG0;
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
        if (hasError)
            return;
        FileHandle preferredFile = isPlanechaseBG ? FSkin.getCachePlanechaseFile(filename) : FSkin.getSkinFile(filename);
        if (preferredFile.exists()) {
            try {
                if (preferredFile.path().contains("fallback_skin")) {
                    texture = new Texture(preferredFile);
                } else {
                    Forge.getAssets().manager().load(preferredFile.path(), Texture.class);
                    Forge.getAssets().manager().finishLoadingAsset(preferredFile.path());
                    texture = Forge.getAssets().manager().get(preferredFile.path(), Texture.class);
                }
                isloaded = true;
            }
            catch (final Exception e) {
                System.err.println("Failed to load skin file: " + preferredFile);
                e.printStackTrace();
                isloaded = false;
                hasError = true;
            }
        }
        if (texture == null) {
            //use default file if can't use preferred file
            FileHandle defaultFile = FSkin.getDefaultSkinFile(filename);
            if(isPlanechaseBG) {
                defaultFile = FSkin.getSkinFile(ForgeConstants.MATCH_BG_FILE);
                if(!defaultFile.exists())
                    defaultFile = FSkin.getDefaultSkinFile(ForgeConstants.MATCH_BG_FILE);
            }

            if (defaultFile.exists()) {
                try {
                    if (defaultFile.path().contains("fallback_skin")) {
                        texture = new Texture(defaultFile);
                    } else {
                        Forge.getAssets().manager().load(defaultFile.path(), Texture.class);
                        Forge.getAssets().manager().finishLoadingAsset(defaultFile.path());
                        texture = Forge.getAssets().manager().get(defaultFile.path(), Texture.class);
                    }
                    isloaded = true;
                }
                catch (final Exception e) {
                    System.err.println("Failed to load skin file: " + defaultFile);
                    e.printStackTrace();
                    isloaded = false;
                    hasError = true;
                    return;
                }
            }
            else {
                System.err.println("Failed to load skin file: " + defaultFile);
                isloaded = false;
                hasError = true;
                return;
            }
        }
        if (repeat) {
            texture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
        }
    }

    @Override
    public float getWidth() {
        if (!isloaded)
            load();
        if (hasError)
            return 0f;
        return texture.getWidth();
    }

    @Override
    public float getHeight() {
        if (!isloaded)
            load();
        if (hasError)
            return 0f;
        return texture.getHeight();
    }

    @Override
    public void draw(Graphics g, float x, float y, float w, float h) {
        if (!isloaded)
            load();
        if (hasError)
            return;
        if (repeat) {
            g.drawRepeatingImage(texture, x, y, w, h);
        } else {
            g.drawImage(texture, x, y, w, h);
        }
    }

    public void drawRotated(Graphics g, float x, float y, float w, float h, float rotation) {
        if (!isloaded)
            load();
        if (hasError)
            return;
        g.drawRotatedImage(texture, x, y, w, h, x + w / 2, y + h / 2, rotation);
    }

    public void drawFlipped(Graphics g, float x, float y, float w, float h) {
        if (!isloaded)
            load();
        if (hasError)
            return;
        g.drawFlippedImage(texture, x, y, w, h);
    }
}