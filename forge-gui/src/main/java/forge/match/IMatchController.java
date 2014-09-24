package forge.match;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import forge.LobbyPlayer;
import forge.game.Match;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.interfaces.IButton;
import forge.util.ITriggerEvent;
import forge.view.CardView;
import forge.view.CombatView;
import forge.view.GameEntityView;
import forge.view.PlayerView;
import forge.view.SpellAbilityView;

public interface IMatchController {
    void startNewMatch(Match match);
    boolean resetForNewGame();
    void openView(List<Player> sortedPlayers, int humanCount);
    void afterGameEnd();
    void showCombat(CombatView combat);
    void showPromptMessage(PlayerView playerView, String message);
    boolean stopAtPhase(PlayerView playerTurn, PhaseType phase);
    IButton getBtnOK(PlayerView playerView);
    IButton getBtnCancel(PlayerView playerView);
    void focusButton(IButton button);
    void flashIncorrectAction();
    void updatePhase();
    void updateTurn(PlayerView player);
    void updatePlayerControl();
    void enableOverlay();
    void disableOverlay();
    void finishGame();
    Object showManaPool(PlayerView player);
    void hideManaPool(PlayerView player, Object zoneToRestore);
    boolean openZones(Collection<ZoneType> zones, Map<PlayerView, Object> players);
    void restoreOldZones(Map<PlayerView, Object> playersToRestoreZonesFor);
    void updateStack();
    void updateZones(List<Pair<PlayerView, ZoneType>> zonesToUpdate);
    void updateSingleCard(CardView card);
    void refreshCardDetails(Iterable<CardView> cards);
    void updateManaPool(List<PlayerView> manaPoolUpdate);
    void updateLives(List<PlayerView> livesUpdate);
    void setPanelSelection(CardView hostCard);
    void hear(LobbyPlayer player, String message);
    int getAbilityToPlay(List<SpellAbilityView> abilities, ITriggerEvent triggerEvent);
    Map<CardView, Integer> assignDamage(CardView attacker, List<CardView> blockers, int damage, GameEntityView defender, boolean overrideOrder);
}
