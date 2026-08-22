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

    /** @return false if a declaration is already in flight. */
    public boolean declareShortcut(final Player controller, final List<SpellAbility> loopAbilities, final int count) {
        if (current != null) {
            return false;
        }
        current = new ShortcutDeclaration(controller, loopAbilities, count);
        return true;
    }

    public void accept(final Player p) {
        if (current == null) {
            return;
        }
        current.recordResponse(p, ShortcutDeclaration.Response.ACCEPT);
        if (current.allAccepted()) {
            current.setStatus(ShortcutDeclaration.Status.ACCEPTED);
            beginExecution();
        }
    }

    public void lower(final Player p, final int newCount) {
        if (current == null) {
            return;
        }
        current.recordResponse(p, ShortcutDeclaration.Response.LOWER);
        current.setProposedCount(newCount);
        current.setStatus(ShortcutDeclaration.Status.DECLARED);
    }

    public void interrupt(final Player p, final int at) {
        if (current == null) {
            return;
        }
        current.recordResponse(p, ShortcutDeclaration.Response.INTERRUPT);
        current.setInterruptAt(at);
        current.setStatus(ShortcutDeclaration.Status.INTERRUPTED);
    }

    public void object(final Player p) {
        if (current == null) {
            return;
        }
        current.recordResponse(p, ShortcutDeclaration.Response.OBJECT);
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
        final RepeatNExecutor executor = new RepeatNExecutor();
        final BooleanSupplier interrupt = () -> false;
        final Consumer<Integer> progress = done -> { /* UI hook deferred */ };
        for (final SpellAbility sa : current.getLoopAbilities()) {
            final SpellAbility ability = sa;
            final Supplier<SpellAbility> factory = () -> ability;
            executor.execute(game, count, factory, progress, interrupt);
            if (game.isGameOver()) {
                break;
            }
        }
        current.setStatus(game.isGameOver() ? ShortcutDeclaration.Status.COMPLETE : ShortcutDeclaration.Status.COMPLETE);
    }
}
