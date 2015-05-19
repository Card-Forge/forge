package forge.net;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;

import forge.assets.FSkinProp;
import forge.deck.CardPool;
import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.interfaces.IGameController;
import forge.interfaces.IGuiGame;
import forge.match.NextGameDecision;
import forge.trackable.TrackableCollection;
import forge.util.ITriggerEvent;
import forge.util.ReflectionUtil;

/**
 * The methods that can be sent through this protocol.
 */
public enum ProtocolMethod {
    // Server -> Client
    setGameView         (Mode.SERVER, Void.TYPE, GameView.class),
    openView            (Mode.SERVER, Void.TYPE, TrackableCollection/*PlayerView*/.class),
    afterGameEnd        (Mode.SERVER),
    showCombat          (Mode.SERVER),
    showPromptMessage   (Mode.SERVER, Void.TYPE, PlayerView.class, String.class),
    updateButtons       (Mode.SERVER, Void.TYPE, PlayerView.class, String.class, String.class, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE),
    flashIncorrectAction(Mode.SERVER),
    updatePhase         (Mode.SERVER),
    updateTurn          (Mode.SERVER, Void.TYPE, PlayerView.class),
    updatePlayerControl (Mode.SERVER),
    enableOverlay       (Mode.SERVER),
    disableOverlay      (Mode.SERVER),
    finishGame          (Mode.SERVER),
    showManaPool        (Mode.SERVER, Void.TYPE, PlayerView.class),
    hideManaPool        (Mode.SERVER, Void.TYPE, PlayerView.class),
    updateStack         (Mode.SERVER),
    updateZones         (Mode.SERVER, Void.TYPE, Iterable/*PlayerZoneUpdate*/.class),
    updateCards         (Mode.SERVER, Void.TYPE, Iterable/*CardView*/.class),
    updateManaPool      (Mode.SERVER, Void.TYPE, Iterable/*PlayerView*/.class),
    updateLives         (Mode.SERVER, Void.TYPE, Iterable/*PlayerView*/.class),
    setPanelSelection   (Mode.SERVER, Void.TYPE, CardView.class),
    getAbilityToPlay    (Mode.SERVER, SpellAbilityView.class, CardView.class, List/*SpellAbilityView*/.class, ITriggerEvent.class),
    assignDamage        (Mode.SERVER, Map.class, CardView.class, List/*CardView*/.class, Integer.TYPE, GameEntityView.class, Boolean.TYPE),
    message             (Mode.SERVER, Void.TYPE, String.class, String.class),
    showErrorDialog     (Mode.SERVER, Void.TYPE, String.class, String.class),
    showConfirmDialog   (Mode.SERVER, Boolean.TYPE, String.class, String.class, String.class, String.class, Boolean.TYPE),
    showOptionDialog    (Mode.SERVER, Integer.TYPE, String.class, String.class, FSkinProp.class, List/*String*/.class, Integer.TYPE),
    showInputDialog     (Mode.SERVER, String.class, String.class, String.class, FSkinProp.class, String.class, List/*String*/.class),
    confirm             (Mode.SERVER, Boolean.TYPE, CardView.class, String.class, Boolean.TYPE, List/*String*/.class),
    getChoices          (Mode.SERVER, List.class, String.class, Integer.TYPE, Integer.TYPE, List.class, Object.class, Function.class),
    order               (Mode.SERVER, List.class, String.class, String.class, Integer.TYPE, Integer.TYPE, List.class, List.class, CardView.class, Boolean.TYPE),
    sideboard           (Mode.SERVER, List.class, CardPool.class, CardPool.class),
    chooseSingleEntityForEffect(Mode.SERVER, GameEntityView.class, String.class, List.class, DelayedReveal.class, Boolean.TYPE),
    setCard             (Mode.SERVER, Void.TYPE, CardView.class),
    // TODO case "setPlayerAvatar":
    openZones           (Mode.SERVER, Boolean.TYPE, Collection/*ZoneType*/.class, Map/*PlayerView,Object*/.class),
    restoreOldZones     (Mode.SERVER, Void.TYPE, Map/*PlayerView,Object*/.class),
    isUiSetToSkipPhase  (Mode.SERVER, Boolean.TYPE, PlayerView.class, PhaseType.class),

