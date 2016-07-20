package forge.screens.quest;

import java.util.List;
import java.util.Set;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.math.Vector2;

import forge.Forge;
import forge.Graphics;
import forge.GuiBase;
import forge.assets.FImage;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.model.FModel;
import forge.quest.QuestController;
import forge.quest.QuestUtil;
import forge.quest.bazaar.IQuestBazaarItem;
import forge.quest.bazaar.QuestBazaarManager;
import forge.quest.bazaar.QuestStallDefinition;
import forge.quest.data.QuestAssets;
import forge.screens.TabPageScreen;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FList;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextArea;
import forge.util.Utils;

public class QuestBazaarScreen extends TabPageScreen<QuestBazaarScreen> {
    public QuestBazaarScreen() {
        super(getPages());
    }

    @Override
    protected boolean allowBackInLandscapeMode() {
        return true;
    }

    private static BazaarPage[] getPages() {
        int pageNum = 0;
        QuestBazaarManager bazaar = FModel.getQuest().getBazaar();
        Set<String> stallNames = bazaar.getStallNames();
        BazaarPage[] pages = new BazaarPage[stallNames.size()];

        for (final String s : stallNames) {
            pages[pageNum++] = new BazaarPage(bazaar.getStall(s));
        }
        return pages;
    }

    private static class BazaarPage extends TabPage<QuestBazaarScreen> {
        private static final float PADDING = Utils.scale(5);

        private final QuestStallDefinition stallDef;
        private final FLabel lblStallName = add(new FLabel.Builder().text("").align(HAlignment.CENTER).build());
        private final FLabel lblEmpty = add(new FLabel.Builder().font(FSkinFont.get(12))
                .text("The merchant does not have anything useful for sale.")
                .align(HAlignment.CENTER).build());
        private final FLabel lblCredits = add(new FLabel.Builder().font(FSkinFont.get(15)).icon(FSkinImage.QUEST_COINSTACK).iconScaleFactor(1f).build());
        private final FLabel lblLife = add(new FLabel.Builder().font(lblCredits.getFont()).icon(FSkinImage.QUEST_LIFE).iconScaleFactor(1f).align(HAlignment.RIGHT).build());
        private final FTextArea lblFluff = add(new FTextArea(false));
        private final FScrollPane scroller = add(new FScrollPane() {
            @Override
            protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
                float y = 0;
                for (FDisplayObject child : getChildren()) {
                    child.setBounds(0, y, visibleWidth, ((BazaarItemDisplay)child).getPreferredHeight(visibleWidth));
                    y += child.getHeight();
                }
                return new ScrollBounds(visibleWidth, y);
            }

            @Override
            public void drawOnContainer(Graphics g) {
                //draw top border above items
                float y = scroller.getTop() - FList.LINE_THICKNESS / 2;
                g.drawLine(FList.LINE_THICKNESS, FList.LINE_COLOR, 0, y, getWidth(), y);
            }
        });

        private BazaarPage(QuestStallDefinition stallDef0) {
            super(stallDef0.getName(), (FImage)GuiBase.getInterface().getSkinIcon(stallDef0.getIcon()));
            stallDef = stallDef0;

            lblFluff.setFont(FSkinFont.get(12));
            lblFluff.setAlignment(HAlignment.CENTER);
            lblFluff.setTextColor(FLabel.INLINE_LABEL_COLOR); //make fluff text a little lighter
        }

        @Override
        protected void onActivate() {
            update();
        }

        public void update() {
            scroller.clear();

            final QuestController qData = FModel.getQuest();
            if (qData.getAssets() == null) {
                return;
            }

            final QuestAssets qS = qData.getAssets();
            lblCredits.setText("Credits: " + QuestUtil.formatCredits(qS.getCredits()));
            lblLife.setText("Life: " + qS.getLife(qData.getMode()));

            final List<IQuestBazaarItem> items = qData.getBazaar().getItems(qData, stallDef.getName());

            lblStallName.setText(stallDef.getDisplayName());
            lblFluff.setText(stallDef.getFluff());

            // No items available to purchase?
            if (items.size() == 0) {
                lblEmpty.setVisible(true);
            }
            else {
                lblEmpty.setVisible(false);
                for (IQuestBazaarItem item : items) {
                    scroller.add(new BazaarItemDisplay(item));
                }
            }
            revalidate();
        }

