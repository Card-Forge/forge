package forge.gamemodes.match;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;

/**
 * A decision the engine needs from a human player, with the facts needed to make it. It carries engine facts only:
 * the GUI shows it as a prompt and decides how the player's input becomes an {@link Answer}, and the host checks the
 * answer against the question it asked. A question is asked, stays pending until answered, and may be withdrawn.
 */
// TODO Only the priority prompt asks a Question so far. Each other input PlayerControllerHuman pushes onto the input
//  queue should become a record here with its own answers: InputConfirm, InputSelectEntitiesFromList and the cost
//  selections in HumanCostDecision, the mulligan and starting-hand inputs, InputAttack and InputBlock, InputSelectTargets
//  and InputPayMana. Once no input reads clicks, IGameController.selectCard, selectPlayer, selectButtonOk,
//  selectButtonCancel and the trigger event can be removed from the controller and the protocol.
public sealed interface Question extends Serializable permits Question.Priority {
    int id();
    PlayerView player();

    /**
     * Which ability to play, or whether to pass. {@code abilities} lists every ability of every card the player can
     * see and act on, each flagged by whether it passes {@code canPlay}; the flag does not promise the cost can be
     * paid. {@code undoCount} is how many actions can be undone, and {@code passLosesMana} whether passing now
     * empties a mana pool that still holds mana, with {@code passBurns} when that deals mana burn.
     */
    record Priority(int id, PlayerView player, Map<CardView, List<SpellAbilityView>> abilities,
                    int undoCount, boolean passLosesMana, boolean passBurns) implements Question {}
}
