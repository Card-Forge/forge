package forge.gamemodes.match.input;

import forge.game.GameEntity;
import forge.game.GameEntityView;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.StackItemView;
import forge.gui.interfaces.IGuiGame;
import forge.trackable.TrackableObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps what the player has already chosen visible while a spell with several targeting steps (the chosen modes of a
 * modal spell, a sub-ability that targets again) is still being targeted, and takes it away once the last step is done.
 * Each step is its own prompt, so without this the first step's choices would lose their "chosen" mark as soon as the
 * next prompt opens.
 */
public final class TargetingProgress {
    private TargetingProgress() {
    }

    /** True if an ability after {@code sa} in its chain still needs targets chosen. */
    public static boolean hasLaterTargetingStep(final SpellAbility sa) {
        for (SpellAbility next = sa.getSubAbility(); next != null; next = next.getSubAbility()) {
            if (next.usesTargeting()) {
                return true;
            }
        }
        return false;
    }

    /** Entities and stack items chosen so far by any step of {@code sa}'s whole ability chain. */
    public static List<TrackableObject> chosenTargets(final SpellAbility sa) {
        final List<TrackableObject> targets = new ArrayList<>();
        for (SpellAbility step = sa.getRootAbility(); step != null; step = step.getSubAbility()) {
            if (!step.usesTargeting()) {
                continue;
            }
            for (final GameEntity ge : step.getTargets().getTargetEntities()) {
                targets.add(GameEntityView.get(ge));
            }
            for (final SpellAbility spell : step.getTargets().getTargetSpells()) {
                final SpellAbilityStackInstance si = sa.getHostCard().getGame().getStack()
                        .getInstanceMatchingSpellAbilityID(spell);
                if (si != null) {
                    targets.add(si.getView());
                }
            }
        }
        return targets;
    }

    /** Marks everything already chosen for {@code sa}'s chain as chosen in the GUI. */
    public static void showChosen(final IGuiGame gui, final SpellAbility sa) {
        gui.setHighlighted(chosenTargets(sa), true);
    }

    /** Removes every "chosen" mark that belongs to {@code sa}'s chain. */
    public static void clearChosen(final IGuiGame gui, final SpellAbility sa) {
        gui.setHighlighted(chosenTargets(sa), false);
    }
}
