package forge.gamemodes.net.client;

import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.game.player.actions.PlayerAction;
import forge.game.spellability.SpellAbilityView;
import forge.gamemodes.match.NextGameDecision;
import forge.gamemodes.match.YieldController;
import forge.gamemodes.match.YieldStateSnapshot;
import forge.gamemodes.match.YieldUpdate;
import forge.gamemodes.net.GameProtocolSender;
import forge.gamemodes.net.ProtocolMethod;
import forge.interfaces.IDevModeCheats;
import forge.interfaces.IGameController;
import forge.interfaces.IMacroSystem;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.player.AutoYieldStore;
import forge.player.PersistentYieldStore;
import forge.util.ITriggerEvent;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NetGameController implements IGameController {

    private final GameProtocolSender sender;

    private final AutoYieldStore yieldStore = new AutoYieldStore();

    /** Local cache mirroring host-side state for client-side UI rendering. */
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

    private boolean activeModeIsInstall() {
        return ForgeConstants.AUTO_YIELD_PER_ABILITY_INSTALL.equals(
                FModel.getPreferences().getPref(ForgePreferences.FPref.UI_AUTO_YIELD_MODE));
    }

    private boolean activeModeIsAbilityScope() {
        return !ForgeConstants.AUTO_YIELD_PER_CARD.equals(
                FModel.getPreferences().getPref(ForgePreferences.FPref.UI_AUTO_YIELD_MODE));
    }

    private AutoYieldStore.Tier activeTier() {
        String mode = FModel.getPreferences().getPref(ForgePreferences.FPref.UI_AUTO_YIELD_MODE);
        if (ForgeConstants.AUTO_YIELD_PER_CARD.equals(mode))            return AutoYieldStore.Tier.GAME;
        if (ForgeConstants.AUTO_YIELD_PER_ABILITY_SESSION.equals(mode)) return AutoYieldStore.Tier.SESSION;
        return AutoYieldStore.Tier.MATCH;
    }

    @Override
    public boolean shouldAutoYield(final String key) {
        if (yieldStore.isDisabled()) return false;
        if (activeModeIsInstall()) {
            return PersistentYieldStore.get().contains(AutoYieldStore.abilitySuffix(key));
        }
        String storageKey = activeModeIsAbilityScope() ? AutoYieldStore.abilitySuffix(key) : key;
        return yieldStore.shouldYield(activeTier(), storageKey);
    }

    @Override
    public void setShouldAutoYield(final String key, final boolean autoYield, final boolean isAbilityScope) {
        String storageKey = isAbilityScope ? AutoYieldStore.abilitySuffix(key) : key;
        if (activeModeIsInstall()) {
            PersistentYieldStore.get().setYield(storageKey, autoYield);
        } else {
            yieldStore.setYield(activeTier(), storageKey, autoYield);
        }
        yieldController.setCardAutoYield(storageKey, autoYield, isAbilityScope);
        send(ProtocolMethod.sendYieldUpdate,
                new YieldUpdate.SetCardAutoYield(storageKey, autoYield, isAbilityScope));
    }

    @Override
    public Iterable<String> getAutoYields() {
        return activeModeIsInstall()
                ? PersistentYieldStore.get().getYields()
                : yieldStore.getYields(activeTier());
    }

    @Override
    public void clearAutoYields() {
        // No-op locally: tier lifecycle is driven separately. Server-side mirror is cleared by HostedMatch.
    }

    @Override
    public boolean getDisableAutoYields() { return yieldStore.isDisabled(); }

    @Override
    public void setDisableAutoYields(final boolean disable) { yieldStore.setDisabled(disable); }

    @Override
    public boolean shouldAlwaysAcceptTrigger(final int trigger) {
        return yieldStore.getTriggerDecision(trigger) == AutoYieldStore.TriggerDecision.ACCEPT;
    }

    @Override
    public boolean shouldAlwaysDeclineTrigger(final int trigger) {
        return yieldStore.getTriggerDecision(trigger) == AutoYieldStore.TriggerDecision.DECLINE;
    }

    @Override
    public void setShouldAlwaysAcceptTrigger(final int trigger) {
        yieldStore.setTriggerDecision(trigger, AutoYieldStore.TriggerDecision.ACCEPT);
        yieldController.setTriggerDecision(trigger, AutoYieldStore.TriggerDecision.ACCEPT);
        send(ProtocolMethod.sendYieldUpdate,
                new YieldUpdate.SetTriggerDecision(trigger, AutoYieldStore.TriggerDecision.ACCEPT));
    }

    @Override
    public void setShouldAlwaysDeclineTrigger(final int trigger) {
        yieldStore.setTriggerDecision(trigger, AutoYieldStore.TriggerDecision.DECLINE);
        yieldController.setTriggerDecision(trigger, AutoYieldStore.TriggerDecision.DECLINE);
        send(ProtocolMethod.sendYieldUpdate,
                new YieldUpdate.SetTriggerDecision(trigger, AutoYieldStore.TriggerDecision.DECLINE));
    }

    @Override
    public void setShouldAlwaysAskTrigger(final int trigger) {
        yieldStore.setTriggerDecision(trigger, AutoYieldStore.TriggerDecision.ASK);
        yieldController.setTriggerDecision(trigger, AutoYieldStore.TriggerDecision.ASK);
        send(ProtocolMethod.sendYieldUpdate,
                new YieldUpdate.SetTriggerDecision(trigger, AutoYieldStore.TriggerDecision.ASK));
    }

    /**
     * Build a YieldStateSnapshot from the local persistent yield state plus the
     * GUI-loaded skip-phase prefs and ship it to the host in one wire message.
     */
    public void seedYieldStateOnHost(Map<PlayerView, EnumSet<PhaseType>> skipPhases) {
        Set<String> cardYields = new HashSet<>();
        Set<String> abilityYields = new HashSet<>();
        boolean abilityScope = activeModeIsAbilityScope();
        for (String key : getAutoYields()) {
            if (abilityScope) abilityYields.add(key);
            else cardYields.add(key);
        }
        // Trigger decisions are per-game; deltas flow during play.
        Map<Integer, AutoYieldStore.TriggerDecision> triggers = new HashMap<>();
        YieldStateSnapshot snap = new YieldStateSnapshot(
                cardYields, abilityYields, triggers, yieldStore.isDisabled(),
                skipPhases == null ? new HashMap<>() : skipPhases);
        send(ProtocolMethod.sendYieldUpdate, new YieldUpdate.SeedFromClient(snap));
    }

    @Override
    public void setUiShouldSkipPhase(final PlayerView turnPlayer, final PhaseType phase, final boolean shouldSkip) {
        yieldController.setSkipPhase(turnPlayer, phase, shouldSkip);
        send(ProtocolMethod.sendYieldUpdate, new YieldUpdate.SetSkipPhase(turnPlayer, phase, shouldSkip));
    }

    @Override
    public void applyYieldUpdate(final YieldUpdate update) {
        if (update instanceof YieldUpdate.SetMarker u) {
            yieldController.setMarker(u.phaseOwner(), u.phase());
        } else if (update instanceof YieldUpdate.ClearMarker) {
            yieldController.clearMarker();
        } else if (update instanceof YieldUpdate.SetStackYield u) {
            yieldController.setStackYield(u.active());
        } else if (update instanceof YieldUpdate.SetTriggerDecision u) {
            yieldController.setTriggerDecision(u.trigId(), u.decision());
        } else if (update instanceof YieldUpdate.SetCardAutoYield u) {
            yieldController.setCardAutoYield(u.cardKey(), u.active(), u.abilityScope());
        } else if (update instanceof YieldUpdate.SetSkipPhase u) {
            yieldController.setSkipPhase(u.turnPlayer(), u.phase(), u.skip());
        }
        // SeedFromClient: no-op on client side; client does not apply its own seed.
    }

    /**
     * User-initiated yield update from local UI: apply to local cache for
     * immediate UI response AND ship to host so the authoritative YieldController
     * stays in sync. The default IGameController.sendYieldUpdate just calls
     * applyYieldUpdate (host-only), which would silently drop the wire send
     * for remote clients.
     */
    @Override
    public void sendYieldUpdate(final YieldUpdate update) {
        applyYieldUpdate(update);
        send(ProtocolMethod.sendYieldUpdate, update);
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
