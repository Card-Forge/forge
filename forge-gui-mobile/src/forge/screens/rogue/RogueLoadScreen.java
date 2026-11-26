package forge.screens.rogue;

import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.card.CardAvatarImage;
import forge.gamemodes.rogue.RogueRun;
import forge.gui.FThreads;
import forge.screens.LaunchScreen;
import forge.screens.home.LoadGameMenu;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FLabel;
import forge.util.Utils;

/** Load Game's page for Rogue Commander: the unfinished run, which Start continues on its map. */
public class RogueLoadScreen extends LaunchScreen {
    private RogueRun run;
    private final FDisplayObject row = add(new FDisplayObject() {
        @Override
        public void draw(Graphics g) {
            float w = getWidth();
            float h = getHeight();
            if (run == null) {
                g.drawText(Forge.getLocalizer().getMessage("lblRogueNoUnfinishedRun"), FSkinFont.get(14), FLabel.getInlineLabelColor(),
                    0, 0, w, h / 3, true, Align.center, true);
                return;
            }
            float rh = Utils.AVG_FINGER_HEIGHT * 1.6f;
            float pad = Utils.scale(4);
            g.fillRect(FSkinColor.get(FSkinColor.Colors.CLR_ACTIVE), 0, 0, w, rh);
            new CardAvatarImage(run.getSelectedRogueDeck().getStartDeck().getCommanders().get(0)).draw(g, pad, pad, rh - 2 * pad, rh - 2 * pad);
            float x = rh + pad;
            g.drawText(run.getCurrentCommanderName(), FSkinFont.get(16), FLabel.getDefaultTextColor(), x, pad, w - x, rh / 2, false, Align.left, true);
            String descension = run.getDescensionLevel() == 0 ? Forge.getLocalizer().getMessage("lblOff")
                : String.valueOf(run.getDescensionLevel());
            String detail = Forge.getLocalizer().getMessage("lblRogueDescensionLevel", descension) + " - "
                + Forge.getLocalizer().getMessage("lblRogueLifeTotal", run.getCurrentLife(), run.getMaxLife());
            g.drawText(detail, FSkinFont.get(12), FLabel.getInlineLabelColor(), x, rh / 2, w - x, rh / 2 - pad, false, Align.left, true);
        }
    });

    public RogueLoadScreen() {
        super(null, LoadGameMenu.getMenu());
    }

    @Override
    public void onActivate() {
        RogueController controller = RogueController.get();
        run = controller.flow().run();
        btnStart.setVisible(run != null);
        if (!controller.busy()) {
            controller.queue(() -> {
                RogueRun loaded = controller.flow().load().orElse(null);
                FThreads.invokeInEdtLater(() -> {
                    run = loaded;
                    btnStart.setVisible(run != null);
                });
            });
        }
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        row.setBounds(0, startY, width, height - startY);
    }

    @Override
    protected void startMatch() {
        if (run != null) {
            RogueMenu.launch();
            RogueController.get().openMap();
        }
    }
}
