package forge.gamemodes.net.client;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.player.actions.PlayerAction;
import forge.game.spellability.SpellAbilityView;
import forge.gamemodes.match.NextGameDecision;
import forge.gamemodes.match.YieldMode;
import forge.gamemodes.match.YieldPrefs;
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
import java.util.Map;
import java.util.Set;

public class NetGameController implements IGameController {

    private final GameProtocolSender sender;

    // Local mirror of yield state for UI display
    private final Set<String> autoYields = Sets.newHashSet();
    private final Map<Integer, Boolean> triggersAlwaysAccept = Maps.newTreeMap();
    private boolean disableAutoYields;

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

    @Override
    public boolean shouldAutoYield(final String key) {
        String abilityKey = key.contains("): ") ? key.substring(key.indexOf("): ") + 3) : key;
        boolean yieldPerAbility = FModel.getPreferences().getPref(ForgePreferences.FPref.UI_AUTO_YIELD_MODE)
                .equals(ForgeConstants.AUTO_YIELD_PER_ABILITY);
        return !disableAutoYields && autoYields.contains(yieldPerAbility ? abilityKey : key);
    }

    @Override
    public void setShouldAutoYield(final String key, final boolean autoYield) {
        String abilityKey = key.contains("): ") ? key.substring(key.indexOf("): ") + 3) : key;
        boolean yieldPerAbility = FModel.getPreferences().getPref(ForgePreferences.FPref.UI_AUTO_YIELD_MODE)
                .equals(ForgeConstants.AUTO_YIELD_PER_ABILITY);
        if (autoYield) {
            autoYields.add(yieldPerAbility ? abilityKey : key);
        } else {
            autoYields.remove(yieldPerAbility ? abilityKey : key);
        }
        send(ProtocolMethod.setShouldAutoYield, key, autoYield);
    }

    @Override
    public Iterable<String> getAutoYields() {
        return autoYields;
    }

    @Override
    public void clearAutoYields() {
        autoYields.clear();
        triggersAlwaysAccept.clear();
    }

    @Override
    public boolean getDisableAutoYields() {
        return disableAutoYields;
    }

    @Override
    public void setDisableAutoYields(final boolean disable) {
        disableAutoYields = disable;
    }

    @Override
    public boolean shouldAlwaysAcceptTrigger(final int trigger) {
        return Boolean.TRUE.equals(triggersAlwaysAccept.get(trigger));
    }

    @Override
    public boolean shouldAlwaysDeclineTrigger(final int trigger) {
        return Boolean.FALSE.equals(triggersAlwaysAccept.get(trigger));
    }

    @Override
    public void setShouldAlwaysAcceptTrigger(final int trigger) {
        triggersAlwaysAccept.put(trigger, Boolean.TRUE);
        send(ProtocolMethod.setShouldAlwaysAcceptTrigger, trigger);
    }

    @Override
    public void setShouldAlwaysDeclineTrigger(final int trigger) {
        triggersAlwaysAccept.put(trigger, Boolean.FALSE);
        send(ProtocolMethod.setShouldAlwaysDeclineTrigger, trigger);
    }

    @Override
    public void setShouldAlwaysAskTrigger(final int trigger) {
        triggersAlwaysAccept.remove(trigger);
        send(ProtocolMethod.setShouldAlwaysAskTrigger, trigger);
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

    @Override
    public void notifyYieldStateChanged(PlayerView player, YieldMode mode, YieldPrefs prefs) {
        send(ProtocolMethod.notifyYieldStateChanged, player, mode, prefs);
    }
}
