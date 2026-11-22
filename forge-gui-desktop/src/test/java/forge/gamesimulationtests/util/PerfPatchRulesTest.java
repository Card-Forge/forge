package forge.gamesimulationtests.util;

import forge.ai.simulation.GameSimulationTest;
import forge.card.CardStateName;
import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardState;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.zone.ZoneType;
import forge.util.Visitor;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;

/**
 * Rules / cache-coherence regression tests for the ANDROID-OPTIMIZE patch.
 *
 * IDEA: the patch caches CardState.getStaticAbilities()/getReplacementEffects(). Because the private
 * build methods (buildStaticAbilities / buildReplacementEffects) are still the uncached ground truth,
 * every test can ask "does the cached answer equal the freshly built answer?" - after every kind of
 * mutation. That catches stale caches even for rules interactions.
 *
 * Lines marked "ADAPT" use engine APIs whose exact signature differs between Forge revisions.
 */
public class PerfPatchRulesTest extends GameSimulationTest {

    // plumbing

    private static Card move(Game g, Card c, ZoneType z) {
        return g.getAction().moveTo(z, c, null, AbilityKey.newMap()); // ADAPT: moveTo signature
    }

    private static void settle(Game g) {
        g.getAction().checkStateEffects(true);
    }

    private static void installCombat(Game g, Combat c) {
        g.getPhaseHandler().setCombat(c); // ADAPT: where the real combat is stored
    }

    private static List<String> sig(Iterable<? extends CardTraitBase> traits) {
        List<String> out = new ArrayList<>();
        for (CardTraitBase t : traits) {
            out.add(t.getClass().getSimpleName() + "#" + (t.getHostCard() == null ? -1 : t.getHostCard().getId())
                    + new TreeMap<>(t.getMapParams()));
        }
        return out;
    }

