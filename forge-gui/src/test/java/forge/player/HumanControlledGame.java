package forge.player;

import com.google.common.collect.Lists;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.gui.interfaces.FakeGuiGame;
import forge.item.PaperCard;
import forge.util.Lang;
import forge.util.Localizer;

import java.util.List;

/**
 * A two-player game in which both players are human-controlled and the first player's UI is a
 * {@link FakeGuiGame}.
 * <p>
 * Built without {@code FModel}: no card database, preferences or asset directory are loaded. Cards
 * are created from inline script text, so each test states exactly the card characteristics it
 * relies on.
 */
final class HumanControlledGame {

    final Game game;
    final Player human;
    final Player opponent;
    final PlayerControllerHuman controller;
    final FakeGuiGame gui = new FakeGuiGame();

    HumanControlledGame() {
        // The engine reads localized strings and keyword descriptions from these singletons.
        Localizer.getInstance().initialize("en-US", "res/languages/");
        Lang.createInstance("en-US");

        final List<RegisteredPlayer> players = Lists.newArrayList(
                new RegisteredPlayer(new Deck()).setPlayer(lobbyPlayer("Human")),
                new RegisteredPlayer(new Deck()).setPlayer(lobbyPlayer("Opponent")));
        final GameRules rules = new GameRules(GameType.Constructed);
        game = new Game(players, rules, new Match(rules, players, "Test"));
        human = game.getPlayers().get(0);
        opponent = game.getPlayers().get(1);
        controller = (PlayerControllerHuman) human.getController();
        controller.setGui(gui.asGui());
    }

    /** Creates a card from script lines, e.g. {@code "Types:Creature Beast", "PT:5/5", "K:Trample"}. */
    Card card(final Player owner, final String name, final String... scriptLines) {
        final List<String> script = Lists.newArrayList("Name:" + name, "ManaCost:1");
        script.addAll(List.of(scriptLines));
        final PaperCard paperCard = new PaperCard(CardRules.fromScript(script), "", CardRarity.Common);
        return CardFactory.getCard(paperCard, owner, game);
    }

    // LobbyPlayerHuman.createIngamePlayer() personalizes the player name from FModel preferences,
    // which would pull in the whole model. Same controller wiring, without that lookup.
    private static LobbyPlayerHuman lobbyPlayer(final String name) {
        return new LobbyPlayerHuman(name) {
            @Override
            public Player createIngamePlayer(final Game game, final int id) {
                final Player player = new Player(getName(), game, id);
                player.setFirstController(new PlayerControllerHuman(game, player, this));
                return player;
            }
        };
    }
}
