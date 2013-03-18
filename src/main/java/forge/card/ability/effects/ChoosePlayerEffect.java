package forge.card.ability.effects;

import java.util.List;

import forge.Card;
import forge.Singletons;
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
                sa.getSourceCard(), sa.getParam("Choices"), sa) : Singletons.getModel().getGame().getPlayers();

        final String choiceDesc = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : "Choose a player";

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (p.isHuman()) {
                    // Was if (sa.getActivatingPlayer().isHuman()) but defined player was being
                    // overwritten by activatingPlayer (or controller if no activator was set).
                    // Revert if it causes issues and remove Goblin Festival from card database.
                    final Object o = GuiChoose.one(choiceDesc, choices);
                    if (null == o) {
                        return;
                    }
                    final Player chosen = (Player) o;
                    card.setChosenPlayer(chosen);

                } else {
                    if ("Curse".equals(sa.getParam("AILogic"))) {
                        for (Player pc : choices) {
                            if (pc.isOpponentOf(p)) {
                                card.setChosenPlayer(pc);
                                break;
                            }
                        }
                        if (card.getChosenPlayer() == null) {
                            System.out.println("No good curse choices. Picking first available: " + choices.get(0));
                            card.setChosenPlayer(choices.get(0));
                        }
                    } else if ("Pump".equals(sa.getParam("AILogic"))) {
                        card.setChosenPlayer(choices.contains(p) ? p : choices.get(0));
                    } else {
                        card.setChosenPlayer(p);
                    }
                }
            }
        }
    }
}
