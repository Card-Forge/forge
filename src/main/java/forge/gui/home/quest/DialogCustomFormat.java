package forge.gui.home.quest;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;



import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import forge.card.CardEdition;
import forge.quest.data.GameFormatQuest;
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
    private GameFormatQuest customFormat;
    private JCheckBox [] choices;
    private String [] codes;


    /**
     * Create the dialog.
     * 
     * @param userFormat
     *  GameFormatQuest, the user-defined format to update
     * 
     */
    public DialogCustomFormat(GameFormatQuest userFormat) {

        customFormat = userFormat;
        if (customFormat == null) {
            throw new RuntimeException("Null GameFormatQuest in DialogCustomFormat");
        }

        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        TreeMap<Integer, CardEdition> sortedMap = new TreeMap<Integer, CardEdition>();

        for (CardEdition ce : Singletons.getModel().getEditions()) {
          if (isSelectable(ce.getCode())) {
              sortedMap.put(new Integer(ce.getIndex()), ce);
          }
        }

        final int numEditions = sortedMap.size();
        final int rows = 30;
        final int columns = numEditions <= rows ? 1 : 1 + ((numEditions - 1) / rows);

        List<CardEdition> sortedEditions = new ArrayList<CardEdition>(sortedMap.values());
        choices = new JCheckBox[rows * columns];
        codes = new String[rows * columns];

        int getIdx = 0;
        int putIdx = 0;

        JPanel p = new JPanel();
        p.setSize(600, 400);
        p.setLayout(new GridLayout(rows, columns, 10, 0));

        for (int row = 1; row < rows; row++) {
            for (int col = 1; col < columns + 1; col++) {
                getIdx = (row - 1) + ((col - 1) * (rows - 1));
                CardEdition edition = getIdx < numEditions ? sortedEditions.get(getIdx) : null;
                choices[putIdx] = (edition != null ? new JCheckBox(edition.getName()) : new JCheckBox());

                if (edition == null) {
                    choices[putIdx].setEnabled(false);
                    codes[putIdx] = new String("");
                }
                else {
                    if (customFormat.isSetLegal(edition.getCode()) && !(customFormat.getAllowedSetCodes().isEmpty())) {
                        choices[putIdx].setSelected(true);
                    }
                    codes[putIdx] = new String(edition.getCode());
                }
                p.add(choices[putIdx]);
                putIdx++;
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
    private boolean isSelectable(final String setCode) {
        if (setCode == null) {
            return true;
        }
        return !(setCode.equals("LEA") || setCode.equals("LEB"));
    }

    /**
     * Update the custom format in accordance with the selections.
     */
    void updateCustomFormat() {
        if (customFormat == null) {
            return;
        }

        // Fix a problem with not updating changes
        customFormat.emptyAllowedSets();

        for (int i = 0; i < choices.length; i++) {
            if (choices[i] != null) {
                if (choices[i].isSelected()) {
                    customFormat.addAllowedSet(codes[i]);
                }
             }
        }
    }
}
