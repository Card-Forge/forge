/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.trigger;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.collect.FCollection;

/**
 * <p>
 * Trigger_Untaps class.
 * </p>
 * 
 * @author Forge
 * @version $Id: TriggerUntaps.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public class TriggerVote extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_Untaps.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerVote(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final Map<String, Object> runParams2) {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        @SuppressWarnings("unchecked")
        final ListMultimap<Object, Player> votes = (ArrayListMultimap<Object, Player>) this.getRunParams().get("AllVotes");
        sa.setTriggeringObject("OtherVoters", getVoters(this.getHostCard().getController(), votes, true, true));
    }

    private static FCollection<Player> getVoters(final Player player,
            final ListMultimap<Object, Player> votes,
            final boolean isOpponent, final boolean votedOtherchoice) {
        final FCollection<Player> voters = new FCollection<Player>();
        for (final Object voteType : votes.keySet()) {
            final List<Player> players = votes.get(voteType);
            if (votedOtherchoice ^ players.contains(player)) {
                voters.addAll(players);
            }
        }
        if (isOpponent) {
            voters.retainAll(player.getOpponents());
        }
        return voters;
    }

}
