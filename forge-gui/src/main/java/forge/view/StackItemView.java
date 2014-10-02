package forge.view;

import forge.game.IIdentifiable;
import forge.game.spellability.SpellAbilityStackInstance;

/**
 * Representation of a {@link forge.game.spellability.SpellAbilityStackInstance}
 * , containing only the information relevant to a user interface.
 * 
 * Conversion from and to SpellAbilityStackInstances happens through
 * {@link LocalGameView}.
 * 
 * @author elcnesh
 */
public class StackItemView implements IIdentifiable {

    private final int id;
    private final String key;
    private final int sourceTrigger;
    private final String text;
    private final CardView source;
    private final PlayerView activatingPlayer;
    private final Iterable<CardView> targetCards;
    private final Iterable<PlayerView> targetPlayers;
    private final boolean ability, optionalTrigger;
    private final StackItemView subInstance;

    public StackItemView(SpellAbilityStackInstance si, LocalGameView gameView) {
        id = si.getId();
        key = si.getSpellAbility().toUnsuppressedString();
        sourceTrigger = si.getSpellAbility().getSourceTrigger();
        text = si.getStackDescription();
        source = gameView.getCardView(si.getSourceCard(), true);
        activatingPlayer = gameView.getPlayerView(si.getActivator(), false);
        targetCards = gameView.getCardViews(si.getTargetChoices().getTargetCards(), false);
        targetPlayers = gameView.getPlayerViews(si.getTargetChoices().getTargetPlayers(), false);
        ability = si.isAbility();
        optionalTrigger = si.isOptionalTrigger();
        subInstance = si.getSubInstance() == null ? null : new StackItemView(si.getSubInstance(), gameView);
    }

    @Override
    public int getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public int getSourceTrigger() {
        return sourceTrigger;
    }

    public String getText() {
        return text;
    }

    public CardView getSource() {
        return source;
    }

    public PlayerView getActivatingPlayer() {
        return activatingPlayer;
    }

    public Iterable<CardView> getTargetCards() {
        return targetCards;
    }

    public Iterable<PlayerView> getTargetPlayers() {
        return targetPlayers;
    }

    public StackItemView getSubInstance() {
        return subInstance;
    }

    public boolean isAbility() {
        return ability;
    }

    public boolean isOptionalTrigger() {
        return optionalTrigger;
    }

    @Override
    public String toString() {
        return text;
    }
}
