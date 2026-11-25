package forge.screens.home.rogue;

import forge.gamemodes.rogue.effect.NPCEffect;
import forge.gamemodes.rogue.npc.NPCContext;
import forge.gamemodes.rogue.npc.NPCContext.NPCChoice;
import forge.util.Localizer;

import java.util.ArrayList;
import java.util.List;

class NPCChoiceOverrideDialog {
    private static final int REQUIRED_SELECTIONS = 3;

    private final NPCContext originalContext;

    NPCChoiceOverrideDialog(NPCContext context) {
        originalContext = context;
    }

    NPCContext show() {
        List<NPCEffect> pool = NPCEffect.getEffectsForNpc(originalContext.npc(), null);
        List<NPCEffect> preselectedEffects = originalContext.choices().stream()
            .map(NPCChoice::npcEffect)
            .toList();

        Localizer localizer = Localizer.getInstance();
        List<NPCEffect> selected = RogueUIHelper.chooseDevEffects(
            localizer.getMessage("lblRogueDevNpcChoiceOverride"),
            localizer.getMessage("lblRogueDevOverrideNpcChoices"), "lblRogueNpcOverrideSelection",
            pool, preselectedEffects, REQUIRED_SELECTIONS);
        if (selected.isEmpty()) {
            return originalContext;
        }

        List<NPCChoice> choices = new ArrayList<>(selected.size());
        for (NPCEffect effect : selected) {
            choices.add(new NPCChoice(effect.getDisplayName(), effect));
        }
        return new NPCContext(originalContext.npc(), originalContext.flavorTextChunks(), choices,
            originalContext.displayNameOverride(), originalContext.avatarIndexOverride());
    }
}
