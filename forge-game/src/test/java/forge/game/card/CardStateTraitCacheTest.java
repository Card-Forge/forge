/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.card;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import forge.card.CardStateName;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.util.Localizer;
import forge.util.collect.FCollectionView;
import forge.util.perf.PerfCounter;
import forge.util.perf.PerfProbe;

/** Correctness and engagement tests for the per-CardState derived-trait caches. */
public class CardStateTraitCacheTest {
    @BeforeClass
    public void initializeLocalization() {
        Path languages = Path.of("forge-gui", "res", "languages").toAbsolutePath();
        if (!Files.isDirectory(languages)) {
            languages = Path.of("..", "forge-gui", "res", "languages").toAbsolutePath();
        }
        Localizer.getInstance().initialize("en-US", languages.toString());
    }

    @AfterMethod
    public void restoreProbeState() {
        PerfProbe.reset();
    }

    @Test
    public void repeatedReadsReuseAllFourDerivedViews() {
        final Card card = new Card(1, null);

        PerfProbe.reset();
        PerfProbe.setEnabled(true);
        try {
            final FCollectionView<Trigger> triggers = card.getTriggers();
            final FCollectionView<StaticAbility> statics = card.getStaticAbilities();
            final FCollectionView<ReplacementEffect> replacements = card.getReplacementEffects();
            final FCollectionView<ReplacementEffect> plainReplacements =
                    card.getCurrentState().getReplacementEffects(false);

            Assert.assertSame(card.getTriggers(), triggers);
            Assert.assertSame(card.getStaticAbilities(), statics);
            Assert.assertSame(card.getReplacementEffects(), replacements);
            Assert.assertSame(card.getCurrentState().getReplacementEffects(false), plainReplacements);
            Assert.assertNotSame(replacements, plainReplacements,
                    "rules-host and plain replacement views have different semantics");
        } finally {
            PerfProbe.setEnabled(false);
        }

        Assert.assertEquals(PerfProbe.getGlobal().get(PerfCounter.TRAIT_CACHE_REBUILDS), 4L);
        Assert.assertEquals(PerfProbe.getGlobal().get(PerfCounter.TRAIT_CACHE_HITS), 4L);
    }

    @Test
    public void changedTraitLayersInvalidateEveryDerivedView() {
        final Card card = new Card(1, null);
        final CardState state = card.getCurrentState();

        final FCollectionView<Trigger> triggersBefore = card.getTriggers();
        final FCollectionView<StaticAbility> staticsBefore = card.getStaticAbilities();
        final FCollectionView<ReplacementEffect> replacementsBefore = card.getReplacementEffects();

        final Trigger trigger = TriggerHandler.parseTrigger(
                "Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | TriggerZones$ Battlefield",
                card, false, state);
        final StaticAbility staticAbility = StaticAbility.create(
                "Mode$ Continuous | Affected$ Card.Self | AddPower$ 1", card, state, false);
        final ReplacementEffect replacement = ReplacementHandler.parseReplacement(
                "Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield "
                        + "| ReplacementResult$ NotReplaced | Description$ Test replacement.",
                card, false);
        final long timestamp = 17L;

        card.addChangedCardTraits(new CardTraitChanges(null, List.of(trigger), List.of(replacement),
                List.of(staticAbility), null), timestamp, 0L, false);

        final FCollectionView<Trigger> triggersWithChange = card.getTriggers();
        final FCollectionView<StaticAbility> staticsWithChange = card.getStaticAbilities();
        final FCollectionView<ReplacementEffect> replacementsWithChange = card.getReplacementEffects();
        Assert.assertNotSame(triggersWithChange, triggersBefore);
        Assert.assertNotSame(staticsWithChange, staticsBefore);
        Assert.assertNotSame(replacementsWithChange, replacementsBefore);
        Assert.assertTrue(triggersWithChange.contains(trigger));
        Assert.assertTrue(staticsWithChange.contains(staticAbility));
        Assert.assertTrue(replacementsWithChange.contains(replacement));

        Assert.assertTrue(card.removeChangedCardTraits(timestamp, 0L));
        Assert.assertFalse(card.getTriggers().contains(trigger));
        Assert.assertFalse(card.getStaticAbilities().contains(staticAbility));
        Assert.assertFalse(card.getReplacementEffects().contains(replacement));
    }

    @Test
    public void clearingCountersDropsGeneratedReplacementEffects() {
        final Card card = new Card(1, null);

        final FCollectionView<ReplacementEffect> withoutShield = card.getReplacementEffects();
        card.setCounters(CounterEnumType.SHIELD, 1);
        final FCollectionView<ReplacementEffect> withShield = card.getReplacementEffects();
        Assert.assertNotSame(withShield, withoutShield);
        Assert.assertTrue(withShield.anyMatch(replacement -> replacement.hasParam("ShieldCounter")),
                "a shield counter must add its destroy replacement");

        card.clearCounters();
        final FCollectionView<ReplacementEffect> cleared = card.getReplacementEffects();
        Assert.assertNotSame(cleared, withShield);
        Assert.assertFalse(cleared.anyMatch(replacement -> replacement.hasParam("ShieldCounter")),
                "clearing counters must not leave a cached shield replacement behind");
    }

    @Test
    public void splitStateTopologyAndMutationsInvalidateOriginal() {
        final Card card = new Card(1, null);
        final CardState original = card.getCurrentState();
        final FCollectionView<Trigger> beforeSplit = original.getTriggers();

        card.addAlternateState(CardStateName.LeftSplit, false);
        final CardState left = card.getState(CardStateName.LeftSplit);
        final Trigger leftTrigger = TriggerHandler.parseTrigger(
                "Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | TriggerZones$ Battlefield",
                card, false, left);
        Assert.assertTrue(left.addTrigger(leftTrigger));

        final FCollectionView<Trigger> withSplit = original.getTriggers();
        Assert.assertNotSame(withSplit, beforeSplit);
        Assert.assertTrue(withSplit.contains(leftTrigger),
                "Original must merge the raw traits of its split state");

        card.clearStates(CardStateName.LeftSplit, false);
        Assert.assertFalse(original.getTriggers().contains(leftTrigger),
                "removing a split state must invalidate Original's merged view");
    }

    @Test
    public void changedTraitsInvalidateEveryCloneState() {
        final Card card = new Card(1, null);
        final CardCloneStates cloneStates = new CardCloneStates(card, null);
        cloneStates.add(card.getCurrentState().copy(card, CardStateName.Original, false));
        card.getCloneStates().put(23L, cloneStates);
        final CardState clone = cloneStates.get(CardStateName.Original);

        final FCollectionView<Trigger> beforeChange = clone.getTriggers();
        final Trigger trigger = TriggerHandler.parseTrigger(
                "Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | TriggerZones$ Battlefield",
                card, false, clone);
        card.addChangedCardTraits(new CardTraitChanges(null, List.of(trigger), null, null, null),
                29L, 0L, false);

        final FCollectionView<Trigger> afterChange = clone.getTriggers();
        Assert.assertNotSame(afterChange, beforeChange);
        Assert.assertTrue(afterChange.contains(trigger),
                "card-wide invalidation must include states held by CardCloneStates");
    }
}
