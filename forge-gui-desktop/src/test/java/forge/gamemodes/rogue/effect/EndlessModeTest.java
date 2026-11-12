package forge.gamemodes.rogue.effect;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import com.google.common.collect.Iterables;
import forge.ai.simulation.SimulationTest;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.card.Card;
import forge.game.card.CardDamageTable;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.ZoneType;
import forge.gamemodes.rogue.RogueRun;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.ImageUtil;
import org.testng.annotations.Test;

public class EndlessModeTest extends SimulationTest {

    @Test
    public void setsUpEndlessMatch() {
        RogueRun run = new RogueRun();
        run.setEndlessLevel(5);

        RegisteredPlayer human = new RegisteredPlayer(new Deck());
        Deck opponentDeck = new Deck();
        opponentDeck.getMain().add("Hedron Archive", 10);
        RegisteredPlayer opponent = new RegisteredPlayer(opponentDeck);
        opponent.setStartingHand(7);
        EndlessMode.INSTANCE.onMatchStart(human, opponent, run);
        assertEquals(opponent.getStartingHand(), 10);
        assertEquals(human.getStartingHand(), 7);
        assertEquals(Iterables.size(human.getExtraCardsInCommandZone()), 1);
        PaperCard matchCard = (PaperCard) human.getExtraCardsInCommandZone().iterator().next();
        assertEquals(matchCard.getName(), "Endless Mode - Level 5");
        assertTrue(matchCard.getRules().isCustom());
        assertNull(FModel.getMagicDb().getCommonCards().getCard(matchCard.getName()));
        assertSame(ImageUtil.getPaperCardFromImageKey(matchCard.getImageKey(false)), matchCard);

        Game game = initAndCreateGame();
        Card gameCard = Card.fromPaperCard(matchCard, game.getPlayers().get(0));
        String displayedText = gameCard.getAbilityText();
        assertFalse(displayedText.isBlank());
        assertFalse(displayedText.contains("<span"));
    }

    @Test(timeOut = 5000)
    public void opponentDamageBonusAppliesOnce() {
        Game game = initAndCreateGame();
        Player human = game.getPlayers().get(0);
        Player opponent = game.getPlayers().get(1);
        Card endlessCard = Card.fromPaperCard(EndlessMode.createMatchCard(2), human);
        human.getZone(ZoneType.Command).add(endlessCard);
        Card opponentSource = addCard("Runeclaw Bear", opponent);
        Card humanSource = addCard("Runeclaw Bear", human);
        game.getAction().checkStateEffects(true);

        for (boolean combat : new boolean[] { false, true }) {
            CardDamageTable damageMap = new CardDamageTable();
            damageMap.put(opponentSource, human, 1);
            damageMap.put(opponentSource, opponent, 1);
            damageMap.put(humanSource, human, 1);
            game.getReplacementHandler().runReplaceDamage(combat, damageMap,
                new CardDamageTable(), new GameEntityCounterTable(), null);

            assertEquals(damageMap.get(opponentSource, human).intValue(), 3);
            assertEquals(damageMap.get(opponentSource, opponent).intValue(), 1);
            assertEquals(damageMap.get(humanSource, human).intValue(), 1);
        }
    }
}
