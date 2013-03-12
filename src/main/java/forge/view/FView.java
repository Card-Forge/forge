package forge.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
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

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import forge.Singletons;
import forge.control.FControl;
import forge.control.RestartUtil;
import forge.gui.DialogMigrateProfile;
import forge.gui.SOverlayUtils;
import forge.gui.deckeditor.VDeckEditorUI;
import forge.gui.framework.DragCell;
import forge.gui.framework.EDocID;
import forge.gui.framework.SLayoutConstants;
import forge.gui.home.VHomeUI;
import forge.gui.match.TargetingOverlay;
import forge.gui.match.VMatchUI;
import forge.gui.toolbox.FButton;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FOverlay;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FSkin;
import forge.model.BuildInfo;
import forge.properties.NewConstants;

/** */
public enum FView {
    /** */
    SINGLETON_INSTANCE;

    /** */
    public static final Integer TARGETING_LAYER = JLayeredPane.MODAL_LAYER - 1;
    private final List<DragCell> allCells = new ArrayList<DragCell>();
    private SplashFrame frmSplash;

    // Non-singleton instances (deprecated, but not updated yet)
    private ViewBazaarUI bazaar = null;

    // Top-level UI components; all have getters.
    private final JFrame frmDocument = new JFrame();
    // A layered pane is the frame's viewport, allowing overlay effects.
    private final JLayeredPane lpnDocument = new JLayeredPane();
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

