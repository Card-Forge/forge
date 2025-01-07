package forge.ai;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

public class LandDropTests extends AITest {
    @Test
    public void testPlayTapland() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCardToZone("Grizzly Bears", p, ZoneType.Hand);
        addCardToZone("Forest", p, ZoneType.Hand);
        Card te = addCardToZone("Tranquil Expanse", p, ZoneType.Hand);
        addCardToZone("Plains", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        this.playUntilPhase(game, PhaseType.END_OF_TURN);

        AssertJUnit.assertTrue(game.getCardsIn(ZoneType.Battlefield).contains(te));
    }

    @Test
    public void playTaplandIfNoPlays() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        // start with a hand with a basic, a tapland, and a card that can't be cast
        addCard("Forest", p);
        addCardToZone("Forest", p, ZoneType.Hand);
        Card desired = addCardToZone("Simic Guildgate", p, ZoneType.Hand);
        addCardToZone("Centaur Courser", p, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        // ensure that the tapland is paid
        SpellAbility sa = p.getController().chooseSpellAbilityToPlay().get(0);
        AssertJUnit.assertEquals(desired, sa.getHostCard());
    }

    @Test
    public void testPlayTypedTapland() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCardToZone("Grizzly Bears", p, ZoneType.Hand);
        addCardToZone("Forest", p, ZoneType.Hand);
        addCardToZone("Tranquil Expanse", p, ZoneType.Hand);
        Card t = addCardToZone("Radiant Grove", p, ZoneType.Hand);
        addCardToZone("Plains", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        this.playUntilPhase(game, PhaseType.END_OF_TURN);

        AssertJUnit.assertTrue(game.getCardsIn(ZoneType.Battlefield).contains(t));
    }

    @Test
    public void testPlayUntappedLandWhenNeeded() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCard("Plains", p);
        addCardToZone("Grizzly Bears", p, ZoneType.Hand);
        addCardToZone("Tranquil Expanse", p, ZoneType.Hand);
        Card t = addCardToZone("Forest", p, ZoneType.Hand);
        addCardToZone("Plains", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        this.playUntilPhase(game, PhaseType.END_OF_TURN);

        AssertJUnit.assertTrue(game.getCardsIn(ZoneType.Battlefield).contains(t));
    }

    @Test
    public void testPlayNonTappedFetchLand() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCard("Plains", p);
        addCardToZone("Grizzly Bears", p, ZoneType.Hand);
        // this card will fetch a land that will enter tapped
        addCardToZone("Evolving Wilds", p, ZoneType.Hand);
        // This land doesn't have basic land types, but enters untapped
        Card t = addCardToZone("Pendelhaven", p, ZoneType.Hand);
        addCardToZone("Eiganjo Castle", p, ZoneType.Hand);
        // make sure there are fetchable targets
        addCardToZone("Forest", p, ZoneType.Library);
        addCardToZone("Plains", p, ZoneType.Library);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        this.playUntilPhase(game, PhaseType.END_OF_TURN);

