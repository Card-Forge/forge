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

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.ai.simulation.GameCopier;
import forge.card.CardStateName;
import forge.game.Game;
import forge.game.CardTraitBase;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

/**
 * The mutator audit the trait-view cache needs, run over real cards in a real game.
 *
 * <p>§4.2 of the performance plan makes this the merge gate for that cache: "Run this through
 * transform, copy, perpetual/change-text, gain/lose ability, face-down, mutate/merge, LKI, and
 * simulation-copy tests. If a complete mutator audit is not possible, do not merge this cache merely
 * on benchmark results."</p>
 *
 * <p>Each test follows the same shape: read every derived view on every state so the caches are
 * warm, change something the views are derived from, then read them all again. The detector is the
 * assertion inside {@code CardState}, which rebuilds each view on a cache hit and fails if the
 * cached one no longer matches — so a missed invalidation surfaces as a failure here rather than as
 * a card behaving wrongly in a game. The content assertions are secondary: they confirm the reads
 * actually observed the change, so a scenario cannot pass by touching nothing.</p>
 */
public class TraitCacheScenarioTest extends AITest {

    /** Reads all four cached views on every state of a card, including its cloned states. */
    private static int warmAllViews(final Card card) {
        int seen = 0;
        for (final CardStateName name : new ArrayList<>(card.getStates())) {
            final CardState state = card.getState(name);
            if (state == null) {
                continue;
            }
            seen += state.getTriggers().size();
            seen += state.getStaticAbilities().size();
            seen += state.getReplacementEffects(true).size();
            seen += state.getReplacementEffects(false).size();
        }
        for (final CardCloneStates clone : card.getCloneStates().values()) {
            for (final CardState state : clone.values()) {
                seen += state.getTriggers().size();
                seen += state.getStaticAbilities().size();
                seen += state.getReplacementEffects(true).size();
                seen += state.getReplacementEffects(false).size();
            }
        }
        return seen;
    }

    private static List<String> describe(final FCollectionView<? extends CardTraitBase> traits) {
        final List<String> out = new ArrayList<>();
        for (final CardTraitBase t : traits) {
            out.add(String.valueOf(t));
        }
        return out;
    }

    private Game gameWithBattlefield() {
        final Game game = initAndCreateGame();
        game.getAction().checkStateEffects(true);
        return game;
    }

    /** A double-faced card changing face: each face is its own state with its own traits. */
    @Test(timeOut = 300000)
    public void transformingACardKeepsEveryFacesViewsCurrent() {
        final Game game = gameWithBattlefield();
        final Player p = game.getPlayers().get(1);
        final Card dfc = addCard("Delver of Secrets", p);

        warmAllViews(dfc);
        final List<String> frontTriggers = describe(dfc.getCurrentState().getTriggers());

        Assert.assertTrue(dfc.changeToState(CardStateName.Backside), "the fixture card must transform");
        warmAllViews(dfc);
        game.getAction().checkStateEffects(true);
        warmAllViews(dfc);

        Assert.assertNotEquals(describe(dfc.getCurrentState().getTriggers()), frontTriggers,
                "the two faces must not present the same triggers, or this proves nothing");
    }

    /** Face-down is a distinct state whose traits are the morph rules, not the card's. */
    @Test(timeOut = 300000)
    public void turningFaceDownAndBackKeepsViewsCurrent() {
        final Game game = gameWithBattlefield();
        final Player p = game.getPlayers().get(1);
        final Card c = addCard("Grizzly Bears", p);

        warmAllViews(c);
        c.turnFaceDown(true);
        warmAllViews(c);
        game.getAction().checkStateEffects(true);
        warmAllViews(c);

        c.setState(CardStateName.Original, true);
        c.setFaceDown(false);
        game.getAction().checkStateEffects(true);
        warmAllViews(c);
    }

