package forge.view;

public class StackItemView {

    private final String key;
    private final int sourceTrigger;
    private final String text;
    private final CardView source;
    private final PlayerView activatingPlayer;
    private final Iterable<CardView> targetCards;
    private final Iterable<PlayerView> targetPlayers;
    private final boolean ability, optionalTrigger;

    public StackItemView(final String key, final int sourceTrigger,
            final String text, final CardView source,
            final PlayerView activatingPlayer,
            final Iterable<CardView> targetCards,
            final Iterable<PlayerView> targetPlayers, final boolean isAbility,
            final boolean isOptionalTrigger) {
        this.key = key;
        this.sourceTrigger = sourceTrigger;
        this.text = text;
        this.source = source;
        this.activatingPlayer = activatingPlayer;
        this.targetCards = targetCards;
        this.targetPlayers = targetPlayers;
        this.ability = isAbility;
        this.optionalTrigger = isOptionalTrigger;
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

    public boolean isAbility() {
        return ability;
    }

    public boolean isOptionalTrigger() {
        return optionalTrigger;
    }

    @Override
    public String toString() {
        return this.getText();
    }
}
