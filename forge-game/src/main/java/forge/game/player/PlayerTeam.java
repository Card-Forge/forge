package forge.game.player;

import forge.TeamColor;

/**
 * Represents a team of players within a game.
 *
 * <p>Each {@link Player} that shares the same numeric team ID belongs to the
 * same {@code PlayerTeam}.  The object aggregates the live member list and
 * carries optional display metadata (color) chosen at lobby time.</p>
 */
public class PlayerTeam {

    private final int id;
    private TeamColor color;
    private final PlayerCollection members = new PlayerCollection();

    /**
     * @param id    the team number (mirrors {@link Player#getTeam()})
     * @param color the display color chosen in the lobby; may be {@code null}
     *              (defaults to {@link TeamColor#NONE})
     */
    public PlayerTeam(final int id, final TeamColor color) {
        this.id = id;
        this.color = color == null ? TeamColor.NONE : color;
    }

    /** @return the numeric team identifier */
    public int getId() {
        return id;
    }

    /** @return the display color of this team (never {@code null}) */
    public TeamColor getColor() {
        return color;
    }

    /** Overwrites the team color (e.g. if updated mid-lobby). */
    public void setColor(final TeamColor color) {
        this.color = color == null ? TeamColor.NONE : color;
    }

    /**
     * @return a live, mutable collection of the {@link Player}s on this team.
     *         Do not modify the returned collection from outside this class;
     *         use {@link #addMember(Player)} instead.
     */
    public PlayerCollection getMembers() {
        return members;
    }

    /**
     * Adds {@code player} to the member list if not already present.
     *
     * @param player the player to add
     */
    public void addMember(final Player player) {
        if (!members.contains(player)) {
            members.add(player);
        }
    }

    /** @return the number of players on this team */
    public int size() {
        return members.size();
    }

    @Override
    public String toString() {
        return "Team " + id + " [" + color.getDisplayName() + ", " + members.size() + " member(s)]";
    }
}

