package forge.player;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardView;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.input.InputAttack;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.gui.interfaces.FakeGuiGame.CombatDamagePrompt;
import forge.gui.interfaces.IGuiGame;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.util.Lang;
import forge.util.Localizer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Characterization tests for the engine-to-UI seam in {@link PlayerControllerHuman}: each test pins
 * when the controller asks the player (through {@code IGuiGame}) and how it turns the answer back
 * into engine objects. They exist to protect moving that logic out of the controller. If you change
 * this behavior on purpose, update the test(s) accordingly; no sign-off needed.
 */
public class PlayerControllerHumanShould {

    private IGuiBase originalGuiBase;

    @BeforeMethod
    public void installHeadlessGuiBase() {
        originalGuiBase = GuiBase.getInterface();
        Localizer.getInstance().initialize("en-US", "res/languages/");
        Lang.createInstance("en-US");
        IGuiBase guiBase = mock(IGuiBase.class);
        when(guiBase.isGuiThread()).thenReturn(false);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(guiBase).invokeInEdtLater(any(Runnable.class));
        GuiBase.setInterface(guiBase);
    }

    @AfterMethod(alwaysRun = true)
    public void restoreGuiBase() {
        GuiBase.setInterface(originalGuiBase);
    }

    @Test
    public void stillOfferLegalAttackInputWhenCombatPriorityStopsAreDisabled() throws InterruptedException {
        HumanControlledGame g = new HumanControlledGame();
        Card attacker = addLegalAttacker(g);
        g.controller.getYieldController().setPref(FPref.UI_SHOW_ACTIONABLE_HIGHLIGHTS, "false");
        IGuiGame fakeGui = g.gui.asGui();
        for (PhaseType phase : List.of(PhaseType.COMBAT_BEGIN,
                PhaseType.COMBAT_DECLARE_ATTACKERS, PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            doReturn(true).when(fakeGui).isUiSetToSkipPhase(g.human.getView(), phase);
            assertThat(g.controller.isUiSetToSkipPhase(g.human.getView(), phase)).isTrue();
        }
        doReturn(g.game.getView()).when(fakeGui).getGameView();
        completeInputImmediately(fakeGui);

        Combat combat = new Combat(g.human);
        assertThat(CombatUtil.canAttack(attacker, g.opponent))
                .as("the untapped, nonsick creature may attack the opponent")
                .isTrue();
        g.controller.declareAttackers(g.human, combat);

        assertThat(g.controller.getInputQueue().getInput()).isInstanceOf(InputAttack.class);
        g.controller.getInputQueue().clearInputs();
    }

    @Test
    public void anExplicitEndOfTurnYieldMayChooseTheLegalEmptyAttackDeclaration() throws InterruptedException {
        HumanControlledGame g = new HumanControlledGame();
        Card attacker = addLegalAttacker(g);
        IGuiGame fakeGui = g.gui.asGui();
        doReturn(g.game.getView()).when(fakeGui).getGameView();
        completeInputImmediately(fakeGui);
        Combat combat = new Combat(g.human);

        assertThat(CombatUtil.canAttack(attacker, g.opponent)).isTrue();
        assertThat(CombatUtil.validateAttackers(combat)).isTrue();
        g.controller.autoPassUntilEndOfTurn();
        g.controller.declareAttackers(g.human, combat);

        assertThat(combat.getAttackers()).isEmpty();
        assertThat(g.controller.getInputQueue().getInput()).isNull();
    }

    // --- assignCombatDamage: when the player is asked ---

    @Test
    public void assignAllCombatDamageToTheOnlyBlockerWithoutAskingThePlayer() {
        // With one blocker and no trample there is no choice to make, so the controller decides on
        // its own. The fake fails on any unscripted prompt, so reaching the assertion proves no
        // dialog was shown.
        HumanControlledGame g = new HumanControlledGame();
        Card attacker = g.card(g.human, "Attacker", "Types:Creature Beast", "PT:3/3");
        Card blocker = g.card(g.opponent, "Blocker", "Types:Creature Bear", "PT:2/2");

        Map<Card, Integer> damage = g.controller.assignCombatDamage(
                attacker, new CardCollection(blocker), null, 3, g.opponent, false);

        assertThat(damage).containsExactly(entry(blocker, 3));
    }

    @Test
    public void assignAllCombatDamageToTheDefenderWhenUnblockedWithoutAskingThePlayer() {
        // An empty blocker list takes the same no-choice branch; the defender is represented by a
        // null key, which callers rely on to route damage to the player or planeswalker.
        HumanControlledGame g = new HumanControlledGame();
        Card attacker = g.card(g.human, "Attacker", "Types:Creature Beast", "PT:3/3");

        Map<Card, Integer> damage = g.controller.assignCombatDamage(
                attacker, new CardCollection(), null, 3, g.opponent, false);

        assertThat(damage).containsExactly(entry(null, 3));
    }

    @Test
    public void notAskThePlayerWhenATramplerHasNoDefenderToTrampleOver() {
        // Trample only opens the dialog together with a defender ("hasKeyword(TRAMPLE) && defender
        // != null"). Without one, a single-blocker trampler falls through to the no-choice branch.
        HumanControlledGame g = new HumanControlledGame();
        Card attacker = g.card(g.human, "Trampler", "Types:Creature Beast", "PT:5/5", "K:Trample");
        Card blocker = g.card(g.opponent, "Blocker", "Types:Creature Bear", "PT:2/2");

        Map<Card, Integer> damage = g.controller.assignCombatDamage(
                attacker, new CardCollection(blocker), null, 5, null, false);

        assertThat(damage).containsExactly(entry(blocker, 5));
    }

    @Test
    public void askThePlayerWhenATramplerIsBlocked() {
        // Trample with a defender lets the player split damage between blocker and defender, so
        // the controller must prompt. This pins what the prompt is given: views of the attacker,
        // blockers and defender, the full damage amount, and the two flags as passed through.
        HumanControlledGame g = new HumanControlledGame();
        Card attacker = g.card(g.human, "Trampler", "Types:Creature Beast", "PT:5/5", "K:Trample");
        Card blocker = g.card(g.opponent, "Blocker", "Types:Creature Bear", "PT:2/2");
        g.gui.answerCombatDamageWith(prompt -> Map.of());

        g.controller.assignCombatDamage(attacker, new CardCollection(blocker), null, 5, g.opponent, false);

        assertThat(g.gui.combatDamagePrompts()).containsExactly(new CombatDamagePrompt(
                attacker.getView(), List.of(blocker.getView()), 5, g.opponent.getView(), false, false));
    }

    @Test
    public void askThePlayerWhenSeveralCreaturesBlock() {
        // Multiple blockers need a damage split even without trample; the defender is not offered
        // as a target here, but its view is still passed along.
        HumanControlledGame g = new HumanControlledGame();
        Card attacker = g.card(g.human, "Attacker", "Types:Creature Beast", "PT:4/4");
        Card first = g.card(g.opponent, "First Blocker", "Types:Creature Bear", "PT:2/2");
        Card second = g.card(g.opponent, "Second Blocker", "Types:Creature Bear", "PT:2/2");
        g.gui.answerCombatDamageWith(prompt -> Map.of());

        g.controller.assignCombatDamage(attacker, new CardCollection(List.of(first, second)), null, 4, g.opponent, false);

        assertThat(g.gui.combatDamagePrompts())
                .singleElement()
                .extracting(CombatDamagePrompt::blockers)
                .isEqualTo(List.of(first.getView(), second.getView()));
    }

    // --- assignCombatDamage: how the answer is translated back ---

    @Test
    public void mapThePlayersAnswerBackToEngineCardsWithANullKeyForTheDefender() {
        // The UI answers in CardViews; the engine needs Cards. Blocker views are looked up in a
        // cache built before the prompt, and a null key (the UI's "defender" entry) is kept as null.
        HumanControlledGame g = new HumanControlledGame();
        Card attacker = g.card(g.human, "Trampler", "Types:Creature Beast", "PT:5/5", "K:Trample");
        Card blocker = g.card(g.opponent, "Blocker", "Types:Creature Bear", "PT:2/2");
        g.gui.answerCombatDamageWith(prompt -> {
            Map<CardView, Integer> answer = new HashMap<>();
            answer.put(prompt.blockers().get(0), 2);
            answer.put(null, 3);
            return answer;
        });

        Map<Card, Integer> damage = g.controller.assignCombatDamage(
                attacker, new CardCollection(blocker), null, 5, g.opponent, false);

        assertThat(damage).containsOnly(entry(blocker, 2), entry(null, 3));
    }

    @Test
    public void dropAnswerEntriesForCardsThatAreNotBlockers() {
        // Only blockers and the defender are valid damage targets. An entry for any other card
        // (here, the attacker itself) is silently discarded rather than rejected.
        HumanControlledGame g = new HumanControlledGame();
        Card attacker = g.card(g.human, "Trampler", "Types:Creature Beast", "PT:5/5", "K:Trample");
        Card blocker = g.card(g.opponent, "Blocker", "Types:Creature Bear", "PT:2/2");
        g.gui.answerCombatDamageWith(prompt -> Map.of(prompt.blockers().get(0), 2, prompt.attacker(), 3));

        Map<Card, Integer> damage = g.controller.assignCombatDamage(
                attacker, new CardCollection(blocker), null, 5, g.opponent, false);

        assertThat(damage).containsExactly(entry(blocker, 2));
    }

    @Test
    public void returnNullWhenThePlayerGivesNoAnswer() {
        // A null answer from the UI is passed on as null, not as an empty assignment. Callers treat
        // the two differently, so this distinction must survive any move of this code.
        HumanControlledGame g = new HumanControlledGame();
        Card attacker = g.card(g.human, "Trampler", "Types:Creature Beast", "PT:5/5", "K:Trample");
        Card blocker = g.card(g.opponent, "Blocker", "Types:Creature Bear", "PT:2/2");
        g.gui.answerCombatDamageWith(prompt -> null);

        Map<Card, Integer> damage = g.controller.assignCombatDamage(
                attacker, new CardCollection(blocker), null, 5, g.opponent, false);

        assertThat(damage).isNull();
    }

    private static Card addLegalAttacker(HumanControlledGame g) {
        g.game.setAge(forge.game.GameStage.Play);
        g.game.getPhaseHandler().devModeSet(PhaseType.MAIN1, g.human);
        Card attacker = addToBattlefield(g, g.human, "Attacker", "Types:Creature Beast", "PT:4/4");
        attacker.setSickness(false);
        return attacker;
    }

    private static Card addToBattlefield(HumanControlledGame g, Player owner, String name, String... scriptLines) {
        Card card = g.card(owner, name, scriptLines);
        card.setGameTimestamp(g.game.getNextTimestamp());
        owner.getZone(ZoneType.Battlefield).add(card);
        return card;
    }

    private static void completeInputImmediately(IGuiGame gui) throws InterruptedException {
        doAnswer(invocation -> {
            invocation.<CountDownLatch>getArgument(0).countDown();
            return null;
        }).when(gui).awaitInput(any(CountDownLatch.class));
    }

}
