package forge.assets;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;

import forge.Graphics;
import forge.properties.ForgeConstants;

public enum FSkinTexture implements FImage {
    BG_TEXTURE(ForgeConstants.TEXTURE_BG_FILE, true, false),
    BG_MATCH(ForgeConstants.MATCH_BG_FILE, false, false),
    BG_SPACE(ForgeConstants.SPACE_BG_FILE, false, false),
    BG_CHAOS_WHEEL(ForgeConstants.CHAOS_WHEEL_IMG_FILE, false, false),
    BG_PLANE1(ForgeConstants.BG_1, false, true),
    BG_PLANE2(ForgeConstants.BG_2, false, true),
    BG_PLANE3(ForgeConstants.BG_3, false, true),
    BG_PLANE4(ForgeConstants.BG_4, false, true),
    BG_PLANE5(ForgeConstants.BG_5, false, true),
    BG_PLANE6(ForgeConstants.BG_6, false, true),
    BG_PLANE7(ForgeConstants.BG_7, false, true),
    BG_PLANE8(ForgeConstants.BG_8, false, true),
    BG_PLANE9(ForgeConstants.BG_9, false, true),
    BG_PLANE10(ForgeConstants.BG_10, false, true),
    BG_PLANE11(ForgeConstants.BG_11, false, true),
    BG_PLANE12(ForgeConstants.BG_12, false, true),
    BG_PLANE13(ForgeConstants.BG_13, false, true),
    BG_PLANE14(ForgeConstants.BG_14, false, true),
    BG_PLANE15(ForgeConstants.BG_15, false, true),
    BG_PLANE16(ForgeConstants.BG_16, false, true),
    BG_PLANE17(ForgeConstants.BG_17, false, true),
    BG_PLANE18(ForgeConstants.BG_18, false, true),
    BG_PLANE19(ForgeConstants.BG_19, false, true),
    BG_PLANE20(ForgeConstants.BG_20, false, true),
    BG_PLANE21(ForgeConstants.BG_21, false, true),
    BG_PLANE22(ForgeConstants.BG_22, false, true),
    BG_PLANE23(ForgeConstants.BG_23, false, true),
    BG_PLANE24(ForgeConstants.BG_24, false, true),
    BG_PLANE25(ForgeConstants.BG_25, false, true),
    BG_PLANE26(ForgeConstants.BG_26, false, true),
    BG_PLANE27(ForgeConstants.BG_27, false, true),
    BG_PLANE28(ForgeConstants.BG_28, false, true),
    BG_PLANE29(ForgeConstants.BG_29, false, true),
    BG_PLANE30(ForgeConstants.BG_30, false, true),
    BG_PLANE31(ForgeConstants.BG_31, false, true),
    BG_PLANE32(ForgeConstants.BG_32, false, true),
    BG_PLANE33(ForgeConstants.BG_33, false, true),
    BG_PLANE34(ForgeConstants.BG_34, false, true),
    BG_PLANE35(ForgeConstants.BG_35, false, true),
    BG_PLANE36(ForgeConstants.BG_36, false, true),
    BG_PLANE37(ForgeConstants.BG_37, false, true),
    BG_PLANE38(ForgeConstants.BG_38, false, true),
    BG_PLANE39(ForgeConstants.BG_39, false, true),
    BG_PLANE40(ForgeConstants.BG_40, false, true),
    BG_PLANE41(ForgeConstants.BG_41, false, true),
    BG_PLANE42(ForgeConstants.BG_42, false, true),
    BG_PLANE43(ForgeConstants.BG_43, false, true),
    BG_PLANE44(ForgeConstants.BG_44, false, true),
    BG_PLANE45(ForgeConstants.BG_45, false, true),
    BG_PLANE46(ForgeConstants.BG_46, false, true),
    BG_PLANE47(ForgeConstants.BG_47, false, true),
    BG_PLANE48(ForgeConstants.BG_48, false, true),
    BG_PLANE49(ForgeConstants.BG_49, false, true),
    BG_PLANE50(ForgeConstants.BG_50, false, true),
    BG_PLANE51(ForgeConstants.BG_51, false, true),
    BG_PLANE52(ForgeConstants.BG_52, false, true),
    BG_PLANE53(ForgeConstants.BG_53, false, true),
    BG_PLANE54(ForgeConstants.BG_54, false, true),
    BG_PLANE55(ForgeConstants.BG_55, false, true),
    BG_PLANE56(ForgeConstants.BG_56, false, true),
    BG_PLANE57(ForgeConstants.BG_57, false, true),
    BG_PLANE58(ForgeConstants.BG_58, false, true),
    BG_PLANE59(ForgeConstants.BG_59, false, true),
    BG_PLANE60(ForgeConstants.BG_60, false, true),
    BG_PLANE61(ForgeConstants.BG_61, false, true),
    BG_PLANE62(ForgeConstants.BG_62, false, true),
    BG_PLANE63(ForgeConstants.BG_63, false, true),
    BG_PLANE64(ForgeConstants.BG_64, false, true),
    BG_PLANE65(ForgeConstants.BG_65, false, true),
    BG_PLANE66(ForgeConstants.BG_66, false, true),
    BG_PLANE67(ForgeConstants.BG_67, false, true),
    BG_PLANE68(ForgeConstants.BG_68, false, true),
    BG_PLANE69(ForgeConstants.BG_69, false, true),
    BG_PLANE70(ForgeConstants.BG_70, false, true),
    BG_PLANE71(ForgeConstants.BG_71, false, true),
    BG_PLANE72(ForgeConstants.BG_72, false, true),
    BG_PLANE73(ForgeConstants.BG_73, false, true),
    BG_PLANE74(ForgeConstants.BG_74, false, true),
    BG_PLANE75(ForgeConstants.BG_75, false, true),
    BG_PLANE76(ForgeConstants.BG_76, false, true),
    BG_PLANE77(ForgeConstants.BG_77, false, true),
    BG_PLANE78(ForgeConstants.BG_78, false, true);

    private final String filename;
    private final boolean repeat;
    private Texture texture;
    private final boolean isPlane;

    FSkinTexture(String filename0, boolean repeat0, boolean isPlane0) {
        filename = filename0;
        repeat = repeat0;
        isPlane = isPlane0;
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