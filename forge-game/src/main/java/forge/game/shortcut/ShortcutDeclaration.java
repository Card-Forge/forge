package forge.game.shortcut;

import forge.game.player.Player;
import forge.game.repeat.RepeatNExecutor;
import forge.game.spellability.SpellAbility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A single CR 720 shortcut proposal made by a controller: "this loop resolves
 * N times, opponents may accept / lower / interrupt / object."
 *
 * <p>Engine data model only. The loop-builder dialog and the opponent-response
 * UI are deferred (see docs/development.md 11h). When every opponent accepts,
 * {@link ShortcutManager#beginExecution()} runs the loop via {@link RepeatNExecutor}.</p>
 */
public class ShortcutDeclaration {
    public enum Response { PENDING, ACCEPT, LOWER, INTERRUPT, OBJECT }
    public enum Status {
        DECLARED, ACCEPTED, EXECUTING, COMPLETE,
        LOWERED, INTERRUPTED, OBJECTED, DRAW
    }

    private final Player controller;
    private final List<SpellAbility> loopAbilities;
    private final Map<Player, Response> responses = new HashMap<>();
    private int proposedCount;
    private int interruptAt;
    private Status status;

    public ShortcutDeclaration(final Player controller, final List<SpellAbility> loopAbilities, final int proposedCount) {
        this.controller = controller;
        this.loopAbilities = new ArrayList<>(loopAbilities);
        this.proposedCount = proposedCount;
        this.interruptAt = proposedCount;
        this.status = Status.DECLARED;
        for (final Player p : this.controller.getGame().getPlayers()) {
            if (!p.equals(controller)) {
                responses.put(p, Response.PENDING);
            }
        }
    }

    public Player getController() { return controller; }
    public List<SpellAbility> getLoopAbilities() { return loopAbilities; }
    public int getProposedCount() { return proposedCount; }
    public void setProposedCount(final int n) { proposedCount = n; }
    public int getInterruptAt() { return interruptAt; }
    public void setInterruptAt(final int n) { interruptAt = n; }
    public Status getStatus() { return status; }
    public void setStatus(final Status s) { status = s; }
    public Map<Player, Response> getResponses() { return responses; }

    public boolean recordResponse(final Player p, final Response r) {
        if (!responses.containsKey(p)) {
            return false;
        }
        responses.put(p, r);
        return true;
    }

    /** A mandatory loop is one with no optional/non-deterministic step. */
    public boolean isMandatoryLoop() { // doc:11h PARTIAL
        for (final SpellAbility sa : loopAbilities) {
            if (!RepeatNExecutor.canRepeatDeterministically(sa)) {
                return false;
            }
        }
        return true;
    }

    public boolean allAccepted() {
        for (final Response r : responses.values()) {
            if (r != Response.ACCEPT) {
                return false;
            }
        }
        return true;
    }
}
