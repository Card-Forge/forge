package forge.game.ability;

import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;

import forge.ai.AITest;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/** "Each player votes for another player" (Mob Verdict) while one player controls how everybody votes (Illusion of Choice). */
public class VoteOtherTest extends AITest {

    /** Records the options of every vote it is asked to cast, by the player the vote belongs to. */
    private static class RecordingController extends PlayerControllerAi {
        final Map<Player, List<Object>> optionsByVoter = Maps.newHashMap();

        RecordingController(Game game, Player p) {
            super(game, p, p.getLobbyPlayer());
        }

        @Override
        public Object vote(SpellAbility sa, String prompt, List<Object> options, ListMultimap<Object, Player> votes,
                Player forPlayer, boolean optional) {
            optionsByVoter.put(forPlayer, options);
            return options.get(0);
        }
    }

    /** A vote belongs to the player voting, so that player is the one who cannot be voted for. */
    @Test
    public void aControlledVoterStillCannotVoteForThemselves() {
        Game game = initAndCreateGame();
        Player controller = game.getPlayers().get(1);
        Player other = game.getPlayers().get(0);
        Card host = addCard("Grizzly Bears", controller);
        RecordingController recorder = new RecordingController(game, controller);
        controller.dangerouslySetController(recorder);
        controller.addControlVote(game.getNextTimestamp());

        SpellAbility sa = AbilityFactory.getAbility("DB$ Vote | Defined$ Player | VotePlayer$ Other", host);
        sa.setActivatingPlayer(controller);
        AbilityUtils.resolve(sa);

        List<Object> options = recorder.optionsByVoter.get(other);
        assertNotNull("the controller votes for the other player", options);
        assertFalse("the other player's vote cannot name the other player", options.contains(other));
        assertTrue("it can name the controller", options.contains(controller));
    }
}