    /** A split card's Original state concatenates the raw traits of both halves. */
    @Test(timeOut = 300000)
    public void splitCardHalvesFeedTheOriginalView() {
        final Game game = gameWithBattlefield();
        final Player p = game.getPlayers().get(1);
        final Card split = addCardToZone("Fire // Ice", p, ZoneType.Hand);

        Assert.assertTrue(split.hasState(CardStateName.LeftSplit), "the fixture must be a split card");
        warmAllViews(split);
        final int before = split.getState(CardStateName.Original).getTriggers().size();

        // a trait added to one half has to appear in the merged Original view
        final CardState left = split.getState(CardStateName.LeftSplit);
        final Trigger borrowed = anyTriggerFrom(addCard("Soul Warden", p));
        left.addTrigger(borrowed);
        warmAllViews(split);

        Assert.assertEquals(split.getState(CardStateName.Original).getTriggers().size(), before + 1,
                "a raw trait added to a split half must show up in the merged Original view");
    }

    private static Trigger anyTriggerFrom(final Card card) {
        final FCollectionView<Trigger> triggers = card.getCurrentState().getTriggers();
        Assert.assertFalse(triggers.isEmpty(), "the fixture card must have a trigger to borrow");
        return triggers.get(0).copy(card, false);
    }

    /** An Adventure adds a global replacement effect, but only to the rules-host view. */
    @Test(timeOut = 300000)
    public void adventureRulesAppearOnlyInTheRulesHostView() {
        final Game game = gameWithBattlefield();
        final Player p = game.getPlayers().get(1);
        final Card adventurer = addCardToZone("Bonecrusher Giant", p, ZoneType.Hand);

        warmAllViews(adventurer);

        // the Adventure subtype sits on the secondary state, so find whichever state carries it
        CardState adventureState = null;
        for (final CardStateName name : adventurer.getStates()) {
            final CardState state = adventurer.getState(name);
            if (state != null && state.getReplacementEffects(true).size() > state.getReplacementEffects(false).size()) {
                adventureState = state;
                break;
            }
        }
        Assert.assertNotNull(adventureState,
                "no state carried a rules-host-only replacement effect, so the two views are never "
                        + "actually different here and this fixture proves nothing");

        final int asRulesHost = adventureState.getReplacementEffects(true).size();
        final int plain = adventureState.getReplacementEffects(false).size();
        // reading them again in the other order must not swap the two cached entries
        Assert.assertEquals(adventureState.getReplacementEffects(false).size(), plain);
        Assert.assertEquals(adventureState.getReplacementEffects(true).size(), asRulesHost);
    }

    /** Counters that generate replacement effects: shield and stun. */
    @Test(timeOut = 300000)
    public void counterDrivenReplacementEffectsStayCurrent() {
        final Game game = gameWithBattlefield();
        final Player p = game.getPlayers().get(1);
        final Card c = addCard("Grizzly Bears", p);

        warmAllViews(c);
        final int base = c.getCurrentState().getReplacementEffects(true).size();

        // setCounters is the mutator the cache overrides; addCounter(..., table) only stages
        c.setCounters(CounterEnumType.SHIELD, 1);
        warmAllViews(c);
        game.getAction().checkStateEffects(true);
        warmAllViews(c);
        final int withShield = c.getCurrentState().getReplacementEffects(true).size();
        Assert.assertTrue(withShield > base, "a shield counter must add a replacement effect");

        c.setCounters(CounterEnumType.STUN, 1);
        warmAllViews(c);
        game.getAction().checkStateEffects(true);
        warmAllViews(c);
        Assert.assertTrue(c.getCurrentState().getReplacementEffects(true).size() > withShield,
                "a stun counter must add a replacement effect");

        c.clearCounters();
        warmAllViews(c);
        game.getAction().checkStateEffects(true);
        warmAllViews(c);
        Assert.assertEquals(c.getCurrentState().getReplacementEffects(true).size(), base,
                "clearing the counters must take those replacement effects away again");
    }

