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

/** The AI's own logic for Rishadan Port (TapAi): the script lets it play the card, and the land is the one that hurts. */
public class ManaDenialAiTest extends FlatMcTestBase {

    @BeforeMethod
    public void setUp() {
        setUpGame();
    }

    private SpellAbility tapAbility(final Card c) {
        for (final SpellAbility sa : c.getNonManaAbilities()) {
            if (sa.getApi() == ApiType.Tap) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new IllegalStateException("no tap ability on " + c);
    }

    @Test
    public void thePortIsNotKeptOutOfTheAisPlayableCards() {
        final Card port = put("Rishadan Port", ai, ZoneType.Battlefield);
        AssertJUnit.assertFalse(ComputerUtilCard.isCardRemAIDeck(port));
    }

    @Test
    public void theAiPortsTheLeastRepresentedColourOnTheOpponentsUpkeep() {
        final SpellAbility port = tapAbility(put("Rishadan Port", ai, ZoneType.Battlefield));
        put("Plains", 2, ai, ZoneType.Battlefield);
        put("Island", 3, opp, ZoneType.Battlefield);
        final Card mountain = put("Mountain", opp, ZoneType.Battlefield);
        at(PhaseType.UPKEEP, opp);
        final AiAbilityDecision decision = SpellApiToAi.Converter.get(port).canPlayWithSubs(ai, port);
        AssertJUnit.assertTrue("decision: " + decision + ", why: " + ManaDenial.why(ai, port), decision.willingToPlay());
        AssertJUnit.assertSame(mountain, port.getTargetCard());
    }

    @Test
    public void theAiDoesNotPortOnItsOwnTurn() {
        final SpellAbility port = tapAbility(put("Rishadan Port", ai, ZoneType.Battlefield));
        put("Plains", 2, ai, ZoneType.Battlefield);
        put("Island", 3, opp, ZoneType.Battlefield);
        at(PhaseType.MAIN1, ai);
        AssertJUnit.assertFalse(SpellApiToAi.Converter.get(port).canPlayWithSubs(ai, port).willingToPlay());
    }
}
