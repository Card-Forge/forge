package forge.view;

public class StackItemView {

    final String key;
    final int sourceTrigger;
    final String text;
    final CardView source;
    final PlayerView activatingPlayer;
    final boolean ability, optionalTrigger;

    public StackItemView(final String key, final int sourceTrigger, final String text, final CardView source, final PlayerView activatingPlayer, final boolean isAbility, final boolean isOptionalTrigger) {
        this.key = key;
        this.sourceTrigger = sourceTrigger;
        this.text = text;
        this.source = source;
        this.activatingPlayer = activatingPlayer;
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

    public boolean isAbility() {
        return ability;
    }

    public boolean isOptionalTrigger() {
        return optionalTrigger;
    }

}
