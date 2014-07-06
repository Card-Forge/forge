package forge.screens.quest;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.text.WordUtils;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.assets.FSkinFont;
import forge.card.MagicColor;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.game.GameFormat;
import forge.item.PreconDeck;
import forge.model.CardCollections;
import forge.model.FModel;
import forge.quest.QuestController;
import forge.quest.QuestWorld;
import forge.quest.StartingPoolType;
import forge.screens.FScreen;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.util.storage.IStorage;

public class NewQuestScreen extends FScreen {
    private static final float PADDING = FOptionPane.PADDING;

    private final FScrollPane scroller = add(new FScrollPane() {
        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float x = PADDING;
            float y = PADDING;
            float right = visibleWidth - PADDING;
            float h = cbxStartingWorld.getHeight();

            for (FDisplayObject obj : getChildren()) {
                if (!obj.isVisible()) { continue; }

                if (obj instanceof FLabel) {
                    //display labels inline before object following them
                    FLabel lbl = (FLabel)obj;
                    if (lbl.getText().endsWith(":")) {
                        obj.setBounds(x, y, visibleWidth / 2 - x, h); //make label take up half of line so combo boxes all the same width
                        x += obj.getWidth();
                        continue;
                    }
                    else if (lbl.getAlignment() == HAlignment.RIGHT) {
                        y -= PADDING; //remove most of the padding above description text
                    }
                }

                //other objects should take up remainder of current line
                obj.setBounds(x, y, right - x, h);
                x = PADDING;
                y += h + PADDING;
            }
            return new ScrollBounds(visibleWidth, y);
        }
    });

    private final FLabel lblStartingWorld = scroller.add(new FLabel.Builder().text("Starting world:").build());
    private final FComboBox<QuestWorld> cbxStartingWorld = scroller.add(new FComboBox<QuestWorld>());

    private final FLabel lblDifficulty = scroller.add(new FLabel.Builder().text("Difficulty:").build());
    private final FComboBox<String> cbxDifficulty = scroller.add(new FComboBox<String>(new String[]{ "Easy", "Medium", "Hard", "Expert" }));

    private final FLabel lblPreferredColor = scroller.add(new FLabel.Builder().text("Starting pool colors:").build());
    private final FComboBox<String> cbxPreferredColor = scroller.add(new FComboBox<String>());
    private final String stringBalancedDistribution = new String("balanced distribution");
    private final String stringRandomizedDistribution = new String("randomized distribution");
    private final String stringBias = new String(" bias");
    
    private final FLabel lblStartingPool = scroller.add(new FLabel.Builder().text("Starting pool:").build());
    private final FComboBox<StartingPoolType> cbxStartingPool = scroller.add(new FComboBox<StartingPoolType>());

    private final FLabel lblUnrestricted = scroller.add(new FLabel.Builder().align(HAlignment.RIGHT).font(FSkinFont.get(12)).text("All cards will be available to play.").build());

    private final FLabel lblPreconDeck = scroller.add(new FLabel.Builder().text("Starter/Event deck:").build());
    private final FComboBox<String> cbxPreconDeck = scroller.add(new FComboBox<String>());

    private final FLabel lblFormat = scroller.add(new FLabel.Builder().text("Sanctioned format:").build());
    private final FComboBox<GameFormat> cbxFormat = scroller.add(new FComboBox<GameFormat>());

    private final FLabel lblCustomDeck = scroller.add(new FLabel.Builder().text("Custom deck:").build());
    private final FComboBox<Deck> cbxCustomDeck = scroller.add(new FComboBox<Deck>());

    private final FLabel btnDefineCustomFormat = scroller.add(new FLabel.ButtonBuilder().text("Define custom format").build());

    private final FLabel lblPrizedCards = scroller.add(new FLabel.Builder().text("Prized cards:").build());
    private final FComboBox<Object> cbxPrizedCards = scroller.add(new FComboBox<Object>());

    private final FLabel lblPrizeFormat = scroller.add(new FLabel.Builder().text("Sanctioned format:").build());
    private final FComboBox<GameFormat> cbxPrizeFormat = scroller.add(new FComboBox<GameFormat>());

    private final FLabel lblPrizeUnrestricted = scroller.add(new FLabel.Builder().align(HAlignment.RIGHT).font(FSkinFont.get(12)).text("All cards will be available to win.").build());
    private final FLabel lblPrizeSameAsStarting = scroller.add(new FLabel.Builder().align(HAlignment.RIGHT).font(FSkinFont.get(12)).text("Only sets found in starting pool will be available.").build());
    private final FLabel btnPrizeDefineCustomFormat = scroller.add(new FLabel.ButtonBuilder().text("Define custom format").build());

    private final FCheckBox cbAllowUnlocks = scroller.add(new FCheckBox("Allow unlock of additional editions"));
    private final FCheckBox cbFantasy = scroller.add(new FCheckBox("Fantasy Mode"));

    private final FLabel btnEmbark = add(new FLabel.ButtonBuilder()
            .font(FSkinFont.get(16)).text("Embark!").build());

    public NewQuestScreen() {
        super("Start a New Quest");

        cbxStartingPool.addItem(StartingPoolType.Complete);
        cbxStartingPool.addItem(StartingPoolType.Rotating);
        cbxStartingPool.addItem(StartingPoolType.CustomFormat);
        cbxStartingPool.addItem(StartingPoolType.Precon);
        cbxStartingPool.addItem(StartingPoolType.DraftDeck);
        cbxStartingPool.addItem(StartingPoolType.SealedDeck);
        cbxStartingPool.addItem(StartingPoolType.Cube);
        cbxStartingPool.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                updateStartingPoolOptions();
                scroller.revalidate();
            }
        });

        cbxPrizedCards.addItem("Same as starting pool");
        cbxPrizedCards.addItem(StartingPoolType.Complete);
        cbxPrizedCards.addItem(StartingPoolType.Rotating);
        cbxPrizedCards.addItem(StartingPoolType.CustomFormat);
        cbxPrizedCards.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                updatePrizeOptions();
                scroller.revalidate();
            }
        });

        for (GameFormat gf : FModel.getFormats()) {
            cbxFormat.addItem(gf);
            cbxPrizeFormat.addItem(gf);
        }

        // Initialize color balance selection
        cbxPreferredColor.addItem(stringBalancedDistribution);
        cbxPreferredColor.addItem(stringRandomizedDistribution);
        cbxPreferredColor.addItem(MagicColor.Constant.WHITE + stringBias);
        cbxPreferredColor.addItem(MagicColor.Constant.BLUE + stringBias);
        cbxPreferredColor.addItem(MagicColor.Constant.BLACK + stringBias);
        cbxPreferredColor.addItem(MagicColor.Constant.RED + stringBias);
        cbxPreferredColor.addItem(MagicColor.Constant.GREEN + stringBias);
        cbxPreferredColor.addItem(MagicColor.Constant.COLORLESS + stringBias);

        for (QuestWorld qw : FModel.getWorlds()) {
            cbxStartingWorld.addItem(qw);
        }
        // Default to 'Main world'
        cbxStartingWorld.setSelectedItem(FModel.getWorlds().get("Main world"));

        cbxStartingWorld.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                updateEnabledFormats();
            }
        });

        updateStartingPoolOptions();
        updatePrizeOptions();
        updateEnabledFormats();

        cbAllowUnlocks.setSelected(true);

        final Map<String, String> preconDescriptions = new HashMap<String, String>();
        IStorage<PreconDeck> preconDecks = QuestController.getPrecons();

        for (PreconDeck preconDeck : preconDecks) {
            if (QuestController.getPreconDeals(preconDeck).getMinWins() > 0) {
                continue;
            }
            String name = preconDeck.getName();
            cbxPreconDeck.addItem(name);
            String description = preconDeck.getDescription();
            description = "<html>" + WordUtils.wrap(description, 40, "<br>", false) + "</html>";
            preconDescriptions.put(name, description);
        }

        // Fantasy box enabled by Default
        cbFantasy.setSelected(true);
        cbFantasy.setEnabled(true);

        cbxPreferredColor.setEnabled(true);
    }

    private void updateStartingPoolOptions() {
        StartingPoolType newVal = getStartingPoolType();
        lblUnrestricted.setVisible(newVal == StartingPoolType.Complete);

        lblPreconDeck.setVisible(newVal == StartingPoolType.Precon);
        cbxPreconDeck.setVisible(newVal == StartingPoolType.Precon);

        lblFormat.setVisible(newVal == StartingPoolType.Rotating);
        cbxFormat.setVisible(newVal == StartingPoolType.Rotating);

        btnDefineCustomFormat.setVisible(newVal == StartingPoolType.CustomFormat);

        boolean usesDeckList = newVal == StartingPoolType.SealedDeck || newVal == StartingPoolType.DraftDeck || newVal == StartingPoolType.Cube;
        lblCustomDeck.setVisible(usesDeckList);
        cbxCustomDeck.setVisible(usesDeckList);

        if (usesDeckList) {
            cbxCustomDeck.removeAllItems();
            CardCollections decks = FModel.getDecks();
            switch (newVal) {
            case SealedDeck:
                for (DeckGroup d : decks.getSealed()) {
                    cbxCustomDeck.addItem(d.getHumanDeck());
                }
                break;
            case DraftDeck:
                for (DeckGroup d : decks.getDraft()) {
                    cbxCustomDeck.addItem(d.getHumanDeck());
                }
                break;
            case Cube:
                for (Deck d : decks.getCubes()) {
                    cbxCustomDeck.addItem(d);
                }
                break;
            default:
                break;
            }
        }
    }

    private void updatePrizeOptions() {
        StartingPoolType newVal = getPrizedPoolType();
        lblPrizeUnrestricted.setVisible(newVal == StartingPoolType.Complete);
        cbAllowUnlocks.setVisible(newVal != StartingPoolType.Complete);

        lblPrizeFormat.setVisible(newVal == StartingPoolType.Rotating);
        cbxPrizeFormat.setVisible(newVal == StartingPoolType.Rotating);
        btnPrizeDefineCustomFormat.setVisible(newVal == StartingPoolType.CustomFormat);
        lblPrizeSameAsStarting.setVisible(newVal == null);

        scroller.revalidate();
    }

    /**
     * Aux function for enabling or disabling the format selection according to world selection.
     */
    private void updateEnabledFormats() {
        final QuestWorld qw = FModel.getWorlds().get(getStartingWorldName());
        if (qw != null) {
            cbxStartingPool.setEnabled(qw.getFormat() == null);
            cbxFormat.setEnabled(qw.getFormat() == null);
            cbxCustomDeck.setEnabled(qw.getFormat() == null);
            // Do NOT disable the following...
            // cbxPrizeFormat.setEnabled(qw.getFormat() == null);
            // cboAllowUnlocks.setEnabled(qw.getFormat() == null);
            // cbxPrizedCards.setEnabled(qw.getFormat() == null);
        }
    }

    public int getSelectedDifficulty() {
        int difficulty = cbxDifficulty.getSelectedIndex();
        if (difficulty < 0) {
            difficulty = 0;
        }
        return difficulty;
    }

    public String getSelectedPrecon() {
        return cbxPreconDeck.getSelectedItem().toString();
    }

    public Deck getSelectedDeck() {
        Object sel = cbxCustomDeck.getSelectedItem();
        return sel instanceof Deck ? (Deck) sel : null;
    }

    public boolean isUnlockSetsAllowed() {
        return cbAllowUnlocks.isSelected();
    }

    public StartingPoolType getStartingPoolType() {
        return (StartingPoolType) cbxStartingPool.getSelectedItem();
    }

    public StartingPoolType getPrizedPoolType() {
         Object v = cbxPrizedCards.getSelectedItem();
         return v instanceof StartingPoolType ? (StartingPoolType) v : null;
    }

    public String getStartingWorldName() {
        return cbxStartingWorld.getSelectedItem().toString();
    }

    public boolean isFantasy() {
        return cbFantasy.isSelected();
    }

    public boolean randomizeColorDistribution() {
        return stringRandomizedDistribution.equals(cbxPreferredColor.getSelectedItem());
    }

    public byte getPreferredColor() {
        if (stringBalancedDistribution.equals(cbxPreferredColor.getSelectedItem())
                || stringRandomizedDistribution.equals(cbxPreferredColor.getSelectedItem())) {
            return MagicColor.ALL_COLORS;
        }
        return MagicColor.fromName(cbxPreferredColor.getSelectedItem().split(" ")[0]);
    }

    public GameFormat getRotatingFormat() {
        return (GameFormat) cbxFormat.getSelectedItem();
    }

    public GameFormat getPrizedRotatingFormat() {
        return (GameFormat) cbxPrizeFormat.getSelectedItem();
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float buttonHeight = btnEmbark.getAutoSizeBounds().height * 1.2f;
        btnEmbark.setBounds(PADDING, height - buttonHeight - PADDING, width - 2 * PADDING, buttonHeight);
        scroller.setBounds(0, startY, width, btnEmbark.getTop() - startY);
    }
}
