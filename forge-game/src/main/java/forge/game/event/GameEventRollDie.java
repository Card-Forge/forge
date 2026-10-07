package forge.game.event;

import forge.game.PlanarDice;

public class GameEventRollDie implements GameEvent {
    public final int sides;           // 0 = unknown
    public final int result;          // natural result, 0 = unknown / planar
    public final PlanarDice planar;   // non-null only for the planar die

    public GameEventRollDie() { this(0, 0, null); }
    public GameEventRollDie(int sides, int result) { this(sides, result, null); }
    public GameEventRollDie(PlanarDice planar) { this(6, 0, planar); }
    private GameEventRollDie(int sides, int result, PlanarDice planar) {
        this.sides = sides; this.result = result; this.planar = planar;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
