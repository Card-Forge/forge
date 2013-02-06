package forge.gui.home.quest;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import net.miginfocom.swing.MigLayout;
import forge.Singletons;
import forge.card.CardEdition;
import forge.gui.toolbox.FButton;
import forge.gui.toolbox.FCheckBox;
import forge.gui.toolbox.FCheckBoxList;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FSkin;

@SuppressWarnings("serial")
public class DialogChooseSets extends JDialog {
    private final List<String> selectedSets = new ArrayList<String>();
    private boolean wantReprints = true;
    private Runnable okCallback;
    
    private final List<FCheckBox> choices = new ArrayList<FCheckBox>();
    private final FCheckBox cbWantReprints = new FCheckBox("Allow compatible reprints from other sets");

    // lists are of set codes (e.g. "2ED")
    public DialogChooseSets(
            Collection<String> preselectedSets, Collection<String> unselectableSets, boolean showWantReprintsCheckbox) {
        
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setSize(775, 575);
        this.setLocationRelativeTo(null);
        this.setTitle("Choose sets");
        
        // create a local copy of the editions list so we can sort it
        List<CardEdition> editions = new ArrayList<CardEdition>();
        for (CardEdition ce : Singletons.getModel().getEditions()) {
            editions.add(ce);
        }
        Collections.sort(editions);
        Collections.reverse(editions);
        
        List<FCheckBox> coreSets = new ArrayList<FCheckBox>();
        List<FCheckBox> expansionSets = new ArrayList<FCheckBox>();
        List<FCheckBox> otherSets = new ArrayList<FCheckBox>();
        
        for (CardEdition ce : editions) {
            String code = ce.getCode();
            FCheckBox box = new FCheckBox(String.format("%s (%s)", ce.getName(), code));
            box.setName(code);
            box.setSelected(null == preselectedSets ? false : preselectedSets.contains(code));
            box.setEnabled(null == unselectableSets ? true : !unselectableSets.contains(code));
            switch (ce.getType()) {
            case CORE: coreSets.add(box); break;
            case EXPANSION: expansionSets.add(box); break;
            default: otherSets.add(box); break;
            }
        }
        
        FPanel p = new FPanel(new MigLayout("insets 0, gap 0, center, wrap 3"));
        p.setOpaque(false);
        p.setBackgroundTexture(FSkin.getIcon(FSkin.Backgrounds.BG_TEXTURE));
        
        String constraints = "aligny top";
        p.add(makeCheckBoxList(coreSets, "Core sets"), constraints);
        p.add(makeCheckBoxList(expansionSets, "Expansions"), constraints);
        p.add(makeCheckBoxList(otherSets, "Other sets"), constraints);
        
        FButton btnOk = new FButton("OK");
        this.getRootPane().setDefaultButton(btnOk);
        btnOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                handleOk();
            }
        });

        FButton btnCancel = new FButton("Cancel");
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        JPanel southPanel = new JPanel(new MigLayout("insets 10, gap 20, ax center"));
        southPanel.setOpaque(false);
        if (showWantReprintsCheckbox) {
            southPanel.add(cbWantReprints, "center, span, wrap");
        }
        southPanel.add(btnOk, "center, w 40%, h 20!");
        southPanel.add(btnCancel, "center, w 40%, h 20!");
        
        p.add(southPanel, "dock south");
      
        JScrollPane scrollPane = new JScrollPane(p);
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        this.setVisible(true);
    }
    
    public void setOkCallback(Runnable onOk) {
        okCallback = onOk;
    }
    
    // result accessors
    public List<String> getSelectedSets() { return selectedSets; }
    public boolean      getWantReprints() { return wantReprints; }

    private JPanel makeCheckBoxList(List<FCheckBox> sets, String title) {
        choices.addAll(sets);
        FCheckBoxList cbl = new FCheckBoxList(false);
        cbl.setListData(sets.toArray());
        cbl.setVisibleRowCount(Math.min(20, sets.size()));
        
        JPanel pnl = new JPanel(new MigLayout("center, wrap"));
        pnl.setOpaque(false);
        pnl.add(new FLabel.Builder().text(title).build());
        pnl.add(new JScrollPane(cbl));
        return pnl;
    }
    
    private void handleOk() {
        for (FCheckBox box : choices) {
            if (box.isSelected()) {
                selectedSets.add(box.getName());
            }
            
            wantReprints = cbWantReprints.isSelected();
        }
        
        try {
            if (null != okCallback) {
                okCallback.run();
            }
        } finally {
            dispose();
        }
    }
}
