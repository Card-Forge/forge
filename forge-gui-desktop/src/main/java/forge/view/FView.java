package forge.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import forge.Singletons;
import forge.assets.FSkinProp;
import forge.control.RestartUtil;
import forge.gui.ImportDialog;
import forge.gui.SOverlayUtils;
import forge.gui.framework.DragCell;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.gui.framework.IVDoc;
import forge.gui.framework.SLayoutConstants;
import forge.gui.framework.SLayoutIO;
import forge.properties.ForgeConstants;
import forge.screens.bazaar.VBazaarUI;
import forge.screens.deckeditor.VDeckEditorUI;
import forge.screens.home.VHomeUI;
import forge.sound.MusicPlaylist;
import forge.sound.SoundSystem;
import forge.toolbox.CardFaceSymbols;
import forge.toolbox.FAbsolutePositioner;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FOverlay;
import forge.toolbox.FPanel;
import forge.toolbox.FProgressBar;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedLayeredPane;
import forge.util.BuildInfo;

public enum FView {
    SINGLETON_INSTANCE;

    public static final Integer DIALOG_BACKDROP_LAYER = JLayeredPane.MODAL_LAYER - 1;
    public static final Integer NAVIGATION_BAR_LAYER = DIALOG_BACKDROP_LAYER - 1;
    public static final Integer NAVIGATION_BAR_REVEAL_LAYER = NAVIGATION_BAR_LAYER - 1;
    public static final Integer OVERLAY_LAYER = NAVIGATION_BAR_REVEAL_LAYER - 1;
    public static final Integer TARGETING_LAYER = OVERLAY_LAYER - 1;

    private final List<DragCell> allCells = new ArrayList<DragCell>();
    private SplashFrame frmSplash;

    // Top-level UI components; all have getters.
    private final FFrame frmDocument = new FFrame();
    // A layered pane is the frame's viewport, allowing overlay effects.
    private final SkinnedLayeredPane lpnDocument = new SkinnedLayeredPane();
    // The status bar to display at the bottom of the frame
    private FNavigationBar navigationBar;
    // The content panel is placed in the layered pane.
    private final JPanel pnlContent = new JPanel();
    // An insets panel neatly maintains a space from the edges of the window and
    // whatever layout is happening, without having to explicitly define a margin each time.
    private FPanel pnlInsets;
    // Preview panel is what is shown when a drag cell is being moved around
    private final JPanel pnlPreview = new PreviewPanel();
    // Tab overflow is for the +X display for extra tabs.
    private final JPanel pnlTabOverflow = new JPanel(new MigLayout("insets 0, gap 0, wrap"));

    private FView() {
        frmSplash = new SplashFrame();
        frmDocument.setTitle("Forge: " + BuildInfo.getVersionString());
        JOptionPane.setRootFrame(frmDocument);
    }

