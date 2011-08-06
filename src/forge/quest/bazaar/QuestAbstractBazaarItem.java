package forge.quest.bazaar;

import forge.AllZone;
import forge.QuestData;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

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
        Image emptyImage = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
        image = new ImageIcon(emptyImage);

    }

    protected QuestAbstractBazaarItem(String name, String description, int price, ImageIcon image) {
        this.name = name;
        this.description = description;
        this.price = price;

        if (image == null) {
            //create a blank image placeholder
            Image emptyImage = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
            this.image = new ImageIcon(emptyImage);
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
        ImageIcon resizedImage = new ImageIcon(image.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH));

        JLabel iconLabel = new JLabel(resizedImage);
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
                QuestData.saveData(AllZone.QuestData);
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

        itemPanel.add(iconLabel, BorderLayout.WEST);
        itemPanel.add(centerPanel, BorderLayout.CENTER);
        itemPanel.add(buyPanel, BorderLayout.EAST);

        itemPanel.setBorder(new CompoundBorder(new LineBorder(Color.BLACK, 1), new EmptyBorder(5, 5, 5, 5)));

        return itemPanel;
    }
}
