package forge.ai.ability;

import com.google.common.collect.Lists;
import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.ai.SpellAbilityAi;
import forge.ai.SpellApiToAi;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.List;

public class ChoosePlayerAiTest extends AITest {
    private Game initAndCreateThreePlayerGame() {
        initAndCreateGame();

        List<RegisteredPlayer> players = Lists.newArrayList();
        Deck deck = new Deck();
        players.add(new RegisteredPlayer(deck).setPlayer(new LobbyPlayerAi("human", null)));
        players.add(new RegisteredPlayer(deck).setPlayer(new LobbyPlayerAi("ai", null)));
        players.add(new RegisteredPlayer(deck).setPlayer(new LobbyPlayerAi("ai-opponent", null)));

        GameRules rules = new GameRules(GameType.Constructed);
        Match match = new Match(rules, players, "Test");
        Game game = new Game(players, rules, match);
        Player ai = game.getPlayers().get(1);
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getPhaseHandler().onStackResolved();
        return game;
    }

    @Test
    public void testCurseChoiceUsesPreferredOpponentFromAllPlayers() {
        Game game = initAndCreateThreePlayerGame();
        Player human = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Player aiOpponent = game.getPlayers().get(2);
        human.setTeam(0);
        ai.setTeam(1);
        aiOpponent.setTeam(2);

        human.setLife(20, null);
        aiOpponent.setLife(5, null);

        Card source = addCard("Mountain", ai);
        SpellAbility choosePlayerSa = new SpellAbility.EmptySa(ApiType.ChoosePlayer, source, ai);
        choosePlayerSa.putParam("AILogic", "Curse");

        SpellAbilityAi choosePlayerAi = SpellApiToAi.Converter.get(ApiType.ChoosePlayer);
        Player chosen = choosePlayerAi.chooseSingleEntity(ai, choosePlayerSa,
                Lists.newArrayList(human, ai, aiOpponent), false, null, null);

        AssertJUnit.assertEquals("AI should not blindly choose the first opponent in turn order",
                aiOpponent, chosen);
    }
}
