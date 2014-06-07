package forge.screens.draft;

import forge.screens.LaunchScreen;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.util.Utils;
import forge.assets.FSkinFont;
import forge.deck.DeckProxy;
import forge.game.GameType;
import forge.itemmanager.DeckManager;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;

public class DraftScreen extends LaunchScreen {
    private static final float PADDING = Utils.scaleMin(5);

    private final FLabel btnNewDraft = add(new FLabel.ButtonBuilder().text("New Booster Draft Game").font(FSkinFont.get(16)).build());
    private final DeckManager lstDecks = add(new DeckManager(GameType.Draft));

    public DraftScreen() {
        super("Booster Draft");

        lstDecks.setPool(DeckProxy.getDraftDecks(FModel.getDecks().getDraft()));
        lstDecks.setup(ItemManagerConfig.DRAFT_DECKS);
        btnStart.setEnabled(!lstDecks.getPool().isEmpty());
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        float x = PADDING;
        float y = startY + PADDING;
        float w = width - 2 * PADDING;
        btnNewDraft.setBounds(x, y, w, btnNewDraft.getAutoSizeBounds().height * 1.2f);
        y += btnNewDraft.getHeight() + PADDING;
        lstDecks.setBounds(x, y, w, height - y - PADDING);
    }

    @Override
    protected boolean buildLaunchParams(LaunchParams launchParams) {
        launchParams.gameType = GameType.Draft;
        return false; //TODO: Support launching match
    }
}
