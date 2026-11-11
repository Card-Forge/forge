package forge.gamemodes.rogue.effect;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import com.google.common.collect.Iterables;
import forge.ai.simulation.SimulationTest;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardDamageTable;
import forge.game.cost.CostAdjustment;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.path.NodePlanebound;
import forge.gamemodes.rogue.path.RoguePath;
import forge.item.PaperCard;
import forge.util.ImageUtil;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;

public class EndlessModeTest extends SimulationTest {

    @Test
    public void testModifiersScaleWithEndlessLevel() {
        assertEquals(new RogueRun().getEndlessLevel(), 0);
        assertEquals(EndlessMode.getLifeBonus(1), 5);
        assertEquals(EndlessMode.getLifeBonus(5), 25);
        assertEquals(EndlessMode.getHandBonus(1), 1);
        assertEquals(EndlessMode.getHandBonus(3), 3);
        assertEquals(EndlessMode.getHandBonus(5), 5);
        assertEquals(EndlessMode.getCostReduction(5), 5);
        assertEquals(EndlessMode.getDamageBonus(5), 5);
        assertEquals(EndlessMode.getBazaarPriceBonus(5), 5);
    }

    @Test
    public void testGeneratedCardParsesCostDamageAndLifeLossEffects() {
        PaperCard card = EndlessMode.createMatchCard(5);
        assertEquals(card.getName(), "Endless Mode - Level 5");
        assertNotEquals(card.getImageKey(false), EndlessMode.createMatchCard(6).getImageKey(false));
        String staticAbility = card.getRules().getMainPart().getStaticAbilities().iterator().next();
        String replacement = card.getRules().getMainPart().getReplacements().iterator().next();
        String trigger = card.getRules().getMainPart().getTriggers().iterator().next();

        assertTrue(staticAbility.contains("Amount$ 5"));
        assertTrue(staticAbility.contains("Activator$ Player.Opponent"));
        assertTrue(replacement.contains("ValidTarget$ You"));
        assertTrue(replacement.contains("ValidSource$ Card.OppCtrl,Emblem.OppCtrl"));
        assertTrue(trigger.contains("ValidCause$ SpellAbility.OppCtrl"));
        assertTrue(trigger.contains("LifeAmount$ GE1"));
        Game game = initAndCreateGame();
        Card gameCard = Card.fromPaperCard(card, game.getPlayers().get(0));
        assertTrue(gameCard.getSVar("TrigLifeLoss").contains("LifeAmount$ 5"));
        String displayedText = gameCard.getAbilityText();
        assertTrue(displayedText.contains("Opponents start with 25 additional life and up to 5 additional cards"));
        assertTrue(displayedText.contains("Their maximum hand size is increased by the same amount."));
        assertTrue(displayedText.contains("The Endless bonus cannot exceed 30% of their main deck size."));
        assertTrue(displayedText.contains("Spells your opponents cast cost {5} less to cast."));
        assertTrue(displayedText.contains("it deals that much damage plus 5 instead; whenever a spell or ability"));
        assertTrue(displayedText.contains("you lose 5 additional life."));
        assertFalse(displayedText.contains("Bazaar"));
        assertFalse(displayedText.contains("<span"));
    }

    @Test
    public void testPathMatchAndBazaarHooksUseCurrentLevel() {
        RogueRun run = new RogueRun();
        run.setEndlessLevel(5);
        NodePlanebound node = new NodePlanebound();
        node.setStartingLifeModification(7);
        run.setPath(new RoguePath(List.of(node)));
        EndlessMode.INSTANCE.afterPathGeneration(run);
        assertEquals(node.getStartingLifeModification(), 32);

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
        assertSame(ImageUtil.getPaperCardFromImageKey(matchCard.getImageKey(false)), matchCard);

        BazaarContext bazaar = new BazaarContext();
        bazaar.priceAdjustment = 2;
        EndlessMode.INSTANCE.onBeforeBazaar(bazaar, run);
        assertEquals(bazaar.priceAdjustment, 7);
    }

    @Test
    public void testCostReductionAppliesOnlyToOpponentSpells() {
        Game game = initAndCreateGame();
        Player human = game.getPlayers().get(0);
        Player opponent = game.getPlayers().get(1);
        Card endlessCard = Card.fromPaperCard(EndlessMode.createMatchCard(2), human);
        human.getZone(ZoneType.Command).add(endlessCard);

        Card opponentSpellCard = addCardToZone("Hedron Archive", opponent, ZoneType.Hand);
        SpellAbility opponentSpell = opponentSpellCard.getFirstSpellAbility();
        opponentSpell.setActivatingPlayer(opponent);
        ManaCostBeingPaid opponentCost = new ManaCostBeingPaid(opponentSpell.getPayCosts().getTotalMana());
        CostAdjustment.adjust(opponentCost, opponentSpell, opponent, null, true, false);
        assertEquals(opponentCost.getGenericManaAmount(), 2);

        Card humanSpellCard = addCardToZone("Hedron Archive", human, ZoneType.Hand);
        SpellAbility humanSpell = humanSpellCard.getFirstSpellAbility();
        humanSpell.setActivatingPlayer(human);
        ManaCostBeingPaid humanCost = new ManaCostBeingPaid(humanSpell.getPayCosts().getTotalMana());
        CostAdjustment.adjust(humanCost, humanSpell, human, null, true, false);
        assertEquals(humanCost.getGenericManaAmount(), 4);
    }

    @Test
    public void testLifeLossTriggerRequiresOpponentSpellOrAbility() {
        Game game = initAndCreateGame();
        Player human = game.getPlayers().get(0);
        Player opponent = game.getPlayers().get(1);
        Card endlessCard = Card.fromPaperCard(EndlessMode.createMatchCard(2), human);
        human.getZone(ZoneType.Command).add(endlessCard);
        Trigger trigger = endlessCard.getTriggers().iterator().next();
        Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(human);
        runParams.put(AbilityKey.LifeAmount, 1);

        Card opponentCard = addCardToZone("Hedron Archive", opponent, ZoneType.Hand);
        SpellAbility opponentSpell = opponentCard.getFirstSpellAbility();
        opponentSpell.setActivatingPlayer(opponent);
        runParams.put(AbilityKey.SpellAbility, opponentSpell);
        assertTrue(trigger.performTest(runParams));

        Card humanCard = addCardToZone("Hedron Archive", human, ZoneType.Hand);
        SpellAbility humanSpell = humanCard.getFirstSpellAbility();
        humanSpell.setActivatingPlayer(human);
        runParams.put(AbilityKey.SpellAbility, humanSpell);
        assertFalse(trigger.performTest(runParams));

        runParams.put(AbilityKey.SpellAbility, null);
        assertFalse(trigger.performTest(runParams));
    }

    @Test(timeOut = 5000)
    public void testDamageReplacementAppliesOnceToOpponentSourceTargetingPlayer() {
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
