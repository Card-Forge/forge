package forge;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.graphics.Texture;
import com.google.common.base.Function;

import forge.assets.FSkin;
import forge.assets.FSkinProp;
import forge.assets.FTextureImage;
import forge.assets.ISkinImage;
import forge.assets.ImageCache;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.FDeckViewer;
import forge.deck.FSideboardDialog;
import forge.error.BugReportDialog;
import forge.events.UiEvent;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.event.GameEventTurnBegan;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.IHasIcon;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.interfaces.IButton;
import forge.interfaces.IGuiBase;
import forge.item.PaperCard;
import forge.match.input.InputQueue;
import forge.properties.ForgeConstants;
import forge.screens.match.FControl;
import forge.screens.match.views.VPlayerPanel;
import forge.screens.match.views.VPhaseIndicator.PhaseLabel;
import forge.screens.match.views.VPlayerPanel.InfoTab;
import forge.screens.match.winlose.ViewWinLose;
import forge.screens.quest.QuestMenu;
import forge.sound.AudioClip;
import forge.sound.AudioMusic;
import forge.sound.IAudioClip;
import forge.sound.IAudioMusic;
import forge.toolbox.FOptionPane;
import forge.toolbox.GuiChoose;
import forge.util.ITriggerEvent;
import forge.util.ThreadUtil;
import forge.util.WaitCallback;
import forge.util.WaitRunnable;
import forge.util.gui.SGuiChoose;

public class GuiMobile implements IGuiBase {

    private final String assetsDir;
    
    public GuiMobile(String assetsDir0) {
        assetsDir = assetsDir0;
    }

    @Override
    public boolean isRunningOnDesktop() {
        return Gdx.app.getType() == ApplicationType.Desktop;
    }

    @Override
    public String getCurrentVersion() {
        return Forge.CURRENT_VERSION;
    }

    @Override
    public void invokeInEdtLater(Runnable proc) {
        Gdx.app.postRunnable(proc);
    }

    @Override
    public void invokeInEdtAndWait(final Runnable proc) {
        if (isGuiThread()) {
            proc.run();
        }
        else {
            new WaitRunnable() {
                @Override
                public void run() {
                    proc.run();
                }
            }.invokeAndWait();
        }
    }

    @Override
    public boolean isGuiThread() {
        return !ThreadUtil.isGameThread();
    }

    @Override
    public String getAssetsDir() {
        return assetsDir;
    }

    @Override
    public boolean mayShowCard(Card card) {
        return FControl.mayShowCard(card);
    }

    @Override
    public ISkinImage getSkinIcon(FSkinProp skinProp) {
        if (skinProp == null) { return null; }
        return FSkin.getImages().get(skinProp);
    }

    @Override
    public ISkinImage getUnskinnedIcon(String path) {
        return new FTextureImage(new Texture(Gdx.files.absolute(path)));
    }

    @Override
    public int showOptionDialog(final String message, final String title, final FSkinProp icon, final String[] options, final int defaultOption) {
        return new WaitCallback<Integer>() {
            @Override
            public void run() {
                FOptionPane.showOptionDialog(message, title, icon == null ? null : FSkin.getImages().get(icon), options, defaultOption, this);
            }
        }.invokeAndWait();
    }

    @Override
    public int showCardOptionDialog(final Card card, final String message, final String title, final FSkinProp icon, final String[] options, final int defaultOption) {
        return new WaitCallback<Integer>() {
            @Override
            public void run() {
                FOptionPane.showCardOptionDialog(card, message, title, icon == null ? null : FSkin.getImages().get(icon), options, defaultOption, this);
            }
        }.invokeAndWait();
    }

    @Override
    public <T> T showInputDialog(final String message, final String title, final FSkinProp icon, final T initialInput, final T[] inputOptions) {
        return new WaitCallback<T>() {
            @Override
            public void run() {
                FOptionPane.showInputDialog(message, title, icon == null ? null : FSkin.getImages().get(icon), initialInput, inputOptions, this);
            }
        }.invokeAndWait();
    }

    @Override
    public <T> List<T> getChoices(final String message, final int min, final int max, final Collection<T> choices, final T selected, final Function<T, String> display) {
        return new WaitCallback<List<T>>() {
            @Override
            public void run() {
                GuiChoose.getChoices(message, min, max, choices, selected, display, this);
            }
        }.invokeAndWait();
    }

