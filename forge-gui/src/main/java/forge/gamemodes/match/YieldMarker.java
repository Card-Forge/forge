package forge.gamemodes.match;

import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;

import java.io.Serializable;
import java.util.Objects;

/** Immutable (phaseOwner, phase) target of a yield-until-phase intent. */
public final class YieldMarker implements Serializable {
    private static final long serialVersionUID = 1L;

    private final PlayerView phaseOwner;
    private final PhaseType phase;

    public YieldMarker(PlayerView phaseOwner, PhaseType phase) {
        this.phaseOwner = Objects.requireNonNull(phaseOwner);
        this.phase = Objects.requireNonNull(phase);
    }

    public PlayerView getPhaseOwner() { return phaseOwner; }
    public PhaseType getPhase()       { return phase; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof YieldMarker)) return false;
        YieldMarker other = (YieldMarker) o;
        return phaseOwner.equals(other.phaseOwner) && phase == other.phase;
    }
    @Override
    public int hashCode() { return Objects.hash(phaseOwner, phase); }
    @Override
    public String toString() { return phaseOwner + "@" + phase; }
}
