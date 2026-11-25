package forge.game.trigger;

import java.util.ArrayList;
import java.util.List;

import forge.ai.AITest;
import forge.ai.ComputerUtil;
import forge.ai.LobbyPlayerAi;
import forge.ai.simulation.GameSimulator;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class SimultaneousEntryTriggerTest extends AITest {

    @Test
    public void landEnteringAlongsideYavimayaIsAForest() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        addCard("Roiling Canopy", p);
        addCards("Forest", 5, p);
        addCard("Grizzly Bears", p);
        addCardToZone("Wastes", p, ZoneType.Graveyard);
        addCardToZone("Yavimaya, Cradle of Growth", p, ZoneType.Graveyard);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = addCardToZone("Splendid Reclamation", p, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();

        AssertJUnit.assertEquals(2, game.getStack().size());
    }

    @Test
    public void kodamaSeesMoxDiamondThatEntersWithItButMovesFirst() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        addCardToZone("Island", p, ZoneType.Hand);
        addCardToZone("Mox Diamond", p, ZoneType.Graveyard);
        addCardToZone("Kodama of the East Tree", p, ZoneType.Graveyard);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = AbilityFactory.getAbility("DB$ ChangeZoneAll | ChangeType$ Permanent.YouOwn | Origin$ Graveyard | Destination$ Battlefield", addCard("Forest", p));
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();

        AssertJUnit.assertEquals(1, game.getStack().size());
        AssertJUnit.assertEquals("Kodama of the East Tree", game.getStack().peekAbility().getHostCard().getName());
    }

    @Test
    public void encoredDormantSliversEachDrawForEveryCopyEnteringTogether() {
        List<RegisteredPlayer> players = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            players.add(RegisteredPlayer.forCommander(new Deck()).setPlayer(new LobbyPlayerAi("p" + i, null)));
        }
        GameRules rules = new GameRules(GameType.Commander);
        rules.addAppliedVariant(GameType.Commander);
        Game game = new Match(rules, players, "Test").createGame();
        Player p = game.getPlayers().get(0);
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        addCard("Sliver Gravemother", p);
        Card dormant = addCardToZone("Dormant Sliver", p, ZoneType.Graveyard);
        addCards("Forest", 4, p);
        fillLibrary(p, 10);
        game.getAction().checkStateEffects(true);
        game.copyLastState();

        SpellAbility encore = dormant.getSpellAbilities().stream()
                .filter(sa -> sa.isKeyword(Keyword.ENCORE)).findFirst().orElseThrow();
        encore.setActivatingPlayer(p);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(p, encore, null));
        GameSimulator.resolveStack(game, game.getPlayers().get(1));

        AssertJUnit.assertEquals(9, p.getNumDrawnThisTurn());
    }
}
