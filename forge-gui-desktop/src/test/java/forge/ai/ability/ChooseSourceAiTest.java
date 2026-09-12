package forge.ai.ability;

import java.util.List;
import java.util.Map;

import org.testng.AssertJUnit;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class ChooseSourceAiTest extends AITest {
    private SpellAbility preventionAbility(String name, Player ai) {
        Card card = createCard(name, ai);
        for (SpellAbility sa : card.getSpellAbilities()) {
            if (sa.getApi() == ApiType.ChooseSource) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new AssertionError("Missing source choice on " + name);
    }

    private Card separator(Game game) {
        Card card = new Card(-1, game);
        card.setName("--PERMANENTS:--");
        return card;
    }

    private Card choose(SpellAbility sa, Card... options) {
        return new ChooseSourceAi().chooseSingleCard(sa.getActivatingPlayer(), sa,
                List.of(options), false, null, Map.of());
    }

    private SpellAbility damage(Card source, int amount) {
        SpellAbility sa = AbilityFactory.getAbility("DB$ DealDamage | Defined$ Opponent | NumDmg$ " + amount, source);
        sa.setActivatingPlayer(source.getController());
        return sa;
    }

    @DataProvider(name = "fallbackSources")
    public Object[][] fallbackSources() {
        return new Object[][] {
                {"Black Vise", false},
                {"Forest", true},
                {"Grizzly Bears", true}
        };
    }

    @Test(dataProvider = "fallbackSources")
    public void mandatoryChoiceReturnsARealSource(String name, boolean ownedByAi) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card source = addCard(name, game.getPlayers().get(ownedByAi ? 1 : 0));
        SpellAbility sa = preventionAbility("Reverse Damage", ai);

        // Call the chooser directly: a null or separator would make ChooseSourceEffect retry forever.
        AssertJUnit.assertEquals(source, choose(sa, separator(game), source));
    }

    @Test
    public void fallbackPrefersOpponentCreatureThenOwnNoncreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        Card forest = addCard("Forest", ai);
        Card vise = addCard("Black Vise", opponent);
        Card bear = addCard("Grizzly Bears", opponent);
        SpellAbility sa = preventionAbility("Reverse Damage", ai);

        AssertJUnit.assertEquals(bear, choose(sa, separator(game), forest, vise, bear));
        AssertJUnit.assertEquals(forest, choose(sa, separator(game), forest, vise));
    }

    @Test
    public void fallbackNeverChoosesASectionHeading() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        Card vise = addCard("Black Vise", opponent);
        Card rack = addCard("The Rack", opponent);
        SpellAbility sa = preventionAbility("Reverse Damage", ai);

        for (int i = 0; i < 100; i++) {
            Card chosen = choose(sa, separator(game), vise, rack);
            AssertJUnit.assertTrue("Choose an offered source, not null or a section heading",
                    chosen == vise || chosen == rack);
        }
    }

    @Test
    public void choosesUnblockedAttackerBeforeIdleCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        Card attacker = addCard("Grizzly Bears", opponent);
        Card idle = addCard("Craw Wurm", opponent);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, opponent);
        Combat combat = new Combat(opponent);
        combat.addAttacker(attacker, ai);
        combat.setBlocked(attacker, false);
        game.getPhaseHandler().setCombat(combat);

        AssertJUnit.assertEquals(attacker, choose(preventionAbility("Reverse Damage", ai),
                separator(game), attacker, idle));
    }

    @Test
    public void blackViseThreatCanDisappearWhenReverseDamageLeavesHand() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card vise = addCard("Black Vise", game.getPlayers().get(0));
        vise.setChosenPlayer(ai);
        SpellAbility damage = AbilityFactory.getAbility(vise.getSVar("TrigDamage"), vise);
        damage.setActivatingPlayer(vise.getController());
        SpellAbility prevention = preventionAbility("Reverse Damage", ai);
        Card reverseDamage = prevention.getHostCard();
        ai.getZone(ZoneType.Hand).add(reverseDamage);
        for (int i = 0; i < 4; i++) {
            addCardToZone("Plains", ai, ZoneType.Hand);
        }
        AssertJUnit.assertEquals(1, AbilityUtils.calculateAmount(vise, damage.getParam("NumDmg"), damage));
        ai.getZone(ZoneType.Hand).remove(reverseDamage);
        AssertJUnit.assertEquals(0, AbilityUtils.calculateAmount(vise, damage.getParam("NumDmg"), damage));
        game.getStack().add(damage);

        // There is no longer predicted damage, but resolving Reverse Damage still requires a source.
        AssertJUnit.assertEquals(vise, choose(prevention, separator(game), vise));
        AbilityUtils.resolve(prevention);
        AssertJUnit.assertEquals(List.of(vise), prevention.getHostCard().getChosenCards());
        AbilityUtils.resolve(damage);
        AssertJUnit.assertEquals(20, ai.getLife());
    }

    @DataProvider(name = "pendingDamage")
    public Object[][] pendingDamage() {
        return new Object[][] {{true}, {false}};
    }

    @Test(dataProvider = "pendingDamage")
    public void reverseDamagePreventsOnlyChosenSourcesNextDamage(boolean pendingDamage) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        Card vise = addCard("Black Vise", opponent);
        SpellAbility damage = damage(vise, 3);
        if (pendingDamage) {
            game.getStack().add(damage);
        }
        SpellAbility prevention = preventionAbility("Reverse Damage", ai);

        // Fail before resolving if the chooser would hang; then exercise the real card's subability.
        AssertJUnit.assertEquals(vise, choose(prevention, separator(game), vise));
        AbilityUtils.resolve(prevention);
        AssertJUnit.assertEquals(List.of(vise), prevention.getHostCard().getChosenCards());

        Card otherSource = addCard("The Rack", opponent);
        AbilityUtils.resolve(damage(otherSource, 2));
        AssertJUnit.assertEquals("Other sources must still deal damage", 18, ai.getLife());
        AbilityUtils.resolve(damage);
        AssertJUnit.assertEquals("Prevent three damage and gain three life", 21, ai.getLife());
        AbilityUtils.resolve(damage(vise, 3));
        AssertJUnit.assertEquals("The prevention effect is consumed after one damage event", 18, ai.getLife());
    }

    @Test
    public void circleOfProtectionFallbackRespectsColorRestriction() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        Card redSource = addCard("Manabarbs", opponent);
        Card greenSource = addCard("Grizzly Bears", opponent);
        SpellAbility greenDamage = damage(greenSource, 2);
        game.getStack().add(greenDamage);
        SpellAbility prevention = preventionAbility("Circle of Protection: Red", ai);

        AssertJUnit.assertEquals(redSource, choose(prevention, separator(game), redSource));
        AbilityUtils.resolve(prevention);
        AbilityUtils.resolve(greenDamage);
        AssertJUnit.assertEquals("The green source is not prevented", 18, ai.getLife());
        AbilityUtils.resolve(damage(redSource, 2));
        AssertJUnit.assertEquals("The red source is prevented without gaining life", 18, ai.getLife());
        AbilityUtils.resolve(damage(redSource, 2));
        AssertJUnit.assertEquals("Only the next damage event is prevented", 16, ai.getLife());
    }
}
