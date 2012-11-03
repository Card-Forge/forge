package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Map;

import forge.Card;
import forge.GameActionUtil;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

public class FlipCoinEffect extends SpellEffect {
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(Map<String, String> params, SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final Player player = params.containsKey("OpponentCalls") ? host.getController().getOpponent() : host
                .getController();

        final StringBuilder sb = new StringBuilder();

        sb.append(player).append(" flips a coin.");
        return sb.toString();
    }

    /**
     * <p>
     * flipResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final Player player = host.getController();

        final ArrayList<Player> caller = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Caller"), sa);
        if (caller.size() == 0) {
            caller.add(player);
        }

        final AbilityFactory afOutcomes = new AbilityFactory();
        final boolean victory = GameActionUtil.flipACoin(caller.get(0), sa.getSourceCard());

        // Run triggers
        // HashMap<String,Object> runParams = new HashMap<String,Object>();
        // runParams.put("Player", player);
        if (params.get("RememberAll") != null) {
            host.addRemembered(host);
        }

        if (victory) {
            if (params.get("RememberWinner") != null) {
                host.addRemembered(host);
            }
            if (params.containsKey("WinSubAbility")) {
                final SpellAbility win = afOutcomes.getAbility(host.getSVar(params.get("WinSubAbility")), host);
                win.setActivatingPlayer(player);
                ((AbilitySub) win).setParent(sa);

                AbilityFactory.resolve(win, false);
            }
            // runParams.put("Won","True");
        } else {
            if (params.get("RememberLoser") != null) {
                host.addRemembered(host);
            }
            if (params.containsKey("LoseSubAbility")) {
                final SpellAbility lose = afOutcomes.getAbility(host.getSVar(params.get("LoseSubAbility")), host);
                lose.setActivatingPlayer(player);
                ((AbilitySub) lose).setParent(sa);

                AbilityFactory.resolve(lose, false);
            }
            // runParams.put("Won","False");
        }

        // AllZone.getTriggerHandler().runTrigger("FlipsACoin",runParams);
    }

    // *************************************************************************
    // ***************************** TwoPiles **********************************
    // *************************************************************************

}