package forge.ai;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import forge.game.Game;
import forge.game.ability.AbilityApiBased;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import org.testng.AssertJUnit;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class RegenerationPredictionTest extends AITest {

    @DataProvider(name = "selfRegenerators")
    public Object[][] selfRegenerators() {
        return new Object[][] {
            { "Wall of Bone", "Swamp", "Forest" },
            { "Will-o'-the-Wisp", "Swamp", "Forest" },
            { "Uthden Troll", "Mountain", "Forest" },
            { "Living Wall", "Forest", null }
        };
    }

    @Test(dataProvider = "selfRegenerators")
    public void predictionRespectsManaAndAbilityRestrictions(String name, String manaName, String wrongManaName) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card creature = addCard(name, ai);
        Card other = addCard("Dragon Whelp", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(ComputerUtil.canRegenerate(ai, creature));
        if (wrongManaName != null) {
            addCard(wrongManaName, ai);
            game.getAction().checkStateEffects(true);
            AssertJUnit.assertFalse("Wrong-colored mana must not pay for regeneration", ComputerUtil.canRegenerate(ai, creature));
        }

        Card mana = addCard(manaName, ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(ComputerUtil.canRegenerate(ai, other));
        AssertJUnit.assertTrue(ComputerUtil.canRegenerate(ai, creature));
        AssertJUnit.assertTrue("Opponent predictions must also see available regeneration",
                ComputerUtil.canRegenerate(game.getPlayers().get(0), creature));
        AssertJUnit.assertFalse("Prediction must not spend mana", mana.isTapped());
        AssertJUnit.assertEquals(0, creature.getShieldCount());

        mana.setTapped(true);
        AssertJUnit.assertFalse(ComputerUtil.canRegenerate(ai, creature));
        mana.setTapped(false);
        SpellAbility regeneration = creature.getSpellAbilities().stream()
                .filter(sa -> sa.getApi() == ApiType.Regenerate).findFirst().orElseThrow();
        regeneration.setSuppressed(true);
        AssertJUnit.assertFalse("A disabled ability must not count", ComputerUtil.canRegenerate(ai, creature));
        regeneration.setSuppressed(false);
        AssertJUnit.assertTrue(ComputerUtil.canRegenerate(ai, creature));
        creature.addStaticAbility("Mode$ CantRegenerate | ValidCard$ Card.Self");
        AssertJUnit.assertFalse("Cannot-regenerate effects must still apply", ComputerUtil.canRegenerate(ai, creature));
    }

    @Test(dataProvider = "selfRegenerators")
    public void aiActuallyRegeneratesAfterAnUnrelatedPrediction(String name, String manaName, String wrongManaName) {
        for (boolean hasMana : new boolean[] { false, true }) {
            Game game = initAndCreateGame();
            Player attacker = game.getPlayers().get(1);
            Player defender = game.getPlayers().get(0);
            attacker.setTeam(0);
            defender.setTeam(1);
            Card threat = addCard("Craw Wurm", attacker);
            threat.setSickness(false);
            Card blocker = addCard(name, defender);
            Card unrelated = addCard("Dragon Whelp", defender);
            if (hasMana) {
                addCard(manaName, defender);
            }
            game.getAction().checkStateEffects(true);
            AssertJUnit.assertFalse(ComputerUtil.canRegenerate(defender, unrelated));

            Combat combat = new Combat(attacker);
            combat.addAttacker(threat, defender);
            combat.addBlocker(threat, blocker);
            game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, attacker);
            game.getPhaseHandler().setCombat(combat);
            combat.fireTriggersForUnblockedAttackers(game);
            combat.orderBlockersForDamageAssignment();
            combat.orderAttackersForDamageAssignment();
            for (int i = 0; i < 100 && !game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DAMAGE); i++) {
                game.getPhaseHandler().mainLoopStep();
            }
            AssertJUnit.assertTrue("Combat must finish", game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DAMAGE));
            if (hasMana) {
                AssertJUnit.assertTrue("AI must save the blocker", blocker.isInPlay());
                AssertJUnit.assertEquals("The regeneration shield must actually replace lethal destruction", 1, blocker.getRegeneratedThisTurn());
            } else {
                AssertJUnit.assertEquals("Without mana the blocker must die", 1, countCardsWithName(game, name, ZoneType.Graveyard));
            }
        }
    }

    @Test
    public void lifePaymentSafetyStillApplies() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card creature = addCard("Mischievous Poltergeist", ai);
        ai.setLife(20, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(ComputerUtil.canRegenerate(ai, creature));
        AssertJUnit.assertEquals("Prediction must not pay life", 20, ai.getLife());
        ai.setLife(4, null);
        AssertJUnit.assertFalse("AI must preserve its life-payment safety margin", ComputerUtil.canRegenerate(ai, creature));
    }

    @Test
    public void sacrificeRegenerationRetainsItsTargetRestriction() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card jar = addCard("Welding Jar", ai);
        Card artifact = addCard("Living Wall", ai);
        Card creature = addCard("Dragon Whelp", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(ComputerUtil.canRegenerate(ai, creature));
        AssertJUnit.assertTrue(ComputerUtil.canRegenerate(ai, artifact));
        AssertJUnit.assertTrue("Prediction must not sacrifice its source", jar.isInPlay());
    }

    @Test
    public void targetedRegenerationStillProtectsAnotherCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCard("Asceticism", ai);
        Card creature = addCard("Dragon Whelp", ai);
        Card land = addCard("Forest", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(ComputerUtil.canRegenerate(ai, creature));

        addCard("Mountain", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(ComputerUtil.canRegenerate(ai, creature));
        AssertJUnit.assertFalse(ComputerUtil.canRegenerate(ai, land));
    }

    @Test
    public void combatPredictionSkipsUnrelatedAbilityEvaluation() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card wall = addCard("Living Wall", ai);
        Card other = addCard("Dragon Whelp", ai);
        AtomicInteger evaluations = new AtomicInteger();
        wall.addSpellAbility(new AbilityApiBased(ApiType.Regenerate, wall, new Cost("1", true), null,
                Map.of("Defined", "Self")) {
            @Override
            public boolean canPlay() {
                evaluations.incrementAndGet();
                return false;
            }
        });
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertFalse(ComputerUtil.canRegenerate(ai, other));
        AssertJUnit.assertEquals("Unrelated regeneration must not evaluate playability or payment", 0, evaluations.get());
    }
}
