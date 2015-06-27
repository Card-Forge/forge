package forge.planarconquest;

import forge.GuiBase;
import forge.assets.FSkinProp;
import forge.assets.ISkinImage;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.item.PaperCard;
import forge.planarconquest.ConquestPlane.Region;

public class ConquestOpponent {
    private final PaperCard commander;
    @SuppressWarnings("unused")
    private final Region region;
    private final ISkinImage mapIcon;

    public ConquestOpponent(PaperCard commander0, Region region0) {
        commander = commander0;
        region = region0;
        mapIcon = GuiBase.getInterface().getSkinIcon(determineMapIcon());
    }

    //determine map icon from commander color set
    private FSkinProp determineMapIcon() {
        ColorSet colors = commander.getRules().getColorIdentity();
        switch (colors.countColors()) {
        case 0:
            return FSkinProp.IMG_MANA_COLORLESS;
        case 1:
            switch (colors.getColor()) {
            case MagicColor.WHITE:
                return FSkinProp.IMG_MANA_W;
            case MagicColor.BLUE:
                return FSkinProp.IMG_MANA_U;
            case MagicColor.BLACK:
                return FSkinProp.IMG_MANA_B;
            case MagicColor.RED:
                return FSkinProp.IMG_MANA_R;
            default:
                return FSkinProp.IMG_MANA_G;
            }
        case 2:
            switch (colors.getColor()) {
            case MagicColor.WHITE | MagicColor.BLUE:
                return FSkinProp.IMG_MANA_HYBRID_WU;
            case MagicColor.BLUE | MagicColor.BLACK:
                return FSkinProp.IMG_MANA_HYBRID_UB;
            case MagicColor.BLACK | MagicColor.RED:
                return FSkinProp.IMG_MANA_HYBRID_BR;
            case MagicColor.RED | MagicColor.GREEN:
                return FSkinProp.IMG_MANA_HYBRID_RG;
            case MagicColor.GREEN | MagicColor.WHITE:
                return FSkinProp.IMG_MANA_HYBRID_GW;
            case MagicColor.WHITE | MagicColor.BLACK:
                return FSkinProp.IMG_MANA_HYBRID_WB;
            case MagicColor.BLUE | MagicColor.RED:
                return FSkinProp.IMG_MANA_HYBRID_UR;
            case MagicColor.BLACK | MagicColor.GREEN:
                return FSkinProp.IMG_MANA_HYBRID_BG;
            case MagicColor.RED | MagicColor.WHITE:
                return FSkinProp.IMG_MANA_HYBRID_RW;
            default:
                return FSkinProp.IMG_MANA_HYBRID_GU;
            }
        default:
            return FSkinProp.IMG_MULTI;
        }
    }

    public ISkinImage getMapIcon() {
        return mapIcon;
    }
}
