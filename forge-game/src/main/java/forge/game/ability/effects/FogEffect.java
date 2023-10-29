package forge.game.ability.effects;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

public class FogEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getHostCard().getController() + " prevents all combat damage this turn.";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card hostCard = sa.getHostCard();
        final Game game = hostCard.getGame();
        final String name = hostCard + "'s Effect";
        final String image = hostCard.getImageKey();
        StringBuilder sb = new StringBuilder("Event$ DamageDone | ActiveZones$ Command | IsCombat$ True");
        sb.append(" | Prevent$ True | Description$ Prevent all combat damage this turn.");
        String repeffstr = sb.toString();

        final Card eff = createEffect(sa, hostCard.getController(), name, image);
        ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, eff, true);
        eff.addReplacementEffect(re);

        game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        game.getAction().moveTo(ZoneType.Command, eff, sa, AbilityKey.newMap());
        eff.updateStateForView();
        game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);

        game.getEndOfTurn().addUntil(new GameCommand() {
            private static final long serialVersionUID = -3297629217432253089L;

            @Override
            public void run() {
                game.getAction().exile(eff, null, null);
            }
        });
    }
}
