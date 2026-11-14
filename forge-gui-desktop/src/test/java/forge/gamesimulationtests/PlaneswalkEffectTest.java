package forge.gamesimulationtests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import forge.ai.AITest;
import forge.card.CardDb;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.gamesimulationtests.util.LobbyPlayerForTests;
import forge.model.FModel;
import java.util.EnumSet;
import java.util.List;
import org.testng.annotations.Test;

public class PlaneswalkEffectTest extends AITest {

    @Test
    public void roguePlaneswalkKeepsActivePlane() {
        List<RegisteredPlayer> players = List.of(
            new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerForTests("Player", null)),
            new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerForTests("Opponent", null)));
        GameRules rules = new GameRules(GameType.RogueCommander);
        Game game = new Game(players, rules, new Match(rules, players, "Test"));
        Player player = game.getPlayers().get(0);
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, player);

        CardDb planes = FModel.getMagicDb().getVariantCards();
        // Inys Haen has both an arrival trigger and a departure trigger.
        Card activePlane = Card.fromPaperCard(planes.getCard("Inys Haen", "MOC"), player);
        Card nextPlane = Card.fromPaperCard(planes.getCard("Krosa", "MOC"), player);
        player.getZone(ZoneType.PlanarDeck).add(activePlane);
        player.getZone(ZoneType.PlanarDeck).add(nextPlane);
        player.initPlane();
        game.getTriggerHandler().resetActiveTriggers();

        SpellAbility planeswalk = AbilityFactory.getAbility("DB$ Planeswalk", activePlane);
        planeswalk.setActivatingPlayer(player);
        planeswalk.resolve();
        game.getStack().addAllTriggeredAbilitiesToStack();

        assertEquals(game.getActivePlanes(), List.of(activePlane));
        assertTrue(activePlane.isInZone(ZoneType.Command));
        assertTrue(nextPlane.isInZone(ZoneType.PlanarDeck));
        assertEquals(game.getStack().size(), 2);
        EnumSet<TriggerType> triggerTypes = EnumSet.noneOf(TriggerType.class);
        for (SpellAbilityStackInstance entry : game.getStack()) {
            SpellAbility ability = entry.getSpellAbility();
            assertEquals(ability.getHostCard(), activePlane);
            triggerTypes.add(ability.getTrigger().getMode());
        }
        assertEquals(triggerTypes, EnumSet.of(TriggerType.PlaneswalkedFrom, TriggerType.PlaneswalkedTo));
    }
}
