package forge.quest.bazaar;

import forge.AllZone;
import forge.QuestData;
import forge.gui.GuiUtils;
import forge.properties.NewConstants;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

public abstract class QuestAbstractBazaarStall extends JPanel implements NewConstants{
	private static final long serialVersionUID = -4147745071116906043L;
	String stallName;
    String fluff;
    ImageIcon icon;



    private JPanel inventoryPanel;

    protected  QuestData questData = AllZone.QuestData;

    protected QuestAbstractBazaarStall(String stallName, String iconName, String fluff) {
        this.fluff = fluff;
        this.icon = GuiUtils.getIconFromFile(iconName);
        this.stallName = stallName;
 
        JLabel stallNameLabel;
        JLabel creditLabel;
 

        GridBagLayout layout  = new GridBagLayout();
        this.setLayout(layout);

        stallNameLabel = new JLabel(stallName);
        stallNameLabel.setFont(new Font("sserif", Font.BOLD, 22));
        stallNameLabel.setHorizontalAlignment(SwingConstants.CENTER);

        creditLabel = new JLabel("Credits: " + questData.getCredits());
        creditLabel.setFont(new Font("sserif", 0, 14));

        JTextArea fluffArea = new JTextArea(fluff);
        fluffArea.setFont(new Font("sserif", Font.ITALIC, 14));
        fluffArea.setLineWrap(true);
        fluffArea.setWrapStyleWord(true);
        fluffArea.setOpaque(false);
        fluffArea.setEditable(false);
        fluffArea.setFocusable(false);

        GridBagConstraints constraints = new GridBagConstraints(0,0,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(2,2,2,2), 0,0);
        layout.setConstraints(stallNameLabel, constraints);
        this.add(stallNameLabel);

        constraints.gridy=1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        layout.setConstraints(fluffArea, constraints);
        this.add(fluffArea);

        constraints.gridy=2;
        layout.setConstraints(creditLabel, constraints);
        this.add(creditLabel);

        java.util.List<QuestAbstractBazaarItem> stallItems = populateItems();

        if (stallItems == null || stallItems.size() == 0){
            //TODO: Get stall-specific fluff in here.
            constraints.gridy=3;
            constraints.anchor=GridBagConstraints.NORTHWEST;
            constraints.fill=GridBagConstraints.BOTH;

            JLabel noSaleLabel = new JLabel("This stall does not offer anything useful for purchase.");
            layout.setConstraints(noSaleLabel,constraints);
            this.add(noSaleLabel);
        }

        else{
            constraints.gridy++;
            constraints.insets = new Insets(10,5,10,5);
            JLabel saleLabel = new JLabel("The following items are for sale:");

            layout.setConstraints(saleLabel,constraints);
            this.add(saleLabel);

            constraints.insets = new Insets(10,5,10,5);

            constraints.anchor=GridBagConstraints.NORTHWEST;
            constraints.fill=GridBagConstraints.HORIZONTAL;
            constraints.weighty = 1;
            constraints.weightx = GridBagConstraints.REMAINDER;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.gridy++;


            inventoryPanel = new JPanel();

            populateInventory(stallItems);

            inventoryPanel.setBorder(new LineBorder(Color.ORANGE,2));



            JScrollPane scrollPane = new JScrollPane(inventoryPanel);
            layout.setConstraints(scrollPane,constraints);
            this.add(scrollPane);
        }

    }

    private void populateInventory(java.util.List<QuestAbstractBazaarItem> stallItems) {
        GridBagLayout innerLayout = new GridBagLayout();
        inventoryPanel.setLayout(innerLayout);
        GridBagConstraints innerConstraints =
                new GridBagConstraints(0,0,1,1,1,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,new Insets(2,2,2,2), 0, 0);

        for (QuestAbstractBazaarItem item : stallItems) {
            JPanel itemPanel = item.getItemPanel();

            innerLayout.setConstraints(itemPanel,innerConstraints);
            inventoryPanel.add(itemPanel);
            innerConstraints.gridy++;
        }

        innerConstraints.weighty=1;
        JLabel fillLabel = new JLabel();

        innerLayout.setConstraints(fillLabel,innerConstraints);
        inventoryPanel.add(fillLabel);
    }

    protected abstract java.util.List<QuestAbstractBazaarItem> populateItems();



    public ImageIcon getStallIcon() {
        return icon;
    }

    public String getStallName() {
        return stallName;
    }

    public void updateItems() {
        this.inventoryPanel.removeAll();
        this.populateInventory(populateItems());
    }
}
