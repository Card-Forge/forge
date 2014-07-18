package forge.screens.quest;

import java.util.List;
import java.util.Set;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.math.Vector2;

import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.model.FModel;
import forge.quest.QuestController;
import forge.quest.bazaar.IQuestBazaarItem;
import forge.quest.bazaar.QuestBazaarManager;
import forge.quest.bazaar.QuestStallDefinition;
import forge.quest.data.QuestAssets;
import forge.screens.TabPageScreen;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FLabel;
import forge.toolbox.FList;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextArea;
import forge.util.Utils;

public class QuestBazaarScreen extends TabPageScreen<QuestBazaarScreen> {
    public QuestBazaarScreen() {
        super(getPages());
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
        private static final float PADDING = Utils.scaleMax(5);

        private final QuestStallDefinition stallDef;
        private final FLabel lblStallName = add(new FLabel.Builder().text("").align(HAlignment.CENTER).build());
        private final FLabel lblEmpty = add(new FLabel.Builder().font(FSkinFont.get(12))
                .text("The merchant does not have anything useful for sale.")
                .align(HAlignment.CENTER).build());
        private final FLabel lblCredits = add(new FLabel.Builder().font(FSkinFont.get(15)).icon(FSkinImage.QUEST_COINSTACK).iconScaleFactor(1f).build());
        private final FLabel lblLife = add(new FLabel.Builder().font(lblCredits.getFont()).icon(FSkinImage.QUEST_LIFE).iconScaleFactor(1f).align(HAlignment.RIGHT).build());
        private final FTextArea lblFluff = add(new FTextArea());
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
            super(stallDef0.getName(), (FImage)stallDef0.getIcon());
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
            lblCredits.setText("Credits: " + qS.getCredits());
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
        private static final float MIN_HEIGHT = Utils.AVG_FINGER_HEIGHT * 2.5f;

        private final FLabel btnBuy = add(new FLabel.ButtonBuilder().text("Buy").font(FSkinFont.get(20)).build());
        private final FLabel lblName = add(new FLabel.Builder().font(FSkinFont.get(15)).insets(Vector2.Zero).build());
        private final FTextArea lblDesc = add(new FTextArea());
        private final FLabel lblCost = add(new FLabel.Builder().text("0").icon(FSkinImage.QUEST_COINSTACK).iconScaleFactor(1f).build());

        private final IQuestBazaarItem item;
        private final FImage icon;

        private BazaarItemDisplay(IQuestBazaarItem item0) {
            item = item0;

            QuestAssets assets = FModel.getQuest().getAssets();
            icon = (FImage)item.getIcon(assets);

            lblName.setText(item.getPurchaseName());
            lblDesc.setText(item.getPurchaseDescription(assets));
            lblCost.setText(String.valueOf(item.getBuyingPrice(assets)));

            lblDesc.setFont(FSkinFont.get(12));
            lblDesc.setTextColor(FLabel.INLINE_LABEL_COLOR);
            lblCost.setHeight(lblCost.getAutoSizeBounds().height);

            float buttonSize = MIN_HEIGHT - lblCost.getHeight() - 3 * BazaarPage.PADDING;
            btnBuy.setSize(buttonSize, buttonSize);
        }

        @Override
        public void drawBackground(Graphics g) {
            if (icon != null) {
                float iconWidth = btnBuy.getWidth();
                g.drawImage(icon, BazaarPage.PADDING / 2, BazaarPage.PADDING, iconWidth, iconWidth * icon.getHeight() / icon.getWidth());
            }

            //draw bottom border
            float y = getHeight() - FList.LINE_THICKNESS / 2;
            g.drawLine(FList.LINE_THICKNESS, FList.LINE_COLOR, 0, y, getWidth(), y);
        }

        public float getPreferredHeight(float width) {
            float padding = BazaarPage.PADDING;
            width -= 2 * (btnBuy.getHeight() + padding);
            float height = lblName.getAutoSizeBounds().height + lblDesc.getPreferredHeight(width) + 3 * padding;
            if (height < MIN_HEIGHT) {
                height = MIN_HEIGHT;
            }
            return height;
        }

        @Override
        protected void doLayout(float width, float height) {
            float padding = BazaarPage.PADDING;
            float buttonSize = btnBuy.getHeight();

            float x = width - padding - buttonSize;
            float y = padding;
            btnBuy.setPosition(x, y);
            y += buttonSize + padding;
            lblCost.setBounds(x, y, buttonSize, lblCost.getHeight());

            float w = x - buttonSize - padding;
            x = buttonSize + padding;
            y = padding;
            lblName.setBounds(x, y, w, lblName.getAutoSizeBounds().height);
            y += lblName.getHeight() + padding;
            lblDesc.setBounds(x, y, w, height - padding - y);
        }
    }
}
