package forge.gamesimulationtests.util;

import forge.ai.simulation.GameSimulationTest;
import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardState;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.staticability.StaticAbility;
import forge.game.zone.ZoneType;
import forge.gui.card.CardDetailUtil;
import forge.util.Visitor;
import forge.util.collect.FCollection;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Game-level tests for the follow-up fixes on top of PR #12297:
 *  - CardState caches must notice list changes even when a mutator forgets the manual version counter,
 *  - card collections built with FCollection.addAllUnique keep the uniqueness / membership invariants,
 *  - CardDetailUtil.composeCardText must be thread safe and keep the *...* markup of attachment lines.
 *
 * Engine calls are the same ones PerfPatchRulesTest uses (lines marked ADAPT there need the same adjustments here).
 */
public class PerfPatchFollowUpRulesTest extends GameSimulationTest {

    // ------------------------------------------------------------------ plumbing (same as PerfPatchRulesTest)

    private static List<String> sig(Iterable<? extends CardTraitBase> traits) {
        List<String> out = new ArrayList<>();
        for (CardTraitBase t : traits) {
            out.add(t.getClass().getSimpleName() + "#" + (t.getHostCard() == null ? -1 : t.getHostCard().getId())
                    + new TreeMap<>(t.getMapParams()));
        }
        return out;
    }

