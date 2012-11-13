package forge.gui.home.quest;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import forge.card.CardEdition;
import forge.Singletons;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class DialogCustomFormat extends JDialog {

    private static final long serialVersionUID = 3155211532871888181L;
    private JScrollPane scrollPane;
    private final JButton btnOK = new JButton("OK");
    private final JButton btnCancel = new JButton("Cancel");
    private final JPanel buttonPanel = new JPanel();
    private final List<String> customFormat;
    private final List<JCheckBox> choices = new ArrayList<JCheckBox>();


    /**
     * Create the dialog.
     * 
     * @param userFormat
     *  GameFormatQuest, the user-defined format to update
     * 
     */
    public DialogCustomFormat(List<String> userFormat) {

        customFormat = userFormat;
        if (customFormat == null) {
            throw new RuntimeException("Null GameFormatQuest in DialogCustomFormat");
        }

        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        List<CardEdition> editions = new ArrayList<CardEdition>();

        for (CardEdition ce : Singletons.getModel().getEditions()) {
          if (canChoose(ce.getCode())) {
              editions.add(ce);
          }
        }
        
        Collections.sort(editions);
        Collections.reverse(editions);

        final int numEditions = editions.size();
        final int columns = 3;
        final int rows = numEditions / columns + (numEditions % columns == 0 ? 0 : 1);
        

        int getIdx = 0;

        JPanel p = new JPanel();
        p.setSize(600, 400);
        p.setLayout(new GridLayout(rows, columns, 10, 0));

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                getIdx = row + (col * rows);
                JCheckBox box;
                if ( getIdx < numEditions ) {
                    CardEdition edition = getIdx < numEditions ? editions.get(getIdx) : null;
                    box = new JCheckBox(edition.getName());
                    box.setName(edition.getCode());
                    box.setSelected(customFormat.contains(edition));
                    choices.add(box);
                } else {
                    box = new JCheckBox();
                    box.setEnabled(false);
                }

                p.add(box);
            }
        }
      scrollPane = new JScrollPane(p);

      getContentPane().add(scrollPane, BorderLayout.CENTER);
      btnOK.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(final ActionEvent arg0) {
              updateCustomFormat();
              dispose();
          }
      });
      buttonPanel.add(btnOK, BorderLayout.WEST);
      btnCancel.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(final ActionEvent arg0) {
              dispose();
          }
      });
      buttonPanel.add(btnCancel, BorderLayout.EAST);
      getContentPane().add(buttonPanel, BorderLayout.SOUTH);

      this.setSize(600, 450);
      this.setLocationRelativeTo(null);
      this.setTitle("Choose sets for custom format:");
      this.setVisible(true);

    }

    /**
     * Make some sets unselectable (currently only Alpha and Beta).
     * Note that these sets can still be (theoretically) unlocked
     * later, for an exorbitant price. There'd be nothing to be
     * gained from allowing these initially (Unlimited already covers
     * their cardbase), and a lot to be lost (namely, easy early access
     * to extremely expensive cards...) --BBU
     *
     * @param setCode
     *      String, set code
     * @return boolean, this set can be selected.
     * 
     */
    private boolean canChoose(final String setCode) {
        if (setCode == null ) {
            return true;
        }
        return !setCode.equals("LEA") && !setCode.equals("LEB") && !"MBP".equals(setCode);
    }

    /**
     * Update the custom format in accordance with the selections.
     */
    void updateCustomFormat() {
        customFormat.clear();
        for (JCheckBox box : choices) {
            if (box.isSelected()) {
                customFormat.add(box.getText());
            }
        }
    }
}
