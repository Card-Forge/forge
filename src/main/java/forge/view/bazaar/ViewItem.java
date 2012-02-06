package forge.view.bazaar;

import java.awt.Font;

import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Command;
import forge.quest.data.bazaar.QuestStallPurchasable;
import forge.view.GuiTopLevel;
import forge.view.toolbox.FLabel;
import forge.view.toolbox.FRoundedPanel;
import forge.view.toolbox.FSkin;
import forge.view.toolbox.FTextArea;

/** An update-able panel instance representing a single item. */
@SuppressWarnings("serial")
public class ViewItem extends FRoundedPanel {
    private final FLabel lblIcon, lblName, lblPrice, btnPurchase;
    private final FTextArea tarDesc;
    private QuestStallPurchasable item;

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
                AllZone.getQuestData().subtractCredits(getItem().getBuyingPrice());
                AllZone.getQuestData().addCredits(getItem().getSellingPrice());
                getItem().onPurchase();
                AllZone.getQuestData().saveData();
                ((GuiTopLevel) AllZone.getDisplay()).getController().getBazaarView().refreshLastInstance();
            }
        });
    }

    /** @param i0 &emsp; {@link forge.quest.data.bazaar.QuestStallPurchasable} */
    public void setItem(QuestStallPurchasable i0) {
        this.item = i0;
    }

    /** @return {@link forge.quest.data.bazaar.QuestStallPurchasable} */
    public QuestStallPurchasable getItem() {
        return this.item;
    }

    /** Updates this panel with current item stats. */
    public void update() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                lblIcon.setIcon(getItem().getIcon());
                lblName.setText(getItem().getPurchaseName());
                lblPrice.setText("Cost: " + String.valueOf(getItem().getBuyingPrice()) + " credits");
                tarDesc.setText(getItem().getPurchaseDescription());

                if (AllZone.getQuestData().getCredits() < getItem().getBuyingPrice()) {
                    btnPurchase.setEnabled(false);
                }

                revalidate();
                repaint();
            }
        });
    }
}
