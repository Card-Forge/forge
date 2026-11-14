package forge.ai.ability;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import forge.ai.*;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class DamageDealAiTest extends AITest {
    @Test
    public void testChooseSingleEntityPrefersOpponentPlayer() {
        // Test for Comet, Stellar Pup fix: AI should target opponent player
        // instead of its own creatures when dealing damage
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        // Setup: AI has a creature, opponent has none
        Card aiCreature = addCard("Elvish Mystic", ai);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        // Create a mock SpellAbility for damage dealing
        Card dummySource = addCard("Mountain", ai);
        SpellAbility damageSa = new SpellAbility.EmptySa(ApiType.DealDamage, dummySource, ai);
        damageSa.putParam("NumDmg", "5");

        // Create options: opponent player + AI's creature
        List<GameEntity> options = new ArrayList<>();
        options.add(opponent);
        options.add(aiCreature);

        // Get the DamageDealAi and test chooseSingleEntity
        SpellAbilityAi damageAi = SpellApiToAi.Converter.get(ApiType.DealDamage);
        GameEntity chosen = damageAi.chooseSingleEntity(ai, damageSa, options, false, null, null);

        // AI should choose opponent player, not its own creature
        AssertJUnit.assertEquals("AI should target opponent player when dealing damage", opponent, chosen);
    }

    @Test
    public void testChooseSingleEntityPrefersKillableOpponentCreature() {
        // When opponent has creatures, AI should prefer killing the one it can kill
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        // Setup: Opponent has two creatures - one killable (2 toughness), one not (3 toughness)
        Card killableCreature = addCard("Grizzly Bears", opponent);  // 2/2
        Card toughCreature = addCard("Centaur Courser", opponent);   // 3/3

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        // Create a mock SpellAbility for damage dealing (enough to kill the 2/2 but not the 3/3)
        Card dummySource = addCard("Mountain", ai);
        SpellAbility damageSa = new SpellAbility.EmptySa(ApiType.DealDamage, dummySource, ai);
        damageSa.putParam("NumDmg", "2");

        // Create options: both creatures (tough one first to ensure AI picks correctly)
        List<GameEntity> options = new ArrayList<>();
        options.add(toughCreature);
        options.add(killableCreature);

        // Get the DamageDealAi and test chooseSingleEntity
        SpellAbilityAi damageAi = SpellApiToAi.Converter.get(ApiType.DealDamage);
        GameEntity chosen = damageAi.chooseSingleEntity(ai, damageSa, options, false, null, null);

        // AI should choose the creature it can kill (Grizzly Bears)
        AssertJUnit.assertEquals("AI should target opponent creature it can kill", killableCreature, chosen);
    }

    @Test
    public void testChooseSingleEntityMandatoryTargetsOwnWorst() {
        // When mandatory and only own stuff available, target worst creature
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        // Setup: AI has two creatures of different value
        Card weakCreature = addCard("Elvish Mystic", ai);  // 1/1
        Card strongCreature = addCard("Serra Angel", ai);  // 4/4 flying vigilance

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        // Create a mock SpellAbility for damage dealing
        Card dummySource = addCard("Mountain", ai);
        SpellAbility damageSa = new SpellAbility.EmptySa(ApiType.DealDamage, dummySource, ai);
        damageSa.putParam("NumDmg", "2");

        // Create options: only AI's creatures (mandatory, no opponents)
        List<GameEntity> options = new ArrayList<>();
        options.add(weakCreature);
        options.add(strongCreature);

        // Get the DamageDealAi and test chooseSingleEntity (not optional = mandatory)
        SpellAbilityAi damageAi = SpellApiToAi.Converter.get(ApiType.DealDamage);
        GameEntity chosen = damageAi.chooseSingleEntity(ai, damageSa, options, false, null, null);

        // AI should choose its worst creature (Elvish Mystic)
        AssertJUnit.assertEquals("AI should target its worst creature when mandatory", weakCreature, chosen);
    }

    @Test
    public void testChooseSingleEntityMandatoryPrefersTeammateOverOwn() {
        // When mandatory, AI should target teammate's creature before its own
        Game game = initAndCreateThreePlayerGame();
        Player ai = game.getPlayers().get(1);
        Player ally = game.getPlayers().get(2);
        Player opponent = game.getPlayers().get(0);

        // Set up teams: AI and ally on team 0, opponent on team 1
        ai.setTeam(0);
        ally.setTeam(0);
        opponent.setTeam(1);

        // Setup: AI and ally both have creatures
        Card aiCreature = addCard("Serra Angel", ai);      // 4/4 - stronger
        Card allyCreature = addCard("Elvish Mystic", ally); // 1/1 - weaker

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        // Create a mock SpellAbility for damage dealing
        Card dummySource = addCard("Mountain", ai);
        SpellAbility damageSa = new SpellAbility.EmptySa(ApiType.DealDamage, dummySource, ai);
        damageSa.putParam("NumDmg", "2");

        // Create options: only AI's and ally's creatures (no opponent targets - mandatory scenario)
        List<GameEntity> options = new ArrayList<>();
        options.add(aiCreature);
        options.add(allyCreature);

        // Get the DamageDealAi and test chooseSingleEntity
        SpellAbilityAi damageAi = SpellApiToAi.Converter.get(ApiType.DealDamage);
        GameEntity chosen = damageAi.chooseSingleEntity(ai, damageSa, options, false, null, null);

        // AI should choose ally's worst creature before its own
        AssertJUnit.assertEquals("AI should target ally's creature before its own when mandatory", allyCreature, chosen);
    }

    @Test
    public void testDistributeDamageBetweenThreeCreaturesTargetsAll() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCards("Forest", 10, ai);
        addCard("Ghalta, Primal Hunger", ai);

        Player user = game.getPlayers().get(0);
        addCard("Hill Giant", user);
        addCard("Colossal Dreadmaw", user);
        addCard("Siege Mastodon", user);

        Card source = addCardToZone("Monstrous Onslaught", ai, ZoneType.Hand);
        SpellAbility sa = source.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);

        MyRandom.setRandom(new Random(0));
        List<SpellAbility> plays = ((PlayerControllerAi) ai.getController()).getAi().chooseSpellAbilityToPlay();
        boolean isCast = plays != null && plays.stream().anyMatch(p -> p.getHostCard() == sa.getHostCard());
        AssertJUnit.assertTrue(isCast);
        AssertJUnit.assertEquals(12, sa.getTotalDividedValue());
        AssertJUnit.assertEquals(3, sa.getTargets().size());
    }

    @Test
    public void testDistributeDamageBetweenFourCreaturesTargetsOnlyTwo() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCards("Forest", 10, ai);
        addCard("Ghalta, Primal Hunger", ai);

        Player user = game.getPlayers().get(0);
        addCard("Craw Wurm", user);
        addCard("Craw Wurm", user);
        addCard("Colossal Dreadmaw", user);
        addCard("Siege Mastodon", user);

        Card source = addCardToZone("Monstrous Onslaught", ai, ZoneType.Hand);
        SpellAbility sa = source.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);

        MyRandom.setRandom(new Random(0));
        List<SpellAbility> plays = ((PlayerControllerAi) ai.getController()).getAi().chooseSpellAbilityToPlay();
        boolean isCast = plays != null && plays.stream().anyMatch(p -> p.getHostCard() == sa.getHostCard());
        AssertJUnit.assertTrue(isCast);
        AssertJUnit.assertEquals(12, sa.getTotalDividedValue());
        AssertJUnit.assertEquals(3, sa.getTargets().size());
    }

    @Test
    public void testDistributeDamageBetweenTwoCreaturesTargetsOne() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCards("Forest", 10, ai);
        addCard("Ravenous Gigantotherium", ai);

        Player user = game.getPlayers().get(0);
        addCard("Hill Giant", user);
        addCard("Hill Giant", user);

        Card source = addCardToZone("Monstrous Onslaught", ai, ZoneType.Hand);
        SpellAbility sa = source.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);

        MyRandom.setRandom(new Random(0));
        List<SpellAbility> plays = ((PlayerControllerAi) ai.getController()).getAi().chooseSpellAbilityToPlay();
        boolean isCast = plays != null && plays.stream().anyMatch(p -> p.getHostCard() == sa.getHostCard());
        AssertJUnit.assertTrue(isCast);
        AssertJUnit.assertEquals(3, sa.getTotalDividedValue());
        AssertJUnit.assertEquals(1, sa.getTargets().size());
    }

    @Test
    public void testDistributeDamageNotTargetsSpellIfNoKills() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCards("Forest", 10, ai);
        addCard("Ravenous Gigantotherium", ai);

        Player user = game.getPlayers().get(0);
        addCard("Colossal Dreadmaw", user);

        Card source = addCardToZone("Monstrous Onslaught", ai, ZoneType.Hand);
        SpellAbility sa = source.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);

        MyRandom.setRandom(new Random(0));
        List<SpellAbility> plays = ((PlayerControllerAi) ai.getController()).getAi().chooseSpellAbilityToPlay();
        boolean isCast = plays != null && plays.stream().anyMatch(p -> p.getHostCard() == sa.getHostCard());
        AssertJUnit.assertFalse(isCast);
    }

}
