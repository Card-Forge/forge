package forge.ai;

public record AiAbilityDecision(int rating, AiPlayDecision decision) {
    private static int MIN_RATING = 30;

    public boolean willingToPlay() {
        return rating > MIN_RATING && decision.willingToPlay();
    }
}
