package forge.game.ability.effects;

import java.util.Map;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;

public class ClassLevelUpEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final int level = host.getClassLevel() + 1;
        host.setClassLevel(level);

        // need to run static ability to get Trigger online
        game.getAction().checkStaticAbilities();

        // Re-register triggers for target card
        game.getTriggerHandler().clearActiveTriggers(host, null);
        game.getTriggerHandler().registerActiveTrigger(host, false);

        // Run ClassLevelGained trigger
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(host);
        runParams.put(AbilityKey.ClassLevel, level);
        game.getTriggerHandler().runTrigger(TriggerType.ClassLevelGained, runParams, false);
    }
}