    /**
     * The changed-trait layers: gaining traits, gaining them by text change, and losing them again —
     * the last being the path a perpetual effect removal takes.
     */
    @Test(timeOut = 300000)
    public void changedTraitLayersAddedAndRemovedStayCurrent() {
        final Game game = gameWithBattlefield();
        final Player p = game.getPlayers().get(1);
        final Card c = addCard("Grizzly Bears", p);
        final Card donor = addCard("Soul Warden", p);

        warmAllViews(c);
        final int base = c.getCurrentState().getTriggers().size();

        final long ts = game.getNextTimestamp();
        c.addChangedCardTraits(null, List.of(anyTriggerFrom(donor)), null, null, null, ts, 0);
        // read before anything else runs: a later checkStateEffects would invalidate incidentally
        // and hide a missing invalidation on this mutator
        warmAllViews(c);
        game.getAction().checkStateEffects(true);
        warmAllViews(c);
        Assert.assertEquals(c.getCurrentState().getTriggers().size(), base + 1,
                "a granted trigger must appear in the view");

        final long textTs = game.getNextTimestamp();
        c.addChangedCardTraitsByText(null, List.of(anyTriggerFrom(donor)), null, null, textTs, 0);
        warmAllViews(c);
        game.getAction().checkStateEffects(true);
        warmAllViews(c);
        Assert.assertEquals(c.getCurrentState().getTriggers().size(), base + 2,
                "a trigger granted by text change must appear too");

        // the perpetual-removal path
        Assert.assertTrue(c.removeChangedCardTraits(ts, 0));
        warmAllViews(c);
        Assert.assertTrue(c.removeChangedCardTraitsByText(textTs, 0));
        warmAllViews(c);
        game.getAction().checkStateEffects(true);
        warmAllViews(c);
        Assert.assertEquals(c.getCurrentState().getTriggers().size(), base,
                "removing both layers must take the granted triggers away again");
    }

    /** An LKI copy is a separate card and must not share or inherit cached views. */
    @Test(timeOut = 300000)
    public void lkiCopiesDoNotShareCachedViews() {
        final Game game = gameWithBattlefield();
        final Player p = game.getPlayers().get(1);
        final Card c = addCard("Soul Warden", p);

        warmAllViews(c);
        final Card lki = CardCopyService.getLKICopy(c);
        warmAllViews(lki);

        final FCollectionView<Trigger> originalTriggers = c.getCurrentState().getTriggers();
        final FCollectionView<Trigger> lkiTriggers = lki.getCurrentState().getTriggers();
        Assert.assertNotSame(lkiTriggers, originalTriggers,
                "an LKI copy must not share the cached collection object with the card it copied");
        for (final Trigger t : lkiTriggers) {
            for (final Trigger o : originalTriggers) {
                Assert.assertNotSame(t, o, "an LKI copy must not share trait objects either");
            }
        }

        // mutating the original must not be visible through the copy's cache, or vice versa
        final int lkiBefore = lkiTriggers.size();
        c.addChangedCardTraits(null, List.of(anyTriggerFrom(c)), null, null, null, game.getNextTimestamp(), 0);
        warmAllViews(c);
        warmAllViews(lki);
        Assert.assertEquals(lki.getCurrentState().getTriggers().size(), lkiBefore,
                "a change to the original must not leak into the LKI copy");
    }

