package forge.view.bazaar;

import java.awt.Font;

import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Command;
import forge.Singletons;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FTextArea;
import forge.quest.data.QuestAssets;
import forge.quest.data.item.IQuestStallPurchasable;

/** An update-able panel instance representing a single item. */
@SuppressWarnings("serial")
public class ViewItem extends FPanel {
    private final FLabel lblIcon, lblName, lblPrice, btnPurchase;
    private final FTextArea tarDesc;
    private IQuestStallPurchasable item;

    /** An update-able panel instance representing a single item. */
    public ViewItem() {
        // Final inits
        lblIcon = new FLabel.Builder().iconScaleFactor(1).iconInBackground(true).build();
        lblName = new FLabel.Builder().fontStyle(Font.BOLD).build();
        lblPrice = new FLabel.Builder().fontStyle(Font.BOLD).fontScaleFactor(0.8).build();
        tarDesc = new FTextArea();
        btnPurchase = new FLabel.Builder().text("Buy").opaque(true).fontScaleFactor(0.2).hoverable(true).build();

        this.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        // Layout
        this.setLayout(new MigLayout("insets 0, gap 0"));

        this.add(lblIcon, "w 100px!, h n:90%:100px, ay center, span 1 3, gap 5px 5px 5px 5px");
        this.add(lblName, "pushx, w 60%!, h 22px!, gap 0 0 5px 5px");
        this.add(btnPurchase, "w 80px!, h 80px!, ay center, span 1 3, gap 0 15px 0 0, wrap");

        this.add(tarDesc, "w 60%!, gap 0 0 0 10px, wrap");
        this.add(lblPrice, "w 60%!, h 20px!, gap 0 0 0 5px");

        btnPurchase.setCommand(new Command() {
            @Override
            public void execute() {
                QuestAssets qA = AllZone.getQuest().getAssets();
                qA.subtractCredits(getItem().getBuyingPrice(qA));
                qA.addCredits(getItem().getSellingPrice(qA));
                getItem().onPurchase(qA);
                AllZone.getQuest().save();
                Singletons.getView().getViewBazaar().refreshLastInstance();
            }
        });
    }

    /** @param i0 &emsp; {@link forge.quest.data.item.IQuestStallPurchasable} */
    public void setItem(IQuestStallPurchasable i0) {
        this.item = i0;
    }

    /** @return {@link forge.quest.data.item.IQuestStallPurchasable} */
    public IQuestStallPurchasable getItem() {
        return this.item;
    }

    /** Updates this panel with current item stats. */
    public void update() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                QuestAssets qA = AllZone.getQuest().getAssets();
                lblIcon.setIcon(getItem().getIcon());
                lblName.setText(getItem().getPurchaseName());
                lblPrice.setText("Cost: " + String.valueOf(getItem().getBuyingPrice(qA)) + " credits");
                tarDesc.setText(getItem().getPurchaseDescription());

                if (qA.getCredits() < getItem().getBuyingPrice(qA)) {
                    btnPurchase.setEnabled(false);
                }

                revalidate();
                repaint();
            }
        });
    }
}
