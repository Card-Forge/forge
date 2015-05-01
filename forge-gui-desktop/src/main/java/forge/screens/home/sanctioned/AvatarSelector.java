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
import forge.toolbox.FSkin.SkinImage;
import forge.view.FDialog;

@SuppressWarnings("serial")
public class AvatarSelector extends FDialog {
    private final List<FLabel> selectables = new ArrayList<FLabel>();
    private final Map<Integer, SkinImage> avatarMap = FSkin.getAvatars();

    public AvatarSelector(final String playerName, final int currentIndex, final Collection<Integer> usedIndices) {
        this.setTitle("Select avatar for " + playerName);

        final JPanel pnlAvatarPics = new JPanel(new WrapLayout());

        pnlAvatarPics.setOpaque(false);
        pnlAvatarPics.setOpaque(false);

        final FLabel initialSelection = makeAvatarLabel(avatarMap.get(currentIndex), currentIndex, currentIndex);
        pnlAvatarPics.add(initialSelection);
        for (final Integer i : avatarMap.keySet()) {
            //if (!usedIndices.contains(i)) {  // Decided to allow duplicate avatars when manually selecting
            if (currentIndex != i) {
                pnlAvatarPics.add(makeAvatarLabel(avatarMap.get(i), i, currentIndex));
            }
        }

        final int width = this.getOwner().getWidth() * 3 / 4;
        final int height = this.getOwner().getHeight() * 3 / 4;
        this.setPreferredSize(new Dimension(width, height));
        this.setSize(width, height);

        final FScrollPane scroller = new FScrollPane(pnlAvatarPics, false);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.add(scroller, "w 100%-24px!, pushy, growy, gap 12px 0 0 0");
        this.setDefaultFocus(initialSelection);
    }

    private FLabel makeAvatarLabel(final SkinImage img0, final int index0, final int oldIndex) {
        final FLabel lbl = new FLabel.Builder().icon(img0).iconScaleFactor(0.95).iconAlignX(SwingConstants.CENTER)
                .iconInBackground(true).hoverable(true).selectable(true).selected(oldIndex == index0)
                .unhoveredAlpha(oldIndex == index0 ? 0.9f : 0.7f).build();

        final Dimension size = new Dimension(80, 80);
        lbl.setPreferredSize(size);
        lbl.setMaximumSize(size);
        lbl.setMinimumSize(size);
        lbl.setName("AvatarLabel" + index0);

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
