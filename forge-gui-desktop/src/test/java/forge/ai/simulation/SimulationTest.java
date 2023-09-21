package forge.ai.simulation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

import forge.ai.AIOption;
import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.ai.simulation.GameStateEvaluator.Score;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactory;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.ZoneType;
import forge.item.PaperToken;
import forge.model.FModel;

public class SimulationTest extends AITest {

    public Game resetGame() {
        // need to be done after FModel.initialize, or the Localizer isn't loaded yet
        List<RegisteredPlayer> players = Lists.newArrayList();
        Deck d1 = new Deck();
        players.add(new RegisteredPlayer(d1).setPlayer(new LobbyPlayerAi("p2", null)));
        Set<AIOption> options = new HashSet<>();
        options.add(AIOption.USE_SIMULATION);
        players.add(new RegisteredPlayer(d1).setPlayer(new LobbyPlayerAi("p1", options)));
        GameRules rules = new GameRules(GameType.Constructed);
        Match match = new Match(rules, players, "Test");
        Game game = new Game(players, rules, match);
        game.setAge(GameStage.Play);

        return game;
    }

    protected GameSimulator createSimulator(Game game, Player p) {
        return new GameSimulator(new SimulationController(new Score(0)) {
            @Override
            public boolean shouldRecurse() {
                return false;
            }
        }, game, p, null);
    }

    protected String gameStateToString(Game game) {
        StringBuilder sb = new StringBuilder();
        for (ZoneType zone : ZoneType.values()) {
            CardCollectionView cards = game.getCardsIn(zone);
            if (!cards.isEmpty()) {
                sb.append("Zone ").append(zone.name()).append(":\n");
                for (Card c : game.getCardsIn(zone)) {
                    sb.append("  ").append(c).append("\n");
                }
            }
        }
        return sb.toString();
    }

    protected Card createToken(String name, Player p) {
        PaperToken token = FModel.getMagicDb().getAllTokens().getToken(name);
        if (token == null) {
            System.out.println("Failed to find token name " + name);
            return null;
        }
        return CardFactory.getCard(token, p, p.getGame());
    }

    protected List<Card> addTokens(String name, int amount, Player p) {
        List<Card> cards = new ArrayList<>();

        for(int i = 0; i < amount; i++) {
            cards.add(addToken(name, p));
        }

        return cards;
    }

    protected Card addToken(String name, Player p) {
        Card c = createToken(name, p);
        // card need a new Timestamp otherwise Static Abilities might collide
        c.setTimestamp(p.getGame().getNextTimestamp());
        p.getZone(ZoneType.Battlefield).add(c);
        return c;
    }
}
