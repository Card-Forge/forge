package forge.screens.rogue;

import forge.Forge;
import forge.assets.FSkinImage;
import forge.deck.FDeckViewer;
import forge.gamemodes.rogue.RogueEvent;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.RogueTutorial;
import forge.gamemodes.rogue.effect.ChestEffect;
import forge.gamemodes.rogue.effect.ChoiceRerollContext;
import forge.gamemodes.rogue.effect.CursedEffect;
import forge.gamemodes.rogue.effect.NPCEffect;
import forge.gamemodes.rogue.effect.RogueEffect;
import forge.gamemodes.rogue.effect.WrathfulEffect;
import forge.gamemodes.rogue.flow.RogueDecisions;
import forge.gamemodes.rogue.flow.RogueDecisions.Pick;
import forge.gamemodes.rogue.flow.RogueDecisions.SanctumAnswer;
import forge.gamemodes.rogue.flow.RogueDecisions.SanctumOffer;
import forge.gamemodes.rogue.flow.RogueDecisions.SanctumOption;
import forge.gamemodes.rogue.npc.NPC;
import forge.gamemodes.rogue.npc.NPCContext;
import forge.util.Localizer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** The questions asked at nodes, each shown as a scene. Every method runs on the flow thread. */
final class RogueNodeQuestions {
    private static final int CONTINUE = 0;
    private static final int REROLL = 1;
    private static final int SKIP_ALL = 1;

    private RogueNodeQuestions() {
    }

    private static RogueButton viewMap(String title) {
        return new RogueButton(Localizer.getInstance().getMessage("lblRogueViewMap"), null, RogueButton.State.NORMAL,
            () -> Forge.openScreen(new RogueMapScreen(title)));
    }

    private static RogueButton viewDeck(RogueRun run) {
        return new RogueButton(Localizer.getInstance().getMessage("lblViewDeck"), null, RogueButton.State.NORMAL,
            () -> FDeckViewer.show(run.getCurrentDeck()));
    }

    private static RogueButton reroll(int left, Runnable command) {
        return new RogueButton(Localizer.getInstance().getMessage("lblRogueRerollsRemaining", left), "Refresh",
            RogueButton.State.NORMAL, command);
    }

    private static Supplier<RogueRun> runOf(RogueRun run) {
        return run == null ? null : () -> run;
    }

    static Pick<RogueEvent.EventChoice> event(RogueEvent event, RogueRun run, ChoiceRerollContext rerolls) {
        return RogueQuestion.ask(q -> {
            List<RogueChoiceList.Choice> choices = new ArrayList<>();
            for (RogueEvent.EventChoice choice : event.getChoices()) {
                String reason = choice.effect().isChoiceAvailable(run) ? null : choice.effect().getUnavailableReason(run);
                choices.add(new RogueChoiceList.Choice(choice.label(), choice.effect().getRawDescription(),
                    choice.effect().isChoiceAvailable(run) ? null : reason == null ? "" : reason,
                    choice.effect().getEffectCardReference()));
            }
            List<RogueButton> buttons = new ArrayList<>(List.of(viewMap(event.getDisplayName()), viewDeck(run)));
            if (rerolls.remainingRerolls > 0) {
                buttons.add(reroll(rerolls.remainingRerolls, () -> q.answer(new Pick<>(true, null))));
            }
            RogueChoiceList list = new RogueChoiceList(choices, 0, i -> q.answer(new Pick<>(false, event.getChoices().get(i))));
            return new RogueSceneScreen(runOf(run), "title_bg", event.getDisplayName(), event.getRawDescription(),
                RogueSceneScreen.building(240, 272, 16), list, buttons);
        });
    }

    /** A story is a context with no choices; its pages are shown one by one. */
    static Pick<NPCEffect> boon(NPCContext context, ChoiceRerollContext rerolls, RogueRun run) {
        List<String> pages = context.flavorTextChunks().isEmpty() ? List.of("") : context.flavorTextChunks();
        List<NPCContext.NPCChoice> choices = context.choices();
        for (int i = 0; i < pages.size(); i++) {
            boolean last = i == pages.size() - 1;
            String page = pages.get(i) == null ? "" : pages.get(i);
            if (last && !choices.isEmpty()) {
                return RogueQuestion.ask(q -> {
                    List<RogueChoiceList.Choice> list = new ArrayList<>();
                    for (NPCContext.NPCChoice choice : choices) {
                        NPCEffect effect = choice.npcEffect();
                        list.add(new RogueChoiceList.Choice(choice.label(), effect == null ? "" : effect.getRawDescription(), null,
                            effect == null ? null : effect.getEffectCardReference()));
                    }
                    List<RogueButton> buttons = new ArrayList<>();
                    if (rerolls.remainingRerolls > 0) {
                        buttons.add(reroll(rerolls.remainingRerolls, () -> q.answer(new Pick<>(true, null))));
                    }
                    return new RogueSceneScreen(runOf(run), "tavern", context.displayName(), page,
                        RogueSceneScreen.face(context.avatarIndex()),
                        new RogueChoiceList(list, 46, index -> q.answer(new Pick<>(false, choices.get(index).npcEffect()))), buttons);
                });
            }
            int answer = RogueQuestion.ask(q -> {
                List<RogueButton> buttons = new ArrayList<>();
                if (rerolls.remainingRerolls > 0) {
                    buttons.add(reroll(rerolls.remainingRerolls, () -> q.answer(REROLL)));
                }
                buttons.add(new RogueButton(Localizer.getInstance().getMessage("lblContinue"), null, RogueButton.State.MAIN,
                    () -> q.answer(CONTINUE)));
                return new RogueSceneScreen(runOf(run), "tavern", context.displayName(), page,
                    RogueSceneScreen.face(context.avatarIndex()), null, buttons);
            });
            if (answer == REROLL) {
                return new Pick<>(true, null);
            }
        }
        return new Pick<>(false, null);
    }