    /** null when the method does not exist (= unpatched baseline tree, where nothing is cached). */
    private static Method privateMethod(String name, Class<?>... params) {
        try {
            Method m = CardState.class.getDeclaredMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Cached read must equal the uncached ground truth for this state.
     * On the unpatched baseline the build* methods do not exist; then only the read itself is exercised
     * (so the rules assertions of each test still run and can be validated on baseline).
     */
    @SuppressWarnings("unchecked")
    private static void assertCoherent(CardState s, String ctx) throws Exception {
        Method buildStatic = privateMethod("buildStaticAbilities");
        Method buildRepl = privateMethod("buildReplacementEffects", boolean.class);

        List<String> cachedStaticSig = sig(s.getStaticAbilities());
        if (buildStatic != null) {
            List<String> freshStatic = sig((Iterable<? extends CardTraitBase>) buildStatic.invoke(s));
            Assert.assertEquals(cachedStaticSig, freshStatic, "STALE static-ability cache: " + s.getCard() + "/" + s.getStateName() + " " + ctx);
        }

        for (boolean rulesHost : new boolean[]{true, false}) {
            List<String> cached = sig(s.getReplacementEffects(rulesHost));
            if (buildRepl != null) {
                List<String> fresh = sig((Iterable<? extends CardTraitBase>) buildRepl.invoke(s, rulesHost));
                Assert.assertEquals(cached, fresh, "STALE replacement-effect cache (rulesHost=" + rulesHost + "): " + s.getCard() + "/" + s.getStateName() + " " + ctx);
            }
        }
    }

    private static void assertCoherent(Card c, String ctx) throws Exception {
        for (CardStateName n : c.getStates()) {
            assertCoherent(c.getState(n), ctx);
        }
    }

    private static void assertCoherentAll(Game g, String ctx) throws Exception {
        for (Card c : g.getCardsInGame()) {
            assertCoherent(c, ctx);
        }
    }

    private static StaticAbility newStatic(Card host, CardState st) {
        return StaticAbility.create("Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | Description$ perf test", host, st, true);   // ADAPT
    }

    private static ReplacementEffect newReplacement(Card host, CardState st) {
        return ReplacementHandler.parseReplacement("Event$ DamageDone | Prevent$ True | ValidTarget$ Card.Self | Description$ perf test", host, true, st); // ADAPT
    }

    // Layers: keywords / abilities (Layer 6) and types (Layer 4) must be reflected by the caches

    @Test
    public void humility_removesAndRestoresKeywordsAndAbilities() throws Exception {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Card angel = addCard("Serra Angel", p);
        settle(game);
        Assert.assertTrue(angel.hasKeyword(Keyword.FLYING));
        Assert.assertEquals(angel.getNetPower(), 4);

        Card humility = addCard("Humility", p);
        settle(game);
        Assert.assertFalse(angel.hasKeyword(Keyword.FLYING), "Layer 6: Humility removes flying");
        Assert.assertFalse(angel.hasKeyword(Keyword.VIGILANCE), "Layer 6: Humility removes vigilance");
        Assert.assertEquals(angel.getNetPower(), 1, "Layer 7b: 1/1");
        Assert.assertEquals(angel.getNetToughness(), 1);
        assertCoherentAll(game, "with Humility");

        move(game, humility, ZoneType.Graveyard);
        settle(game);
        Assert.assertTrue(angel.hasKeyword(Keyword.FLYING), "flying is back when Humility leaves");
        Assert.assertEquals(angel.getNetPower(), 4);
        assertCoherentAll(game, "after Humility left");
    }

    @Test
    public void humility_removesIntrinsicReplacementEffectsAndRestoresThem() throws Exception {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Card worker = addCard("Arcbound Worker", p); // Modular 1 -> intrinsic ETB-counter replacement
        settle(game);
        int before = worker.getReplacementEffects().size();
        Assert.assertTrue(before > 0, "premise: Modular creates a replacement effect (check on baseline)");

        Card humility = addCard("Humility", p);
        settle(game);
        // (no size assertion here: on the baseline tree Humility does not shrink this list for Arcbound Worker,
        //  so "fewer" was a wrong premise. The meaningful checks are coherence and exact restoration.)
        assertCoherent(worker, "under Humility");

        move(game, humility, ZoneType.Graveyard);
        settle(game);
        Assert.assertEquals(worker.getReplacementEffects().size(), before, "restored after Humility leaves");
        assertCoherent(worker, "after Humility");
    }

    @Test
    public void bloodMoon_landTypeChangeRemovesIntrinsicAbilitiesAndRestores() throws Exception {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Card tower = addCard("Command Tower", p);
        Card forest = addCard("Forest", p);
        settle(game);
        Assert.assertFalse(tower.getType().hasSubtype("Mountain"));

        Card moon = addCard("Blood Moon", p);
        settle(game);
        Assert.assertTrue(tower.getType().hasSubtype("Mountain"), "Layer 4: nonbasic land becomes a Mountain");
        Assert.assertFalse(forest.getType().hasSubtype("Mountain"), "basic lands are unaffected");
        Assert.assertEquals(tower.getManaAbilities().size(), 1);
        Assert.assertEquals(tower.getManaAbilities().get(0).getManaPart().getOrigProduced(), "R", "only the Mountain ability remains");
        Assert.assertTrue(tower.hasRemoveIntrinsic(), "CR 305.7 flag read by the cache");
        assertCoherentAll(game, "with Blood Moon");

        move(game, moon, ZoneType.Graveyard);
        settle(game);
        Assert.assertFalse(tower.getType().hasSubtype("Mountain"));
        Assert.assertFalse(tower.hasRemoveIntrinsic());
        assertCoherentAll(game, "after Blood Moon left");
    }

    @Test
    public void urborg_addsSwampToAllLands_andBloodMoonTimestampInteraction() throws Exception {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Card forest = addCard("Forest", p);
        Card urborg = addCard("Urborg, Tomb of Yawgmoth", p);
        settle(game);
        Assert.assertTrue(forest.getType().hasSubtype("Swamp"), "Urborg: each land is a Swamp in addition");
        Assert.assertEquals(forest.getManaAbilities().size(), 2, "Forest gains the Swamp mana ability");
        assertCoherentAll(game, "Urborg");

        Card moon = addCard("Blood Moon", p);                           // later timestamp: Urborg becomes a vanilla Mountain
        settle(game);
        Assert.assertTrue(urborg.getType().hasSubtype("Mountain"));
        Assert.assertFalse(forest.getType().hasSubtype("Swamp"), "Urborg lost its ability, so Forest is no longer a Swamp");
        Assert.assertEquals(forest.getManaAbilities().size(), 1);
        assertCoherentAll(game, "Urborg + Blood Moon");

        move(game, moon, ZoneType.Graveyard);
        settle(game);
        Assert.assertTrue(forest.getType().hasSubtype("Swamp"), "Urborg works again");
        assertCoherentAll(game, "Blood Moon gone");
    }

    // =====================================================================================
    // Replacement effects (ReplacementHandler.getReplacementList rewrite)
    // =====================================================================================

    @Test
    public void restInPeace_exilesInsteadOfGraveyard_andStopsWhenGone() throws Exception {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", p);
        Card rip = addCard("Rest in Peace", p);
        settle(game);

        Card moved = move(game, bears, ZoneType.Graveyard);
        Assert.assertEquals(moved.getZone().getZoneType(), ZoneType.Exile, "graveyard replacement from a static replacer");

        Card bears2 = addCard("Grizzly Bears", p);
        move(game, rip, ZoneType.Exile);
        settle(game);
        Card moved2 = move(game, bears2, ZoneType.Graveyard);
        Assert.assertEquals(moved2.getZone().getZoneType(), ZoneType.Graveyard, "no replacer left -> normal graveyard");
        assertCoherentAll(game, "RIP lifecycle");
    }

    @Test
    public void etb_selfReplacement_landEntersTapped() throws Exception {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Card cove = addCardToZone("Tranquil Cove", p, ZoneType.Hand);
        Card forest = addCardToZone("Forest", p, ZoneType.Hand);
        Card cove2 = move(game, cove, ZoneType.Battlefield);
        Card forest2 = move(game, forest, ZoneType.Battlefield);
        Assert.assertTrue(cove2.isTapped(), "ETB-tapped replacement (ETB pre-list path)");
        Assert.assertFalse(forest2.isTapped());
        assertCoherentAll(game, "ETB tapped");
    }

    @Test
    public void etb_replacementFromAnotherPermanent_appliesOnlyToOpponentCreatures() throws Exception {
        Game game = initAndCreateGame();
        Player p0 = game.getPlayers().get(0);
        Player p1 = game.getPlayers().get(1);
        Card authority = addCard("Authority of the Consuls", p0);
        settle(game);
        Card own = move(game, addCardToZone("Grizzly Bears", p0, ZoneType.Hand), ZoneType.Battlefield);
        Card theirs = move(game, addCardToZone("Grizzly Bears", p1, ZoneType.Hand), ZoneType.Battlefield);
        Assert.assertFalse(own.isTapped(), "own creature enters untapped");
        Assert.assertTrue(theirs.isTapped(), "opponent creature enters tapped");

        move(game, authority, ZoneType.Graveyard);
        settle(game);
        Card theirs2 = move(game, addCardToZone("Grizzly Bears", p1, ZoneType.Hand), ZoneType.Battlefield);
        Assert.assertFalse(theirs2.isTapped(), "no replacer -> untapped");
        assertCoherentAll(game, "Authority lifecycle");
    }

    @Test
    public void etbCounters_andCounterDoubling() throws Exception {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Card hydra = move(game, addCardToZone("Kalonian Hydra", p, ZoneType.Hand), ZoneType.Battlefield);
        Assert.assertEquals(hydra.getCounters(CounterEnumType.P1P1), 4, "enters with four +1/+1 counters");

        addCard("Doubling Season", p);
        settle(game);
        Card hydra2 = move(game, addCardToZone("Kalonian Hydra", p, ZoneType.Hand), ZoneType.Battlefield);
        Assert.assertEquals(hydra2.getCounters(CounterEnumType.P1P1), 8, "Doubling Season doubles ETB counters (replacement stacking)");
        assertCoherentAll(game, "counters");
    }

    @Test
    public void shieldStunFinalityCounters_areHiddenCacheInputs() throws Exception {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", p);
        int base = bears.getReplacementEffects().size();
        assertCoherent(bears, "baseline");

        for (CounterEnumType type : new CounterEnumType[]{CounterEnumType.SHIELD, CounterEnumType.STUN, CounterEnumType.FINALITY}) {
            int before = bears.getReplacementEffects().size();
            bears.addCounterInternal(type, 1, p, false, null, null);                    // ADAPT: counter API
            Assert.assertTrue(bears.getReplacementEffects().size() > before, type + " counter must add a replacement effect");
            assertCoherent(bears, "with " + type);
            bears.subtractCounter(type, 1, p);                                          // ADAPT: counter API
            Assert.assertEquals(bears.getReplacementEffects().size(), before, type + " removal must remove it again");
            assertCoherent(bears, "without " + type);
        }
        Assert.assertEquals(bears.getReplacementEffects().size(), base);
    }

    // =====================================================================================
    // Card states: split / adventure / MDFC / saga / planeswalker / battle (flags in replacementFlags)
    // =====================================================================================

    @Test
    public void multiStateCards_cacheMatchesGroundTruthInEveryState() throws Exception {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        String[] names = {"Fire // Ice", "Bonecrusher Giant", "Agadeem's Awakening", "The Eldest Reborn",
                "Jace Beleren", "Invasion of Zendikar", "Grizzly Bears"};
        for (String name : names) {
            for (ZoneType z : new ZoneType[]{ZoneType.Battlefield, ZoneType.Hand, ZoneType.Graveyard}) {
                Card c = addCardToZone(name, p, z);
                assertCoherent(c, name + " in " + z);
            }
        }
    }

    @Test
    public void copyFrom_bumpsVersionsSoCopiedTraitsAreSeen() throws Exception {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", p);
        Card lord = addCard("Lord of Atlantis", p);
        Card cove = addCard("Tranquil Cove", p);
        Card angel = addCard("Serra Angel", p);
        CardState st = bears.getState(CardStateName.Original);

        Assert.assertEquals(st.getStaticAbilities().size(), 0, "premise");
        Assert.assertEquals(st.getReplacementEffects(true).size(), 0, "premise");

        st.copyFrom(lord.getState(CardStateName.Original), false);
        Assert.assertEquals(st.getStaticAbilities().size(), lord.getState(CardStateName.Original).getStaticAbilities().size());
        Assert.assertTrue(st.getStaticAbilities().size() > 0, "statics copied");
        assertCoherent(st, "copied Lord");

        st.copyFrom(cove.getState(CardStateName.Original), false);
        Assert.assertTrue(st.getReplacementEffects(true).size() > 0, "replacement copied");
        Assert.assertEquals(st.getStaticAbilities().size(), 0, "statics replaced by the new source");
        assertCoherent(st, "copied Cove");

        st.copyFrom(angel.getState(CardStateName.Original), false);
        Assert.assertEquals(st.getReplacementEffects(true).size(), 0);
        assertCoherent(st, "copied Angel");
    }

    @Test
    public void directAddAndRemove_ofStaticAndReplacement() throws Exception {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", p);
        CardState st = bears.getCurrentState();

        int s0 = st.getStaticAbilities().size();
        StaticAbility sa = newStatic(bears, st);
        st.addStaticAbility(sa);
        Assert.assertEquals(st.getStaticAbilities().size(), s0 + 1);
        assertCoherent(st, "static added");
        st.removeStaticAbility(sa);
        Assert.assertEquals(st.getStaticAbilities().size(), s0);
        assertCoherent(st, "static removed");

        int r0 = st.getReplacementEffects(true).size();
        ReplacementEffect re = newReplacement(bears, st);
        st.addReplacementEffect(re);
        Assert.assertEquals(st.getReplacementEffects(true).size(), r0 + 1);
        assertCoherent(st, "replacement added");
    }

    /**
     * The patch bumps a version counter inside add copyFrom. If CardState has ANY other mutator of the two lists
     * (remove*, clear*, set*...) that was not patched, the cache goes stale. This test finds them by reflection.
     */
    @Test
    public void everyOtherMutatorOfTheCachedListsInvalidatesTheCache() throws Exception {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", p);
        CardState st = bears.getCurrentState();
        List<String> needManualReview = new ArrayList<>();

        for (Method m : CardState.class.getDeclaredMethods()) {
            String n = m.getName();
            boolean mutator = n.matches("(remove|clear|set|reset|replace).*(StaticAbilit|ReplacementEffect).*");
            if (!mutator || m.isSynthetic()) continue;
            m.setAccessible(true);

            StaticAbility sa = newStatic(bears, st);
            ReplacementEffect re = newReplacement(bears, st);
            st.addStaticAbility(sa);
            st.addReplacementEffect(re);
            assertCoherent(st, "primed before " + n);          // fills the caches

            Class<?>[] types = m.getParameterTypes();
            Object[] args = new Object[types.length];
            boolean drivable = true;
            for (int i = 0; i < types.length; i++) {
                if (types[i].isAssignableFrom(StaticAbility.class) && types[i] != Object.class) args[i] = sa;
                else if (types[i].isAssignableFrom(ReplacementEffect.class) && types[i] != Object.class) args[i] = re;
                else drivable = false;
            }
            if (!drivable) { needManualReview.add(m.toString()); continue; }
            m.invoke(st, args);
            assertCoherent(st, "after CardState." + n);
        }
        Assert.assertTrue(needManualReview.isEmpty(),
                "These mutators could not be driven automatically - check by hand that they bump staticAbilitiesVersion/replacementEffectsVersion: " + needManualReview);
    }

    @Test
    public void mutatingAReturnedCachedListMustNotPoisonTheCache() throws Exception {
        // Before the patch every call returned a fresh copy, so callers were free to mutate the result.
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Card lord = addCard("Lord of Atlantis", p);
        CardState st = lord.getCurrentState();
        int expected = st.getStaticAbilities().size();
        Assert.assertTrue(expected > 0, "premise");
        try {
            st.getStaticAbilities().clear();
        } catch (UnsupportedOperationException ok) {
            // fine: returning an unmodifiable view is also a valid fix
        }
        Assert.assertEquals(st.getStaticAbilities().size(), expected, "a caller clearing the returned list poisoned the shared cache");
        assertCoherent(st, "after caller mutation");
    }

    // =====================================================================================
    // Randomized rules fuzz: after EVERY step all caches must equal the uncached ground truth
    // =====================================================================================

    @Test
    public void randomizedFuzz_cachesStayCoherent() {
        final String[] pool = {"Grizzly Bears", "Serra Angel", "Lord of Atlantis", "Arcbound Worker", "Humility", "Blood Moon",
                "Urborg, Tomb of Yawgmoth", "Rest in Peace", "Authority of the Consuls", "Doubling Season", "Forest",
                "Command Tower", "Kalonian Hydra", "Jace Beleren", "Bonecrusher Giant", "Fire // Ice"};
        final CounterEnumType[] counters = {CounterEnumType.SHIELD, CounterEnumType.STUN, CounterEnumType.FINALITY};

        for (long seed : new long[]{1, 2, 3, 4}) {
            Game game = initAndCreateGame();
            Player[] ps = {game.getPlayers().get(0), game.getPlayers().get(1)};
            Random rnd = new Random(seed);
            StringBuilder trail = new StringBuilder();
            try {
                for (int step = 0; step < 150; step++) {
                    Player p = ps[rnd.nextInt(2)];
                    CardCollection bf = new CardCollection(game.getCardsIn(ZoneType.Battlefield));
                    Card pick = bf.isEmpty() ? null : bf.get(rnd.nextInt(bf.size()));
                    int action = rnd.nextInt(8);
                    if (pick == null) action = 0;
                    switch (action) {
                        case 0: case 1: case 2: {
                            String name = pool[rnd.nextInt(pool.length)];
                            trail.append("add ").append(name).append(" to ").append(p).append("; ");
                            addCard(name, p);
                            break;
                        }
                        case 3:
                            trail.append("to graveyard ").append(pick).append("; ");
                            move(game, pick, ZoneType.Graveyard);
                            break;
                        case 4:
                            ZoneType z = rnd.nextBoolean() ? ZoneType.Exile : ZoneType.Hand;
                            trail.append("to ").append(z).append(' ').append(pick).append("; ");
                            move(game, pick, z);
                            break;
                        case 5: {
                            CounterEnumType t = counters[rnd.nextInt(counters.length)];
                            trail.append("counter ").append(t).append(" on ").append(pick).append("; ");
                            pick.addCounterInternal(t, 1, p, false, null, null);        // ADAPT
                            break;
                        }
                        case 6:
                            if (pick.isCreature()) {
                                trail.append("keyword toggle on ").append(pick).append("; ");
                                if (pick.hasKeyword("Modular:2")) pick.getCurrentState().removeIntrinsicKeyword("Modular:2");
                                else pick.addIntrinsicKeyword("Modular:2");
                            }
                            break;
                        default:
                            trail.append("settle only; ");
                    }
                    settle(game);
                    assertCoherentAll(game, "seed=" + seed + " step=" + step);
                }
            } catch (Throwable t) {
                throw new AssertionError("seed=" + seed + " trail=" + trail, t);
            }
        }
    }

    // =====================================================================================
    // Combat.isRealCombat: view updates only for the real combat
    // =====================================================================================

    @Test
    public void combat_simulationCopyDoesNotTouchViews_realCombatDoes() {
        Game game = initAndCreateGame();
        Player p0 = game.getPlayers().get(0);
        Player p1 = game.getPlayers().get(1);
        Card attacker = addCard("Grizzly Bears", p0);
        Card blocker = addCard("Grizzly Bears", p1);

        Combat sim = new Combat(p0);                       // not installed in the game = AI simulation / copy
        sim.initConstraints();
        sim.addAttacker(attacker, p1);
        Assert.assertTrue(sim.isAttacking(attacker), "model is updated");
        Assert.assertFalse(attacker.getView().isAttacking(), "view must NOT be touched by a non-real combat");

        Combat real = new Combat(p0);
        real.initConstraints();
        installCombat(game, real);
        real.addAttacker(attacker, p1);
        Assert.assertTrue(attacker.getView().isAttacking(), "real combat updates the view");
        real.addBlocker(attacker, blocker);
        Assert.assertTrue(blocker.getView().isBlocking());
        real.removeBlockAssignment(attacker, blocker);
        Assert.assertFalse(blocker.getView().isBlocking(), "removeBlockAssignment");
        real.addBlocker(attacker, blocker);
        real.undoBlockingAssignment(blocker);
        Assert.assertFalse(blocker.getView().isBlocking(), "undoBlockingAssignment");
        real.removeFromCombat(attacker);
        Assert.assertFalse(attacker.getView().isAttacking(), "removeFromCombat");
    }

    @Test
    public void combat_removalAfterCombatWasUninstalled_mustNotLeaveStaleAttackingFlag() {
        // Regression risk of isRealCombat(): if a card is removed from combat AFTER game.getCombat() was reset,
        // the view flag is never cleared. Fails => end-of-combat cleanup needs to refresh views explicitly.
        Game game = initAndCreateGame();
        Player p0 = game.getPlayers().get(0);
        Player p1 = game.getPlayers().get(1);
        Card attacker = addCard("Grizzly Bears", p0);
        Combat real = new Combat(p0);
        real.initConstraints();
        installCombat(game, real);
        real.addAttacker(attacker, p1);
        Assert.assertTrue(attacker.getView().isAttacking());
        installCombat(game, null);
        real.removeFromCombat(attacker);
        Assert.assertFalse(attacker.getView().isAttacking(), "stale 'attacking' view flag after combat ended");
    }

    // =====================================================================================
    // Card collections: Game / PlayerCollection rewrites must equal the straightforward definition
    // =====================================================================================

    private void populate(Game game) {
        for (Player p : game.getPlayers()) {
            addCardToZone("Grizzly Bears", p, ZoneType.Hand);
            addCardToZone("Forest", p, ZoneType.Hand);
            addCardToZone("Serra Angel", p, ZoneType.Library);
            addCardToZone("Forest", p, ZoneType.Library);
            addCardToZone("Lord of Atlantis", p, ZoneType.Graveyard);
            addCardToZone("Tranquil Cove", p, ZoneType.Exile);
            addCardToZone("Grizzly Bears", p, ZoneType.Battlefield);
            addCardToZone("Command Tower", p, ZoneType.Battlefield);
        }
    }

    private static List<Integer> ids(Iterable<Card> cards) {
        List<Integer> out = new ArrayList<>();
        for (Card c : cards) out.add(c.getId());
        return out;
    }

    @Test
    public void getCardsInGame_sameCardsAndOrderAsForEachCardInGame() {
        Game game = initAndCreateGame();
        populate(game);
        final List<Card> visited = new ArrayList<>();
        game.forEachCardInGame(new Visitor<Card>() {
            @Override public boolean visit(Card c) { visited.add(c); return true; }
        });
        Assert.assertEquals(ids(game.getCardsInGame()), ids(visited), "content AND order of getCardsInGame");
    }

    @Test
    public void getCardsInGame_withPhasedOutCreature() {
        Game game = initAndCreateGame();
        populate(game);
        Card bears = addCard("Grizzly Bears", game.getPlayers().get(0));
        bears.phase(false);                                                              // ADAPT: phase-out API
        final List<Card> visited = new ArrayList<>();
        game.forEachCardInGame(new Visitor<Card>() {
            @Override public boolean visit(Card c) { visited.add(c); return true; }
        });
        Assert.assertEquals(ids(game.getCardsInGame()), ids(visited), "phased-out cards must be treated exactly like forEachCardInGame does");
    }

    @Test
    public void getCardsInOwnedBy_stillIncludesPhasedOutPermanents() {
        // The old implementation went through getCardsIncludePhasingIn(zone); the patch uses player.getCardsIn(zone).
        Game game = initAndCreateGame();
        populate(game);
        Player p = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", p);
        bears.phase(false);                                                              // ADAPT
        List<ZoneType> zones = Arrays.asList(ZoneType.Battlefield, ZoneType.Hand, ZoneType.Graveyard, ZoneType.Exile);

        CardCollection expected = new CardCollection();
        for (ZoneType z : zones) expected.addAll(game.getCardsIncludePhasingIn(z));
        List<Integer> expectedIds = ids(CardLists.filter(expected, CardPredicates.isOwner(p)));

        Assert.assertEquals(ids(game.getCardsInOwnedBy(zones, p)), expectedIds,
                "getCardsInOwnedBy must keep the phasing semantics of the original implementation");
    }

    @Test
    public void getCardsIn_variantsMatchTheDefinition() {
        Game game = initAndCreateGame();
        populate(game);
        PlayerCollection players = game.getPlayers();

        for (ZoneType z : new ZoneType[]{ZoneType.Hand, ZoneType.Library, ZoneType.Graveyard, ZoneType.Exile, ZoneType.Battlefield, ZoneType.Stack}) {
            List<Integer> expected = new ArrayList<>();
            if (z == ZoneType.Stack) {
                expected = ids(game.getStackZone().getCards());
            } else {
                for (Player p : players) expected.addAll(ids(p.getCardsIn(z, false)));
            }
            Assert.assertEquals(ids(game.getCardsIn(z)), expected, "Game.getCardsIn(" + z + ")");
            if (z != ZoneType.Stack) {
                List<Integer> viaPlayers = ids(players.getCardsIn(z));
                List<Integer> manual = new ArrayList<>();
                for (Player p : players) manual.addAll(ids(p.getCardsIn(z)));
                Assert.assertEquals(viaPlayers, manual, "PlayerCollection.getCardsIn(" + z + ")");
            }
        }

        List<ZoneType> listZones = Arrays.asList(ZoneType.Hand, ZoneType.Battlefield, ZoneType.Graveyard);
        List<Integer> expList = new ArrayList<>();
        for (ZoneType z : listZones) for (Player p : players) expList.addAll(ids(p.getCardsIn(z)));
        Assert.assertEquals(ids(game.getCardsIn(listZones)), expList, "Game.getCardsIn(List) keeps the caller's zone order");

        EnumSet<ZoneType> set = EnumSet.of(ZoneType.Graveyard, ZoneType.Hand, ZoneType.Battlefield);
        List<Integer> expSet = new ArrayList<>();
        for (ZoneType z : ZoneType.values()) if (set.contains(z)) for (Player p : players) expSet.addAll(ids(p.getCardsIn(z)));
        Assert.assertEquals(ids(game.getCardsIn(set)), expSet, "Game.getCardsIn(EnumSet)");

        Assert.assertEquals(game.getCardsIn(EnumSet.noneOf(ZoneType.class)).size(), 0);
        Assert.assertEquals(ids(players.getCreaturesInPlay()), idsOfCreatures(players), "PlayerCollection.getCreaturesInPlay");

        byte green = forge.card.MagicColor.GREEN;
        Assert.assertEquals(ids(game.getColoredCardsInPlay(green)),
                ids(CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.isColor(green))), "getColoredCardsInPlay");
    }

    private static List<Integer> idsOfCreatures(PlayerCollection players) {
        List<Integer> out = new ArrayList<>();
        for (Player p : players) out.addAll(ids(p.getCreaturesInPlay()));
        return out;
    }

    @Test
    public void copyLastState_hasSameCardsAsTheZone() {
        Game game = initAndCreateGame();
        populate(game);
        CardCollectionView lki = game.copyLastStateBattlefield();
        CardCollectionView real = game.getCardsIn(ZoneType.Battlefield);
        Assert.assertEquals(ids(lki), ids(real), "LKI copies keep the ids/order of the real battlefield");
    }

    // =====================================================================================
    // Card.isValid (ParsedRestriction cache) against the original split-based logic
    // =====================================================================================

    private static boolean legacyIsValid(Card c, String restriction, Player ctrl, Card src) {
        final String[] incR = restriction.split("\\.", 2);
        boolean testFailed = false;
        if (incR[0].startsWith("!")) { testFailed = true; incR[0] = incR[0].substring(1); }
        if (c.getCurrentStateName() == CardStateName.PreparedSpell && c.isInZone(ZoneType.Exile)) return testFailed;
        if (incR[0].equals("Spell")) { if (!c.isSpell()) return testFailed; }
        else if (incR[0].equals("Permanent")) { if (!c.isPermanent()) return testFailed; }
        else if (incR[0].equals("Effect")) { if (!c.isImmutable()) return testFailed; }
        else if (incR[0].equals("Emblem")) { if (!c.isEmblem()) return testFailed; }
        else if (incR[0].equals("Boon")) { if (!c.isBoon()) return testFailed; }
        else if (incR[0].equals("card") || incR[0].equals("Card")) { if (c.isImmutable()) return testFailed; }
        else if (incR[0].equals("Any")) { if (!(c.isCreature() || c.isPlaneswalker() || c.isBattle())) return false; }
        else if (!c.getType().hasStringType(incR[0])) { return testFailed; }
        if (incR.length > 1) {
            for (String exR : incR[1].split("\\+")) {
                if (!c.hasProperty(exR, ctrl, src, null)) return testFailed;
            }
        }
        return !testFailed;
    }

    @Test
    public void isValid_parsedRestrictionCacheEqualsLegacyParsing() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Card[] subjects = {addCard("Grizzly Bears", p), addCard("Serra Angel", p), addCard("Forest", p), addCard("Jace Beleren", p),
                addCardToZone("Grizzly Bears", p, ZoneType.Hand)};
        String[] restrictions = {"Creature", "!Creature", "Creature.Green", "Creature.nonGreen", "Creature.Green+withFlying",
                "Creature.withFlying", "Creature.YouCtrl", "Land", "!Land.Basic", "Land.Basic+Forest", "Permanent", "Permanent.YouCtrl",
                "Spell", "!Spell", "Effect", "Emblem", "Boon", "Card", "card", "Card.Self", "Any", "Any.YouCtrl", "!Any",
                "Planeswalker", "Artifact.Creature", "Creature.", "!Creature.White+withFlying"};
        for (int round = 0; round < 2; round++) {            // round 2 = cache hits
            for (Card subject : subjects) {
                for (String r : restrictions) {
                    Assert.assertEquals(subject.isValid(r, p, subject, null), legacyIsValid(subject, r, p, subject),
                            "round " + round + " restriction '" + r + "' on " + subject);
                }
            }
        }
        // overflow the 8192-entry cache: results must stay correct after it was cleared
        Card bears = subjects[0];
        for (int i = 0; i < 9000; i++) {
            String r = "NotAType" + i + ".Green";
            Assert.assertEquals(bears.isValid(r, p, bears, null), legacyIsValid(bears, r, p, bears), r);
        }
        for (String r : restrictions) {
            Assert.assertEquals(bears.isValid(r, p, bears, null), legacyIsValid(bears, r, p, bears), "after cache clear: " + r);
        }
    }

    // =====================================================================================
    // hashCode rewrites must be bit-identical to Objects.hash(Class, id)
    // =====================================================================================

    @Test
    public void hashCodes_identicalToObjectsHash() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Card cove = addCard("Tranquil Cove", p);
        ReplacementEffect re = cove.getReplacementEffects().get(0);
        Assert.assertEquals(re.hashCode(), Objects.hash(ReplacementEffect.class, re.getId()));

        Card bears = addCardToZone("Grizzly Bears", p, ZoneType.Hand);
        SpellAbility sa = bears.getSpellAbilities().get(0);
        Assert.assertEquals(sa.hashCode(), Objects.hash(SpellAbility.class, sa.getId()));
    }
}
