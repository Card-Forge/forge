package forge.gui.framework;

import java.io.File;

import forge.Singletons;
import forge.assets.FSkinProp;
import forge.properties.FileLocation;
import forge.properties.ForgeConstants;
import forge.screens.bazaar.CBazaarUI;
import forge.screens.bazaar.VBazaarUI;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.VDeckEditorUI;
import forge.screens.home.CHomeUI;
import forge.screens.home.VHomeUI;
import forge.screens.match.CMatchUI;
import forge.screens.match.VMatchUI;
import forge.screens.workshop.CWorkshopUI;
import forge.screens.workshop.VWorkshopUI;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinImage;
import forge.view.FView;

/**
 * Definitions for Forge screens
 */
public class FScreen {
    public static final FScreen HOME_SCREEN = new FScreen(
            VHomeUI.SINGLETON_INSTANCE,
            CHomeUI.SINGLETON_INSTANCE,
            "Home",
            FSkin.getIcon(FSkinProp.ICO_FAVICON),
            false,
            "Exit Forge",
            null,
            false);
    public static final FScreen WORKSHOP_SCREEN = new FScreen(
            VWorkshopUI.SINGLETON_INSTANCE,
            CWorkshopUI.SINGLETON_INSTANCE,
            "Workshop",
            FSkin.getIcon(FSkinProp.ICO_SETTINGS), //TODO: Create icon for workshop screen
            false,
            "Back to Home",
            ForgeConstants.WORKSHOP_LAYOUT_FILE,
            false);
    public static final FScreen DECK_EDITOR_CONSTRUCTED = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "Deck Editor",
            FSkin.getImage(FSkinProp.IMG_PACK),
            false,
            "Back to Home",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen DECK_EDITOR_ARCHENEMY = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "Scheme Deck Editor",
            FSkin.getImage(FSkinProp.IMG_PACK),
            true,
            "Close Editor",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen DECK_EDITOR_COMMANDER = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "Commander Deck Editor",
            FSkin.getImage(FSkinProp.IMG_PACK),
            true,
            "Close Editor",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen DECK_EDITOR_PLANECHASE = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "Planar Deck Editor",
            FSkin.getImage(FSkinProp.IMG_PACK),
            true,
            "Close Editor",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen DECK_EDITOR_DRAFT = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "Draft Deck Editor",
            FSkin.getImage(FSkinProp.IMG_PACK),
            true,
            "Close Editor",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen DECK_EDITOR_SEALED = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "Sealed Deck Editor",
            FSkin.getImage(FSkinProp.IMG_PACK),
            true,
            "Close Editor",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen DECK_EDITOR_QUEST = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "Quest Deck Editor",
            FSkin.getImage(FSkinProp.IMG_PACK),
            true,
            "Close Editor",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen DECK_EDITOR_QUEST_TOURNAMENT = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "Quest Tournament Deck Editor",
            FSkin.getImage(FSkinProp.IMG_PACK),
            true,
            "Close Editor",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen QUEST_CARD_SHOP = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "Spell Shop",
            FSkin.getIcon(FSkinProp.ICO_QUEST_BOOK),
            true,
            "Leave Shop",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen DRAFTING_PROCESS = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "Draft",
            FSkin.getImage(FSkinProp.IMG_ZONE_HAND),
            true,
            "Leave Draft",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen QUEST_BAZAAR = new FScreen(
            VBazaarUI.SINGLETON_INSTANCE,
            CBazaarUI.SINGLETON_INSTANCE,
            "Bazaar",
            FSkin.getIcon(FSkinProp.ICO_QUEST_BOTTLES),
            true,
            "Leave Bazaar",
            null,
            false);

    private final IVTopLevelUI view;
    private final ICDoc controller;
    private String tabCaption, closeButtonTooltip;
    private final SkinImage tabIcon;
    private final boolean allowTabClose;
    private final FileLocation layoutFile;
    private final boolean isMatch;

    private FScreen(final IVTopLevelUI view0, final ICDoc controller0,
            final String tabCaption0, final SkinImage tabIcon0,
            final boolean allowTabClose0, final String closeButtonTooltip0,
            final FileLocation layoutFile0, final boolean isMatch) {
        this.view = view0;
        this.controller = controller0;
        this.tabCaption = tabCaption0;
        this.tabIcon = tabIcon0;
        this.allowTabClose = allowTabClose0;
        this.closeButtonTooltip = closeButtonTooltip0;
        this.layoutFile = layoutFile0;
        this.isMatch = isMatch;
    }

    public static FScreen getMatchScreen(final CMatchUI controller, final VMatchUI view) {
        return new FScreen(
                view,
                controller,
                "Game",
                FSkin.getIcon(FSkinProp.ICO_ALPHASTRIKE), //TODO: Create icon for match screen
                true,
                "Concede Game",
                ForgeConstants.MATCH_LAYOUT_FILE,
                true);
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
    public void setTabCaption(final String caption) {
        this.tabCaption = caption;
        FView.SINGLETON_INSTANCE.getNavigationBar().updateTitle(this);
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
    public void setCloseButtonTooltip(String closeButtonTooltip0) {
        closeButtonTooltip = closeButtonTooltip0;
    }

    public boolean onSwitching(final FScreen toScreen) {
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
        return deleteLayoutFile(layoutFile);
    }
    public static boolean deleteMatchLayoutFile() {
        return deleteLayoutFile(ForgeConstants.MATCH_LAYOUT_FILE);
    }
    private static boolean deleteLayoutFile(final FileLocation file) {
        try {
            final File f = new File(file.userPrefLoc);
            f.delete();
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
            FOptionPane.showErrorDialog("Failed to delete layout file.");
        }
        return false;

    }

    public void close() {
        Singletons.getView().getNavigationBar().closeTab(this);
    }

    public boolean isMatchScreen() {
        return isMatch;
    }
}
