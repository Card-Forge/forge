package forge.gamemodes.net.client;

import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.game.player.actions.PlayerAction;
import forge.game.spellability.SpellAbilityView;
import forge.gamemodes.match.NextGameDecision;
import forge.gamemodes.match.YieldMarker;
import forge.gamemodes.match.YieldPrefs;
import forge.gamemodes.net.GameProtocolSender;
import forge.gamemodes.net.ProtocolMethod;
import forge.gui.interfaces.IGuiGame;
import forge.interfaces.IDevModeCheats;
import forge.interfaces.IGameController;
import forge.interfaces.IMacroSystem;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.player.AutoYieldStore;
import forge.player.PersistentYieldStore;
import forge.util.ITriggerEvent;

import java.util.List;
import java.util.Map;

public class NetGameController implements IGameController {

    private final GameProtocolSender sender;
    private final IGuiGame clientGui;
    private final PlayerView playerView;

    private final AutoYieldStore yieldStore = new AutoYieldStore();

    private final java.util.EnumMap<ForgePreferences.FPref, Boolean> yieldInterruptPrefs =
            new java.util.EnumMap<>(ForgePreferences.FPref.class);

    public NetGameController(final IToServer server, final IGuiGame clientGui, final PlayerView playerView) {
        sender = new GameProtocolSender(server);
        this.clientGui = clientGui;
        this.playerView = playerView;
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
        send(ProtocolMethod.setShouldAutoYield, storageKey, autoYield, isAbilityScope);
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
        send(ProtocolMethod.setShouldAlwaysAcceptTrigger, trigger);
    }

    @Override
    public void setShouldAlwaysDeclineTrigger(final int trigger) {
        yieldStore.setTriggerDecision(trigger, AutoYieldStore.TriggerDecision.DECLINE);
        send(ProtocolMethod.setShouldAlwaysDeclineTrigger, trigger);
    }

    @Override
    public void setShouldAlwaysAskTrigger(final int trigger) {
        yieldStore.setTriggerDecision(trigger, AutoYieldStore.TriggerDecision.ASK);
        send(ProtocolMethod.setShouldAlwaysAskTrigger, trigger);
    }

    public void replayActiveYields() {
        boolean abilityScope = activeModeIsAbilityScope();
        for (String key : getAutoYields()) {
            send(ProtocolMethod.setShouldAutoYield, key, Boolean.TRUE, abilityScope);
        }
    }

    @Override
    public void setUiShouldSkipPhase(final PlayerView turnPlayer, final PhaseType phase, final boolean shouldSkip) {
        send(ProtocolMethod.setUiShouldSkipPhase, turnPlayer, phase, shouldSkip);
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

    // Delegate to the local YieldController so reads see auto-cleared state from server-driven syncs.
    @Override
    public YieldMarker getYieldMarker() {
        return clientGui.getCurrentYieldMarker(playerView);
    }

    @Override
    public void setYieldMarker(final PlayerView phaseOwner, final PhaseType phase) {
        if (phaseOwner == null || phase == null) {
            clearYieldMarker();
            return;
        }
        clientGui.activateYieldMarker(playerView, new YieldMarker(phaseOwner, phase));
        send(ProtocolMethod.setYieldMarker, phaseOwner, phase);
    }

    @Override
    public void clearYieldMarker() {
        clientGui.clearYieldMarker(playerView);
        send(ProtocolMethod.clearYieldMarker);
    }

    @Override
    public boolean isStackYieldActive() {
        return clientGui.isCurrentStackYieldActive(playerView);
    }

    @Override
    public void setStackYield(final boolean active) {
        clientGui.setStackYieldUiState(playerView, active);
        send(ProtocolMethod.setStackYield, active);
    }

    @Override
    public boolean getYieldInterruptPref(final ForgePreferences.FPref pref) {
        Boolean stored = yieldInterruptPrefs.get(pref);
        return stored != null ? stored : "true".equals(pref.getDefault());
    }

    @Override
    public void setYieldInterruptPref(final ForgePreferences.FPref pref, final boolean value) {
        yieldInterruptPrefs.put(pref, value);
        send(ProtocolMethod.setYieldInterruptPref, pref, value);
    }

    @Override
    public YieldPrefs getYieldPrefs() {
        return new YieldPrefs(this);
    }

    @Override
    public void setYieldPrefs(final YieldPrefs prefs) {
        if (prefs == null) return;
        this.yieldInterruptPrefs.clear();
        for (Map.Entry<ForgePreferences.FPref, Boolean> e : prefs.getInterrupts().entrySet()) {
            this.yieldInterruptPrefs.put(e.getKey(), e.getValue());
        }
        send(ProtocolMethod.setYieldPrefs, prefs);
    }
}
