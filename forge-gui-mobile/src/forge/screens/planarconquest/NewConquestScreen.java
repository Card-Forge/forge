package forge.screens.planarconquest;

import forge.FThreads;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.planarconquest.ConquestController;
import forge.planarconquest.ConquestData;
import forge.planarconquest.ConquestPlane;
import forge.planarconquest.ConquestPreferences.CQPref;
import forge.screens.FScreen;
import forge.screens.LoadingOverlay;
import forge.screens.planarconquest.ConquestMenu.LaunchReason;
import forge.toolbox.FChoiceList;
import forge.toolbox.FComboBox;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.util.ThreadUtil;
import forge.util.Utils;

public class NewConquestScreen extends FScreen {
    private static final float EMBARK_BTN_HEIGHT = 2 * Utils.AVG_FINGER_HEIGHT;
    private static final float PADDING = FOptionPane.PADDING;

    private final FLabel lblDifficulty = add(new FLabel.Builder().text("Difficulty:").build());
    private final FComboBox<String> cbxDifficulty = add(new FComboBox<String>(new String[]{ "Easy", "Medium", "Hard", "Expert" }));

    private final FLabel lblStartingPlane = add(new FLabel.Builder().text("Starting plane:").build());
    private final FComboBox<ConquestPlane> cbxStartingPlane = add(new FComboBox<ConquestPlane>(ConquestPlane.values()));

    private final FLabel lblStartingCommander = add(new FLabel.Builder().text("Starting commander:").build());
    private final FChoiceList<PaperCard> lstCommanders = add(new FChoiceList<PaperCard>(cbxStartingPlane.getSelectedItem().getCommanders()));

    private final FLabel btnEmbark = add(new FLabel.ButtonBuilder()
            .font(FSkinFont.get(22)).text("Embark!").icon(FSkinImage.QUEST_ZEP).command(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    //create new quest in game thread so option panes can wait for input
                    ThreadUtil.invokeInGameThread(new Runnable() {
                        @Override
                        public void run() {
                            newConquest();
                        }
                    });
                }
            }).build());

    public NewConquestScreen() {
        super("New Planar Conquest");

        cbxStartingPlane.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                lstCommanders.setListData(cbxStartingPlane.getSelectedItem().getCommanders());
                if (lstCommanders.getCount() > 0) {
                    lstCommanders.setSelectedIndex(0);
                }
            }
        });
    }
 
    public int getSelectedDifficulty() {
        int difficulty = cbxDifficulty.getSelectedIndex();
        if (difficulty < 0) {
            difficulty = 0;
        }
        return difficulty;
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float x = PADDING;
        float y = startY + PADDING;
        float right = width - PADDING;
        float w = width - 2 * PADDING;
        float h = cbxStartingPlane.getHeight();
        float gapY = PADDING / 2;

        lblDifficulty.setBounds(x, y, width / 2 - x, h);
        x += lblDifficulty.getWidth();
        cbxDifficulty.setBounds(x, y, right - x, h);
        x = PADDING;
        y += h + gapY;

        lblStartingPlane.setBounds(x, y, width / 2 - x, h);
        x += lblStartingPlane.getWidth();
        cbxStartingPlane.setBounds(x, y, right - x, h);
        x = PADDING;
        y += h + gapY;

        lblStartingCommander.setBounds(x, y, w, h);
        y += h;
        lstCommanders.setBounds(x, y, w, height - EMBARK_BTN_HEIGHT - 2 * PADDING - y);
        y += lstCommanders.getHeight() + PADDING;

        btnEmbark.setBounds(x, y, w, EMBARK_BTN_HEIGHT);
    }

    private void newConquest() {
        String conquestName = FModel.getConquest().promptForName();
        if (conquestName == null) { return; }
        startNewConquest(conquestName);
    }

    private void startNewConquest(final String conquestName) {
        FThreads.invokeInEdtLater(new Runnable() {
            @Override
            public void run() {
                LoadingOverlay.show("Starting new conquest...", new Runnable() {
                    @Override
                    public void run() {
                        ConquestController qc = FModel.getConquest();
                        qc.load(new ConquestData(conquestName, getSelectedDifficulty(), cbxStartingPlane.getSelectedItem(), lstCommanders.getSelectedItem()));
                        qc.save();

                        // Save in preferences.
                        FModel.getConquestPreferences().setPref(CQPref.CURRENT_CONQUEST, conquestName + ".dat");
                        FModel.getConquestPreferences().save();

                        ConquestMenu.launchPlanarConquest(LaunchReason.NewConquest); //launch quest mode for new quest
                    }
                });
            }
        });
    }
}
