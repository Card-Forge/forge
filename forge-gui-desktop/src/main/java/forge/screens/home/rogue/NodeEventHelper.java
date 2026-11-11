package forge.screens.home.rogue;

import forge.gamemodes.rogue.CodexHelper;
import forge.gamemodes.rogue.RogueEvent;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.RogueTutorial;
import forge.gamemodes.rogue.effect.*;
import forge.gamemodes.rogue.npc.NPCContext;
import forge.gamemodes.rogue.npc.NPCEncounterComposite;
import forge.gamemodes.rogue.path.NodeEvent;
import forge.localinstance.properties.ForgePreferences;
import forge.util.MyRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JOptionPane;

class NodeEventHelper {

    private final CSubmenuRogueMap map;
    private final NodeEffectResultHelper effectResultHelper;

    NodeEventHelper(CSubmenuRogueMap map, NodeEffectResultHelper effectResultHelper) {
        this.map = map;
        this.effectResultHelper = effectResultHelper;
    }

    void handleEventNode(NodeEvent eventNode, RogueRun currentRun) {
        if (currentRun == null) {
            return;
        }

        RogueEvent event = resolveEvent(eventNode);
        if (event == null) {
            currentRun.nextNode();
            map.updateView();
            return;
        }

        RogueMetaProgress progress = RogueMetaProgress.getInstance();
        event = NPCEncounterComposite.INSTANCE.onBeforeEvent(event, currentRun, progress);
        event = resolveDevEventOverride(event);
        RogueTutorialHelper.showIfNotSeen(RogueTutorial.EVENT);

        EventDialog.DialogResult result;
        boolean rerollRequested;
        do {
            ChoiceRerollContext rerollCtx = new ChoiceRerollContext();
            RogueEffectComposite.INSTANCE.onBeforeEvent(rerollCtx, currentRun);
            CodexHelper.recordTraitChoices(event.getChoices().stream()
                .map(RogueEvent.EventChoice::effect)
                .toList());
            result = new EventDialog(event, currentRun, rerollCtx).show();
            rerollRequested = result.rerollRequested();

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
        map.completeSideNode(eventNode);
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

        RogueEvent picked = (RogueEvent) JOptionPane.showInputDialog(
            null, "Override event:", "[DEV] Pick Event",
            JOptionPane.PLAIN_MESSAGE, null,
            RogueEvent.values(), event);
        return picked != null ? picked : event;
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

        if (map.checkSideNodeDefeat(event.getDisplayName())) {
            return NodeFlowOutcome.DO_NOT_COMPLETE_NODE;
        }

        showEventResult(choice, ctx);
        showNpcDialogs(npcContexts);
        return NodeFlowOutcome.COMPLETE_NODE;
    }

    private void showEventResult(RogueEvent.EventChoice choice, EffectResultContext ctx) {
        map.showEventResultDialog(
            getEventResultText(choice, ctx), buildNodeResultSections(ctx));
    }

    private void showNpcDialogs(List<NPCContext> contexts) {
        for (NPCContext context : contexts) {
            new NPCDialog(context, new ChoiceRerollContext()).show();
        }
    }

    private List<NodeResultPanel.CardSection> buildNodeResultSections(EffectResultContext ctx) {
        List<NodeResultPanel.CardSection> sections = new ArrayList<>();
        if (ctx.gainedWoundEffect != null) {
            sections.add(new NodeResultPanel.CardSection("Wound gained:",
                ctx.gainedWoundEffect.getDisplayName() + " - " + ctx.gainedWoundEffect.getDescription()));
        }
        if (ctx.removedCards != null && !ctx.removedCards.isEmpty()) {
            sections.add(new NodeResultPanel.CardSection("Cards removed:", ctx.removedCards));
        }
        if (ctx.addedCards != null && !ctx.addedCards.isEmpty()) {
            sections.add(new NodeResultPanel.CardSection("Cards added:", ctx.addedCards));
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
