/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2021  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.screens.match;

import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import forge.card.MagicColor;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.gui.SOverlayUtils;
import forge.localinstance.skin.FSkinProp;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.util.Localizer;
import forge.util.TextUtil;
import forge.view.FDialog;
import forge.view.arcane.CardPanel;
import forge.view.arcane.MiscCardPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Assembles Swing components of assign generic amount dialog.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VAssignGenericAmount {
    final Localizer localizer = Localizer.getInstance();
    private final CMatchUI matchUI;

    // Width and height of assign dialog
    private final int wDlg = 700;
    private final int hDlg = 500;
    private final FDialog dlg = new FDialog();

    // Amount storage
    private final int totalAmountToAssign;

    private final String lblAmount;
    private final JLabel lblTotalAmount;
    private final boolean atLeastOne;
    //  Label Buttons
    private final FButton btnOK    = new FButton(localizer.getMessage("lblOk"));
    private final FButton btnReset = new FButton(localizer.getMessage("lblReset"));

    private static class AssignTarget {
        public final Object entity;
        public final JLabel label;
        public final int max;
        public int amount;

        public AssignTarget(final Object e, final JLabel lbl, int max0) {
            entity = e;
            label = lbl;
            max = max0;
            amount = 0;
        }
    }

    private final List<AssignTarget> targetsList = new ArrayList<>();
    private final Map<SkinnedPanel, AssignTarget> targetsMap = new HashMap<>();

    // Mouse actions
    private final MouseAdapter mad = new MouseAdapter() {
        @Override
        public void mouseEntered(final MouseEvent evt) {
            ((SkinnedPanel) evt.getSource()).setBorder(new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_ACTIVE), 2));
        }

        @Override
        public void mouseExited(final MouseEvent evt) {
            ((SkinnedPanel) evt.getSource()).setBorder((Border)null);
        }

        @Override
        public void mousePressed(final MouseEvent evt) {
            SkinnedPanel panel = (SkinnedPanel)evt.getSource();
            AssignTarget at = targetsMap.get(panel);

            boolean meta = evt.isControlDown();
            boolean isLMB = SwingUtilities.isLeftMouseButton(evt);
            boolean isRMB = SwingUtilities.isRightMouseButton(evt);

            if ( isLMB || isRMB)
                assignAmountTo(at, meta, isLMB);
        }
    };

    public VAssignGenericAmount(final CMatchUI matchUI, final CardView effectSource, final Map<Object, Integer> targets, final int amount, final boolean atLeastOne, final String amountLabel) {
        this.matchUI = matchUI;
        dlg.setTitle(localizer.getMessage("lbLAssignAmountForEffect", amountLabel, effectSource.toString()));

        totalAmountToAssign = amount;
        this.atLeastOne = atLeastOne;

        lblAmount = amountLabel;
        lblTotalAmount = new FLabel.Builder().text(localizer.getMessage("lblTotalAmountText", lblAmount)).build();

        // Top-level UI stuff
        final JPanel overlay = SOverlayUtils.genericOverlay();
        final SkinnedPanel pnlMain = new SkinnedPanel();
        pnlMain.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        // Effect Source area
        final CardPanel pnlSource = new CardPanel(matchUI, effectSource);
        pnlSource.setOpaque(false);
        pnlSource.setCardBounds(0, 0, 105, 150);

        final JPanel pnlInfo = new JPanel(new MigLayout("insets 0, gap 0, wrap"));
        pnlInfo.setOpaque(false);
        pnlInfo.add(lblTotalAmount, "gap 0 0 20px 5px");
        pnlInfo.add(new FLabel.Builder().text(localizer.getMessage("lblLClickAmountMessage", lblAmount)).build(), "gap 0 0 0 5px");
        pnlInfo.add(new FLabel.Builder().text(localizer.getMessage("lblRClickAmountMessage", lblAmount)).build(), "gap 0 0 0 5px");

        // Targets area
        final JPanel pnlTargets = new JPanel();
        pnlTargets.setOpaque(false);
        int cols = targets.size();
        final String wrap = "wrap " + cols;
        pnlTargets.setLayout(new MigLayout("insets 0, gap 0, ax center, " + wrap));

        final FScrollPane scrTargets = new FScrollPane(pnlTargets, false);

        // Top row of cards...
        for (final Map.Entry<Object, Integer> e : targets.entrySet()) {
            int maxAmount = e.getValue() != null ? e.getValue() : amount;
            final AssignTarget at = new AssignTarget(e.getKey(), new FLabel.Builder().text("0").fontSize(18).fontAlign(SwingConstants.CENTER).build(), maxAmount);
            addPanelForTarget(pnlTargets, at);
        }

        // ... bottom row of labels.
        for (final AssignTarget l : targetsList) {
            if (l.entity instanceof Byte) {
                pnlTargets.add(l.label, "w 100px!, h 30px!, gap 5px 5px 0 5px");
            } else {
                pnlTargets.add(l.label, "w 145px!, h 30px!, gap 5px 5px 0 5px");
            }
        }

        btnOK.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent arg0) { finish(); } });
        btnReset.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent arg0) { resetAssignedAmount(); initialAssignAmount(); } });

            // Final UI layout
        pnlMain.setLayout(new MigLayout("insets 0, gap 0, wrap 2, ax center"));
        pnlMain.add(pnlSource, "w 125px!, h 160px!, gap 50px 0 0 15px");
        pnlMain.add(pnlInfo, "gap 20px 0 0 15px");
        pnlMain.add(scrTargets, "w 96%!, gap 2% 0 0 0, pushy, growy, ax center, span 2");

        final JPanel pnlButtons = new JPanel(new MigLayout("insets 0, gap 0, ax center"));
        pnlButtons.setOpaque(false);
        pnlButtons.add(btnOK, "w 110px!, h 30px!, gap 0 10px 0 0");
        pnlButtons.add(btnReset, "w 110px!, h 30px!");

        pnlMain.add(pnlButtons, "ax center, w 350px!, gap 10px 10px 10px 10px, span 2");
        overlay.add(pnlMain);

        pnlMain.getRootPane().setDefaultButton(btnOK);

        initialAssignAmount();
        SOverlayUtils.showOverlay();

        dlg.setUndecorated(true);
        dlg.setContentPane(pnlMain);
        dlg.setSize(new Dimension(wDlg, hDlg));
        dlg.setLocation((overlay.getWidth() - wDlg) / 2, (overlay.getHeight() - hDlg) / 2);
        dlg.setModalityType(ModalityType.APPLICATION_MODAL);
        dlg.setVisible(true);
    }

    private void addPanelForTarget(final JPanel pnlTargets, final AssignTarget at) {
        if (at.entity instanceof CardView) {
            final CardPanel cp = new CardPanel(matchUI, (CardView)at.entity);
            cp.setCardBounds(0, 0, 105, 150);
            cp.setOpaque(true);
            pnlTargets.add(cp, "w 145px!, h 170px!, gap 5px 5px 3px 3px, ax center");
            cp.addMouseListener(mad);
            targetsMap.put(cp, at);
        } else if (at.entity instanceof PlayerView) {
            final PlayerView p = (PlayerView)at.entity;
            SkinImage playerAvatar = matchUI.getPlayerAvatar(p, 0);
            final MiscCardPanel mp = new MiscCardPanel(matchUI, p.getName(), playerAvatar);
            mp.setCardBounds(0, 0, 105, 150);
            pnlTargets.add(mp, "w 145px!, h 170px!, gap 5px 5px 3px 3px, ax center");
            mp.addMouseListener(mad);
            targetsMap.put(mp, at);
        } else if (at.entity instanceof Byte) {
            SkinImage manaSymbol;
            Byte color = (Byte) at.entity;
            if (color == MagicColor.WHITE) {
                manaSymbol = FSkin.getImage(FSkinProp.IMG_MANA_W);
            } else if (color == MagicColor.BLUE) {
                manaSymbol = FSkin.getImage(FSkinProp.IMG_MANA_U);
            } else if (color == MagicColor.BLACK) {
                manaSymbol = FSkin.getImage(FSkinProp.IMG_MANA_B);
            } else if (color == MagicColor.RED) {
                manaSymbol = FSkin.getImage(FSkinProp.IMG_MANA_R);
            } else if (color == MagicColor.GREEN) {
                manaSymbol = FSkin.getImage(FSkinProp.IMG_MANA_G);
            } else { // Should never come here, but add this to avoid compile error
                manaSymbol = FSkin.getImage(FSkinProp.IMG_MANA_COLORLESS);
            }
            final MiscCardPanel mp = new MiscCardPanel(matchUI, "", manaSymbol);
            mp.setCardBounds(0, 0, 70, 70);
            pnlTargets.add(mp, "w 100px!, h 150px!, gap 5px 5px 3px 3px, ax center");
            mp.addMouseListener(mad);
            targetsMap.put(mp, at);
        }
        targetsList.add(at);
    }

    private void assignAmountTo(AssignTarget at, final boolean meta, final boolean isAdding) {
        int assigned = at.amount;
        int leftToAssign = Math.max(0, at.max - assigned);
        int amountToAdd = isAdding ? 1 : -1;
        int remainingAmount = Math.min(getRemainingAmount(), leftToAssign);
        // Left click adds, right click substracts.
        // Hold Ctrl to assign to maximum amount
        if (meta)  {
            if (isAdding) {
                amountToAdd = leftToAssign > 0 ? leftToAssign : 0;
            } else {
                amountToAdd = -assigned;
            }
        }

        if (amountToAdd > remainingAmount) {
            amountToAdd = remainingAmount;
        }
        if (atLeastOne && assigned + amountToAdd < 1) {
            amountToAdd = 1 - assigned;
        }

        if (0 == amountToAdd || amountToAdd + assigned < 0) {
            return;
        }

        addAssignedAmount(at, amountToAdd);
        updateLabels();
    }

    private void initialAssignAmount() {
        if (!atLeastOne) {
            updateLabels();
            return;
        }

        for(AssignTarget at : targetsList) {
            addAssignedAmount(at, 1);
        }
        updateLabels();
    }

    private void resetAssignedAmount() {
        for(AssignTarget at : targetsList)
            at.amount = 0;
    }

    private void addAssignedAmount(final AssignTarget at, int addedAmount) {
        // If we don't have enough left or we're trying to unassign too much return
        final int canAssign = getRemainingAmount();
        if (canAssign < addedAmount) {
            addedAmount = canAssign;
        }

        at.amount = Math.max(0, addedAmount + at.amount);
    }

    private int getRemainingAmount() {
        int spent = 0;
        for(AssignTarget at : targetsList) {
            spent += at.amount;
        }
        return totalAmountToAssign - spent;
    }

    /** Updates labels and other UI elements.*/
    private void updateLabels() {
        int amountLeft = totalAmountToAssign;

        for ( AssignTarget at : targetsList )
        {
            amountLeft -= at.amount;
            StringBuilder sb = new StringBuilder();
            sb.append(at.amount);
            if (at.max - at.amount == 0) {
                sb.append(" (").append(localizer.getMessage("lblMax")).append(")");
            }
            at.label.setText(sb.toString());
        }

        lblTotalAmount.setText(TextUtil.concatNoSpace(localizer.getMessage("lblAvailableAmount", lblAmount), ": " , String.valueOf(amountLeft), " (of ", String.valueOf(totalAmountToAssign), ")"));
        btnOK.setEnabled(amountLeft == 0);
    }

    private void finish() {
        if ( getRemainingAmount() > 0 )
            return;

        dlg.dispose();
        SOverlayUtils.hideOverlay();
    }

    public Map<Object, Integer> getAssignedMap() {
        Map<Object, Integer> result = new HashMap<>(targetsList.size());
        for (AssignTarget at : targetsList)
            result.put(at.entity, at.amount);
        return result;
    }
}
