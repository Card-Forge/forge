package forge.game.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.combat.Combat;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.Map;

import static junit.framework.Assert.assertEquals;

/** Camouflage asks a human defender to sort blockers into piles, after dropping creatures that can't block. */
public class CamouflageEffectTest extends AITest {

    /** Camouflage only asks a non-AI declarer; this AI controller poses as one and declines to block. */
    private static class PosingHuman extends PlayerControllerAi {
        PosingHuman(Game game, Player p) {
            super(game, p, p.getLobbyPlayer());
        }

        @Override
        public boolean isAI() {
            return false;
        }

        @Override
        public CardCollectionView chooseCardsForEffect(CardCollectionView sourceList, SpellAbility sa, String title,
                int min, int max, boolean isOptional, Map<String, Object> params) {
            return new CardCollection();
        }
    }

    @Test
    public void tappedDefenderIsDroppedFromThePool() {
        Game game = initAndCreateGame();
        Player attacker = game.getPlayers().get(1);
        Player defender = game.getPlayers().get(0);
        defender.dangerouslySetController(new PosingHuman(game, defender));

        Card bears = addCard("Grizzly Bears", attacker);
        bears.setSickness(false);
        Card tapped = addCard("Runeclaw Bear", defender);
        tapped.setTapped(true);
        Combat combat = new Combat(attacker);
        combat.addAttacker(bears, defender);
        game.getPhaseHandler().setCombat(combat);

        SpellAbility sa = AbilityFactory.getAbility("DB$ Camouflage | Defined$ Player.Opponent | Defender$ Player.Opponent",
                bears);
        sa.setActivatingPlayer(attacker);
        AbilityUtils.resolve(sa);

        assertEquals("the tapped creature can't block, so nothing blocks", 0, combat.getAllBlockers().size());
    }
}
