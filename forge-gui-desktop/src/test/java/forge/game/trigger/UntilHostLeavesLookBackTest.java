package forge.game.trigger;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * A card exiled "until this leaves the battlefield" returns immediately after the host leaves,
 * and the two are never on the battlefield together (Brutal Cathar ruling, 2021-09-24). A
 * leaves-the-battlefield trigger looks back to the game state just before the event (CR 603.10a),
 * when the returning card was still in exile, so it must not see the host leave.
 * Card-Forge/forge#9484.
 */
public class UntilHostLeavesLookBackTest extends AITest {

    /** Brutal Cathar for p1 exiling p2's `watcherName`; a second copy stays on the battlefield as the control. */
    private Card[] exileUnderCathar(Game game, Player p1, Player p2, String watcherName) {
        Card exiled = addCard(watcherName, p2);
        game.getAction().checkStateEffects(true);
        Card cathar = game.getAction().moveToPlay(addCardToZone("Brutal Cathar", p1, ZoneType.Hand), null, AbilityKey.newMap());
        playUntilStackClear(game);
        AssertJUnit.assertTrue("the " + watcherName + " is exiled under Brutal Cathar",
                game.getCardState(exiled).isInZone(ZoneType.Exile));
        Card watcher = addCard(watcherName, p2);
        game.getAction().checkStateEffects(true);
        return new Card[] { cathar, exiled, watcher };
    }

    /** Cast `spell` from `caster`'s hand at `target` (null for none) and resolve everything it queues. */
    private void cast(Game game, Player caster, String spell, Card target) {
        Card card = addCardToZone(spell, caster, ZoneType.Hand);
        SpellAbility sa = card.getSpells().get(0);
        game.getStack().freezeStack(sa);
        sa.setActivatingPlayer(caster);
        sa.setHostCard(game.getAction().moveToStack(card, sa));
        if (target != null) {
            sa.setTargetCard(target);
        }
        game.getStack().addAndUnfreeze(sa);
        do {
            game.getPhaseHandler().mainLoopStep();
        } while (!game.isGameOver() && (!game.getStack().isEmpty() || game.getStack().hasSimultaneousStackEntries()));
    }

    private void assertOnlyTheWatcherSawIt(Game game, Card exiled, Card watcher) {
        Card returned = game.getCardState(exiled);
        AssertJUnit.assertTrue("the exiled card came back", returned.isInPlay());
        AssertJUnit.assertEquals("same card, back on the battlefield", exiled.getId(), returned.getId());
        AssertJUnit.assertEquals("the copy on the battlefield saw Brutal Cathar die",
                1, game.getCardState(watcher).getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals("the returned copy was in exile when Brutal Cathar died",
                0, returned.getCounters(CounterEnumType.P1P1));
    }

    /**
     * The death reaches the trigger with a zone table but without the batch event that prunes
     * stale hosts (#9491), so the trigger has to look back for itself. Fails on master.
     */
    @Test
    public void aReturnedCardDoesNotSeeTheHostDie() {
        Game game = initAndCreateGame();
        Player p1 = game.getPlayers().get(0);
        Player p2 = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p1);
        Card[] c = exileUnderCathar(game, p1, p2, "Slaughter Specialist"); // "whenever a creature an opponent controls dies"

        game.copyLastState();
        Map<AbilityKey, Object> params = AbilityKey.newMap();
        AbilityKey.addCardZoneTableParams(params, new CardZoneTable(game.getLastStateBattlefield(), game.getLastStateGraveyard()));
        game.getAction().destroy(game.getCardState(c[0]), null, false, params);
        game.getAction().checkStateEffects(true);
        playUntilStackClear(game);

        assertOnlyTheWatcherSawIt(game, c[1], c[2]);
    }

    /** The common in-game route: a destroy spell, then state-based actions. Guards #9491 for both trigger modes. */
    @Test
    public void aReturnedCardDoesNotSeeTheHostDieToASpell() {
        for (String watcher : new String[] { "Slaughter Specialist", "Sengir Connoisseur" }) { // ChangesZone, ChangesZoneAll
            Game game = initAndCreateGame();
            Player p1 = game.getPlayers().get(0);
            Player p2 = game.getPlayers().get(1);
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p1);
            Card[] c = exileUnderCathar(game, p1, p2, watcher);

            cast(game, p2, "Murder", game.getCardState(c[0]));
            AssertJUnit.assertTrue("Murder killed Brutal Cathar", game.getCardState(c[0]).isInZone(ZoneType.Graveyard));

            assertOnlyTheWatcherSawIt(game, c[1], c[2]);
        }
    }

    /**
     * The check must not go the other way: a permanent that entered earlier in the SAME resolution
     * was on the battlefield before the death and still sees it. Zombie Apocalypse returns Midnight
     * Reaper, then destroys the Human; the Reaper takes its 1 damage.
     */
    @Test
    public void aCardThatEnteredEarlierInTheResolutionStillSeesTheDeath() {
        Game game = initAndCreateGame();
        Player p1 = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p1);
        fillLibrary(p1, 5);
        addCardToZone("Midnight Reaper", p1, ZoneType.Graveyard);
        addCard("Elite Vanguard", p1); // a nontoken Human
        game.getAction().checkStateEffects(true);

        cast(game, p1, "Zombie Apocalypse", null);

        AssertJUnit.assertEquals(1, countCardsWithName(game, "Midnight Reaper", ZoneType.Battlefield));
        AssertJUnit.assertEquals(0, countCardsWithName(game, "Elite Vanguard", ZoneType.Battlefield));
        AssertJUnit.assertEquals("Midnight Reaper saw the Human die", 19, p1.getLife());
    }
}
