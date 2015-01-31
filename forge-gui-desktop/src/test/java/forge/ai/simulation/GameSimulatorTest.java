package forge.ai.simulation;

import java.util.List;

import com.google.common.collect.Lists;

import forge.GuiBase;
import forge.GuiDesktop;
import forge.ai.LobbyPlayerAi;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.IPaperCard;
import forge.model.FModel;
import junit.framework.TestCase;

public class GameSimulatorTest extends TestCase {
    private Game initAndCreateGame() {
        List<RegisteredPlayer> players = Lists.newArrayList();
        Deck d1 = new Deck();
        players.add(new RegisteredPlayer(d1).setPlayer(new LobbyPlayerAi("p2")));
        players.add(new RegisteredPlayer(d1).setPlayer(new LobbyPlayerAi("p1")));
        GameRules rules = new GameRules(GameType.Constructed);
        Match match = new Match(rules, players);
        Game game = new Game(players, rules, match);

        GuiBase.setInterface(new GuiDesktop());
        FModel.initialize(null);
        return game;
    }
 
    private Card findCardWithName(Game game, String name) {
        for (Card c : game.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    private SpellAbility findSAWithPrefix(Card c, String prefix) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.toString().startsWith(prefix)) {
                return sa;
            }
        }
        return null;
    }

    private Card addCard(String name, Player p) {
        IPaperCard paperCard = FModel.getMagicDb().getCommonCards().getCard(name);
        Card c = Card.fromPaperCard(paperCard, p);
        p.getZone(ZoneType.Battlefield).add(c);
        return c;
    }

    public void testActivateAbilityTriggers() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Plains", p);
        addCard("Plains", p);
        addCard("Plains", p);
        String heraldCardName = "Herald of Anafenza";
        Card herald = addCard(heraldCardName, p);
        herald.setSickness(false);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        SpellAbility outlastSA = findSAWithPrefix(herald, "Outlast");
        assertNotNull(outlastSA);

        GameSimulator sim = new GameSimulator(game);
        int score = sim.simulateSpellAbility(outlastSA);
        assertTrue(score >  0);
        Game simGame = sim.getSimulatedGameState();

        Card heraldCopy = findCardWithName(simGame, heraldCardName);
        assertNotNull(heraldCopy);
        assertTrue(heraldCopy.isTapped());
        assertTrue(heraldCopy.hasCounters());
        assertEquals(1, heraldCopy.getToughnessBonusFromCounters());
        assertEquals(1, heraldCopy.getPowerBonusFromCounters());

        Card warriorToken = findCardWithName(simGame, "Warrior");
        assertNotNull(warriorToken);
        assertTrue(warriorToken.isSick());
        assertEquals(1, warriorToken.getCurrentPower());
        assertEquals(1, warriorToken.getCurrentToughness());
    }

}
