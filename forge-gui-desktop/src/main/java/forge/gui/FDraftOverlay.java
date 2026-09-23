package forge.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import forge.Singletons;
import forge.screens.home.online.OnlineMenu;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import forge.view.FDialog;
import forge.view.FFrame;
import net.miginfocom.swing.MigLayout;

/**
 * Floating overlay window displayed during network draft sessions.
 * Shows pack/round info, a countdown timer, and neighbor seats with queue depths.
 *
 * Follows the same FDialog pattern as {@link FNetOverlay}.
 */
public enum FDraftOverlay {
    SINGLETON_INSTANCE;

    private static final int DEFAULT_WIDTH  = 420;
    private static final int MAX_PACK_ICONS = 9;
    private static final int TABLE_FONT_SIZE = 12;
    private static final int TABLE_ICON_W = 13;
    private static final int TABLE_ICON_H = 18;
    private static final int TABLE_ROW_H  = TABLE_ICON_H + 4;
    private static final Color HIGHLIGHT_BG   = new Color(245, 232, 150);
    private static final Color HIGHLIGHT_TEXT = new Color(30, 30, 25);

    private boolean hasBeenShown;

    private final FSkin.SkinnedLabel lblPackInfo  = makeTextLabel("");
    private final FSkin.SkinnedLabel lblTimer     = makeTextLabel("");
    private final FSkin.SkinnedLabel lblAllSeats  = makeTextLabel("");
    private final DraftTimerRope     rope         = new DraftTimerRope();
    private final JPanel pnlNeighbors = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
    private final JPanel pnlSeatTable = new JPanel(new MigLayout("insets 4 0 4 0, gap 0, wrap 3",
            "[][grow,sizegroup tcol][grow,sizegroup tcol]", ""));

    private static ImageIcon cardBackIcon;
    private static ImageIcon tableCardBackIcon;

    private String   leftName, rightName;
    private boolean  leftAI,   rightAI;
    private int      mySeat;
    private int      currentPack, totalPacks;
    private int      currentPick, initialPackSize;
    private boolean  passingRight;
    private int[]    queueDepths = new int[0];
    private String[] allNames = new String[0];
    private boolean[] allAI   = new boolean[0];
    private boolean  expanded;

    /** Countdown timer (client-side fire-and-forget). */
    private Timer countdownTimer;
    private int               secondsRemaining;
    private boolean           waitingForPack;

    private final FDialog window = new FDialog(false, true, "4");

