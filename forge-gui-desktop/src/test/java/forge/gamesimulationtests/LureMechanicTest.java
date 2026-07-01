package forge.gamesimulationtests;

import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LureMechanicTest extends SimulationTest {

    @Test
    public void testTreeshakerChimeraMustBeBlockedByAll() {
        Game game = initAndCreateGame();
        Player p1 = game.getPlayers().get(0);
        Player p2 = game.getPlayers().get(1);

        // Set up battlefield
        // P1 has Treeshaker Chimera
        Card chimera = createCard("Treeshaker Chimera", p1);
        p1.getZone(ZoneType.Battlefield).add(chimera);
        chimera.setGameTimestamp(game.getNextTimestamp());

        // P2 has 3 Grizzly Bears, all untapped and able to block
        Card bear1 = createCard("Grizzly Bears", p2);
        Card bear2 = createCard("Grizzly Bears", p2);
        Card bear3 = createCard("Grizzly Bears", p2);
        p2.getZone(ZoneType.Battlefield).add(bear1);
        p2.getZone(ZoneType.Battlefield).add(bear2);
        p2.getZone(ZoneType.Battlefield).add(bear3);
        bear1.setGameTimestamp(game.getNextTimestamp());
        bear2.setGameTimestamp(game.getNextTimestamp());
        bear3.setGameTimestamp(game.getNextTimestamp());

        // Ensure creatures are not sick
        chimera.setSickness(false);
        bear1.setSickness(false);
        bear2.setSickness(false);
        bear3.setSickness(false);

        // Advance to Combat
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, p1);

        // Declare Chimera as attacker
        Combat combat = new Combat(p1);
        combat.addAttacker(chimera, p2);
        
        // Add another attacker (normal)
        Card elf = createCard("Llanowar Elves", p1);
        p1.getZone(ZoneType.Battlefield).add(elf);
        elf.setGameTimestamp(game.getNextTimestamp());
        elf.setSickness(false);
        combat.addAttacker(elf, p2);

        game.getPhaseHandler().setCombat(combat);
        
        // Verify that blockers MUST block Chimera
        // Bear1 cannot block Elf because it must block Chimera
        boolean canBlockElf = CombatUtil.canBlock(elf, bear1, combat);
        Assert.assertFalse(canBlockElf, "Bear should not be able to block Elf because it must block Chimera");
        
        // Bear1 can block Chimera
        boolean canBlockChimera = CombatUtil.canBlock(chimera, bear1, combat);
        Assert.assertTrue(canBlockChimera, "Bear should be able to block Chimera");
    }
    
    @Test
    public void testLureEnchantment() {
        Game game = initAndCreateGame();
        Player p1 = game.getPlayers().get(0);
        Player p2 = game.getPlayers().get(1);

        // P1 has Grizzly Bears enchanted with Lure
        Card bear = createCard("Grizzly Bears", p1);
        p1.getZone(ZoneType.Battlefield).add(bear);
        bear.setGameTimestamp(game.getNextTimestamp());
        
        Card lure = createCard("Lure", p1);
        p1.getZone(ZoneType.Battlefield).add(lure);
        lure.attachToEntity(bear, null);
        // Note: verify Lure static abilities are applied. 
        // In simulation test, adding to zone and enchanting might not trigger update of static abilities automatically
        // unless we run game loop or simpler checks.
        // Let's ensure static abilities are active.
        lure.setGameTimestamp(game.getNextTimestamp());
        game.getAction().checkStaticAbilities();
        
        // P2 has a blocker
        Card blocker = createCard("Hill Giant", p2);
        p2.getZone(ZoneType.Battlefield).add(blocker);
        blocker.setGameTimestamp(game.getNextTimestamp());
        
        bear.setSickness(false);
        blocker.setSickness(false);

        // Add another attacker
        Card elf = createCard("Llanowar Elves", p1);
        p1.getZone(ZoneType.Battlefield).add(elf);
        elf.setGameTimestamp(game.getNextTimestamp());
        elf.setSickness(false);
        
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, p1);
        
        Combat combat = new Combat(p1);
        combat.addAttacker(bear, p2);
        combat.addAttacker(elf, p2);
        game.getPhaseHandler().setCombat(combat);
        
        // Blocker cannot block Elf because it must block Bear (Lure)
        boolean canBlockElf = CombatUtil.canBlock(elf, blocker, combat);
        Assert.assertFalse(canBlockElf, "Blocker should not be able to block Elf because it must block Lured Bear");
        
         // Blocker can block Bear
        boolean canBlockBear = CombatUtil.canBlock(bear, blocker, combat);
        Assert.assertTrue(canBlockBear, "Blocker should be able to block Lured Bear");
    }
}
