package forge.web.match;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import forge.ai.LobbyPlayerAi;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.ZoneType;
import forge.gui.card.CardDetailUtil;
import forge.model.FModel;
import forge.web.WebTestSupport;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/** The page turns some of CardDetailUtil's English lines into chips (match/rulestext.ts), so this test fixes their format. */
public class CardDetailTextTest {
    @BeforeClass
    public void setUp() {
        WebTestSupport.initModel();
    }

    // Fails if a line that rulestext.ts matches by its English prefix is reworded. The page would then show it as plain text
    @Test
    public void stateLinesKeepTheShapesThePageReads() {
        final List<RegisteredPlayer> players = Lists.newArrayList();
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("p1", null)));
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("p2", null)));
        final GameRules rules = new GameRules(GameType.Constructed);
        final Game game = new Game(players, rules, new Match(rules, players, "Test"));
        final Player p = game.getPlayers().get(0);
        final Card bear = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Grizzly Bears"), p);
        p.getZone(ZoneType.Battlefield).add(bear);
        final Multiset<CounterType> counters = HashMultiset.create();
        counters.add(CounterEnumType.P1P1, 2);
        bear.setCounters(counters);
        bear.setDamage(1);
        bear.addAssignedDamage(2, bear);

        final CardView view = bear.getView();
        final List<String> lines = List.of(CardDetailUtil.composeCardText(view.getCurrentState(), game.getView(), true).split("\n"));
        Assert.assertTrue(lines.stream().anyMatch(l -> l.matches(".+ counters: 2")), "no counter line in " + lines);
        Assert.assertTrue(lines.contains("Damage: 1"), "no damage line in " + lines);
        Assert.assertTrue(lines.contains("Assigned Damage: 2"), "no assigned damage line in " + lines);
    }
}
