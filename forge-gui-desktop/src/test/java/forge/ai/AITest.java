package forge.ai;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import forge.GuiDesktop;
import forge.StaticData;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactory;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.gui.GuiBase;
import forge.item.IPaperCard;
import forge.item.PaperToken;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

public class AITest {
    private static boolean initialized = false;

    public Game resetGame() {
        // need to be done after FModel.initialize, or the Localizer isn't loaded yet
        List<RegisteredPlayer> players = Lists.newArrayList();
        Deck d1 = new Deck();
        players.add(new RegisteredPlayer(d1).setPlayer(new LobbyPlayerAi("p2", null)));
        players.add(new RegisteredPlayer(d1).setPlayer(new LobbyPlayerAi("p1", null)));
        GameRules rules = new GameRules(GameType.Constructed);
        Match match = new Match(rules, players, "Test");
        Game game = new Game(players, rules, match);
        Player p = game.getPlayers().get(1);
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getPhaseHandler().onStackResolved();
        // game.getAction().checkStateEffects(true);

        return game;
    }

    protected Game initAndCreateGame() {
        if (!initialized) {
            GuiBase.setInterface(new GuiDesktop());
            FModel.initialize(null, preferences -> {
                preferences.setPref(FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
                preferences.setPref(FPref.UI_LANGUAGE, "en-US");
                return null;
            });
            initialized = true;
        }

        return resetGame();
    }

    protected int countCardsWithName(Game game, String name) {
        int i = 0;
        for (Card c : game.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals(name)) {
                i++;
            }
        }
        return i;
    }

    protected Card findCardWithName(Game game, String name) {
        for (Card c : game.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }


    protected SpellAbility findSAWithPrefix(Card c, String prefix) {
        return findSAWithPrefix(c.getSpellAbilities(), prefix);
    }

    protected SpellAbility findSAWithPrefix(Iterable<SpellAbility> abilities, String prefix) {
        for (SpellAbility sa : abilities) {
            if (sa.getDescription().startsWith(prefix)) {
                return sa;
            }
        }
        return null;
    }

    protected Card createCard(String name, Player p) {
        IPaperCard paperCard = FModel.getMagicDb().getCommonCards().getCard(name);
        if (paperCard == null) {
            StaticData.instance().attemptToLoadCard(name);
            paperCard = FModel.getMagicDb().getCommonCards().getCard(name);
        }
        if (paperCard == null) {
            throw new RuntimeException("Card not found: " + name);
        }
        return Card.fromPaperCard(paperCard, p);
    }

    protected Card addCardToZone(String name, Player p, ZoneType zone) {
        Card c = createCard(name, p);
        // card need a new Timestamp otherwise Static Abilities might collide
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        p.getZone(zone).add(c);
        return c;
    }

    protected Card addCard(String name, Player p) {
        return addCardToZone(name, p, ZoneType.Battlefield);
    }

    protected List<Card> addCards(String name, int count, Player p) {
        List<Card> cards = Lists.newArrayList();
        for (int i = 0; i < count; i++) {
            cards.add(addCard(name, p));
        }
        return cards;
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
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        p.getZone(ZoneType.Battlefield).add(c);
        return c;
    }

    void playUntilStackClear(Game game) {
        do {
            game.getPhaseHandler().mainLoopStep();
        } while (!game.isGameOver() && !game.getStack().isEmpty());
    }

    void playUntilPhase(Game game, PhaseType phase) {
        do {
            game.getPhaseHandler().mainLoopStep();
        } while (!game.isGameOver() && !game.getPhaseHandler().is(phase));
    }

    protected String gameStateToString(Game game) {
        StringBuilder sb = new StringBuilder();
        for (ZoneType zone : ZoneType.values()) {
            CardCollectionView cards = game.getCardsIn(zone);
            if (!cards.isEmpty()) {
                sb.append("Zone ").append(zone.name()).append(":\n");
                for (Card c : game.getCardsIn(zone)) {
                    sb.append("  ").append(c);
                    if (c.isTapped()) {
                        sb.append(" (T)");
                    }
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }
}