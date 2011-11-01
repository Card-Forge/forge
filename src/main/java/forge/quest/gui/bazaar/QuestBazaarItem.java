package forge.quest.gui.bazaar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import forge.AllZone;
import forge.gui.GuiUtils;
import forge.gui.MultiLineLabel;
import forge.quest.data.bazaar.QuestStallPurchasable;

/**
 * <p>
 * QuestBazaarItem class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestBazaarItem {

    /** The item. */
    private QuestStallPurchasable item;

    /**
     * <p>
     * Constructor for QuestBazaarItem.
     * </p>
     * 
     * @param purchasable
     *            a {@link forge.quest.data.bazaar.QuestStallPurchasable}
     *            object.
     */
    protected QuestBazaarItem(final QuestStallPurchasable purchasable) {
        this.item = purchasable;
    }

    /**
     * Invoked by the Bazaar UI when the item is purchased. The credits of the
     * item should not be deducted here.
     */
    public final void purchaseItem() {
        this.item.onPurchase();
    }

    /**
     * <p>
     * getItemPanel.
     * </p>
     * 
     * @return a {@link javax.swing.JPanel} object.
     */
    protected final JPanel getItemPanel() {
        ImageIcon icon = GuiUtils.getIconFromFile(this.item.getImageName());
        if (icon == null) {
            // The original size was only 40 x 40 pixels.
            // Increased the size to give added pixels for more detail.
            icon = GuiUtils.getEmptyIcon(80, 80);
        }
        // The original size was only 40 x 40 pixels.
        // Increased the size to give added pixels for more detail.
        final ImageIcon resizedImage = GuiUtils.getResizedIcon(icon, 80, 80);

        final JLabel iconLabel = new JLabel(resizedImage);
        iconLabel.setBorder(new LineBorder(Color.BLACK));
        final JPanel iconPanel = new JPanel(new BorderLayout());
        iconPanel.add(iconLabel, BorderLayout.NORTH);

        final JLabel nameLabel = new JLabel(this.item.getPurchaseName());
        nameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

        final JLabel descriptionLabel = new MultiLineLabel("<html>" + this.item.getPurchaseDescription() + "</html>");
        descriptionLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        final JLabel priceLabel = new JLabel("<html><b>Cost:</b> " + this.item.getPrice() + " credits</html>");
        priceLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        final JButton purchaseButton = new JButton("Buy");
        purchaseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                AllZone.getQuestData().subtractCredits(QuestBazaarItem.this.item.getPrice());
                QuestBazaarItem.this.purchaseItem();
                AllZone.getQuestData().saveData();
                QuestBazaarPanel.refreshLastInstance();
            }
        });

        if (AllZone.getQuestData().getCredits() < this.item.getPrice()) {
            purchaseButton.setEnabled(false);
        }

        final JPanel itemPanel = new JPanel() {
            private static final long serialVersionUID = -5182857296365949682L;

            @Override
            public Dimension getPreferredSize() {
                final Dimension realSize = super.getPreferredSize();
                realSize.width = 100;
                return realSize;
            }
        };
        final GridBagLayout layout = new GridBagLayout();
        itemPanel.setLayout(layout);

        final GridBagConstraints constraints = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER,
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
