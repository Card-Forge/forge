package forge.game.shortcut;

import forge.game.Game;
import forge.game.player.Player;
import forge.game.repeat.RepeatNExecutor;
import forge.game.spellability.SpellAbility;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CR 720 shortcut-proposal manager (#52). Engine core only — one in-flight
 * declaration at a time. The loop-builder dialog and opponent-response UI are
 * deferred; this class exposes the state machine and wires execution to the
 * {@link RepeatNExecutor} (#50). Opponent prompts ("accept / lower / interrupt
 * / object") are expected to be driven by the match UI through these methods.
 */
public class ShortcutManager { // doc:11h PARTIAL

    private final Game game;
    private ShortcutDeclaration current;

    public ShortcutManager(final Game game) {
        this.game = game;
    }

    public ShortcutDeclaration getDeclaration() {
        return current;
    }

    /** @return false if a nonterminal declaration is already in flight. */
    public boolean declareShortcut(final Player controller, final List<SpellAbility> loopAbilities, final int count) {
        if (current != null) {
            final ShortcutDeclaration.Status s = current.getStatus();
            if (s != ShortcutDeclaration.Status.COMPLETE && s != ShortcutDeclaration.Status.OBJECTED && s != ShortcutDeclaration.Status.DRAW) {
                return false;
            }
        }
        current = new ShortcutDeclaration(controller, loopAbilities, count);
        return true;
    }

    public void accept(final Player p) {
        if (current == null) {
            return;
        }
        if (!current.recordResponse(p, ShortcutDeclaration.Response.ACCEPT)) {
            return;
        }
        if (current.allAccepted()) {
            current.setStatus(ShortcutDeclaration.Status.ACCEPTED);
            beginExecution();
        }
    }

    public void lower(final Player p, final int newCount) {
        if (current == null) {
            return;
        }
        if (!current.recordResponse(p, ShortcutDeclaration.Response.LOWER)) {
            return;
        }
        final int oldCount = current.getProposedCount();
        current.setProposedCount(newCount);
        if (oldCount != newCount) {
            for (final Player responder : current.getResponses().keySet()) {
                current.getResponses().put(responder, ShortcutDeclaration.Response.PENDING);
            }
        }
        current.setStatus(ShortcutDeclaration.Status.DECLARED);
    }

    public void interrupt(final Player p, final int at) {
        if (current == null) {
            return;
        }
        if (!current.recordResponse(p, ShortcutDeclaration.Response.INTERRUPT)) {
            return;
        }
        current.setInterruptAt(at);
        current.setStatus(ShortcutDeclaration.Status.INTERRUPTED);
        beginExecution();
    }

    public void object(final Player p) {
        if (current == null) {
            return;
        }
        if (!current.recordResponse(p, ShortcutDeclaration.Response.OBJECT)) {
            return;
        }
        if (current.isMandatoryLoop()) {
            game.declareLoopDraw();
            current.setStatus(ShortcutDeclaration.Status.DRAW);
        } else {
            current.setStatus(ShortcutDeclaration.Status.OBJECTED);
        }
    }

    private void beginExecution() {
        if (current == null) {
            return;
        }
        current.setStatus(ShortcutDeclaration.Status.EXECUTING);
        final int count = Math.min(current.getProposedCount(), Math.max(1, current.getInterruptAt()));
        final boolean[] interruptedFlag = new boolean[1];
        final BooleanSupplier interrupt = () -> interruptedFlag[0];
        final Consumer<Integer> progress = done -> { /* UI hook deferred */ };
        boolean fullyCompleted = true;
        for (final SpellAbility sa : current.getLoopAbilities()) {
            final SpellAbility ability = sa;
            final Supplier<SpellAbility> factory = () -> ability;
            final RepeatNExecutor.RepeatResult result = RepeatNExecutor.execute(game, count, factory, progress, interrupt);
            if (result.aborted) {
                fullyCompleted = false;
                if (current.getStatus() == ShortcutDeclaration.Status.INTERRUPTED) {
                    interruptedFlag[0] = true;
                }
                break;
            }
            if (game.isGameOver()) {
                break;
            }
        }
        if (game.isGameOver()) {
            current.setStatus(ShortcutDeclaration.Status.COMPLETE);
        } else if (!fullyCompleted) {
            // Status already set by interrupt() or will be EXECUTING if aborted for other reasons
            if (current.getStatus() == ShortcutDeclaration.Status.EXECUTING) {
                current.setStatus(ShortcutDeclaration.Status.OBJECTED);
            }
        } else {
            current.setStatus(ShortcutDeclaration.Status.COMPLETE);
        }
    }
}