    /** A cloned card gets its cloned states' views, and losing the clone gets its own back. */
    @Test(timeOut = 300000)
    public void cloneStatesAndTheirRemovalStayCurrent() {
        final Game game = gameWithBattlefield();
        final Player p = game.getPlayers().get(1);
        final Card target = addCard("Grizzly Bears", p);
        final Card source = addCard("Soul Warden", p);

        warmAllViews(target);
        final int ownTriggers = target.getCurrentState().getTriggers().size();

        final long ts = game.getNextTimestamp();
        final CardCloneStates cloned = CardFactory.getCloneStates(source, target, source.getFirstSpellAbility());
        target.addCloneState(cloned, ts);
        warmAllViews(target);
        game.getAction().checkStateEffects(true);
        warmAllViews(target);
        final int clonedTriggers = target.getCurrentState().getTriggers().size();
        Assert.assertTrue(clonedTriggers > ownTriggers,
                "cloning a card with a trigger must change what the view returns");

        Assert.assertTrue(target.removeCloneState(ts));
        warmAllViews(target);
        game.getAction().checkStateEffects(true);
        warmAllViews(target);
        Assert.assertEquals(target.getCurrentState().getTriggers().size(), ownTriggers,
                "losing the clone must restore the card's own traits");
    }

    /**
     * "Becomes a copy of" writes another card's characteristics into a state that is already live —
     * and, unlike a clone state, already has warm caches.
     */
    @Test(timeOut = 300000)
    public void copyingCharacteristicsOntoALiveCardStaysCurrent() {
        final Game game = gameWithBattlefield();
        final Player p = game.getPlayers().get(1);
        final Card target = addCard("Grizzly Bears", p);
        final Card source = addCard("Soul Warden", p);

        warmAllViews(target);
        final int ownTriggers = target.getCurrentState().getTriggers().size();

        new CardCopyService(source).copyCopiableCharacteristics(target, null, null);
        // read before anything else can invalidate incidentally
        warmAllViews(target);

        Assert.assertTrue(target.getCurrentState().getTriggers().size() > ownTriggers,
                "copying a card with a trigger onto a live card must change what its view returns");
    }

    /**
     * A simulated game copy: every copied card is a new object and must build its own views, and the
     * copy's caches must not be tied to the original game.
     */
    @Test(timeOut = 300000)
    public void simulationCopiesBuildIndependentViews() {
        final Game game = gameWithBattlefield();
        final Player p = game.getPlayers().get(1);
        addCard("Soul Warden", p);
        addCard("Glorious Anthem", p);
        addCard("Bonecrusher Giant", p);
        final Card counterHolder = addCard("Grizzly Bears", p);
        counterHolder.setCounters(CounterEnumType.SHIELD, 1);
        game.getAction().checkStateEffects(true);

        for (final Card c : game.getCardsInGame()) {
            warmAllViews(c);
        }

        final GameCopier copier = new GameCopier(game);
        final Game copy = copier.makeCopy(null, p);
        for (final Card c : copy.getCardsInGame()) {
            warmAllViews(c);
        }

        // and the originals must still be current after the copy walked them
        for (final Card c : game.getCardsInGame()) {
            warmAllViews(c);
        }

        for (final Card original : game.getCardsInGame()) {
            final Object mapped = copier.find(original);
            if (!(mapped instanceof Card copied)) {
                continue;
            }
            Assert.assertNotSame(copied.getCurrentState().getTriggers(),
                    original.getCurrentState().getTriggers(),
                    "a copied card must not share its cached view with the card it was copied from");
        }
    }

    /** Static abilities granted by another permanent, applied and then removed by the layer system. */
    @Test(timeOut = 300000)
    public void staticGrantedAbilitiesStayCurrentAcrossLayerRuns() {
        final Game game = gameWithBattlefield();
        final Player p = game.getPlayers().get(1);
        final Card bear = addCard("Grizzly Bears", p);
        game.getAction().checkStateEffects(true);
        warmAllViews(bear);

        final List<String> before = describe(bear.getCurrentState().getStaticAbilities());

        // Lightning Greaves grants keywords, which is a cached-view input
        final Card greaves = addCard("Lightning Greaves", p);
        greaves.attachToEntity(bear, null);
        warmAllViews(bear);
        game.getAction().checkStateEffects(true);
        warmAllViews(bear);
        Assert.assertTrue(bear.hasKeyword("Haste"), "the fixture must actually grant a keyword");

        greaves.unattachFromEntity(bear);
        warmAllViews(bear);
        game.getAction().checkStateEffects(true);
        warmAllViews(bear);
        Assert.assertEquals(describe(bear.getCurrentState().getStaticAbilities()), before,
                "removing the granted keywords must restore the original static abilities");
        Assert.assertFalse(bear.hasKeyword("Haste"));
    }

