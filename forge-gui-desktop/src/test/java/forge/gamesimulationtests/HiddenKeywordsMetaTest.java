package forge.gamesimulationtests;

import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.game.spellability.SpellAbility;
import forge.game.ability.ApiType;
import org.testng.Assert;
import org.testng.annotations.Test;

public class HiddenKeywordsMetaTest extends SimulationTest {

    @Test
    public void testMustBeBlockedIfAble() {
        Game game = initAndCreateGame();
        Player p1 = game.getPlayers().get(0);
        Player p2 = game.getPlayers().get(1);

        // P1 has Prized Unicorn (Must be blocked if able)
        // Since we might not have the card script, we can mock the ability or use a card that has it.
        // Or better, create a vanilla card and give it the keyword to test the mechanism directly,
        // since the issue is about the mechanism support.
        Card unicorn = createCard("Grizzly Bears", p1);
        unicorn.setName("Prized Unicorn"); // Rename for flavor
        unicorn.addIntrinsicKeyword("CARDNAME must be blocked if able.");
        p1.getZone(ZoneType.Battlefield).add(unicorn);
        unicorn.setGameTimestamp(game.getNextTimestamp());

        // P2 has a blocker
        Card bear = createCard("Grizzly Bears", p2);
        p2.getZone(ZoneType.Battlefield).add(bear);
        bear.setGameTimestamp(game.getNextTimestamp());

        unicorn.setSickness(false);
        bear.setSickness(false);
        
        // Ensure bear can block
        bear.setTapped(false);

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, p1);
        
        Combat combat = new Combat(p1);
        combat.addAttacker(unicorn, p2);
        game.getPhaseHandler().setCombat(combat);
        
        // Bear MUST block Unicorn if able
        // We can check this by seeing if a "No Block" assignment is valid?
        // CombatUtil doesn't have "isValidAssignment".
        // But we can check specific flags or use AI simulation logic, but CombatUtil logic is what we want.
        
        // Actually, CombatUtil.canBlock checks if it CAN block.
        // We need to check the "Must Block" requirement.
        // CombatUtil.mustBlockAnAttacker checks if a blocker MUST block something.
        
        boolean mustBlockSomething = CombatUtil.mustBlockAnAttacker(bear, combat, null);
        // Debug assertions
        Assert.assertTrue(unicorn.hasStartOfKeyword("CARDNAME must be blocked if able."), "Unicorn should have the keyword");
        Assert.assertTrue(CombatUtil.canBeBlocked(unicorn, combat, p2), "Unicorn should be blockable");
        Assert.assertTrue(CombatUtil.canBlock(unicorn, bear), "Bear should be able to block Unicorn");
        Assert.assertTrue(CombatUtil.canBlock(bear, combat), "Bear should be able to block in combat");
        
        Assert.assertTrue(mustBlockSomething, "Bear should be required to block an attacker");
        
        // If we add another attacker that doesn't force blocks
        Card elf = createCard("Llanowar Elves", p1);
        p1.getZone(ZoneType.Battlefield).add(elf);
        elf.setGameTimestamp(game.getNextTimestamp());
        elf.setSickness(false);
        combat.addAttacker(elf, p2);
        
