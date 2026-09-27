package forge.api;

import forge.card.CardRarity;
import forge.card.CardRules;
import forge.deck.Deck;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.CardView;
import forge.game.event.GameEventCardDestroyed;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableProperty;
import forge.util.Localizer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ConcurrentModificationException;
import java.util.List;

import static org.testng.Assert.*;

public class EngineApiTest {
    @BeforeClass
    public void initializeLanguage() {
        Localizer.getInstance().initialize("en-US", "../forge-gui/res/languages");
    }

    private static PaperCard printing(String name, String mana, String type) {
        CardRules rules = new CardRules.Reader().readCard(List.of("Name:" + name,
                "ManaCost:" + mana, "Types:" + type, "Oracle:Test rules text"));
        return new PaperCard(rules, "TST", CardRarity.Common);
    }

    @Test
    public void searchFiltersAndPaginatesDeterministically() {
        var catalog = new CardCatalog(List.of(printing("Red Spell", "R", "Sorcery"),
                printing("Blue Spell", "U", "Instant"), printing("Forest", "no cost", "Basic Land Forest")));
        var page = catalog.search(new CardCatalog.Query("spell", 8, 1, 0, 20));
        assertEquals(page.total(), 1);
        assertEquals(page.cards().get(0).name(), "Red Spell");
        assertEquals(catalog.search(new CardCatalog.Query("", null, null, 1, 1)).cards().get(0).name(), "Forest");
        assertTrue(catalog.search(new CardCatalog.Query("", null, null, Integer.MAX_VALUE, 20)).cards().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new CardCatalog.Query("", null, null, 0, 201));
    }

    @Test
    public void failedBatchAndStaleWritesCannotCorruptDeck() {
        var card = printing("Red Spell", "R", "Sorcery");
        var editor = new DeckEditor(new CardCatalog(List.of(card)), new Deck("Test"));
        String id = CardCatalog.id(card);
        assertThrows(IllegalArgumentException.class, () -> editor.apply(0, List.of(
                new DeckEditor.Edit("Main", id, 4), new DeckEditor.Edit("Main", "missing", 1))));
        assertEquals(editor.snapshot().revision(), 0L);
        assertTrue(editor.snapshot().entries().isEmpty());
        editor.apply(0, List.of(new DeckEditor.Edit("Main", id, 4)));
        assertThrows(ConcurrentModificationException.class,
                () -> editor.apply(0, List.of(new DeckEditor.Edit("Main", id, 2))));
        assertEquals(editor.toDeck().getMain().count(card), 4);
        editor.toDeck().getMain().clear();
        assertEquals(editor.toDeck().getMain().count(card), 4);
        assertTrue(editor.undo(1).entries().isEmpty());
        assertEquals(editor.redo(2).entries().get(0).quantity(), 4);
        editor.undo(3);
        editor.apply(4, List.of(new DeckEditor.Edit("Sideboard", id, 2)));
        assertFalse(editor.snapshot().canRedo());
    }

    @Test
    public void hiddenCardsExposeCountsWithoutIdentities() {
        var owner = new PlayerView(1, null);
        var opponent = new PlayerView(2, null);
        var secret = new CardView(42, null, "Secret card");
        secret.set(TrackableProperty.Controller, owner);
        secret.set(TrackableProperty.Zone, ZoneType.Hand);
        var hand = new TrackableCollection<CardView>();
        hand.add(secret);
        owner.set(TrackableProperty.Hand, hand);
        var projection = GameStateMapper.zone(owner, ZoneType.Hand, opponent);
        assertEquals(projection.count(), 1);
        assertTrue(projection.visibleCards().isEmpty());
        assertEquals(GameStateMapper.card(secret, owner).name(), "Secret card");
        secret.set(TrackableProperty.Zone, ZoneType.Library);
        assertNull(GameStateMapper.card(secret, owner));
        assertNull(GameStateMapper.card(secret, opponent));
        assertThrows(NullPointerException.class, () -> GameStateMapper.card(secret, null));
    }

    @Test
    public void faceDownCardsNeverExposeTheirPrintedFace() {
        var owner = new PlayerView(1, null);
        var opponent = new PlayerView(2, null);
        var card = new CardView(42, null, "Secret face");
        card.set(TrackableProperty.Controller, owner);
        card.set(TrackableProperty.Zone, ZoneType.Battlefield);
        card.set(TrackableProperty.Facedown, true);
        assertEquals(GameStateMapper.card(card, opponent).name(), "Face-down card");
        assertNull(GameStateMapper.card(card, opponent).id());
        assertNull(GameStateMapper.card(card, opponent).power());
        card.set(TrackableProperty.Zone, ZoneType.Exile);
        assertNull(GameStateMapper.card(card, opponent));
    }

    @Test
    public void choiceDetailsRequireTheViewersCurrentPermission() {
        var owner = new PlayerView(1, null);
        var opponent = new PlayerView(2, null);
        var card = new CardView(42, null, "Secret library card");
        card.set(TrackableProperty.Controller, owner);
        card.set(TrackableProperty.Zone, ZoneType.Library);
        assertEquals(MatchSession.choiceCard(card, owner).get("name"), "Face-down or hidden card");
        assertEquals(MatchSession.choiceCard(card, opponent).get("text"), "");
        assertNull(MatchSession.choiceCard(card, owner).get("power"));
        var allowed = new TrackableCollection<PlayerView>();
        allowed.add(owner);
        card.set(TrackableProperty.PlayerMayLook, allowed);
        assertEquals(MatchSession.choiceCard(card, owner).get("name"), "Secret library card");
        assertEquals(MatchSession.choiceCard(card, opponent).get("name"), "Face-down or hidden card");
        card.set(TrackableProperty.PlayerMayLook, null);
        assertEquals(MatchSession.choiceCard(card, owner).get("name"), "Face-down or hidden card");
        card.set(TrackableProperty.Zone, ZoneType.Hand);
        assertEquals(MatchSession.choiceCard(card, owner).get("name"), "Secret library card");
        assertEquals(MatchSession.choiceCard(card, opponent).get("name"), "Face-down or hidden card");
        card.set(TrackableProperty.Facedown, true);
        assertEquals(MatchSession.choiceCard(card, owner).get("name"), "Face-down or hidden card");
    }

    @Test
    public void observationDetachesFromRealGameEventBus() {
        var game = new Match(new GameRules(GameType.Constructed), List.of(), "API test").createGame();
        var observation = new GameObservation(game);
        game.fireEvent(new GameEventCardDestroyed());
        assertEquals(observation.revision(), 1L);
        observation.close();
        observation.close();
        game.fireEvent(new GameEventCardDestroyed());
        assertEquals(observation.revision(), 1L);
    }
}