    /** Whole-game play with the caches warm on every card, every turn. */
    @Test(timeOut = 600000)
    public void playingATurnKeepsEveryCardsViewsCurrent() {
        final Game game = gameWithBattlefield();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);

        for (int i = 0; i < 4; i++) {
            addCard("Mountain", ai);
        }
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        addCardToZone("Bonecrusher Giant", ai, ZoneType.Hand);
        addCard("Soul Warden", ai).setSickness(false);
        addCard("Glorious Anthem", ai);
        for (final Card c : addCards("Grizzly Bears", 2, ai)) {
            c.setSickness(false);
        }
        addCards("Runeclaw Bear", 2, opponent);
        addCard("Sulfuric Vortex", opponent);
        fillLibrary(ai, 12);
        fillLibrary(opponent, 12);

        game.getPhaseHandler().devModeSet(forge.game.phase.PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        int warmed = 0;
        for (final Card c : game.getCardsInGame()) {
            warmed += warmAllViews(c);
        }
        Assert.assertTrue(warmed > 0, "the fixture must have traits to cache");

        playUntilNextTurn(game);

        for (final Card c : game.getCardsInGame()) {
            warmAllViews(c);
        }
    }

    /** Spell and mana ability views are deliberately not cached; they must still be rebuilt. */
    @Test(timeOut = 300000)
    public void spellAndManaAbilityViewsAreNotCached() {
        final Game game = gameWithBattlefield();
        final Player p = game.getPlayers().get(1);
        final Card land = addCard("Mountain", p);
        warmAllViews(land);

        final FCollectionView<SpellAbility> first = land.getCurrentState().getSpellAbilities();
        final FCollectionView<SpellAbility> second = land.getCurrentState().getSpellAbilities();
        Assert.assertNotSame(first, second,
                "spell ability views must not be cached: they synthesise land/permanent abilities "
                        + "and call card-level update logic that the plan excludes from this change");
    }

    /**
     * The cached views are handed out through {@code FCollectionView}, which extends
     * {@code Collection} and therefore inherits {@code remove}, {@code clear} and {@code removeIf}.
     * Before the cache, mutating a returned view corrupted a throwaway collection; now it would
     * corrupt the cache for every later reader. This records that no caller does so today — the
     * hazard is latent, not live — and fails if one appears.
     */
    @Test(timeOut = 300000)
    public void nothingMutatesACachedViewThroughTheReturnedCollection() {
        final Game game = gameWithBattlefield();
        final Player p = game.getPlayers().get(1);
        final Card c = addCard("Soul Warden", p);
        warmAllViews(c);

        final CardState state = c.getCurrentState();
        final List<String> triggersBefore = describe(state.getTriggers());
        final List<String> staticsBefore = describe(state.getStaticAbilities());
        final List<String> replacementsBefore = describe(state.getReplacementEffects(true));

        // play a whole turn: if any engine path mutates a handed-out view, the cached contents move
        addCard("Mountain", p);
        fillLibrary(p, 8);
        fillLibrary(game.getPlayers().get(0), 8);
        game.getPhaseHandler().devModeSet(forge.game.phase.PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);
        playUntilNextTurn(game);

        warmAllViews(c);
        Assert.assertEquals(describe(state.getTriggers()), triggersBefore,
                "something mutated the cached trigger view through the collection it was handed");
        Assert.assertEquals(describe(state.getStaticAbilities()), staticsBefore,
                "something mutated the cached static ability view through the collection it was handed");
        Assert.assertEquals(describe(state.getReplacementEffects(true)), replacementsBefore,
                "something mutated the cached replacement view through the collection it was handed");
    }
}