    static Pick<ChestEffect> chest(List<ChestEffect> loot, RogueRun run, ChoiceRerollContext rerolls) {
        Localizer localizer = Localizer.getInstance();
        return RogueQuestion.ask(q -> {
            String title = localizer.getMessage("lblRogueChooseYourLoot");
            List<RogueButton> buttons = new ArrayList<>(List.of(viewMap(title), viewDeck(run)));
            if (rerolls.remainingRerolls > 0) {
                buttons.add(reroll(rerolls.remainingRerolls, () -> q.answer(new Pick<>(true, null))));
            }
            buttons.add(new RogueButton(localizer.getMessage("lblRogueSkipLoot"), null, RogueButton.State.NORMAL,
                () -> q.answer(new Pick<>(false, null))));
            return new RogueChestScreen(runOf(run), title, loot, effect -> q.answer(new Pick<>(false, effect)), buttons);
        });
    }

    static SanctumAnswer sanctum(SanctumOffer offer, RogueRun run) {
        Localizer localizer = Localizer.getInstance();
        List<SanctumOption> options = offer.options();
        return RogueQuestion.ask(q -> {
            List<RogueChoiceList.Choice> choices = new ArrayList<>();
            for (SanctumOption option : options) {
                String reason = option.disabledReason() == null ? "" : option.disabledReason();
                choices.add(new RogueChoiceList.Choice(option.label(), option.description(), option.enabled() ? null : reason, null));
            }
            String title = localizer.getMessage("lblRogueSanctum");
            List<RogueButton> buttons = List.of(viewMap(title), viewDeck(run), new RogueButton(localizer.getMessage("lblSkip"), null,
                RogueButton.State.NORMAL, () -> q.answer(new SanctumAnswer(RogueDecisions.SanctumAction.SKIP, null))));
            return new RogueSceneScreen(runOf(run), "tavern", title, localizer.getMessage("lblRogueChooseAction"),
                RogueSceneScreen.building(160, 240, 32), new RogueChoiceList(choices, 46, i -> {
                    SanctumAnswer answer = options.get(i).answer();
                    if (offer.accepts(answer)) {
                        q.answer(answer);
                    }
                }), buttons);
        });
    }

    private static String nameOf(Object item) {
        return item instanceof RogueEffect effect ? effect.getDisplayName() : String.valueOf(item);
    }

    /** A developer's override: one item, or exactly count of them. Cancel keeps current. */
    static <T> List<T> devPick(String title, List<T> all, List<T> current, int count) {
        Localizer localizer = Localizer.getInstance();
        Set<Integer> chosen = new LinkedHashSet<>();
        for (T item : current) {
            int i = all.indexOf(item);
            if (i >= 0) {
                chosen.add(i);
            }
        }
        List<T> answer = RogueQuestion.ask(q -> {
            List<RogueChoiceList.Choice> choices = new ArrayList<>();
            for (T item : all) {
                RogueEffect effect = item instanceof RogueEffect e ? e : null;
                choices.add(new RogueChoiceList.Choice(nameOf(item), effect == null ? "" : effect.getRawDescription(), null,
                    effect == null ? null : effect.getEffectCardReference()));
            }
            RogueButton[] okHolder = new RogueButton[1];
            Runnable update = () -> {
                int missing = count - chosen.size();
                okHolder[0].setText(missing > 0 ? localizer.getMessage("lblRogueChooseMore", missing) : localizer.getMessage("lblOK"));
                okHolder[0].setState(missing == 0 ? RogueButton.State.MAIN : RogueButton.State.DISABLED);
            };
            RogueButton ok = new RogueButton(localizer.getMessage("lblOK"), null, RogueButton.State.MAIN, () -> {
                if (chosen.size() == count) {
                    List<T> picked = new ArrayList<>();
                    for (int i : chosen) {
                        picked.add(all.get(i));
                    }
                    q.answer(picked);
                }
            });
            okHolder[0] = ok;
            update.run();
            RogueButton cancel = new RogueButton(localizer.getMessage("lblCancel"), null, RogueButton.State.NORMAL, () -> q.answer(current));
            RogueChoiceList list = new RogueChoiceList(choices, 0, i -> {
                if (count == 1) {
                    q.answer(List.of(all.get(i)));
                    return;
                }
                if (!chosen.remove(i) && chosen.size() < count) {
                    chosen.add(i);
                }
                update.run();
            }, chosen::contains);
            return new RogueSceneScreen(null, "title_bg", title, null, null, list, count == 1 ? List.of(cancel) : List.of(cancel, ok));
        });
        return answer == null || answer.isEmpty() ? current : answer;
    }

