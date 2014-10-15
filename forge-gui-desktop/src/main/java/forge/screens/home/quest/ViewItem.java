package forge.screens.home.quest;

import forge.GuiBase;
import forge.UiCommand;
import forge.assets.FSkinProp;
import forge.model.FModel;
import forge.quest.QuestUtil;
import forge.quest.bazaar.IQuestBazaarItem;
import forge.quest.data.QuestAssets;
import forge.screens.bazaar.VBazaarUI;
import forge.toolbox.FHtmlViewer;
import forge.toolbox.FLabel;
import forge.toolbox.FPanel;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinImage;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

import java.awt.*;

/** An update-able panel instance representing a single item. */
@SuppressWarnings("serial")
public class ViewItem extends FPanel {
    private final FLabel lblIcon, lblName, lblPrice, btnPurchase;
    private final FHtmlViewer tarDesc;
    private IQuestBazaarItem item;

    /** An update-able panel instance representing a single item. */
    public ViewItem() {
        // Final inits
        this.lblIcon = new FLabel.Builder().iconScaleFactor(1).iconInBackground(true).build();
        this.lblName = new FLabel.Builder().fontStyle(Font.BOLD).build();
        this.lblPrice = new FLabel.Builder().fontStyle(Font.BOLD).fontSize(12).build();
        this.tarDesc = new FHtmlViewer();
        this.btnPurchase = new FLabel.Builder().text("Buy").opaque(true).fontSize(20).hoverable(true).build();

        this.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        // Layout
        this.setLayout(new MigLayout("insets 0, gap 0"));

        this.add(this.lblIcon, "w 100px!, h n:90%:100px, ay center, span 1 3, gap 5px 5px 5px 5px");
        this.add(this.lblName, "pushx, w 60%!, h 22px!, gap 0 0 5px 5px");
        this.add(this.btnPurchase, "w 80px!, h 80px!, ay center, span 1 3, gap 0 15px 0 0, wrap");

        this.add(this.tarDesc, "w 60%!, gap 0 0 0 10px, wrap");
        this.add(this.lblPrice, "w 60%!, h 20px!, gap 0 0 0 5px");

        this.btnPurchase.setCommand(new UiCommand() {
            @Override
            public void run() {
                QuestUtil.buyQuestItem(getItem());
                VBazaarUI.SINGLETON_INSTANCE.refreshLastInstance();
            }
        });
    }

    /**
     * @param i0
     *            &emsp; {@link forge.quest.bazaar.IQuestBazaarItem}
     */
    public void setItem(final IQuestBazaarItem i0) {
        this.item = i0;
    }

    /** @return {@link forge.quest.bazaar.IQuestBazaarItem} */
    public IQuestBazaarItem getItem() {
        return this.item;
    }

    /** Updates this panel with current item stats. */
    public void update() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final QuestAssets qA = FModel.getQuest().getAssets();
                IQuestBazaarItem bazaarItem = ViewItem.this.getItem();

                SkinImage i;
                try {
                    i = (SkinImage) bazaarItem.getIcon(qA);
                } catch (final Exception e1) {
                    // give up, icon unknown
                    e1.printStackTrace();
                    i = (SkinImage) GuiBase.getInterface().getSkinIcon(FSkinProp.ICO_UNKNOWN);
                }

                ViewItem.this.lblIcon.setIcon(i);

                ViewItem.this.lblName.setText(bazaarItem.getPurchaseName());
                ViewItem.this.lblPrice.setText("Cost: " + String.valueOf(bazaarItem.getBuyingPrice(qA)) + " credits");
                String desc = bazaarItem.getPurchaseDescription(qA);
                ViewItem.this.tarDesc.setText(FSkin.encodeSymbols(desc, false)); //encode in case there are mana symbols in description

                ViewItem.this.btnPurchase.setEnabled(!(qA.getCredits() < bazaarItem.getBuyingPrice(qA)));

                ViewItem.this.revalidate();
                ViewItem.this.repaintSelf();
            }
        });
    }
}
