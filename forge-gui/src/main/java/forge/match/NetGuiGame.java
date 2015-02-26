package forge.match;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;

import forge.LobbyPlayer;
import forge.UiCommand;
import forge.assets.FSkinProp;
import forge.deck.CardPool;
import forge.game.GameEntity;
import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.IHasIcon;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.interfaces.IButton;
import forge.item.PaperCard;
import forge.net.game.GuiGameEvent;
import forge.net.game.server.IToClient;
import forge.player.PlayerControllerHuman;
import forge.trackable.TrackableObject;
import forge.util.FCollectionView;
import forge.util.ITriggerEvent;

public class NetGuiGame extends AbstractGuiGame {

    private final IToClient client;
    public NetGuiGame(final IToClient client) {
        this.client = client;
    }

    private void send(final String method) {
        send(method, Collections.<TrackableObject>emptySet());
    }
    private void send(final String method, final TrackableObject object) {
        send(method, Collections.singleton(object));
    }
    private void send(final String method, final Iterable<? extends TrackableObject> objects) {
        client.send(new GuiGameEvent(method, objects));
    }

    @Override
    public void setGameView(final GameView gameView) {
        super.setGameView(gameView);
        send("setGameView", gameView);
    }

    @Override
    public boolean resetForNewGame() {
        send("resetForNewGame");
        return true;
    }

    @Override
    public void openView(final Iterable<PlayerView> myPlayers) {
        send("openView", myPlayers);
    }

    @Override
    public void afterGameEnd() {
        send("afterGameEnd");
    }

    @Override
    public void showCombat() {
        send("showCombat");
    }

    @Override
    public void showPromptMessage(PlayerView playerView, String message) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean stopAtPhase(PlayerView playerTurn, PhaseType phase) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public IButton getBtnOK(final PlayerView playerView) {
        return new NetButton(playerView, "OK");
    }

    @Override
    public IButton getBtnCancel(final PlayerView playerView) {
        return new NetButton(playerView, "Cancel");
    }

    @Override
    public void focusButton(IButton button) {
        // TODO Auto-generated method stub

    }

    @Override
    public void flashIncorrectAction() {
        // TODO Auto-generated method stub

    }

    @Override
    public void updatePhase() {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateTurn(PlayerView player) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updatePlayerControl() {
        // TODO Auto-generated method stub

    }

    @Override
    public void enableOverlay() {
        // TODO Auto-generated method stub

    }

    @Override
    public void disableOverlay() {
        // TODO Auto-generated method stub

    }

    @Override
    public void finishGame() {
        // TODO Auto-generated method stub

    }

    @Override
    public Object showManaPool(PlayerView player) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void hideManaPool(PlayerView player, Object zoneToRestore) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateStack() {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateZones(List<Pair<PlayerView, ZoneType>> zonesToUpdate) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateSingleCard(CardView card) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateManaPool(Iterable<PlayerView> manaPoolUpdate) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateLives(Iterable<PlayerView> livesUpdate) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setPanelSelection(CardView hostCard) {
        // TODO Auto-generated method stub

    }

    @Override
    public void hear(LobbyPlayer player, String message) {
        // TODO Auto-generated method stub

    }

    @Override
    public SpellAbility getAbilityToPlay(List<SpellAbility> abilities,
            ITriggerEvent triggerEvent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<CardView, Integer> assignDamage(CardView attacker,
            List<CardView> blockers, int damage, GameEntityView defender,
            boolean overrideOrder) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void message(String message, String title) {
        // TODO Auto-generated method stub

    }

    @Override
    public void showErrorDialog(String message, String title) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean showConfirmDialog(String message, String title,
            String yesButtonText, String noButtonText, boolean defaultYes) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int showOptionDialog(String message, String title, FSkinProp icon,
            String[] options, int defaultOption) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int showCardOptionDialog(CardView card, String message,
            String title, FSkinProp icon, String[] options, int defaultOption) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String showInputDialog(String message, String title, FSkinProp icon,
            String initialInput, String[] inputOptions) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean confirm(CardView c, String question, boolean defaultIsYes,
            String[] options) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public <T> List<T> getChoices(String message, int min, int max,
            Collection<T> choices, T selected, Function<T, String> display) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> List<T> order(String title, String top, int remainingObjectsMin,
            int remainingObjectsMax, List<T> sourceChoices,
            List<T> destChoices, CardView referenceCard,
            boolean sideboardingMode) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PaperCard> sideboard(CardPool sideboard, CardPool main) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public GameEntityView chooseSingleEntityForEffect(String title,
            FCollectionView<? extends GameEntity> optionList,
            DelayedReveal delayedReveal, boolean isOptional,
            PlayerControllerHuman controller) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setCard(CardView card) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setPlayerAvatar(LobbyPlayer player, IHasIcon ihi) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean openZones(Collection<ZoneType> zones,
            Map<PlayerView, Object> players) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void restoreOldZones(Map<PlayerView, Object> playersToRestoreZonesFor) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isUiSetToSkipPhase(PlayerView playerTurn, PhaseType phase) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void updateCurrentPlayer(PlayerView player) {
        // TODO Auto-generated method stub

    }

    private final static class NetButton implements IButton {

        private final PlayerView playerView;
        private final String button;
        private NetButton(final PlayerView playerView, final String button) {
            this.playerView = playerView;
            this.button = button;
        }

        @Override
        public boolean isEnabled() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void setEnabled(boolean b0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public boolean isVisible() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void setVisible(boolean b0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public String getText() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setText(String text0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public boolean isSelected() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void setSelected(boolean b0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public boolean requestFocusInWindow() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void setCommand(UiCommand command0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setTextColor(FSkinProp color) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setTextColor(int r, int g, int b) {
            // TODO Auto-generated method stub
            
        }
        
    }
}
