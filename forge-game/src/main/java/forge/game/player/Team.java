package forge.game.player;

/**
 * Lightweight value object representing a team identity.
 * Currently wraps an int id (negative means unassigned/FFA).
 */
public final class Team {
    public static final int UNASSIGNED_ID = -1;
    public static final Team UNASSIGNED = new Team(UNASSIGNED_ID, null);

    private final int id;
    private final String name;

    // Shared life values for teams (mutable)
    private int startingLife = 20;
    private int life = 20;
    private boolean startingLifeInitialized = false;

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
}
