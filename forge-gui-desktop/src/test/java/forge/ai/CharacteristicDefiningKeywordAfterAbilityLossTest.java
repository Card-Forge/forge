package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class CharacteristicDefiningKeywordAfterAbilityLossTest extends AITest {

    private void humble(Player caster, Card target) {
        Card humble = addCardToZone("Humble", caster, ZoneType.Hand);
        SpellAbility sa = humble.getFirstSpellAbility();
        sa.setActivatingPlayer(caster);
        sa.getTargets().add(target);
        AbilityUtils.resolve(sa);
        caster.getGame().getAction().checkStateEffects(true);
    }

    @Test
    public void devoidStaysColorlessAfterLosingAllAbilities() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card messenger = addCard("Emrakul's Messenger", opp);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(messenger.getColor().isColorless());

        humble(p, messenger);

        AssertJUnit.assertFalse(messenger.hasKeyword("Flying"));
        AssertJUnit.assertTrue(messenger.getColor().isColorless());
    }

    @Test
    public void changelingKeepsEveryCreatureTypeAfterLosingAllAbilities() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card changeling = addCard("Changeling Outcast", opp);
        Card colossus = addCard("Curious Colossus", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(changeling.getType().hasAllCreatureTypes());

        SpellAbility sa = AbilityFactory.getAbility(colossus, "TrigAnimateAll");
        sa.setActivatingPlayer(p);
        sa.getTargets().add(opp);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertFalse(changeling.hasKeyword("Changeling"));
        AssertJUnit.assertTrue(changeling.getType().hasAllCreatureTypes());
        AssertJUnit.assertEquals(1, changeling.getNetPower());
    }
}