    private static Method privateMethod(String name, Class<?>... params) {
        try {
            Method m = CardState.class.getDeclaredMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static void assertCoherent(CardState s, String ctx) throws Exception {
        Method buildStatic = privateMethod("buildStaticAbilities");
        Method buildRepl = privateMethod("buildReplacementEffects", boolean.class);
        if (buildStatic != null) {
            Assert.assertEquals(sig(s.getStaticAbilities()), sig((Iterable<? extends CardTraitBase>) buildStatic.invoke(s)),
                    "STALE static-ability cache " + ctx);
        }
        for (boolean rulesHost : new boolean[]{true, false}) {
            if (buildRepl != null) {
                Assert.assertEquals(sig(s.getReplacementEffects(rulesHost)),
                        sig((Iterable<? extends CardTraitBase>) buildRepl.invoke(s, rulesHost)),
                        "STALE replacement-effect cache (rulesHost=" + rulesHost + ") " + ctx);
            }
        }
    }

    private static StaticAbility newStatic(Card host, CardState st) {
        return StaticAbility.create("Mode$ Continuous | Affected$ Creature.YouCtrl | AddPower$ 1 | Description$ perf test", host, st, true);   // ADAPT
    }

    private static ReplacementEffect newReplacement(Card host, CardState st) {
        return ReplacementHandler.parseReplacement("Event$ DamageDone | Prevent$ True | ValidTarget$ Card.Self | Description$ perf test", host, true, st); // ADAPT
    }

    private static List<Integer> ids(Iterable<Card> cards) {
        List<Integer> out = new ArrayList<>();
        for (Card c : cards) out.add(c.getId());
        return out;
    }

    // ------------------------------------------------------------------ CardState caches

    @Test
    @SuppressWarnings("unchecked")
    public void cardStateCaches_noticeListChangesThatBypassTheManualVersionCounters() throws Exception {
        // PR #12297 relied on every mutator doing "staticAbilitiesVersion++" by hand. The caches now also compare
        // FCollection.version(), so a mutation straight on the list (a mutator somebody forgot) is still seen.
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", p);
        CardState st = bears.getCurrentState();

        Field staticField = CardState.class.getDeclaredField("staticAbilities");
        Field replacementField = CardState.class.getDeclaredField("replacementEffects");
        staticField.setAccessible(true);
        replacementField.setAccessible(true);
        FCollection<StaticAbility> rawStatics = (FCollection<StaticAbility>) staticField.get(st);
        FCollection<ReplacementEffect> rawReplacements = (FCollection<ReplacementEffect>) replacementField.get(st);

        int statics0 = st.getStaticAbilities().size();                        // fills the caches
        int replacements0 = st.getReplacementEffects(true).size();
        st.getReplacementEffects(false);
        assertCoherent(st, "primed");

        StaticAbility sa = newStatic(bears, st);
        rawStatics.add(sa);                                                   // NOT addStaticAbility(): no counter bump
        Assert.assertEquals(st.getStaticAbilities().size(), statics0 + 1, "static ability added behind the counter's back");
        assertCoherent(st, "after raw static add");
        rawStatics.remove(sa);
        Assert.assertEquals(st.getStaticAbilities().size(), statics0, "static ability removed behind the counter's back");
        assertCoherent(st, "after raw static remove");

        ReplacementEffect re = newReplacement(bears, st);
        rawReplacements.add(re);
        Assert.assertEquals(st.getReplacementEffects(true).size(), replacements0 + 1, "replacement added behind the counter's back");
        assertCoherent(st, "after raw replacement add");
        rawReplacements.remove(re);
        Assert.assertEquals(st.getReplacementEffects(true).size(), replacements0, "replacement removed behind the counter's back");
        assertCoherent(st, "after raw replacement remove");
    }

    @Test
    public void cardStateCaches_stillHitWhenNothingChanged() throws Exception {
        // the stamp must be stable across reads, otherwise the cache would never be reused
        Game game = initAndCreateGame();
        Card lord = addCard("Lord of Atlantis", game.getPlayers().get(0));
        CardState st = lord.getCurrentState();
        List<String> first = sig(st.getStaticAbilities());
        for (int i = 0; i < 50; i++) {
            Assert.assertEquals(sig(st.getStaticAbilities()), first, "read #" + i);
        }
        assertCoherent(st, "repeated reads");
    }

    // ------------------------------------------------------------------ collections built without duplicate checks

    /** Enough cards (> 64 in play and in the game) that the lazily built membership table is really exercised. */
    private void populateHeavily(Game game) {
        for (Player p : game.getPlayers()) {
            for (int i = 0; i < 12; i++) {
                addCardToZone("Grizzly Bears", p, ZoneType.Hand);
                addCardToZone("Forest", p, ZoneType.Library);
                addCardToZone("Serra Angel", p, ZoneType.Graveyard);
                addCardToZone("Tranquil Cove", p, ZoneType.Exile);
                addCardToZone(i % 2 == 0 ? "Grizzly Bears" : "Forest", p, ZoneType.Battlefield);
            }
        }
    }

    private static void assertUniqueAndMembershipCorrect(Collection<Card> result, Card outsider, String ctx) {
        Set<Integer> seen = new HashSet<>();
        for (Card c : result) {
            Assert.assertTrue(seen.add(c.getId()), ctx + ": duplicate card " + c);
        }
        if (result instanceof FCollection) {
            @SuppressWarnings("unchecked")
            FCollection<Card> fc = (FCollection<Card>) result;
            for (Card c : new ArrayList<>(result)) {
                Assert.assertTrue(fc.contains(c), ctx + ": contains(" + c + ") (table built after the unchecked append)");
                Assert.assertFalse(fc.add(c), ctx + ": add(" + c + ") must be a duplicate");
            }
            if (outsider != null && !seen.contains(outsider.getId())) {
                Assert.assertFalse(fc.contains(outsider), ctx + ": contains(outsider)");
            }
        }
    }

    @Test
    public void cardCollectionsBuiltWithoutDuplicateChecks_keepTheUniquenessInvariant() {
        Game game = initAndCreateGame();
        populateHeavily(game);
        PlayerCollection players = game.getPlayers();
        Card outsider = addCardToZone("Command Tower", players.get(0), ZoneType.Hand);   // never part of the Battlefield results

        assertUniqueAndMembershipCorrect(game.getCardsInGame(), null, "getCardsInGame");
        for (ZoneType z : new ZoneType[]{ZoneType.Hand, ZoneType.Library, ZoneType.Graveyard, ZoneType.Exile, ZoneType.Battlefield}) {
            assertUniqueAndMembershipCorrect(game.getCardsIn(z), z == ZoneType.Battlefield ? outsider : null, "Game.getCardsIn(" + z + ")");
            assertUniqueAndMembershipCorrect(players.getCardsIn(z), z == ZoneType.Battlefield ? outsider : null, "PlayerCollection.getCardsIn(" + z + ")");
        }
        assertUniqueAndMembershipCorrect(game.getCardsIncludePhasingIn(ZoneType.Battlefield), outsider, "getCardsIncludePhasingIn");
        assertUniqueAndMembershipCorrect(game.getCardsIn(EnumSet.of(ZoneType.Hand, ZoneType.Battlefield, ZoneType.Graveyard)), null, "getCardsIn(EnumSet)");
        assertUniqueAndMembershipCorrect(players.getCreaturesInPlay(), outsider, "getCreaturesInPlay");
        assertUniqueAndMembershipCorrect(game.getColoredCardsInPlay(forge.card.MagicColor.GREEN), outsider, "getColoredCardsInPlay");
        assertUniqueAndMembershipCorrect(game.copyLastStateBattlefield(), null, "copyLastStateBattlefield");
    }

    @Test
    public void getCardsInGame_hasTheSameCardsInTheSameOrderAsForEachCardInGame() {
        // getCardsInGame lists the zones by hand; this keeps it from silently drifting away from forEachCardInGame
        Game game = initAndCreateGame();
        populateHeavily(game);
        final List<Card> visited = new ArrayList<>();
        game.forEachCardInGame(new Visitor<Card>() {
            @Override public boolean visit(Card c) { visited.add(c); return true; }
        });
        Assert.assertTrue(visited.size() > 64, "premise: enough cards to exercise the big-collection paths");
        Assert.assertEquals(ids(game.getCardsInGame()), ids(visited), "content AND order");
    }

    @Test
    public void getCardsIn_enumSetMatchesTheDefinition_inOneZoneOrderPass() {
        Game game = initAndCreateGame();
        populateHeavily(game);
        PlayerCollection players = game.getPlayers();
        Player p = players.get(0);
        Card phased = addCard("Grizzly Bears", p);
        phased.phase(false);                                                                  // ADAPT: phase-out API (as in PerfPatchRulesTest)

        EnumSet<ZoneType> zones = EnumSet.of(ZoneType.Graveyard, ZoneType.Hand, ZoneType.Battlefield, ZoneType.Exile);
        List<Integer> expected = new ArrayList<>();
        for (ZoneType z : ZoneType.values()) {
            if (zones.contains(z)) {
                for (Player pl : players) expected.addAll(ids(pl.getCardsIn(z)));
            }
        }
        Assert.assertEquals(ids(game.getCardsIn(zones)), expected, "ascending ZoneType order, players in turn order");
        Assert.assertEquals(game.getCardsIn(EnumSet.noneOf(ZoneType.class)).size(), 0);

        // a caller may still pass a list that repeats a zone: that path must keep de-duplicating
        List<ZoneType> repeated = new ArrayList<>();
        repeated.add(ZoneType.Hand);
        repeated.add(ZoneType.Hand);
        repeated.add(ZoneType.Graveyard);
        assertUniqueAndMembershipCorrect(game.getCardsIn(repeated), null, "getCardsIn(list with a repeated zone)");
    }

    @Test
    public void cardCollectionsFromGameQueries_stayMutableAndFilterable() {
        Game game = initAndCreateGame();
        populateHeavily(game);
        CardCollectionView battlefield = game.getCardsIn(ZoneType.Battlefield);
        CardCollection copy = new CardCollection(battlefield);
        int before = copy.size();
        Card first = copy.getFirst();
        Assert.assertTrue(copy.remove(first));
        Assert.assertFalse(copy.contains(first));
        Assert.assertTrue(copy.add(first), "re-adding the removed card");
        Assert.assertEquals(copy.size(), before);
        int owned = 0;
        for (Player p : game.getPlayers()) owned += CardLists.filter(battlefield, CardPredicates.isOwner(p)).size();
        Assert.assertEquals(owned, battlefield.size(), "every battlefield card is found exactly once when filtering by owner");
    }

    // ------------------------------------------------------------------ CardDetailUtil

    private static String composeText(Game game, Card card) {
        return CardDetailUtil.composeCardText(card.getView().getCurrentState(), game.getView(), true);
    }

    @Test
    public void composeCardText_isThreadSafe() throws Exception {
        final Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        final List<Card> cards = new ArrayList<>();
        for (String name : new String[]{"Grizzly Bears", "Serra Angel", "Lord of Atlantis", "Tranquil Cove", "Kalonian Hydra", "Jace Beleren"}) {
            cards.add(addCard(name, p));
        }
        final List<String> expected = new ArrayList<>();
        try {
            for (Card c : cards) expected.add(composeText(game, c));
        } catch (RuntimeException e) {
            throw new SkipException("composeCardText cannot run in this test environment (needs GUI/localization setup): " + e);
        }

        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (int t = 0; t < 8; t++) {
                futures.add(pool.submit(new Callable<String>() {
                    @Override public String call() {
                        for (int round = 0; round < 400; round++) {
                            for (int i = 0; i < cards.size(); i++) {
                                String got = composeText(game, cards.get(i));
                                if (!got.equals(expected.get(i))) {
                                    return "card " + cards.get(i) + " round " + round + ":\n" + got + "\n--- expected ---\n" + expected.get(i);
                                }
                            }
                        }
                        return null;
                    }
                }));
            }
            for (Future<String> f : futures) {
                Assert.assertEquals(f.get(60, TimeUnit.SECONDS), null, "text composed concurrently must equal the single-threaded text");
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void composeCardText_keepsTheMarkupAroundAttachmentLines() throws Exception {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", p);
        Card aura = addCard("Pacifism", p);
        Method attach = null;
        for (Method m : Card.class.getMethods()) {
            if (m.getName().equals("attachToEntity") && m.getParameterCount() == 2) attach = m;
        }
        if (attach == null) {
            throw new SkipException("Card.attachToEntity(entity, sa) not found in this Forge revision");
        }
        attach.invoke(aura, bears, null);                                                     // ADAPT if the signature differs
        String text;
        try {
            text = composeText(game, aura);
        } catch (RuntimeException e) {
            throw new SkipException("composeCardText cannot run in this test environment: " + e);
        }
        Assert.assertTrue(text.contains("\n*Attached to " + bears + "*"),
                "the '*...*' markers tell the card detail panel to render the line specially:\n" + text);
    }
}
