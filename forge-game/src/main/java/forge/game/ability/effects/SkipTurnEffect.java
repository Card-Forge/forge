package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.replacement.ReplacementLayer;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

public class SkipTurnEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final int numTurns = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumTurns"), sa);

        for (final Player player : getTargetPlayers(sa)) {
            sb.append(player).append(" ");
        }

        sb.append("skips his/her next ").append(numTurns).append(" turn(s).");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card hostCard = sa.getHostCard();
        final Game game = hostCard.getGame();
        final String name = hostCard + "'s Effect";
        final String image = hostCard.getImageKey();
        final int numTurns = AbilityUtils.calculateAmount(hostCard, sa.getParam("NumTurns"), sa);
        String repeffstr = "Event$ BeginTurn | ActiveZones$ Command | ValidPlayer$ You " +
        "| Description$ Skip your next " + (numTurns > 1 ? Lang.getNumeral(numTurns) + " turns." : "turn.");
        String effect = "DB$ StoreSVar | SVar$ NumTurns | Type$ CountSVar | Expression$ NumTurns/Minus.1";
        String exile = "DB$ ChangeZone | Defined$ Self | Origin$ Command | Destination$ Exile " +
        "| ConditionCheckSVar$ NumTurns | ConditionSVarCompare$ EQ0";

        for (final Player player : getTargetPlayers(sa)) {
            final Card eff = createEffect(sa, player, name, image);
            eff.setSVar("NumTurns", "Number$" + numTurns);
            SpellAbility calcTurn = AbilityFactory.getAbility(effect, eff);
            calcTurn.setSubAbility((AbilitySub) AbilityFactory.getAbility(exile, eff));

            ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, eff, true);
            // Set to layer to Control so it will be applied before "would begin your turn" replacement effects
            // (Any layer before Other is OK, since default layer is Other.)
            re.setLayer(ReplacementLayer.Control);
            re.setOverridingAbility(calcTurn);
            eff.addReplacementEffect(re);

            game.getAction().moveToCommand(eff, sa);
        }
    }
}
