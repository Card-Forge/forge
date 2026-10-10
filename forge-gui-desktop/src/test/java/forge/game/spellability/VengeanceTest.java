package forge.game.spellability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.ai.ComputerUtil;
import forge.card.CardStateName;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class VengeanceTest extends AITest {

    @Test
    public void castOnlyFromGraveyardThenExiled() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        addCards("Swamp", 6, p);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        Card inHand = addCardToZone("Hela, Mistress of Darkness", p, ZoneType.Hand);
        assertFalse(inHand.getAllPossibleAbilities(p, true).stream().anyMatch(SpellAbility::isVengeance));
        // nor through a permission to cast the card from where it is
        assertFalse(AbilityUtils.getSpellsFromPlayEffect(inHand, p, CardStateName.Original, true, null)
                .stream().anyMatch(SpellAbility::isVengeance));

        Card hela = addCardToZone("Hela, Mistress of Darkness", p, ZoneType.Graveyard);
        SpellAbility tutor = hela.getAllPossibleAbilities(p, true).get(0);
        assertTrue(tutor.isVengeance());

        assertTrue(ComputerUtil.handlePlayingSpellAbility(p, tutor, null));
        game.getStack().resolveStack();
        assertTrue(game.getCardState(hela).isInZone(ZoneType.Exile));
    }
}
