package forge.card.ability.effects;

import java.util.HashMap;

import javax.swing.JOptionPane;

import forge.Card;
import forge.Singletons;
import forge.card.ability.AbilityFactory;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.trigger.TriggerType;
import forge.game.GameAction;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;

public class ClashEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getSourceCard().getName() + " - Clash with an opponent.";
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final boolean victory = clashWithOpponent(sa.getSourceCard());

        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Player", sa.getSourceCard().getController());

        if (victory) {
            if (sa.hasParam("WinSubAbility")) {
                final SpellAbility win = AbilityFactory.getAbility(
                        sa.getSourceCard().getSVar(sa.getParam("WinSubAbility")), sa.getSourceCard());
                win.setActivatingPlayer(sa.getSourceCard().getController());
                ((AbilitySub) win).setParent(sa);

                AbilityUtils.resolve(win, false);
            }
            runParams.put("Won", "True");
        } else {
            if (sa.hasParam("OtherwiseSubAbility")) {
                final SpellAbility otherwise = AbilityFactory.getAbility(
                        sa.getSourceCard().getSVar(sa.getParam("OtherwiseSubAbility")), sa.getSourceCard());
                otherwise.setActivatingPlayer(sa.getSourceCard().getController());
                ((AbilitySub) otherwise).setParent(sa);

                AbilityUtils.resolve(otherwise, false);
            }
            runParams.put("Won", "False");
        }

        Singletons.getModel().getGame().getTriggerHandler().runTrigger(TriggerType.Clashed, runParams, false);
    }

    /**
     * <p>
     * clashWithOpponent.
     * </p>
     * 
     * @param source
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean clashWithOpponent(final Card source) {
        /*
         * Each clashing player reveals the top card of his or her library, then
         * puts that card on the top or bottom. A player wins if his or her card
         * had a higher mana cost.
         * 
         * Clash you win or win you don't. There is no tie.
         */
        final Player player = source.getController();
        final Player opponent = player.getOpponent();
        final ZoneType lib = ZoneType.Library;
    
        final PlayerZone pLib = player.getZone(lib);
        final PlayerZone oLib = opponent.getZone(lib);
    
        final StringBuilder reveal = new StringBuilder();
    
        Card pCard = null;
        Card oCard = null;
    
        if (pLib.size() > 0) {
            pCard = pLib.get(0);
        }
        if (oLib.size() > 0) {
            oCard = oLib.get(0);
        }
    
        if ((pLib.size() == 0) && (oLib.size() == 0)) {
            return false;
        } else if (pLib.isEmpty()) {
            clashMoveToTopOrBottom(opponent, oCard);
            return false;
        } else if (oLib.isEmpty()) {
            clashMoveToTopOrBottom(player, pCard);
            return true;
        } else {
            final int pCMC = pCard.getCMC();
            final int oCMC = oCard.getCMC();
            
            // TODO: Split cards will return two CMC values, so both players may become winners of clash
            
            reveal.append(player).append(" reveals: ").append(pCard.getName()).append(".  CMC = ").append(pCMC);
            reveal.append("\r\n");
            reveal.append(opponent).append(" reveals: ").append(oCard.getName()).append(".  CMC = ").append(oCMC);
            reveal.append("\r\n\r\n");
            if (pCMC > oCMC) {
                reveal.append(player).append(" wins clash.");
            } else {
                reveal.append(player).append(" loses clash.");
            }
            JOptionPane.showMessageDialog(null, reveal.toString(), source.getName(), JOptionPane.PLAIN_MESSAGE);
            clashMoveToTopOrBottom(player, pCard);
            clashMoveToTopOrBottom(opponent, oCard);
            // JOptionPane.showMessageDialog(null, reveal.toString(),
            // source.getName(), JOptionPane.PLAIN_MESSAGE);
            return pCMC > oCMC;
        }
    }
    
    public final void clashMoveToTopOrBottom(final Player p, final Card c) {
        GameAction action = p.getGame().getAction();
        boolean putOnTop = p.getController().willPutCardOnTop(c);
        if ( putOnTop )
            action.moveToLibrary(c);
        else
            action.moveToBottomOfLibrary(c);
        
        // computer just puts the card back until such time it can make a smarter decision

    }


}
