package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

public class VoteEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.join(getDefinedPlayersOrTargeted(sa), ", "));
        sb.append(" vote ");
        if (sa.hasParam("VoteType")) {
            sb.append(StringUtils.join(sa.getParam("VoteType").split(","), " or "));
        } else if (sa.hasParam("VoteMessage")) {
            sb.append(sa.getParam("VoteMessage"));
        }
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(final SpellAbility sa) {
        final List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);
        final List<Object> voteType = new ArrayList<Object>();
        final Card host = sa.getHostCard();
        final Game game = host.getGame();

        if (sa.hasParam("VoteType")) {
            voteType.addAll(Arrays.asList(sa.getParam("VoteType").split(",")));
        } else if (sa.hasParam("VoteCard")) {
            ZoneType zone = sa.hasParam("Zone") ? ZoneType.smartValueOf(sa.getParam("Zone")) : ZoneType.Battlefield;
            voteType.addAll(CardLists.getValidCards(game.getCardsIn(zone), sa.getParam("VoteCard"), host.getController(), host));
        }
        if (voteType.isEmpty()) {
            return;
        }

        // starting with the activator
        int pSize = tgtPlayers.size();
        Player activator = sa.getActivatingPlayer();
        while (tgtPlayers.contains(activator) && !activator.equals(Iterables.getFirst(tgtPlayers, null))) {
            tgtPlayers.add(pSize - 1, tgtPlayers.remove(0));
        }
        ListMultimap<Object, Player> votes = ArrayListMultimap.create();

        for (final Player p : tgtPlayers) {
            int voteAmount = p.getKeywords().getAmount("You get an additional vote.") + 1;
            for (int i = 0; i < voteAmount; i++) {
                final Object result = p.getController().vote(sa, host + "Vote:", voteType, votes);
                votes.put(result, p);
                host.getGame().getAction().nofityOfValue(sa, p, result + "\r\nCurrent Votes:" + votes, p);
            }
        }
        
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("AllVotes", votes);
        game.getTriggerHandler().runTrigger(TriggerType.Vote, runParams, false);

        List<String> subAbs = Lists.newArrayList();
        final List<Object> mostVotes = getMostVotes(votes);
        if (sa.hasParam("Tied") && mostVotes.size() > 1) {
            subAbs.add(sa.getParam("Tied"));
        } else if (sa.hasParam("VoteSubAbility")) {
            for (final Object o : mostVotes) {
                host.addRemembered(o);
            }
            subAbs.add(sa.getParam("VoteSubAbility"));
        } else {
            for (Object type : mostVotes) {
                subAbs.add(sa.getParam("Vote" + type.toString()));
            }
        }
        
        for (final String subAb : subAbs) {
            final SpellAbility action = AbilityFactory.getAbility(host.getSVar(subAb), host);
            action.setActivatingPlayer(sa.getActivatingPlayer());
            ((AbilitySub) action).setParent(sa);
            AbilityUtils.resolve(action);
        }
        if (sa.hasParam("VoteSubAbility")) {
            host.clearRemembered();
        }
    }

    private static List<Object> getMostVotes(final ListMultimap<Object, Player> votes) {
        final List<Object> most = Lists.newArrayList();
        int amount = 0;
        for (final Object voteType : votes.keySet()) {
            final int voteAmount = votes.get(voteType).size();
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
