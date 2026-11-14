package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import forge.ai.simulation.FlatMcTestBase;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The AI's use of Rishadan Port and similar land-tapping abilities ({@link ManaDenial}): it is used on the opponent's
 * upkeep against the land that hurts most to lose, only when the AI's own mana is safe to spend, and only when the
 * opponent's mana is worth cutting.
 */
public class ManaDenialTest extends FlatMcTestBase {

    @BeforeMethod
    public void setUp() {
        setUpGame();
    }

    /** Puts a Rishadan Port in play for the AI and returns its "tap target land" ability. */
    private SpellAbility port() {
        final Card port = put("Rishadan Port", ai, ZoneType.Battlefield);
        for (final SpellAbility sa : port.getNonManaAbilities()) {
            if (sa.getApi() == ApiType.Tap) { // not the land-play ability, which is a non-mana ability too
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new IllegalStateException("no Port ability");
    }

    /** The Port plus two Plains: enough of the AI's own mana that nothing it holds needs the Port's cost. */
    private SpellAbility portWithTwoPlains() {
        final SpellAbility port = port();
        put("Plains", 2, ai, ZoneType.Battlefield);
        return port;
    }

    /** Three Islands and a lone Mountain: the Mountain is the land the Port should take. */
    private Card opposingBoard() {
        put("Island", 3, opp, ZoneType.Battlefield);
        return put("Mountain", opp, ZoneType.Battlefield);
    }

    private AiAbilityDecision decide(final SpellAbility sa) {
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa);
    }

    private void assertPicks(final Card expected, final SpellAbility port) {
        AssertJUnit.assertSame("why: " + ManaDenial.why(ai, port), expected, ManaDenial.choose(ai, port));
    }

    private void assertPicksNothing(final SpellAbility port) {
        AssertJUnit.assertNull("why: " + ManaDenial.why(ai, port), ManaDenial.choose(ai, port));
    }

    @Test
    public void anAllBasicBoardTargetsTheLeastRepresentedColour() {
        final SpellAbility port = portWithTwoPlains();
        final Card mountain = opposingBoard();
        at(PhaseType.UPKEEP, opp);
        assertPicks(mountain, port);
    }

    @Test
    public void aDualLandIsPreferredToABasicOfThePlentifulColour() {
        final SpellAbility port = portWithTwoPlains();
        put("Island", 3, opp, ZoneType.Battlefield);
        put("Swamp", opp, ZoneType.Battlefield);
        final Card sea = put("Underground Sea", opp, ZoneType.Battlefield);
        at(PhaseType.UPKEEP, opp);
        assertPicks(sea, port);
    }

    @Test
    public void aTappedLandIsNeverTheTarget() {
        final SpellAbility port = portWithTwoPlains();
        final Card mountain = opposingBoard();
        mountain.setTapped(true);
        at(PhaseType.UPKEEP, opp);
        final Card picked = ManaDenial.choose(ai, port);
        AssertJUnit.assertNotNull("why: " + ManaDenial.why(ai, port), picked);
        AssertJUnit.assertNotSame(mountain, picked);
    }

    @Test
    public void nothingOnTheAisOwnTurn() {
        final SpellAbility port = portWithTwoPlains();
        put("Island", 3, opp, ZoneType.Battlefield);
        at(PhaseType.UPKEEP, ai);
        AssertJUnit.assertNull(ManaDenial.choose(ai, port));
    }

    @Test
    public void theAiDoesNotPortOnItsOwnTurn() {
        final SpellAbility port = portWithTwoPlains();
        put("Island", 3, opp, ZoneType.Battlefield);
        at(PhaseType.MAIN1, ai);
        AssertJUnit.assertFalse(decide(port).willingToPlay());
    }

    @Test
    public void aHeldCounterspellKeepsItsMana() {
        final SpellAbility port = port();
        put("Island", 2, ai, ZoneType.Battlefield);
        put("Counterspell", ai, ZoneType.Hand);
        opposingBoard();
        at(PhaseType.UPKEEP, opp);
        assertPicksNothing(port);
    }

    @Test
    public void withManaToSpareTheCounterspellDoesNotStopThePort() {
        final SpellAbility port = port();
        put("Island", 3, ai, ZoneType.Battlefield);
        put("Counterspell", ai, ZoneType.Hand);
        final Card mountain = opposingBoard();
        at(PhaseType.UPKEEP, opp);
        assertPicks(mountain, port);
    }

    @Test
    public void aRegenerationShieldKeepsItsMana() {
        final SpellAbility port = port();
        put("Swamp", ai, ZoneType.Battlefield);
        put("Drudge Skeletons", ai, ZoneType.Battlefield); // {B}: Regenerate
        opposingBoard();
        at(PhaseType.UPKEEP, opp);
        assertPicksNothing(port);
    }

    @Test
    public void withManaToSpareTheRegenerationDoesNotStopThePort() {
        final SpellAbility port = port();
        put("Swamp", 2, ai, ZoneType.Battlefield);
        put("Drudge Skeletons", ai, ZoneType.Battlefield);
        final Card mountain = opposingBoard();
        at(PhaseType.UPKEEP, opp);
        assertPicks(mountain, port);
    }

    @Test
    public void aCreatureInHandDoesNotHoldItBackBecauseTheLandsUntap() {
        final SpellAbility port = port();
        put("Forest", 2, ai, ZoneType.Battlefield);
        put("Grizzly Bears", ai, ZoneType.Hand);
        final Card mountain = opposingBoard();
        at(PhaseType.UPKEEP, opp);
        assertPicks(mountain, port);
    }

    @Test
    public void anOrdinaryColourIsNotPortedWhenTheOpponentHasManyLands() {
        final SpellAbility port = portWithTwoPlains();
        put("Island", 5, opp, ZoneType.Battlefield);
        put("Swamp", 2, opp, ZoneType.Battlefield);
        at(PhaseType.UPKEEP, opp);
        assertPicksNothing(port);
    }

    @Test
    public void aLoneColourIsPortedEvenAgainstManyLands() {
        final SpellAbility port = portWithTwoPlains();
        put("Island", 6, opp, ZoneType.Battlefield);
        final Card mountain = put("Mountain", opp, ZoneType.Battlefield);
        at(PhaseType.UPKEEP, opp);
        assertPicks(mountain, port);
    }

    @Test
    public void thePortIsNotKeptOutOfTheAisPlayableCards() {
        final Card port = put("Rishadan Port", ai, ZoneType.Battlefield);
        AssertJUnit.assertFalse(ComputerUtilCard.isCardRemAIDeck(port));
    }

    @Test
    public void theAiPortsTheLeastRepresentedColourOnTheOpponentsUpkeep() {
        final SpellAbility port = portWithTwoPlains();
        final Card mountain = opposingBoard();
        at(PhaseType.UPKEEP, opp);
        final AiAbilityDecision decision = decide(port);
        AssertJUnit.assertTrue("decision: " + decision + ", why: " + ManaDenial.why(ai, port), decision.willingToPlay());
        AssertJUnit.assertSame(mountain, port.getTargetCard());
    }
}