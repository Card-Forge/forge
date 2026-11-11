package forge.screens.home;

import java.util.Arrays;

import javax.swing.JPanel;

import forge.card.DraftOptions;
import forge.gamemodes.limited.BoosterDraft;
import forge.toolbox.FComboBox;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

/** The picks per pack setting of the Draft Settings dialog. */
public class DraftPicksSelector {
    private final BoosterDraft draft;
    private final FComboBox<String> combo = new FComboBox<>();

    public DraftPicksSelector(final BoosterDraft draft, final int podSize) {
        this.draft = draft;
        refresh(podSize);
    }

    /** A set's pick rule can depend on pod size, so both the marker and the selection follow it. */
    public void refresh(final int podSize) {
        final DraftOptions.DoublePick defaultPicks = draft.getDefaultPicks(podSize);
        combo.removeAllItems();
        for (DraftOptions.DoublePick rule : BoosterDraft.PICK_RULES) {
            combo.addItem(BoosterDraft.markSetDefault(BoosterDraft.picksLabel(rule), rule == defaultPicks));
        }
        combo.setSelectedIndex(BoosterDraft.PICK_RULES.indexOf(defaultPicks));
    }

    public void addTo(final JPanel panel) {
        final Localizer localizer = Localizer.getInstance();
        panel.add(new FLabel.Builder().fontSize(12).text(localizer.getMessage("lblDraftPicksLabel")).build(), "gaptop 10");
        panel.add(combo, "w 200!");
        panel.add(new FLabel.Builder().fontSize(11).text(localizer.getMessage("lblDraftPicksHelp")).build());
    }

    public DraftOptions.DoublePick getSelected() {
        return BoosterDraft.PICK_RULES.get(combo.getSelectedIndex());
    }

    /** Shows the Draft Settings dialog for an offline draft. Returns false if cancelled. */
    public static boolean showDialog(final BoosterDraft draft) {
        final Localizer localizer = Localizer.getInstance();
        final DraftPicksSelector selector = new DraftPicksSelector(draft, draft.getPodSize());
        final JPanel panel = new JPanel(new MigLayout("insets 4, gap 2 4, wrap 1"));
        panel.setOpaque(false);
        selector.addTo(panel);
        final int result = FOptionPane.showOptionDialog(null, localizer.getMessage("lblDraftSettingsTitle"), null, panel,
                Arrays.asList(localizer.getMessage("lblOK"), localizer.getMessage("lblCancel")));
        if (result != 0) {
            return false;
        }
        draft.setDoublePick(selector.getSelected());
        return true;
    }
}
