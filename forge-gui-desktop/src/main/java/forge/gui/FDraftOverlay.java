package forge.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
    private static final int DEFAULT_HEIGHT = 95;

    private boolean hasBeenShown;

    private final FSkin.SkinnedLabel lblPackInfo  = new FSkin.SkinnedLabel("");
    private final FSkin.SkinnedLabel lblTimer     = new FSkin.SkinnedLabel("");
    private final JPanel pnlNeighbors = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));

    private static ImageIcon cardBackIcon;

    private String   leftName, rightName;
    private boolean  leftAI,   rightAI;
    private int      mySeat;
    private int      currentPack, totalPacks;
    private int      currentPick, initialPackSize;
    private boolean  passingRight;
    private int[]    queueDepths = new int[0];

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

        // Two-row layout: [pack info | timer] then [neighbor strip spanning both columns];
        // second row grows to fill remaining vertical space so the neighbor strip
        // centers between the info row and the bottom of the window
        window.setLayout(new MigLayout("insets 4, gap 0, wrap 2", "", "[][grow]"));

        // Apply bold skin font and text color to all labels
        FSkin.SkinColor textColor = FSkin.getColor(FSkin.Colors.CLR_TEXT);

        lblPackInfo.setFont(FSkin.getBoldFont(14));
        lblTimer.setFont(FSkin.getBoldFont(14));

        if (textColor != null) {
            lblPackInfo.setForeground(textColor);
            lblTimer.setForeground(textColor);
        }

        lblPackInfo.setHorizontalAlignment(SwingConstants.LEFT);
        lblTimer.setHorizontalAlignment(SwingConstants.RIGHT);

        lblPackInfo.setOpaque(false);
        lblTimer.setOpaque(false);
        pnlNeighbors.setOpaque(false);

        // Row 1: pack info on the left, timer on the right
        window.add(lblPackInfo,  "pushx, growx, gapleft 4");
        window.add(lblTimer,     "pushx, growx, gapright 4, al right");
        // Row 2: neighbor strip spans both columns, vertically centered in its cell
        window.add(pnlNeighbors, "span 2, pushx, growx, gapleft 4, gapright 4, ay center");

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
            waitingForPack = false;
            lblPackInfo.setText("");
            lblTimer.setText("");
            pnlNeighbors.removeAll();
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
            window.setBounds(mainFrame.getX() + 10, mainFrame.getY() + 50, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            window.getTitleBar().addMouseListener(new FMouseAdapter() {
                @Override
                public void onLeftDoubleClick(MouseEvent e) {
                    hide();
                }
            });
            hasBeenShown = true;
        }
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

    private static void loadCardBackIcon() {
        if (cardBackIcon != null) return;
        try {
            FSkin.SkinImage sleeve = FSkin.getSleeves().get(0);
            if (sleeve != null) {
                cardBackIcon = sleeve.resize(18, 25).getIcon();
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
            FSkin.SkinnedLabel plus = new FSkin.SkinnedLabel("x" + depth);
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

    private JLabel makeTextLabel(String text) {
        FSkin.SkinnedLabel lbl = new FSkin.SkinnedLabel(text);
        lbl.setFont(FSkin.getBoldFont(14));
        FSkin.SkinColor color = FSkin.getColor(FSkin.Colors.CLR_TEXT);
        if (color != null) lbl.setForeground(color);
        lbl.setOpaque(false);
        return lbl;
    }

}
