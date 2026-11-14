package forge.ai.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * A creature is evaluated as it is now, but its copy can arrive smaller. A characteristic-defining
 * body reads the new controller's board, and a copy gets no counters and has X as 0.
 */
public class CopyCreatureAiTest extends AITest {

    private Card upkeep(Game game, Player ai, Player opp) {
        Card crypto = addCard("Cryptoplasm", ai);
        crypto.setSickness(false);
        addCards("Island", 5, ai);
        for (Player p : game.getPlayers()) {
            fillLibrary(p, 20);
        }
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        game.getAction().checkStateEffects(true);
        return crypto;
    }

    private void run(Game game) {
        playUntilPhase(game, PhaseType.MAIN1);
        playUntilStackClear(game);
    }

    @Test
    public void doesNotCopyABodyThatOnlyExistsOnTheirBoard() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        // 8/8 for them, 0/0 for us: copying it is suicide
        addCard("Nightmare", opp);
        addCards("Swamp", 8, opp);
        Card crypto = upkeep(game, ai, opp);

        run(game);

        assertEquals("still ours and alive", ZoneType.Battlefield, crypto.getZone().getZoneType());
        assertEquals(0, countCardsWithName(game, "Nightmare", ZoneType.Graveyard));
    }

    @Test
    public void takesTheNextBestWhenTheTopPickIsATrap() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        addCard("Nightmare", opp);
        addCards("Swamp", 8, opp);
        addCard("Serra Angel", opp);
        Card crypto = upkeep(game, ai, opp);

        run(game);

        assertEquals("Serra Angel", crypto.getName());
        assertEquals(4, crypto.getNetPower());
        assertEquals(ZoneType.Battlefield, crypto.getZone().getZoneType());
    }

    @Test
    public void ordinaryBoardStillCopiesTheBest() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        addCard("Serra Angel", opp);
        addCard("Grizzly Bears", opp);
        Card crypto = upkeep(game, ai, opp);

        run(game);

        assertEquals("Serra Angel", crypto.getName());
    }

    private SpellAbility saheeli(Game game, Player ai) {
        Card saheeli = addCard("Saheeli, the Sun's Brilliance", ai);
        saheeli.setSickness(false);
        addCard("Island", ai);
        addCard("Mountain", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        SpellAbility sa = saheeli.getSpellAbilities().stream()
                .filter(s -> s.getApi() == ApiType.CopyPermanent).findFirst().orElseThrow();
        sa.setActivatingPlayer(ai);
        return sa;
    }

    @Test
    public void doesNotCopyACreatureMadeOfXCounters() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        Card devastator = addCard("Shivan Devastator", ai);
        SpellAbility cast = devastator.getFirstSpellAbility();
        cast.setXManaCostPaid(3);
        devastator.setCastSA(cast);
        devastator.setCounters(CounterEnumType.P1P1, 3);
        addCard("Grizzly Bears", ai);
        SpellAbility sa = saheeli(game, ai);

        assertTrue(SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay());
        assertEquals("Grizzly Bears", sa.getTargets().getFirstTargetedCard().getName());
    }

    @Test
    public void stillCopiesACreatureThatAnotherPermanentGivesCounters() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        Card devastator = addCard("Shivan Devastator", ai);
        devastator.setCounters(CounterEnumType.P1P1, 3);
        // each Dragon we control enters with an additional +1/+1 counter
        addCard("Dragonstorm Globe", ai);
        SpellAbility sa = saheeli(game, ai);

        assertTrue(SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay());
        assertEquals("Shivan Devastator", sa.getTargets().getFirstTargetedCard().getName());
    }

    @Test
    public void stillCopiesACreatureWhoseEntryGivesCounters() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        // Modular 1 puts a counter on the copy as well
        addCard("Arcbound Worker", ai).setCounters(CounterEnumType.P1P1, 1);
        SpellAbility sa = saheeli(game, ai);

        assertTrue(SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay());
        assertEquals("Arcbound Worker", sa.getTargets().getFirstTargetedCard().getName());
    }
}
