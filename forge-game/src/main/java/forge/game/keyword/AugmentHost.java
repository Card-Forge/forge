package forge.game.keyword;

import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;

public class AugmentHost extends SimpleKeyword {
    protected String rightSideName = "";
    protected String leftSideSVar = "";
    protected String rightSideSVar = "";
    protected AbilitySub hostAbility;
    @Override
    protected void parse(String details) {
        String[] k = details.split(":");
        rightSideName = k[0];
        leftSideSVar = k[1];
        rightSideSVar = k[2];
    }

    public String getRightSideName() {
        return rightSideName;
    }

    public final void createTraits(final Card host, final boolean intrinsic) {
        super.createTraits(host, intrinsic);
        String left = host.getSVar(leftSideSVar);
        int colonPos = left.indexOf(':');
        String key = colonPos > 0 ? left.substring(0, colonPos) : left;
        String value = colonPos > 0 ? left.substring(1+colonPos).trim() : null;
        switch (key.charAt(0)) {
            case 'T': addTrigger(TriggerHandler.parseTrigger(value, host, true, host.getCurrentState()));
        }
        hostAbility = (AbilitySub) AbilityFactory.getAbility(host.getSVar(rightSideSVar), host);
    }

    public void combine(Augment augment) {
        SpellAbility sub;
        Trigger augmentTrigger = augment.augmentTrigger.copy(getHostCard(), false);
        AbilitySub hostAbility = (AbilitySub) this.hostAbility.copy(getHostCard(), false);
        if ((sub = augmentTrigger.getOverridingAbility()) != null) {
            while (sub.getSubAbility() != null) {
                sub = sub.getSubAbility();
            }
            sub.appendSubAbility(hostAbility);
        } else {
            augmentTrigger.setOverridingAbility(hostAbility);
        }
        augmentTrigger.putParam("TriggerDescription", augmentTrigger.getParam("TriggerDescription") + " " + hostAbility.getDescription());
        augmentTrigger.setHostCard(getHostCard());
        augmentTrigger.setCardState(getHostCard().getCurrentState());
        getTriggers().clear();
        getAbilities().clear();
        addTrigger(augmentTrigger);
        augment.createTraits(augment.getHostCard(), true, true);
    }
}
