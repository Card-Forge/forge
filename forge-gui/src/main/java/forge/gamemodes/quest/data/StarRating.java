package forge.gamemodes.quest.data;

public class StarRating {
    public String Name;
    public String Edition;
    public int rating;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((Edition == null) ? 0 : Edition.hashCode());
        result = prime * result + ((Name == null) ? 0 : Name.hashCode());
        result = prime * result + rating;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        StarRating other = (StarRating) obj;
        if (Edition == null) {
            if (other.Edition != null) {
                return false;
            }
        } else if (!Edition.equals(other.Edition)) {
            return false;
        }

        if (Name == null) {
            if (other.Name != null) {
                return false;
            }
        } else if (!Name.equals(other.Name)) {
            return false;
        }

        if (rating != other.rating) {
            return false;
        }

        return true;
    }
}