        AssertJUnit.assertTrue(game.getCardsIn(ZoneType.Battlefield).contains(t));
    }

    @Ignore
    @Test
    public void testPlayUntappedFetchLand() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCard("Plains", p);
        addCardToZone("Grizzly Bears", p, ZoneType.Hand);
        // this card will fetch a land that will enter tapped
        addCardToZone("Evolving Wilds", p, ZoneType.Hand);
        // This land doesn't have basic land types, but enters untapped
        Card t = addCardToZone("Windswept Heath", p, ZoneType.Hand);
        addCardToZone("Plains", p, ZoneType.Hand);
        // make sure there are fetchable targets
        addCardToZone("Forest", p, ZoneType.Library);
        addCardToZone("Plains", p, ZoneType.Library);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        this.playUntilPhase(game, PhaseType.END_OF_TURN);

        // check the graveyard because they need to have fetched to play the grizzly bears
        AssertJUnit.assertTrue(game.getCardsIn(ZoneType.Graveyard).contains(t));
    }

    @Test
    public void testPlayDual() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCardToZone("Grizzly Bears", p, ZoneType.Hand);
        addCardToZone("Forest", p, ZoneType.Hand);
        addCardToZone("Tranquil Expanse", p, ZoneType.Hand);
        Card t = addCardToZone("Savannah", p, ZoneType.Hand);
        addCardToZone("Plains", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        this.playUntilPhase(game, PhaseType.END_OF_TURN);

        AssertJUnit.assertTrue(game.getCardsIn(ZoneType.Battlefield).contains(t));
    }

    @Test
    public void testPlayCheckland() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        // We have a plains in play, and we want to play a checkland over the forest and plains
        // we also don't want to play the tapland
        addCard("Plains", p);
        addCardToZone("Grizzly Bears", p, ZoneType.Hand);
        addCardToZone("Forest", p, ZoneType.Hand);
        addCardToZone("Tranquil Expanse", p, ZoneType.Hand);
        Card t = addCardToZone("Sunpetal Grove", p, ZoneType.Hand);
        addCardToZone("Plains", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        this.playUntilPhase(game, PhaseType.END_OF_TURN);

        AssertJUnit.assertTrue(game.getCardsIn(ZoneType.Battlefield).contains(t));
    }

    @Ignore
    @Test
    public void testPlayShockland() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        Card gs = addCardToZone("Godless Shrine", p, ZoneType.Hand);
        addCardToZone("Plains", p, ZoneType.Hand);
        addCardToZone("Swamp", p, ZoneType.Hand);
        Card ts = addCardToZone("Thoughtseize", p, ZoneType.Hand);
        addCardToZone("Bitterblossom", p, ZoneType.Hand);
        addCardToZone("Lingering Souls", p, ZoneType.Hand);
        addCardToZone("Sorin, Solemn Visitor", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        addCardToZone("Godless Shrine", opponent, ZoneType.Hand);
        addCardToZone("Plains", opponent, ZoneType.Hand);
        addCardToZone("Swamp", opponent, ZoneType.Hand);
        addCardToZone("Thoughtseize", opponent, ZoneType.Hand);
        Card bb = addCardToZone("Bitterblossom", opponent, ZoneType.Hand);
        addCardToZone("Lingering Souls", opponent, ZoneType.Hand);
        addCardToZone("Sorin, Solemn Visitor", opponent, ZoneType.Hand);

        this.playUntilPhase(game, PhaseType.END_OF_TURN);

        // test that we shock in Godless Shrine and cast thoughtseize, and then take bitterblossom
        AssertJUnit.assertTrue(game.getCardsIn(ZoneType.Battlefield).contains(gs));
        AssertJUnit.assertTrue(game.getCardsIn(ZoneType.Graveyard).contains(ts));
        AssertJUnit.assertTrue(game.getCardsIn(ZoneType.Graveyard).contains(bb));
    }

    @Test
    public void playBouncelandIfNoPlays() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        // start with a hand with a basic, a bounceland, and a card that can't be cast
        addCard("Forest", p);
        addCardToZone("Forest", p, ZoneType.Hand);
        Card desired = addCardToZone("Simic Growth Chamber", p, ZoneType.Hand);
        addCardToZone("Centaur Courser", p, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        // ensure that the tapland is played
        SpellAbility sa = p.getController().chooseSpellAbilityToPlay().get(0);
        AssertJUnit.assertEquals(desired, sa.getHostCard());
    }

    @Ignore
    @Test
    public void playTronOverBasic() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        // start with a hand with a basic, a Tron land, and a card that can't be cast
        addCard("Urza's Tower", p);
        addCard("Urza's Mine", p);
        addCardToZone("Forest", p, ZoneType.Hand);
        Card desired = addCardToZone("Urza's Power Plant", p, ZoneType.Hand);
        addCardToZone("Opt", p, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        // ensure that the tron land is played
        SpellAbility sa = p.getController().chooseSpellAbilityToPlay().get(0);
        AssertJUnit.assertEquals(desired, sa.getHostCard());
    }

    @Test
    public void playManalessLands() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        // start with a hand with a land that can't produce mana.
        Card desired = addCardToZone("Maze of Ith", p, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        // ensure that the land is played
        SpellAbility sa = p.getController().chooseSpellAbilityToPlay().get(0);
        AssertJUnit.assertEquals(desired, sa.getHostCard());
    }

    @Ignore
    @Test
    public void playBasicOverUtility() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        // start with a hand with a colorless utility land and a basic
        addCardToZone("Rogue's Passage", p,  ZoneType.Hand);
        Card desired = addCardToZone("Forest", p, ZoneType.Hand);

        // make sure that there is a card in the library with G mana cost
        addCardToZone("Grizzly Bears", p,  ZoneType.Library);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        // ensure that the basic land is played
        SpellAbility sa = p.getController().chooseSpellAbilityToPlay().get(0);
        AssertJUnit.assertEquals(desired, sa.getHostCard());
    }

    @Test
    public void noBouncelandIfNothingToBounce() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        // start with a hand with a basic, a bounceland, and a card that can't be cast
        Card desired = addCardToZone("Forest", p, ZoneType.Hand);
        addCardToZone("Simic Growth Chamber", p, ZoneType.Hand);
        addCardToZone("Centaur Courser", p, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        // ensure that the forest is played
        SpellAbility sa = p.getController().chooseSpellAbilityToPlay().get(0);
        AssertJUnit.assertEquals(desired, sa.getHostCard());
    }

    // test bounceland with amulet of vigor
}