    // Client -> Server
    // Note: these should all return void, to avoid awkward situations in
    // which client and server wait for one another's response and block
    // the threads that're supposed to give that response
    useMana                   (Mode.CLIENT, Void.TYPE, Byte.TYPE),
    undoLastAction            (Mode.CLIENT, Void.TYPE, Boolean.TYPE),
    selectPlayer              (Mode.CLIENT, Void.TYPE, PlayerView.class, ITriggerEvent.class),
    selectCard                (Mode.CLIENT, Void.TYPE, CardView.class, List.class, ITriggerEvent.class),
    selectButtonOk            (Mode.CLIENT),
    selectButtonCancel        (Mode.CLIENT),
    selectAbility             (Mode.CLIENT, Void.TYPE, SpellAbilityView.class),
    passPriorityUntilEndOfTurn(Mode.CLIENT),
    passPriority              (Mode.CLIENT),
    nextGameDecision          (Mode.CLIENT, Void.TYPE, NextGameDecision.class),
    getActivateDescription    (Mode.CLIENT, Void.TYPE, String.class, CardView.class),
    concede                   (Mode.CLIENT),
    alphaStrike               (Mode.CLIENT),
    reorderHand               (Mode.CLIENT, Void.TYPE, CardView.class, Integer.TYPE);

    private enum Mode {
        SERVER(IGuiGame.class),
        CLIENT(IGameController.class);

        private final Class<?> toInvoke;
        private Mode(final Class<?> toInvoke) {
            this.toInvoke = toInvoke;
        }
    };

    private final ProtocolMethod.Mode mode;
    private final Class<?> returnType;
    private final Class<?>[] args;

    private ProtocolMethod(final ProtocolMethod.Mode mode) {
        this(mode, Void.TYPE);
    }
    private ProtocolMethod(final ProtocolMethod.Mode mode, final Class<?> returnType) {
        this(mode, returnType, (Class<?>[]) null);
    }
    @SafeVarargs
    private ProtocolMethod(final ProtocolMethod.Mode mode, final Class<?> returnType, final Class<?> ... args) {
        this.mode = mode;
        this.returnType = returnType;
        this.args = args == null ? new Class<?>[] {} : args;
    }

    public Method getMethod() {
        try {
            final Class<?> toCall = mode.toInvoke;
            final Method candidate = toCall.getMethod(name(), args);
            // Don't check Client return values for now as some use void
            // and a default return value, to improve performance
            if (mode == Mode.SERVER && !candidate.getReturnType().equals(returnType)) {
                throw new NoSuchMethodException(String.format("Wrong return type for method %s", name()));
            }
            return candidate;
        } catch (final NoSuchMethodException | SecurityException e) {
            System.err.println(String.format("Warning: class contains no accessible method named %s", name()));
            return getMethodNoArgs();
        }
    }

    private Method getMethodNoArgs() {
        try {
            return mode.toInvoke.getMethod(name(), (Class<?>[]) null);
        } catch (final NoSuchMethodException | SecurityException e) {
            System.err.println(String.format("Warning: class contains no accessible arg-less method named %s", name()));
            return null;
        }
    }
    public Class<?> getReturnType() {
        return returnType;
    }
    public Class<?>[] getArgTypes() {
        return args;
    }

    public void checkArgs(final Object[] args) {
        for (int iArg = 0; iArg < args.length; iArg++) {
            final Object arg = args[iArg];
            final Class<?> type = this.args[iArg];
            if (!ReflectionUtil.isInstance(arg, type)) {
                throw new InternalError(String.format("Protocol method %s: illegal argument (%d) of type %s, %s expected", name(), iArg, arg.getClass().getName(), type.getName()));
            }
        }
    }

    public void checkReturnValue(final Object value) {
        if (returnType.equals(Void.TYPE)) {
            // If void is expected, any return value is fine
            return;
        }
        if (!ReflectionUtil.isInstance(value, returnType)) {
            throw new IllegalStateException(String.format("Protocol method %s: illegal return object type %s returned by client, expected %s", name(), value.getClass().getName(), getReturnType().getName()));
        }
    }
}