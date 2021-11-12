package forge.screens.home.settings;

import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import forge.ImageKeys;
import forge.StaticData;
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.CardEdition.CardInSet;
import forge.gui.SOverlayUtils;
import forge.gui.UiCommand;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.item.PaperCard;
import forge.item.PaperToken;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.skin.FSkinProp;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.token.TokenDb;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FOverlay;
import forge.toolbox.FPanel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FTextArea;
import forge.util.FileUtil;
import forge.util.ImageUtil;
import forge.util.Localizer;
import forge.util.RuntimeVersion;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Assembles Swing components of utilities submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuDownloaders implements IVSubmenu<CSubmenuDownloaders> {
    /** */
    SINGLETON_INSTANCE;
    final Localizer localizer = Localizer.getInstance();


    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Utilities");

    /** */
    private final JPanel pnlContent = new JPanel(new MigLayout("insets 0, gap 0, wrap, ay center"));
    private final FScrollPane scrContent = new FScrollPane(pnlContent, false);

    private final FLabel btnCheckForUpdates           = _makeButton(localizer.getMessage("btnCheckForUpdates"));
    private final FLabel btnDownloadSetPics           = _makeButton(localizer.getMessage("btnDownloadSetPics"));
    private final FLabel btnDownloadPics              = _makeButton(localizer.getMessage("btnDownloadPics"));
    private final FLabel btnDownloadPicsHQ            = _makeButton(localizer.getMessage("btnDownloadPicsHQ"));
    private final FLabel btnDownloadQuestImages       = _makeButton(localizer.getMessage("btnDownloadQuestImages"));
    private final FLabel btnDownloadAchievementImages = _makeButton(localizer.getMessage("btnDownloadAchievementImages"));
    private final FLabel btnReportBug                 = _makeButton(localizer.getMessage("btnReportBug"));
    private final FLabel btnListImageData             = _makeButton(localizer.getMessage("btnListImageData"));
    private final FLabel btnImportPictures            = _makeButton(localizer.getMessage("btnImportPictures"));
    private final FLabel btnHowToPlay                 = _makeButton(localizer.getMessage("btnHowToPlay"));
    private final FLabel btnDownloadPrices            = _makeButton(localizer.getMessage("btnDownloadPrices"));
    private final FLabel btnDownloadSkins             = _makeButton(localizer.getMessage("btnDownloadSkins"));
    private final FLabel btnLicensing                 = _makeButton(localizer.getMessage("btnLicensing"));

    /**
     * Constructor.
     */
    VSubmenuDownloaders() {
        final Localizer localizer = Localizer.getInstance();

        final String constraintsLBL = "w 90%!, h 20px!, center, gap 0 0 3px 8px";
        final String constraintsBTN = "h 30px!, w 50%!, center";

        pnlContent.setOpaque(false);

        if (javaRecentEnough()) {
            // With Blacksmith we would upload the releases and the /latest would redirect to the right URL
            // That currently doesn't happen so lets comment out this button for now
//            pnlContent.add(btnCheckForUpdates, constraintsBTN);
//            pnlContent.add(_makeLabel(localizer.getMessage("lblCheckForUpdates")), constraintsLBL);

            pnlContent.add(btnDownloadPics, constraintsBTN);
            pnlContent.add(_makeLabel(localizer.getMessage("lblDownloadPics")), constraintsLBL);

            pnlContent.add(btnDownloadPicsHQ, constraintsBTN);
            pnlContent.add(_makeLabel(localizer.getMessage("lblDownloadPicsHQ")), constraintsLBL);

            pnlContent.add(btnDownloadSetPics, constraintsBTN);
            pnlContent.add(_makeLabel(localizer.getMessage("lblDownloadSetPics")), constraintsLBL);

            pnlContent.add(btnDownloadQuestImages, constraintsBTN);
            pnlContent.add(_makeLabel(localizer.getMessage("lblDownloadQuestImages")), constraintsLBL);

            pnlContent.add(btnDownloadAchievementImages, constraintsBTN);
            pnlContent.add(_makeLabel(localizer.getMessage("lblDownloadAchievementImages")), constraintsLBL);

            pnlContent.add(btnDownloadPrices, constraintsBTN);
            pnlContent.add(_makeLabel(localizer.getMessage("lblDownloadPrices")), constraintsLBL);

            pnlContent.add(btnDownloadSkins, constraintsBTN);
            pnlContent.add(_makeLabel(localizer.getMessage("lblDownloadSkins")), constraintsLBL);
        } else {
            String text = localizer.getMessage("lblYourVersionOfJavaIsTooOld");
            FLabel label = new FLabel.Builder().fontAlign(SwingConstants.CENTER).text(text).fontStyle(Font.BOLD).fontSize(18).build();
            pnlContent.add(label, "w 90%!, h 25px!, center, gap 0 0 30px 3px");

            text  = localizer.getMessage("lblPleaseUpdateToTheLatestVersionOfJava");
            label = new FLabel.Builder().fontAlign(SwingConstants.CENTER).text(text).fontStyle(Font.BOLD).fontSize(18).build();
            pnlContent.add(label, "w 90%!, h 25px!, center, gap 0 0 0 36px");

            text = localizer.getMessage("lblYoureRunning") + " " + System.getProperty("java.version");
            text = text + " . " + localizer.getMessage("lblYouNeedAtLeastJavaVersion") ;
            label = new FLabel.Builder().fontAlign(SwingConstants.CENTER).text(text).fontStyle(Font.BOLD).fontSize(18).build();
            pnlContent.add(label, "w 90%!, h 25px!, center, gap 0 0 0 36px");
        }

        pnlContent.add(btnListImageData, constraintsBTN);
        pnlContent.add(_makeLabel(localizer.getMessage("lblListImageData")), constraintsLBL);

        pnlContent.add(btnImportPictures, constraintsBTN);
        pnlContent.add(_makeLabel(localizer.getMessage("lblImportPictures")), constraintsLBL);

        pnlContent.add(btnReportBug, constraintsBTN);
        pnlContent.add(_makeLabel(localizer.getMessage("lblReportBug")), constraintsLBL);

        pnlContent.add(btnHowToPlay, constraintsBTN);
        pnlContent.add(_makeLabel(localizer.getMessage("lblHowToPlay")), constraintsLBL);

        pnlContent.add(btnLicensing, constraintsBTN);
        pnlContent.add(_makeLabel(localizer.getMessage("lblLicensing")), constraintsLBL);
    }

    private boolean javaRecentEnough() {
        RuntimeVersion javaVersion = RuntimeVersion.of(System.getProperty("java.version"));

        return javaVersion.getMajor() >= 9 || (javaVersion.getMajor() >= 1 && (javaVersion.getMinor() > 8 || (javaVersion.getMinor() == 8 && javaVersion.getUpdate() >= 101)));
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0"));
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(scrContent, "w 98%!, h 98%!, gap 1% 0 1% 0");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.SETTINGS;
    }

    public void setCheckForUpdatesCommand(UiCommand command)           { btnCheckForUpdates.setCommand(command);     }
    public void setDownloadPicsCommand(UiCommand command)              { btnDownloadPics.setCommand(command);        }
    public void setDownloadPicsHQCommand(UiCommand command)            { btnDownloadPicsHQ.setCommand(command);      }
    public void setDownloadSetPicsCommand(UiCommand command)           { btnDownloadSetPics.setCommand(command);     }
    public void setDownloadQuestImagesCommand(UiCommand command)       { btnDownloadQuestImages.setCommand(command); }
    public void setDownloadAchievementImagesCommand(UiCommand command) { btnDownloadAchievementImages.setCommand(command); }
    public void setReportBugCommand(UiCommand command)                 { btnReportBug.setCommand(command);           }
    public void setListImageDataCommand(UiCommand command)             { btnListImageData.setCommand(command);       }
    public void setImportPicturesCommand(UiCommand command)            { btnImportPictures.setCommand(command);      }
    public void setHowToPlayCommand(UiCommand command)                 { btnHowToPlay.setCommand(command);           }
    public void setDownloadPricesCommand(UiCommand command)            { btnDownloadPrices.setCommand(command);      }
    public void setLicensingCommand(UiCommand command)                 { btnLicensing.setCommand(command);           }
    public void setDownloadSkinsCommand(UiCommand command)             { btnDownloadSkins.setCommand(command);       }

    public void focusTopButton() {
        btnDownloadPics.requestFocusInWindow();
    }

    private void _showDialog(Component c, final Runnable onShow) {
        JPanel overlay = FOverlay.SINGLETON_INSTANCE.getPanel();
        overlay.setLayout(new MigLayout("insets 0, gap 0, ax center, ay center"));

        FPanel p = new FPanel(new MigLayout("insets dialog, wrap, center"));
        p.setOpaque(false);
        p.setBackgroundTexture(FSkin.getIcon(FSkinProp.BG_TEXTURE));

        final FButton btnClose = new FButton(localizer.getMessage("lblOK"));
        btnClose.addActionListener(new ActionListener() { @Override
            public void actionPerformed(final ActionEvent arg0) { SOverlayUtils.hideOverlay(); } });

        p.add(c, "w 500!");
        p.add(btnClose, "w 200!, h pref+12!, center, gaptop 30");
        overlay.add(p, "gap 0 0 10% 10%");
        SOverlayUtils.showOverlay();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (null != onShow) {
                    onShow.run();
                }
                btnClose.requestFocusInWindow();
            }
        });
    }
    
    /**
     * Loops through the editions and card databases, looking for missing images and unimplemented cards.
     * 
     * @param tar - Text area to report info
     * @param scr
     */
    public void auditUpdate(FTextArea tar, FScrollPane scr) {
        // Get top-level Forge objects
        CardDb cardDb = StaticData.instance().getCommonCards();
        CardDb variantDb = StaticData.instance().getVariantCards();
        TokenDb tokenDb = StaticData.instance().getAllTokens();
        CardEdition.Collection editions = StaticData.instance().getEditions();

        int missingCount = 0;
        int notImplementedCount = 0;

        final StringBuffer nifSB = new StringBuffer(); // NO IMAGE FOUND BUFFER
        final StringBuffer cniSB = new StringBuffer(); // CARD NOT IMPLEMENTED BUFFER
        
        nifSB.append("\n\n-------------------\n");
        nifSB.append("NO IMAGE FOUND LIST\n");
        nifSB.append("-------------------\n\n");
        
        cniSB.append("\n\n-------------------\n");
        cniSB.append("UNIMPLEMENTED CARD LIST\n");
        cniSB.append("-------------------\n\n");

        for (CardEdition e : editions) {
            if (CardEdition.Type.FUNNY.equals(e.getType()))
                continue;
            boolean nifHeader = false;
            boolean cniHeader = false;
            boolean tokenHeader = false;

            String imagePath;
            int artIndex = 1;

            HashMap<String, Pair<Boolean, Integer>> cardCount = new HashMap<>();
            for (CardInSet c : e.getAllCardsInSet()) {
                if (cardCount.containsKey(c.name)) {
                    cardCount.put(c.name, Pair.of(c.collectorNumber.startsWith("F"), cardCount.get(c.name).getRight() + 1));
                } else {
                    cardCount.put(c.name, Pair.of(c.collectorNumber.startsWith("F"), 1));
                }
            }
            
            // loop through the cards in this edition, considering art variations...
            for (Entry<String, Pair<Boolean, Integer>> entry : cardCount.entrySet()) {
                String c = entry.getKey();
                artIndex = entry.getValue().getRight();

                PaperCard cp = cardDb.getCard(c, e.getCode(), artIndex);
                if (cp == null) {
                    cp = variantDb.getCard(c, e.getCode(), artIndex);
                }

                if (cp == null) {
                    if (entry.getValue().getLeft()) //skip funny cards
                        continue;
                    if (!cniHeader) {
                        cniSB.append("Edition: ").append(e.getName()).append(" ").append("(").append(e.getCode()).append("/").append(e.getCode2()).append(")\n");
                        cniHeader = true;
                    }
                    cniSB.append(" ").append(c).append("\n");
                    notImplementedCount++;
                    continue;
                }

                // check the front image
                imagePath = ImageUtil.getImageRelativePath(cp, false, true, false);
                if (imagePath != null) {
                    File file = ImageKeys.getImageFile(imagePath);
                    if (file == null) {
                        if (!nifHeader) {
                            nifSB.append("Edition: ").append(e.getName()).append(" ").append("(").append(e.getCode()).append("/").append(e.getCode2()).append(")\n");
                            nifHeader = true;
                        }
                        nifSB.append(" ").append(imagePath).append("\n");
                        missingCount++;
                    }
                } 

                // check the back face
                if (cp.hasBackFace()) {
                    imagePath = ImageUtil.getImageRelativePath(cp, true, true, false);
                    if (imagePath != null) {
                        File file = ImageKeys.getImageFile(imagePath);
                        if (file == null) {
                            if (!nifHeader) {
                                nifSB.append("Edition: ").append(e.getName()).append(" ").append("(").append(e.getCode()).append("/").append(e.getCode2()).append(")\n");
                                nifHeader = true;
                            }
                            nifSB.append(" ").append(imagePath).append("\n");
                            missingCount++;
                        }
                    } 
                }
            }

            // TODO: Audit token images here...
            for(Entry<String, Integer> tokenEntry : e.getTokens().entrySet()) {
                String name = tokenEntry.getKey();
                artIndex = tokenEntry.getValue();
                try {
                    PaperToken token = tokenDb.getToken(name, e.getCode());
                    if (token == null) {
                        continue;
                    }

                    for(int i = 0; i < artIndex; i++) {
                        String imgKey = token.getImageKey(i);
                        File file = ImageKeys.getImageFile(imgKey);
                        if (file == null) {
                            if (!nifHeader) {
                                nifSB.append("Edition: ").append(e.getName()).append(" ").append("(").append(e.getCode()).append("/").append(e.getCode2()).append(")\n");
                                nifHeader = true;
                            }
                            if (!tokenHeader) {
                                nifSB.append("\nTOKENS\n");
                                tokenHeader = true;
                            }
                            nifSB.append(" ").append(token.getImageFilename(i + 1)).append("\n");
                            missingCount++;
                        }
                    }
                } catch(Exception ex) {
                    System.out.println("No Token found: " + name + " in " + e.getName());
                }
            }
            if (nifHeader)
                nifSB.append("\n");
        }

        String totalStats = "Missing images: " + missingCount + "\nUnimplemented cards: " + notImplementedCount + "\n";
        cniSB.append("\n-----------\n");
        cniSB.append(totalStats);
        cniSB.append("-----------\n\n");
        
        nifSB.append(cniSB); // combine things together...

        tar.setText(nifSB.toString());
        tar.setCaretPosition(0); // this will move scroll view to the top...
        
        final FButton btnClipboardCopy = new FButton(localizer.getMessage("btnCopyToClipboard"));
        btnClipboardCopy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(nifSB.toString()), null);
                SOverlayUtils.hideOverlay();
            }
        });
        scr.getParent().add(btnClipboardCopy, "w 200!, h pref+12!, center, gaptop 10");
        
        String labelText = "<html>Missing images: " + missingCount + "<br>Unimplemented cards: " + notImplementedCount + "<br>";
        final FLabel statsLabel = new FLabel.Builder().text(labelText).fontSize(15).build();
        scr.getParent().add(statsLabel);

        FOverlay.SINGLETON_INSTANCE.getPanel().validate();
        FOverlay.SINGLETON_INSTANCE.getPanel().repaint();
    }

    public void showCardandImageAuditData() {
        final FTextArea tar = new FTextArea("Auditing card and image data. Please wait...");
        tar.setOpaque(true);
        tar.setLineWrap(false);
        tar.setWrapStyleWord(false);
        tar.setEditable(false);
        tar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        tar.setFont(FSkin.getRelativeFixedFont(12));
        tar.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        tar.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        final FScrollPane scr = new FScrollPane(tar, true, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

        _showDialog(scr, new Runnable() {
            @Override
            public void run() {
                auditUpdate(tar, scr);
                scr.getViewport().setViewPosition(new Point(0, 0));
            }
        });
    }

    public void showLicensing() {
        String license = "<html>Forge License Information<br><br>"
                + "This program is free software : you can redistribute it and/or modify "
                + "it under the terms of the GNU General Public License as published by "
                + "the Free Software Foundation, either version 3 of the License, or "
                + "(at your option) any later version.<br><br>"
                + "This program is distributed in the hope that it will be useful, "
                + "but WITHOUT ANY WARRANTY; without even the implied warranty of "
                + "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the "
                + "GNU General Public License for more details.<br><br>"
                + "You should have received a copy of the GNU General Public License "
                + "along with this program.  If not, see http://www.gnu.org/licenses/.</html>";

        FLabel licenseLabel = new FLabel.Builder().text(license).fontSize(15).build();

        _showDialog(licenseLabel, null);
    }

    public void showHowToPlay() {
        FTextArea directions = new FTextArea(FileUtil.readFileToString(ForgeConstants.HOWTO_FILE));
        final FScrollPane scr = new FScrollPane(directions, false, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        _showDialog(scr, new Runnable() {
            @Override public void run() { scr.getViewport().setViewPosition(new Point(0, 0)); }
        });
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return localizer.getMessage("ContentDownloaders");
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_UTILITIES;
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_UTILITIES;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CSubmenuDownloaders getLayoutControl() {
        return CSubmenuDownloaders.SINGLETON_INSTANCE;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell(forge.gui.framework.DragCell)
     */
    @Override
    public void setParentCell(DragCell cell0) {
        this.parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return parentCell;
    }

    private static FLabel _makeButton(String label) {
        return new FLabel.Builder().opaque().hoverable().text(label).fontSize(14).build();
    }

    private static FLabel _makeLabel(String label) {
        return new FLabel.Builder().fontAlign(SwingConstants.CENTER)
                .text(label).fontStyle(Font.ITALIC).build();
    }

}