    FDraftOverlay() {
        window.setTitle(Localizer.getInstance().getMessage("lblDraft"));
        window.setVisible(false);
        window.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        window.setBorder(new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_BORDERS)));

        // Rows: [pack info | timer], [timer rope], [neighbor strip], [separator], [all-seats toggle], [seat table]
        window.setLayout(new MigLayout("insets 8 4 4 4, gap 0, wrap 2", "", "[]10[10!]10[]6[]6[]0[]"));

        lblPackInfo.setHorizontalAlignment(SwingConstants.LEFT);
        lblTimer.setHorizontalAlignment(SwingConstants.RIGHT);

        pnlNeighbors.setOpaque(false);
        pnlSeatTable.setOpaque(false);
        pnlSeatTable.setVisible(false);

        // Smaller than the body text — this is a secondary control, sized to match the seat table
        lblAllSeats.setFont(FSkin.getBoldFont(TABLE_FONT_SIZE));
        lblAllSeats.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblAllSeats.addMouseListener(new FMouseAdapter() {
            @Override
            public void onLeftClick(MouseEvent e) {
                toggleExpanded();
            }
        });
        updateAllSeatsLabel();

        // Row 1: pack info on the left, timer on the right
        window.add(lblPackInfo,  "pushx, growx, gapleft 4");
        window.add(lblTimer,     "pushx, growx, gapright 4, al right");
        // Row 2: timer rope spans both columns, with extra horizontal margin to inset it from the dialog edge
        window.add(rope,         "span 2, growx, h 10!, gapleft 4, gapright 4");
        // Row 3: neighbor strip spans both columns, vertically centered in its cell
        window.add(pnlNeighbors, "span 2, pushx, growx, gapleft 4, gapright 4, ay center");
        // Row 4: separator dividing the neighbor strip from the disclosure toggle
        window.add(new JSeparator(JSeparator.HORIZONTAL), "span 2, growx, gapleft 4, gapright 4");
        // Row 5: clickable disclosure toggle for the full seat table
        window.add(lblAllSeats,  "span 2, growx, gapleft 4");
        // Row 6: full seat table, hidden until expanded (hidemode 3 keeps it out of the collapsed layout)
        window.add(pnlSeatTable, "span 2, growx, gapleft 4, gapright 4, hidemode 3");

        // Load card back icon (scaled to small size)
        loadCardBackIcon();
    }

    /**
     * Called at draft start.
     *
     * @param mySeat       this player's seat index (0-based)
     * @param names        display names for all seats in pod order
     * @param aiFlags      parallel boolean array — true if that seat is AI
     * @param totalPacks   total number of packs in the draft
     */
    public void initDraft(int mySeat, String[] names, boolean[] aiFlags, int totalPacks) {
        SwingUtilities.invokeLater(() -> {
            if (names.length == 0) {
                // Stale pre-participants event view — skip; a later call with
                // populated participants will reinitialize properly
                return;
            }
            this.mySeat     = mySeat;
            this.totalPacks = totalPacks;
            // At draft start each seat holds exactly one pack (the one they're
            // picking from); the server's subsequent SeatPicked broadcasts overwrite
            // these as picks are made, but showing 1s up front gives every seat a
            // visible pack indicator on pack 1 / pick 1
            this.queueDepths = new int[names.length];
            for (int i = 0; i < queueDepths.length; i++) queueDepths[i] = 1;
            this.allNames = names.clone();
            this.allAI    = aiFlags.clone();

            int podSize = names.length;
            int leftIdx  = (mySeat - 1 + podSize) % podSize;
            int rightIdx = (mySeat + 1) % podSize;

            leftName  = names[leftIdx];
            rightName = names[rightIdx];
            leftAI    = aiFlags[leftIdx];
            rightAI   = aiFlags[rightIdx];

            // Only show "waiting" if no pack has arrived yet — otherwise preserve
            // state set by onPackArrived() which may have run before us
            if (currentPack == 0) {
                waitingForPack = true;
            }
            updateDisplay();
            show();
        });
    }

    /**
     * Called when a new pack arrives for this player to pick from.
     *
     * @param packNumber   1-based pack number
     * @param pickNumber   0-based pick number within the pack round
     * @param packSize     number of cards in the pack
     * @param timerSeconds seconds allowed to pick (0 = no timer)
     */
    public void onPackArrived(int packNumber, int pickNumber, int packSize, int timerSeconds) {
        SwingUtilities.invokeLater(() -> {
            currentPack    = packNumber;
            currentPick    = pickNumber + 1;
            if (pickNumber == 0) {
                initialPackSize = packSize;
            }
            waitingForPack = false;
            // Pack direction: odd packs pass right, even packs pass left (conventional booster draft)
            passingRight   = (packNumber % 2 == 1);
            updateDisplay();
            if (timerSeconds > 0) {
                startCountdown(timerSeconds);
            }
        });
    }

    /**
     * Called when any seat in the pod picks a card.
     *
     * @param newDepths updated queue-depth array (one entry per seat)
     */
    public void onSeatPicked(int[] newDepths) {
        SwingUtilities.invokeLater(() -> {
            if (newDepths != null && newDepths.length == queueDepths.length) {
                System.arraycopy(newDepths, 0, queueDepths, 0, newDepths.length);
            }
            updateDisplay();
            if (expanded) {
                buildSeatTable();
            }
        });
    }

    /**
     * Called when THIS player submits a pick.
     * Stops the countdown and shows "Waiting for packs..." until the next pack arrives.
     */
    public void onPickSubmitted() {
        SwingUtilities.invokeLater(() -> {
            stopCountdown();
            waitingForPack = true;
            updateDisplay();
        });
    }

    /** Hides the overlay and clears draft state. Call at draft end. */
    public void reset() {
        SwingUtilities.invokeLater(() -> {
            stopCountdown();
            leftName = rightName = null;
            leftAI = rightAI = false;
            mySeat = 0;
            currentPack = totalPacks = 0;
            passingRight = false;
            queueDepths = new int[0];
            allNames = new String[0];
            allAI = new boolean[0];
            waitingForPack = false;
            expanded = false;
            lblPackInfo.setText("");
            lblTimer.setText("");
            pnlNeighbors.removeAll();
            pnlSeatTable.removeAll();
            pnlSeatTable.setVisible(false);
            updateAllSeatsLabel();
            hide();
        });
    }

    public void hide() {
        window.setVisible(false);
        OnlineMenu.draftItem.setState(false);
    }

    public void show() {
        if (!hasBeenShown) {
            FFrame mainFrame = Singletons.getView().getFrame();
            window.setLocation(mainFrame.getX() + 10, mainFrame.getY() + 50);
            window.getTitleBar().addMouseListener(new FMouseAdapter() {
                @Override
                public void onLeftDoubleClick(MouseEvent e) {
                    hide();
                }
            });
            hasBeenShown = true;
        }
        sizeToContent();
        window.setVisible(true);
        OnlineMenu.draftItem.setState(true);
    }

    private void updateDisplay() {
        final Localizer localizer = Localizer.getInstance();
        // Row 1 – pack info + pick number
        if (currentPack > 0 && totalPacks > 0) {
            String text = localizer.getMessage("lblDraftOverlayPackOfN",
                    String.valueOf(currentPack), String.valueOf(totalPacks));
            if (currentPick > 0 && initialPackSize > 0) {
                text += "  \u2022  " + localizer.getMessage("lblDraftOverlayPickOfN",
                        String.valueOf(currentPick), String.valueOf(initialPackSize));
            }
            lblPackInfo.setText(text);
        } else {
            lblPackInfo.setText(localizer.getMessage("lblDraft"));
        }

        // Row 1 – timer
        if (waitingForPack) {
            lblTimer.setText(localizer.getMessage("lblDraftOverlayWaitingForPack"));
            lblTimer.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        } else if (countdownTimer != null && countdownTimer.isRunning()) {
            updateTimerLabel();
        } else {
            lblTimer.setText("");
        }

        // Row 2 – neighbor strip
        if (leftName != null && rightName != null) {
            buildNeighborPanel();
        }

        window.revalidate();
        window.repaint();
    }

    private void startCountdown(int seconds) {
        stopCountdown();
        secondsRemaining = seconds;
        updateTimerLabel();
        rope.start(seconds);
        countdownTimer = new Timer(1000, e -> {
            secondsRemaining--;
            if (secondsRemaining <= 0) {
                secondsRemaining = 0;
                stopCountdown();
            }
            updateTimerLabel();
        });
        countdownTimer.start();
    }

    private void stopCountdown() {
        if (countdownTimer != null) {
            countdownTimer.stop();
            countdownTimer = null;
        }
        rope.stop();
    }

    private void updateTimerLabel() {
        int mins = secondsRemaining / 60;
        int secs = secondsRemaining % 60;
        lblTimer.setText(Localizer.getInstance().getMessage("lblDraftOverlayTimer",
                String.format("%d:%02d", mins, secs)));

        // Color based on urgency
        Color timerColor;
        if (secondsRemaining <= 5) {
            timerColor = Color.RED;
        } else if (secondsRemaining <= 15) {
            timerColor = Color.YELLOW;
        } else {
            FSkin.SkinColor skinText = FSkin.getColor(FSkin.Colors.CLR_TEXT);
            timerColor = (skinText != null) ? skinText.getColor() : Color.WHITE;
        }
        lblTimer.setForeground(timerColor);
    }

    /**
     * Builds the neighbor display panel with text labels and card-back icons.
     *
     * Layout (passing right / odd packs — packs flow left→right, so they arrive
     * on the LEFT side of each seat):
     *   [packs]LeftName  →  [packs]YOU  →  [packs]RightName
     *
     * Layout (passing left / even packs — packs flow right→left, so they arrive
     * on the RIGHT side of each seat):
     *   LeftName[packs]  ←  YOU[packs]  ←  RightName[packs]
     *
     * Icons always sit on the incoming side of their seat — the side the next
     * pack will arrive from.
     */
    private void buildNeighborPanel() {
        pnlNeighbors.removeAll();

        final Localizer localizer = Localizer.getInstance();
        int podSize  = queueDepths.length;
        int leftIdx  = podSize > 0 ? (mySeat - 1 + podSize) % podSize : 0;
        int rightIdx = podSize > 0 ? (mySeat + 1) % podSize : 0;

        int myDepth    = podSize > 0 ? queueDepths[mySeat]   : 0;
        int leftDepth  = podSize > 0 ? queueDepths[leftIdx]  : 0;
        int rightDepth = podSize > 0 ? queueDepths[rightIdx] : 0;

        String aiSuffix = " (" + localizer.getMessage("lblAI") + ")";
        String leftLabel  = leftName  + (leftAI  ? aiSuffix : "");
        String rightLabel = rightName + (rightAI ? aiSuffix : "");
        String arrow = passingRight ? " \u2192 " : " \u2190 ";
        String you = localizer.getMessage("lblDraftOverlayYou");

        if (passingRight) {
            // Incoming side is the LEFT of each name — icons before the name
            addPackIcons(leftDepth);
            pnlNeighbors.add(makeTextLabel(leftLabel + arrow));
            addPackIcons(myDepth);
            pnlNeighbors.add(makeTextLabel(you + arrow));
            addPackIcons(rightDepth);
            pnlNeighbors.add(makeTextLabel(rightLabel));
        } else {
            // Incoming side is the RIGHT of each name — icons after the name
            pnlNeighbors.add(makeTextLabel(leftLabel));
            addPackIcons(leftDepth);
            pnlNeighbors.add(makeTextLabel(arrow + you));
            addPackIcons(myDepth);
            pnlNeighbors.add(makeTextLabel(arrow + rightLabel));
            addPackIcons(rightDepth);
        }

        pnlNeighbors.revalidate();
        pnlNeighbors.repaint();
    }

    private void toggleExpanded() {
        expanded = !expanded;
        updateAllSeatsLabel();
        if (expanded) {
            buildSeatTable();
        }
        pnlSeatTable.setVisible(expanded);
        sizeToContent();
    }

    /**
     * Fits the window height to its laid-out content, keeping the fixed width and current location.
     * {@code pack()} measures the title bar (a JMenuBar), contents, and border insets, and the seat
     * table is {@code hidemode 3} so it adds nothing while collapsed — so this stays correct across
     * skins and font scales without a hardcoded height.
     */
    private void sizeToContent() {
        int x = window.getX();
        int y = window.getY();
        window.pack();
        window.setBounds(x, y, DEFAULT_WIDTH, window.getHeight());
    }

    private void updateAllSeatsLabel() {
        // Paint the triangle — the skin font (Roboto) lacks the right-pointing glyph, so a font glyph would tofu
        lblAllSeats.setText(Localizer.getInstance().getMessage("lblDraftOverlayAllSeats"));
        FSkin.SkinColor c = FSkin.getColor(FSkin.Colors.CLR_TEXT);
        lblAllSeats.setIcon(new TriangleIcon(expanded, c != null ? c.getColor() : Color.LIGHT_GRAY));
    }

    /** A small filled triangle: right-pointing when collapsed, down-pointing when expanded. */
    private static final class TriangleIcon implements Icon {
        private static final int SIZE = 8;
        private final boolean down;
        private final Color color;

        TriangleIcon(boolean down, Color color) {
            this.down = down;
            this.color = color;
        }

        @Override public int getIconWidth()  { return SIZE; }
        @Override public int getIconHeight() { return SIZE; }

        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            int[] xs, ys;
            if (down) {
                xs = new int[] {x, x + SIZE, x + SIZE / 2};
                ys = new int[] {y + 1, y + 1, y + SIZE - 1};
            } else {
                xs = new int[] {x + 1, x + 1, x + SIZE - 1};
                ys = new int[] {y, y + SIZE, y + SIZE / 2};
            }
            g2.fillPolygon(xs, ys, 3);
            g2.dispose();
        }
    }

    /** Rebuilds the per-seat table: every seat in pod order on the left, its current pack count on the right. */
    private void buildSeatTable() {
        pnlSeatTable.removeAll();
        final Localizer localizer = Localizer.getInstance();

        pnlSeatTable.add(makeTableLabel(localizer.getMessage("lblDraftOverlaySeat")), "gapleft 8, gapbottom 4");
        pnlSeatTable.add(makeTableLabel(localizer.getMessage("lblPlayer")), "growx, gapleft 14, gapbottom 4");
        pnlSeatTable.add(makePacksCell(localizer.getMessage("lblDraftOverlayPacks"), -1, null, null), "growx, gapbottom 4");

        String aiSuffix = " (" + localizer.getMessage("lblAI") + ")";
        Color zebra  = FSkin.getColor(FSkin.Colors.CLR_ZEBRA).getColor();
        Color stripe = blend(zebra, FSkin.getColor(FSkin.Colors.CLR_HOVER).getColor(), 0.5f);
        // growx/growy make the bands contiguous; hmin keeps rows from shrinking when a seat has no packs
        String rowCon = "growx, growy, hmin " + TABLE_ROW_H;
        for (int i = 0; i < allNames.length; i++) {
            String name = allNames[i];
            if (i < allAI.length && allAI[i]) name += aiSuffix;
            int depth = (i < queueDepths.length) ? queueDepths[i] : 0;

            boolean me = (i == mySeat);
            Color bg = me ? HIGHLIGHT_BG : (i % 2 == 0 ? zebra : stripe);
            // The light-yellow highlight needs dark text for contrast; other rows keep the default light text
            Color fg = me ? HIGHLIGHT_TEXT : null;

            pnlSeatTable.add(makeRowLabel(String.valueOf(i + 1), bg, fg, 8), rowCon);
            pnlSeatTable.add(makeRowLabel(name, bg, fg, 14), rowCon);
            pnlSeatTable.add(makePacksCell(null, depth, bg, fg), rowCon);
        }
        pnlSeatTable.revalidate();
        pnlSeatTable.repaint();
    }

    private static Color blend(Color a, Color b, float t) {
        return new Color(
                Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t));
    }

    /**
     * Builds the packs cell: one pack sleeve per held pack followed by a {@code (×N)} count.
     * A negative depth marks the header cell — text only.
     */
    private JPanel makePacksCell(String headerText, int depth, Color bg, Color fg) {
        JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        cell.setOpaque(bg != null);
        if (bg != null) cell.setBackground(bg);
        cell.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 0));
        if (depth < 0) {
            cell.add(tableText(headerText, fg));
        } else if (depth == 0) {
            cell.add(tableText("0", fg));
        } else {
            int shown = Math.min(depth, MAX_PACK_ICONS);
            for (int k = 0; k < shown && tableCardBackIcon != null; k++) {
                JLabel icon = new JLabel(tableCardBackIcon);
                icon.setOpaque(false);
                cell.add(icon);
            }
            cell.add(tableText("(×" + depth + ")", fg));
        }
        return cell;
    }

    private static void loadCardBackIcon() {
        if (cardBackIcon != null) return;
        try {
            FSkin.SkinImage sleeve = FSkin.getSleeves().get(0);
            if (sleeve != null) {
                cardBackIcon = sleeve.resize(18, 25).getIcon();
                tableCardBackIcon = sleeve.resize(TABLE_ICON_W, TABLE_ICON_H).getIcon();
            }
        } catch (Exception e) {
            // Fallback: icon stays null, text "[P]" will be used
        }
    }

    private void addPackIcons(int depth) {
        if (depth <= 0) return;
        if (cardBackIcon != null) {
            JLabel icon = new JLabel(cardBackIcon);
            icon.setOpaque(false);
            pnlNeighbors.add(icon);
        } else {
            pnlNeighbors.add(makeTextLabel("[P]"));
        }
        if (depth > 1) {
            FSkin.SkinnedLabel plus = new FSkin.SkinnedLabel("×" + depth);
            plus.setFont(FSkin.getBoldFont(12));
            FSkin.SkinColor color = FSkin.getColor(FSkin.Colors.CLR_TEXT);
            if (color != null) plus.setForeground(color);
            plus.setOpaque(false);
            plus.setVerticalAlignment(SwingConstants.TOP);
            Dimension pref = plus.getPreferredSize();
            plus.setPreferredSize(new Dimension(pref.width, 25));
            pnlNeighbors.add(plus);
        }
    }

    private FSkin.SkinnedLabel makeTextLabel(String text) {
        FSkin.SkinnedLabel lbl = new FSkin.SkinnedLabel(text);
        lbl.setFont(FSkin.getBoldFont(14));
        FSkin.SkinColor color = FSkin.getColor(FSkin.Colors.CLR_TEXT);
        if (color != null) lbl.setForeground(color);
        lbl.setOpaque(false);
        return lbl;
    }

    private FSkin.SkinnedLabel makeTableLabel(String text) {
        FSkin.SkinnedLabel lbl = makeTextLabel(text);
        lbl.setFont(FSkin.getBoldFont(TABLE_FONT_SIZE));
        return lbl;
    }

    private FSkin.SkinnedLabel tableText(String text, Color fg) {
        FSkin.SkinnedLabel lbl = makeTableLabel(text);
        if (fg != null) lbl.setForeground(fg);
        return lbl;
    }

    private FSkin.SkinnedLabel makeRowLabel(String text, Color bg, Color fg, int leftPad) {
        FSkin.SkinnedLabel lbl = tableText(text, fg);
        lbl.setOpaque(true);
        lbl.setBackground(bg);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, leftPad, 0, 0));
        return lbl;
    }

}
