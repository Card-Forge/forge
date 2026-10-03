package forge.gui.interfaces;

import forge.game.GameEntityView;
import forge.game.card.CardView;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;

/**
 * A scripted stand-in for the UI that a {@code PlayerController} talks to through {@link IGuiGame}.
 * <p>
 * It is strict about prompts and lenient about notifications: any non-void call (a question the
 * controller asks the player) fails the test unless the test has scripted an answer for it, while
 * void calls (view updates, messages) are silently accepted. That way a test notices when the code
 * under test starts or stops asking the player something, without having to stub every UI refresh.
 * <p>
 * Only the prompts that tests need are exposed, one typed method pair per prompt: an
 * {@code answer...With} method to script the reply, and a method returning the recorded prompts.
 * This avoids a hand-written implementation of the whole interface, which would have to change
 * every time {@link IGuiGame} does.
 */
public final class FakeGuiGame {

    private static final Answer<Object> FAIL_ON_UNSCRIPTED_PROMPT = invocation -> {
        if (invocation.getMethod().getReturnType() == void.class) {
            return null;
        }
        if (invocation.getMethod().isDefault()) {
            return invocation.callRealMethod();
        }
        throw new AssertionError("Unscripted GUI prompt: " + invocation.getMethod().getName());
    };

    private final IGuiGame gui = Mockito.mock(IGuiGame.class, FAIL_ON_UNSCRIPTED_PROMPT);
    private final List<CombatDamagePrompt> combatDamagePrompts = new ArrayList<>();

    public IGuiGame asGui() {
        return gui;
    }

    public record CombatDamagePrompt(CardView attacker, List<CardView> blockers, int damage,
                                     GameEntityView defender, boolean overrideOrder, boolean maySkip) {
    }

    public FakeGuiGame answerCombatDamageWith(final Function<CombatDamagePrompt, Map<CardView, Integer>> answer) {
        Mockito.doAnswer(invocation -> {
            // Blockers arrive as a TrackableCollection, whose equals() is not List equality; record a
            // plain snapshot so prompts compare by content.
            final List<CardView> blockers = invocation.getArgument(1);
            final CombatDamagePrompt prompt = new CombatDamagePrompt(
                    invocation.getArgument(0), List.copyOf(blockers), invocation.getArgument(2),
                    invocation.getArgument(3), invocation.getArgument(4), invocation.getArgument(5));
            combatDamagePrompts.add(prompt);
            return answer.apply(prompt);
        }).when(gui).assignCombatDamage(any(), any(), anyInt(), any(), anyBoolean(), anyBoolean());
        return this;
    }

    public List<CombatDamagePrompt> combatDamagePrompts() {
        return Collections.unmodifiableList(combatDamagePrompts);
    }
}
