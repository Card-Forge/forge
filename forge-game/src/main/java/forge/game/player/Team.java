package forge.game.player;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight value object representing a team identity.
 * Currently wraps an int id (negative means unassigned/FFA).
 */
public final class Team {
    public static final int UNASSIGNED_ID = -1;
    public static final Team UNASSIGNED = new Team(UNASSIGNED_ID, null);

    private final int id;
    private final String name;
    private final List<Player> members = new ArrayList<>();

    // Shared life values for teams (mutable)
    private int startingLife = 20;
    private int life = 20;
    private boolean startingLifeInitialized = false;

    private int poisonThreshold = 10; // is commonly 10, but turns into 15 for 2HG

    private Team(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public static Team of(int id) {
        return id == UNASSIGNED_ID ? UNASSIGNED : new Team(id, null);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getStartingLife() {
        return startingLife;
    }

    public void setStartingLife(int startingLife) {
        this.startingLife = startingLife;
        this.startingLifeInitialized = true;
    }

    public int getLife() {
        return life;
    }

    public void setLife(int life) {
        this.life = life;
    }

    public boolean isStartingLifeInitialized() {
        return startingLifeInitialized;
    }

    @Override
    public String toString() {
        return name != null ? name : "Team(" + id + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Team other)) {
            return false;
        }
        return this.id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    public void addPlayer(Player player) {
        members.add(player);
    }

    public List<Player> getMembers() {
        return members;
    }

    public int getPoisonThreshold() {
        return poisonThreshold;
    }

    public void setPoisonThreshold(int poisonThreshold) {
        this.poisonThreshold = poisonThreshold;
    }

    public int getPoisonCounters() {
        int count = 0;
        for(Player p : members) {
            if (p.getPoisonCounters() > 0) {
                count += p.getPoisonCounters();
            }
        }
        return count;
    }
}
