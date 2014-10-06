package forge.game.spellability;

import forge.game.card.CardView;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty.SpellAbilityProp;


public class SpellAbilityView extends TrackableObject<SpellAbilityProp> {
    SpellAbilityView(SpellAbility sa) {
        super(sa.getId(), SpellAbilityProp.class);
        updateHostCard(sa);
        updateDescription(sa);

        //only update remaining properties if within Game context
        if (sa.getHostCard().getGame() == null) { return; }

        updateCanPlay(sa);
        updatePromptIfOnlyPossibleAbility(sa);
    }

    @Override
    public String toString() {
        return this.getDescription();
    }

    public CardView getHostCard() {
        return get(SpellAbilityProp.HostCard);
    }
    void updateHostCard(SpellAbility sa) {
        set(SpellAbilityProp.HostCard, CardView.get(sa.getHostCard()));
    }

    public String getDescription() {
        return get(SpellAbilityProp.Description);
    }
    void updateDescription(SpellAbility sa) {
        set(SpellAbilityProp.Description, sa.toUnsuppressedString());
    }

    public boolean canPlay() {
        return get(SpellAbilityProp.CanPlay);
    }
    void updateCanPlay(SpellAbility sa) {
        set(SpellAbilityProp.CanPlay, sa.canPlay());
    }

    public boolean promptIfOnlyPossibleAbility() {
        return get(SpellAbilityProp.PromptIfOnlyPossibleAbility);
    }
    void updatePromptIfOnlyPossibleAbility(SpellAbility sa) {
        set(SpellAbilityProp.PromptIfOnlyPossibleAbility, sa.promptIfOnlyPossibleAbility());
    }
}
