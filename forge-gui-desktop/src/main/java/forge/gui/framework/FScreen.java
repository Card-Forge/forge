package forge.gui.framework;

import java.io.File;

import forge.Singletons;
import forge.localinstance.properties.FileLocation;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.skin.FSkinProp;
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
import forge.util.Localizer;
import forge.view.FView;

/**
 * Definitions for Forge screens
 */
public class FScreen {
    public static final FScreen HOME_SCREEN = new FScreen(
            VHomeUI.SINGLETON_INSTANCE,
            CHomeUI.SINGLETON_INSTANCE,
            "lblHomeWithSpaces",
            FSkin.getIcon(FSkinProp.ICO_FAVICON),
            false,
            "lblExitForge",
            null,
            false);
    public static final FScreen WORKSHOP_SCREEN = new FScreen(
            VWorkshopUI.SINGLETON_INSTANCE,
            CWorkshopUI.SINGLETON_INSTANCE,
            "lblWorkshop",
            FSkin.getIcon(FSkinProp.ICO_SETTINGS), //TODO: Create icon for workshop screen
            false,
            "lblBacktoHome",
            ForgeConstants.WORKSHOP_LAYOUT_FILE,
            false);
    public static final FScreen DECK_EDITOR_CONSTRUCTED = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "lblDeckEditorWithSpaces",
            FSkin.getImage(FSkinProp.IMG_PACK),
            false,
            "lblBacktoHome",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen DECK_EDITOR_ARCHENEMY = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "lblSchemeDeckEditor",
            FSkin.getImage(FSkinProp.IMG_PACK),
            true,
            "lblCloseEditor",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen DECK_EDITOR_COMMANDER = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "lblCommanderDeckEditor",
            FSkin.getImage(FSkinProp.IMG_PACK),
            true,
            "lblCloseEditor",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen DECK_EDITOR_TINY_LEADERS = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "lblTinyLeadersDeckEditor",
            FSkin.getImage(FSkinProp.IMG_PACK),
            true,
            "lblCloseEditor",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen DECK_EDITOR_BRAWL = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "lblBrawlDeckEditor",
            FSkin.getImage(FSkinProp.IMG_PACK),
            true,
            "lblCloseEditor",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen DECK_EDITOR_OATHBREAKER = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "lblOathbreakerDeckEditor",
            FSkin.getImage(FSkinProp.IMG_PACK),
            true,
            "lblCloseEditor",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen DECK_EDITOR_PLANECHASE = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "lblPlanarDeckEditor",
            FSkin.getImage(FSkinProp.IMG_PACK),
            true,
            "lblCloseEditor",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen DECK_EDITOR_DRAFT = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "lblDraftDeckEditor",
            FSkin.getImage(FSkinProp.IMG_PACK),
            true,
            "lblCloseEditor",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen DECK_EDITOR_SEALED = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "lblSealedDeckEditor",
            FSkin.getImage(FSkinProp.IMG_PACK),
            true,
            "lblCloseEditor",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen TOKEN_VIEWER = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "lblTokenViewer",
            FSkin.getImage(FSkinProp.IMG_PACK),
            true,
            "lblCloseViewer",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);

    public static final FScreen DECK_EDITOR_QUEST = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "lblQuestDeckEditor",
            FSkin.getImage(FSkinProp.IMG_PACK),
            true,
            "lblCloseEditor",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen DECK_EDITOR_QUEST_TOURNAMENT = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "lblQuestTournamentDeckEditor",
            FSkin.getImage(FSkinProp.IMG_PACK),
            true,
            "lblCloseEditor",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen QUEST_CARD_SHOP = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "lblSpellShop",
            FSkin.getIcon(FSkinProp.ICO_QUEST_BOOK),
            true,
            "lblLeaveShop",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen DRAFTING_PROCESS = new FScreen(
            VDeckEditorUI.SINGLETON_INSTANCE,
            CDeckEditorUI.SINGLETON_INSTANCE,
            "lblDraft",
            FSkin.getImage(FSkinProp.IMG_ZONE_HAND),
            true,
            "lblLeaveDraft",
            ForgeConstants.EDITOR_LAYOUT_FILE,
            false);
    public static final FScreen QUEST_BAZAAR = new FScreen(
            VBazaarUI.SINGLETON_INSTANCE,
            CBazaarUI.SINGLETON_INSTANCE,
            "lblBazaar",
            FSkin.getIcon(FSkinProp.ICO_QUEST_BOTTLES),
            true,
            "lblLeaveBazaar",
            null,
            false);

    private final IVTopLevelUI view;
    private final ICDoc controller;
    private String tabCaption, closeButtonTooltip;
    private final SkinImage tabIcon;
    private final boolean allowTabClose;
    private final FileLocation layoutFile;
    private final boolean isMatch;
    private String daytime = null;

    private FScreen(final IVTopLevelUI view0, final ICDoc controller0,
            final String tabCaption0, final SkinImage tabIcon0,
            final boolean allowTabClose0, final String closeButtonTooltip0,
            final FileLocation layoutFile0, final boolean isMatch) {
        this.view = view0;
        this.controller = controller0;
        this.tabCaption = Localizer.getInstance().getMessage(tabCaption0);
        this.tabIcon = tabIcon0;
        this.allowTabClose = allowTabClose0;
        this.closeButtonTooltip = Localizer.getInstance().getMessage(closeButtonTooltip0);
        this.layoutFile = layoutFile0;
        this.isMatch = isMatch;
    }

    public static FScreen getMatchScreen(final CMatchUI controller, final VMatchUI view) {
        return new FScreen(
                view,
                controller,
                "lblGame",
                FSkin.getIcon(FSkinProp.ICO_ALPHASTRIKE), //TODO: Create icon for match screen
                true,
                "lblConcedeGame",
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
    public String getDaytime() {
        return daytime;
    }
    public void setDaytime(final String daytime) {
        this.daytime = daytime;
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
            FOptionPane.showErrorDialog(Localizer.getInstance().getMessage("txerrFailedtodeletelayoutfile"));
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
