package forge.ai.controller;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.apache.commons.lang3.StringUtils;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class ManaPreferenceTest extends AITest {

    @Test
    public void catharsisTapsItsDualLandsForTheScarcerColour() {
        String spent = castCatharsis(false, "Sacred Foundry", 2, "Mountain", 4);
        AssertJUnit.assertEquals(spent, 4, StringUtils.countMatches(spent, "R"));
        AssertJUnit.assertEquals(spent, 2, StringUtils.countMatches(spent, "W"));
    }

    @Test
    public void evokedCatharsisSpendsTheColourItHasTwoOf() {
        String spent = castCatharsis(true, "Mountain", 1, "Plains", 3);
        AssertJUnit.assertEquals(spent, 2, StringUtils.countMatches(spent, "W"));
    }

    private String castCatharsis(boolean evoke, String firstLand, int first, String secondLand, int second) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        addCards(firstLand, first, p);
        addCards(secondLand, second, p);
        Card catharsis = addCardToZone("Catharsis", p, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        for (SpellAbility sa : catharsis.getAllPossibleAbilities(p, false)) {
            if (sa.isSpell() && sa.isEvoke() == evoke) {
                sa.setActivatingPlayer(p);
                p.getController().playChosenSpellAbility(sa);
            }
        }
        return StringUtils.join(game.getStack().peekAbility().getPayingMana(), " ");
    }
}
