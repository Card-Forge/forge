package forge.screens.quest;

import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.model.FModel;
import forge.quest.QuestUtil;
import forge.screens.FScreen;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;

public class QuestStatsScreen extends FScreen {
    private static final float PADDING = FOptionPane.PADDING;

    private final FScrollPane scroller = add(new FScrollPane() {
        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float x = PADDING;
            float y = PADDING;
            float w = visibleWidth - 2 * PADDING;
            float h = lblWins.getAutoSizeBounds().height;
            for (FDisplayObject lbl : getChildren()) {
                lbl.setBounds(x, y, w, h);
                y += h + PADDING;
            }
            return new ScrollBounds(visibleWidth, y);
        }
    });
    private final FLabel lblWins = scroller.add(new FLabel.Builder()
        .icon(FSkinImage.QUEST_PLUS)
        .font(FSkinFont.get(16)).iconScaleFactor(1).build());
    private final FLabel lblLosses = scroller.add(new FLabel.Builder()
        .icon(FSkinImage.QUEST_MINUS)
        .font(FSkinFont.get(16)).iconScaleFactor(1).build());
    private final FLabel lblCredits = scroller.add(new FLabel.Builder()
        .icon(FSkinImage.QUEST_COINSTACK)
        .font(FSkinFont.get(16)).iconScaleFactor(1).build());
    private final FLabel lblWinStreak = scroller.add(new FLabel.Builder()
        .icon(FSkinImage.QUEST_PLUSPLUS)
        .font(FSkinFont.get(16)).iconScaleFactor(1).build());
    private final FLabel lblLife = scroller.add(new FLabel.Builder()
        .icon(FSkinImage.QUEST_LIFE)
        .font(FSkinFont.get(16)).iconScaleFactor(1).build());
    private final FLabel lblWorld = scroller.add(new FLabel.Builder()
        .icon(FSkinImage.QUEST_MAP)
        .font(FSkinFont.get(16)).iconScaleFactor(1).build());

    public FLabel getLblWins() {
        return lblWins;
    }
    public FLabel getLblLosses() {
        return lblLosses;
    }
    public FLabel getLblCredits() {
        return lblCredits;
    }
    public FLabel getLblWinStreak() {
        return lblWinStreak;
    }
    public FLabel getLblLife() {
        return lblLife;
    }
    public FLabel getLblWorld() {
        return lblWorld;
    }

    public QuestStatsScreen() {
        super("Quest Statistics", QuestMenu.getMenu());
    }

    @Override
    public void onActivate() {
        QuestUtil.updateQuestView(QuestMenu.getMenu());
        setHeaderCaption(FModel.getQuest().getName() + " - Statistics\n(" + FModel.getQuest().getRank() + ")");
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        scroller.setBounds(0, startY, width, height - startY);
    }
}
