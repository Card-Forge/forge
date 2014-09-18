package forge;

import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;

import forge.assets.FSkinProp;
import forge.assets.ISkinImage;
import forge.control.FControl;
import forge.deck.CardPool;
import forge.error.BugReportDialog;
import forge.events.UiEvent;
import forge.game.GameObject;
import forge.game.GameType;
import forge.game.Match;
import forge.game.phase.PhaseType;
import forge.game.player.IHasIcon;
import forge.game.player.RegisteredPlayer;
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
import forge.player.GamePlayerUtil;
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
import forge.view.CardView;
import forge.view.CombatView;
import forge.view.GameEntityView;
import forge.view.PlayerView;
import forge.view.SpellAbilityView;

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
    public ISkinImage getSkinIcon(FSkinProp skinProp) {
        if (skinProp == null) { return null; }
        return FSkin.getIcon(skinProp);
    }

    @Override
    public ISkinImage getUnskinnedIcon(String path) {
        return new FSkin.UnskinnedIcon(path);
    }

    @Override
    public ISkinImage createLayeredImage(FSkinProp background, FSkinProp overlay, float opacity) {
        BufferedImage image = new BufferedImage(background.getWidth(), background.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        FSkin.SkinImage backgroundImage = FSkin.getImage(background);
        FSkin.SkinImage overlayImage = FSkin.getImage(overlay);
        FSkin.drawImage(g, backgroundImage, 0, 0, background.getWidth(), background.getHeight());
        FSkin.drawImage(g, overlayImage, (background.getWidth() - overlay.getWidth()) / 2, (background.getHeight() - overlay.getHeight()) / 2, overlay.getWidth(), overlay.getHeight());
        return new FSkin.UnskinnedIcon(image, opacity);
    }

    @Override
    public int showOptionDialog(String message, String title, FSkinProp icon, String[] options, int defaultOption) {
        return FOptionPane.showOptionDialog(message, title, icon == null ? null : FSkin.getImage(icon), options, defaultOption);
    }

    @Override
    public int showCardOptionDialog(final CardView card, String message, String title, FSkinProp skinIcon, String[] options, int defaultOption) {
        if (card != null) {
            FThreads.invokeInEdtAndWait(GuiBase.getInterface(), new Runnable() {
                @Override
                public void run() {
                    GuiBase.getInterface().setCard(card);
                }
            });
        }
        return showOptionDialog(message, title, skinIcon, options, defaultOption);
    }

    @Override
    public <T> T showInputDialog(String message, String title, FSkinProp icon, T initialInput, T[] inputOptions) {
        if (initialInput instanceof GameObject || (inputOptions != null && inputOptions.length > 0 && inputOptions[0] instanceof GameObject)) {
            System.err.println("Warning: GameObject passed to GUI! Printing stack trace.");
            Thread.dumpStack();
        }
        return FOptionPane.showInputDialog(message, title, icon == null ? null : FSkin.getImage(icon), initialInput, inputOptions);
    }

    @Override
    public <T> List<T> getChoices(final String message, final int min, final int max, final Collection<T> choices, final T selected, final Function<T, String> display) {
        if ((choices != null && !choices.isEmpty() && choices.iterator().next() instanceof GameObject) || selected instanceof GameObject) {
            System.err.println("Warning: GameObject passed to GUI! Printing stack trace.");
            Thread.dumpStack();
        }
        return GuiChoose.getChoices(message, min, max, choices, selected, display);
    }

    @Override
    public <T> List<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax,
            final List<T> sourceChoices, final List<T> destChoices, final CardView referenceCard, final boolean sideboardingMode) {
        if ((sourceChoices != null && !sourceChoices.isEmpty() && sourceChoices.iterator().next() instanceof GameObject)
                || (destChoices != null && !destChoices.isEmpty() && destChoices.iterator().next() instanceof GameObject)) {
            System.err.println("Warning: GameObject passed to GUI! Printing stack trace.");
            Thread.dumpStack();
        }
        return GuiChoose.order(title, top, remainingObjectsMin, remainingObjectsMax, sourceChoices, destChoices, referenceCard, sideboardingMode);
    }

    @Override
    public List<PaperCard> sideboard(CardPool sideboard, CardPool main) {
        return GuiChoose.sideboard(sideboard.toFlatList(), main.toFlatList());
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
            FThreads.invokeInEdtLater(GuiBase.getInterface(), new Runnable() {
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
        final PlayerView p = Singletons.getControl().getGameView().getPlayerTurn();
        final PhaseType ph = Singletons.getControl().getGameView().getPhase();
        final CMatchUI matchUi = CMatchUI.SINGLETON_INSTANCE;
        PhaseLabel lbl = matchUi.getFieldViewFor(p).getPhaseIndicator().getLabelFor(ph);

        matchUi.resetAllPhaseButtons();
        if (lbl != null) {
            lbl.setActive(true);
        }
    }

    @Override
    public void updateTurn(final PlayerView player) {
        VField nextField = CMatchUI.SINGLETON_INSTANCE.getFieldViewFor(player);
        SDisplayUtil.showTab(nextField);
        CPrompt.SINGLETON_INSTANCE.updateText();
        CMatchUI.SINGLETON_INSTANCE.repaintCardOverlays();
    }

    @Override
    public void updatePlayerControl() {
        CMatchUI.SINGLETON_INSTANCE.initHandViews(GamePlayerUtil.getGuiPlayer());
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
        new ViewWinLose(Singletons.getControl().getGameView());
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
    public void setPanelSelection(final CardView c) {
        GuiUtils.setPanelSelection(c);
    }

    @Override
    public SpellAbilityView getAbilityToPlay(List<SpellAbilityView> abilities, ITriggerEvent triggerEvent) {
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
        if (abilities.size() == 1 && !abilities.get(0).isPromptIfOnlyPossibleAbility()) {
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
        for (final SpellAbilityView ab : abilities) {
            enabled = ab.canPlay();
            if (enabled) {
                hasEnabled = true;
            }
            GuiUtils.addMenuItem(menu, FSkin.encodeSymbols(ab.toString(), true),
                    shortcut > 0 ? KeyStroke.getKeyStroke(shortcut, 0) : null,
                    new Runnable() {
                        @Override
                        public void run() {
                            CPrompt.SINGLETON_INSTANCE.selectAbility(ab);
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
    public void setCard(final CardView card) {
        CMatchUI.SINGLETON_INSTANCE.setCard(card);
    }

    @Override
    public void showCombat(final CombatView combat) {
        CMatchUI.SINGLETON_INSTANCE.showCombat(combat);
    }

    @Override
    public void setUsedToPay(final CardView card, final boolean b) {
        CMatchUI.SINGLETON_INSTANCE.setUsedToPay(card, b);
    }

    @Override
    public void setHighlighted(final PlayerView player, final boolean b) {
        CMatchUI.SINGLETON_INSTANCE.setHighlighted(player, b);
    }

    @Override
    public void showPromptMessage(final String message) {
        CMatchUI.SINGLETON_INSTANCE.showMessage(message);
    }

    @Override
    public boolean stopAtPhase(final PlayerView playerTurn, PhaseType phase) {
        return CMatchUI.SINGLETON_INSTANCE.stopAtPhase(playerTurn, phase);
    }

    @Override
    public InputQueue getInputQueue() {
        return FControl.instance.getInputQueue();
    }

    public Object showManaPool(final PlayerView player) {
        return null; //not needed since mana pool icons are always visible
    }

    @Override
    public void hideManaPool(final PlayerView player, final Object zoneToRestore) {
        //not needed since mana pool icons are always visible
    }

    @Override
    public boolean openZones(final Collection<ZoneType> zones, final Map<PlayerView, Object> players) {
        if (zones.size() == 1) {
            switch (zones.iterator().next()) {
            case Battlefield:
            case Hand:
                return true; //don't actually need to open anything, but indicate that zone can be opened
            default:
                return false;
            }
        }
        return false;
    }

    @Override
    public void restoreOldZones(final Map<PlayerView, Object> playersToRestoreZonesFor) {
    }

    @Override
    public void updateZones(final List<Pair<PlayerView, ZoneType>> zonesToUpdate) {
        CMatchUI.SINGLETON_INSTANCE.updateZones(zonesToUpdate);
    }

    @Override
    public void updateCards(final Iterable<CardView> cardsToUpdate) {
        CMatchUI.SINGLETON_INSTANCE.updateCards(cardsToUpdate);
    }

    @Override
    public void refreshCardDetails(final Iterable<CardView> cards) {
        CMatchUI.SINGLETON_INSTANCE.refreshCardDetails(cards);
    }

    @Override
    public void updateManaPool(final List<PlayerView> manaPoolUpdate) {
        CMatchUI.SINGLETON_INSTANCE.updateManaPool(manaPoolUpdate);
    }

    @Override
    public void updateLives(final List<PlayerView> livesUpdate) {
        CMatchUI.SINGLETON_INSTANCE.updateLives(livesUpdate);
    }

    @Override
    public void endCurrentGame() {
        FControl.instance.endCurrentGame();
    }

    @Override
    public Map<CardView, Integer> getDamageToAssign(final CardView attacker,
            final List<CardView> blockers, final int damageDealt,
            final GameEntityView defender, final boolean overrideOrder) {
        return CMatchUI.SINGLETON_INSTANCE.getDamageToAssign(attacker,
                blockers, damageDealt, defender, overrideOrder);
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
