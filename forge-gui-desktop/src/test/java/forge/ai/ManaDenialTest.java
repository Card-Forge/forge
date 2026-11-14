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

/** Rishadan Port is used on the opponent's upkeep against the land that hurts most to lose. */
public class ManaDenialTest extends FlatMcTestBase {

    @BeforeMethod
    public void setUp() {
        setUpGame();
    }

    private SpellAbility portAbility() {
        final Card port = put("Rishadan Port", ai, ZoneType.Battlefield);
        put("Plains", 2, ai, ZoneType.Battlefield);
        for (final SpellAbility sa : port.getNonManaAbilities()) {
            if (sa.getApi() == ApiType.Tap) { // not the land-play ability, which is a non-mana ability too
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new IllegalStateException("no Port ability");
    }

    @Test
    public void anAllBasicBoardTargetsTheLeastRepresentedColour() {
        final SpellAbility port = portAbility();
        put("Island", 3, opp, ZoneType.Battlefield);
        final Card mountain = put("Mountain", opp, ZoneType.Battlefield);
        at(PhaseType.UPKEEP, opp);
        AssertJUnit.assertSame("why: " + ManaDenial.why(ai, port), mountain, ManaDenial.choose(ai, port));
    }

    @Test
    public void aDualLandIsPreferredToABasicOfThePlentifulColour() {
        final SpellAbility port = portAbility();
        put("Island", 3, opp, ZoneType.Battlefield);
        put("Swamp", opp, ZoneType.Battlefield);
        final Card sea = put("Underground Sea", opp, ZoneType.Battlefield);
        at(PhaseType.UPKEEP, opp);
        AssertJUnit.assertSame("why: " + ManaDenial.why(ai, port), sea, ManaDenial.choose(ai, port));
    }

    @Test
    public void aTappedLandIsNeverTheTarget() {
        final SpellAbility port = portAbility();
        put("Island", 3, opp, ZoneType.Battlefield);
        final Card mountain = put("Mountain", opp, ZoneType.Battlefield);
        mountain.setTapped(true);
        at(PhaseType.UPKEEP, opp);
        AssertJUnit.assertNotNull("why: " + ManaDenial.why(ai, port), ManaDenial.choose(ai, port));
        AssertJUnit.assertNotSame(mountain, ManaDenial.choose(ai, port));
    }

    @Test
    public void nothingOnTheAisOwnTurn() {
        final SpellAbility port = portAbility();
        put("Island", 3, opp, ZoneType.Battlefield);
        at(PhaseType.UPKEEP, ai);
        AssertJUnit.assertNull(ManaDenial.choose(ai, port));
    }
}
