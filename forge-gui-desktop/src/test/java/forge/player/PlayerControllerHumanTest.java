package forge.player;

import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class PlayerControllerHumanTest extends SimulationTest {
    @Test
    public void declareBlockersAutoPassInterruptIgnoresManaAbilitiesOnly() {
        Game game = initAndCreateGame();
        Player player = game.getPlayers().get(1);

        addCardToZone("Mountain", player, ZoneType.Battlefield);

        setDeclareBlockersPriority(game, player);

        AssertJUnit.assertFalse(PlayerControllerHuman.hasPlayableNonManaAbility(player));
    }

    @Test
    public void declareBlockersAutoPassInterruptIgnoresUnaffordableInstantCombatTrick() {
        Game game = initAndCreateGame();
        Player player = game.getPlayers().get(1);

        addCardToZone("Grizzly Bears", player, ZoneType.Battlefield);
        addCardToZone("Brute Force", player, ZoneType.Hand);

        setDeclareBlockersPriority(game, player);

        AssertJUnit.assertFalse(PlayerControllerHuman.hasPlayableNonManaAbility(player));
    }

    @Test
    public void declareBlockersAutoPassInterruptFindsAffordableInstantCombatTrick() {
        Game game = initAndCreateGame();
        Player player = game.getPlayers().get(1);

        addCardToZone("Grizzly Bears", player, ZoneType.Battlefield);
        addCardToZone("Mountain", player, ZoneType.Battlefield);
        addCardToZone("Brute Force", player, ZoneType.Hand);

        setDeclareBlockersPriority(game, player);

        AssertJUnit.assertTrue(PlayerControllerHuman.hasPlayableNonManaAbility(player));
    }

    @Test
    public void declareBlockersAutoPassInterruptIgnoresSorceryTiming() {
        Game game = initAndCreateGame();
        Player player = game.getPlayers().get(1);

        addCardToZone("Wrath of God", player, ZoneType.Hand);

        setDeclareBlockersPriority(game, player);

        AssertJUnit.assertFalse(PlayerControllerHuman.hasPlayableNonManaAbility(player));
    }

    @Test
    public void declareBlockersAutoPassInterruptRequiresPlayerToBeAttacked() {
        Game game = initAndCreateThreePlayerGame();
        Player attackingPlayer = game.getPlayers().get(0);
        Player attackedPlayer = game.getPlayers().get(1);
        Player uninvolvedPlayer = game.getPlayers().get(2);

        Card attacker = addCardToZone("Grizzly Bears", attackingPlayer, ZoneType.Battlefield);
        addCardToZone("Grizzly Bears", attackedPlayer, ZoneType.Battlefield);
        addCardToZone("Mountain", attackedPlayer, ZoneType.Battlefield);
        addCardToZone("Brute Force", attackedPlayer, ZoneType.Hand);
        addCardToZone("Grizzly Bears", uninvolvedPlayer, ZoneType.Battlefield);
        addCardToZone("Mountain", uninvolvedPlayer, ZoneType.Battlefield);
        addCardToZone("Brute Force", uninvolvedPlayer, ZoneType.Hand);

        Combat combat = new Combat(attackingPlayer);
        combat.addAttacker(attacker, attackedPlayer);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, attackingPlayer, false);
        game.getPhaseHandler().setCombat(combat);

        game.getPhaseHandler().setPriority(uninvolvedPlayer);
        AssertJUnit.assertFalse(PlayerControllerHuman.shouldInterruptDeclareBlockersAutoPass(uninvolvedPlayer));

        game.getPhaseHandler().setPriority(attackedPlayer);
        AssertJUnit.assertTrue(PlayerControllerHuman.shouldInterruptDeclareBlockersAutoPass(attackedPlayer));
    }

    private void setDeclareBlockersPriority(Game game, Player player) {
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, player);
        game.getAction().checkStateEffects(true);
    }
}
