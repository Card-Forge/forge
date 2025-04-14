package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.ai.SpellApiToAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;

public class ClassLevelUpAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        Card host = sa.getHostCard();
        final int level = host.getClassLevel() + 1;
        for (StaticAbility stAb : host.getStaticAbilities()) {
            if (!stAb.hasParam("AddTrigger") || !stAb.isClassLevelNAbility(level)) {
                continue;
            }
            for (String sTrig : stAb.getParam("AddTrigger").split(" & ")) {
                Trigger t = host.getTriggerForStaticAbility(AbilityUtils.getSVar(stAb, sTrig), stAb);
                if (t.getMode() != TriggerType.ClassLevelGained) {
                    continue;
                }
                SpellAbility effect = t.ensureAbility();
                if (!SpellApiToAi.Converter.get(effect).doTriggerAI(aiPlayer, effect, false)) {
                    return false;
                }
            }
        }
        return true;
    }

}
