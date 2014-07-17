package forge.screens.quest;

import java.util.List;
import java.util.Set;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.assets.FImage;
import forge.assets.FSkinFont;
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
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextArea;

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
        private final QuestStallDefinition stallDef;
        private final FLabel lblStallName = add(new FLabel.Builder().text("").align(HAlignment.CENTER).build());
        private final FLabel lblEmpty = add(new FLabel.Builder().text("The merchant does not have anything useful for sale.")
                .align(HAlignment.CENTER).build());
        private final FLabel lblStats = add(new FLabel.Builder().align(HAlignment.CENTER).font(FSkinFont.get(12)).build());
        private final FTextArea lblFluff = add(new FTextArea());
        private final FScrollPane scroller = add(new FScrollPane() {
            @Override
            protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
                float y = 0;
                for (FDisplayObject child : getChildren()) {
                    child.setBounds(0, y, visibleWidth, child.getHeight());
                    y += child.getHeight();
                }
                return new ScrollBounds(visibleWidth, y);
            }
        });

        private BazaarPage(QuestStallDefinition stallDef0) {
            super(stallDef0.getName(), (FImage)stallDef0.getIcon());
            stallDef = stallDef0;

            lblFluff.setFont(FSkinFont.get(15));
            lblFluff.setAlignment(HAlignment.CENTER);
        }

        public void update() {
            scroller.clear();

            final QuestController qData = FModel.getQuest();
            if (qData.getAssets() == null) {
                return;
            }

            final QuestAssets qS = qData.getAssets();
            lblStats.setText("Credits: " + qS.getCredits() + "         Life: " + qS.getLife(qData.getMode()));

            final List<IQuestBazaarItem> items = qData.getBazaar().getItems(qData, stallDef.getName());

            lblStallName.setText(stallDef.getDisplayName());
            lblFluff.setText(stallDef.getFluff());

            // No items available to purchase?
            if (items.size() == 0) {
                lblEmpty.setVisible(true);
            }
            else {
                for (IQuestBazaarItem item : items) {
                    scroller.add(new BazaarItemDisplay(item));
                }
            }
            revalidate();
        }

        @Override
        protected void doLayout(float width, float height) {
        }
    }

    private static class BazaarItemDisplay extends FContainer {
        private final IQuestBazaarItem item;

        private BazaarItemDisplay(IQuestBazaarItem item0) {
            item = item0;
        }

        @Override
        protected void doLayout(float width, float height) {
            // TODO Auto-generated method stub
            
        }
    }
}
