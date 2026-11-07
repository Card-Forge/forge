package forge.ai;

import java.util.Collections;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.ComboGuard;
import forge.ai.simulation.ComboPlan;
import forge.ai.simulation.DonateCombo;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Illusions of Grandeur and Donate under Flat Monte Carlo.
 *
 * <p>Report: the AI cast Illusions of Grandeur without a Donate. The script says not to (MustHaveInHand Donate, one
 * copy at most, mana for both, or life so low that the twenty are needed), and the heuristics obey it, but a play they
 * turn down comes back as a discovered play and a rollout only sees the twenty life gained: the twenty lost when the
 * enchantment leaves lie beyond what it plays out. The rule now holds for those plays too ({@code DonateCombo}).
 *
 * <p>Reading a failure: "donate-combo-incomplete" expected and null found means the AI may again take the loan without
 * the Donate; null expected and a reason found means the pair would never be set up.
 */
public class FlatMonteCarloDonateComboTest extends AITest {
    private Game game;
    private Player ai;

    private void setUp() {
        game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_TIMEOUT = 10;
        ai = game.getPlayers().get(1);
    }

    private SpellAbility cast(final String name) {
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.getName().equals(name)) {
                final SpellAbility sa = c.getFirstSpellAbility();
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new IllegalStateException(name + " is not in hand");
    }

    private String verdict(final String name) {
        return ComboGuard.problem(ai, cast(name), null, ComboPlan.EMPTY);
    }

    private void islands(final int count) {
        addCards("Island", count, ai);
    }

    // ------------------------------------------------------------------------------------------ Illusions of Grandeur

    @Test
    public void illusionsAloneIsNotCast() {
        setUp();
        islands(7);
        addCardToZone("Illusions of Grandeur", ai, ZoneType.Hand);
        AssertJUnit.assertEquals("seven lands, but no Donate to give it away", "donate-combo-incomplete",
                verdict("Illusions of Grandeur"));
    }

    @Test
    public void illusionsIsCastWithDonateAndTheManaForBoth() {
        setUp();
        islands(5);
        addCardToZone("Illusions of Grandeur", ai, ZoneType.Hand);
        addCardToZone("Donate", ai, ZoneType.Hand);
        AssertJUnit.assertNull("Donate in hand, five sources: the upkeep of 2 and Donate's 3 next turn", verdict("Illusions of Grandeur"));
    }

    @Test
    public void illusionsWaitsForCostReducersInHandToBeDeployed() {
        setUp();
        islands(7);
        addCardToZone("Illusions of Grandeur", ai, ZoneType.Hand);
        addCardToZone("Donate", ai, ZoneType.Hand);
        addCardToZone("Sapphire Medallion", ai, ZoneType.Hand);
        AssertJUnit.assertEquals("the Medallion is a setup card and should be deployed first", "donate-setup-first",
                verdict("Illusions of Grandeur"));
    }

    @Test
    public void illusionsCanUseAnAlreadyDeployedMedallionForNextTurnDonate() {
        setUp();
        islands(4);
        addCardToZone("Sapphire Medallion", ai, ZoneType.Battlefield);
        addCardToZone("Illusions of Grandeur", ai, ZoneType.Hand);
        addCardToZone("Donate", ai, ZoneType.Hand);
        AssertJUnit.assertNull("four lands plus one reduction covers the {2} upkeep and reduced Donate", verdict("Illusions of Grandeur"));
    }

    @Test
    public void channelDoesNotCountAsNextTurnManaForIllusions() {
        setUp();
        islands(4);
        final Card channel = addCardToZone("Channel", ai, ZoneType.Command);
        final SpellAbility channelMana = forge.game.ability.AbilityFactory.getAbility(channel, "ABMana");
        channel.addSpellAbility(channelMana);
        channelMana.setActivatingPlayer(ai);
        addCardToZone("Illusions of Grandeur", ai, ZoneType.Hand);
        addCardToZone("Donate", ai, ZoneType.Hand);
        AssertJUnit.assertFalse("a Command-zone Channel effect must not count as a renewable next-turn source",
                forge.ai.ComputerUtilMana.getManaSourcesForNextTurn(ai).contains(channel));
        AssertJUnit.assertEquals("four lands and temporary Channel mana are not a safe next-turn setup",
                "donate-combo-incomplete", verdict("Illusions of Grandeur"));
    }

