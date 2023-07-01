package forge.deck;

import forge.Forge;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.toolbox.FButton;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Aggregates;
import forge.util.Utils;

public class FVanguardChooser extends FScreen {
    public static final float PADDING = Utils.scale(5);

    private static final CardPool allHumanAvatars = new CardPool();
    private static final CardPool allAiAvatars = new CardPool();
    private static final CardPool nonRandomHumanAvatars = new CardPool();
    private static final CardPool nonRandomAiAvatars = new CardPool();
    
    static {
        for (PaperCard c : FModel.getMagicDb().getVariantCards().getAllCards()) {
            if (c.getRules().getType().isVanguard()) {
                allHumanAvatars.add(c);
                if (!c.getRules().getAiHints().getRemRandomDecks()) {
                    nonRandomHumanAvatars.add(c);
                }
                if (!c.getRules().getAiHints().getRemAIDecks()) {
                    allAiAvatars.add(c);
                    if (!c.getRules().getAiHints().getRemRandomDecks()) {
                        nonRandomAiAvatars.add(c);
                    }
                }
            }
        }
    }

    private final CardManager lstVanguards = add(new CardManager(true));
    private final FButton btnRandom = add(new FButton(Forge.getLocalizer().getMessage("lblRandomVanguard")));
    private boolean isAi;

    public FVanguardChooser(boolean isAi0, FEventHandler selectionChangedHandler) {
        super("");
        isAi = isAi0;
        lstVanguards.setItemActivateHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                Forge.back();
            }
        });
        btnRandom.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                selectRandom();
                Forge.back();
            }
        });
        lstVanguards.setup(ItemManagerConfig.VANGUARDS);
        lstVanguards.setPool(isAi ? allAiAvatars : allHumanAvatars, true);
        lstVanguards.setSelectionChangedHandler(selectionChangedHandler);
        selectRandom();
    }

    private void selectRandom() {
        if (lstVanguards.getItemCount() == 0) { return; }

        if (isAi) {
            lstVanguards.setSelectedItem(Aggregates.random(nonRandomAiAvatars).getKey());
        }
        else {
            lstVanguards.setSelectedItem(Aggregates.random(nonRandomHumanAvatars).getKey());
        }
    }

    public void setIsAi(boolean isAi0) {
        if (isAi == isAi0) { return; }
        isAi = isAi0;

        PaperCard lastSelection = lstVanguards.getSelectedItem();

        lstVanguards.setPool(isAi ? allAiAvatars : allHumanAvatars, true);

        if (lastSelection != null) {
            lstVanguards.setSelectedItem(lastSelection);
        }
        if (lstVanguards.getSelectedIndex() == -1) {
            selectRandom();
        }
    }

    public CardManager getLstVanguards() {
        return lstVanguards;
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float x = PADDING;
        float y = startY;
        width -= 2 * x;

        float buttonHeight = Utils.AVG_FINGER_HEIGHT;
        lstVanguards.setBounds(x, y, width, height - y - buttonHeight - 2 * PADDING); //leave room for buttons at bottom

        y += lstVanguards.getHeight() + PADDING;
        btnRandom.setBounds(x, y, width, buttonHeight);
    }
}
