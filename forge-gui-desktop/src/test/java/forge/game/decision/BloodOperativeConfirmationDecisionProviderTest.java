package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class BloodOperativeConfirmationDecisionProviderTest extends AITest {
    private final ConfirmationDecisionProvider provider = new ConfirmationDecisionProvider();

    @Test
    public void exactBloodEtbConfirmationIsNotYetAdmittedOnTheBaseline() {
        final Game game = initAndCreateGame();
        final Player decider = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final Card source = addCardToZone("Blood Operative", decider, ZoneType.Battlefield);
        final Card legalCard = addCardToZone("Runeclaw Bear", opponent, ZoneType.Graveyard);
        assertEquals(source.getName(), "Blood Operative");
        assertTrue(decider.getZone(ZoneType.Battlefield).contains(source));
        assertEquals(source.getController(), decider);
        assertEquals(opponent.getZone(ZoneType.Graveyard).size(), 1);
        assertTrue(opponent.getZone(ZoneType.Graveyard).contains(legalCard));

        final Trigger trigger = source.getTriggers().stream()
                .filter(candidate -> candidate.getMode() == TriggerType.ChangesZone)
                .filter(candidate -> "Any".equals(candidate.getParam("Origin")))
                .filter(candidate -> "Battlefield".equals(candidate.getParam("Destination")))
                .filter(candidate -> "Card.Self".equals(candidate.getParam("ValidCard")))
                .filter(candidate -> "You".equals(candidate.getParam("OptionalDecider")))
                .filter(candidate -> "TrigChangeZone".equals(candidate.getParam("Execute")))
                .filter(candidate -> candidate.isIntrinsic())
                .filter(candidate -> !candidate.isStatic())
                .filter(candidate -> candidate.getSpawningAbility() == null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected Blood Operative ETB trigger is unavailable"));
        assertEquals(trigger.getMode(), TriggerType.ChangesZone);
        assertFalse(trigger.isStatic());
        assertTrue(trigger.isIntrinsic());
        assertNull(trigger.getSpawningAbility());
        assertEquals(trigger.getParam("Origin"), "Any");
        assertEquals(trigger.getParam("Destination"), "Battlefield");
        assertEquals(trigger.getParam("ValidCard"), "Card.Self");
        assertEquals(trigger.getParam("OptionalDecider"), "You");
        assertEquals(trigger.getParam("Execute"), "TrigChangeZone");

        final SpellAbility ability = trigger.ensureAbility();
        assertNotNull(ability);
        ability.setActivatingPlayer(decider);
        assertEquals(ability.getApi(), ApiType.ChangeZone);
        assertEquals(ability.getParam("Origin"), "Graveyard");
        assertEquals(ability.getParam("Destination"), "Exile");
        assertEquals(ability.getParam("ValidTgts"), "Card");
        assertFalse(ability.hasParam("Optional"));
        ability.setOptionalTrigger(true);
        assertTrue(ability.isOptionalTrigger());
        assertTrue(ability.isIntrinsic());
        final TargetRestrictions restrictions = ability.getTargetRestrictions();
        assertNotNull(restrictions);
        assertTrue(ability.usesTargeting());
        assertEquals(restrictions.getZone(), List.of(ZoneType.Graveyard));
        assertFalse(restrictions.isRandomTarget());
        assertFalse(restrictions.isRandomNumTargets());
        assertEquals(ability.getMinTargets(), 1);
        assertEquals(ability.getMaxTargets(), 1);
        assertEquals(ability.getTargets().size(), 0);
        assertTrue(ability.canTarget(legalCard));

        final WrappedAbility wrapper = new WrappedAbility(trigger, ability, decider);
        assertEquals(wrapper.getDecider(), decider);
        assertTrue(wrapper.isOptionalTrigger());
        assertTrue(wrapper.isIntrinsic());

        ability.getTargets().add(legalCard);

        final ConfirmationDecisionProvider.Generation generation = provider.generate(wrapper, decider);

        assertEquals(generation.getStatus(), ConfirmationDecisionProvider.Status.ADMITTED);
        final DecisionRequest request = generation.getRequest();
        assertNotNull(request);
        assertEquals(request.getDecisionType(), DecisionType.CONFIRMATION);
        assertFalse(request.isForced());
        assertEquals(request.getCandidates().stream().map(LegalCandidate::getSemanticKey).toList(),
                List.of("ACCEPT", "DECLINE"));
    }
}
