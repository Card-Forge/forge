package forge.gui.framework;

import java.io.File;

import forge.Singletons;
import forge.gui.bazaar.CBazaarUI;
import forge.gui.bazaar.VBazaarUI;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.VDeckEditorUI;
import forge.gui.home.CHomeUI;
import forge.gui.home.VHomeUI;
import forge.gui.match.CMatchUI;
import forge.gui.match.VMatchUI;
import forge.gui.toolbox.FOptionPane;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FSkin.SkinImage;
import forge.gui.workshop.CWorkshopUI;
import forge.gui.workshop.VWorkshopUI;
import forge.properties.FileLocation;
import forge.properties.NewConstants;

/** 
 * Definitions for Forge screens
 *
 */
public enum FScreen {
    HOME_SCREEN(
            VHomeUI.SINGLETON_INSTANCE,
            CHomeUI.SINGLETON_INSTANCE,
            "Home",
            FSkin.getIcon(FSkin.InterfaceIcons.ICO_FAVICON),
            false,
            "Exit Forge",
            null),
    MATCH_SCREEN(
            VMatchUI.SINGLETON_INSTANCE,
            CMatchUI.SINGLETON_INSTANCE,
            "Game",
            FSkin.getIcon(FSkin.DockIcons.ICO_ALPHASTRIKE), //TODO: Create icon for match screen
            true,
            "Concede Game",
            NewConstants.MATCH_LAYOUT_FILE),
    WORKSHOP_SCREEN(
            VWorkshopUI.SINGLETON_INSTANCE,
            CWorkshopUI.SINGLETON_INSTANCE,
            "Workshop",
            FSkin.getIcon(FSkin.DockIcons.ICO_SETTINGS), //TODO: Create icon for workshop screen
            false,
            "Back to Home",
            NewConstants.WORKSHOP_LAYOUT_FILE),
    DECK_EDITOR_CONSTRUCTED(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "Deck Editor",
            FSkin.getImage(FSkin.EditorImages.IMG_PACK),
            false,
            "Back to Home",
            NewConstants.EDITOR_LAYOUT_FILE),
    DECK_EDITOR_ARCHENEMY(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "Scheme Deck Editor",
            FSkin.getImage(FSkin.EditorImages.IMG_PACK),
            true,
            "Close Editor",
            NewConstants.EDITOR_LAYOUT_FILE),
    DECK_EDITOR_COMMANDER(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "Commander Deck Editor",
            FSkin.getImage(FSkin.EditorImages.IMG_PACK),
            true,
            "Close Editor",
            NewConstants.EDITOR_LAYOUT_FILE),
    DECK_EDITOR_PLANECHASE(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "Planar Deck Editor",
            FSkin.getImage(FSkin.EditorImages.IMG_PACK),
            true,
            "Close Editor",
            NewConstants.EDITOR_LAYOUT_FILE),
    DECK_EDITOR_VANGUARD(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "Vanguard Deck Editor",
            FSkin.getImage(FSkin.EditorImages.IMG_PACK),
            true,
            "Close Editor",
            NewConstants.EDITOR_LAYOUT_FILE),
    DECK_EDITOR_DRAFT(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "Draft Deck Editor",
            FSkin.getImage(FSkin.EditorImages.IMG_PACK),
            true,
            "Close Editor",
            NewConstants.EDITOR_LAYOUT_FILE),
    DECK_EDITOR_SEALED(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "Sealed Deck Editor",
            FSkin.getImage(FSkin.EditorImages.IMG_PACK),
            true,
            "Close Editor",
            NewConstants.EDITOR_LAYOUT_FILE),
    DECK_EDITOR_QUEST(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "Quest Deck Editor",
            FSkin.getImage(FSkin.EditorImages.IMG_PACK),
            true,
            "Close Editor",
            NewConstants.EDITOR_LAYOUT_FILE),
    QUEST_CARD_SHOP(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "Spell Shop",
            FSkin.getIcon(FSkin.QuestIcons.ICO_BOOK),
            true,
            "Leave Shop",
            NewConstants.EDITOR_LAYOUT_FILE),
    DRAFTING_PROCESS(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "Draft",
            FSkin.getImage(FSkin.ZoneImages.ICO_HAND),
            true,
            "Leave Draft",
            NewConstants.EDITOR_LAYOUT_FILE),
    QUEST_BAZAAR(
            VBazaarUI.SINGLETON_INSTANCE,
            CBazaarUI.SINGLETON_INSTANCE,
            "Bazaar",
            FSkin.getIcon(FSkin.QuestIcons.ICO_BOTTLES),
            true,
            "Leave Bazaar",
            null);

    private final IVTopLevelUI view;
    private final ICDoc controller;
    private final String tabCaption;
    private final SkinImage tabIcon;
    private final boolean allowTabClose;
    private final String closeButtonTooltip;
    private final FileLocation layoutFile;
    
    private FScreen(IVTopLevelUI view0, ICDoc controller0, String tabCaption0, SkinImage tabIcon0, boolean allowTabClose0, String closeButtonTooltip0, FileLocation layoutFile0) {
        this.view = view0;
        this.controller = controller0;
        this.tabCaption = tabCaption0;
        this.tabIcon = tabIcon0;
        this.allowTabClose = allowTabClose0;
        this.closeButtonTooltip = closeButtonTooltip0;
        this.layoutFile = layoutFile0;
    }

    public IVTopLevelUI getView() {
        return view;
    }

    public ICDoc getController() {
        return controller;
    }

    public String getTabCaption() {
        return tabCaption;
    }
    
    public SkinImage getTabIcon() {
        return tabIcon;
    }
    
    public boolean allowTabClose() {
        return allowTabClose;
    }
    
    public String getCloseButtonTooltip() {
        return closeButtonTooltip;
    }
    
    public boolean onSwitching(FScreen toScreen) {
        return view.onSwitching(this, toScreen);
    }
    
    public boolean onClosing() {
        return view.onClosing(this);
    }
    
    public FileLocation getLayoutFile() {
        return layoutFile;
    }

    public boolean deleteLayoutFile() {
        if (layoutFile == null) { return false; }

        try {
            File file = new File(layoutFile.userPrefLoc);
            file.delete();
            return true;
        }
        catch (final Exception e) {
            e.printStackTrace();
            FOptionPane.showErrorMessageDialog("Failed to delete layout file.");
        }
        return false;
    }

    public void open() {
        Singletons.getControl().setCurrentScreen(this);
    }
    
    public void close() {
        Singletons.getView().getNavigationBar().closeTab(this);
    }
}