    /** */
    public void initialize() {
        // pnlInsets and navigationBar are skinned components
        // which must be instantiated after the skin is loaded.
        pnlInsets = new FPanel(new BorderLayout());
        pnlInsets.setBorderToggle(false);
        navigationBar = new FNavigationBar(frmDocument);

        // Frame styling
        frmDocument.initialize(navigationBar);
        frmDocument.setMinimumSize(new Dimension(800, 600));
        frmDocument.setLocationRelativeTo(null);
        frmDocument.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frmDocument.setIconImage(FSkin.getIcon(FSkinProp.ICO_FAVICON));

        // Frame components
        frmDocument.setContentPane(lpnDocument);
        lpnDocument.add(pnlInsets, (Integer) 1);
        FAbsolutePositioner.SINGLETON_INSTANCE.initialize(lpnDocument, 2);
        lpnDocument.add(pnlPreview, (Integer) 3);
        lpnDocument.add(pnlTabOverflow, (Integer) 4);
        lpnDocument.add(navigationBar, NAVIGATION_BAR_LAYER);
        lpnDocument.add(navigationBar.getPnlReveal(), NAVIGATION_BAR_REVEAL_LAYER);
        lpnDocument.add(FOverlay.SINGLETON_INSTANCE.getPanel(), OVERLAY_LAYER);
        lpnDocument.add(FDialog.getBackdropPanel(), DIALOG_BACKDROP_LAYER);
        // Note: when adding new panels here, keep in mind that the layered pane
        // has a null layout, so new components will be W0 x H0 pixels - gotcha!
        // FControl has a method called "sizeComponents" which will fix this.

        pnlInsets.add(pnlContent, BorderLayout.CENTER);
        pnlInsets.setBackgroundTexture(FSkin.getIcon(FSkinProp.BG_TEXTURE));
        pnlInsets.setCornerDiameter(0);
        pnlInsets.setBorder(new EmptyBorder(
                SLayoutConstants.BORDER_T, SLayoutConstants.BORDER_T, 0, 0));

        pnlContent.setOpaque(false);
        pnlContent.setLayout(null);

        FOverlay.SINGLETON_INSTANCE.getPanel().setBackground(FSkin.getColor(FSkin.Colors.CLR_OVERLAY));

        // Populate all drag tab components.
        cacheUIStates();

        // Does not use progress bar, due to be deprecated with battlefield refactoring.
        CardFaceSymbols.loadImages();

        // Initialize actions on all drag tab components (which should
        // be realized / populated already).
        for (final EDocID doc : EDocID.values()) {
            final IVDoc<? extends ICDoc> d = doc.getDoc();
            if (d != null) {
                d.getLayoutControl().initialize();
            }
        }

        // All is ready to go - fire up home screen and discard splash frame.
        Singletons.getControl().setCurrentScreen(FScreen.HOME_SCREEN);

        FView.this.frmSplash.dispose();
        FView.this.frmSplash = null;

        SLayoutIO.loadWindowLayout();
        frmDocument.setVisible(true);

        // remove this once our userbase has been migrated to the profile layout
        {
            // get profile directories -- if one of them is actually under the res directory, don't
            // count is as data to migrate
            final Set<File> profileDirs = new HashSet<File>();
            for (final String dname : ForgeConstants.PROFILE_DIRS) {
                profileDirs.add(new File(dname));
            }

            final List<File> resDirs = new ArrayList<File>();
            for (final String resDir : Lists.newArrayList("decks", "gauntlet", "layouts", "pics", "preferences", "quest/data")) {
                resDirs.add(new File("res", resDir));
            }

            final Set<File> doNotDeleteDirs = new HashSet<File>();
            for (final String dir : Lists.newArrayList("decks", "decks/constructed", "decks/draft", "decks/plane", "decks/scheme", "decks/sealed", "gauntlet", "layouts", "pics", "preferences", "quest/data")) {
                doNotDeleteDirs.add(new File("res", dir));
            }

            // if we have any data to migrate, pop up the migration dialog
            if (_addRemainingFiles(null, resDirs, profileDirs, doNotDeleteDirs)) {
                new ImportDialog("res", new Runnable() {
                    @Override public void run() {
                        // remove known cruft files, yes this is ugly, but it's also temporary
                        for (final String cruftFile : Lists.newArrayList("decks/SkieraCube-cards_not_supported_yet.txt", "decks/cube/ArabianExtended.dck", "decks/cube/GtcGuildBoros.dck", "decks/cube/GtcGuildDimir.dck", "decks/cube/GtcGuildGruul.dck", "decks/cube/GtcGuildOrzhov.dck", "decks/cube/GtcGuildSimic.dck", "decks/cube/GtcPromoBoros.dck", "decks/cube/GtcPromoDimir.dck", "decks/cube/GtcPromoGruul.dck", "decks/cube/GtcPromoOrzhov.dck", "decks/cube/GtcPromoSimic.dck", "decks/cube/JuzamjediCube.dck", "decks/cube/RtRGuildAzorius.dck", "decks/cube/RtRGuildGolgari.dck", "decks/cube/RtRGuildIzzet.dck", "decks/cube/RtRGuildRakdos.dck", "decks/cube/RtRGuildSelesnya.dck", "decks/cube/RtRPromoAzorius.dck", "decks/cube/RtRPromoGolgari.dck", "decks/cube/RtRPromoIzzet.dck", "decks/cube/RtRPromoRakdos.dck", "decks/cube/RtRPromoSelesnya.dck", "decks/cube/SkieraCube.dck", "gauntlet/LOCKED_DotP Preconstructed.dat", "gauntlet/LOCKED_Swimming With Sharks.dat", "layouts/editor_default.xml", "layouts/home_default.xml", "layouts/match_default.xml", "pics/snow_covered_forest1.jpg", "pics/snow_covered_forest2.jpg", "pics/snow_covered_forest3.jpg", "pics/snow_covered_island1.jpg", "pics/snow_covered_island2.jpg", "pics/snow_covered_island3.jpg", "pics/snow_covered_mountain1.jpg", "pics/snow_covered_mountain2.jpg", "pics/snow_covered_mountain3.jpg", "pics/snow_covered_plains1.jpg", "pics/snow_covered_plains2.jpg", "pics/snow_covered_plains3.jpg", "pics/snow_covered_swamp1.jpg", "pics/snow_covered_swamp2.jpg", "pics/snow_covered_swamp3.jpg", "pics/VAN/Birds of Paradise Avatar.full.jpg", "pics/VAN/Erhnam Djinn Avatar.full.jpg", "pics/VAN/Goblin Warchief Avatar.full.jpg", "pics/VAN/Grinning Demon Avatar.full.jpg", "pics/VAN/Platinum Angel Avatar.full.jpg", "pics/VAN/Prodigal Sorcerer Avatar.full.jpg", "pics/VAN/Rith, the Awakener Avatar.full.jpg", "pics/VAN/Royal Assassin Avatar.full.jpg", "pics/VAN/Serra Angel Avatar.full.jpg", "pics/VAN/Tradewind Rider Avatar.full.jpg", "pics_product/10E.jpg", "pics_product/2ED.jpg", "pics_product/3ED.jpg", "pics_product/4ED.jpg", "pics_product/5DN.jpg", "pics_product/5ED.jpg", "pics_product/6ED.jpg", "pics_product/7ED.jpg", "pics_product/8ED.jpg", "pics_product/9ED.jpg", "pics_product/ALA.jpg", "pics_product/ALL.jpg", "pics_product/APC.jpg", "pics_product/ARB.jpg", "pics_product/ARN.jpg", "pics_product/ATQ.jpg", "pics_product/BOK.jpg", "pics_product/CFX.jpg", "pics_product/CHK.jpg", "pics_product/CHR.jpg", "pics_product/CSP.jpg", "pics_product/DIS.jpg", "pics_product/DKA.jpg", "pics_product/DRK.jpg", "pics_product/DST.jpg", "pics_product/EVE.jpg", "pics_product/EXO.jpg", "pics_product/FEM.jpg", "pics_product/FUT.jpg", "pics_product/GPT.jpg", "pics_product/HML.jpg", "pics_product/ICE.jpg", "pics_product/INV.jpg", "pics_product/ISD.jpg", "pics_product/JUD.jpg", "pics_product/LEA.jpg", "pics_product/LEB.jpg", "pics_product/LEG.jpg", "pics_product/LGN.jpg", "pics_product/LRW.jpg", "pics_product/M10.jpg", "pics_product/M11.jpg", "pics_product/M12.jpg", "pics_product/MBS.jpg", "pics_product/MIR.jpg", "pics_product/MMQ.jpg", "pics_product/MOR.jpg", "pics_product/MRD.jpg", "pics_product/NMS.jpg", "pics_product/NPH.jpg", "pics_product/ODY.jpg", "pics_product/ONS.jpg", "pics_product/PCY.jpg", "pics_product/PLC.jpg", "pics_product/PLS.jpg", "pics_product/PO2.jpg", "pics_product/POR.jpg", "pics_product/PTK.jpg", "pics_product/RAV.jpg", "pics_product/ROE.jpg", "pics_product/S99.jpg", "pics_product/SCG.jpg", "pics_product/SHM.jpg", "pics_product/SOK.jpg", "pics_product/SOM.jpg", "pics_product/STH.jpg", "pics_product/TMP.jpg", "pics_product/TOR.jpg", "pics_product/TSP.jpg", "pics_product/UDS.jpg", "pics_product/ULG.jpg", "pics_product/USG.jpg", "pics_product/VIS.jpg", "pics_product/WTH.jpg", "pics_product/WWK.jpg", "pics_product/ZEN.jpg", "pics_product/booster/7E.png", "pics_product/booster/AP.png", "pics_product/booster/DPA.png", "pics_product/booster/EX.png", "pics_product/booster/IN.png", "pics_product/booster/MI.png", "pics_product/booster/OD.png", "pics_product/booster/PS.png", "pics_product/booster/ST.png", "pics_product/booster/TE.png", "pics_product/booster/UD.png", "pics_product/booster/UL.png", "pics_product/booster/UZ.png", "pics_product/booster/VI.png", "pics_product/booster/WL.png", "preferences/.project", "preferences/editor.default.preferences", "preferences/main.properties", "quest/quest.preferences", "quest/quest.properties")) {
                            new File("res", cruftFile).delete();
                        }

                        // assemble a list of remaining files.
                        final List<File> remainingFiles = new LinkedList<File>();
                        _addRemainingFiles(remainingFiles, resDirs, profileDirs, doNotDeleteDirs);

                        // if any files remain, display them and make clear that they should be moved or
                        // deleted manually or the user will continue to be prompted for migration
                        final FPanel p = new FPanel(new MigLayout("insets dialog, gap 10, center, wrap"));
                        p.setOpaque(false);
                        p.setBackgroundTexture(FSkin.getIcon(FSkinProp.BG_TEXTURE));

                        if (remainingFiles.isEmpty()) {
                            p.add(new FLabel.Builder().text("<html>You're done!  It looks like everything went smoothly." +
                                    "  Now just restart Forge to load the data from its new home!  Note that there is more data available" +
                                    " from the downloaders now.  You might want to run through the content downloaders to check for new files.</html>").build());
                        } else {
                            p.add(new FLabel.Builder().text("<html>There seem to be a few files left over in your old data" +
                                    " directories.  They should be deleted or moved somewhere else to avoid having the data" +
                                    " migration prompt pop up again!</html>").build());

                            final JTextArea files = new JTextArea(StringUtils.join(remainingFiles, '\n'));
                            files.setFont(new Font("Monospaced", Font.PLAIN, 10));
                            files.setOpaque(false);
                            files.setWrapStyleWord(true);
                            files.setLineWrap(true);
                            files.setEditable(false);
                            final FScrollPane scroller = new FScrollPane(files, true);
                            p.add(scroller, "w 600:100%:100%, h 100:100%:100%, gaptop 10");

                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    // resize the panel properly for the new log contents
                                    p.getParent().validate();
                                    p.getParent().invalidate();
                                }
                            });
                        }

                        final FButton btnOk = new FButton(remainingFiles.isEmpty() ? "Restart Forge" : "Close Forge");
                        btnOk.addActionListener(new ActionListener() {
                            @Override public void actionPerformed(final ActionEvent e) {
                                if (remainingFiles.isEmpty()) {
                                    RestartUtil.restartApplication(null);
                                } else {
                                    System.exit(0);
                                }
                            }
                        });
                        p.add(btnOk, "center, w pref+64!, h pref+12!, gaptop 20");

                        final JPanel overlay = FOverlay.SINGLETON_INSTANCE.getPanel();
                        overlay.setLayout(new MigLayout("insets 0, gap 0, wrap, ax center, ay center"));
                        overlay.add(p, "w 100::80%, h 50::90%");
                        SOverlayUtils.showOverlay();

                        SwingUtilities.invokeLater(new Runnable() {
                            @Override public void run() {
                                btnOk.requestFocusInWindow();
                            }
                        });
                    }
                }).show();
            }
        }

        //start background music
        SoundSystem.instance.setBackgroundMusic(MusicPlaylist.MENUS);
    }

    // will populate remainingFiles with remaining files if not null, returns whether any files have
    // been added to remainingFiles (or would have been added if remainingFiles is null)
    // directories listed in profileDirs will not be searched
    // removes empty directories to reduce tree conflicts
    private static boolean _addRemainingFiles(final List<File> remainingFiles, final List<File> dirRoots, final Set<File> profileDirs, final Set<File> doNotDeleteDirs) {
        final Deque<File> stack = new LinkedList<File>(dirRoots);
        final Set<File> seenDirs = new HashSet<File>();
        boolean ret = false;
        while (!stack.isEmpty()) {
            File cur = stack.peek();
            if (profileDirs.contains(cur)) {
                // don't touch active profile dirs
                stack.pop();
                continue;
            }

            if (seenDirs.contains(cur)) {
                cur = stack.pop();
                if (cur.exists() && !doNotDeleteDirs.contains(cur)) {
                    // remove empty dir (will fail if not empty)
                    cur.delete();
                }
                continue;
            }

            seenDirs.add(cur);
            final File[] curListing = cur.listFiles();
            if (null == curListing) {
                continue;
            }
            for (final File f : curListing) {
                if (f.isDirectory()) {
                    if (!".svn".equals(f.getName())) {
                        stack.push(f);
                    }
                } else {
                    if (null == remainingFiles) {
                        return true;
                    }
                    remainingFiles.add(f);
                    ret = true;
                }
            }
        }

        return ret;
    }

    /** @return {@link forge.view.SplashFrame} */
    public SplashFrame getSplash() {
        return frmSplash;
    }

    /** @return {@link javax.swing.JFrame} */
    public FFrame getFrame() {
        return frmDocument;
    }

    /** @return {@link javax.swing.JLayeredPane} */
    public SkinnedLayeredPane getLpnDocument() {
        return lpnDocument;
    }

    /** @return {@link forge.view.FNavigationBar} */
    public FNavigationBar getNavigationBar() {
        return navigationBar;
    }

    /** @return {@link forge.toolbox.FPanel} */
    public FPanel getPnlInsets() {
        return pnlInsets;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlContent() {
        return pnlContent;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlPreview() {
        return pnlPreview;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlTabOverflow() {
        return pnlTabOverflow;
    }

    /** @return {@link java.util.List}<{@link forge.gui.framework.DragCell}> */
    public List<DragCell> getDragCells() {
        final List<DragCell> clone = new ArrayList<DragCell>();
        clone.addAll(allCells);
        return clone;
    }

    /** @param pnl0 &emsp; {@link forge.gui.framework.DragCell} */
    public void addDragCell(final DragCell pnl0) {
        allCells.add(pnl0);
        pnlContent.add(pnl0);
    }

    /** @param pnl0 &emsp; {@link forge.gui.framework.DragCell} */
    public void removeDragCell(final DragCell pnl0) {
        allCells.remove(pnl0);
        pnlContent.remove(pnl0);
    }

    /** */
    public void removeAllDragCells() {
        allCells.clear();
        pnlContent.removeAll();
    }

    /** PreviewPanel shows where a dragged component could
     * come to rest when the mouse is released.<br>
     * This class is an unfortunate necessity to overcome
     * translucency issues for preview panel. */
    @SuppressWarnings("serial")
    class PreviewPanel extends JPanel {
        /** PreviewPanel shows where a dragged component could
         * come to rest when the mouse is released. */
        public PreviewPanel() {
            super();
            setOpaque(false);
            setVisible(false);
            setBorder(new LineBorder(Color.DARK_GRAY, 2));
        }

        @Override
        public void paintComponent(final Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(0, 0, 0, 50));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private static void cacheUIStates() {
        VHomeUI.SINGLETON_INSTANCE.instantiate();
        VDeckEditorUI.SINGLETON_INSTANCE.instantiate();
        VBazaarUI.SINGLETON_INSTANCE.instantiate();
    }

    public void incrementSplashProgessBar(final int value) {
        if (this.frmSplash == null) { return; }
        this.frmSplash.getProgressBar().setValueThreadSafe(value);
    }

    public void setSplashProgessBarMessage(final String message) {
        setSplashProgessBarMessage(message, 0);
    }
    public void setSplashProgessBarMessage(final String message, final int cnt) {
        if (this.frmSplash == null) { return; }

        final FProgressBar progressBar = this.frmSplash.getProgressBar(); //must cache for sake of runnable below
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if ( cnt > 0 ) {
                    progressBar.reset();
                    progressBar.setMaximum(cnt);
                }
                progressBar.setShowETA(false);
                progressBar.setShowCount(cnt > 0);
                progressBar.setDescription(message);
            }
        });
    }

    public void refreshAllCellLayouts(final boolean showTabs) {
        for (final DragCell cell : allCells) {
            cell.doCellLayout(showTabs);
        }
    }
}
