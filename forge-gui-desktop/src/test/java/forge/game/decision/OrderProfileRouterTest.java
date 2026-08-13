package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class OrderProfileRouterTest extends AITest {
    @Test
    public void preClassifierHasOnlyTheClosedL1FamilyAndUnownedShapes() {
        final Fixture fixture = fixture();
        final List<SpellAbility> nullEntry = new ArrayList<>();
        nullEntry.add(null);
        nullEntry.add(fixture.copy);

        assertEquals(OrderProfileRouter.preClassify(null),
                OrderProfileRouter.PreClassification.UNOWNED_OTHER);
        assertEquals(OrderProfileRouter.preClassify(List.of()),
                OrderProfileRouter.PreClassification.UNOWNED_OTHER);
        assertEquals(OrderProfileRouter.preClassify(List.of(fixture.copy)),
                OrderProfileRouter.PreClassification.UNOWNED_OTHER);
        assertEquals(OrderProfileRouter.preClassify(nullEntry),
                OrderProfileRouter.PreClassification.UNOWNED_OTHER);
        assertEquals(OrderProfileRouter.preClassify(List.of(fixture.copy, fixture.original)),
                OrderProfileRouter.PreClassification.UNOWNED_OTHER);
        assertEquals(OrderProfileRouter.preClassify(List.of(fixture.original,
                        fixture.secondOriginal)),
                OrderProfileRouter.PreClassification.UNOWNED_OTHER);
        assertEquals(OrderProfileRouter.preClassify(List.of(fixture.copy, fixture.secondCopy)),
                OrderProfileRouter.PreClassification.COPY_SPELL_FAMILY_INTENT);
        assertEquals(OrderProfileRouter.preClassify(List.of(fixture.trigger, fixture.secondTrigger)),
                OrderProfileRouter.PreClassification.L1_EXACT);
    }

    @Test
    public void familyIntentIsStrictlySplitIntoExactOrMalformedBeforeResolverLookup() {
        final Fixture fixture = fixture();
        final CopySpellResolveFirstOrderDecisionCoordinator coordinator =
                new CopySpellResolveFirstOrderDecisionCoordinator();

        assertEquals(OrderProfileRouter.classify(List.of(fixture.copy, fixture.secondCopy),
                        fixture.player, coordinator),
                OrderProfileRouter.Classification.L1C_EXACT);

        fixture.copy.getHostCard().setCastSA(null);
        assertEquals(OrderProfileRouter.preClassify(List.of(fixture.copy, fixture.secondCopy)),
                OrderProfileRouter.PreClassification.COPY_SPELL_FAMILY_INTENT);
        assertEquals(OrderProfileRouter.classify(List.of(fixture.copy, fixture.secondCopy),
                        fixture.player, coordinator),
                OrderProfileRouter.Classification.MALFORMED_L1C_INTENT);
    }

    @Test
    public void routerKeepsResolverOwnershipAndNativeCompatibilityDisjoint() {
        final Fixture fixture = fixture();
        final SimultaneousTriggerOrderDecisionCoordinator l1Coordinator =
                new SimultaneousTriggerOrderDecisionCoordinator();
        final CopySpellResolveFirstOrderDecisionCoordinator l1cCoordinator =
                new CopySpellResolveFirstOrderDecisionCoordinator();
        final SimultaneousTriggerOrderDecisionProvider l1Provider =
                new SimultaneousTriggerOrderDecisionProvider();
        final CopySpellResolveFirstOrderDecisionProvider l1cProvider =
                new CopySpellResolveFirstOrderDecisionProvider();
        final AtomicInteger l1cCalls = new AtomicInteger();
        l1cProvider.setResolver(request -> {
            l1cCalls.incrementAndGet();
            return request.getCandidates().get(0);
        });
        final List<SpellAbility> copiedResult = OrderProfileRouter.order(
                List.of(fixture.copy, fixture.secondCopy), fixture.player, l1Coordinator, l1Provider,
                l1cCoordinator, l1cProvider, ignored -> {
                    throw new AssertionError("native callback must not run for exact L1C");
                });
        assertEquals(l1cCalls.get(), 1);
        assertEquals(copiedResult, List.of(fixture.secondCopy, fixture.copy));

        final AtomicInteger nativeCalls = new AtomicInteger();
        final List<SpellAbility> unownedResult = OrderProfileRouter.order(
                List.of(fixture.copy, fixture.original), fixture.player, l1Coordinator, l1Provider,
                l1cCoordinator, l1cProvider, input -> {
                    nativeCalls.incrementAndGet();
                    return input;
                });
        assertEquals(nativeCalls.get(), 1);
        assertEquals(unownedResult, List.of(fixture.copy, fixture.original));

        fixture.copy.getHostCard().setCastSA(null);
        final AtomicInteger forbiddenNativeCalls = new AtomicInteger();
        expectThrows(SimultaneousTriggerOrderIntegrityException.class, () -> OrderProfileRouter.order(
                List.of(fixture.copy, fixture.secondCopy), fixture.player, l1Coordinator, l1Provider,
                l1cCoordinator, l1cProvider, input -> {
                    forbiddenNativeCalls.incrementAndGet();
                    return input;
                }));
        assertEquals(forbiddenNativeCalls.get(), 0);

        final CopySpellResolveFirstOrderDecisionProvider nativeCompatible =
                new CopySpellResolveFirstOrderDecisionProvider();
        final AtomicInteger malformedNativeCalls = new AtomicInteger();
        OrderProfileRouter.order(List.of(fixture.copy, fixture.secondCopy), fixture.player,
                l1Coordinator, l1Provider, l1cCoordinator, nativeCompatible, input -> {
                    malformedNativeCalls.incrementAndGet();
                    return input;
                });
        assertEquals(malformedNativeCalls.get(), 1);
        assertTrue(OrderProfileRouter.preClassify(List.of(fixture.copy, fixture.secondCopy))
                == OrderProfileRouter.PreClassification.COPY_SPELL_FAMILY_INTENT);
    }

    private Fixture fixture() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(0);
        final Player opponent = game.getPlayers().get(1);

        final Card source = addCardToZone("Pyromatics", player, ZoneType.Battlefield);
        final SpellAbility original = source.getFirstSpellAbility();
        original.setActivatingPlayer(player);
        final SpellAbility secondOriginal = addCardToZone("Pyromatics", player,
                ZoneType.Battlefield).getFirstSpellAbility();
        secondOriginal.setActivatingPlayer(player);
        final SpellAbility copy = CardFactory.copySpellAbilityAndPossiblyHost(original, original, player);
        final SpellAbility secondCopy = CardFactory.copySpellAbilityAndPossiblyHost(original, original, player);

        final WrappedAbility trigger = triggerFor("Gelectrode", player, opponent);
        final WrappedAbility secondTrigger = triggerFor("Gelectrode", player, opponent);
        return new Fixture(player, original, secondOriginal, copy, secondCopy, trigger, secondTrigger);
    }

    private WrappedAbility triggerFor(final String sourceName, final Player player, final Player opponent) {
        final Card source = addCard(sourceName, player);
        final Trigger trigger = source.getTriggers().stream()
                .filter(value -> TriggerType.SpellCast.equals(value.getMode()))
                .findFirst().orElseThrow();
        final SpellAbility effect = AbilityFactory.getAbility(source, trigger.getParam("Execute"));
        effect.setActivatingPlayer(player);
        effect.setOptionalTrigger(true);
        effect.setIntrinsic(true);
        final Card castCard = addCardToZone("Opt", player, ZoneType.Hand);
        final SpellAbility castAbility = castCard.getFirstSpellAbility();
        castAbility.setActivatingPlayer(player);
        final Map<AbilityKey, Object> triggeringObjects = AbilityKey.newMap();
        triggeringObjects.put(AbilityKey.Activator, opponent);
        triggeringObjects.put(AbilityKey.SpellAbility, castAbility);
        trigger.setTriggeringObjects(effect, triggeringObjects);
        return new WrappedAbility(trigger, effect, player);
    }

    private static final class Fixture {
        private final Player player;
        private final SpellAbility original;
        private final SpellAbility secondOriginal;
        private final SpellAbility copy;
        private final SpellAbility secondCopy;
        private final WrappedAbility trigger;
        private final WrappedAbility secondTrigger;

        private Fixture(final Player player, final SpellAbility original, final SpellAbility secondOriginal,
                final SpellAbility copy, final SpellAbility secondCopy, final WrappedAbility trigger,
                final WrappedAbility secondTrigger) {
            this.player = player;
            this.original = original;
            this.secondOriginal = secondOriginal;
            this.copy = copy;
            this.secondCopy = secondCopy;
            this.trigger = trigger;
            this.secondTrigger = secondTrigger;
        }
    }
}
