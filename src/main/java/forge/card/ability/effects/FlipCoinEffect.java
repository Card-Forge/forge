package forge.card.ability.effects;

import java.util.List;

import forge.Card;
import forge.card.ability.AbilityFactory;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.gui.GuiDialog;

public class FlipCoinEffect extends SpellEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final Player player = host.getController();

        final StringBuilder sb = new StringBuilder();

        sb.append(player).append(" flips a coin.");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final Player player = host.getController();

        final List<Player> caller = AbilityUtils.getDefinedPlayers(sa.getSourceCard(), sa.getParam("Caller"), sa);
        if (caller.isEmpty()) {
            caller.add(player);
        }

        final AbilityFactory afOutcomes = new AbilityFactory();
        final boolean victory = GuiDialog.flipCoin(caller.get(0), sa.getSourceCard());

        // Run triggers
        // HashMap<String,Object> runParams = new HashMap<String,Object>();
        // runParams.put("Player", player);
        if (sa.getParam("RememberAll") != null) {
            host.addRemembered(host);
        }

        if (victory) {
            if (sa.getParam("RememberWinner") != null) {
                host.addRemembered(host);
            }
            if (sa.hasParam("WinSubAbility")) {
                final SpellAbility win = afOutcomes.getAbility(host.getSVar(sa.getParam("WinSubAbility")), host);
                win.setActivatingPlayer(player);
                ((AbilitySub) win).setParent(sa);

                AbilityUtils.resolve(win, false);
            }
            // runParams.put("Won","True");
        } else {
            if (sa.getParam("RememberLoser") != null) {
                host.addRemembered(host);
            }
            if (sa.hasParam("LoseSubAbility")) {
                final SpellAbility lose = afOutcomes.getAbility(host.getSVar(sa.getParam("LoseSubAbility")), host);
                lose.setActivatingPlayer(player);
                ((AbilitySub) lose).setParent(sa);

                AbilityUtils.resolve(lose, false);
            }
            // runParams.put("Won","False");
        }

        // AllZone.getTriggerHandler().runTrigger("FlipsACoin",runParams);
    }

}
