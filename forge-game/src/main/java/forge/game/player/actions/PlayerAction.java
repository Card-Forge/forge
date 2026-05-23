package forge.game.player.actions;

import forge.game.GameEntityView;
import forge.game.player.PlayerController;
import forge.util.Localizer;

import java.util.List;
import java.util.regex.Pattern;

public abstract class PlayerAction {
    private static final Pattern ENTITY_ID_SUFFIX = Pattern.compile(" \\((\\d+)\\)$");
    private static final Localizer LOCALIZER = Localizer.getInstance();

    private final String name;
    private final GameEntityView gameEntityView;

    public PlayerAction(GameEntityView cardView) {
        gameEntityView = cardView;
        name = null;
    }

    public PlayerAction(final GameEntityView cardView, final String actionName) {
        gameEntityView = cardView;
        name = actionName;
    }

    public void run(PlayerController controller) {
        // Turn this abstract soon
        // This should try to replicate the recorded macro action
    }

    public GameEntityView getGameEntityView() {
        return gameEntityView;
    }

    public String describe() {
        final StringBuilder sb = new StringBuilder(name == null ? getClass().getSimpleName() : name);
        final String entity = describeEntity();
        if (!entity.isEmpty()) {
            sb.append(": ").append(entity);
        }
        appendDetails(sb);
        return sb.toString();
    }

    protected String describeEntity() {
        return gameEntityView == null ? "" : describeEntity(gameEntityView);
    }

    protected static String describeEntity(final GameEntityView entity) {
        return entity == null ? "" : ENTITY_ID_SUFFIX.matcher(String.valueOf(entity)).replaceAll(" $1");
    }

    protected static String describeList(final List<String> values) {
        if (values == null || values.isEmpty()) {
            return localize("lblMacroNone");
        }
        final StringBuilder sb = new StringBuilder();
        for (final String value : values) {
            sb.append('\n').append("- ").append(value);
        }
        return sb.toString();
    }

    protected static String localize(final String key, final Object... args) {
        return LOCALIZER.getMessage(key, args);
    }

    protected void appendDetails(final StringBuilder sb) {
    }

    @Override
    public String toString() {
        return describe();
    }
}
