package forge.ai.ability;

import java.util.ArrayList;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.ai.SpellApiToAi;
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
        DamageDealAi damageAi = (DamageDealAi) SpellApiToAi.Converter.get(ApiType.DealDamage);
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
        DamageDealAi damageAi = (DamageDealAi) SpellApiToAi.Converter.get(ApiType.DealDamage);
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
        DamageDealAi damageAi = (DamageDealAi) SpellApiToAi.Converter.get(ApiType.DealDamage);
        GameEntity chosen = damageAi.chooseSingleEntity(ai, damageSa, options, false, null, null);

        // AI should choose its worst creature (Elvish Mystic)
        AssertJUnit.assertEquals("AI should target its worst creature when mandatory", weakCreature, chosen);
    }
}
