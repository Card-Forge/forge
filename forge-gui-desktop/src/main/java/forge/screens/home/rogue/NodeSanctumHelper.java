package forge.screens.home.rogue;

import forge.card.CardRulesPredicates;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.RogueRun.CarryCardType;
import forge.gamemodes.rogue.RogueTutorial;
import forge.gamemodes.rogue.effect.ChoiceRerollContext;
import forge.gamemodes.rogue.effect.RogueEffectComposite;
import forge.gamemodes.rogue.effect.SanctumContext;
import forge.gamemodes.rogue.npc.NPCContext;
import forge.gamemodes.rogue.npc.NPCEncounterComposite;
import forge.gamemodes.rogue.path.NodeSanctum;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.toolbox.FOptionPane;
import forge.util.Aggregates;
import java.util.List;
import java.util.function.Predicate;

class NodeSanctumHelper {

    private static final String SANCTUM_COOK_SOURCE_ID = "sanctum_cook";

    private final CSubmenuRogueMap map;

    NodeSanctumHelper(CSubmenuRogueMap map) {
        this.map = map;
    }

    void handleSanctumNode(NodeSanctum sanctumNode, RogueRun currentRun) {
        if (currentRun == null) {
            return;
        }

        resolveSanctum(currentRun, sanctumNode);
        map.completeSideNode(sanctumNode);
    }

    void resolveSanctum(RogueRun currentRun, NodeSanctum sanctumNode) {
        RogueTutorialHelper.showIfNotSeen(RogueTutorial.SANCTUM);
        RogueMetaProgress progress = RogueMetaProgress.getInstance();
        SanctumContext sanctumCtx = new SanctumContext();
        RogueEffectComposite.INSTANCE.onBeforeSanctum(sanctumCtx, currentRun);
        NPCEncounterComposite.INSTANCE.onBeforeSanctum(sanctumCtx, currentRun, progress);
        showNpcDialogs(sanctumCtx.preSanctumDialogs);

        int baseHealAmount = sanctumCtx.restLifeGainDisabled ? 0
            : sanctumCtx.restFullHeal ? currentRun.getMaxLife() : sanctumNode.getHealAmount();
        int missingLife = Math.max(0, currentRun.getMaxLife() - currentRun.getCurrentLife());
        int effectiveHealAmount = Math.min(baseHealAmount, missingLife);
        boolean hasWounds = !currentRun.getActiveWoundEffects().isEmpty();
        boolean canPayRestCost = sanctumCtx.restLifeGainDisabled
            || currentRun.getMaxLife() > sanctumCtx.restMaxLifeCost;
        boolean restEnabled = canPayRestCost && (effectiveHealAmount > 0 || hasWounds);
        String restDescription = sanctumCtx.restLifeGainDisabled
            ? "Cure all {{Wound}}s."
            : sanctumCtx.restFullHeal
                ? "Heal to full Life. Lose " + sanctumCtx.restMaxLifeCost
                    + " Max. Life. Cure all {{Wound}}s."
                : "Gain " + effectiveHealAmount + " Life & Cure all {{Wound}}s.";
        String restDisabledReason = restEnabled
            ? null
            : !canPayRestCost ? "You don't have enough Max. Life."
                : sanctumCtx.restLifeGainDisabled
                ? "You have no wounds to cure."
                : "You are already at maximum life and have no wounds to cure.";

        SanctumDialog dialog = new SanctumDialog(
            restDescription, restEnabled, restDisabledReason, sanctumCtx);
        SanctumDialog.SanctumChoice choice = dialog.show();

        switch (choice) {
            case HEAL:
                if (!sanctumCtx.restLifeGainDisabled && sanctumCtx.restMaxLifeCost > 0) {
                    currentRun.loseMaxLife(sanctumCtx.restMaxLifeCost);
                }
                currentRun.gainLifeUpToMax(sanctumCtx.restFullHeal && !sanctumCtx.restLifeGainDisabled
                    ? currentRun.getMaxLife() : baseHealAmount);
                currentRun.clearWounds();
                break;

            case COOK:
                PaperCard craftedFood = craftSanctumFood(currentRun);
                if (craftedFood == null) {
                    FOptionPane.showMessageDialog(
                        "No Food items matching your commander's color identity were available to craft.",
                        "Sanctum");
                    break;
                }
                currentRun.addCarryCard(craftedFood, CarryCardType.ITEM, SANCTUM_COOK_SOURCE_ID);
                map.showNodeResultDialog(
                    "Sanctum",
                    "You cooked:",
                    List.of(new NodeResultPanel.CardSection(null, List.of(craftedFood))),
                    900,
                    700,
                    NodeResultPanel.MessageAlignment.CENTER);
                break;

            case REFLECT:
                currentRun.addRemovalCredits(3);
                break;

            case CUSTOM:
                SanctumContext.SanctumChoice customChoice = dialog.getCustomChoice();
                if (customChoice != null) {
                    RogueEffectComposite.INSTANCE.onSanctumChoice(customChoice, currentRun);
                    showNpcDialogs(NPCEncounterComposite.INSTANCE.onSanctumChoice(
                        customChoice, currentRun, progress));
                }
                break;

            case SKIP:
                break;
        }
    }

    private void showNpcDialogs(List<NPCContext> contexts) {
        for (NPCContext context : contexts) {
            new NPCDialog(context, new ChoiceRerollContext()).show();
        }
    }

    private PaperCard craftSanctumFood(RogueRun currentRun) {
        Predicate<PaperCard> foodFilter = PaperCardPredicates.fromRules(
            CardRulesPredicates.IS_ARTIFACT.and(CardRulesPredicates.subType("Food")));
        List<PaperCard> foods = currentRun.getAllCardsForActiveCommander(foodFilter);
        return foods.isEmpty() ? null : Aggregates.random(foods);
    }
}
