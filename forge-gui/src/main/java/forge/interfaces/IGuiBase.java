package forge.interfaces;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;

import forge.LobbyPlayer;
import forge.assets.FSkinProp;
import forge.assets.ISkinImage;
import forge.events.UiEvent;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.event.GameEventTurnBegan;
import forge.game.phase.PhaseType;
import forge.game.player.IHasIcon;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.match.input.InputQueue;
import forge.sound.IAudioClip;
import forge.sound.IAudioMusic;
import forge.util.ITriggerEvent;


public interface IGuiBase {
    boolean isRunningOnDesktop();
    String getCurrentVersion();
    void invokeInEdtLater(Runnable runnable);
    void invokeInEdtAndWait(final Runnable proc);
    boolean isGuiThread();
    String getAssetsDir();
    boolean mayShowCard(Card card);
    ISkinImage getSkinIcon(FSkinProp skinProp);
    ISkinImage getUnskinnedIcon(String path);
    void showBugReportDialog(String title, String text, boolean showExitAppBtn);
    int showOptionDialog(String message, String title, FSkinProp icon, String[] options, int defaultOption);
    int showCardOptionDialog(Card card, String message, String title, FSkinProp icon, String[] options, int defaultOption);
    <T> T showInputDialog(String message, String title, FSkinProp icon, T initialInput, T[] inputOptions);
    <T> List<T> getChoices(final String message, final int min, final int max, final Collection<T> choices, final T selected, final Function<T, String> display);
    <T> List<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax,
            final List<T> sourceChoices, final List<T> destChoices, final Card referenceCard, final boolean sideboardingMode);
    String showFileDialog(String title, String defaultDir);
    File getSaveFile(File defaultFile);
    void showCardList(final String title, final String message, final List<PaperCard> list);
    boolean showBoxedProduct(final String title, final String message, final List<PaperCard> list);
    void fireEvent(UiEvent e);
    void setCard(Card card);
    void showCombat(Combat combat);
    void setUsedToPay(Card card, boolean b);
    void setHighlighted(Player player, boolean b);
    void showPromptMessage(String message);
    boolean stopAtPhase(Player playerTurn, PhaseType phase);
    InputQueue getInputQueue();
    Game getGame();
    IButton getBtnOK();
    IButton getBtnCancel();
    void focusButton(IButton button);
    void flashIncorrectAction();
    void updatePhase();
    void updateTurn(GameEventTurnBegan event, Game game);
    void updatePlayerControl();
    void enableOverlay();
    void disableOverlay();
    void finishGame();
    boolean openZones(List<ZoneType> zones, Map<Player, Object> players);
    void restoreOldZones(Map<Player, Object> playersToRestoreZonesFor);
    void updateStack();
    void updateZones(List<Pair<Player, ZoneType>> zonesToUpdate);
    void updateCards(Set<Card> cardsToUpdate);
    void refreshCardDetails(Collection<Card> cards);
    void updateManaPool(List<Player> manaPoolUpdate);
    void updateLives(List<Player> livesUpdate);
    void endCurrentGame();
    void startMatch(GameType gauntletType, List<RegisteredPlayer> starter);
    void setPanelSelection(Card hostCard);
    Map<Card, Integer> getDamageToAssign(Card attacker, List<Card> blockers,
            int damageDealt, GameEntity defender, boolean overrideOrder);
    SpellAbility getAbilityToPlay(List<SpellAbility> abilities, ITriggerEvent triggerEvent);
    void hear(LobbyPlayer player, String message);
    int getAvatarCount();
    void copyToClipboard(String text);
    void browseToUrl(String url) throws Exception;
	LobbyPlayer getGuiPlayer();
    LobbyPlayer getAiPlayer(String name);
	LobbyPlayer createAiPlayer();
	LobbyPlayer createAiPlayer(String name, int avatarIndex);
	LobbyPlayer getQuestPlayer();
    IAudioClip createAudioClip(String filename);
    IAudioMusic createAudioMusic(String filename);
    void startAltSoundSystem(String filename, boolean isSynchronized);
    void clearImageCache();
    void startGame(Match match);
    void continueMatch(Match match);
    void showSpellShop();
    void showBazaar();
    void setPlayerAvatar(LobbyPlayer player, IHasIcon ihi);
}