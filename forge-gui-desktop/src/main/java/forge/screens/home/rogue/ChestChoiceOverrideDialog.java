package forge.screens.home.rogue;

import forge.gamemodes.rogue.effect.ChestEffect;
import forge.util.Localizer;
import java.util.List;

class ChestChoiceOverrideDialog {
    private static final int REQUIRED_SELECTIONS = 2;

    private final List<ChestEffect> originalChoices;

    ChestChoiceOverrideDialog(List<ChestEffect> choices) {
        originalChoices = choices;
    }

    List<ChestEffect> show() {
        Localizer localizer = Localizer.getInstance();
        List<ChestEffect> selected = RogueUIHelper.chooseDevEffects(
            localizer.getMessage("lblRogueDevChestLootOverride"),
            localizer.getMessage("lblRogueDevOverrideChestLoot"), "lblRogueChestOverrideSelection",
            List.of(ChestEffect.values()), originalChoices, REQUIRED_SELECTIONS);
        return selected.isEmpty() ? originalChoices : selected;
    }
}
