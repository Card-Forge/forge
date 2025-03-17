package forge.game.ability.effects;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import forge.game.event.GameEventRandomLog;
import forge.util.Lang;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

public class VoteEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        sb.append(Lang.joinHomogenous(getDefinedPlayersOrTargeted(sa))).append(" vote ");
        if (sa.hasParam("VoteType")) {
            sb.append("for ").append(StringUtils.join(sa.getParam("VoteType").split(","), " or "));
        } else if (sa.hasParam("VoteMessage")) {
            sb.append(sa.getParam("VoteMessage"));
        }
        sb.append(".");
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(final SpellAbility sa) {
        final List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);
        final List<Object> voteType = Lists.newArrayList();
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final Player activator = sa.getActivatingPlayer();

        final boolean secret = sa.hasParam("Secretly");
        final boolean other = sa.hasParam("VotePlayer") && sa.getParam("VotePlayer").equals("Other");
        final StringBuilder record = new StringBuilder();

        if (sa.hasParam("VoteType")) {
            voteType.addAll(Arrays.asList(sa.getParam("VoteType").split(",")));
        } else if (sa.hasParam("VoteCard")) {
            ZoneType zone = sa.hasParam("Zone") ? ZoneType.smartValueOf(sa.getParam("Zone")) : ZoneType.Battlefield;
            voteType.addAll(CardLists.getValidCards(game.getCardsIn(zone), sa.getParam("VoteCard"), activator, host, sa));
        } else if (sa.hasParam("VotePlayer")) {
            String param = other ? "Player" : sa.getParam("VotePlayer");
            voteType.addAll(AbilityUtils.getDefinedPlayers(host, param, sa));
        }
        if (voteType.isEmpty()) {
            return;
        }

        // starting with the activator
        int aidx = tgtPlayers.indexOf(activator);
        if (aidx != -1) {
            Collections.rotate(tgtPlayers, -aidx);
        }

        ListMultimap<Object, Player> votes = ArrayListMultimap.create();
        Player voter = game.getControlVote();

        for (final Player p : tgtPlayers) {
            if (!p.isInGame()) {
                continue;
            }
            final List<Object> voteOpts = Lists.newArrayList(voteType);
            int voteAmount = p.getAdditionalVotesAmount() + 1;
            int optionalVotes = p.getAdditionalOptionalVotesAmount();
            Player realVoter = voter == null ? p : voter;

            if (other) {
                voteOpts.remove(realVoter);
                if (voteOpts.isEmpty()) continue;
            }

            Map<String, Object> params = Maps.newHashMap();
            params.put("Voter", realVoter);
            voteAmount += p.getController().chooseNumber(sa, Localizer.getInstance().getMessage("lblHowManyAdditionalVotesDoYouWant"), 0, optionalVotes, params);

            for (int i = 0; i < voteAmount; i++) {
                Object result = realVoter.getController().vote(sa, host + " " + Localizer.getInstance().getMessage("lblVote") + ":", voteOpts, votes, p, sa.hasParam("UpTo"));

                if (result != null) {
                    votes.put(result, p);
                    if (!secret) {
                        game.getAction().notifyOfValue(sa, p, result + "\r\n" +
                                Localizer.getInstance().getMessage("lblCurrentVote") + ":" + votes, p);
                    }
                    if (record.length() > 0) {
                        record.append("\r\n");
                    }
                    record.append(p).append(" ").append(Localizer.getInstance().getMessage("lblVotedFor", result));
                }
            }
        }

        final String voteResult = record.toString();
        if (secret) {
            game.getAction().notifyOfValue(sa, host, voteResult, null);
        }
        game.fireEvent(new GameEventRandomLog(voteResult));

        final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
        runParams.put(AbilityKey.AllVotes, votes);
        game.getTriggerHandler().runTrigger(TriggerType.Vote, runParams, false);

        if (sa.hasParam("EachVote")) {
            for (Map.Entry<Object, Collection<Player>> e : votes.asMap().entrySet()) {
                final SpellAbility action = AbilityFactory.getAbility(host, sa.getParam("Vote" + e.getKey().toString()));

                action.setActivatingPlayer(sa.getActivatingPlayer());
                ((AbilitySub) action).setParent(sa);

                for (Player p : e.getValue()) {
                    host.addRemembered(p);
                    AbilityUtils.resolve(action);
                    host.removeRemembered(p);
                }
            }
        } else {
            List<String> subAbs = Lists.newArrayList();
            final List<Object> mostVotes = getMostVotes(votes);
            if (sa.hasParam("Tied") && mostVotes.size() > 1) {
                subAbs.add(sa.getParam("Tied"));
            } else if (sa.hasParam("VoteSubAbility")) {
                host.addRemembered(mostVotes);
                subAbs.add(sa.getParam("VoteSubAbility"));
            } else {
                for (Object type : mostVotes) {
                    subAbs.add(sa.getParam("Vote" + type.toString()));
                }
            }
            if (sa.hasParam("StoreVoteNum")) {
                for (final Object type : voteType) {
                    sa.setSVar("VoteNum" + type, "Number$" + votes.get(type).size());
                }
            } else {
                for (final String subAb : subAbs) {
                    final SpellAbility action = AbilityFactory.getAbility(host, subAb);
                    action.setActivatingPlayer(sa.getActivatingPlayer());
                    ((AbilitySub) action).setParent(sa);
                    AbilityUtils.resolve(action);
                }
            }
            if (sa.hasParam("VoteSubAbility")) {
                host.clearRemembered();
            }
            if (sa.hasParam("RememberVotedObjects")) {
                host.addRemembered(votes.keySet());
            }
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
