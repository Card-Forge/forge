package forge.screens.home.sanctioned;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import forge.ImageCache;
import forge.gui.UiCommand;
import forge.gui.WrapLayout;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.deckeditor.CardArtSleeveDialog;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import forge.util.SleeveArt;
import forge.view.FDialog;

/**
 * Modal lobby dialog for choosing a player's deck-back sleeve. Shows two sections: a "Card Art
 * Sleeves" library of saved card art (add via the picker, remove via right-click), and the built-in
 * sprite sleeves. The dialog records the chosen result; the caller reads it after close:
 * a built-in index ({@link #getResultIndex()} &gt;= 0) or a card image key ({@link #getResultArtKey()}).
 */
@SuppressWarnings("serial")
public class SleeveSelector extends FDialog {
    private final Map<Integer, FSkin.SkinImage> sleeveMap = FSkin.getSleeves();
    private final JPanel pnlCardArt = new JPanel(new WrapLayout(WrapLayout.LEFT, 6, 6));
    private final int currentIndex;
    private final String currentArtKey;

    private int resultIndex = -1;
    private String resultArtKey = null;
    private int resultOffset = 500;

    public SleeveSelector(final String playerName, final int currentIndex, final String currentArtKey,
            final Collection<Integer> usedIndices) {
        this.currentIndex = currentIndex;
        this.currentArtKey = currentArtKey == null ? "" : currentArtKey;
        final Localizer localizer = Localizer.getInstance();
        this.setTitle(localizer.getMessage("lblSelectSleeveForPlayer", playerName));

        final JPanel content = new JPanel(new MigLayout("fillx, wrap 1, insets 0, gapy 4"));
        content.setOpaque(false);

        content.add(sectionHeader(localizer.getMessage("lblCardArtSleeves")), "growx");
        pnlCardArt.setOpaque(false);
        for (final Map.Entry<String, Integer> entry : SleeveArt.parseLibrary(
                FModel.getPreferences().getPref(FPref.UI_SLEEVE_ART_LIBRARY)).entrySet()) {
            pnlCardArt.add(makeCardArtLabel(entry.getKey(), entry.getValue()));
        }
        pnlCardArt.add(makeAddTile());
        content.add(pnlCardArt, "growx");

        content.add(sectionHeader(localizer.getMessage("lblDefaultSleeves")), "growx, gaptop 10");
        final JPanel pnlBuiltIn = new JPanel(new WrapLayout(WrapLayout.LEFT, 6, 6));
        pnlBuiltIn.setOpaque(false);
        for (final Integer i : sleeveMap.keySet()) {
            pnlBuiltIn.add(makeBuiltInLabel(sleeveMap.get(i), i));
        }
        content.add(pnlBuiltIn, "growx");

        final int width = this.getOwner().getWidth() * 3 / 4;
        final int height = this.getOwner().getHeight() * 3 / 4;
        this.setPreferredSize(new Dimension(width, height));
        this.setSize(width, height);

        final FScrollPane scroller = new FScrollPane(content, false);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.add(scroller, "w 100%-24px!, pushy, growy, gap 12px 0 0 0");
    }

    // A blank placeholder card tile with a "+" that opens the card-art picker
    private FLabel makeAddTile() {
        final FLabel lbl = new FLabel.Builder().text("+").fontSize(48).fontAlign(SwingConstants.CENTER)
                .hoverable(true).build();
        sizeTile(lbl);
        lbl.setToolTipText(Localizer.getInstance().getMessage("lblUseCardArtSleeve"));
        lbl.setBorder(new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_BORDERS), 2));
        lbl.setCommand((UiCommand) this::addCardArt);
        return lbl;
    }

    public int getResultIndex() {
        return resultIndex;
    }
    public String getResultArtKey() {
        return resultArtKey;
    }
    public int getResultOffset() {
        return resultOffset;
    }

    private static JLabel sectionHeader(final String text) {
        final JLabel lbl = new JLabel(text);
        lbl.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor());
        return lbl;
    }

    private FLabel makeBuiltInLabel(final FSkin.SkinImage img, final int index) {
        final FLabel lbl = new FLabel.Builder().icon(img).iconScaleFactor(0.95).iconAlignX(SwingConstants.CENTER)
                .iconInBackground(true).hoverable(true).selectable(true)
                .selected(currentArtKey.isEmpty() && currentIndex == index).build();
        sizeTile(lbl);
        lbl.setCommand((UiCommand) () -> {
            resultIndex = index;
            resultArtKey = null;
            setVisible(false);
        });
        return lbl;
    }

    private FLabel makeCardArtLabel(final String key, final int offset) {
        final FLabel lbl = new FLabel.Builder().iconScaleFactor(0.95).iconAlignX(SwingConstants.CENTER)
                .iconInBackground(true).hoverable(true).selectable(true).selected(key.equals(currentArtKey)).build();
        sizeTile(lbl);
        lbl.setToolTipText(Localizer.getInstance().getMessage("lblCardArtSleeveTip"));
        refreshCardArtIcon(lbl, key, offset);
        lbl.setCommand((UiCommand) () -> {
            resultArtKey = key;
            resultOffset = offset;
            setVisible(false);
        });
        lbl.setRightClickCommand((UiCommand) () -> removeCardArt(key, lbl));
        return lbl;
    }

    private static void sizeTile(final FLabel lbl) {
        final Dimension size = new Dimension(116, 160);
        lbl.setPreferredSize(size);
        lbl.setMaximumSize(size);
        lbl.setMinimumSize(size);
    }

    private void refreshCardArtIcon(final FLabel lbl, final String key, final int offset) {
        final BufferedImage art = ImageCache.getSleeveArtCropped(key, offset);
        if (art != null) {
            lbl.setIcon(new FSkin.UnskinnedIcon(art));
            lbl.repaintSelf();
        } else {
            ImageCache.fetchSleeveArt(key, () -> refreshCardArtIcon(lbl, key, offset));
        }
    }

    private void addCardArt() {
        final CardArtSleeveDialog.Result chosen = CardArtSleeveDialog.show();
        if (chosen == null) {
            return;
        }
        final String key = chosen.card.getImageKey(false);
        final LinkedHashMap<String, Integer> library = SleeveArt.parseLibrary(
                FModel.getPreferences().getPref(FPref.UI_SLEEVE_ART_LIBRARY));
        library.put(key, chosen.offset); // overwrite any prior framing for this art
        FModel.getPreferences().setPref(FPref.UI_SLEEVE_ART_LIBRARY, SleeveArt.formatLibrary(library));
        FModel.getPreferences().save();
        resultArtKey = key;
        resultOffset = chosen.offset;
        setVisible(false);
    }

    private void removeCardArt(final String key, final FLabel lbl) {
        final LinkedHashMap<String, Integer> library = SleeveArt.parseLibrary(
                FModel.getPreferences().getPref(FPref.UI_SLEEVE_ART_LIBRARY));
        library.remove(key);
        FModel.getPreferences().setPref(FPref.UI_SLEEVE_ART_LIBRARY, SleeveArt.formatLibrary(library));
        FModel.getPreferences().save();
        pnlCardArt.remove(lbl);
        pnlCardArt.revalidate();
        pnlCardArt.repaint();
    }
}
