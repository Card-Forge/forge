package forge;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;

import forge.assets.FSkinProp;
import forge.assets.ISkinImage;
import forge.control.FControl;
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
import forge.gui.BoxedProductCardListViewer;
import forge.gui.CardListViewer;
import forge.gui.FNetOverlay;
import forge.gui.GuiChoose;
import forge.gui.GuiUtils;
import forge.gui.SOverlayUtils;
import forge.gui.framework.FScreen;
import forge.gui.framework.SDisplayUtil;
import forge.gui.framework.SLayoutIO;
import forge.interfaces.IButton;
import forge.interfaces.IGuiBase;
import forge.item.PaperCard;
import forge.match.input.InputQueue;
import forge.model.FModel;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorQuestCardShop;
import forge.screens.match.CMatchUI;
import forge.screens.match.VMatchUI;
import forge.screens.match.ViewWinLose;
import forge.screens.match.controllers.CPrompt;
import forge.screens.match.controllers.CStack;
import forge.screens.match.views.VField;
import forge.screens.match.views.VHand;
import forge.screens.match.views.VPrompt;
import forge.sound.AltSoundSystem;
import forge.sound.AudioClip;
import forge.sound.AudioMusic;
import forge.sound.IAudioClip;
import forge.sound.IAudioMusic;
import forge.toolbox.FButton;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.toolbox.MouseTriggerEvent;
import forge.toolbox.special.PhaseLabel;
import forge.util.BuildInfo;
import forge.util.ITriggerEvent;

public class GuiDesktop implements IGuiBase {
    
    private boolean showOverlay = true;

    @Override
    public boolean isRunningOnDesktop() {
        return true;
    }

    @Override
    public String getCurrentVersion() {
        return BuildInfo.getVersionString();
    }

    @Override
    public void invokeInEdtLater(Runnable proc) {
        SwingUtilities.invokeLater(proc);
    }

    @Override
    public void invokeInEdtAndWait(final Runnable proc) {
        if (SwingUtilities.isEventDispatchThread()) {
            // Just run in the current thread.
            proc.run();
        }
        else {
            try {
                SwingUtilities.invokeAndWait(proc);
            }
            catch (final InterruptedException exn) {
                throw new RuntimeException(exn);
            }
            catch (final InvocationTargetException exn) {
                throw new RuntimeException(exn);
            }
        }
    }

    @Override
    public boolean isGuiThread() {
        return SwingUtilities.isEventDispatchThread();
    }

    @Override
    public String getAssetsDir() {
        return StringUtils.containsIgnoreCase(BuildInfo.getVersionString(), "svn") ?
                "../forge-gui/" : "";
    }

    @Override
    public boolean mayShowCard(Card card) {
        return Singletons.getControl().mayShowCard(card);
    }

    @Override
    public ISkinImage getSkinIcon(FSkinProp skinProp) {
        if (skinProp == null) { return null; }
        return FSkin.getIcon(skinProp);
    }

    @Override
    public ISkinImage getUnskinnedIcon(String path) {
        return new FSkin.UnskinnedIcon(path);
    }

    @Override
    public int showOptionDialog(String message, String title, FSkinProp icon, String[] options, int defaultOption) {
        return FOptionPane.showOptionDialog(message, title, icon == null ? null : FSkin.getImage(icon), options, defaultOption);
    }

    @Override
    public <T> T showInputDialog(String message, String title, FSkinProp icon, T initialInput, T[] inputOptions) {
        return FOptionPane.showInputDialog(message, title, icon == null ? null : FSkin.getImage(icon), initialInput, inputOptions);
    }

    @Override
    public <T> List<T> getChoices(final String message, final int min, final int max, final Collection<T> choices, final T selected, final Function<T, String> display) {
        return GuiChoose.getChoices(message, min, max, choices, selected, display);
    }