    static void planeboundEffects(List<WrathfulEffect> wrathful, List<CursedEffect> cursed, RogueRun run) {
        RogueDecisions.Heading heading = RogueDecisions.planeboundEffects(wrathful, cursed);
        List<RogueChoiceList.Choice> lines = new ArrayList<>();
        for (WrathfulEffect effect : wrathful) {
            lines.add(new RogueChoiceList.Choice(effect.getDisplayName(), effect.getRawDescription(), null, effect.getEffectCardReference()));
        }
        for (CursedEffect effect : cursed) {
            lines.add(new RogueChoiceList.Choice(effect.getDisplayName(), effect.getRawDescription(), null, effect.getEffectCardReference()));
        }
        RogueQuestion.<Integer>ask(q -> new RogueSceneScreen(runOf(run), "title_bg", heading.title(), heading.text(),
            RogueSceneScreen.building(304, 288, 16), new RogueChoiceList(lines, 0, i -> { }),
            List.of(new RogueButton(Localizer.getInstance().getMessage("lblOK"), null, RogueButton.State.MAIN, () -> q.answer(CONTINUE)))));
    }

    static void result(RogueDecisions.ResultKind kind, String title, String text, List<RogueDecisions.Section> sections, RogueRun run) {
        Localizer localizer = Localizer.getInstance();
        switch (kind) {
            case SPARK -> MobileRogueDecisions.dialog(title, text, FSkinImage.QUEST_ELIXIR, RogueDialog.Entrance.DROP,
                List.of(localizer.getMessage("lblOK")));
            case REVIVE, DEFEAT -> MobileRogueDecisions.dialog(title, text, List.of(localizer.getMessage("lblOK")));
            case EVENT, BOON, SANCTUM -> RogueQuestion.<Integer>ask(q -> new RogueSceneScreen(runOf(run),
                kind == RogueDecisions.ResultKind.EVENT ? "title_bg" : "tavern", title, text, null, new RogueSections(sections),
                List.of(new RogueButton(localizer.getMessage("lblOK"), null, RogueButton.State.MAIN, () -> q.answer(CONTINUE)))));
        }
    }

    /** Lessons about desktop's own controls, which mobile does not have. */
    static final Set<RogueTutorial> DESKTOP_ONLY = EnumSet.of(RogueTutorial.MATCH_UI, RogueTutorial.MATCH_CARD_HIGHLIGHTING,
        RogueTutorial.MATCH_PHASES_AND_YIELDS, RogueTutorial.CARD_SELECTION_ZOOM, RogueTutorial.DECK_EDITOR);

    /** Teferi tells the lesson page by page; Skip All Tutorials asks first. */
    static void tutorial(RogueTutorial tutorial, RogueRun run) {
        RogueMetaProgress progress = RogueMetaProgress.getInstance();
        if (DESKTOP_ONLY.contains(tutorial) || progress.hasSeenTutorial(tutorial)) {
            return;
        }
        progress.markTutorialSeen(tutorial);
        Localizer localizer = Localizer.getInstance();
        List<String> pages = tutorial.getMessageChunks();
        for (int i = 0; i < pages.size(); i++) {
            String page = pages.get(i);
            int answer = RogueQuestion.ask(q -> new RogueSceneScreen(runOf(run), "tavern", NPC.TEFERI.name, page,
                RogueSceneScreen.face(NPC.TEFERI.avatarIndex), null, List.of(
                    new RogueButton(localizer.getMessage("lblRogueSkipAllTutorials"), null, RogueButton.State.NORMAL, () -> q.answer(SKIP_ALL)),
                    new RogueButton(localizer.getMessage("lblContinue"), null, RogueButton.State.MAIN, () -> q.answer(CONTINUE)))));
            if (answer == SKIP_ALL) {
                String skip = localizer.getMessage("lblRogueSkipAllTutorials");
                if (MobileRogueDecisions.dialog(skip, localizer.getMessage("lblRogueSkipAllTutorialsConfirm"),
                        List.of(localizer.getMessage("lblCancel"), skip)) == 1) {
                    progress.markAllTutorialsSeen();
                    return;
                }
                i--;
            }
        }
    }
}
