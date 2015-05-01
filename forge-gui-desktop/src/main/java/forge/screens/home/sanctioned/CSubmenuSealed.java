package forge.screens.home.sanctioned;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import forge.Singletons;
import forge.UiCommand;
import forge.deck.DeckBase;
import forge.deck.DeckGroup;
import forge.deck.DeckProxy;
import forge.game.GameType;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.item.InventoryItem;
import forge.itemmanager.ItemManagerConfig;
import forge.limited.SealedCardPoolGenerator;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.ACEditorBase;
import forge.screens.deckeditor.controllers.CEditorLimited;
import forge.toolbox.FOptionPane;

/**
 * Controls the sealed submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
@SuppressWarnings("serial")
public enum CSubmenuSealed implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private final UiCommand cmdDeckSelect = new UiCommand() {
        @Override
        public void run() {
            VSubmenuSealed.SINGLETON_INSTANCE.getBtnStart().setEnabled(true);
        }
    };

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        final VSubmenuSealed view = VSubmenuSealed.SINGLETON_INSTANCE;

        view.getLstDecks().setSelectCommand(cmdDeckSelect);

        view.getBtnBuildDeck().setCommand(new UiCommand() {
            @Override
            public void run() {
                setupSealed();
            }
        });

        view.getBtnStart().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                startGame(GameType.Sealed);
            }
        });

        view.getBtnDirections().setCommand(new UiCommand() {
            @Override
            public void run() {
                view.showDirections();
            }
        });
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        final VSubmenuSealed view = VSubmenuSealed.SINGLETON_INSTANCE;
        view.getLstDecks().setPool(DeckProxy.getAllSealedDecks());
        view.getLstDecks().setup(ItemManagerConfig.SEALED_DECKS);

        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                final JButton btnStart = view.getBtnStart();
                if (btnStart.isEnabled()) {
                    view.getBtnStart().requestFocusInWindow();
                } else {
                    view.getBtnBuildDeck().requestFocusInWindow();
                }
            }
        });
    }

    private void startGame(final GameType gameType) {
        final DeckProxy human = VSubmenuSealed.SINGLETON_INSTANCE.getLstDecks().getSelectedItem();

        if (human == null) {
            FOptionPane.showErrorDialog("Please build and/or select a deck for yourself.", "No Deck");
            return;
        }

        if (FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            final String errorMessage = gameType.getDeckFormat().getDeckConformanceProblem(human.getDeck());
            if (null != errorMessage) {
                FOptionPane.showErrorDialog("Your deck " + errorMessage + " Please edit or choose a different deck.", "Invalid Deck");
                return;
            }
        }

        final int matches = FModel.getDecks().getSealed().get(human.getName()).getAiDecks().size();
        FModel.getGauntletMini().launch(matches, human.getDeck(), gameType);
    }

    @SuppressWarnings("unchecked")
    private <T extends DeckBase> void setupSealed() {
        final DeckGroup sealed = SealedCardPoolGenerator.generateSealedDeck(true);
        if (sealed == null) { return; }

        final ACEditorBase<? extends InventoryItem, T> editor = (ACEditorBase<? extends InventoryItem, T>) new CEditorLimited(
                FModel.getDecks().getSealed(), FScreen.DECK_EDITOR_SEALED, CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture());

        Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_SEALED);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(editor);
        editor.getDeckController().setModel((T) sealed);
    }

}
