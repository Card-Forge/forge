package forge.view;

import forge.game.IIdentifiable;

/**
 * Representation of a {@link forge.game.spellability.SpellAbility}, containing
 * only the information relevant to a user interface.
 * 
 * Conversion from and to SpellAbilities happens through {@link LocalGameView}.
 * 
 * @author elcnesh
 */
public class SpellAbilityView implements IIdentifiable {

    private final int id;
    private CardView hostCard;
    private String description;
    private boolean canPlay, promptIfOnlyPossibleAbility;

    public SpellAbilityView(final int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return this.getDescription();
    }

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
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
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
