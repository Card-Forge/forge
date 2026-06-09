package forge.game.player.actions;

import forge.game.GameEntityView;
import forge.game.card.CardView;

import java.util.regex.Pattern;

public abstract class PlayerAction {
    private static final Pattern ENTITY_ID_SUFFIX = Pattern.compile(" \\((\\d+)\\)$");

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

    public boolean isSelectionAction() {
        return false;
    }

    public boolean isTargetSelectionAction() {
        return isSelectionAction();
    }

    public boolean clearsPostStackOrderWait() {
        return isSelectionAction();
    }

    public PassPriorityAction asPassPriorityAction() {
        return null;
    }

    public CardView getSelectedCardView() {
        return null;
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

    private static String describeEntity(final GameEntityView entity) {
        return entity == null ? "" : ENTITY_ID_SUFFIX.matcher(String.valueOf(entity)).replaceAll(" $1");
    }

    protected void appendDetails(final StringBuilder sb) {
    }

    @Override
    public String toString() {
        return describe();
    }
}
