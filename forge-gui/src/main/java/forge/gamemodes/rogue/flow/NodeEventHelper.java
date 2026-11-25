package forge.gamemodes.rogue.flow;

import forge.gamemodes.rogue.CodexHelper;
import forge.gamemodes.rogue.RogueEvent;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.RogueTutorial;
import forge.gamemodes.rogue.effect.*;
import forge.gamemodes.rogue.flow.RogueDecisions.Pick;
import forge.gamemodes.rogue.flow.RogueDecisions.ResultKind;
import forge.gamemodes.rogue.flow.RogueDecisions.Section;
import forge.gamemodes.rogue.npc.NPCContext;
import forge.gamemodes.rogue.npc.NPCEncounterComposite;
import forge.gamemodes.rogue.path.NodeEvent;
import forge.localinstance.properties.ForgePreferences;
import forge.util.MyRandom;
import forge.util.Localizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class NodeEventHelper {

    private final RogueFlow flow;
    private final NodeEffectResultHelper effectResultHelper;

    NodeEventHelper(RogueFlow flow, NodeEffectResultHelper effectResultHelper) {
        this.flow = flow;
        this.effectResultHelper = effectResultHelper;
    }

    void handleEventNode(NodeEvent eventNode, RogueRun currentRun) {
        if (currentRun == null) {
            return;
        }

        RogueEvent event = resolveEvent(eventNode);
        if (event == null) {
            currentRun.nextNode();
            flow.decisions().mapChanged(currentRun);
            return;
        }

        RogueMetaProgress progress = RogueMetaProgress.getInstance();
        event = NPCEncounterComposite.INSTANCE.onBeforeEvent(event, currentRun, progress);
        event = resolveDevEventOverride(event);
        flow.decisions().showTutorial(RogueTutorial.EVENT);

        Pick<RogueEvent.EventChoice> result;
        boolean rerollRequested;
        do {
            ChoiceRerollContext rerollCtx = new ChoiceRerollContext();
            RogueEffectComposite.INSTANCE.onBeforeEvent(rerollCtx, currentRun);
            CodexHelper.recordTraitChoices(event.getChoices().stream()
                .map(RogueEvent.EventChoice::effect)
                .collect(Collectors.toList()));
            result = flow.decisions().chooseEvent(event, currentRun, rerollCtx);
            rerollRequested = result.reroll();

            if (rerollRequested) {
                RogueEffectComposite.INSTANCE.onChoiceReroll(rerollCtx, currentRun);
                event = generateRerolledEvent(event);
                event = resolveDevEventOverride(event);
                eventNode.setEvent(event);
            }
        } while (rerollRequested);

        RogueEvent.EventChoice choice = result.choice();
        if (choice == null) {
            return;
        }

        NodeFlowOutcome choiceOutcome = handleEventChoice(eventNode, event, choice, currentRun);
        if (choiceOutcome != NodeFlowOutcome.COMPLETE_NODE) {
            return;
        }
        flow.completeSideNode(eventNode);
    }

    private RogueEvent resolveEvent(NodeEvent eventNode) {
        return eventNode.getEvent();
    }

    private RogueEvent generateRerolledEvent(RogueEvent currentEvent) {
        List<RogueEvent> events = new ArrayList<>();
        for (RogueEvent event : RogueEvent.values()) {
            if (event.isAvailable()) {
                events.add(event);
            }
        }
        if (events.size() > 1) {
            events.remove(currentEvent);
        }
        Collections.shuffle(events, MyRandom.getRandom());
        return events.isEmpty() ? currentEvent : events.get(0);
    }

    private RogueEvent resolveDevEventOverride(RogueEvent event) {
        if (!ForgePreferences.DEV_MODE) {
            return event;
        }

        return flow.decisions().devPick(
            Localizer.getInstance().getMessage("lblRogueDevPickEvent"), List.of(RogueEvent.values()),
            List.of(event), 1).get(0);
    }

    private NodeFlowOutcome handleEventChoice(NodeEvent eventNode, RogueEvent event,
                                              RogueEvent.EventChoice choice, RogueRun currentRun) {
        EventEffect effect = choice.effect();
        if (!effect.isChoiceAvailable(currentRun)) {
            return NodeFlowOutcome.DO_NOT_COMPLETE_NODE;
        }
        CodexHelper.recordTraitAcquired(effect);

        EffectResultContext ctx = new EffectResultContext();
        if (effect.getEffectType() == RogueEffect.EffectType.ONESHOT) {
            effect.applyEffect(currentRun, ctx);
        } else {
            currentRun.addEventEffect(effect);
        }

        List<NPCContext> npcContexts = NPCEncounterComposite.INSTANCE.onAfterEventChoice(
            event, choice, effect, currentRun, RogueMetaProgress.getInstance());
        if (effect.getEffectType() == RogueEffect.EffectType.ONESHOT) {
            NodeFlowOutcome nodeFlowOutcome = effectResultHelper.handleEffectTrigger(eventNode, ctx, currentRun);
            if (nodeFlowOutcome != NodeFlowOutcome.COMPLETE_NODE) {
                showNpcDialogs(npcContexts);
                return nodeFlowOutcome;
            }
            CodexHelper.recordAcquiredCards(currentRun, ctx.addedCards);
        }

        if (flow.checkSideNodeDefeat(event.getDisplayName())) {
            return NodeFlowOutcome.DO_NOT_COMPLETE_NODE;
        }

        showEventResult(choice, ctx);
        showNpcDialogs(npcContexts);
        return NodeFlowOutcome.COMPLETE_NODE;
    }

    private void showEventResult(RogueEvent.EventChoice choice, EffectResultContext ctx) {
        flow.decisions().showResult(ResultKind.EVENT, Localizer.getInstance().getMessage("lblRogueEventCompleted"),
            getEventResultText(choice, ctx), buildNodeResultSections(ctx));
    }

    private void showNpcDialogs(List<NPCContext> contexts) {
        for (NPCContext context : contexts) {
            flow.decisions().chooseBoon(context, new ChoiceRerollContext());
        }
    }

    private List<Section> buildNodeResultSections(EffectResultContext ctx) {
        List<Section> sections = new ArrayList<>();
        if (ctx.gainedWoundEffect != null) {
            sections.add(new Section(Localizer.getInstance().getMessage("lblRogueWoundGained"),
                ctx.gainedWoundEffect.getDisplayName() + " - " + ctx.gainedWoundEffect.getDescription(), List.of()));
        }
        if (ctx.removedCards != null && !ctx.removedCards.isEmpty()) {
            sections.add(new Section(Localizer.getInstance().getMessage("lblRogueCardsRemoved"), null, ctx.removedCards));
        }
        if (ctx.addedCards != null && !ctx.addedCards.isEmpty()) {
            sections.add(new Section(Localizer.getInstance().getMessage("lblRogueCardsAdded"), null, ctx.addedCards));
        }
        return sections;
    }

    private String getEventResultText(RogueEvent.EventChoice choice, EffectResultContext ctx) {
        if (ctx.resultTextOverride != null) {
            return ctx.resultTextOverride;
        }
        return choice.resultText();
    }
}
