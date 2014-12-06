package forge.planarconquest;

import forge.assets.FSkinProp;

public enum ConquestAction {
    Attack1(FSkinProp.IMG_ATTACK),
    Attack2(FSkinProp.IMG_ATTACK),
    Attack3(FSkinProp.IMG_ATTACK),
    Defend(FSkinProp.IMG_DEFEND),
    Recruit(FSkinProp.IMG_PHASING),
    Study(FSkinProp.IMG_COSTRESERVED),
    Deploy(FSkinProp.IMG_SUMMONSICK),
    Undeploy(FSkinProp.IMG_SUMMONSICK),
    Travel(FSkinProp.IMG_SUMMONSICK);

    private final FSkinProp icon;

    private ConquestAction(FSkinProp icon0) {
        icon = icon0;
    }

    public FSkinProp getIcon() {
        return icon;
    }
}
