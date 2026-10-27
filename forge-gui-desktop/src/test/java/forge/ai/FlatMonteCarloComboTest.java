package forge.ai;

import java.util.Arrays;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.ComboPlan;
import forge.ai.simulation.DeckPlan;
import forge.ai.simulation.OpeningHand;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * The combo plan: what the AI reads from its own deck (the key cards and the win cards), the choices that follow from
 * it (what to discard, what to put onto the battlefield with a cheating spell, what to put into the graveyard), the
 * mulligan rules, and sideboarding against the deck of the opponent.
 */
public class FlatMonteCarloComboTest extends AITest {

    private Player deal(final Game game, final Object... list) {
        final Player p = game.getPlayers().get(1);
        for (int i = 0; i < list.length; i += 2) {
            for (int n = 0; n < (Integer) list[i + 1]; n++) {
                addCardToZone((String) list[i], p, ZoneType.Library);
            }
        }
        return p;
    }

    private static PaperCard paper(final String name) {
        return FModel.getMagicDb().getCommonCards().getCard(name);
    }

    private static List<PaperCard> papers(final Object... list) {
        final List<PaperCard> out = new java.util.ArrayList<>();
        for (int i = 0; i < list.length; i += 2) {
            for (int n = 0; n < (Integer) list[i + 1]; n++) {
                out.add(paper((String) list[i]));
            }
        }
        return out;
    }