        // Bear must block Unicorn, so it cannot block Elf if it can only block 1.
        // However, standard rules say "if able". If it blocks Elf, it is blocking *an* attacker.
        // But "Unicorn must be blocked". If blocking Elf leaves Unicorn unblocked, is that valid?
        // Rules: "must be blocked if able" means a legal block assignment must include blocking this creature if possible.
        // This is a validity check on the *whole combat assignment*, which is harder to test with just CombatUtil methods.
        // However, typically the AI or validity checker uses `CombatUtil.validateBlockers`.
        // Let's check `CombatUtil.validateBlockers`.
    }

    @Test
    public void testDetain() {
        Game game = initAndCreateGame();
        Player p1 = game.getPlayers().get(0);
        
        Card bear = createCard("Grizzly Bears", p1);
        p1.getZone(ZoneType.Battlefield).add(bear);
        bear.setGameTimestamp(game.getNextTimestamp());
        bear.setSickness(false);
        
        // Simulate Detain effect
        // Detain adds "CARDNAME can't attack or block." and "CARDNAME's activated abilities can't be activated."
        bear.addIntrinsicKeyword("CARDNAME can't attack or block.");
        bear.addIntrinsicKeyword("CARDNAME's activated abilities can't be activated.");
        
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, p1);
        
        Assert.assertFalse(CombatUtil.canAttack(bear, null), "Detained creature should not be able to attack");
    }

    @Test
    public void testZilortha() {
        Game game = initAndCreateGame();
        Player p1 = game.getPlayers().get(0);

        // Zilortha: Lethal damage dealt to creatures you control is determined by their power rather than their toughness.
        Card zilortha = createCard("Grizzly Bears", p1);
        zilortha.setName("Zilortha, Strength Incarnate");
        zilortha.addIntrinsicKeyword("Lethal damage dealt to creatures you control is determined by their power rather than their toughness.");
        p1.getZone(ZoneType.Battlefield).add(zilortha);
        
        Card bear = createCard("Grizzly Bears", p1); // 2/2
        p1.getZone(ZoneType.Battlefield).add(bear);

        // Under Zilortha, lethal for a 2/2 is 2 (power). 
    // If we make it a 5/2, lethal should be 5.
    bear.setCounters(CounterEnumType.P1P0, 3); // Now 5/2

    Assert.assertEquals(bear.getNetPower(), 5);
        Assert.assertEquals(bear.getNetToughness(), 2);
        
        // The keyword is "Lethal damage dealt to CARDNAME is determined by its power rather than its toughness."
        // Zilortha grants this to others, but for the test we can add it directly to the card to verify getLethal()
        bear.addIntrinsicKeyword("Lethal damage dealt to CARDNAME is determined by its power rather than its toughness.");
        
        Assert.assertEquals(bear.getLethal(), 5, "Lethal damage should be based on power (5)");
    }

    @Test
    public void testUndiscoveredParadise() {
        Game game = initAndCreateGame();
        Player p1 = game.getPlayers().get(0);

        Card paradise = createCard("Undiscovered Paradise", p1);
        p1.getZone(ZoneType.Battlefield).add(paradise);
        paradise.setGameTimestamp(game.getNextTimestamp());

        // Verify initial state
        Assert.assertFalse(paradise.hasKeyword("During your next untap step, as you untap your permanents, return CARDNAME to its owner's hand."),
                "Undiscovered Paradise should not have the hidden keyword initially.");

        // Activate mana ability
        SpellAbility manaAb = null;
        for (SpellAbility sa : paradise.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Mana) {
                manaAb = sa;
                break;
            }
        }
        Assert.assertNotNull(manaAb, "Mana ability not found");
        
        manaAb.setActivatingPlayer(p1);
        paradise.setTapped(true);
        
        // Manually resolve subability if present, as AbilityMana might not auto-chain it in simulation
        if (manaAb.getSubAbility() != null) {
            manaAb.getSubAbility().setActivatingPlayer(p1);
            manaAb.getSubAbility().resolve();
        } else {
            // Fallback to main resolve if no subability found (shouldn't happen based on script)
            manaAb.resolve();
        }
        
        // Force static ability update to apply the new keyword from the Pump effect
        game.getAction().checkStaticAbilities();

        // Verify hidden keyword added
        Assert.assertTrue(paradise.hasKeyword("During your next untap step, as you untap your permanents, return CARDNAME to its owner's hand."),
                "Undiscovered Paradise should have the hidden keyword after use.");
    }

    @Test
    public void testGuardianBeast() {
        Game game = initAndCreateGame();
        Player p1 = game.getPlayers().get(0);
        Player p2 = game.getPlayers().get(1);

        Card beast = createCard("Guardian Beast", p1);
        Card artifact = createCard("Sol Ring", p1);

        // Use moveTo to ensure static abilities are recalculated
        game.getAction().moveTo(ZoneType.Battlefield, beast, null, null);
        game.getAction().moveTo(ZoneType.Battlefield, artifact, null, null);
        
        beast.setGameTimestamp(game.getNextTimestamp());
        artifact.setGameTimestamp(game.getNextTimestamp());
        
        // Force update
        game.getAction().checkStaticAbilities();

        // 1. Guardian Beast Untapped -> Artifact has keyword
        Assert.assertTrue(artifact.hasKeyword("Other players can't gain control of CARDNAME."),
                "Artifact should have protection keyword when Guardian Beast is untapped.");

        // 2. Guardian Beast Tapped -> Artifact loses keyword
        beast.setTapped(true);
        
        // Force update after state change
        game.getAction().checkStaticAbilities();
        
        Assert.assertFalse(artifact.hasKeyword("Other players can't gain control of CARDNAME."),
                "Artifact should NOT have protection keyword when Guardian Beast is tapped.");
    }
}