    @Test
    public void flatMcCanDiscoverARemovedBlueCostReducer() {
        setUp();
        final Card medallion = addCardToZone("Sapphire Medallion", ai, ZoneType.Hand);
        final SpellAbility cast = medallion.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertTrue("Sapphire Medallion is marked AI:RemoveDeck:Random",
                ComputerUtilCard.isCardRemRandomDeck(medallion));
        AssertJUnit.assertTrue("AI:RemoveDeck medallions with ReduceCost statics must be candidates for FlatMC",
                ((PlayerControllerAi) ai.getController()).getAi().isRemoveDeckCostReductionPermanent(cast));
    }

    @Test
    public void illusionsWaitsForTheManaToPayTheUpkeepAndDonate() {
        setUp();
        islands(3);
        addCardToZone("Illusions of Grandeur", ai, ZoneType.Hand);
        addCardToZone("Donate", ai, ZoneType.Hand);
        AssertJUnit.assertEquals("three sources and no land to come: the pair cannot be finished next turn",
                "donate-combo-incomplete", verdict("Illusions of Grandeur"));
    }

    @Test
    public void aLandInHandMakesTheFifthSource() {
        setUp();
        islands(4);
        addCardToZone("Island", ai, ZoneType.Hand);
        addCardToZone("Illusions of Grandeur", ai, ZoneType.Hand);
        addCardToZone("Donate", ai, ZoneType.Hand);
        AssertJUnit.assertNull("four sources and the land drop of next turn", verdict("Illusions of Grandeur"));
    }

    @Test
    public void aSecondIllusionsIsNotCast() {
        setUp();
        islands(7);
        addCardToZone("Illusions of Grandeur", ai, ZoneType.Battlefield);
        addCardToZone("Illusions of Grandeur", ai, ZoneType.Hand);
        addCardToZone("Donate", ai, ZoneType.Hand);
        AssertJUnit.assertEquals("one is out already: the second would only add upkeep", "donate-combo-incomplete",
                verdict("Illusions of Grandeur"));
    }

    @Test
    public void illusionsIsCastWhenTheLifeIsLowEnoughToNeedTheTwenty() {
        setUp();
        islands(4);
        ai.setLife(3, null);
        addCardToZone("Illusions of Grandeur", ai, ZoneType.Hand);
        AssertJUnit.assertNull("the script casts it as a last resort below 4 life, Donate or not", verdict("Illusions of Grandeur"));
    }

    @Test
    public void embargoAlsoWantsItsDonate() {
        setUp();
        islands(5);
        addCardToZone("Embargo", ai, ZoneType.Hand);
        AssertJUnit.assertEquals("Embargo without Donate", "donate-combo-incomplete", verdict("Embargo"));
        addCardToZone("Donate", ai, ZoneType.Hand);
        AssertJUnit.assertNull("Embargo with Donate in hand", verdict("Embargo"));
    }

    // ------------------------------------------------------------------------------------------ Donate

    @Test
    public void donateWithNothingToGiveIsNotCast() {
        setUp();
        islands(5);
        addCardToZone("Donate", ai, ZoneType.Hand);
        AssertJUnit.assertEquals("only lands in play: nothing worth giving the opponent", "donate-nothing-to-give",
                verdict("Donate"));
    }

    @Test
    public void donateIsCastWhenIllusionsIsInPlay() {
        setUp();
        islands(5);
        addCardToZone("Illusions of Grandeur", ai, ZoneType.Battlefield);
        addCardToZone("Donate", ai, ZoneType.Hand);
        AssertJUnit.assertNull("Illusions of Grandeur is the permanent to give", verdict("Donate"));
    }

    // ------------------------------------------------------------------------------------------ nothing else changes

    @Test
    public void otherSpellsAreNotAffected() {
        setUp();
        islands(5);
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        AssertJUnit.assertNull("an instant", verdict("Lightning Bolt"));
        AssertJUnit.assertNull("a creature without a cast preference", verdict("Grizzly Bears"));
    }

    @Test(timeOut = 240_000)
    public void flatMcCastsDonateForIllusionsAlreadyOnTheBattlefield() {
        setUp();
        final Player opp = game.getPlayers().get(0);
        fillLibrary(ai, 20);
        fillLibrary(opp, 20);
        islands(3); // Donate costs {2}{U}
        addCardToZone("Illusions of Grandeur", ai, ZoneType.Battlefield);
        addCardToZone("Donate", ai, ZoneType.Hand);
        addCard("Hill Giant", opp);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        final java.util.List<SpellAbility> chosen = ((PlayerControllerAi) ai.getController()).chooseSpellAbilityToPlay();
        final String play = chosen == null || chosen.isEmpty() ? null : chosen.get(0).getHostCard().getName();
        AssertJUnit.assertEquals("with Illusions in play and three blue mana, FlatMC should complete the Donate combo",
                "Donate", play);
    }

