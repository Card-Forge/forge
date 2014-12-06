package forge.planarconquest;

import forge.assets.FSkinProp;

public enum ConquestAction {
    AttackN(FSkinProp.IMG_ATTACK),
    AttackNE(FSkinProp.IMG_ATTACK),
    AttackE(FSkinProp.IMG_ATTACK),
    Defend(FSkinProp.IMG_DEFEND),
    Recruit(FSkinProp.IMG_PHASING),
    Study(FSkinProp.IMG_COSTRESERVED),
    Deploy(FSkinProp.IMG_SUMMONSICK),
    ReturnToBase(FSkinProp.IMG_SUMMONSICK),
    Travel(FSkinProp.IMG_SUMMONSICK);

    private final FSkinProp icon;

    private ConquestAction(FSkinProp icon0) {
        icon = icon0;
    }

    public FSkinProp getIcon() {
        return icon;
    }
}
