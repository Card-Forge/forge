package forge.gamemodes.match;

import java.io.Serializable;
import java.util.List;

import forge.game.card.CardView;
import forge.game.spellability.SpellAbilityView;

/** A player's answer to a {@link Question}. The host ignores an answer whose id is not that of the pending question. */
public sealed interface Answer extends Serializable permits Answer.Pass, Answer.Undo, Answer.Play {
    int questionId();

    record Pass(int questionId) implements Answer {}
    record Undo(int questionId) implements Answer {}
    /** {@code alsoOn} lists cards selected together with the ability's card; a mana ability is activated on them too. */
    record Play(int questionId, SpellAbilityView ability, List<CardView> alsoOn) implements Answer {}
}