    @Override
    public <T> List<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax,
            final List<T> sourceChoices, final List<T> destChoices, final Card referenceCard, final boolean sideboardingMode) {
        return GuiChoose.order(title, top, remainingObjectsMin, remainingObjectsMax, sourceChoices, destChoices, referenceCard, sideboardingMode);
    }

    @Override
    public void showCardList(final String title, final String message, final List<PaperCard> list) {
        final CardListViewer cardView = new CardListViewer(title, message, list);
        cardView.setVisible(true);
        cardView.dispose();
    }

    @Override
    public boolean showBoxedProduct(final String title, final String message, final List<PaperCard> list) {
        final BoxedProductCardListViewer viewer = new BoxedProductCardListViewer(title, message, list);
        viewer.setVisible(true);
        viewer.dispose();
        return viewer.skipTheRest();
    }

    @Override
    public IButton getBtnOK() {
        return VMatchUI.SINGLETON_INSTANCE.getBtnOK();
    }

    @Override
    public IButton getBtnCancel() {
        return VMatchUI.SINGLETON_INSTANCE.getBtnCancel();
    }

    @Override
    public void focusButton(final IButton button) {
        // ensure we don't steal focus from an overlay
        if (!SOverlayUtils.overlayHasFocus()) {
            FThreads.invokeInEdtLater(new Runnable() {
                @Override
                public void run() {
                    ((FButton)button).requestFocusInWindow();
                }
            });
        }
    }

    @Override
    public void flashIncorrectAction() {
        SDisplayUtil.remind(VPrompt.SINGLETON_INSTANCE);
    }

    @Override
    public void updatePhase() {
        PhaseHandler pH = Singletons.getControl().getObservedGame().getPhaseHandler();
        Player p = pH.getPlayerTurn();
        PhaseType ph = pH.getPhase();

        final CMatchUI matchUi = CMatchUI.SINGLETON_INSTANCE;
        PhaseLabel lbl = matchUi.getFieldViewFor(p).getPhaseIndicator().getLabelFor(ph);

        matchUi.resetAllPhaseButtons();
        if (lbl != null) {
            lbl.setActive(true);
        }
    }

    @Override
    public void updateTurn(final GameEventTurnBegan event, final Game game) {
        VField nextField = CMatchUI.SINGLETON_INSTANCE.getFieldViewFor(event.turnOwner);
        SDisplayUtil.showTab(nextField);
        CPrompt.SINGLETON_INSTANCE.updateText(game);
    }

    @Override
    public void updatePlayerControl() {
        CMatchUI.SINGLETON_INSTANCE.initHandViews(getGuiPlayer());
        SLayoutIO.loadLayout(null);
        VMatchUI.SINGLETON_INSTANCE.populate();
        for (VHand h : VMatchUI.SINGLETON_INSTANCE.getHands()) {
            h.getLayoutControl().updateHand();
        }
    }
    
    @Override
    public void disableOverlay() {
        showOverlay = false;
    }
    
    @Override
    public void enableOverlay() {
        showOverlay = true;
    }

    @Override
    public void finishGame() {
        new ViewWinLose(Singletons.getControl().getObservedGame());
        if (showOverlay) {
            SOverlayUtils.showOverlay();
        }
    }

    @Override
    public void updateStack() {
        CStack.SINGLETON_INSTANCE.update();
    }

    @Override
    public void startMatch(GameType gameType, List<RegisteredPlayer> players) {
        FControl.instance.startMatch(gameType, players);
    }

    @Override
    public void setPanelSelection(Card c) {
        GuiUtils.setPanelSelection(c);
    }

    @Override
    public SpellAbility getAbilityToPlay(List<SpellAbility> abilities, ITriggerEvent triggerEvent) {
        if (triggerEvent == null) {
            if (abilities.isEmpty()) {
                return null;
            }
            if (abilities.size() == 1) {
                return abilities.get(0);
            }
            return GuiChoose.oneOrNone("Choose ability to play", abilities);
        }

        if (abilities.isEmpty()) {
            return null;
        }
        if (abilities.size() == 1 && !abilities.get(0).promptIfOnlyPossibleAbility()) {
            if (abilities.get(0).canPlay()) {
                return abilities.get(0); //only return ability if it's playable, otherwise return null
            }
            return null;
        }

        //show menu if mouse was trigger for ability
        final JPopupMenu menu = new JPopupMenu("Abilities");

        boolean enabled;
        boolean hasEnabled = false;
        int shortcut = KeyEvent.VK_1; //use number keys as shortcuts for abilities 1-9
        for (final SpellAbility ab : abilities) {
            enabled = ab.canPlay();
            if (enabled) {
                hasEnabled = true;
            }
            GuiUtils.addMenuItem(menu, FSkin.encodeSymbols(ab.toString(), true),
                    shortcut > 0 ? KeyStroke.getKeyStroke(shortcut, 0) : null,
                    new Runnable() {
                        @Override
                        public void run() {
                            CPrompt.SINGLETON_INSTANCE.getInputControl().selectAbility(ab);
                        }
                    }, enabled);
            if (shortcut > 0) {
                shortcut++;
                if (shortcut > KeyEvent.VK_9) {
                    shortcut = 0; //stop adding shortcuts after 9
                }
            }
        }
        if (hasEnabled) { //only show menu if at least one ability can be played
            SwingUtilities.invokeLater(new Runnable() { //use invoke later to ensure first ability selected by default
                public void run() {
                    MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[]{menu, menu.getSubElements()[0]});
                }
            });
            MouseEvent mouseEvent = ((MouseTriggerEvent)triggerEvent).getMouseEvent();
            menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
        }

        return null; //delay ability until choice made
    }

    @Override
    public void hear(LobbyPlayer player, String message) {
        FNetOverlay.SINGLETON_INSTANCE.addMessage(player.getName(), message);
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
        CMatchUI.SINGLETON_INSTANCE.fireEvent(e);
    }

    @Override
    public void setCard(Card card) {
        CMatchUI.SINGLETON_INSTANCE.setCard(card);
    }

    @Override
    public void showCombat(Combat combat) {
        CMatchUI.SINGLETON_INSTANCE.showCombat(combat);
    }

    @Override
    public void setUsedToPay(Card card, boolean b) {
        CMatchUI.SINGLETON_INSTANCE.setUsedToPay(card, b);
    }

    @Override
    public void setHighlighted(Player player, boolean b) {
        CMatchUI.SINGLETON_INSTANCE.setHighlighted(player, b);
    }

    @Override
    public void showPromptMessage(String message) {
        CMatchUI.SINGLETON_INSTANCE.showMessage(message);
    }

    @Override
    public boolean stopAtPhase(Player playerTurn, PhaseType phase) {
        return CMatchUI.SINGLETON_INSTANCE.stopAtPhase(playerTurn, phase);
    }

    @Override
    public InputQueue getInputQueue() {
        return FControl.instance.getInputQueue();
    }

    @Override
    public Game getGame() {
        return FControl.instance.getObservedGame();
    }

    @Override
    public void updateZones(List<Pair<Player, ZoneType>> zonesToUpdate) {
        CMatchUI.SINGLETON_INSTANCE.updateZones(zonesToUpdate);
    }

    @Override
    public void updateCards(Set<Card> cardsToUpdate) {
        CMatchUI.SINGLETON_INSTANCE.updateCards(cardsToUpdate);
    }

    @Override
    public void refreshCardDetails(Collection<Card> cards) {
        CMatchUI.SINGLETON_INSTANCE.refreshCardDetails(cards);
    }

    @Override
    public void updateManaPool(List<Player> manaPoolUpdate) {
        CMatchUI.SINGLETON_INSTANCE.updateManaPool(manaPoolUpdate);
    }

    @Override
    public void updateLives(List<Player> livesUpdate) {
        CMatchUI.SINGLETON_INSTANCE.updateLives(livesUpdate);
    }

    @Override
    public void endCurrentGame() {
        FControl.instance.endCurrentGame();
    }

    @Override
    public Map<Card, Integer> getDamageToAssign(Card attacker, List<Card> blockers,
            int damageDealt, GameEntity defender, boolean overrideOrder) {
        return CMatchUI.SINGLETON_INSTANCE.getDamageToAssign(attacker, blockers,
                damageDealt, defender, overrideOrder);
    }

    @Override
    public String showFileDialog(String title, String defaultDir) {
        final JFileChooser fc = new JFileChooser(defaultDir);
        final int rc = fc.showDialog(null, title);
        if (rc != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return fc.getSelectedFile().getAbsolutePath();
    }

    @Override
    public void showBugReportDialog(String title, String text, boolean showExitAppBtn) {
        BugReportDialog.show(title, text, showExitAppBtn);
    }

    @Override
    public File getSaveFile(File defaultFile) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(defaultFile);
        fc.showSaveDialog(null);
        return fc.getSelectedFile();
    }

    @Override
    public void copyToClipboard(String text) {
        StringSelection ss = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
    }

    @Override
    public void browseToUrl(String url) throws Exception {
        Desktop.getDesktop().browse(new URI(url));
    }

	@Override
	public LobbyPlayer getGuiPlayer() {
		return FControl.instance.getGuiPlayer();
	}

    @Override
    public LobbyPlayer getAiPlayer(String name) {
        return FControl.instance.getAiPlayer(name);
    }

	@Override
	public LobbyPlayer createAiPlayer() {
		return FControl.instance.getAiPlayer();
	}

	@Override
	public LobbyPlayer createAiPlayer(String name, int avatarIndex) {
		return FControl.instance.getAiPlayer(name, avatarIndex);
	}

	@Override
	public LobbyPlayer getQuestPlayer() {
		return getGuiPlayer();
	}

    @Override
    public IAudioClip createAudioClip(String filename) {
        return AudioClip.fileExists(filename) ? new AudioClip(filename) : null;
    }

    @Override
    public IAudioMusic createAudioMusic(String filename) {
        return new AudioMusic(filename);
    }

    @Override
    public void startAltSoundSystem(String filename, boolean isSynchronized) {
        new AltSoundSystem(filename, isSynchronized).start();
    }

    @Override
    public void clearImageCache() {
        ImageCache.clear();
    }

    @Override
    public void startGame(Match match) {
        SOverlayUtils.startGameOverlay();
        SOverlayUtils.showOverlay();
        Singletons.getControl().startGameWithUi(match);
    }

    @Override
    public void continueMatch(Match match) {
        Singletons.getControl().endCurrentGame();
        if (match == null) {
            Singletons.getControl().setCurrentScreen(FScreen.HOME_SCREEN);
        }
        else {
            Singletons.getControl().startGameWithUi(match);
        }
    }

    @Override
    public void showSpellShop() {
        Singletons.getControl().setCurrentScreen(FScreen.QUEST_CARD_SHOP);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(
                new CEditorQuestCardShop(FModel.getQuest()));
    }

    @Override
    public void showBazaar() {
        Singletons.getControl().setCurrentScreen(FScreen.QUEST_BAZAAR);
        Singletons.getView().getFrame().validate();
    }

    @Override
    public void setPlayerAvatar(LobbyPlayer player, IHasIcon ihi) {
        CMatchUI.SINGLETON_INSTANCE.avatarImages.put(player, ihi.getIconImageKey());
    }
}
