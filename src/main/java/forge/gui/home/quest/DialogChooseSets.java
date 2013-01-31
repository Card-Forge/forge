package forge.gui.home.quest;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import net.miginfocom.swing.MigLayout;
import forge.Singletons;
import forge.card.CardEdition;

@SuppressWarnings("serial")
public class DialogChooseSets extends JDialog {
    private final List<String> selectedSets = new ArrayList<String>();
    private boolean wantReprints = true;
    private Runnable okCallback;
    
    private final List<JCheckBox> choices = new ArrayList<JCheckBox>();
    private final JCheckBox cbWantReprints = new JCheckBox("Allow compatible reprints from other sets");

    // lists are of set codes (e.g. "2ED")
    public DialogChooseSets(
            Collection<String> preselectedSets, Collection<String> unselectableSets, boolean showWantReprintsCheckbox) {
        
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setSize(800, 600);
        this.setLocationRelativeTo(null);
        this.setTitle("Choose sets");
        
        // create a local copy of the editions list so we can sort it
        List<CardEdition> editions = new ArrayList<CardEdition>();
        for (CardEdition ce : Singletons.getModel().getEditions()) {
            editions.add(ce);
        }
        Collections.sort(editions);
        Collections.reverse(editions);
        
        JPanel p = new JPanel(new MigLayout("insets 0, gap 0, center, wrap 3"));
        for (CardEdition ce : editions) {
            String code = ce.getCode();
            JCheckBox box = new JCheckBox(String.format("%s (%s)", ce.getName(), code));
            box.setName(code);
            box.setSelected(null == preselectedSets ? false : preselectedSets.contains(code));
            box.setEnabled(null == unselectableSets ? true : !unselectableSets.contains(code));
            p.add(box);
            choices.add(box);
        }
        
        JPanel southPanel = new JPanel(new MigLayout("insets 10, gap 20, ax center"));
        
        if (showWantReprintsCheckbox) {
            southPanel.add(cbWantReprints, "center, span, wrap");
        }
        
        JButton btnOk = new JButton("OK");
        this.getRootPane().setDefaultButton(btnOk);
        btnOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                handleOk();
            }
        });

        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

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

    private void handleOk() {
        for (JCheckBox box : choices) {
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
