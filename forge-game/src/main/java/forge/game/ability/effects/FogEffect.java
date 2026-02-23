package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.SpellAbility;

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

        game.getAction().moveToCommand(eff, sa);

        game.getEndOfTurn().addUntil(() -> game.getAction().exileEffect(eff));
    }
}