    @Test
    public void livingEndDeckIsReadAsMassReanimationWithCyclingPriority() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, "Living End", 4, "Violent Outburst", 4, "Demonic Dread", 4, "Shardless Agent", 4,
                "Street Wraith", 4, "Architects of Will", 4, "Curator of Mysteries", 4, "Waker of Waves", 4, "Fury", 3,
                "Grief", 3, "Swamp", 8, "Mountain", 4, "Forest", 4);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        AssertJUnit.assertEquals(DeckPlan.Archetype.COMBO, plan.archetype);
        AssertJUnit.assertTrue(plan.combos.isScanComplete());
        AssertJUnit.assertTrue(plan.combos.has(ComboPlan.Kind.MASS_REANIMATION));
        AssertJUnit.assertTrue(plan.combos.wantsCyclingPriority(p));
        AssertJUnit.assertTrue(plan.combos.isCascadeSafe());
    }

    @Test
    public void reanimatorDiscardsTheBigCreatureAndKeepsTheReanimationSpell() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, "Reanimate", 4, "Animate Dead", 4, "Exhume", 2, "Entomb", 4, "Griselbrand", 4,
                "Archon of Cruelty", 3, "Faithless Looting", 4, "Careful Study", 2, "Thoughtseize", 4, "Swamp", 12,
                "Polluted Delta", 4, "Cabal Therapy", 2, "Dark Ritual", 3);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        AssertJUnit.assertTrue(plan.combos.has(ComboPlan.Kind.REANIMATOR));
        final Card griselbrand = addCardToZone("Griselbrand", p, ZoneType.Hand);
        addCardToZone("Swamp", p, ZoneType.Hand);
        addCardToZone("Reanimate", p, ZoneType.Hand);
        final CardCollection hand = new CardCollection(p.getCardsIn(ZoneType.Hand));
        final CardCollection discards = plan.combos.preferredDiscards(p, hand, 1);
        AssertJUnit.assertNotNull(discards);
        AssertJUnit.assertEquals(griselbrand, discards.getFirst());
    }

    @Test
    public void cheatingSpellPutsTheWinCardIntoPlayNotALand() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, "Show and Tell", 4, "Sneak Attack", 3, "Through the Breach", 2,
                "Emrakul, the Aeons Torn", 3, "Griselbrand", 3, "Inferno Titan", 3, "Ponder", 4, "Brainstorm", 4,
                "Island", 12, "Mountain", 6, "Counterspell", 4, "Lightning Bolt", 4);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        AssertJUnit.assertTrue(plan.combos.has(ComboPlan.Kind.CHEAT_FATTY));
        final CardCollection options = new CardCollection();
        options.add(addCardToZone("Island", p, ZoneType.Hand));
        final Card emrakul = addCardToZone("Emrakul, the Aeons Torn", p, ZoneType.Hand);
        options.add(emrakul);
        options.add(addCardToZone("Mountain", p, ZoneType.Hand));
        AssertJUnit.assertEquals(emrakul, plan.combos.choose(p, ZoneType.Battlefield, Arrays.asList(ZoneType.Hand), options));
    }

    @Test
    public void entombTakesTheBestCreature() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, "Reanimate", 4, "Animate Dead", 4, "Exhume", 2, "Entomb", 4, "Griselbrand", 4,
                "Archon of Cruelty", 3, "Faithless Looting", 4, "Careful Study", 2, "Thoughtseize", 4, "Swamp", 12);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        final CardCollection options = new CardCollection();
        for (final Card c : p.getCardsIn(ZoneType.Library)) {
            if (c.getName().equals("Swamp") || c.getName().equals("Griselbrand")) {
                options.add(c);
            }
        }
        final Card chosen = plan.combos.choose(p, ZoneType.Graveyard, Arrays.asList(ZoneType.Library), options);
        AssertJUnit.assertNotNull(chosen);
        AssertJUnit.assertEquals("Griselbrand", chosen.getName());
    }

    @Test
    public void ordinaryDecksAreNotComboDecks() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, "Lightning Bolt", 4, "Goblin Guide", 4, "Monastery Swiftspear", 4, "Lava Spike", 4,
                "Chain Lightning", 4, "Rift Bolt", 4, "Mountain", 20, "Eidolon of the Great Revel", 4, "Searing Spear", 4);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        AssertJUnit.assertTrue(plan.combos.isScanComplete());
        AssertJUnit.assertTrue(plan.combos.isEmpty());
    }

    @Test
    public void mulliganNeverKeepsAHandWithoutLandsOrNearlyOnlyLands() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, "Mountain", 20, "Lightning Bolt", 20);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        final String[][] hands = {
                { "Lightning Bolt", "Lightning Bolt", "Lightning Bolt", "Lightning Bolt", "Lightning Bolt", "Lightning Bolt", "Lightning Bolt" },
                { "Mountain", "Lightning Bolt", "Lightning Bolt", "Lightning Bolt", "Lightning Bolt", "Lightning Bolt", "Lightning Bolt" },
                { "Mountain", "Mountain", "Mountain", "Mountain", "Mountain", "Mountain", "Lightning Bolt" },
        };
        for (final String[] names : hands) {
            for (final Card c : new CardCollection(p.getCardsIn(ZoneType.Hand))) {
                p.getZone(ZoneType.Hand).remove(c);
            }
            for (final String n : names) {
                addCardToZone(n, p, ZoneType.Hand);
            }
            AssertJUnit.assertFalse("kept " + Arrays.toString(names),
                    OpeningHand.keep(p, p.getCardsIn(ZoneType.Hand), 0, true, plan, 4));
        }
        for (final Card c : new CardCollection(p.getCardsIn(ZoneType.Hand))) {
            p.getZone(ZoneType.Hand).remove(c);
        }
        for (final String n : new String[] { "Mountain", "Mountain", "Mountain", "Lightning Bolt", "Lightning Bolt", "Lightning Bolt", "Lightning Bolt" }) {
            addCardToZone(n, p, ZoneType.Hand);
        }
        AssertJUnit.assertTrue(OpeningHand.keep(p, p.getCardsIn(ZoneType.Hand), 0, true, plan, 4));
    }

    @Test
    public void sideboardingBringsInAnswersToWhatTheOpponentPlays() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final List<PaperCard> main = papers("Lightning Bolt", 4, "Goblin Guide", 4, "Monastery Swiftspear", 4,
                "Chain Lightning", 4, "Searing Spear", 4, "Skullcrack", 4, "Rift Bolt", 4, "Mountain", 24);
        final List<PaperCard> side = papers("Ancient Grudge", 2, "Pyroblast", 3, "Anger of the Gods", 2);
        final List<PaperCard> artifacts = papers("Ensnaring Bridge", 4, "Smokestack", 2, "Mother of Runes", 4,
                "Wurmcoil Engine", 4, "Chalice of the Void", 4, "Sol Ring", 4, "Mishra's Bauble", 4, "Plains", 22);
        final SideboardAdvisor.Result vsArtifacts = SideboardAdvisor.advise(p, main, side, Arrays.asList(artifacts),
                Arrays.asList("Artifacts"), SideboardAdvisor.maxSwapsFor(main.size()));
        AssertJUnit.assertFalse(vsArtifacts.swaps.isEmpty());
        for (final SideboardAdvisor.Swap swap : vsArtifacts.swaps) {
            AssertJUnit.assertEquals("Ancient Grudge", swap.in.getName()); // Pyroblast only hits blue cards
            AssertJUnit.assertFalse("Mountain".equals(swap.out.getName())); // lands never come out
        }

        final List<PaperCard> creatures = papers("Llanowar Elves", 4, "Grizzly Bears", 4, "Goblin Guide", 4,
                "Delver of Secrets", 4, "Serra Angel", 4, "Mother of Runes", 4, "Plains", 24);
        final SideboardAdvisor.Result vsCreatures = SideboardAdvisor.advise(p, main, side, Arrays.asList(creatures),
                Arrays.asList("Creatures"), SideboardAdvisor.maxSwapsFor(main.size()));
        AssertJUnit.assertEquals("Anger of the Gods", vsCreatures.swaps.get(0).in.getName());
    }
}
