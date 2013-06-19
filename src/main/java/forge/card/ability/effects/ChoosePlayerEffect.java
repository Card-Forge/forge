package forge.card.ability.effects;

import java.util.List;

import forge.Card;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.TargetRestrictions;
import forge.game.player.Player;

public class ChoosePlayerEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        for (final Player p : getTargetPlayers(sa)) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a player.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getSourceCard();

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        final TargetRestrictions tgt = sa.getTargetRestrictions();

        final List<Player> choices = sa.hasParam("Choices") ? AbilityUtils.getDefinedPlayers(
                sa.getSourceCard(), sa.getParam("Choices"), sa) : sa.getActivatingPlayer().getGame().getPlayers();

        final String choiceDesc = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : "Choose a player";

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {

                // Was if (sa.getActivatingPlayer().isHuman()) but defined player was being
                // overwritten by activatingPlayer (or controller if no activator was set).
                // Revert if it causes issues and remove Goblin Festival from card database.

                Player chosen = choices.isEmpty() ? null : p.getController().chooseSinglePlayerForEffect(choices, sa, choiceDesc);

                if( null != chosen )
                    card.setChosenPlayer(chosen);
            }
        }
    }
}
