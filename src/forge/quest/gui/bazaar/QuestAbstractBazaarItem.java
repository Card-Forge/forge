package forge.quest.gui.bazaar;

import forge.AllZone;
import forge.gui.GuiUtils;
import forge.gui.MultiLineLabel;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class QuestAbstractBazaarItem {

    String name;
    String description;
    int price;
    ImageIcon image;

    protected QuestAbstractBazaarItem(String name, String description, int price) {
        this.name = name;
        this.description = description;
        this.price = price;

        //create a blank image placeholder
        this.image = GuiUtils.getEmptyIcon(40, 40);
    }

    protected QuestAbstractBazaarItem(String name, String description, int price, ImageIcon image) {
        this.name = name;
        this.description = description;
        this.price = price;

        if (image == null) {
            //create a blank image placeholder
            this.image = GuiUtils.getEmptyIcon(40, 40);
        }
        else {
            this.image = image;
        }
    }

    /**
     * Invoked by the Bazaar UI when the item is purchased. The credits of the item should not be deducted here.
     */
    public abstract void purchaseItem();

    protected final JPanel getItemPanel() {
        ImageIcon resizedImage = GuiUtils.getResizedIcon(image, 40, 40);

        JLabel iconLabel = new JLabel(resizedImage);
        iconLabel.setBorder(new LineBorder(Color.BLACK));
        JPanel iconPanel = new JPanel(new BorderLayout());
        iconPanel.add(iconLabel, BorderLayout.NORTH);

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

        JLabel descriptionLabel = new MultiLineLabel("<html>" + description + "</html>");
        descriptionLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        JLabel priceLabel = new JLabel("<html><b>Cost:</b> " + price + " credits</html>");
        priceLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));


        JButton purchaseButton = new JButton("Buy");
        purchaseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                AllZone.QuestData.subtractCredits(price);
                purchaseItem();
                AllZone.QuestData.saveData();
                QuestBazaarPanel.refreshLastInstance();
            }
        });

        if (AllZone.QuestData.getCredits() < price) {
            purchaseButton.setEnabled(false);
        }

        JPanel itemPanel = new JPanel(){
            @Override
            public Dimension getPreferredSize() {
                Dimension realSize = super.getPreferredSize();
                realSize.width = 100;
                return realSize;
            }
        };
        GridBagLayout layout = new GridBagLayout();
        itemPanel.setLayout(layout);

        GridBagConstraints constraints = new GridBagConstraints(
                0,
                0,
                1,
                1,
                0,
                0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE,
                new Insets(2,2,2,2),
                0,
                0
        );

        constraints.gridheight = GridBagConstraints.REMAINDER;
        layout.setConstraints(iconLabel, constraints);
        itemPanel.add(iconLabel);

        constraints.gridheight = 1;
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        layout.setConstraints(nameLabel, constraints);
        itemPanel.add(nameLabel);

        constraints.gridy = 1;
        layout.setConstraints(descriptionLabel, constraints);
        itemPanel.add(descriptionLabel);

        constraints.gridy = 2;
        layout.setConstraints(priceLabel, constraints);
        itemPanel.add(priceLabel);

        constraints.gridy = 2;
        constraints.gridx=2;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridheight= 1;
        constraints.weightx = 0;
        layout.setConstraints(purchaseButton,constraints);
        itemPanel.add(purchaseButton);

        itemPanel.setBorder(new CompoundBorder(new LineBorder(Color.BLACK, 1), new EmptyBorder(5, 5, 5, 5)));

        itemPanel.setMinimumSize(new Dimension(0,0));


        return itemPanel;
    }
}
