package forge.adventure.data;

import com.badlogic.gdx.graphics.Color;

/**
 * Canonical challenge ratings for Adventure points of interest.
 */
public enum ChallengeRating {
    LOW("low", Color.GREEN, "GREEN"),
    MEDIUM("medium", Color.YELLOW, "YELLOW"),
    HARD("hard", Color.ORANGE, "ORANGE"),
    VERY_HARD("veryHard", Color.RED, "RED");

    private final String value;
    private final Color color;
    private final String markupColor;

    ChallengeRating(String value, Color color, String markupColor) {
        this.value = value;
        this.color = new Color(color);
        this.markupColor = markupColor;
    }

    public Color getColor() {
        return new Color(color);
    }

    public String getNotificationBullet() {
        return " [%50][" + markupColor + "][+ChallengeDot][%100][BLACK]";
    }

    public static ChallengeRating from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (ChallengeRating rating : values()) {
            if (rating.value.equals(value)) {
                return rating;
            }
        }
        throw new IllegalArgumentException("Invalid challenge rating: " + value);
    }
}