    // ------------------------------------------------------------------------------------------ the AI's next play

    @Test(timeOut = 240_000)
    public void flatMcDoesNotCastIllusionsWithoutDonate() {
        setUp();
        fillLibrary(ai, 20);
        fillLibrary(game.getPlayers().get(0), 20);
        islands(7);
        addCardToZone("Illusions of Grandeur", ai, ZoneType.Hand);
        addCard("Hill Giant", game.getPlayers().get(0));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        final java.util.List<SpellAbility> chosen = ((PlayerControllerAi) ai.getController()).chooseSpellAbilityToPlay();
        final String play = chosen == null || chosen.isEmpty() ? null : chosen.get(0).getHostCard().getName();
        AssertJUnit.assertFalse("seven lands and no Donate, but the AI chose: " + play, "Illusions of Grandeur".equals(play));
    }

    @Test(timeOut = 240_000)
    public void flatMcUsesLethalRemovalBeforeGushWhenIllusionsWasDonated() {
        setUp();
        final Player opp = game.getPlayers().get(0);
        fillLibrary(ai, 20);
        fillLibrary(opp, 20);
        islands(2); // Chain of Vapor is payable; Gush can instead return both Islands
        final Card illusions = createCard("Illusions of Grandeur", ai);
        illusions.setGameTimestamp(game.getNextTimestamp());
        illusions.setController(opp, game.getNextTimestamp());
        opp.getZone(ZoneType.Battlefield).add(illusions);
        addCardToZone("Chain of Vapor", ai, ZoneType.Hand);
        addCardToZone("Gush", ai, ZoneType.Hand);
        opp.setLife(20, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        final java.util.List<SpellAbility> chosen = ((PlayerControllerAi) ai.getController()).chooseSpellAbilityToPlay();
        final String play = chosen == null || chosen.isEmpty() ? null : chosen.get(0).getHostCard().getName();
        AssertJUnit.assertEquals("the lethal bounce must be chosen before drawing with Gush", "Chain of Vapor", play);
        AssertJUnit.assertTrue("Chain of Vapor must already target the donated Illusions",
                chosen.get(0).getTargets().contains(illusions));
    }

    @Test(timeOut = 240_000)
    public void flatMcSetsRepealXToTheDonatedIllusionsManaValue() {
        setUp();
        final Player opp = game.getPlayers().get(0);
        fillLibrary(ai, 20);
        fillLibrary(opp, 20);
        islands(5); // Repeal for X=4 costs five mana; Gush can instead return two Islands
        final Card illusions = createCard("Illusions of Grandeur", ai);
        illusions.setGameTimestamp(game.getNextTimestamp());
        illusions.setController(opp, game.getNextTimestamp());
        opp.getZone(ZoneType.Battlefield).add(illusions);
        addCardToZone("Repeal", ai, ZoneType.Hand);
        addCardToZone("Gush", ai, ZoneType.Hand);
        opp.setLife(20, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        final java.util.List<SpellAbility> chosen = ((PlayerControllerAi) ai.getController()).chooseSpellAbilityToPlay();
        final String play = chosen == null || chosen.isEmpty() ? null : chosen.get(0).getHostCard().getName();
        AssertJUnit.assertEquals("Repeal should remove donated Illusions before Gush", "Repeal", play);
        AssertJUnit.assertEquals("Repeal X must match Illusions' mana value", Integer.valueOf(4),
                chosen.get(0).getXManaCostPaid());
        AssertJUnit.assertTrue("Repeal must target the donated Illusions", chosen.get(0).getTargets().contains(illusions));
    }

    @Test
    public void donatedIllusionsAboveTwentyLifeIsNotAnImmediateWin() {
        setUp();
        final Player opp = game.getPlayers().get(0);
        islands(1);
        final Card illusions = createCard("Illusions of Grandeur", ai);
        illusions.setGameTimestamp(game.getNextTimestamp());
        illusions.setController(opp, game.getNextTimestamp());
        opp.getZone(ZoneType.Battlefield).add(illusions);
        opp.setLife(21, null);
        addCardToZone("Chain of Vapor", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertNull("losing 20 life from 21 does not finish the game",
                DonateCombo.sureFinish(ai, Collections.singletonList(cast("Chain of Vapor"))));
    }
}
