package forge;

import com.google.common.base.Function;
import forge.ai.AIOption;
import forge.ai.LobbyPlayerAi;
import forge.card.CardDb;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.generation.DeckGenerator5Color;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static forge.view.SimulateMatch.simulateSingleGameOfMatch;

public class GameFuzzingTest {
    @Ignore
    @Test
    public void PlayGameWithRandomDecks() {
        GuiBase.setInterface(new GuiDesktop());
        FModel.initialize(null, new Function<ForgePreferences, Void>() {
            @Override
            public Void apply(ForgePreferences preferences) {
                preferences.setPref(ForgePreferences.FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
                preferences.setPref(ForgePreferences.FPref.UI_LANGUAGE, "en-US");
                return null;
            }
        });

        // first deck
        CardDb cardDb = FModel.getMagicDb().getCommonCards();
        final DeckGenerator5Color gen = new DeckGenerator5Color(cardDb, DeckFormat.Constructed);
        final Deck first_deck = new Deck("first", gen.getDeck(60, false));
        final Deck second_deck = new Deck("second", gen.getDeck(60, false));

        final RegisteredPlayer p1 = new RegisteredPlayer(first_deck);
        final RegisteredPlayer p2 = new RegisteredPlayer(second_deck);

        Set<AIOption> options = new HashSet<>();
        // options.add(AIOption.USE_SIMULATION);
        p1.setPlayer(new LobbyPlayerAi("p1", options));
        p2.setPlayer(new LobbyPlayerAi("p2", options));
        GameRules rules = new GameRules(GameType.Constructed);
        // need game rules, players, and title
        Match m = new Match(rules, Arrays.asList(p1, p2), "test");

        simulateSingleGameOfMatch(m, 120);
    }
}
