package forge.ai.simulation;

import forge.game.player.Player;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.TreeSet;

/**
 * This test ensures that when new fields are added to Player (or other snapshot-relevant classes),
 * developers are reminded to update GameSnapshot accordingly.
 *
 * If this test fails, a new field was added. You must either:
 * 1. Add it to GameSnapshot.assignPlayerState() and add it to SNAPSHOT_HANDLED_FIELDS below, or
 * 2. Add it to SNAPSHOT_EXCLUDED_FIELDS below with a comment explaining why it's excluded.
 */
public class GameSnapshotCoverageTest {

    // Fields that ARE copied by GameSnapshot.assignPlayerState() or related methods
    private static final Set<String> SNAPSHOT_HANDLED_FIELDS = Set.of(
        // Life tracking
        "life",
        "lifeStartedThisTurnWith",
        "lifeLostThisTurn",
        "lifeLostLastTurn",
        "lifeGainedThisTurn",
        "lifeGainedTimesThisTurn",
        "lifeGainedByTeamThisTurn",

        // Hand/draw tracking
        "maxHandSize",
        "unlimitedHandSize",
        "numDrawnThisTurn",
        "numDrawnLastTurn",
        "numDrawnThisDrawStep",
        "numCardsInHandStartedThisTurnWith",
        "lastDrawnCard",

        // Land tracking
        "landsPlayedThisTurn",
        "landsPlayedLastTurn",
        "numPowerSurgeLands",
        "tappedLandForManaThisTurn",

        // Spell/ability tracking
        "spellsCastThisTurn",
        "spellsCastLastTurn",
        "spellsCastThisGame",
        "spellsCastSinceBeginningOfLastTurn",
        "investigatedThisTurn",
        "surveilThisTurn",
        "committedCrimeThisTurn",
        "expentThisTurn",
        "numLibrarySearchedOwn",

        // Per-turn counters
        "numExploredThisTurn",
        "numTokenCreatedThisTurn",
        "numForetoldThisTurn",
        "numRollsThisTurn",
        "diceRollsThisTurn",
        "numFlipsThisTurn",
        "venturedThisTurn",
        "attractionsVisitedThisTurn",
        "numRingTemptedYou",
        "descended",
        "speed",
        "crankCounter",
        "devotionMod",
        "elementalBendThisTurn",

        // Combat tracking
        "attackedThisTurn",
        "attackedPlayersLastTurn",
        "attackedPlayersThisCombat",
        "beenDealtCombatDamageSinceLastTurn",
        "simultaneousDamage",

        // Per-turn lists
        "discardedThisTurn",
        "sacrificedThisTurn",
        "completedDungeons",
        "lostOwnership",
        "gainedOwnership",

        // Misc game state
        "namedCard",
        "numManaShards",
        "lastTurnNr",
        "triedToDrawFromEmptyLibrary",
        "manaPool",             // copied via copyManaPool()
        "counters",             // copied via setCounters()

        // Planar/scheme
        "currentPlanes",
        "planeswalkedToThisTurn",
        "activeScheme"
    );

    // Fields intentionally NOT copied - with reasons:
    private static final Set<String> SNAPSHOT_EXCLUDED_FIELDS = Set.of(
        // Immutable / set at game start - never change during play
        "startingLife",
        "startingHandSize",
        "game",
        "view",
        "zones",                // structural - cards within zones are handled by copyGameState
        "extraZones",
        "controller",           // special-cased in restore path via dangerouslySetController
        "stats",                // achievement/statistics tracking, not gameplay state
        "achievementTracker",
        "teamNumber",

        // Handled by copyCommandersToSnapshot() separately
        "commanders",
        "commanderCast",
        "commanderDamage",
        "commanderEffect",

        // Effect cards - lazily created DetachedCardEffects, tied to player identity
        // These are created on demand and live in the Command zone, handled by zone copying
        "monarchEffect",
        "initiativeEffect",
        "blessingEffect",
        "contraptionSprocketEffect",
        "radiationEffect",
        "keywordEffect",
        "speedEffect",

        // Keyword system - rebuilt by checkStateEffects/resetActiveTriggers after restore
        "keywords",
        "storedKeywords",
        "changedKeywords",
        "inboundTokens",        // transient ETB replacement tracking, empty between actions

        // Timestamp-keyed maps managed by static abilities - rebuilt by layer system
        "adjustLandPlays",
        "adjustLandPlaysInfinite",
        "controlledBy",
        "controlledWhileSearching",
        "additionalVotes",
        "additionalOptionalVotes",
        "controlVotes",
        "additionalVillainousChoices",
        "declaresAttackers",
        "declaresBlockers",

        // Payment state - only relevant during active cost payment, empty between actions
        "paidForStack",

        // Subgame-specific - not relevant for normal undo
        "maingameCardsMap",

        // Ring cards - card references resolved through zone system
        "ringBearer",
        "theRing",

        // Notes
        "notes",
        "notedNum",
        "draftNotes"
    );

    @Test
    public void testAllPlayerFieldsAccountedFor() {
        Set<String> unaccounted = new TreeSet<>();

        for (Field f : Player.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            if (Modifier.isFinal(f.getModifiers()) && !isCollectionType(f)) continue; // final primitives/immutables are safe

            String name = f.getName();
            if (!SNAPSHOT_HANDLED_FIELDS.contains(name) && !SNAPSHOT_EXCLUDED_FIELDS.contains(name)) {
                unaccounted.add(name);
            }
        }

        if (!unaccounted.isEmpty()) {
            AssertJUnit.fail(
                "New Player field(s) not accounted for in GameSnapshot: " + unaccounted + "\n" +
                "Either add them to GameSnapshot.assignPlayerState() (and SNAPSHOT_HANDLED_FIELDS in this test),\n" +
                "or add them to SNAPSHOT_EXCLUDED_FIELDS with a comment explaining why."
            );
        }
    }

    private static boolean isCollectionType(Field f) {
        Class<?> type = f.getType();
        return java.util.Collection.class.isAssignableFrom(type)
            || java.util.Map.class.isAssignableFrom(type)
            || com.google.common.collect.Table.class.isAssignableFrom(type);
    }
}
