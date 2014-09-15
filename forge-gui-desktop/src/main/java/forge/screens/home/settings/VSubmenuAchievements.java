package forge.screens.home.settings;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import forge.achievement.Achievement;
import forge.achievement.AchievementCollection;
import forge.assets.FSkinProp;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.toolbox.*;
import forge.toolbox.FComboBox.TextAlignment;
import forge.toolbox.FSkin.SkinImage;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/**
 * Assembles Swing components of achievements submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VSubmenuAchievements implements IVSubmenu<CSubmenuAchievements> {
    /** */
    SINGLETON_INSTANCE;

    private static final int MIN_SHELVES = 3;
    private static final int TROPHIES_PER_SHELVE = 4;
    private static final int IMAGE_SCALE = 3;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Achievements");
    private final FLabel lblTitle = new FLabel.Builder()
        .text("Achievements").fontAlign(SwingConstants.CENTER)
        .opaque(true).fontSize(16).build();
    private final FComboBox<AchievementCollection> cbCollections = new FComboBox<AchievementCollection>();
    private final TrophyCase trophyCase = new TrophyCase();
    private final FScrollPane scroller = new FScrollPane(trophyCase, false,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    private VSubmenuAchievements() {
        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        trophyCase.setMinimumSize(new Dimension(266 * IMAGE_SCALE, 0));

        AchievementCollection.buildComboBox(cbCollections);

        cbCollections.setSkinFont(FSkin.getBoldFont(14));
        cbCollections.setTextAlignment(TextAlignment.CENTER);
        cbCollections.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setAchievements((AchievementCollection)cbCollections.getSelectedItem());
            }
        });
        cbCollections.setSelectedIndex(0);
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();

        String width = "w " + (trophyCase.getMinimumSize().width + 20) + "px!";
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 10, gap 10, wrap"));
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "pushx, growx, h 30px!");

        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new MigLayout("insets 0, gap 5, wrap, align center"));
        panel.add(cbCollections, width + ", h 30px!");
        panel.add(scroller, width + ", pushy, growy");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(panel, "push, grow");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.SETTINGS;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return "Achievements";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_ACHIEVEMENTS;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_ACHIEVEMENTS;
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
    public CSubmenuAchievements getLayoutControl() {
        return CSubmenuAchievements.SINGLETON_INSTANCE;
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

    private void setAchievements(AchievementCollection achievements0) {
        trophyCase.achievements = achievements0;

        trophyCase.shelfCount = achievements0.getCount() % TROPHIES_PER_SHELVE;
        if (trophyCase.shelfCount < MIN_SHELVES) {
            trophyCase.shelfCount = MIN_SHELVES;
        }

        trophyCase.setMinimumSize(new Dimension(trophyCase.getMinimumSize().width, IMAGE_SCALE * (14 + trophyCase.shelfCount * 86)));
        trophyCase.setPreferredSize(trophyCase.getMinimumSize());
        scroller.revalidate();
        scroller.repaint();
    }

    @SuppressWarnings("serial")
    private static class TrophyCase extends JPanel {
        private static final SkinImage imgTop = FSkin.getImage(FSkinProp.BG_TROPHY_CASE_TOP).scale(IMAGE_SCALE);
        private static final SkinImage imgShelf = FSkin.getImage(FSkinProp.BG_TROPHY_CASE_SHELF).scale(IMAGE_SCALE);
        private static final SkinImage imgBronzeTrophy = FSkin.getImage(FSkinProp.IMG_BRONZE_TROPHY).scale(1.8);
        private static final SkinImage imgSilverTrophy = FSkin.getImage(FSkinProp.IMG_SILVER_TROPHY).scale(1.8);
        private static final SkinImage imgGoldTrophy = FSkin.getImage(FSkinProp.IMG_GOLD_TROPHY).scale(1.8);
        private static final SkinImage imgTrophyPlate = FSkin.getImage(FSkinProp.IMG_TROPHY_PLATE);
        private static final Font font = FSkin.getFixedFont(14).deriveFont(Font.BOLD);
        private static final Font subFont = FSkin.getFixedFont(12);
        private static final Color foreColor = new Color(239, 220, 144);

        private AchievementCollection achievements;
        private int shelfCount;

        @Override
        public void paintComponent(final Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();

            Dimension imgTopSize = imgTop.getSizeForPaint(g2d);
            RenderingHints hints = new RenderingHints(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHints(hints);

            int x = 0;
            int y = 0;
            int w = imgTopSize.width;
            int h = imgTopSize.height;

            FSkin.drawImage(g2d, imgTop, x, y, w, h);
            y += h;

            Dimension imgShelfSize = imgShelf.getSizeForPaint(g2d);
            h = imgShelfSize.height;
            for (int i = 0; i < shelfCount; i++) {
                FSkin.drawImage(g2d, imgShelf, x, y, w, h);
                y += h;
            }

            Dimension trophySize = imgBronzeTrophy.getSizeForPaint(g2d);
            Dimension trophyPlateSize = imgTrophyPlate.getSizeForPaint(g2d);

            x += (w - TROPHIES_PER_SHELVE * trophySize.width) / 2;
            y = imgTopSize.height + (h - trophySize.height - 12 * IMAGE_SCALE) / 2;

            FontMetrics fm;
            String label;
            int trophyCount = 0;
            int startX = x;
            int plateY = imgTopSize.height + imgShelfSize.height - trophyPlateSize.height;
            int textY;
            int dy = h;
            w = trophySize.width;
            h = trophySize.height;
            int plateOffset = (w - trophyPlateSize.width) / 2;

            for (Achievement achievement : achievements) {
                if (trophyCount == TROPHIES_PER_SHELVE) {
                    trophyCount = 0;
                    y += dy;
                    plateY += dy;
                    x = startX;
                }
                if (achievement.earnedGold()) {
                    FSkin.drawImage(g2d, imgGoldTrophy, x, y, w, h);
                }
                else if (achievement.earnedSilver()) {
                    FSkin.drawImage(g2d, imgSilverTrophy, x, y, w, h);
                }
                else if (achievement.earnedBronze()) {
                    FSkin.drawImage(g2d, imgBronzeTrophy, x, y, w, h);
                }
                FSkin.drawImage(g2d, imgTrophyPlate, x + plateOffset, plateY, trophyPlateSize.width, trophyPlateSize.height);

                g2d.setColor(foreColor);
                g2d.setFont(font);

                fm = g2d.getFontMetrics();
                label = achievement.getDisplayName();
                textY = plateY + (trophyPlateSize.height * 2 / 3 - fm.getHeight()) / 2 + fm.getAscent();
                g2d.drawString(label, x + plateOffset + (trophyPlateSize.width - fm.stringWidth(label)) / 2, textY);

                label = achievement.getSubTitle();
                if (label != null) {
                    textY += fm.getAscent();
                    g2d.setFont(subFont);
                    fm = g2d.getFontMetrics();
                    g2d.drawString(label, x + plateOffset + (trophyPlateSize.width - fm.stringWidth(label)) / 2, textY);
                }

                trophyCount++;
                x += w;
            }

            g2d.dispose();
        }
    }
}
