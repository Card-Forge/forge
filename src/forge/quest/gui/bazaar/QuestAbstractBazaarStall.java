package forge.quest.gui.bazaar;

import forge.AllZone;
import forge.QuestData;
import forge.gui.GuiUtils;
import forge.properties.NewConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public abstract class QuestAbstractBazaarStall extends JPanel implements NewConstants {
    private static final long serialVersionUID = -4147745071116906043L;
    String stallName;
    String fluff;
    ImageIcon icon;


    private JLabel creditLabel = new JLabel();
    private JPanel inventoryPanel = new JPanel();

    protected QuestData questData = AllZone.QuestData;

    protected QuestAbstractBazaarStall(String stallName, String iconName, String fluff) {
        this.fluff = fluff;
        this.icon = GuiUtils.getIconFromFile(iconName);
        this.stallName = stallName;

        initUI();

    }

    private void initUI() {
        this.removeAll();
        
        JLabel stallNameLabel;

        GridBagLayout layout = new GridBagLayout();
        this.setLayout(layout);

        stallNameLabel = new JLabel(stallName);
        stallNameLabel.setFont(new Font("sserif", Font.BOLD, 22));
        stallNameLabel.setHorizontalAlignment(SwingConstants.CENTER);

        creditLabel.setText("Credits: " + questData.getCredits());
        creditLabel.setFont(new Font("sserif", 0, 14));

        JTextArea fluffArea = new JTextArea(fluff);
        fluffArea.setFont(new Font("sserif", Font.ITALIC, 14));
        fluffArea.setLineWrap(true);
        fluffArea.setWrapStyleWord(true);
        fluffArea.setOpaque(false);
        fluffArea.setEditable(false);
        fluffArea.setFocusable(false);

        GridBagConstraints constraints = new GridBagConstraints(0,
                0,
                1,
                1,
                1,
                0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE,
                new Insets(2, 2, 2, 2),
                0,
                0);
        layout.setConstraints(stallNameLabel, constraints);
        this.add(stallNameLabel);

        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        layout.setConstraints(fluffArea, constraints);
        this.add(fluffArea);

        constraints.gridy = 2;
        layout.setConstraints(creditLabel, constraints);
        this.add(creditLabel);

        constraints.gridy = 3;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(10,5,10,5);
        constraints.weighty = 1;
        constraints.weightx = GridBagConstraints.REMAINDER;

        populateInventory(populateItems());

        JScrollPane scrollPane = new JScrollPane(inventoryPanel);
        scrollPane.setBorder(new EmptyBorder(0,0,0,0));
        layout.setConstraints(scrollPane, constraints);
        this.add(scrollPane);

        this.setBorder(new EmptyBorder(0,5,0,0));
    }

    private void populateInventory(java.util.List<QuestAbstractBazaarItem> stallItems) {
        inventoryPanel.removeAll();

        GridBagLayout innerLayout = new GridBagLayout();
        inventoryPanel.setLayout(innerLayout);
        GridBagConstraints innerConstraints =
                new GridBagConstraints(0,
                        0,
                        1,
                        1,
                        1,
                        0,
                        GridBagConstraints.NORTHWEST,
                        GridBagConstraints.HORIZONTAL,
                        new Insets(2, 2, 2, 2),
                        0,
                        0);

        JLabel purchaseLabel = new JLabel();

        if (stallItems.size() == 0){

            purchaseLabel.setText("The merchant does not have anything useful for sale");
            inventoryPanel.add(purchaseLabel);
            innerConstraints.gridy++;
        }

        else{

            innerConstraints.insets = new Insets(5,20,5,5);
            for (QuestAbstractBazaarItem item : stallItems) {
                JPanel itemPanel = item.getItemPanel();

                innerLayout.setConstraints(itemPanel, innerConstraints);
                inventoryPanel.add(itemPanel);
                innerConstraints.gridy++;
            }
        }
        innerConstraints.weighty = 1;
        JLabel fillLabel = new JLabel();

        innerLayout.setConstraints(fillLabel, innerConstraints);
        inventoryPanel.add(fillLabel);
    }

    protected abstract java.util.List<QuestAbstractBazaarItem> populateItems();


    public ImageIcon getStallIcon() {
        return icon;
    }

    public String getStallName() {
        return stallName;
    }

    public void updateItems(){
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                populateInventory(populateItems());
                creditLabel.setText("Credits: " + questData.getCredits());
                inventoryPanel.invalidate();
                inventoryPanel.repaint();
            }
        });
    }
}
