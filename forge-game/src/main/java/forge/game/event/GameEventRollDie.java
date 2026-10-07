package forge.game.event;

import forge.game.PlanarDice;

/**
 * GameEvent fired when a die is rolled.
 * Must remain a record type to pass Forge's automated serialization test suite.
 */
public record GameEventRollDie(int sides, int result, PlanarDice planar) implements GameEvent {

    // Overloaded constructor for empty/unknown defaults (0, 0, null)
    public GameEventRollDie() {
        this(0, 0, null);
    }

    // Overloaded constructor for standard numeric D6 rolls
    public GameEventRollDie(int sides, int result) {
        this(sides, result, null);
    }

    // Overloaded constructor for Planar dice rolls (Defaults to a 6-sided framework)
    public GameEventRollDie(PlanarDice planar) {
        this(6, 0, planar);
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
