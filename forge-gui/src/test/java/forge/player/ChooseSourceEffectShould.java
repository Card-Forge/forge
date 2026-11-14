package forge.player;

import com.google.common.collect.Iterables;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.DelayedReveal;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.collect.FCollectionView;
import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the way {@code ChooseSourceEffect.resolve} used to hang: a pick loop that keeps asking once
 * there is nothing left to pick.
 * <p>
 * The chooser is a {@link ScriptedChooser}, not the GUI-backed human controller, so a loop that never
 * ends shows up as a failed assertion after a few dozen questions instead of a hung build.
 */
public class ChooseSourceEffectShould {

    @Test(timeOut = 30_000)
    public void stopAskingOnceOnlySectionHeadersAreLeftInThePool() {
        // The pool offered to the chooser holds the sources plus a "--PERMANENTS:--" style header entry
        // per section, and only the sources are removed as they are picked. Amount$ 2 with a single
        // source leaves nothing but the header for the second pick. The loop rejects every header, so
        // without an exit for "no real source left" it asks forever; an isEmpty() check on the pool
        // never fires because the header is still in it.
        HumanControlledGame g = new HumanControlledGame();
        ScriptedChooser chooser = ScriptedChooser.replacing(g.human);
        Card source = inPlay(g, "Lone Source", "Types:Artifact",
                "A:AB$ ChooseSource | Cost$ 0 | Defined$ You | Amount$ 2 | Choices$ Card.Self");

        resolve(g, source);

        assertThat(source.getChosenCards()).containsExactly(source);
        assertThat(chooser.asked()).as("times the chooser was asked").isEqualTo(1);
    }

    private static Card inPlay(final HumanControlledGame g, final String name, final String... scriptLines) {
        final Card card = g.card(g.human, name, scriptLines);
        g.game.getAction().moveToPlay(card, null, null);
        return card;
    }

    private static void resolve(final HumanControlledGame g, final Card source) {
        // A non-land permanent's first spell ability is the one that casts it, not the scripted one.
        final SpellAbility sa = source.getSpellAbilities().stream()
                .filter(ability -> ability.getApi() == ApiType.ChooseSource)
                .findFirst().orElseThrow();
        sa.setActivatingPlayer(g.human);
        AbilityUtils.resolve(sa);
    }

    /**
     * Answers {@code chooseSingleEntityForEffect} without a UI: the first entry that is not a section
     * header, or the first entry at all when only headers are left (what a player clicking the top
     * row would get). Counts the questions and fails once there are clearly too many.
     */
    private static final class ScriptedChooser extends PlayerControllerHuman {
        private static final int GIVE_UP_AFTER = 25;
        private int asked;

        private ScriptedChooser(final Player player) {
            super(player.getGame(), player, player.getLobbyPlayer());
        }

        static ScriptedChooser replacing(final Player player) {
            final ScriptedChooser chooser = new ScriptedChooser(player);
            player.dangerouslySetController(chooser);
            return chooser;
        }

        int asked() {
            return asked;
        }

        @Override
        public <T extends GameEntity> T chooseSingleEntityForEffect(final FCollectionView<T> optionList,
                                                                    final DelayedReveal delayedReveal, final SpellAbility sa, final String title,
                                                                    final boolean isOptional, final Player targetedPlayer, final Map<String, Object> params) {
            if (++asked > GIVE_UP_AFTER) {
                throw new AssertionError("asked to choose " + asked + " times; the pick loop is not ending");
            }
            for (final T option : optionList) {
                if (!option.getName().startsWith("--")) {
                    return option;
                }
            }
            return Iterables.getFirst(optionList, null);
        }
    }
}
