package forge.card.ability.effects;

import java.util.List;

import forge.Card;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.gui.GuiChoose;

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

        final Target tgt = sa.getTarget();

        final List<Player> choices = sa.hasParam("Choices") ? AbilityUtils.getDefinedPlayers(
                sa.getSourceCard(), sa.getParam("Choices"), sa) : sa.getActivatingPlayer().getGame().getPlayers();

        final String choiceDesc = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : "Choose a player";

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                Player chosen = null;
                if (p.isHuman()) {
                    // Was if (sa.getActivatingPlayer().isHuman()) but defined player was being
                    // overwritten by activatingPlayer (or controller if no activator was set).
                    // Revert if it causes issues and remove Goblin Festival from card database.
                    chosen = GuiChoose.one(choiceDesc, choices);
                } else {
                    if ("Curse".equals(sa.getParam("AILogic"))) {
                        for (Player pc : choices) {
                            if (pc.isOpponentOf(p)) {
                                chosen = pc;
                                break;
                            }
                        }
                        if (chosen == null) {
                            System.out.println("No good curse choices. Picking first available: " + choices.get(0));
                            chosen = choices.get(0);
                        }
                    } else if ("Pump".equals(sa.getParam("AILogic"))) {
                        chosen = choices.contains(p) ? p : choices.get(0);
                    } else {
                        System.out.println("Default player choice logic.");
                        chosen = choices.contains(p) ? p : choices.get(0);
                    }
                }

                if( null != chosen )
                    card.setChosenPlayer(chosen);
            }
        }
    }
}
