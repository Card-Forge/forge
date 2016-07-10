package forge.screens.home.quest;

import forge.GuiBase;
import forge.Singletons;
import forge.assets.FSkinProp;
import forge.game.GameType;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.itemmanager.DeckManager;
import forge.limited.BoosterDraft;
import forge.model.FModel;
import forge.quest.IQuestTournamentView;
import forge.quest.QuestEventDraft;
import forge.quest.QuestDraftUtils.Mode;
import forge.quest.data.QuestEventDraftContainer;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorQuestDraftingProcess;
import forge.screens.deckeditor.controllers.CEditorQuestLimited;
import forge.screens.deckeditor.views.VCurrentDeck;
import forge.screens.home.*;
import forge.screens.match.controllers.CDetailPicture;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPanel;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.Colors;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.JXButtonPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

/**
 * Assembles Swing components of quest draft submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VSubmenuQuestDraft implements IVSubmenu<CSubmenuQuestDraft>, IQuestTournamentView {
    SINGLETON_INSTANCE;

    private final DragTab tab = new DragTab("Tournaments");

    private final LblHeader lblTitle = new LblHeader("Quest Mode: Draft Tournament");

    private final FLabel lblCredits = new FLabel.Builder()
        .icon(FSkin.getIcon(FSkinProp.ICO_QUEST_COINSTACK))
        .iconScaleFactor(0.75f)
        .fontSize(14).build();

    private final FScrollPanel pnlTournaments = new FScrollPanel(new MigLayout("insets 0, gap 0, wrap, ax center"), true,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    private final JLabel lblInfo = new FLabel.Builder().text("Select a tournament to join:")
        .fontStyle(Font.BOLD).fontSize(16)
        .fontAlign(SwingConstants.LEFT).build();

    private final JLabel lblNoDrafts = new FLabel.Builder().text("There are no tournaments available at this time.")
        .fontStyle(Font.PLAIN).fontSize(16)
        .fontAlign(SwingConstants.LEFT).build();

    private final JPanel pnlStats = new JPanel();
    private final FLabel lblPastResults = new FLabel.Builder()
    .text("Past Results:")
    .fontSize(19).build();
    private final FLabel lblFirst = new FLabel.Builder()
    .fontSize(15).build();
    private final FLabel lblSecond = new FLabel.Builder()
    .fontSize(15).build();
    private final FLabel lblThird = new FLabel.Builder()
    .fontSize(15).build();
    private final FLabel lblFourth = new FLabel.Builder()
    .fontSize(15).build();
    private final FLabel lblTokens = new FLabel.Builder()
    .fontSize(15).build();

    private final StartButton btnStartDraft  = new StartButton();
    private final StartButton btnStartTournament  = new StartButton();
    private final StartButton btnStartMatch  = new StartButton();

    private final FLabel btnEditDeck = new FLabel.ButtonBuilder().text("Edit Deck").fontSize(24).build();
    private final FLabel btnLeaveTournament = new FLabel.ButtonBuilder().text("Leave Tournament").fontSize(12).build();
    private final FLabel btnSpendToken = new FLabel.ButtonBuilder().text("Spend Token").fontSize(14).build();
    private final FLabel btnStartMatchSmall = new FLabel.ButtonBuilder().text("Start Next Match").fontSize(12).build();

    private final PnlMatchup[] matchups = new PnlMatchup[8];

    private final JPanel pnlDeckImage;

    private Mode mode = Mode.SELECT_TOURNAMENT;

    private DragCell parentCell;

    VSubmenuQuestDraft() {

        SkinImage avatar = FSkin.getAvatars().get(GuiBase.getInterface().getAvatarCount() - 1);

        matchups[0] = new PnlMatchup(PnlMatchup.LineDirection.DOWN, PnlMatchup.LineSide.RIGHT, PnlMatchup.BoxSize.SMALL);
        matchups[0].setPlayerOne("Undetermined", avatar);
        matchups[0].setPlayerTwo("Undetermined", avatar);

        matchups[1] = new PnlMatchup(PnlMatchup.LineDirection.UP, PnlMatchup.LineSide.RIGHT, PnlMatchup.BoxSize.SMALL);
        matchups[1].setPlayerOne("Undetermined", avatar);
        matchups[1].setPlayerTwo("Undetermined", avatar);

        matchups[2] = new PnlMatchup(PnlMatchup.LineDirection.DOWN, PnlMatchup.LineSide.RIGHT, PnlMatchup.BoxSize.SMALL);
        matchups[2].setPlayerOne("Undetermined", avatar);
        matchups[2].setPlayerTwo("Undetermined", avatar);

        matchups[3] = new PnlMatchup(PnlMatchup.LineDirection.UP, PnlMatchup.LineSide.RIGHT, PnlMatchup.BoxSize.SMALL);
        matchups[3].setPlayerOne("Undetermined", avatar);
        matchups[3].setPlayerTwo("Undetermined", avatar);

        matchups[4] = new PnlMatchup(PnlMatchup.LineDirection.DOWN, PnlMatchup.LineSide.BOTH, PnlMatchup.BoxSize.MEDIUM);
        matchups[4].setPlayerOne("Undetermined", avatar);
        matchups[4].setPlayerTwo("Undetermined", avatar);

        matchups[5] = new PnlMatchup(PnlMatchup.LineDirection.UP, PnlMatchup.LineSide.BOTH, PnlMatchup.BoxSize.MEDIUM);
        matchups[5].setPlayerOne("Undetermined", avatar);
        matchups[5].setPlayerTwo("Undetermined", avatar);

        matchups[6] = new PnlMatchup(PnlMatchup.LineDirection.STRAIGHT, PnlMatchup.LineSide.BOTH, PnlMatchup.BoxSize.LARGE);
        matchups[6].setPlayerOne("Undetermined", avatar);
        matchups[6].setPlayerTwo("Undetermined", avatar);

        matchups[7] = new PnlMatchup(PnlMatchup.LineDirection.STRAIGHT, PnlMatchup.LineSide.LEFT, PnlMatchup.BoxSize.LARGE_SINGLE, true);
        matchups[7].setPlayerOne("Undetermined", avatar);
        matchups[7].setPlayerTwo("Undetermined", avatar);

        pnlDeckImage = new ProportionalPanel(FSkin.getImage(FSkinProp.IMG_QUEST_DRAFT_DECK), 680, 475);

        final String constraints = "h 30px!, gap 0 0 0 10px";
        pnlStats.setLayout(new MigLayout("insets 0, gap 0, wrap, hidemode 0"));
        pnlStats.add(lblPastResults, "h 30px!, gap 0 0 0 25px");
        pnlStats.add(lblFirst, constraints);
        pnlStats.add(lblSecond, constraints);
        pnlStats.add(lblThird, constraints);
        pnlStats.add(lblFourth, constraints);
        pnlStats.add(lblTokens, "h 30px!, gap 0 0 50px 10px, ax center");
        pnlStats.add(btnSpendToken, "w 150px!, h 40px!, ax center");
        pnlStats.setOpaque(false);

        btnSpendToken.setToolTipText("Creates a new tournament that can be played immediately.");

    }

    public LblHeader getLblTitle() {
        return lblTitle;
    }

    public FLabel getLblCredits() {
        return lblCredits;
    }

    public FScrollPanel getPnlTournaments() {
        return pnlTournaments;
    }

    public StartButton getBtnStartDraft() {
        return btnStartDraft;
    }

    public StartButton getBtnStartTournament() {
        return btnStartTournament;
    }

    public StartButton getBtnStartMatch() {
        return btnStartMatch;
    }

    public FLabel getBtnStartMatchSmall() {
        return btnStartMatchSmall;
    }

    public FLabel getBtnEditDeck() {
        return btnEditDeck;
    }

    public FLabel getBtnLeaveTournament() {
        return btnLeaveTournament;
    }

    public PnlMatchup[] getLblsMatchups() {
        return matchups;
    }

    public FLabel getLblFirst() {
        return lblFirst;
    }

    public FLabel getLblSecond() {
        return lblSecond;
    }

    public FLabel getLblThird() {
        return lblThird;
    }

    public FLabel getLblFourth() {
        return lblFourth;
    }

    public FLabel getLblTokens() {
        return lblTokens;
    }

    public FLabel getBtnSpendToken() {
        return btnSpendToken;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Mode getMode() {
        return mode;
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_QUESTDRAFTS;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CSubmenuQuestDraft getLayoutControl() {
        return CSubmenuQuestDraft.SINGLETON_INSTANCE;
    }

    @Override
    public void setParentCell(DragCell cell0) {
        this.parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return parentCell;
    }

    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.QUEST;
    }

    @Override
    public String getMenuTitle() {
        return "Tournaments";
    }

    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_QUESTDRAFTS;
    }

    @Override
    public void populate() {

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();

        switch (mode) {

            case SELECT_TOURNAMENT:
                populateSelectTournament();
                break;
            case PREPARE_DECK:
                populatePrepareDeck();
                break;
            case TOURNAMENT_ACTIVE:
                populateTournamentActive();
                break;
            case EMPTY:
            default:
                VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, ax right, wrap 2"));
                VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 80%!, h 40px!, gap 0 0 15px 35px, ax right, span 2");
                VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblNoDrafts, "h 30px!, gap 0 0 5px, span 2");
                VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlTournaments, "w 88% - 200px!, pushy, growy");
                VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlStats, "w 185px!, pushy, growy, gap 4% 4% 0 0");
                break;

        }

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
    }

    private void populateSelectTournament() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, ax right, wrap 2"));
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 80%!, h 40px!, gap 0 0 15px 35px, ax right, span 2");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblCredits, "h 25px!, gap 0 0 30px, span 2");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblInfo, "h 30px!, gap 0 0 5px, span 2");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlTournaments, "w 88% - 200px!, pushy, growy");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlStats, "w 185px!, pushy, growy, gap 4% 4% 0 0");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(btnStartDraft, "gap 0 6% 30px 30px, ax center, span 2");
    }

    private void populatePrepareDeck() {
        lblTitle.setText("Quest Mode: Draft Tournament - " + FModel.getQuest().getAchievements().getCurrentDraft().getTitle());
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout(
                "insets 0, gap 0, ax center, wrap",
                "",
                "[][grow, center][][][]"
                ));
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 80%!, h 40px!, gap 20% 0 15px 35px, ax right");
        pnlDeckImage.setMaximumSize(new Dimension(680, 475));
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlDeckImage, "ax center, grow");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(btnEditDeck, "w 150px, h 50px, gap 0 0 15px 0, ax center");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(btnStartTournament, "gap 0 0 0 15px, ax center");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(btnLeaveTournament, "w 150px, h 35px, gap 0 0 25px 10%, ax center");
        btnEditDeck.setFontSize(24);
        btnLeaveTournament.setFontSize(12);
    }

    private void populateTournamentActive() {
        lblTitle.setText("Quest Mode: Draft Tournament - " + FModel.getQuest().getAchievements().getCurrentDraft().getTitle());
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, ax center, wrap 1"));
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 80%!, h 40px!, gap 20% 0 15px 10px, ax right, span 2");

        FScrollPanel panel = new FScrollPanel(new MigLayout("insets 0, gap 0, wrap 4, ax center"), true,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        String constraintsLeft = "w 350px!, h 196px!, gap 0px 0px 0px 0px, ay center";
        String constraintsMiddle = "w 350px!, h 392px!, gap 0px 0px 0px 0px, ay center";
        String constraintsRight = "w 350px!, h 784px!, gap 0px 0px 0px 0px, ay center";

        panel.add(matchups[0], constraintsLeft);
        panel.add(matchups[4], constraintsMiddle + ", span 1 2");
        panel.add(matchups[6], constraintsRight + ", span 1 4");
        panel.add(matchups[7], constraintsRight + ", span 1 4");
        panel.add(matchups[1], constraintsLeft);
        panel.add(matchups[2], constraintsLeft);
        panel.add(matchups[5], constraintsMiddle + ", span 1 2");
        panel.add(matchups[3], constraintsLeft);

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(panel, "gap 0 0 0 0, ax center");

        btnEditDeck.setFontSize(12);

        JPanel bottomButtons = new JPanel(new MigLayout("insets 0, gap 0, wrap 2, ax center"));

        if (FModel.getQuest().getAchievements().getCurrentDraft().playerHasMatchesLeft()) {
            VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(btnStartMatch, "gap 0 0 0 20px, ax center");
            bottomButtons.add(btnEditDeck, "w 135px!, h 25px!, gap 0 25px 10px 10px, ax right");
            bottomButtons.add(btnLeaveTournament, "w 135px!, h 25px!, gap 25px 0 10px 10px, ax right");
            btnLeaveTournament.setFontSize(12);
        }
        else {
            VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(btnLeaveTournament, "w 250px!, h 60px!, gap 0 0 20px 20px, ax center");
            bottomButtons.add(btnEditDeck, "w 135px!, h 25px!, gap 0 25px 10px 10px, ax right");
            bottomButtons.add(btnStartMatchSmall, "w 135px!, h 25px!, gap 25px 0 10px 10px, ax right");
            btnLeaveTournament.setFontSize(24);
        }

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(bottomButtons, "w 100%!");
        bottomButtons.setOpaque(false);
    }

    private final class ProportionalPanel extends JPanel {
        private static final long serialVersionUID = 2098643413467094674L;

        private final SkinImage image;

        int w, h;

        private ProportionalPanel(SkinImage image, int w, int h) {
            this.image = image;
            this.w = w;
            this.h = h;
        }

        @Override
        public Dimension getPreferredSize() {
            return new ProportionalDimension(super.getSize(), w, h);
        }

        @Override
        public void paintComponent(final Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();

            Dimension srcSize = image.getSizeForPaint(g2d);
            int wSrc = srcSize.width;
            int hSrc = srcSize.height;

            int wImg = getPreferredSize().width;
            int hImg = getPreferredSize().height;

            int xOffset = (getSize().width - wImg) / 2;

            RenderingHints hints = new RenderingHints(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHints(hints);

            FSkin.drawImage(g2d, image,
                    xOffset, 0, wImg + xOffset, hImg, // Destination
                    0, 0, wSrc, hSrc); // Source

            g2d.dispose();
        }
    }

    private final class ProportionalDimension extends Dimension {
        private static final long serialVersionUID = -428811386088062426L;

        private ProportionalDimension(Dimension d, int w, int h) {

            double containerAspect = (double) d.width / d.height;
            double imageAspect = (double) w / h;
            double scale = 1.0;

            if (imageAspect < containerAspect) {
                scale = (double) d.height / h;
            } else if (imageAspect > containerAspect) {
                scale = (double) d.width / w;
            }

            height = (int) (((double) h) * scale);
            width = (int) (((double) w) * scale);

        }

    }

    public static class PnlMatchup extends JPanel {

        private static final long serialVersionUID = 2055607559359905216L;

        private enum LineDirection {
            UP, DOWN, STRAIGHT, NONE
        }

        private enum LineSide {
            LEFT, RIGHT, BOTH
        }

        private enum BoxSize {
            SMALL, MEDIUM, LARGE, LARGE_SINGLE
        }

        private final SkinColor clr1 = FSkin.getColor(FSkin.Colors.CLR_THEME2).alphaColor(255);
        private final SkinColor clr2 = FSkin.getColor(FSkin.Colors.CLR_THEME).alphaColor(100);
        private final SkinColor clr3 = FSkin.getColor(FSkin.Colors.CLR_THEME).alphaColor(100);

        private final int wImg = 55;
        private final int hImg = 55;

        private SkinImage img1;
        private SkinImage img2;

        private FLabel name1 = new FLabel.Builder().fontSize(14).fontAlign(SwingConstants.LEFT).build();
        private FLabel name2 = new FLabel.Builder().fontSize(14).fontAlign(SwingConstants.LEFT).build();

        private LineDirection lineDir;
        private LineSide lineSide;
        private BoxSize size;

        private boolean singleBox;

        public PnlMatchup(LineDirection dir, LineSide side, BoxSize size, boolean singleBox) {
            this.setLayout(new MigLayout("insets 0, gap 0, wrap"));
            if (!singleBox) {
                this.add(name1, "w 100%!, h 50% - 30px!, gap 135px 0 0 30px, ax right");
                this.add(name2, "w 100%!, h 50% - 30px!, gap 135px 0 31px 0, ax right");
            } else {
                this.add(name1, "w 100%!, h 50%!, gap 135px 0 11px 0, ax right");
            }
            this.lineDir = dir;
            this.lineSide = side;
            this.size = size;
            this.singleBox = singleBox;
            name1.setVerticalAlignment(SwingConstants.BOTTOM);
            name2.setVerticalAlignment(SwingConstants.TOP);
        }

        public PnlMatchup(LineDirection dir, LineSide side, BoxSize size) {
            this(dir, side, size, false);
        }

        public void setPlayerOne(final String name, final SkinImage image) {
            name1.setText(name);
            img1 = image;
        }

        public void setPlayerTwo(final String name, final SkinImage image) {
            name2.setText(name);
            img2 = image;
        }

        @Override
        public void paintComponent(final Graphics g) {

            Graphics2D g2d = (Graphics2D) g.create();

            int width = getWidth() - 100;
            int height = getHeight() - 40;

            if (size.equals(BoxSize.MEDIUM)) {
                height -= 196;
                g2d.translate(0, 98);
            } else if (size.equals(BoxSize.LARGE)) {
                height -= 588;
                g2d.translate(0, 294);
            } else if (size.equals(BoxSize.LARGE_SINGLE)) {
                height -= 665;
                g2d.translate(0, 333);
            }

            g2d.setColor(clr1.getColor());
            g2d.setStroke(new BasicStroke(4));

            if (lineSide.equals(LineSide.LEFT) || lineSide.equals(LineSide.BOTH)) {
                g2d.drawLine(0, height / 2 + 20, 47, height / 2 + 20);
            }

            g2d.translate(50, 20);

            if (lineSide.equals(LineSide.RIGHT) || lineSide.equals(LineSide.BOTH)) {
                g2d.drawLine(width, height / 2, width + 65, height / 2);
                if (lineDir.equals(LineDirection.DOWN)) {
                    g2d.drawLine(width + 48, height / 2, width + 48, height + 400);
                } else if (lineDir.equals(LineDirection.UP)) {
                    g2d.drawLine(width + 48, height / 2, width + 48, -400);
                } else if (lineDir.equals(LineDirection.STRAIGHT)) {
                    g2d.drawLine(width, height / 2, width + 45, height / 2);
                }
            }

            FSkin.setGraphicsGradientPaint(g2d, 0, 0, clr3, 0, height / 2 + 15 , clr2);
            g2d.fillRect(0, 0, width, height / 2);

            FSkin.setGraphicsGradientPaint(g2d, 0, height / 2 - 15, clr2, 0, height, clr3);
            g2d.fillRect(0, height / 2, width, height / 2);

            g2d.setColor(clr1.getColor());
            g2d.setStroke(new BasicStroke(4));
            g2d.drawRect(1, 1, width - 2, height - 2);

            if (!singleBox) {
                FSkin.setGraphicsGradientPaint(g2d, 70, height / 2 - 1, clr1.alphaColor(0), width, height / 2 - 1, clr1);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRect(70, height / 2 - 1, width - 70, 2);
            }

            g2d.setColor(FSkin.getColor(Colors.CLR_TEXT).getColor());
            g2d.setStroke(new BasicStroke(1));

            if (!singleBox) {
                Rectangle2D textSize = g2d.getFontMetrics().getStringBounds("VS", g2d);
                g2d.drawString("VS", (width + (int) textSize.getWidth()) / 2, (height + (int) textSize.getHeight()) / 2 - 2);
            }

            // Padding here
            g2d.translate(12, 12);

            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            Dimension srcSize = img1.getSizeForPaint(g2d);
            int wSrc = srcSize.width;
            int hSrc = srcSize.height;

            FSkin.drawImage(g2d, img1,
                    0, 0, wImg, hImg, // Destination
                    0, 0, wSrc, hSrc); // Source

            g2d.translate(0, 77);

            if (!singleBox) {
                FSkin.drawImage(g2d, img2,
                        0, 0, wImg, hImg, // Destination
                        0, 0, wSrc, hSrc); // Source
            }

            g2d.dispose();
        }
    }

    @Override
    public void updateEventList(QuestEventDraftContainer events) {
        pnlTournaments.removeAll();
        if (events == null) { return; }

        final JXButtonPanel grpPanel = new JXButtonPanel();

        boolean firstPanel = true;

        for (final QuestEventDraft draft : events) {
            final PnlDraftEvent draftPanel = new PnlDraftEvent(draft);
            final JRadioButton button = draftPanel.getRadioButton();

            if (firstPanel) {
                button.setSelected(true);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() { button.requestFocusInWindow(); }
                });
                firstPanel = false;
            }

            grpPanel.add(draftPanel, button, "w 100%!, h 135px!, gapy 15px");

            button.addKeyListener(startOnEnter);
            button.addMouseListener(startOnDblClick);
        }

        pnlTournaments.add(grpPanel, "w 100%!");
    }

    private final KeyAdapter startOnEnter = new KeyAdapter() {
        @Override
        public void keyPressed(final KeyEvent e) {
            if (KeyEvent.VK_ENTER == e.getKeyChar()) {
                btnStartDraft.doClick();
            }
        }
    };

    private final MouseAdapter startOnDblClick = new MouseAdapter() {
        @Override
        public void mouseClicked(final MouseEvent e) {
            if (MouseEvent.BUTTON1 == e.getButton() && 2 == e.getClickCount()) {
                btnStartDraft.doClick();
            }
        }
    };

    @Override
    public void updateTournamentBoxLabel(String playerID, int iconID, int box, boolean first) {
        SkinImage icon = FSkin.getAvatars().get(iconID);
        if (icon == null) {
            icon = FSkin.getAvatars().get(0);
        }

        if (first) {
            matchups[box].setPlayerOne(playerID, icon);
        }
        else {
            matchups[box].setPlayerTwo(playerID, icon);
        }
    }

    @Override
    public void startDraft(BoosterDraft draft) {
        final CEditorQuestDraftingProcess draftController = new CEditorQuestDraftingProcess(CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture());
        draftController.showGui(draft);

        draftController.setDraftQuest(CSubmenuQuestDraft.SINGLETON_INSTANCE);

        Singletons.getControl().setCurrentScreen(FScreen.DRAFTING_PROCESS);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(draftController);
    }

    @Override
    public void editDeck(boolean isExistingDeck) {
        final CDetailPicture cDetailPicture = CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture();
        if (isExistingDeck) {
            VCurrentDeck.SINGLETON_INSTANCE.setItemManager(new DeckManager(GameType.Draft, cDetailPicture));
        }
        Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_QUEST_TOURNAMENT);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(new CEditorQuestLimited(FModel.getQuest(), cDetailPicture));
    }
}
