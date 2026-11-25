package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class CantPreventDamageAffectedTest extends AITest {

    private void whippoorwill(Player p, Card target) {
        Card bird = addCard("Whippoorwill", p);
        SpellAbility sa = bird.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().get();
        sa.setActivatingPlayer(p);
        sa.getTargets().add(target);
        AbilityUtils.resolve(sa);
    }

    private void shock(Player p, Card target) {
        Card shock = addCardToZone("Shock", p, ZoneType.Hand);
        SpellAbility sa = shock.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        sa.getTargets().add(target);
        AbilityUtils.resolve(sa);
    }

    @Test
    public void otherCreaturesKeepTheirPrevention() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bear = addCard("Runeclaw Bear", opp);
        Card giant = addCard("Callous Giant", opp);
        game.getAction().checkStateEffects(true);

        whippoorwill(p, bear);
        shock(p, giant);

        AssertJUnit.assertEquals(0, giant.getDamage());
    }

    @Test
    public void theAffectedCreatureLosesItsPrevention() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card giant = addCard("Callous Giant", opp);
        game.getAction().checkStateEffects(true);

        whippoorwill(p, giant);
        shock(p, giant);

        AssertJUnit.assertEquals(2, giant.getDamage());
    }
}
