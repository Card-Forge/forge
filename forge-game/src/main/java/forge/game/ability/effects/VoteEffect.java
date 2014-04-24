package forge.game.ability.effects;

import com.google.common.collect.Iterables;

import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class VoteEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        return StringUtils.join(getTargetPlayers(sa), ", ") + " vote";
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final List<Player> tgtPlayers = getTargetPlayers(sa);
        final String voteType = sa.getParam("VoteType");
        final Card host = sa.getHostCard();
        // starting with the activator
        int pSize = tgtPlayers.size();
        Player activator = sa.getActivatingPlayer();
        while (tgtPlayers.contains(activator) && !activator.equals(Iterables.getFirst(tgtPlayers, null))) {
            tgtPlayers.add(pSize - 1, tgtPlayers.remove(0));
        }
        int choice1 = 0;
        int choice2 = 0;

        for (final Player p : tgtPlayers) {
            final boolean result = p.getController().chooseBinary(sa, sa.getHostCard() + "Vote:", PlayerController.BinaryChoiceType.valueOf(voteType));
            if (result) {
                choice1++;
            } else {
                choice2++;
            }
            host.getGame().getAction().nofityOfValue(sa, p, voteType.split("Or")[result ? 0 : 1], null);
        }
        String subAb;
        if (choice1 > choice2) {
            subAb = sa.getParam("FirstChoice");
        } else if (choice1 < choice2) {
            subAb = sa.getParam("SecondChoice");
        } else {
            subAb = sa.getParam("Tied");
        }
        final SpellAbility action = AbilityFactory.getAbility(host.getSVar(subAb), host);
        action.setActivatingPlayer(sa.getActivatingPlayer());
        ((AbilitySub) action).setParent(sa);
        AbilityUtils.resolve(action);
    }
}
