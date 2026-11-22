package forge.web.deck;

import forge.StaticData;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.io.DeckStorage;
import forge.game.GameType;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.FileUtil;
import forge.util.storage.IStorage;
import forge.util.storage.StorageImmediatelySerialized;
import forge.web.WebTestSupport;
import forge.web.protocol.ToBrowser;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeckEditorTest {
    private File dir;
    private final Map<GameType, IStorage<Deck>> made = new HashMap<>();
    private final DeckStore.Storages storages = format -> made.computeIfAbsent(DeckStore.family(format), f -> {
        final File folder = new File(dir, f.name());
        folder.mkdirs();
        return new StorageImmediatelySerialized<>(f.name(), new DeckStorage(folder, folder.getPath()));
    });

    @BeforeClass
    public void setUp() {
        WebTestSupport.initModel();
    }

    @BeforeMethod
    public void freshFolders() throws IOException {
        dir = Files.createTempDirectory("forge-editor").toFile();
        made.clear();
    }

    @AfterMethod(alwaysRun = true)
    public void deleteFolders() {
        FileUtil.deleteDirectory(dir);
    }

    private DeckEditor editor(final Deck deck, final GameType format) {
        return new DeckEditor(deck, false, false, new DeckEditor.Stored(storages.of(format)), LegalityScope.of(format, null), storages,
                false, (id, text, f) -> { });
    }

    private static PaperCard card(final String name) {
        return StaticData.instance().getCommonCards().getCard(name);
    }

    // Fails if editing a precon writes to the precon rather than a copy
    @Test
    public void readOnlySourceIsCopied() {
        final Deck precon = new Deck("Precon");
        precon.getMain().add(card("Forest"), 10);
        final DeckEditor e = new DeckEditor(precon, true, false, new DeckEditor.Stored(storages.of(GameType.Constructed)),
                LegalityScope.of(GameType.Constructed, null), storages, false, (id, text, f) -> { });
        Assert.assertEquals(e.copyOf(), "Precon");
        Assert.assertNull(e.add("Llanowar Elves", DeckSection.Main, 1));
        Assert.assertEquals(precon.getMain().countByName("Llanowar Elves"), 0);
        Assert.assertEquals(e.deck().getName(), "Precon (copy)");
        Assert.assertNull(e.copyOf());
        Assert.assertTrue(storages.of(GameType.Constructed).contains("Precon (copy)"));
    }

    // Fails if a new deck saves over a deck the player already has under the same name
    @Test
    public void newDeckDoesNotOverwriteAStoredNewDeck() {
        final Deck existing = new Deck(DeckEditor.newDeckName());
        existing.getMain().add(card("Swamp"), 30);
        storages.of(GameType.Constructed).add(existing);
        final DeckEditor e = editor(new Deck(DeckEditor.newDeckName()), GameType.Constructed);
        Assert.assertNull(e.add("Lightning Bolt", DeckSection.Main, 1));
        Assert.assertEquals(e.deck().getName(), DeckEditor.newDeckName() + " (2)");
        Assert.assertEquals(storages.of(GameType.Constructed).get(DeckEditor.newDeckName()).getMain().countByName("Swamp"), 30);
    }

    // Fails if a card's printings are not listed newest first or do not carry the style that the printing picker filters by, a card added in a chosen printing is added in another, or a printing of a different card is accepted for it
    @Test
    public void aCardIsAddedInTheChosenPrinting() {
        final DeckEditor e = editor(new Deck("Printings"), GameType.Constructed);
        final List<ToBrowser.Printing> printings = CardCatalog.printings("Llanowar Elves", null);
        for (int i = 1; i < printings.size(); i++) {
            Assert.assertTrue(printings.get(i - 1).year() >= printings.get(i).year(), "printing " + i + " is newer than the one before it");
        }
        Assert.assertTrue(printings.stream().anyMatch(p -> "cards".equals(p.style())), "no printing is in its set's main run");
        Assert.assertTrue(printings.stream().anyMatch(p -> p.style() != null && !"cards".equals(p.style())), "no printing has a style of its own");
        final String oldest = printings.get(printings.size() - 1).key();
        Assert.assertNull(e.addPrinting("Llanowar Elves", DeckSection.Main, oldest));
        final Map.Entry<PaperCard, Integer> held = e.deck().getMain().iterator().next();
        Assert.assertEquals(held.getKey().getImageKey(false), oldest);
        Assert.assertEquals(held.getValue().intValue(), 1);
        Assert.assertNotNull(e.addPrinting("Forest", DeckSection.Main, oldest), "a printing of another card was accepted");
        Assert.assertEquals(e.deck().getMain().countAll(), 1);
    }

    // Fails if a Limited deck adds a card its pool does not hold, or more copies than the pool holds, or a refused add changes the deck
    @Test
    public void aLimitedDeckAddsOnlyFromItsPool() {
        final Deck pool = new Deck("Sealed");
        pool.getOrCreate(DeckSection.Sideboard).add(card("Shock"), 1);
        final DeckEditor e = editor(pool, GameType.Sealed);
        Assert.assertNull(e.add("Shock", DeckSection.Main, 1));
        Assert.assertNotNull(e.add("Lightning Bolt", DeckSection.Main, 1), "a card from outside the pool was added");
        Assert.assertNotNull(e.add("Shock", DeckSection.Main, 1), "a second Shock was added from a pool of one");
        Assert.assertEquals(e.deck().getMain().countAll(), 1, "a refused add changed the main deck");
        Assert.assertEquals(e.deck().getMain().countByName("Shock"), 1);
        Assert.assertEquals(e.deck().get(DeckSection.Sideboard).countAll(), 0, "a refused add changed the pool");
    }

    // Fails if a rename onto another deck's file overwrites it
    @Test
    public void renameOntoTakenNameRefused() {
        final Deck other = new Deck("Zoo");
        other.getMain().add(card("Forest"), 1);
        storages.of(GameType.Constructed).add(other);
        final DeckEditor e = editor(new Deck("Burn"), GameType.Constructed);
        e.add("Lightning Bolt", DeckSection.Main, 1);
        Assert.assertNotNull(e.rename("zoo"));
        Assert.assertEquals(storages.of(GameType.Constructed).get("Zoo").getMain().countByName("Forest"), 1);
    }

    // Fails if a guest's change is saved to the host's storage and not sent to the guest's browser
    @Test
    public void guestReadOnlyCopyGoesToDevice() {
        final Deck host = new Deck("Host deck");
        host.getMain().add(card("Forest"), 10);
        final List<String> sent = new ArrayList<>();
        final DeckEditor e = new DeckEditor(host, true, false, new DeckEditor.Stored(storages.of(GameType.Constructed)),
                LegalityScope.of(GameType.Constructed, null), storages, true, (id, text, f) -> sent.add(text));
        Assert.assertNull(e.add("Llanowar Elves", DeckSection.Main, 1));
        Assert.assertTrue(e.target() instanceof DeckEditor.Device);
        Assert.assertEquals(sent.size(), 1);
        Assert.assertTrue(sent.get(0).contains("Llanowar Elves"));
        Assert.assertFalse(storages.of(GameType.Constructed).contains("Host deck (copy)"));
    }

    // Fails if a basic land outside the commander's colours is offered, or Wastes, which has no colour identity, is not
    @Test
    public void theLandRowFollowsTheCommandersColourIdentity() {
        final Deck deck = new Deck("Krenko");
        deck.getOrCreate(DeckSection.Commander).add(card("Krenko, Mob Boss"));
        final Map<String, Boolean> allowed = new HashMap<>();
        editor(deck, GameType.Commander).state().lands().forEach(l -> allowed.put(l.name(), l.allowed()));
        Assert.assertEquals(allowed.get("Mountain"), Boolean.TRUE);
        Assert.assertEquals(allowed.get("Plains"), Boolean.FALSE);
        Assert.assertEquals(allowed.get("Wastes"), Boolean.TRUE);
    }

    // Fails if a Commander Draft deck replaces a one-colour commander with another. Desktop's draft editor pairs them
    @Test
    public void aCommanderDraftPairsTwoOneColourLegends() {
        final Deck pool = new Deck("Draft");
        pool.getOrCreate(DeckSection.Sideboard).add(StaticData.instance().getCommonCards().getCard("Isamaru, Hound of Konda"), 1);
        pool.getOrCreate(DeckSection.Sideboard).add(StaticData.instance().getCommonCards().getCard("Krenko, Mob Boss"), 1);
        final DeckEditor e = editor(pool, GameType.CommanderDraft);
        Assert.assertNull(e.makeCommander("Isamaru, Hound of Konda", null));
        Assert.assertNull(e.makeCommander("Krenko, Mob Boss", null));
        Assert.assertEquals(e.deck().getCommanders().size(), 2);
    }

    // Fails if a planar deck takes a second copy of a plane or a phenomenon or a card that is no plane, is saved with the Constructed decks, stays illegal once it is full, or can be moved to another format
    @Test
    public void aPlanarDeckHoldsOneOfEachPlane() {
        final DeckEditor e = editor(new Deck("Planes"), GameType.Planechase);
        Assert.assertNull(e.add("Murasa", DeckSection.Planes, 1));
        Assert.assertNotNull(e.add("Murasa", DeckSection.Planes, 1), "a second copy of a plane was added");
        Assert.assertNull(e.add("Chaotic Aether", DeckSection.Planes, 1));
        Assert.assertNotNull(e.add("Chaotic Aether", DeckSection.Planes, 1), "a second copy of a phenomenon was added");
        Assert.assertNotNull(e.add("Lightning Bolt", DeckSection.Planes, 1), "a card that is no plane was added");
        Assert.assertNotNull(e.add("Naya", DeckSection.Main, 1), "a plane was added outside the planar deck");
        Assert.assertEquals(e.deck().get(DeckSection.Planes).countAll(), 2);
        Assert.assertNull(e.state().verdict(), "a planar deck was checked while decks may be illegal");
        FModel.getPreferences().setPref(FPref.ENFORCE_DECK_LEGALITY, true);
        try {
            Assert.assertNotNull(e.state().verdict(), "a planar deck of two cards is legal");
            Assert.assertNull(e.fillWithRandomPlanes());
            Assert.assertNull(e.state().verdict());
        } finally {
            FModel.getPreferences().setPref(FPref.ENFORCE_DECK_LEGALITY, false);
        }
        Assert.assertEquals(e.deck().get(DeckSection.Planes).toFlatList().stream().map(PaperCard::getName).distinct().count(), 10, "a plane was added twice");
        e.setCheck(LegalityScope.of(GameType.Constructed, null));
        Assert.assertEquals(e.check().format(), GameType.Planechase);
        Assert.assertTrue(storages.of(GameType.Planechase).contains("Planes"));
        Assert.assertFalse(storages.of(GameType.Constructed).contains("Planes"));
    }
}
