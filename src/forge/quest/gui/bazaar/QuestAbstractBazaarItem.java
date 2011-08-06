package forge.quest.gui.bazaar;

import forge.AllZone;
import forge.gui.GuiUtils;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
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

        JLabel descriptionLabel = new JLabel("<html>" + description + "</html>");
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


        JPanel itemPanel = new JPanel();
        BorderLayout layout = new BorderLayout();
        layout.setHgap(3);
        itemPanel.setLayout(layout);


        layout = new BorderLayout();
        layout.setVgap(3);
        JPanel centerPanel = new JPanel(layout);

        centerPanel.add(nameLabel, BorderLayout.NORTH);
        centerPanel.add(descriptionLabel, BorderLayout.CENTER);
        centerPanel.add(priceLabel, BorderLayout.SOUTH);

        JPanel buyPanel = new JPanel(new BorderLayout());
        buyPanel.add(purchaseButton, BorderLayout.SOUTH);

        itemPanel.add(iconPanel, BorderLayout.WEST);
        itemPanel.add(centerPanel, BorderLayout.CENTER);
        itemPanel.add(buyPanel, BorderLayout.EAST);

        itemPanel.setBorder(new CompoundBorder(new LineBorder(Color.BLACK, 1), new EmptyBorder(5, 5, 5, 5)));

        return itemPanel;
    }
}
