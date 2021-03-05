package forge.screens.planarconquest;

import com.badlogic.gdx.utils.Align;

import forge.assets.FImage;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.gamemodes.planarconquest.ConquestData;
import forge.gamemodes.planarconquest.ConquestPlane;
import forge.gamemodes.planarconquest.IVConquestStats;
import forge.interfaces.IButton;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.toolbox.FComboBox;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.util.Utils;
import forge.util.Localizer;

public class ConquestStatsScreen extends FScreen implements IVConquestStats {
    private static final float PADDING = Utils.scale(5f);

    private final FComboBox<Object> cbPlanes = add(new FComboBox<>());
    private final FScrollPane scroller = add(new FScrollPane() {
        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float x = 0;
            float y = 0;
            float w = visibleWidth;
            float h = lblAEtherShards.getAutoSizeBounds().height;
            for (FDisplayObject lbl : getChildren()) {
                if (lbl.isVisible()) {
                    lbl.setBounds(x, y, w, lbl.getHeight() == 0 ? h : lbl.getHeight()); //respect height override if set
                    y += lbl.getHeight() + PADDING;
                }
            }
            return new ScrollBounds(visibleWidth, y);
        }
    });
    private final FLabel lblAEtherShards = scroller.add(new StatLabel(FSkinImage.AETHER_SHARD));
    private final FLabel lblPlaneswalkEmblems = scroller.add(new StatLabel(FSkinImage.PW_BADGE_COMMON));
    private final FLabel lblTotalWins = scroller.add(new StatLabel(FSkinImage.QUEST_PLUS));
    private final FLabel lblTotalLosses = scroller.add(new StatLabel(FSkinImage.QUEST_MINUS));
    private final FLabel lblConqueredEvents = scroller.add(new StatLabel(FSkinImage.MULTIVERSE));
    private final FLabel lblUnlockedCards = scroller.add(new StatLabel(FSkinImage.SPELLBOOK));
    private final FLabel lblCommanders = scroller.add(new StatLabel(FSkinImage.COMMANDER));
    private final FLabel lblPlaneswalkers = scroller.add(new StatLabel(FSkinImage.PLANESWALKER));

    public ConquestStatsScreen() {
        super(null, ConquestMenu.getMenu());

        cbPlanes.addItem(Localizer.getInstance().getMessage("lblAllPlanes"));
        for (ConquestPlane plane : FModel.getPlanes()) {
            if (!plane.isUnreachable() || FModel.getConquest().getModel().getCurrentPlane().equals(plane)) {
                cbPlanes.addItem(plane);
            }
        }
        cbPlanes.setAlignment(Align.center);
        cbPlanes.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ConquestPlane plane = cbPlanes.getSelectedIndex() > 0 ? (ConquestPlane)cbPlanes.getSelectedItem() : null;
                FModel.getConquest().getModel().updateStatLabels(ConquestStatsScreen.this, plane);
            }
        });
    }

    @Override
    public void onActivate() {
        update();
    }

    public void update() {
        ConquestData model = FModel.getConquest().getModel();
        setHeaderCaption(model.getName());

        //update plane selector to show current plane
        FEventHandler handler = cbPlanes.getChangedHandler();
        cbPlanes.setChangedHandler(null); //temporarily clear to prevent updating plane stats twice
        cbPlanes.setSelectedItem(model.getCurrentPlane());
        cbPlanes.setChangedHandler(handler);

        model.updateStatLabels(this, model.getCurrentPlane());
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float x = PADDING;
        float y = startY + PADDING;
        width -= 2 * x;

        cbPlanes.setBounds(x, y, width, cbPlanes.getHeight());
        y += cbPlanes.getHeight() + PADDING;
        scroller.setBounds(x, y, width, height - PADDING - y);
    }

    @Override
    public IButton getLblAEtherShards() {
        return lblAEtherShards;
    }
    @Override
    public IButton getLblPlaneswalkEmblems() {
        return lblPlaneswalkEmblems;
    }
    @Override
    public IButton getLblTotalWins() {
        return lblTotalWins;
    }
    @Override
    public IButton getLblTotalLosses() {
        return lblTotalLosses;
    }
    @Override
    public IButton getLblConqueredEvents() {
        return lblConqueredEvents;
    }
    @Override
    public IButton getLblUnlockedCards() {
        return lblUnlockedCards;
    }
    @Override
    public IButton getLblCommanders() {
        return lblCommanders;
    }
    @Override
    public IButton getLblPlaneswalkers() {
        return lblPlaneswalkers;
    }

    private static class StatLabel extends FLabel {
        private StatLabel(FImage icon0) {
            super(new FLabel.Builder().icon(icon0).font(FSkinFont.get(14)).iconScaleFactor(1));
        }
    }
}
