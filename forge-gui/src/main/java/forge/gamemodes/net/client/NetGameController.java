package forge.gamemodes.net.client;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.player.actions.PlayerAction;
import forge.game.spellability.SpellAbilityView;
import forge.gamemodes.match.NextGameDecision;
import forge.gamemodes.net.GameProtocolSender;
import forge.gamemodes.net.ProtocolMethod;
import forge.interfaces.IDevModeCheats;
import forge.interfaces.IGameController;
import forge.interfaces.IMacroSystem;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.ITriggerEvent;

import java.util.List;

public class NetGameController implements IGameController {

    private final GameProtocolSender sender;

    private final forge.player.AutoYieldStore yieldStore = new forge.player.AutoYieldStore();

    public NetGameController(final IToServer server) {
        this.sender = new GameProtocolSender(server);
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

    private forge.player.AutoYieldStore.Tier activeTier() {
        String mode = FModel.getPreferences().getPref(ForgePreferences.FPref.UI_AUTO_YIELD_MODE);
        if (ForgeConstants.AUTO_YIELD_PER_CARD.equals(mode))            return forge.player.AutoYieldStore.Tier.GAME;
        if (ForgeConstants.AUTO_YIELD_PER_ABILITY_SESSION.equals(mode)) return forge.player.AutoYieldStore.Tier.SESSION;
        return forge.player.AutoYieldStore.Tier.MATCH;
    }

    @Override
    public boolean shouldAutoYield(final String key) {
        if (yieldStore.isDisabled()) return false;
        if (activeModeIsInstall()) {
            return forge.player.PersistentYieldStore.get().contains(forge.player.AutoYieldStore.abilitySuffix(key));
        }
        String storageKey = activeModeIsAbilityScope() ? forge.player.AutoYieldStore.abilitySuffix(key) : key;
        return yieldStore.shouldYield(activeTier(), storageKey);
    }

    @Override
    public void setShouldAutoYield(final String key, final boolean autoYield, final boolean isAbilityScope) {
        if (activeModeIsInstall()) {
            forge.player.PersistentYieldStore.get().setYield(forge.player.AutoYieldStore.abilitySuffix(key), autoYield);
        } else {
            String storageKey = isAbilityScope ? forge.player.AutoYieldStore.abilitySuffix(key) : key;
            yieldStore.setYield(activeTier(), storageKey, autoYield);
        }
        send(ProtocolMethod.setShouldAutoYield, key, autoYield, isAbilityScope);
    }

    @Override
    public Iterable<String> getAutoYields() {
        return activeModeIsInstall()
                ? forge.player.PersistentYieldStore.get().getYields()
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
        return yieldStore.getTriggerDecision(trigger) == forge.player.AutoYieldStore.TriggerDecision.ACCEPT;
    }

    @Override
    public boolean shouldAlwaysDeclineTrigger(final int trigger) {
        return yieldStore.getTriggerDecision(trigger) == forge.player.AutoYieldStore.TriggerDecision.DECLINE;
    }

    @Override
    public void setShouldAlwaysAcceptTrigger(final int trigger) {
        yieldStore.setTriggerDecision(trigger, forge.player.AutoYieldStore.TriggerDecision.ACCEPT);
        send(ProtocolMethod.setShouldAlwaysAcceptTrigger, trigger);
    }

    @Override
    public void setShouldAlwaysDeclineTrigger(final int trigger) {
        yieldStore.setTriggerDecision(trigger, forge.player.AutoYieldStore.TriggerDecision.DECLINE);
        send(ProtocolMethod.setShouldAlwaysDeclineTrigger, trigger);
    }

    @Override
    public void setShouldAlwaysAskTrigger(final int trigger) {
        yieldStore.setTriggerDecision(trigger, forge.player.AutoYieldStore.TriggerDecision.ASK);
        send(ProtocolMethod.setShouldAlwaysAskTrigger, trigger);
    }

    /** Replays all currently-active yields to the host. Called from FGameClient.setGameControllers. */
    public void replayActiveYields() {
        boolean abilityScope = activeModeIsAbilityScope();
        for (String key : getAutoYields()) {
            send(ProtocolMethod.setShouldAutoYield, key, Boolean.TRUE, abilityScope);
        }
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