        // Insets panel has background image / texture, which
        // must be instantiated after the skin is loaded.
        pnlInsets = new FPanel(new BorderLayout());
    }

    /** */
    public void initialize() {
        // Frame styling
        frmDocument.setMinimumSize(new Dimension(800, 600));
        frmDocument.setLocationRelativeTo(null);
        frmDocument.setExtendedState(frmDocument.getExtendedState() | Frame.MAXIMIZED_BOTH);
        frmDocument.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frmDocument.setIconImage(FSkin.getIcon(FSkin.InterfaceIcons.ICO_FAVICON).getImage());
        frmDocument.setTitle("Forge: " + BuildInfo.getVersionString());

        // Frame components
        frmDocument.setContentPane(lpnDocument);
        lpnDocument.add(pnlInsets, (Integer) 1);
        lpnDocument.add(pnlPreview, (Integer) 2);
        lpnDocument.add(pnlTabOverflow, (Integer) 3);
        lpnDocument.add(FOverlay.SINGLETON_INSTANCE.getPanel(), JLayeredPane.MODAL_LAYER);
        // Note: when adding new panels here, keep in mind that the layered pane
        // has a null layout, so new components will be W0 x H0 pixels - gotcha!
        // FControl has a method called "sizeComponents" which will fix this.
        lpnDocument.add(TargetingOverlay.SINGLETON_INSTANCE.getPanel(), TARGETING_LAYER);

        pnlInsets.add(pnlContent, BorderLayout.CENTER);
        pnlInsets.setBackgroundTexture(FSkin.getIcon(FSkin.Backgrounds.BG_TEXTURE));
        pnlInsets.setCornerDiameter(0);
        pnlInsets.setBorder(new EmptyBorder(
                SLayoutConstants.BORDER_T, SLayoutConstants.BORDER_T, 0, 0));

        pnlContent.setOpaque(false);
        pnlContent.setLayout(null);

        FOverlay.SINGLETON_INSTANCE.getPanel().setBackground(FSkin.getColor(FSkin.Colors.CLR_OVERLAY));

        // Populate all drag tab components.
        this.cacheUIStates();

        // Initialize actions on all drag tab components (which should
        // be realized / populated already).
        for (EDocID doc : EDocID.values()) {
            doc.getDoc().getLayoutControl().initialize();
        }

        // All is ready to go - fire up home screen and discard splash frame.
        Singletons.getControl().changeState(FControl.Screens.HOME_SCREEN);

        FView.this.frmSplash.dispose();
        FView.this.frmSplash = null;
         
        // Allow OS to set location. Hopefully this doesn't cause issues
        frmDocument.setLocationByPlatform(true);
        frmDocument.setVisible(true);
        
        // remove this once our userbase has been migrated to the profile layout
        {
            // get profile directories -- if one of them is actually under the res directory, don't
            // try to migrate it
            final Set<File> profileDirs = new HashSet<File>();
            for (String dname : NewConstants.PROFILE_DIRS) {
                profileDirs.add(new File(dname));
            }

            final List<File> resDirs = new ArrayList<File>();
            for (String resDir : Lists.newArrayList("decks", "gauntlet", "layouts", "pics", "pics_product", "preferences", "quest/data")) {
                resDirs.add(new File("res", resDir));
            }
            
            // check quickly whether we have any data to migrate
            boolean hasData = false;
            for (File resDir : resDirs) {
                if (resDir.exists() && !profileDirs.contains(resDir)) {
                    // cycle through all dirs instead of breaking after the first found so each dir is printed to stdout
                    System.out.println("profile data found in obsolete location: " + resDir.getAbsolutePath());
                    hasData = true;
                }
            }
        
            if (hasData) {
                new DialogMigrateProfile("res", true, new Runnable() {
                    @Override public void run() {
                        // remove known cruft files, yes this is ugly, but it's also temporary
                        for (String cruftFile : Lists.newArrayList("decks/SkieraCube-cards_not_supported_yet.txt", "decks/cube/ArabianExtended.dck", "decks/cube/GtcGuildBoros.dck", "decks/cube/GtcGuildDimir.dck", "decks/cube/GtcGuildGruul.dck", "decks/cube/GtcGuildOrzhov.dck", "decks/cube/GtcGuildSimic.dck", "decks/cube/GtcPromoBoros.dck", "decks/cube/GtcPromoDimir.dck", "decks/cube/GtcPromoGruul.dck", "decks/cube/GtcPromoOrzhov.dck", "decks/cube/GtcPromoSimic.dck", "decks/cube/JuzamjediCube.dck", "decks/cube/RtRGuildAzorius.dck", "decks/cube/RtRGuildGolgari.dck", "decks/cube/RtRGuildIzzet.dck", "decks/cube/RtRGuildRakdos.dck", "decks/cube/RtRGuildSelesnya.dck", "decks/cube/RtRPromoAzorius.dck", "decks/cube/RtRPromoGolgari.dck", "decks/cube/RtRPromoIzzet.dck", "decks/cube/RtRPromoRakdos.dck", "decks/cube/RtRPromoSelesnya.dck", "decks/cube/SkieraCube.dck", "gauntlet/LOCKED_DotP Preconstructed.dat", "gauntlet/LOCKED_Swimming With Sharks.dat", "layouts/editor_default.xml", "layouts/home_default.xml", "layouts/match_default.xml", "pics/snow_covered_forest1.jpg", "pics/snow_covered_forest2.jpg", "pics/snow_covered_forest3.jpg", "pics/snow_covered_island1.jpg", "pics/snow_covered_island2.jpg", "pics/snow_covered_island3.jpg", "pics/snow_covered_mountain1.jpg", "pics/snow_covered_mountain2.jpg", "pics/snow_covered_mountain3.jpg", "pics/snow_covered_plains1.jpg", "pics/snow_covered_plains2.jpg", "pics/snow_covered_plains3.jpg", "pics/snow_covered_swamp1.jpg", "pics/snow_covered_swamp2.jpg", "pics/snow_covered_swamp3.jpg", "pics/VAN/Birds of Paradise Avatar.full.jpg", "pics/VAN/Erhnam Djinn Avatar.full.jpg", "pics/VAN/Goblin Warchief Avatar.full.jpg", "pics/VAN/Grinning Demon Avatar.full.jpg", "pics/VAN/Platinum Angel Avatar.full.jpg", "pics/VAN/Prodigal Sorcerer Avatar.full.jpg", "pics/VAN/Rith, the Awakener Avatar.full.jpg", "pics/VAN/Royal Assassin Avatar.full.jpg", "pics/VAN/Serra Angel Avatar.full.jpg", "pics/VAN/Tradewind Rider Avatar.full.jpg", "pics_product/10E.jpg", "pics_product/2ED.jpg", "pics_product/3ED.jpg", "pics_product/4ED.jpg", "pics_product/5DN.jpg", "pics_product/5ED.jpg", "pics_product/6ED.jpg", "pics_product/7ED.jpg", "pics_product/8ED.jpg", "pics_product/9ED.jpg", "pics_product/ALA.jpg", "pics_product/ALL.jpg", "pics_product/APC.jpg", "pics_product/ARB.jpg", "pics_product/ARN.jpg", "pics_product/ATQ.jpg", "pics_product/BOK.jpg", "pics_product/CFX.jpg", "pics_product/CHK.jpg", "pics_product/CHR.jpg", "pics_product/CSP.jpg", "pics_product/DIS.jpg", "pics_product/DKA.jpg", "pics_product/DRK.jpg", "pics_product/DST.jpg", "pics_product/EVE.jpg", "pics_product/EXO.jpg", "pics_product/FEM.jpg", "pics_product/FUT.jpg", "pics_product/GPT.jpg", "pics_product/HML.jpg", "pics_product/ICE.jpg", "pics_product/INV.jpg", "pics_product/ISD.jpg", "pics_product/JUD.jpg", "pics_product/LEA.jpg", "pics_product/LEB.jpg", "pics_product/LEG.jpg", "pics_product/LGN.jpg", "pics_product/LRW.jpg", "pics_product/M10.jpg", "pics_product/M11.jpg", "pics_product/M12.jpg", "pics_product/MBS.jpg", "pics_product/MIR.jpg", "pics_product/MMQ.jpg", "pics_product/MOR.jpg", "pics_product/MRD.jpg", "pics_product/NMS.jpg", "pics_product/NPH.jpg", "pics_product/ODY.jpg", "pics_product/ONS.jpg", "pics_product/PCY.jpg", "pics_product/PLC.jpg", "pics_product/PLS.jpg", "pics_product/PO2.jpg", "pics_product/POR.jpg", "pics_product/PTK.jpg", "pics_product/RAV.jpg", "pics_product/ROE.jpg", "pics_product/S99.jpg", "pics_product/SCG.jpg", "pics_product/SHM.jpg", "pics_product/SOK.jpg", "pics_product/SOM.jpg", "pics_product/STH.jpg", "pics_product/TMP.jpg", "pics_product/TOR.jpg", "pics_product/TSP.jpg", "pics_product/UDS.jpg", "pics_product/ULG.jpg", "pics_product/USG.jpg", "pics_product/VIS.jpg", "pics_product/WTH.jpg", "pics_product/WWK.jpg", "pics_product/ZEN.jpg", "pics_product/fatpacks/PLS.JPG", "preferences/.project", "preferences/editor.default.preferences", "preferences/main.properties")) {
                            new File("res", cruftFile).delete();
                        }
                        
                        // attempt to remove old directories and assemble a list of remaining files.
                        Deque<File> stack = new LinkedList<File>(resDirs);
                        Set<File> seenDirs = new HashSet<File>();
                        final List<File> remainingFiles = new LinkedList<File>();
                        while (!stack.isEmpty()) {
                            File cur = stack.peek();
                            if (profileDirs.contains(cur)) {
                                // don't touch active profile dirs
                                stack.pop();
                                continue;
                            }
                            
                            if (seenDirs.contains(cur)) {
                                boolean succeeded = stack.pop().delete();
                                System.out.println(String.format("attempting to remove old profile dir: %s (%s)",
                                        cur, succeeded ? "succeeded" : "failed"));
                                continue;
                            }
                            
                            seenDirs.add(cur);
                            File[] curListing = cur.listFiles();
                            if (null == curListing) {
                                continue;
                            }
                            for (File f : curListing) {
                                if (f.isDirectory()) {
                                    stack.push(f);
                                } else {
                                    remainingFiles.add(f);
                                }
                            }
                        }
                        
                        // if any files remain, display them and make clear that they should be moved or
                        // deleted manually or the user will continue to be prompted for migration
                        FPanel p = new FPanel(new MigLayout("insets dialog, gap 10, center, wrap"));
                        p.setOpaque(false);
                        p.setBackgroundTexture(FSkin.getIcon(FSkin.Backgrounds.BG_TEXTURE));

                        if (remainingFiles.isEmpty()) {
                            p.add(new FLabel.Builder().text("<html>You're done!  It looks like everything went smoothly." +
                            		"  Now just restart Forge to load the data from its new home!</html>").build());
                        } else {
                            p.add(new FLabel.Builder().text("<html>There seem to be a few files left over in your old data" +
                            		" directories.  They should be deleted or moved somewhere else to avoid having this data" +
                            		" migration message pop up again!  If there are any empty directories left over after that," +
                            		" just run through this migration procedure one more time and that should get them cleared" +
                            		" up.</html>").build());
                            
                            JTextArea files = new JTextArea(StringUtils.join(remainingFiles, '\n'));
                            files.setFont(new Font("Monospaced", Font.PLAIN, 10));
                            files.setOpaque(false);
                            files.setWrapStyleWord(true);
                            files.setLineWrap(true);
                            files.setEditable(false);
                            JScrollPane scroller = new JScrollPane(files);
                            p.add(scroller, "w 400:100%:100%, h 60:100%:100%");
                        }
                        
                        final FButton btnOk = new FButton(remainingFiles.isEmpty() ? "Restart Forge" : "Close Forge");
                        btnOk.addActionListener(new ActionListener() {
                            @Override public void actionPerformed(ActionEvent e) {
                                if (remainingFiles.isEmpty()) {
                                    RestartUtil.restartApplication(null);
                                } else {
                                    System.exit(0);
                                }
                            }
                        });
                        p.add(btnOk, "center, w 40%, h pref+12!");

                        JPanel overlay = FOverlay.SINGLETON_INSTANCE.getPanel();
                        overlay.setLayout(new MigLayout("insets 0, gap 0, wrap, ax center, ay center"));
                        overlay.add(p, "w 800::80%, h 500::90%");
                        SOverlayUtils.showOverlay();
                        
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override public void run() { btnOk.requestFocusInWindow(); }
                        });
                    }
                });
            }
        }
    }

    /** @return {@link forge.view.SplashFrame} */
    public SplashFrame getSplash() {
        return frmSplash;
    }

    /** @return {@link javax.swing.JFrame} */
    public JFrame getFrame() {
        return frmDocument;
    }

    /** @return {@link javax.swing.JLayeredPane} */
    public JLayeredPane getLpnDocument() {
        return lpnDocument;
    }

    /** @return {@link forge.gui.toolbox.FPanel} */
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

    /** @return {@link forge.view.ViewBazaarUI} */
    public ViewBazaarUI getViewBazaar() {
        if (Singletons.getControl().getState() != FControl.Screens.QUEST_BAZAAR) {
            throw new IllegalArgumentException("FView$getViewBazaar\n"
                    + "may only be called while the bazaar UI is showing.");
        }
        bazaar.refreshLastInstance();
        return FView.this.bazaar;
    }

    /** */
    private void cacheUIStates() {
        FView.this.bazaar = new ViewBazaarUI(Singletons.getModel().getQuest().getBazaar());
        VMatchUI.SINGLETON_INSTANCE.instantiate();
        VHomeUI.SINGLETON_INSTANCE.instantiate();
        VDeckEditorUI.SINGLETON_INSTANCE.instantiate();
    }
}
