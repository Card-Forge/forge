package forge.view;

public class SpellAbilityView {

    private CardView hostCard;
    private boolean canPlay, promptIfOnlyPossibleAbility;

    /**
     * @return the hostCard
     */
    public CardView getHostCard() {
        return hostCard;
    }

    /**
     * @param hostCard the hostCard to set
     */
    public void setHostCard(CardView hostCard) {
        this.hostCard = hostCard;
    }

    /**
     * @return the canPlay
     */
    public boolean canPlay() {
        return canPlay;
    }

    /**
     * @param canPlay the canPlay to set
     */
    public void setCanPlay(boolean canPlay) {
        this.canPlay = canPlay;
    }

    /**
     * @return the promptIfOnlyPossibleAbility
     */
    public boolean isPromptIfOnlyPossibleAbility() {
        return promptIfOnlyPossibleAbility;
    }

    /**
     * @param promptIfOnlyPossibleAbility the promptIfOnlyPossibleAbility to set
     */
    public void setPromptIfOnlyPossibleAbility(boolean promptIfOnlyPossibleAbility) {
        this.promptIfOnlyPossibleAbility = promptIfOnlyPossibleAbility;
    }

}
