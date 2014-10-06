package forge.game.spellability;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty.StackItemProp;


public class StackItemView extends TrackableObject<StackItemProp> {
    public StackItemView(SpellAbilityStackInstance si) {
        super(si.getId(), StackItemProp.class);
        updateKey(si);
        updateSourceTrigger(si);
        updateText(si);
        updateSourceCard(si);
        updateActivator(si);
        updateTargetCards(si);
        updateTargetPlayers(si);
        updateAbility(si);
        updateOptionalTrigger(si);
        updateSubInstance(si);
    }

    public String getKey() {
        return get(StackItemProp.Key);
    }
    void updateKey(SpellAbilityStackInstance si) {
        set(StackItemProp.Key, si.getSpellAbility(false).toUnsuppressedString());
    }

    public int getSourceTrigger() {
        return get(StackItemProp.SourceTrigger);
    }
    void updateSourceTrigger(SpellAbilityStackInstance si) {
        set(StackItemProp.SourceTrigger, si.getSpellAbility(false).getSourceTrigger());
    }

    public String getText() {
        return get(StackItemProp.Text);
    }
    void updateText(SpellAbilityStackInstance si) {
        set(StackItemProp.Text, si.getStackDescription());
    }

    public CardView getSourceCard() {
        return get(StackItemProp.SourceCard);
    }
    void updateSourceCard(SpellAbilityStackInstance si) {
        set(StackItemProp.SourceCard, si.getSourceCard());
    }

    public PlayerView getActivator() {
        return get(StackItemProp.Activator);
    }
    void updateActivator(SpellAbilityStackInstance si) {
        set(StackItemProp.Activator, PlayerView.get(si.getActivator()));
    }

    public Iterable<CardView> getTargetCards() {
        return get(StackItemProp.TargetCards);
    }
    void updateTargetCards(SpellAbilityStackInstance si) {
        set(StackItemProp.TargetCards, CardView.getCollection(si.getTargetChoices().getTargetCards()));
    }

    public Iterable<PlayerView> getTargetPlayers() {
        return get(StackItemProp.TargetPlayers);
    }
    void updateTargetPlayers(SpellAbilityStackInstance si) {
        set(StackItemProp.TargetPlayers, PlayerView.getCollection(si.getTargetChoices().getTargetPlayers()));
    }

    public boolean isAbility() {
        return get(StackItemProp.Ability);
    }
    void updateAbility(SpellAbilityStackInstance si) {
        set(StackItemProp.Ability, si.isAbility());
    }

    public boolean isOptionalTrigger() {
        return get(StackItemProp.OptionalTrigger);
    }
    void updateOptionalTrigger(SpellAbilityStackInstance si) {
        set(StackItemProp.OptionalTrigger, si.isOptionalTrigger());
    }

    public StackItemView getSubInstance() {
        return get(StackItemProp.SubInstance);
    }
    void updateSubInstance(SpellAbilityStackInstance si) {
        set(StackItemProp.SubInstance, si.getSubInstance() == null ? null : new StackItemView(si.getSubInstance()));
    }

    @Override
    public String toString() {
        return getText();
    }
}
