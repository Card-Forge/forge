package forge.screens.home.quest;

import forge.assets.FSkinProp;
import forge.card.CardEdition;
import forge.gui.SOverlayUtils;
import forge.model.FModel;
import forge.toolbox.*;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DialogChooseSets {
    private final List<String> selectedSets = new ArrayList<>();
    private boolean wantReprints = true;
    private Runnable okCallback;

    private final List<FCheckBox> choices = new ArrayList<>();
    private final FCheckBox cbWantReprints = new FCheckBox("Allow compatible reprints from other sets");

    // lists are of set codes (e.g. "2ED")
    public DialogChooseSets(
            Collection<String> preselectedSets, Collection<String> unselectableSets, boolean showWantReprintsCheckbox) {

        // create a local copy of the editions list so we can sort it
        List<CardEdition> editions = new ArrayList<>();
        for (CardEdition ce : FModel.getMagicDb().getEditions()) {
            editions.add(ce);
        }
        Collections.sort(editions);
        Collections.reverse(editions);

        List<FCheckBox> coreSets = new ArrayList<>();
        List<FCheckBox> expansionSets = new ArrayList<>();
        List<FCheckBox> otherSets = new ArrayList<>();

        for (CardEdition ce : editions) {
            String code = ce.getCode();
            FCheckBox box = new FCheckBox(String.format("%s (%s)", ce.getName(), code));
            box.setName(code);
            box.setSelected(null != preselectedSets && preselectedSets.contains(code));
            box.setEnabled(null == unselectableSets || !unselectableSets.contains(code));
            switch (ce.getType()) {
            case CORE: coreSets.add(box); break;
            case EXPANSION: expansionSets.add(box); break;
            default: otherSets.add(box); break;
            }
        }

        FPanel p = new FPanel(new MigLayout("insets 0, gap 0, center, wrap 3"));
        p.setOpaque(false);
        p.setBackgroundTexture(FSkin.getIcon(FSkinProp.BG_TEXTURE));

        p.add(new FLabel.Builder().text("Choose sets").fontSize(18).build(), "center, span, wrap, gaptop 10");

        String constraints = "aligny top";
        p.add(makeCheckBoxList(coreSets, "Core sets", true), constraints);
        p.add(makeCheckBoxList(expansionSets, "Expansions", false), constraints);
        p.add(makeCheckBoxList(otherSets, "Other sets", false), constraints);

        final JPanel overlay = FOverlay.SINGLETON_INSTANCE.getPanel();
        overlay.setLayout(new MigLayout("insets 0, gap 0, wrap, ax center, ay center"));

        final Runnable cleanup = new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.hideOverlay();
            }
        };

        FButton btnOk = new FButton("OK");
        btnOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                cleanup.run();
                handleOk();
            }
        });

        FButton btnCancel = new FButton("Cancel");
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cleanup.run();
            }
        });

        JPanel southPanel = new JPanel(new MigLayout("insets 10, gap 20, ax center"));
        southPanel.setOpaque(false);
        if (showWantReprintsCheckbox) {
            southPanel.add(cbWantReprints, "center, span, wrap");
        }
        southPanel.add(btnOk, "center, w 40%, h 20!");
        southPanel.add(btnCancel, "center, w 40%, h 20!");

        p.add(southPanel, "dock south, gapBottom 10");

        overlay.add(p);
        p.getRootPane().setDefaultButton(btnOk);
        SOverlayUtils.showOverlay();
    }

    public void setOkCallback(Runnable onOk) {
        okCallback = onOk;
    }

    // result accessors
    public List<String> getSelectedSets() { return selectedSets; }
    public boolean      getWantReprints() { return wantReprints; }

    private JPanel makeCheckBoxList(List<FCheckBox> sets, String title, boolean focused) {
        choices.addAll(sets);
        final FCheckBoxList<FCheckBox> cbl = new FCheckBoxList<>(false);
        cbl.setListData(sets.toArray(new FCheckBox[sets.size()]));
        cbl.setVisibleRowCount(Math.min(20, sets.size()));

        if (focused) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    cbl.requestFocusInWindow();
                }
            });
        }

        JPanel pnl = new JPanel(new MigLayout("center, wrap"));
        pnl.setOpaque(false);
        pnl.add(new FLabel.Builder().text(title).build());
        pnl.add(new FScrollPane(cbl, true));
        return pnl;
    }

    private void handleOk() {
        for (FCheckBox box : choices) {
            if (box.isSelected()) {
                selectedSets.add(box.getName());
            }

            wantReprints = cbWantReprints.isSelected();
        }

        if (null != okCallback) {
            okCallback.run();
        }
    }
}
