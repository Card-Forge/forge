package forge.screens.home.settings;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import forge.achievement.Achievement;
import forge.achievement.AchievementCollection;
import forge.assets.FSkinProp;
import forge.game.card.CardView;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.item.IPaperCard;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.toolbox.*;
import forge.toolbox.FComboBox.TextAlignment;
import forge.toolbox.FSkin.Colors;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FSkin.SkinFont;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.special.CardZoomer;
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
    private static final int PADDING = 5;
    private static final int TROPHY_PADDING = 45;
    private static final SkinFont NAME_FONT = FSkin.getBoldFont(14);
    private static final SkinFont DESC_FONT = FSkin.getFont(12);
    private static final SkinColor TEXT_COLOR = FSkin.getColor(Colors.CLR_TEXT);
    private static final SkinColor NOT_EARNED_COLOR = TEXT_COLOR.alphaColor(128);
    private static final SkinColor TEXTURE_OVERLAY_COLOR = FSkin.getColor(Colors.CLR_THEME);
    private static final SkinColor BORDER_COLOR = FSkin.getColor(Colors.CLR_BORDERS);

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

        trophyCase.setMinimumSize(new Dimension(FSkinProp.IMG_TROPHY_SHELF.getWidth(), 0));
        trophyCase.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseMoved(MouseEvent e) {
                trophyCase.setSelectedAchievement(getAchievementAt(e.getX(), e.getY()));
            }

            @Override
            public void mouseDragged(MouseEvent e) {
            }
        });
        trophyCase.addMouseListener(new FMouseAdapter() {
            private boolean preventMouseOut;

            @Override
            public void onMiddleMouseDown(MouseEvent e) {
                showCard(e);
            }

            @Override
            public void onMiddleMouseUp(MouseEvent e) {
                CardZoomer.SINGLETON_INSTANCE.closeZoomer();
            }

            @Override
            public void onLeftDoubleClick(MouseEvent e) {
                showCard(e);
            }

            private void showCard(MouseEvent e) {
                final Achievement achievement = getAchievementAt(e.getX(), e.getY());
                if (achievement != null) {
                    final IPaperCard pc = achievement.getPaperCard();
                    if (pc != null) {
                        preventMouseOut = true;
                        CardZoomer.SINGLETON_INSTANCE.setCard(CardView.getCardForUi(pc).getCurrentState(), true);
                    }
                }
            }

            @Override
            public void onMouseExit(MouseEvent e) {
                if (preventMouseOut) {
                    preventMouseOut = false;
                    return;
                }
                trophyCase.setSelectedAchievement(null);
            }
        });

        AchievementCollection.buildComboBox(cbCollections);

        cbCollections.setSkinFont(FSkin.getBoldFont(14));
        cbCollections.setTextAlignment(TextAlignment.CENTER);
        cbCollections.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setAchievements(cbCollections.getSelectedItem());
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

        trophyCase.shelfCount = (int)Math.ceil((double)achievements0.getCount() / (double)TROPHIES_PER_SHELVE);
        if (trophyCase.shelfCount < MIN_SHELVES) {
            trophyCase.shelfCount = MIN_SHELVES;
        }

        trophyCase.setMinimumSize(new Dimension(trophyCase.getMinimumSize().width, (FSkinProp.IMG_TROPHY_CASE_TOP.getHeight() + trophyCase.shelfCount * FSkinProp.IMG_TROPHY_SHELF.getHeight())));
        trophyCase.setPreferredSize(trophyCase.getMinimumSize());
        scroller.revalidate();
        scroller.repaint();
    }

    private Achievement getAchievementAt(float x0, float y0) {
        float w = scroller.getWidth();
        float shelfHeight = FSkinProp.IMG_TROPHY_SHELF.getHeight();
        float trophyWidth = FSkinProp.IMG_COMMON_TROPHY.getWidth() + TROPHY_PADDING;
        float x = (w - TROPHIES_PER_SHELVE * trophyWidth) / 2;
        float y = FSkinProp.IMG_TROPHY_CASE_TOP.getHeight();

        int trophyCount = 0;
        float startX = x;

        for (Achievement achievement : trophyCase.achievements) {
            if (trophyCount == TROPHIES_PER_SHELVE) {
                trophyCount = 0;
                x = startX;
                y += shelfHeight;
            }

            if (x <= x0 && x0 < x + trophyWidth && y <= y0 && y0 < y + shelfHeight) {
                return achievement;
            }

            trophyCount++;
            x += trophyWidth;
        }
        return null;
    }

    @SuppressWarnings("serial")
    private static class TrophyCase extends JPanel {
        private static final SkinImage imgTop = FSkin.getImage(FSkinProp.IMG_TROPHY_CASE_TOP);
        private static final SkinImage imgShelf = FSkin.getImage(FSkinProp.IMG_TROPHY_SHELF);
        private static final SkinImage imgTrophyPlate = FSkin.getImage(FSkinProp.IMG_TROPHY_PLATE);
        private static final Font font = FSkin.getFixedFont(14).deriveFont(Font.BOLD);
        private static final Font subFont = FSkin.getFixedFont(12);
        private static final Color foreColor = new Color(239, 220, 144);

        private AchievementCollection achievements;
        private int shelfCount;
        private Achievement selectedAchievement;

        private void setSelectedAchievement(Achievement selectedAchievement0) {
            if (selectedAchievement == selectedAchievement0) { return; }
            selectedAchievement = selectedAchievement0;
            repaint();
        }

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

            int trophyImageWidth = FSkinProp.IMG_COMMON_TROPHY.getWidth();
            int trophyWidth = trophyImageWidth + TROPHY_PADDING;
            int trophyHeight = FSkinProp.IMG_COMMON_TROPHY.getHeight();
            Dimension trophyPlateSize = imgTrophyPlate.getSizeForPaint(g2d);
            int plateY = imgTopSize.height + h - trophyPlateSize.height;

            x += (w - TROPHIES_PER_SHELVE * trophyWidth) / 2;
            y = plateY - trophyHeight;

            FontMetrics fm;
            String label;
            int trophyCount = 0;
            int startX = x;
            int textY;
            int dy = h;
            w = trophyWidth;
            h = trophyHeight;
            int plateOffset = (w - trophyPlateSize.width) / 2;
            Rectangle selectRect = null;

            for (Achievement achievement : achievements) {
                if (trophyCount == TROPHIES_PER_SHELVE) {
                    trophyCount = 0;
                    y += dy;
                    plateY += dy;
                    x = startX;
                }
                FSkin.drawImage(g2d, (SkinImage)achievement.getImage(), x + TROPHY_PADDING / 2, y, trophyImageWidth, h);
                FSkin.drawImage(g2d, imgTrophyPlate, x + plateOffset, plateY, trophyPlateSize.width, trophyPlateSize.height);

                g2d.setColor(foreColor);
                g2d.setFont(font);

                fm = g2d.getFontMetrics();
                label = achievement.getDisplayName();
                textY = plateY + (trophyPlateSize.height * 2 / 3 - fm.getHeight()) / 2 + fm.getAscent();
                g2d.drawString(label, x + plateOffset + (trophyPlateSize.width - fm.stringWidth(label)) / 2, textY);

                label = achievement.getSubTitle(false);
                if (label != null) {
                    textY += fm.getAscent();
                    g2d.setFont(subFont);
                    fm = g2d.getFontMetrics();
                    g2d.drawString(label, x + plateOffset + (trophyPlateSize.width - fm.stringWidth(label)) / 2, textY);
                }

                if (achievement == selectedAchievement) {
                    g2d.setColor(Color.GREEN);
                    int arcSize = w / 10;
                    int selY = y - imgShelfSize.height + trophyHeight + trophyPlateSize.height;
                    g2d.drawRoundRect(x, selY, w, imgShelfSize.height, arcSize, arcSize);
                    selectRect = new Rectangle(x, selY, w, imgShelfSize.height);
                }

                trophyCount++;
                x += w;
            }

            //draw tooltip for selected achievement if needed
            if (selectRect != null) {
                String subTitle = selectedAchievement.getSubTitle(true);
                String sharedDesc = selectedAchievement.getSharedDesc();
                String mythicDesc = selectedAchievement.getMythicDesc();
                String rareDesc = selectedAchievement.getRareDesc();
                String uncommonDesc = selectedAchievement.getUncommonDesc();
                String commonDesc = selectedAchievement.getCommonDesc();

                int nameHeight = NAME_FONT.getFontMetrics().getHeight();
                int descHeight = DESC_FONT.getFontMetrics().getHeight();

                w = imgShelfSize.width - 2 * PADDING - 1;
                h = nameHeight + PADDING * 5 / 2;
                if (subTitle != null) {
                    h += descHeight;
                }
                if (sharedDesc != null) {
                    h += descHeight;
                }
                if (mythicDesc != null) {
                    h += descHeight;
                }
                if (rareDesc != null) {
                    h += descHeight;
                }
                if (uncommonDesc != null) {
                    h += descHeight;
                }
                if (commonDesc != null) {
                    h += descHeight;
                }

                x = PADDING;
                y = selectRect.y + selectRect.height + PADDING;
                FScrollPane scroller = (FScrollPane)getParent().getParent();
                if (y + h - scroller.getVerticalScrollBar().getValue() > scroller.getHeight()) {
                    if (selectRect.y - PADDING > h) {
                        y = selectRect.y - h - PADDING;
                    }
                    else {
                        y = getHeight() - h;
                    }
                }

                FSkin.drawImage(g2d, FSkin.getImage(FSkinProp.BG_TEXTURE), x, y, w, h);
                FSkin.setGraphicsColor(g2d, TEXTURE_OVERLAY_COLOR);
                g2d.fillRect(x, y, w, h);
                FSkin.setGraphicsColor(g2d, BORDER_COLOR);
                g2d.drawRect(x, y, w, h);

                x += PADDING;
                y += PADDING;
                w -= 2 * PADDING;
                h -= 2 * PADDING;

                FSkin.setGraphicsFont(g2d, NAME_FONT);
                FSkin.setGraphicsColor(g2d, TEXT_COLOR);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                fm = g2d.getFontMetrics();
                y += fm.getAscent();
                g2d.drawString(selectedAchievement.getDisplayName(), x, y);
                y += nameHeight;

                FSkin.setGraphicsFont(g2d, DESC_FONT);
                if (subTitle != null) {
                    g2d.drawString(subTitle, x, y);
                    y += descHeight;
                }
                y += PADDING;
                if (sharedDesc != null) {
                    g2d.drawString(selectedAchievement.isSpecial() ? sharedDesc : sharedDesc + "...", x, y);
                    y += descHeight;
                }
                if (mythicDesc != null) {
                    FSkin.setGraphicsColor(g2d, selectedAchievement.earnedMythic() ? TEXT_COLOR : NOT_EARNED_COLOR);
                    g2d.drawString(selectedAchievement.isSpecial() ? mythicDesc : "(Mythic) " + mythicDesc, x, y); //handle flavor text here too
                    y += descHeight;
                }
                if (rareDesc != null) {
                    FSkin.setGraphicsColor(g2d, selectedAchievement.earnedRare() ? TEXT_COLOR : NOT_EARNED_COLOR);
                    g2d.drawString("(Rare) " + rareDesc, x, y);
                    y += descHeight;
                }
                if (uncommonDesc != null) {
                    FSkin.setGraphicsColor(g2d, selectedAchievement.earnedUncommon() ? TEXT_COLOR : NOT_EARNED_COLOR);
                    g2d.drawString("(Uncommon) " + uncommonDesc, x, y);
                    y += descHeight;
                }
                if (commonDesc != null) {
                    FSkin.setGraphicsColor(g2d, selectedAchievement.earnedCommon() ? TEXT_COLOR : NOT_EARNED_COLOR);
                    g2d.drawString("(Common) " + commonDesc, x, y);
                }
            }

            g2d.dispose();
        }
    }
}
