package forge.screens.rogue;

import forge.gamemodes.rogue.RogueCustomDeckGenerator;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.rogue.RogueEvent;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.RogueTutorial;
import forge.gamemodes.rogue.effect.ChestEffect;
import forge.gamemodes.rogue.effect.ChoiceRerollContext;
import forge.gamemodes.rogue.effect.CursedEffect;
import forge.gamemodes.rogue.effect.NPCEffect;
import forge.gamemodes.rogue.effect.WrathfulEffect;
import forge.gamemodes.rogue.flow.RogueDecisions;
import forge.gamemodes.rogue.npc.NPCContext;
import forge.gamemodes.rogue.path.NodePlanebound;
import forge.Forge;
import forge.assets.FImage;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.util.Localizer;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** The mobile client's answers to Rogue Commander's questions. Every method runs on the flow thread. */
class MobileRogueDecisions implements RogueDecisions {

    /** Shows a dialog and waits for the answer's index. */
    static int dialog(String title, String text, List<String> answers) {
        return dialog(title, text, null, RogueDialog.Entrance.PLAIN, answers);
    }

    /** image may be null. */
    static int dialog(String title, String text, FImage image, RogueDialog.Entrance entrance, List<String> answers) {
        CompletableFuture<Integer> choice = new CompletableFuture<>();
        FThreads.invokeInEdtLater(() -> RogueDialog.show(title, text, image, entrance, answers, choice::complete));
        return RogueController.get().await(choice);
    }

    @Override
    public void showMessage(String title, String text) {
        dialog(title, text, List.of(Localizer.getInstance().getMessage("lblOK")));
    }

    @Override
    public boolean confirmReplaceRun() {
        Localizer localizer = Localizer.getInstance();
        return dialog(localizer.getMessage("lblRogueAbandonCurrentRun"), localizer.getMessage("lblRogueAbandonForNewRun"),
            List.of(localizer.getMessage("lblCancel"), localizer.getMessage("lblRogueStartNewRun"))) == 1;
    }

    @Override
    public List<PaperCard> show(String title, List<PaperCard> cards, int maxSelections, String rerollLabel, int rerollCost,
                                boolean rerollEnabled, int gold) {
        RogueRun run = RogueController.get().flow().run();
        List<PaperCard> chosen = RogueQuestion.ask(q -> new RogueRewardScreen(run, title, cards, maxSelections, rerollLabel,
            rerollEnabled, q));
        showTutorial(RogueTutorial.CARD_REWARDS);
        return chosen;
    }

    @Override
    public List<PaperCard> chooseCommanders() {
        Localizer localizer = Localizer.getInstance();
        List<PaperCard> commanders = RogueCustomDeckGenerator.getEligibleCommanders();
        if (commanders.isEmpty()) {
            showMessage(localizer.getMessage("lblRogueChooseAnyCommander"), localizer.getMessage("lblRogueNoLegalCommanders"));
            return List.of();
        }
        List<PaperCard> chosen = RogueQuestion.ask(q -> new RogueCommanderPickScreen(commanders, null, q));
        if (chosen.isEmpty()) {
            return chosen;
        }
        List<PaperCard> partners = RogueCustomDeckGenerator.getEligiblePartnerCommanders(chosen.get(0));
        if (partners.isEmpty()) {
            return chosen;
        }
        return RogueQuestion.ask(q -> new RogueCommanderPickScreen(partners, chosen.get(0), q));
    }

    @Override
    public int choosePortrait(PaperCard commander, int current) {
        return RogueQuestion.ask(q -> new RoguePortraitScreen(commander.getName(), current, q));
    }

    @Override
    public List<PaperCard> chooseCards(CardChoice choice) {
        return RogueQuestion.ask(q -> new RogueCardPickScreen(choice, q));
    }

    @Override
    public Pick<NPCEffect> chooseBoon(NPCContext context, ChoiceRerollContext rerolls) {
        return RogueNodeQuestions.boon(context, rerolls, RogueController.get().flow().timedRun());
    }

    @Override
    public Pick<ChestEffect> chooseChest(List<ChestEffect> loot, RogueRun run, ChoiceRerollContext rerolls) {
        return RogueNodeQuestions.chest(loot, run, rerolls);
    }

    @Override
    public Pick<RogueEvent.EventChoice> chooseEvent(RogueEvent event, RogueRun run, ChoiceRerollContext rerolls) {
        return RogueNodeQuestions.event(event, run, rerolls);
    }

    @Override
    public SanctumAnswer chooseSanctum(SanctumOffer offer) {
        return RogueNodeQuestions.sanctum(offer, RogueController.get().flow().run());
    }

    @Override
    public ShopAnswer shop(ShopOffer offer) {
        return RogueQuestion.ask(q -> new RogueShopScreen(offer, q));
    }

    @Override
    public <T> List<T> devPick(String title, List<T> all, List<T> current, int count) {
        return RogueNodeQuestions.devPick(title, all, current, count);
    }

    @Override
    public void showPlaneboundEffects(NodePlanebound node, List<WrathfulEffect> wrathful, List<CursedEffect> cursed) {
        RogueNodeQuestions.planeboundEffects(wrathful, cursed, RogueController.get().flow().run());
    }

    @Override
    public void showResult(ResultKind kind, String title, String text, List<Section> sections) {
        RogueNodeQuestions.result(kind, title, text, sections, RogueController.get().flow().run());
    }

    @Override
    public void showTutorial(RogueTutorial tutorial) {
        RogueNodeQuestions.tutorial(tutorial, RogueController.get().flow().timedRun());
    }

    /** Posts the match to the GUI thread and returns once it has started; a failure is thrown on the flow thread. */
    @Override
    public void startMatch(RogueMatch match) {
        RogueRun run = match.run();
        if (!run.getCarryCards().isEmpty()) {
            showTutorial(RogueTutorial.CARRY_CARDS);
        }
        CompletableFuture<Void> started = new CompletableFuture<>();
        FThreads.invokeInEdtLater(() -> {
            try {
                HostedMatch hostedMatch = GuiBase.getInterface().hostMatch();
                hostedMatch.setEndGameHook(() -> match.recordPublicCards().accept(hostedMatch));
                run.setHostedMatch(hostedMatch);
                hostedMatch.startMatch(match.rules(), match.variants(), match.players(), match.human(),
                    GuiBase.getInterface().getNewGuiGame());
                started.complete(null);
            } catch (RuntimeException e) {
                started.completeExceptionally(e);
            }
        });
        started.join();
    }

    @Override
    public void mapChanged(RogueRun run) {
        run.getPath().revealVisible(run);
        FThreads.invokeInEdtLater(() -> {
            if (Forge.getCurrentScreen() instanceof RogueMapScreen map) {
                map.refresh();
            }
        });
    }

    @Override
    public void runStarted(RogueRun run) {
        run.getPath().revealVisible(run);
        FThreads.invokeInEdtLater(() -> {
            RogueController.backToRuns();
            RogueMapScreen map = new RogueMapScreen();
            map.playIntro();
            Forge.openScreen(map);
        });
    }

    @Override
    public void runEnded() {
        FThreads.invokeInEdtLater(RogueController::backToRuns);
    }
}