        @Override
        protected void doLayout(float width, float height) {
            float x = PADDING;
            float y = PADDING;
            float w = width - 2 * PADDING;

            lblStallName.setBounds(x, y, w, lblStallName.getAutoSizeBounds().height);
            y += lblStallName.getHeight() + PADDING;
            lblFluff.setBounds(x, y, w, lblFluff.getPreferredHeight(w));
            y += lblFluff.getHeight() + PADDING;
            lblCredits.setBounds(x, y, w / 2, lblCredits.getAutoSizeBounds().height);
            lblLife.setBounds(x + w / 2, y, w / 2, lblCredits.getHeight());
            y += lblCredits.getHeight() + PADDING;
            scroller.setBounds(0, y, width, height - y);
            if (lblEmpty.isVisible()) {
                lblEmpty.setBounds(x, y, w, lblEmpty.getAutoSizeBounds().height);
            }
        }
    }

    private static class BazaarItemDisplay extends FContainer {
        private final FLabel lblName = add(new FLabel.Builder().font(FSkinFont.get(15)).insets(Vector2.Zero).build());
        private final FTextArea lblDesc = add(new FTextArea(false));
        private final FLabel lblIcon = add(new FLabel.Builder().iconInBackground().iconScaleFactor(1f).insets(Vector2.Zero).build());
        private final FLabel lblCost = add(new FLabel.Builder().text("0").icon(FSkinImage.QUEST_COINSTACK).iconScaleFactor(1f).build());
        private final FLabel btnBuy = add(new FLabel.ButtonBuilder().text("Buy").font(FSkinFont.get(20)).build());

        private final IQuestBazaarItem item;

        private BazaarItemDisplay(IQuestBazaarItem item0) {
            item = item0;

            QuestAssets assets = FModel.getQuest().getAssets();
            int buyingPrice = item.getBuyingPrice(assets);

            lblName.setText(item.getPurchaseName());
            lblDesc.setText(item.getPurchaseDescription(assets));
            lblIcon.setIcon((FImage)item.getIcon(assets));
            lblCost.setText(String.valueOf(buyingPrice));

            lblDesc.setFont(FSkinFont.get(12));
            lblDesc.setTextColor(FLabel.INLINE_LABEL_COLOR);

            lblName.setHeight(lblName.getAutoSizeBounds().height);
            btnBuy.setHeight(btnBuy.getAutoSizeBounds().height * 1.2f);
            lblCost.setHeight(lblCost.getAutoSizeBounds().height);

            if (assets.getCredits() < buyingPrice) {
                btnBuy.setEnabled(false);
            }
            else {
                btnBuy.setCommand(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        QuestUtil.buyQuestItem(item);
                        ((BazaarPage)((QuestBazaarScreen)Forge.getCurrentScreen()).getSelectedPage()).update();
                    }
                });
            }
        }

        @Override
        public void drawOverlay(Graphics g) {
            //draw bottom border
            float y = getHeight() - FList.LINE_THICKNESS / 2;
            g.drawLine(FList.LINE_THICKNESS, FList.LINE_COLOR, 0, y, getWidth(), y);
        }

        public float getPreferredHeight(float width) {
            float padding = BazaarPage.PADDING;
            width -= 2 * padding;
            float labelWidth = width * 0.7f;
            float iconWidth = width - labelWidth - padding;
            float iconHeight = iconWidth * lblIcon.getIcon().getHeight() / lblIcon.getIcon().getWidth();
            float height1 = lblName.getHeight() + lblDesc.getPreferredHeight(labelWidth) + 3 * padding;
            float height2 = iconHeight + btnBuy.getHeight() + lblCost.getHeight() + 4 * padding;
            return Math.max(height1, height2);
        }

        @Override
        protected void doLayout(float width, float height) {
            float padding = BazaarPage.PADDING;
            width -= 2 * padding;
            float labelWidth = width * 0.7f;
            float iconWidth = width - labelWidth - padding;
            float iconHeight = iconWidth * lblIcon.getIcon().getHeight() / lblIcon.getIcon().getWidth();

            float x = padding;
            float y = padding;
            lblName.setBounds(x, y, labelWidth, lblName.getHeight());
            y += lblName.getHeight() + padding;
            lblDesc.setBounds(x, y, labelWidth, height - y - padding);
            x += labelWidth + padding;
            y = padding;
            lblIcon.setBounds(x, y, iconWidth, iconHeight);
            y += iconHeight + padding;
            lblCost.setBounds(x, y, iconWidth, lblCost.getHeight());
            y += lblCost.getHeight() + padding;
            btnBuy.setBounds(x, y, iconWidth, btnBuy.getHeight());
        }
    }
}