    @Override
    public <T> List<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax,
            final List<T> sourceChoices, final List<T> destChoices, final Card referenceCard, final boolean sideboardingMode) {
        return new WaitCallback<List<T>>() {
            @Override
            public void run() {
                GuiChoose.order(title, top, remainingObjectsMin, remainingObjectsMax, sourceChoices, destChoices, referenceCard, this);
            }
        }.invokeAndWait();
    }

    @Override
    public List<PaperCard> sideboard(final CardPool sideboard, final CardPool main) {
        return new WaitCallback<List<PaperCard>>() {
            @Override
            public void run() {
                FSideboardDialog sideboardDialog = new FSideboardDialog(sideboard, main, this);
                sideboardDialog.show();
            }
        }.invokeAndWait();
    }

    @Override
    public void showBugReportDialog(String title, String text, boolean showExitAppBtn) {
        BugReportDialog.show(title, text, showExitAppBtn);
    }

    @Override
    public void showCardList(final String title, final String message, final List<PaperCard> list) {
        Deck deck = new Deck(title + " - " + message);
        deck.getMain().addAllFlat(list);
        FDeckViewer.show(deck);
    }

    @Override
    public boolean showBoxedProduct(final String title, final String message, final List<PaperCard> list) {
        Deck deck = new Deck(title + " - " + message); //TODO: Make this nicer
        deck.getMain().addAllFlat(list);
        FDeckViewer.show(deck);
        return false;
    }

    @Override
    public IButton getBtnOK() {
        return FControl.getView().getPrompt().getBtnOk();
    }

    @Override
    public IButton getBtnCancel() {
        return FControl.getView().getPrompt().getBtnCancel();
    }

    @Override
    public void focusButton(final IButton button) {
        //not needed for mobile game
    }

    @Override
    public void flashIncorrectAction() {
        //SDisplayUtil.remind(VPrompt.SINGLETON_INSTANCE); //TODO
    }

    @Override
    public void updatePhase() {
        PhaseHandler pH = FControl.getGame().getPhaseHandler();
        Player p = pH.getPlayerTurn();
        PhaseType ph = pH.getPhase();

        PhaseLabel lbl = FControl.getPlayerPanel(p).getPhaseIndicator().getLabel(ph);

        FControl.resetAllPhaseButtons();
        if (lbl != null) {
            lbl.setActive(true);
        }
    }

    @Override
    public void updateTurn(final GameEventTurnBegan event, final Game game) {
        //VField nextField = FControl.getFieldViewFor(event.turnOwner);
        //SDisplayUtil.showTab(nextField);
    }

    @Override
    public void updatePlayerControl() {
        //TODO
    }
    
    @Override
    public void disableOverlay() {
    }
    
    @Override
    public void enableOverlay() {
    }

    @Override
    public void finishGame() {
        new ViewWinLose(FControl.getGame()).setVisible(true);
    }

    @Override
    public void updateStack() {
        FControl.getView().getStack().update();
    }

    @Override
    public void startMatch(GameType gameType, List<RegisteredPlayer> players) {
        FControl.startMatch(gameType, players);
    }

    @Override
    public void setPanelSelection(Card c) {
        //GuiUtils.setPanelSelection(c); //TODO
    }

    @Override
    public SpellAbility getAbilityToPlay(List<SpellAbility> abilities, ITriggerEvent triggerEvent) {
        if (abilities.isEmpty()) {
            return null;
        }
        if (abilities.size() == 1) {
            return abilities.get(0);
        }
        return SGuiChoose.oneOrNone("Choose ability to play", abilities);
    }

    @Override
    public void hear(LobbyPlayer player, String message) {
        //FNetOverlay.SINGLETON_INSTANCE.addMessage(player.getName(), message); //TODO
    }

    @Override
    public int getAvatarCount() {
        if (FSkin.isLoaded()) {
            return FSkin.getAvatars().size();
        }
        return 0;
    }

    @Override
    public void fireEvent(UiEvent e) {
        FControl.fireEvent(e);
    }

    @Override
    public void setCard(Card card) {
        //doesn't need to do anything
    }

    @Override
    public void showCombat(Combat combat) {
        FControl.showCombat(combat);
    }

    @Override
    public void setUsedToPay(Card card, boolean b) {
        FControl.setUsedToPay(card, b);
    }

    @Override
    public void setHighlighted(Player player, boolean b) {
        FControl.setHighlighted(player, b);
    }

    @Override
    public void showPromptMessage(String message) {
        FControl.showMessage(message);
    }

    @Override
    public boolean stopAtPhase(Player playerTurn, PhaseType phase) {
        return FControl.stopAtPhase(playerTurn, phase);
    }

    @Override
    public InputQueue getInputQueue() {
        return FControl.getInputQueue();
    }

    @Override
    public Game getGame() {
        return FControl.getGame();
    }

    @Override
    public Object showManaPool(Player player) {
        VPlayerPanel playerPanel = FControl.getPlayerPanel(player);
        InfoTab oldSelectedTab = playerPanel.getSelectedTab();
        playerPanel.setSelectedTab(playerPanel.getManaPoolTab());
        return oldSelectedTab;
    }

    @Override
    public void hideManaPool(Player player, Object zoneToRestore) {
        VPlayerPanel playerPanel = FControl.getPlayerPanel(player);
        if (zoneToRestore == playerPanel.getManaPoolTab()) {
            return; //if mana pool was selected previously, we don't need to switch back to anything
        }
        if (playerPanel.getSelectedTab() != playerPanel.getManaPoolTab()) {
            return; //if player switch away from mana pool already, don't change anything
        }
        playerPanel.setSelectedTab((InfoTab)zoneToRestore);
    }

    @Override
    public boolean openZones(List<ZoneType> zones, Map<Player, Object> players) {
        if (zones.size() == 1) {
            ZoneType zoneType = zones.get(0);
            switch (zoneType) {
            case Battlefield:
            case Command:
                players.clear(); //clear since no zones need to be restored
                return true; //Battlefield is always open
            default:
                //open zone tab for given zone if needed
                boolean result = true;
                for (Player player : players.keySet()) {
                    VPlayerPanel playerPanel = FControl.getPlayerPanel(player);
                    players.put(player, playerPanel.getSelectedTab()); //backup selected tab before changing it
                    InfoTab zoneTab = playerPanel.getZoneTab(zoneType);
                    if (zoneTab == null) {
                        result = false;
                    }
                    else {
                        playerPanel.setSelectedTab(zoneTab);
                    }
                }
                return result;
            }
        }
        return false;
    }

    @Override
    public void restoreOldZones(Map<Player, Object> playersToRestoreZonesFor) {
        for (Entry<Player, Object> player : playersToRestoreZonesFor.entrySet()) {
            VPlayerPanel playerPanel = FControl.getPlayerPanel(player.getKey());
            playerPanel.setSelectedTab((InfoTab)player.getValue());
        }
    }

    @Override
    public void updateZones(List<Pair<Player, ZoneType>> zonesToUpdate) {
        FControl.updateZones(zonesToUpdate);
    }

    @Override
    public void updateCards(Set<Card> cardsToUpdate) {
        FControl.updateCards(cardsToUpdate);
    }

    @Override
    public void refreshCardDetails(Collection<Card> cards) {
        FControl.refreshCardDetails(cards);
    }

    @Override
    public void updateManaPool(List<Player> manaPoolUpdate) {
        FControl.updateManaPool(manaPoolUpdate);
    }

    @Override
    public void updateLives(List<Player> livesUpdate) {
        FControl.updateLives(livesUpdate);
    }

    @Override
    public void endCurrentGame() {
        FControl.endCurrentGame();
    }

    @Override
    public Map<Card, Integer> getDamageToAssign(Card attacker, List<Card> blockers,
            int damageDealt, GameEntity defender, boolean overrideOrder) {
        return FControl.getDamageToAssign(attacker, blockers,
                damageDealt, defender, overrideOrder);
    }

    @Override
    public String showFileDialog(String title, String defaultDir) {
        return ForgeConstants.USER_GAMES_DIR + "Test.fgs"; //TODO: Show dialog
    }

    @Override
    public File getSaveFile(File defaultFile) {
        return defaultFile; //TODO: Show dialog
    }

    @Override
    public void copyToClipboard(String text) {
        Forge.getClipboard().setContents(text);
    }

    @Override
    public void browseToUrl(String url) throws Exception {
        Gdx.net.openURI(url);
    }

    @Override
    public LobbyPlayer getGuiPlayer() {
        return FControl.getGuiPlayer();
    }

    @Override
    public LobbyPlayer getAiPlayer(String name) {
        return FControl.getAiPlayer(name);
    }

    @Override
    public LobbyPlayer createAiPlayer() {
        return FControl.getAiPlayer();
    }

    @Override
    public LobbyPlayer createAiPlayer(String name, int avatarIndex) {
        return FControl.getAiPlayer(name, avatarIndex);
    }

    @Override
    public LobbyPlayer getQuestPlayer() {
        return getGuiPlayer();
    }

    @Override
    public IAudioClip createAudioClip(String filename) {
        return AudioClip.createClip(ForgeConstants.SOUND_DIR + filename);
    }

    @Override
    public IAudioMusic createAudioMusic(String filename) {
        return new AudioMusic(filename);
    }

    @Override
    public void startAltSoundSystem(String filename, boolean isSynchronized) {
        //TODO: Support alt sound system
    }

    @Override
    public void clearImageCache() {
        ImageCache.clear();
    }

    @Override
    public void startGame(Match match) {
        FControl.startGame(match);
    }

    @Override
    public void continueMatch(Match match) {
        FControl.endCurrentGame();
        if (match == null) {
            Forge.back();
        }
        else {
            FControl.startGame(match);
        }
    }

    @Override
    public void showSpellShop() {
        QuestMenu.showSpellShop();
    }

    @Override
    public void showBazaar() {
        QuestMenu.showBazaar();
    }

    @Override
    public void setPlayerAvatar(LobbyPlayer player, IHasIcon ihi) {
        FControl.setPlayerAvatar(player, ImageCache.getIcon(ihi));
    }
}
