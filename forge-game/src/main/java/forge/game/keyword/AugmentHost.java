package forge.game.keyword;

import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;

import java.util.HashMap;
import java.util.Map;

public class AugmentHost extends SimpleKeyword {
    protected String rightSideName = "";
    protected String leftSideSVar = "";
    protected String rightSideSVar = "";
    protected AbilitySub hostAbility;
    protected Map<String, String> transferredParams;
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
        transferredParams = new HashMap<>();
        switch (key.charAt(0)) {
            case 'T': Trigger t = TriggerHandler.parseTrigger(value, host, true, host.getCurrentState());
                      if (t.hasParam("OptionalDecider")) {
                          transferredParams.put("OptionalDecider", t.getParam("OptionalDecider"));
                      }
                      addTrigger(t);
                      break;
            case 'A': addSpellAbility(AbilityFactory.getAbility(host.getSVar(leftSideSVar), host));
                      break;
        }
        hostAbility = (AbilitySub) AbilityFactory.getAbility(host.getSVar(rightSideSVar), host);
        hostAbility.setSVars(host.getSVars());
    }

    public void combine(Augment augment) {

        if (augment.augmentTrigger != null) {
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
            for (String key : transferredParams.keySet()) {
                augmentTrigger.putParam(key, transferredParams.get(key));
            }
            augmentTrigger.setHostCard(getHostCard());
            augmentTrigger.setCardState(getHostCard().getCurrentState());
            getTriggers().clear();
            getAbilities().clear();
            addTrigger(augmentTrigger);
            augment.createTraits(augment.getHostCard(), true, true);
        } else if (augment.augmentActivated != null) {
            SpellAbility augmentActivated = augment.augmentActivated.copy(getHostCard(), false);
            AbilitySub hostAbility = (AbilitySub) this.hostAbility.copy(getHostCard(), false);
            SpellAbility sub = augmentActivated;
            while (sub.getSubAbility() != null) {
                sub = sub.getSubAbility();
            }
            sub.appendSubAbility(hostAbility);
            augmentActivated.putParam("SpellDescription", augmentActivated.getParam("SpellDescription") + hostAbility.getDescription());
            augmentActivated.setHostCard(getHostCard());
            augmentActivated.setCardState(getHostCard().getCurrentState());
            getTriggers().clear();
            getAbilities().clear();
            addSpellAbility(augmentActivated);
            augment.createTraits(augment.getHostCard(), true, true);
        }
    }
}
