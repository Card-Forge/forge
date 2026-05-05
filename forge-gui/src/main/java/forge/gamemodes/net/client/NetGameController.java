package forge.gamemodes.net.client;

import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.game.player.actions.PlayerAction;
import forge.game.spellability.SpellAbilityView;
import forge.gamemodes.match.NextGameDecision;
import forge.gamemodes.match.YieldController;
import forge.gamemodes.match.YieldUpdate;
import forge.gamemodes.net.GameProtocolSender;
import forge.gamemodes.net.ProtocolMethod;
import forge.interfaces.IDevModeCheats;
import forge.interfaces.IGameController;
import forge.interfaces.IMacroSystem;
import forge.player.AutoYieldStore;
import forge.util.ITriggerEvent;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class NetGameController implements IGameController {

    private final GameProtocolSender sender;

    /** Source of truth for this client's yield state (auto-yields, trigger decisions, markers, skip-phase, etc.). */
    private final YieldController yieldController = new YieldController(null);

    public NetGameController(final IToServer server) {
        sender = new GameProtocolSender(server);
    }

    @Override
    public YieldController getYieldController() {
        return yieldController;
    }

    private void send(final ProtocolMethod method, final Object... args) {
        sender.send(method, args);
    }

    private <T> T sendAndWait(final ProtocolMethod method, final Object... args) {
        return sender.sendAndWait(method, args);
    }

    @Override
    public void useMana(final byte color) {
        send(ProtocolMethod.useMana, color);
    }

    @Override
    public void undoLastAction() {
        send(ProtocolMethod.undoLastAction);
    }

    @Override
    public void selectPlayer(final PlayerView playerView, final ITriggerEvent triggerEvent) {
        send(ProtocolMethod.selectPlayer, playerView, null/*triggerEvent*/); //some platform don't have mousetriggerevent class or it will not allow them to click/tap
    }

    @Override
    public boolean selectCard(final CardView cardView, final List<CardView> otherCardViewsToSelect, final ITriggerEvent triggerEvent) {
        send(ProtocolMethod.selectCard, cardView, otherCardViewsToSelect, null/*triggerEvent*/); //some platform don't have mousetriggerevent class or it will not allow them to click/tap
        // Difference from local games! Always consider a card as successfully selected,
        // to avoid blocks where server and client wait for each other to respond.
        // Some cost in functionality but a huge gain in stability & speed.
        return true;
    }

    @Override
    public void selectButtonOk() {
        send(ProtocolMethod.selectButtonOk);
    }

    @Override
    public void selectButtonCancel() {
        send(ProtocolMethod.selectButtonCancel);
    }

    @Override
    public void selectAbility(final SpellAbilityView sa) {
        send(ProtocolMethod.selectAbility, sa);
    }

    @Override
    public void passPriorityUntilEndOfTurn() {
        send(ProtocolMethod.passPriorityUntilEndOfTurn);
    }

    @Override
    public void passPriority() {
        send(ProtocolMethod.passPriority);
    }

    @Override
    public void nextGameDecision(final NextGameDecision decision) {
        send(ProtocolMethod.nextGameDecision, decision);
    }

    @Override
    public boolean mayLookAtAllCards() {
        // Don't do this over network
        return false;
    }

    @Override
    public String getActivateDescription(final CardView card) {
        return sendAndWait(ProtocolMethod.getActivateDescription, card);
    }

    @Override
    public void concede() {
        send(ProtocolMethod.concede);
    }

    @Override
    public IDevModeCheats cheat() {
        // No cheating in network games!
        return IDevModeCheats.NO_CHEAT;
    }

    @Override
    public boolean canPlayUnlimitedLands() {
        // Don't do this over network
        return false;
    }

    @Override
    public void alphaStrike() {
        send(ProtocolMethod.alphaStrike);
    }

    @Override
    public void reorderHand(final CardView card, final int index) {
        send(ProtocolMethod.reorderHand, card, index);
    }

    @Override
    public void requestResync() {
        send(ProtocolMethod.requestResync);
    }

    @Override
    public boolean shouldAutoYield(final String key) {
        return yieldController.shouldAutoYield(key);
    }

    @Override
    public void setShouldAutoYield(final String key, final boolean autoYield, final boolean isAbilityScope) {
        String storageKey = yieldController.setShouldAutoYield(key, autoYield, isAbilityScope);
        send(ProtocolMethod.sendYieldUpdate, new YieldUpdate.CardAutoYield(storageKey, autoYield, isAbilityScope));
    }

    @Override
    public boolean getDisableAutoYields() { return yieldController.getDisableAutoYields(); }
    @Override
    public void setDisableAutoYields(final boolean disable) {
        yieldController.setDisableAutoYields(disable);
        send(ProtocolMethod.sendYieldUpdate, new YieldUpdate.SetDisableYields(disable));
    }

    @Override
    public AutoYieldStore.TriggerDecision getTriggerDecision(final String key) {
        return yieldController.getTriggerDecision(key);
    }

    @Override
    public void setTriggerDecision(final String key, final AutoYieldStore.TriggerDecision decision, final boolean isAbilityScope) {
        String storageKey = yieldController.setTriggerDecision(key, decision, isAbilityScope);
        send(ProtocolMethod.sendYieldUpdate, new YieldUpdate.TriggerDecision(storageKey, decision, isAbilityScope));
    }

    @Override
    public boolean getDisableAutoTriggers() { return yieldController.getDisableAutoTriggers(); }

    @Override
    public void setDisableAutoTriggers(final boolean disable) {
        yieldController.setDisableAutoTriggers(disable);
        send(ProtocolMethod.sendYieldUpdate, new YieldUpdate.SetDisableTriggers(disable));
    }

    public void setUiShouldSkipPhase(final PlayerView turnPlayer, final PhaseType phase, final boolean shouldSkip) {
        send(ProtocolMethod.sendYieldUpdate, new YieldUpdate.SkipPhase(turnPlayer, phase, shouldSkip));
    }

    @Override
    public void applyYieldUpdate(final YieldUpdate update) {
        // Local self-apply for marker/stack-yield user actions that route through
        // sendYieldUpdate. Other cases dispatch via dedicated setters above.
        if (update instanceof YieldUpdate.SetMarker u) {
            yieldController.setMarker(u.phaseOwner(), u.phase(), u.atOrPastAtClick());
        } else if (update instanceof YieldUpdate.ClearMarker) {
            yieldController.clearMarker();
        } else if (update instanceof YieldUpdate.StackYield u) {
            yieldController.setAutoPassUntilStackEmpty(u.active());
        }
    }

    /**
     * Remote client's outbound path for user-initiated yield actions (right-click
     * marker, ESC, "yield to entire stack" menu): mutate local cache for immediate
     * UI response and ship to host.
     */
    @Override
    public void sendYieldUpdate(final YieldUpdate update) {
        applyYieldUpdate(update);
        send(ProtocolMethod.sendYieldUpdate, update);
    }

    /**
     * Build a YieldStateSnapshot from the local persistent yield state plus the
     * GUI-loaded skip-phase prefs and ship it to the host in one wire message.
     */
    public void seedYieldStateOnHost(Map<PlayerView, EnumSet<PhaseType>> skipPhases) {
        send(ProtocolMethod.sendYieldUpdate, new YieldUpdate.SeedFromClient(yieldController.buildClientSnapshot(skipPhases)));
    }

    private IMacroSystem macros;
    @Override
    public IMacroSystem macros() {
        if (macros == null) {
            macros = new NetMacroSystem();
        }
        return macros;
    }
    public class NetMacroSystem implements IMacroSystem {
        @Override
        public void addRememberedAction(PlayerAction action) {
            // DO i need to send this?
        }

        @Override
        public void setRememberedActions() {
            send(ProtocolMethod.setRememberedActions);
        }

        @Override
        public void nextRememberedAction() {
            send(ProtocolMethod.nextRememberedAction);
        }

        @Override
        public boolean isRecording() {
            return false;
        }

        @Override
        public String playbackText() {
            return null;
        }
    }
}
