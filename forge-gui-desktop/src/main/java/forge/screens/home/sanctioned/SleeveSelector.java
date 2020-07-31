package forge.screens.home.sanctioned;

import forge.gui.WrapLayout;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.view.FDialog;
import forge.util.Localizer;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class SleeveSelector extends FDialog {
    private final List<FLabel> selectables = new ArrayList<>();
    private final Map<Integer, FSkin.SkinImage> sleeveMap = FSkin.getSleeves();

    public SleeveSelector(final String playerName, final int currentIndex, final Collection<Integer> usedIndices) {
        this.setTitle(Localizer.getInstance().getMessage("lblSelectSleevesFroPlayer", playerName));

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

        final Dimension size = new Dimension(60, 80);
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

    public List<FLabel> getSelectables() {
        return this.selectables;
    }
}
