package forge.screens.home.sanctioned;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import forge.gui.WrapLayout;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import forge.view.FDialog;

/**
 * Modal lobby dialog for choosing a player's card-back sleeve, shown as a grid of the
 * available built-in sleeves. The dialog only builds the grid; the caller wires the
 * click behaviour through {@link #getSelectables()}.
 */
@SuppressWarnings("serial")
public class SleeveSelector extends FDialog {
    private final List<FLabel> selectables = new ArrayList<>();
    private final Map<Integer, FSkin.SkinImage> sleeveMap = FSkin.getSleeves();

    /**
     * @param playerName   shown in the dialog title
     * @param currentIndex the sleeve currently chosen; it is highlighted and given default focus
     * @param usedIndices  sleeve indices already in use by other players
     */
    public SleeveSelector(final String playerName, final int currentIndex, final Collection<Integer> usedIndices) {
        this.setTitle(Localizer.getInstance().getMessage("lblSelectSleeveForPlayer", playerName));

        final JPanel pnlSleevePics = new JPanel(new WrapLayout());

        pnlSleevePics.setOpaque(false);
        pnlSleevePics.setOpaque(false);

        final FLabel initialSelection = makeSleeveLabel(sleeveMap.get(currentIndex), currentIndex, currentIndex);
        pnlSleevePics.add(initialSelection);
        for (final Integer i : sleeveMap.keySet()) {
            if (currentIndex != i) {
                pnlSleevePics.add(makeSleeveLabel(sleeveMap.get(i), i, currentIndex));
            }
        }

        final int width = this.getOwner().getWidth() * 3 / 4;
        final int height = this.getOwner().getHeight() * 3 / 4;
        this.setPreferredSize(new Dimension(width, height));
        this.setSize(width, height);

        final FScrollPane scroller = new FScrollPane(pnlSleevePics, false);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.add(scroller, "w 100%-24px!, pushy, growy, gap 12px 0 0 0");
        this.setDefaultFocus(initialSelection);
    }

    private FLabel makeSleeveLabel(final FSkin.SkinImage img0, final int index0, final int oldIndex) {
        final FLabel lbl = new FLabel.Builder().icon(img0).iconScaleFactor(0.95).iconAlignX(SwingConstants.CENTER)
                .iconInBackground(true).hoverable(true).selectable(true).selected(oldIndex == index0)
                .unhoveredAlpha(oldIndex == index0 ? 0.9f : 0.7f).build();

        final Dimension size = new Dimension(116, 160);
        lbl.setPreferredSize(size);
        lbl.setMaximumSize(size);
        lbl.setMinimumSize(size);
        lbl.setName("SleeveLabel" + index0);

        if (oldIndex == index0) {
            lbl.setBorder(new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_BORDERS).alphaColor(255), 3));
        }

        selectables.add(lbl);

        return lbl;
    }

    /**
     * The selectable sleeve labels, one per sleeve. Each label is named {@code "SleeveLabel" + index};
     * callers parse that index (e.g. via {@code name.substring(11)}) to apply the chosen sleeve.
     */
    public List<FLabel> getSelectables() {
        return this.selectables;
    }
}
