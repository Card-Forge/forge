
package forge.quest.gui.bazaar;


import forge.AllZone;
import forge.gui.ForgeFontConstants;
import forge.gui.GuiUtils;
import forge.gui.MultiLineLabel;
import forge.quest.data.bazaar.QuestStallPurchasable;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class QuestBazaarItem {

    QuestStallPurchasable item;
    protected QuestBazaarItem(QuestStallPurchasable purchasable){
        this.item = purchasable;
    }
    
    /**
     * Invoked by the Bazaar UI when the item is purchased. The credits of the item should not be deducted here.
     */
    public void purchaseItem(){
        item.onPurchase();
    }
    
    protected final JPanel getItemPanel() {
        ImageIcon icon = GuiUtils.getIconFromFile(item.getImageName());
        if (icon == null){
            icon = GuiUtils.getEmptyIcon(40,40);
        }
        ImageIcon resizedImage = GuiUtils.getResizedIcon(icon, 40, 40);
        
        JLabel iconLabel = new JLabel(resizedImage);
        iconLabel.setBorder(new LineBorder(Color.BLACK));
        JPanel iconPanel = new JPanel(new BorderLayout());
        iconPanel.add(iconLabel, BorderLayout.NORTH);
        
        JLabel nameLabel = new JLabel(item.getPurchaseName());
        nameLabel.setFont(new Font(ForgeFontConstants.SANS_SERIF, Font.BOLD, 14));
        
        JLabel descriptionLabel = new MultiLineLabel("<html>" + item.getPurchaseDescription()+ "</html>");
        descriptionLabel.setFont(new Font(ForgeFontConstants.SANS_SERIF, Font.PLAIN, 12));
        
        JLabel priceLabel = new JLabel("<html><b>Cost:</b> " + item.getPrice() + " credits</html>");
        priceLabel.setFont(new Font(ForgeFontConstants.SANS_SERIF, Font.PLAIN, 12));
        

        JButton purchaseButton = new JButton("Buy");
        purchaseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                AllZone.QuestData.subtractCredits(item.getPrice());
                purchaseItem();
                AllZone.QuestData.saveData();
                QuestBazaarPanel.refreshLastInstance();
            }
        });
        
        if(AllZone.QuestData.getCredits() < item.getPrice()) {
            purchaseButton.setEnabled(false);
        }
        
        JPanel itemPanel = new JPanel() {
			private static final long serialVersionUID = -5182857296365949682L;

			@Override
            public Dimension getPreferredSize() {
                Dimension realSize = super.getPreferredSize();
                realSize.width = 100;
                return realSize;
            }
        };
        GridBagLayout layout = new GridBagLayout();
        itemPanel.setLayout(layout);
        
        GridBagConstraints constraints = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0);
        
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
        constraints.gridx = 2;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridheight = 1;
        constraints.weightx = 0;
        layout.setConstraints(purchaseButton, constraints);
        itemPanel.add(purchaseButton);
        
        itemPanel.setBorder(new CompoundBorder(new LineBorder(Color.BLACK, 1), new EmptyBorder(5, 5, 5, 5)));
        
        itemPanel.setMinimumSize(new Dimension(0, 0));
        

        return itemPanel;
    }
}
