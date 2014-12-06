package forge.planarconquest;

import forge.assets.FSkinProp;

public enum ConquestAction {
    AttackN(FSkinProp.IMG_ATTACK),
    AttackNE(FSkinProp.IMG_ATTACK),
    AttackE(FSkinProp.IMG_ATTACK),
    Defend(FSkinProp.IMG_DEFEND),
    Recruit(FSkinProp.IMG_ATTACK),
    Study(FSkinProp.ICO_QUEST_ZEP),
    Deploy(FSkinProp.IMG_SUMMONSICK),
    ReturnToBase(FSkinProp.ICO_QUEST_ZEP),
    Travel(FSkinProp.ICO_QUEST_ZEP);

    private final FSkinProp icon;

    private ConquestAction(FSkinProp icon0) {
        icon = icon0;
    }

    public FSkinProp getIcon() {
        return icon;
    }
}
