package forge.interfaces;

import forge.card.MagicColor;
import forge.game.card.CardCollection;
import forge.game.player.actions.PlayerAction;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Map;

public interface IMacroSystem {
    void addRememberedAction(PlayerAction action);
    void setRememberedActions();
    void nextRememberedAction();
    default void repeatRememberedActions() { nextRememberedAction(); }
    default void cancelPlayback() { }
    default void cancelCurrentMacro() { cancelPlayback(); }
    default boolean isRecording() { return false; }
    default boolean isReplaying() { return false; }
    default boolean hasRememberedActions() { return false; }
    default Byte consumeRememberedColorChoice(final List<MagicColor.Color> choices) { return null; }
    default Map<Byte, Integer> consumeRememberedManaCombo(final List<MagicColor.Color> choices,
                                                          final int manaAmount, final boolean different) {
        return null;
    }
    default List<String> consumeRememberedModeChoice(final List<String> choices, final int min, final int num,
                                                     final boolean allowRepeat) {
        return null;
    }
    default List<String> consumeRememberedAbilityOrder(final List<String> choices) { return null; }
    default ImmutablePair<CardCollection, CardCollection> consumeRememberedScry(final CardCollection topN) { return null; }
    String playbackText();
}
