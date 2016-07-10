package forge.screens.quest;

import java.util.List;

import forge.assets.FImage;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.interfaces.IButton;
import forge.interfaces.ICheckBox;
import forge.interfaces.IComboBox;
import forge.model.FModel;
import forge.quest.QuestUtil;
import forge.quest.bazaar.QuestItemType;
import forge.quest.bazaar.QuestPetController;
import forge.screens.FScreen;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.util.Utils;

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
                if (lbl.isVisible()) {
                    lbl.setBounds(x, y, w, lbl.getHeight() == 0 ? h : lbl.getHeight()); //respect height override if set
                    y += lbl.getHeight() + PADDING;
                }
            }
            return new ScrollBounds(visibleWidth, y);
        }
    });
    private final FLabel lblWins = scroller.add(new StatLabel(FSkinImage.QUEST_PLUS));
    private final FLabel lblLosses = scroller.add(new StatLabel(FSkinImage.QUEST_MINUS));
    private final FLabel lblCredits = scroller.add(new StatLabel(FSkinImage.QUEST_COINSTACK));
    private final FLabel lblWinStreak = scroller.add(new StatLabel(FSkinImage.QUEST_PLUSPLUS));
    private final FLabel lblLife = scroller.add(new StatLabel(FSkinImage.QUEST_LIFE));
    private final FLabel lblWorld = scroller.add(new StatLabel(FSkinImage.QUEST_MAP));
    private final FComboBox<String> cbxPet = scroller.add(new FComboBox<String>());
    private final FComboBox<String> cbxMatchLength  = scroller.add(new FComboBox<String>());
    private final FCheckBox cbPlant = scroller.add(new FCheckBox("Summon Plant"));
    private final FLabel lblZep = scroller.add(new FLabel.Builder().text("Launch Zeppelin").icon(FSkinImage.QUEST_ZEP).font(FSkinFont.get(16)).opaque().build());

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
    public IComboBox<String> getCbxPet() {
        return cbxPet;
    }
    public ICheckBox getCbPlant() {
        return cbPlant;
    }
    public IComboBox<String> getCbxMatchLength() {
        return cbxMatchLength;
    }
    public IButton getLblZep() {
        return lblZep;
    }

    public QuestStatsScreen() {
        super("Quest Statistics", QuestMenu.getMenu());
        lblZep.setHeight(Utils.scale(60));

        cbxPet.setDropDownChangeHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                final int slot = 1;
                final int index = cbxPet.getSelectedIndex();
                List<QuestPetController> pets = FModel.getQuest().getPetsStorage().getAvaliablePets(slot, FModel.getQuest().getAssets());
                String petName = index <= 0 || index > pets.size() ? null : pets.get(index - 1).getName();
                FModel.getQuest().selectPet(slot, petName);
                FModel.getQuest().save();
            }
        });

        cbxMatchLength.setDropDownChangeHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                String match = cbxMatchLength.getSelectedItem();
                if (match != null) {
                    FModel.getQuest().setMatchLength(match.substring(match.length() - 1));
                    FModel.getQuest().save();
                }
            }
        });

        cbPlant.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                FModel.getQuest().selectPet(0, cbPlant.isSelected() ? "Plant" : null);
                FModel.getQuest().save();
            }
        });
        lblZep.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if (!QuestUtil.checkActiveQuest("Launch a Zeppelin.")) {
                    return;
                }
                FModel.getQuest().getAchievements().setCurrentChallenges(null);
                FModel.getQuest().getAssets().setItemLevel(QuestItemType.ZEPPELIN, 2);
                update();
            }
        });
    }

    void addTournamentResultsLabels(QuestTournamentsScreen tournamentsScreen) {
        scroller.add(new FLabel.Builder().font(FSkinFont.get(16)).text("Tournament Results").build());
        scroller.add(tournamentsScreen.getLblFirst());
        scroller.add(tournamentsScreen.getLblSecond());
        scroller.add(tournamentsScreen.getLblThird());
        scroller.add(tournamentsScreen.getLblFourth());
    }

    @Override
    public void onActivate() {
        update();
    }

    public void update() {
        QuestUtil.updateQuestView(QuestMenu.getMenu());
        setHeaderCaption(FModel.getQuest().getName() + " - Statistics\n(" + FModel.getQuest().getRank() + ")");
        scroller.revalidate(); //revalidate to account for changes in label visibility
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        scroller.setBounds(0, startY, width, height - startY);
    }

    private static class StatLabel extends FLabel {
        private StatLabel(FImage icon0) {
            super(new FLabel.Builder().icon(icon0).font(FSkinFont.get(16)).iconScaleFactor(1));
        }
    }
}
