package forge.screens.quest;

import java.util.Set;

import forge.assets.FImage;
import forge.model.FModel;
import forge.quest.bazaar.QuestBazaarManager;
import forge.quest.bazaar.QuestStallDefinition;
import forge.screens.TabPageScreen;

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

        private BazaarPage(QuestStallDefinition stallDef0) {
            super(stallDef0.getName(), (FImage)stallDef0.getIcon());
            stallDef = stallDef0;
        }

        @Override
        protected void doLayout(float width, float height) {
        }
    }
}
