package forge.game.ability.effects;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class VoteEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        return StringUtils.join(getDefinedPlayersOrTargeted(sa), ", ") + " vote "
                + StringUtils.join(sa.getParam("VoteType").split(","), " or ");
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);
        final List<String> voteType = Arrays.asList(sa.getParam("VoteType").split(","));
        final Card host = sa.getHostCard();
        // starting with the activator
        int pSize = tgtPlayers.size();
        Player activator = sa.getActivatingPlayer();
        while (tgtPlayers.contains(activator) && !activator.equals(Iterables.getFirst(tgtPlayers, null))) {
            tgtPlayers.add(pSize - 1, tgtPlayers.remove(0));
        }
        ArrayListMultimap<String, Player> votes = ArrayListMultimap.create();

        for (final Player p : tgtPlayers) {
            int voteAmount = p.getAmountOfKeyword("You get an additional vote.") + 1;
            for (int i = 0; i < voteAmount; i++) {
                final String result = p.getController().vote(sa, sa.getHostCard() + "Vote:", voteType);
                votes.put(result, p);
                host.getGame().getAction().nofityOfValue(sa, p, result + "\r\nCurrent Votes:" + votes, p);
            }
        }
        

        List<String> subAbs = Lists.newArrayList();
        final List<String> mostVotes = getMostVotes(votes);
        if (sa.hasParam("Tied") && mostVotes.size() > 1) {
            subAbs.add(sa.getParam("Tied"));
        } else {
            for (String type : mostVotes) {
                subAbs.add(sa.getParam("Vote" + type));
            }
        }
        
        for (final String subAb : subAbs) {
            final SpellAbility action = AbilityFactory.getAbility(host.getSVar(subAb), host);
            action.setActivatingPlayer(sa.getActivatingPlayer());
            ((AbilitySub) action).setParent(sa);
            AbilityUtils.resolve(action);
        }
    }

    private List<String> getMostVotes(ArrayListMultimap<String, Player> votes) {
        List<String> most = Lists.newArrayList();
        int amount = 0;
        for (String voteType : votes.keySet()) {
            int voteAmount = votes.get(voteType).size();
            if (voteAmount == amount) {
                most.add(voteType);
            } else if (voteAmount > amount) {
                amount = voteAmount;
                most.clear();
                most.add(voteType);
            }
        }
        return most;
    }
}
