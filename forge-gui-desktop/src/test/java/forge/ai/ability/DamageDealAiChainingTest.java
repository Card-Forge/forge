package forge.ai.ability;

import forge.ai.AITest;
import forge.ai.AiCardMemory;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import org.apache.commons.lang3.tuple.Pair;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Random;

public class DamageDealAiChainingTest extends AITest {
    private Random originalRandom;

    @BeforeMethod
    public void enableChainingDeterministically() {
        originalRandom = MyRandom.getRandom();
        MyRandom.setRandom(new Random(0) {
            @Override
            public int nextInt() {
                // MyRandom delegates its bit generation to this unbounded call.
                return 0;
            }
        });
    }

    @AfterMethod(alwaysRun = true)
    public void restoreRandom() {
        if (originalRandom != null) {
            MyRandom.setRandom(originalRandom);
        }
    }

    @Test(groups = { "UnitTest", "fast" })
    public void doesNotCountPlayerOnlyDamageTowardKillingACreature() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        addCards("Mountain", 6, ai);
        final SpellAbility shock = spell("Shock", ai);
        final SpellAbility axe = spell("Lava Axe", ai);
        final Card baloth = addCard("Rumbling Baloth", opponent);
        game.getAction().checkStateEffects(true);

        Assert.assertTrue(shock.canTarget(baloth));
        Assert.assertFalse(axe.canTarget(baloth));
        Assert.assertNull(DamageDealAi.getDamagingSAToChain(ai, shock, "2"),
                "Lava Axe cannot follow Shock against every target Shock may choose");
        final boolean willing = SpellApiToAi.Converter.get(shock).canPlayWithSubs(ai, shock).willingToPlay();

        Assert.assertFalse(willing && shock.getTargets().contains(baloth),
                "Shock cannot kill an undamaged Baloth using Lava Axe's player-only damage");
        Assert.assertTrue(AiCardMemory.isMemorySetEmpty(ai,
                AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_NEXT_SPELL),
                "An incompatible follow-up spell must not reserve mana");
    }

    @Test(groups = { "UnitTest", "fast" })
    public void checksProtectionEvenWhenBothSpellsHaveAnyTarget() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        addCards("Mountain", 4, ai);
        final SpellAbility ghostfire = spell("Ghostfire", ai);
        final SpellAbility shock = spell("Shock", ai);
        final Card knight = addCard("Silver Knight", opponent);
        game.getAction().checkStateEffects(true);

        Assert.assertTrue(ghostfire.canTarget(knight), "Colorless Ghostfire bypasses protection from red");
        Assert.assertFalse(shock.canTarget(knight));
        Assert.assertNull(DamageDealAi.getDamagingSAToChain(ai, ghostfire, "3"),
                "Matching printed target descriptions do not make the actual legal targets equal");
    }

    @Test(groups = { "UnitTest", "fast" })
    public void combinesCompatibleBurnSpellsToKillACreature() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        addCards("Mountain", 2, ai);
        final SpellAbility shock = spell("Shock", ai);
        final SpellAbility bolt = spell("Lightning Bolt", ai);
        final Card baloth = addCard("Rumbling Baloth", opponent);
        game.getAction().checkStateEffects(true);

        final Pair<SpellAbility, Integer> chain = DamageDealAi.getDamagingSAToChain(ai, shock, "2");
        Assert.assertNotNull(chain);
        Assert.assertSame(chain.getKey(), bolt);
        Assert.assertEquals(chain.getValue().intValue(), 3);
        Assert.assertTrue(SpellApiToAi.Converter.get(shock).canPlayWithSubs(ai, shock).willingToPlay());
        Assert.assertTrue(shock.getTargets().contains(baloth));
        Assert.assertTrue(bolt.canTarget(baloth));
    }

    @Test(groups = { "UnitTest", "fast" })
    public void combinesPlayerOnlyBurnWithAnyTargetBurnForLethal() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        addCards("Mountain", 6, ai);
        final SpellAbility axe = spell("Lava Axe", ai);
        final SpellAbility shock = spell("Shock", ai);
        opponent.setLife(7, null);
        moveToMain2(game, ai);

        final Pair<SpellAbility, Integer> chain = DamageDealAi.getDamagingSAToChain(ai, axe, "5");
        Assert.assertNotNull(chain, "An Any-target spell can follow a player-only spell");
        Assert.assertSame(chain.getKey(), shock);
        Assert.assertEquals(chain.getValue().intValue(), 2);
        Assert.assertTrue(SpellApiToAi.Converter.get(axe).canPlayWithSubs(ai, axe).willingToPlay());
        Assert.assertTrue(axe.getTargets().contains(opponent));
        Assert.assertTrue(shock.canTarget(opponent));
    }

    @Test(groups = { "UnitTest", "fast" })
    public void keepsNarrowerBurnWhenEveryCurrentTargetIsShared() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCards("Mountain", 6, ai);
        final SpellAbility shock = spell("Shock", ai);
        final SpellAbility axe = spell("Lava Axe", ai);
        game.getAction().checkStateEffects(true);

        final Pair<SpellAbility, Integer> chain = DamageDealAi.getDamagingSAToChain(ai, shock, "2");
        Assert.assertNotNull(chain,
                "With only lands on the battlefield, Lava Axe covers all current Shock targets");
        Assert.assertSame(chain.getKey(), axe);
    }

    @Test(groups = { "UnitTest", "fast" })
    public void doesNotPlanAChainWithoutEnoughManaForBothSpells() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        addCard("Mountain", ai);
        final SpellAbility shock = spell("Shock", ai);
        spell("Lightning Bolt", ai);
        final Card baloth = addCard("Rumbling Baloth", opponent);
        game.getAction().checkStateEffects(true);

        Assert.assertNull(DamageDealAi.getDamagingSAToChain(ai, shock, "2"));
        final boolean willing = SpellApiToAi.Converter.get(shock).canPlayWithSubs(ai, shock).willingToPlay();
        Assert.assertFalse(willing && shock.getTargets().contains(baloth));
        Assert.assertTrue(AiCardMemory.isMemorySetEmpty(ai,
                AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_NEXT_SPELL));
    }

    private SpellAbility spell(final String name, final Player player) {
        final SpellAbility ability = addCardToZone(name, player, ZoneType.Hand).getFirstSpellAbility();
        ability.setActivatingPlayer(player);
        return ability;
    }
}
