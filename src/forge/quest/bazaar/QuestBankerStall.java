package forge.quest.bazaar;

import forge.QuestData;
import forge.error.ErrorViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class QuestBankerStall extends QuestAbstractBazaarStall {

    private static final long serialVersionUID = 2409591658245091210L;

    private JLabel titleLabel = new JLabel();

    private JLabel estatesDescLabel = new JLabel();
    private JLabel estatesPriceLabel = new JLabel();
    private JLabel estatesIconLabel = new JLabel();

    private JLabel creditsLabel = new JLabel();

    private JButton learnEstatesButton = new JButton();
    private ImageIcon estatesIcon;


    public QuestBankerStall() {
        super("Banker", "CoinIconSmall.png", "");

        try {
            jbInit();
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
        }


        setup();


    }

    //only do this ONCE:
    private void setup() {
        learnEstatesButton.setBounds(new Rectangle(10, 297, 120, 50));
        learnEstatesButton.setText(getButtonText());
        //buyPlantButton.setIcon(icon);
        learnEstatesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    learnEstatesButton_actionPerformed(e);
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        });

    }//setup();

    private String getDesc() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");

        if (questData.getEstatesLevel() == 0) {
            sb.append("<u>Level 1 Estates</u><br>");
            sb.append("Gives a bonus of <b>10%</b> to match winnings.<br>");
            sb.append("Improves sell percentage by <b>1.0%</b>.");
        } else if (questData.getEstatesLevel() == 1) {
            sb.append("<u>Level 2 Estates</u><br>");
            sb.append("Gives a bonus of <b>15%</b> to match winnings.<br>");
            sb.append("Improves sell percentage by <b>1.75%</b>.");
        } else if (questData.getEstatesLevel() == 2) {
            sb.append("<u>Level 3 Estates</u><br>");
            sb.append("Gives a bonus of <b>20%</b> to match winnings.<br>");
            sb.append("Improves sell percentage by <b>2.5%</b>.");
        } else if (questData.getEstatesLevel() >= 3 && questData.getLuckyCoinLevel() == 0) {
            sb.append("Estates Level Maxed out.<br>");
            sb.append("<u><b>Lucky Coin</b></u><br>");
            sb.append("This coin is believed to give good luck to its owner.<br>");
            sb.append("Improves the chance of getting a random <br>rare after each match by <b>15%</b>.");
            /*sb.append("Current Level: 3/3<br>");
               sb.append("Gives a bonus of <b>20%</b> to match winnings.<br>");
               sb.append("Improves sell percentage by <b>2.5%</b>.");*/
        } else {
            sb.append("Currently nothing for sale at the Treasury. <br>Please check back later.");
        }

        sb.append("</html>");
        return sb.toString();
    }

    private long getPrice() {
        long l = 0;
        if (questData.getEstatesLevel() == 0)
            l = 500;
        else if (questData.getEstatesLevel() == 1)
            l = 750;
        else if (questData.getEstatesLevel() == 2)
            l = 1000;
        else if (questData.getEstatesLevel() >= 3 && questData.getLuckyCoinLevel() == 0)
            l = 500;


        return l;
    }

    private String getButtonText() {
        if (questData.getEstatesLevel() < 3)
            return "Learn Estates";
        else
            return "Buy Coin";
    }

    private String getImageString() {
        if (questData.getEstatesLevel() < 3)
            return "GoldIconLarge.png";
        else
            return "CoinIcon.png";
    }

    private void jbInit() throws Exception {
        titleLabel.setFont(new Font("sserif", Font.BOLD, 22));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setText("Treasury");
        titleLabel.setBounds(new Rectangle(130, 5, 198, 60));
        stallPanel.setLayout(null);

        /*
        potionStatsLabel.setFont(new Font("sserif", Font.BOLD, 12));
        potionStatsLabel.setText(getStats());
        potionStatsLabel.setBounds(new Rectangle(10, 65, 100, 15));
        */

        estatesDescLabel.setFont(new Font("sserif", 0, 12));
        estatesDescLabel.setText(getDesc());
        estatesDescLabel.setBounds(new Rectangle(10, 80, 300, 150));

        estatesPriceLabel.setFont(new Font("sserif", 0, 12));
        estatesPriceLabel.setText("<html><b><u>Price</u></b>: " + getPrice() + " credits</html>");
        estatesPriceLabel.setBounds(new Rectangle(10, 230, 150, 15));

        creditsLabel.setFont(new Font("sserif", 0, 12));
        creditsLabel.setText("Credits: " + questData.getCredits());
        creditsLabel.setBounds(new Rectangle(10, 265, 150, 15));

        estatesIcon = getIcon(getImageString());
        estatesIconLabel.setText("");
        estatesIconLabel.setIcon(estatesIcon);
        estatesIconLabel.setBounds(new Rectangle(255, 65, 256, 256));
        estatesIconLabel.setIconTextGap(0);

        //String fileName = "LeafIconSmall.png";
        //ImageIcon icon = getIcon(fileName);

        learnEstatesButton.setEnabled(true);
        if (questData.getCredits() < getPrice() || (questData.getEstatesLevel() >= 3 && questData.getLuckyCoinLevel() >= 1))
            learnEstatesButton.setEnabled(false);


        //jPanel2.add(quitButton, null);
        stallPanel.add(learnEstatesButton, null);
        stallPanel.add(titleLabel, null);
        stallPanel.add(estatesDescLabel, null);
        stallPanel.add(estatesIconLabel, null);
        stallPanel.add(estatesPriceLabel, null);
        stallPanel.add(creditsLabel, null);
    }

    void learnEstatesButton_actionPerformed(ActionEvent e) throws Exception {
        questData.subtractCredits(getPrice());

        if (questData.getEstatesLevel() < 3) {
            questData.addEstatesLevel(1);
        } else if (questData.getLuckyCoinLevel() < 1) {
            questData.addLuckyCoinLevel(1);
        }
        QuestData.saveData(questData);
        jbInit();
    }

}
