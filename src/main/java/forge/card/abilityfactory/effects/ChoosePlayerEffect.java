package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.gui.GuiChoose;

public class ChoosePlayerEffect extends SpellEffect {
    
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
        
        final ArrayList<Player> choices = sa.hasParam("Choices") ? AbilityFactory.getDefinedPlayers(
                sa.getSourceCard(), sa.getParam("Choices"), sa) : new ArrayList<Player>(Singletons.getModel().getGame().getPlayers());
        
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
                    if (sa.hasParam("AILogic")) {
                        if (sa.getParam("AILogic").equals("Curse")) {
                            card.setChosenPlayer(p.getOpponent());
                        } else {
                            card.setChosenPlayer(p);
                        }
                    } else {
                        card.setChosenPlayer(p);
                    }
                }
            }
        }
    }
}