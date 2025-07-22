package forge.ai;

public class AiAbilityDecision {
    private static int MIN_RATING = 30;

    private final int rating;
    private final AiPlayDecision decision;

    public AiAbilityDecision(int rating, AiPlayDecision decision) {
        this.rating = rating;
        this.decision = decision;
    }

    public int getRating() {
        return rating;
    }

    public AiPlayDecision getDecision() {
        return decision;
    }

    public boolean willingToPlay() {
        return rating > MIN_RATING && decision.willingToPlay();
    }
}
