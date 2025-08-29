package forge.game.event;

import forge.game.player.Player;

public record GameEventSurveil(Player player, int toLibrary, int toGraveyard) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "" + player + " surveilled " + toLibrary + " to library, " + toGraveyard + " to graveyard";
    }
}
