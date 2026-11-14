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

/** The Port is only used when the AI's own mana is safe to spend, and when the opponent's mana is worth cutting. */
public class ManaDenialSafetyTest extends FlatMcTestBase {

    @BeforeMethod
    public void setUp() {
        setUpGame();
    }

    private SpellAbility port() {
        final Card port = put("Rishadan Port", ai, ZoneType.Battlefield);
        for (final SpellAbility sa : port.getNonManaAbilities()) {
            if (sa.getApi() == ApiType.Tap) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new IllegalStateException("no Port ability");
    }

    /**
     * Three Islands and a lone Mountain: the Mountain is the land the Port would take.
     */
    private Card opposingBoard() {
        put("Island", 3, opp, ZoneType.Battlefield);
        return put("Mountain", opp, ZoneType.Battlefield);
    }

    @Test
    public void aHeldCounterspellKeepsItsMana() {
        final SpellAbility port = port();
        put("Island", 2, ai, ZoneType.Battlefield);
        put("Counterspell", ai, ZoneType.Hand);
        opposingBoard();
        at(PhaseType.UPKEEP, opp);
        AssertJUnit.assertNull("why: " + ManaDenial.why(ai, port), ManaDenial.choose(ai, port));
    }

    @Test
    public void withManaToSpareTheCounterspellDoesNotStopThePort() {
        final SpellAbility port = port();
        put("Island", 3, ai, ZoneType.Battlefield);
        put("Counterspell", ai, ZoneType.Hand);
        final Card mountain = opposingBoard();
        at(PhaseType.UPKEEP, opp);
        AssertJUnit.assertSame("why: " + ManaDenial.why(ai, port), mountain, ManaDenial.choose(ai, port));
    }

    @Test
    public void aRegenerationShieldKeepsItsMana() {
        final SpellAbility port = port();
        put("Swamp", ai, ZoneType.Battlefield);
        put("Drudge Skeletons", ai, ZoneType.Battlefield); // {B}: Regenerate
        opposingBoard();
        at(PhaseType.UPKEEP, opp);
        AssertJUnit.assertNull("why: " + ManaDenial.why(ai, port), ManaDenial.choose(ai, port));
    }

    @Test
    public void withManaToSpareTheRegenerationDoesNotStopThePort() {
        final SpellAbility port = port();
        put("Swamp", 2, ai, ZoneType.Battlefield);
        put("Drudge Skeletons", ai, ZoneType.Battlefield);
        final Card mountain = opposingBoard();
        at(PhaseType.UPKEEP, opp);
        AssertJUnit.assertSame("why: " + ManaDenial.why(ai, port), mountain, ManaDenial.choose(ai, port));
    }

    @Test
    public void aCreatureInHandDoesNotHoldItBackBecauseTheLandsUntap() {
        final SpellAbility port = port();
        put("Forest", 2, ai, ZoneType.Battlefield);
        put("Grizzly Bears", ai, ZoneType.Hand);
        final Card mountain = opposingBoard();
        at(PhaseType.UPKEEP, opp);
        AssertJUnit.assertSame("why: " + ManaDenial.why(ai, port), mountain, ManaDenial.choose(ai, port));
    }

    @Test
    public void anOrdinaryColourIsNotPortedWhenTheOpponentHasManyLands() {
        final SpellAbility port = port();
        put("Plains", 2, ai, ZoneType.Battlefield);
        put("Island", 5, opp, ZoneType.Battlefield);
        put("Swamp", 2, opp, ZoneType.Battlefield);
        at(PhaseType.UPKEEP, opp);
        AssertJUnit.assertNull("why: " + ManaDenial.why(ai, port), ManaDenial.choose(ai, port));
    }

    @Test
    public void aLoneColourIsPortedEvenAgainstManyLands() {
        final SpellAbility port = port();
        put("Plains", 2, ai, ZoneType.Battlefield);
        put("Island", 6, opp, ZoneType.Battlefield);
        final Card mountain = put("Mountain", opp, ZoneType.Battlefield);
        at(PhaseType.UPKEEP, opp);
        AssertJUnit.assertSame("why: " + ManaDenial.why(ai, port), mountain, ManaDenial.choose(ai, port));
    }
}
