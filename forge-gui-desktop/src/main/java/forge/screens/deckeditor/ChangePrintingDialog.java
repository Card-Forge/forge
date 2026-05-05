package forge.screens.deckeditor;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import com.google.common.collect.ImmutableList;

import forge.StaticData;
import forge.card.CardEdition;
import forge.gui.FThreads;
import forge.item.PaperCard;
import forge.toolbox.FComboBox;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FTextField;
import forge.toolbox.special.CardImageGrid;
import forge.util.Localizer;

public final class ChangePrintingDialog {

    private static final int THUMB_W = 200;
    private static final int THUMB_H = 280;
    private static final int COLUMNS = 4;
    private static final int SCROLLBAR_W = 16;
    // Empirical buffer: FScrollPane's themed border eats ~4px; without this, 4*cellW+SCROLLBAR_W rounds down to 3 cols.
    private static final int VIEWPORT_BUFFER = 12;
    private static final int CONTAINER_H = 736;          // 700 grid + 28 search bar + 8 gap
    private static final int SEARCH_DEBOUNCE_MS = 200;

    private ChangePrintingDialog() {}

    public static PaperCard show(final PaperCard current) {
        FThreads.assertExecutedByEdt(true);

        final List<PaperCard> printings = StaticData.instance().getCommonCards().getAllCardsNoAlt(current.getName());
        if (printings.size() <= 1) {
            return null;
        }
        printings.sort(printingComparator());

        // Match mobile: pull the current printing to the top so it's the first thing the user sees.
        final PaperCard currentNonFoil = current.isFoil() ? current.getUnFoiled() : current;
        if (printings.remove(currentNonFoil)) {
            printings.add(0, currentNonFoil);
        }

        final CardImageGrid grid = new CardImageGrid(COLUMNS, THUMB_W, THUMB_H);
        grid.setItems(printings);
        grid.setSelected(currentNonFoil);

        final Localizer localizer = Localizer.getInstance();

        final FTextField txtSearch = new FTextField.Builder()
                .ghostText(localizer.getMessage("lblChangePrintingSearchHint"))
                .build();
        final FComboBox<ArtStyle> cbStyle = new FComboBox<>(ArtStyle.values());

        final CardLayout centerLayout = new CardLayout();
        final JPanel centerPanel = new JPanel(centerLayout);
        centerPanel.setOpaque(false);
        final FLabel emptyLabel = new FLabel.Builder()
                .text(localizer.getMessage("lblChangePrintingNoResults"))
                .fontSize(14)
                .build();
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        centerPanel.add(grid.getComponent(), "list");
        centerPanel.add(emptyLabel, "empty");

        final Runnable applyFilter = () -> {
            final String query = txtSearch.getText().toLowerCase().trim();
            final ArtStyle style = (ArtStyle) cbStyle.getSelectedItem();
            final List<PaperCard> filtered = printings.stream()
                    .filter(pc -> query.isEmpty() || matchesSet(pc, query))
                    .filter(pc -> style == null || style.matches(pc))
                    .collect(Collectors.toList());
            grid.setItems(filtered);
            centerLayout.show(centerPanel, grid.isEmpty() ? "empty" : "list");
        };
        final Timer searchTimer = new Timer(SEARCH_DEBOUNCE_MS, e -> applyFilter.run());
        searchTimer.setRepeats(false);
        txtSearch.addChangeListener(new FTextField.ChangeListener() {
            @Override public void textChanged() { searchTimer.restart(); }
        });
        cbStyle.addActionListener(e -> applyFilter.run());

        final JPanel topBar = new JPanel(new BorderLayout(8, 0));
        topBar.setOpaque(false);
        topBar.add(txtSearch, BorderLayout.CENTER);
        topBar.add(cbStyle, BorderLayout.EAST);

        final int containerW = COLUMNS * grid.getCellWidth() + SCROLLBAR_W + VIEWPORT_BUFFER;
        final Dimension fixedSize = new Dimension(containerW, CONTAINER_H);
        final JPanel container = new JPanel(new BorderLayout(0, 8)) {
            private static final long serialVersionUID = 1L;
            @Override public Dimension getPreferredSize() { return fixedSize; }
            @Override public Dimension getMinimumSize()   { return fixedSize; }
            @Override public Dimension getMaximumSize()   { return fixedSize; }
        };
        container.setOpaque(false);
        container.add(topBar, BorderLayout.NORTH);
        container.add(centerPanel, BorderLayout.CENTER);

        // Route through getMessage(key, args) so Localizer's UTF-8 round-trip handles the em-dash.
        final String title = localizer.getMessage("lblChangePrintingDialogTitle", current.getName());

        final FOptionPane optionPane = new FOptionPane(null, title, null, container,
                ImmutableList.of(localizer.getMessage("lblOK"), localizer.getMessage("lblCancel")), 0);

        grid.addDoubleClickListener(pc -> optionPane.setResult(0));

        optionPane.setVisible(true);
        final int result = optionPane.getResult();
        optionPane.dispose();
        grid.dispose();

        if (result != 0) {
            return null;
        }
        return grid.getSelected();
    }

    private static Comparator<PaperCard> printingComparator() {
        return Comparator.comparing((PaperCard pc) -> {
                    CardEdition ed = StaticData.instance().getEditions().get(pc.getEdition());
                    return ed != null ? ed.getDate() : null;
                }, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(PaperCard::getCollectorNumber, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private static boolean matchesSet(PaperCard pc, String lowerQuery) {
        final String editionCode = pc.getEdition();
        if (editionCode != null && editionCode.toLowerCase().contains(lowerQuery)) {
            return true;
        }
        final CardEdition edition = StaticData.instance().getEditions().get(editionCode);
        return edition != null && edition.getName().toLowerCase().contains(lowerQuery);
    }

    /** Edition-section categories the user can filter by. {@code section} = null means no filter (All). */
    private enum ArtStyle {
        ALL("lblArtStyleAll", null),
        STANDARD("lblArtStyleStandard", CardEdition.EditionSectionWithCollectorNumbers.CARDS.getName()),
        BORDERLESS("lblArtStyleBorderless", CardEdition.EditionSectionWithCollectorNumbers.BORDERLESS.getName()),
        FULL_ART("lblArtStyleFullArt", CardEdition.EditionSectionWithCollectorNumbers.FULL_ART.getName()),
        SHOWCASE("lblArtStyleShowcase", CardEdition.EditionSectionWithCollectorNumbers.SHOWCASE.getName()),
        EXTENDED_ART("lblArtStyleExtendedArt", CardEdition.EditionSectionWithCollectorNumbers.EXTENDED_ART.getName()),
        RETRO_FRAME("lblArtStyleRetroFrame", CardEdition.EditionSectionWithCollectorNumbers.RETRO_FRAME.getName()),
        PROMO("lblArtStylePromo", CardEdition.EditionSectionWithCollectorNumbers.PROMO.getName());

        private final String labelKey;
        private final String section;

        ArtStyle(String labelKey, String section) {
            this.labelKey = labelKey;
            this.section = section;
        }

        boolean matches(PaperCard pc) {
            if (section == null) return true;
            final CardEdition edition = StaticData.instance().getEditions().get(pc.getEdition());
            return edition != null && section.equalsIgnoreCase(edition.getSectionForCollectorNumber(pc.getCollectorNumber()));
        }

        @Override
        public String toString() {
            return Localizer.getInstance().getMessage(labelKey);
        }
    }

}
