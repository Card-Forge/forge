package forge.net;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;

import forge.UiCommand;
import forge.assets.FSkinProp;
import forge.deck.CardPool;
import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.interfaces.IButton;
import forge.interfaces.IGuiGame;
import forge.match.MatchButtonType;
import forge.trackable.TrackableCollection;
import forge.util.ITriggerEvent;

public final class GameProtocol {

    /**
     * Private constructor to prevent instantiation.
     */
    private GameProtocol() {
    }

    public static ProtocolMethod getProtocolMethod(final String name) {
        return ProtocolMethod.valueOf(name);
    }

    /**
     * The methods that can be sent through this protocol.
     */
    public enum ProtocolMethod {
        setGameView(Void.TYPE, GameView.class),
        openView(Void.TYPE, TrackableCollection.class),
        afterGameEnd(),
        showCombat(),
        showPromptMessage(Void.TYPE, PlayerView.class, String.class),
        stopAtPhase(Boolean.TYPE, PlayerView.class, PhaseType.class),
        focusButton(Void.TYPE, MatchButtonType.class),
        flashIncorrectAction(),
        updatePhase(),
        updateTurn(Void.TYPE, PlayerView.class),
        updatePlayerControl(),
        enableOverlay(),
        disableOverlay(),
        finishGame(),
        showManaPool(Void.TYPE, PlayerView.class),
        hideManaPool(Void.TYPE, PlayerView.class),
        updateStack(),
        updateZones(Void.TYPE, Iterable.class),
        updateCards(Void.TYPE, Iterable.class),
        updateManaPool(Void.TYPE, Iterable.class),
        updateLives(Void.TYPE, Iterable.class),
        setPanelSelection(Void.TYPE, CardView.class),
        getAbilityToPlay(SpellAbilityView.class, List.class, ITriggerEvent.class),
        assignDamage(Map.class, CardView.class, List.class, Integer.TYPE, GameEntityView.class, Boolean.TYPE),
        message(Void.TYPE, String.class, String.class),
        showErrorDialog(Void.TYPE, String.class, String.class),
        showConfirmDialog(Boolean.TYPE, String.class, String.class, String.class, String.class, Boolean.TYPE),
        showOptionDialog(Integer.TYPE, String.class, String.class, FSkinProp.class, Array.class, Integer.TYPE),
        showCardOptionDialog(Integer.TYPE, CardView.class, String.class, String.class, FSkinProp.class, String.class, Array.class),
        showInputDialog(String.class, String.class, String.class, FSkinProp.class, String.class, Array.class),
        confirm(Boolean.TYPE, CardView.class, String.class, Boolean.TYPE, Array.class),
        getChoices(List.class, String.class, Integer.TYPE, Integer.TYPE, Collection.class, Object.class, Function.class),
        order(List.class, String.class, String.class, Integer.TYPE, Integer.TYPE, List.class, List.class, CardView.class, Boolean.TYPE),
        sideboard(List.class, CardPool.class, CardPool.class),
        chooseSingleEntityForEffect(GameEntityView.class, String.class, TrackableCollection.class, DelayedReveal.class, Boolean.TYPE),
        setCard(Void.TYPE, CardView.class),
        // TODO case "setPlayerAvatar":
        openZones(Boolean.TYPE, Collection.class, Map.class),
        restoreOldZones(Void.TYPE, Map.class),
        isUiSetToSkipPhase(Boolean.TYPE, PlayerView.class, PhaseType.class),
        // BUTTONS
        btn_setEnabled(Void.TYPE, Boolean.TYPE),
        btn_setVisible(Void.TYPE, Boolean.TYPE),
        btn_setText(Void.TYPE, String.class),
        btn_isSelected(Boolean.TYPE),
        btn_setSelected(Void.TYPE, Boolean.TYPE),
        btn_requestFocusInWindows(Boolean.TYPE),
        btn_setCommand(Void.TYPE, UiCommand.class),
        btn_setImage(Void.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE),
        btn_setTextImage(Void.TYPE, FSkinProp.class);

        private final Class<?> returnType;
        private final Class<?>[] args;

        private ProtocolMethod() {
            this(Void.TYPE);
        }
        private ProtocolMethod(final Class<?> returnType) {
            this(returnType, (Class<?>[]) null);
        }
        @SafeVarargs
        private ProtocolMethod(final Class<?> returnType, final Class<?> ... args) {
            this.returnType = returnType;
            this.args = args == null ? new Class<?>[] {} : args;
        }

        public Method getMethod() {
            try {
                final String name;
                final Class<?> toCall;
                if (name().startsWith("btn_")) {
                    name = name().substring("btn_".length());
                    toCall = IButton.class;
                } else {
                    name = name();
                    toCall = IGuiGame.class;
                }

                final Method candidate = toCall.getMethod(name, args);
                if (!candidate.getReturnType().equals(returnType)) {
                    throw new NoSuchMethodException(String.format("Wrong return type for method %s", name()));
                }
                return candidate;
            } catch (final NoSuchMethodException | SecurityException e) {
                System.err.println(String.format("Warning: class contains no method named %s", name()));
                return null;
            }
        }

        public boolean invokeOnButton() {
            return name().startsWith("btn_");
        }
    }
}
