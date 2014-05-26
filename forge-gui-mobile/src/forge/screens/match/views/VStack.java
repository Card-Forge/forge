package forge.screens.match.views;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.LobbyPlayer;
import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.card.CardDetailUtil;
import forge.card.CardRenderer;
import forge.card.CardZoom;
import forge.card.CardDetailUtil.DetailColors;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.MagicStack;
import forge.menu.FCheckBoxMenuItem;
import forge.menu.FDropDown;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.util.Utils;

public class VStack extends FDropDown {
    public static final float CARD_WIDTH = Utils.AVG_FINGER_WIDTH;
    public static final float CARD_HEIGHT = Math.round(CARD_WIDTH * FCardPanel.ASPECT_RATIO);
    private static final float PADDING = Utils.scaleMin(3);
    private static final FSkinFont FONT = FSkinFont.get(11);
    private static final float ALPHA_COMPOSITE = 0.5f;
    private static final TextRenderer textRenderer = new TextRenderer(true);

    private final MagicStack stack;
    private final LobbyPlayer localPlayer;

    private int stackSize;

    public VStack(MagicStack stack0, LobbyPlayer localPlayer0) {
        stack = stack0;
        localPlayer = localPlayer0;
    }

    @Override
    protected boolean autoHide() {
        return false;
    }

    @Override
    public void update() {
        if (stackSize != stack.size()) {
            int oldStackSize = stackSize;
            stackSize = stack.size();
            getMenuTab().setText("Stack (" + stackSize + ")");

            if (stackSize > 0) {
                if (!isVisible()) {
                    if (stackSize > oldStackSize) { //don't re-show stack if user hid it and then resolved an item on the stack
                        show();
                    }
                    return; //don't call super.update() either way since show handles this
                }
            }
            else {
                hide();
                return; //super.update() isn't needed if hidden
            }
        }
        super.update();
    }

    @Override
    protected ScrollBounds updateAndGetPaneSize(float maxWidth, float maxVisibleHeight) {
        clear();

        float height;
        float x = PADDING;
        float y = PADDING;
        float dy = PADDING - 1;
        float totalWidth = Math.min(4 * CARD_WIDTH, maxWidth);
        float width = totalWidth - 2 * PADDING;

        if (stack.isEmpty()) { //show label if stack empty
            FLabel label = add(new FLabel.Builder().text("[Empty]").fontSize(FONT.getSize()).align(HAlignment.CENTER).build());

            height = Math.round(label.getAutoSizeBounds().height) + 2 * PADDING;
            label.setBounds(x, y, width, height);
            return new ScrollBounds(totalWidth, y + height + PADDING);
        }
        else {
            StackInstanceDisplay display;
            boolean isTop = true;
            for (final SpellAbilityStackInstance stackInstance : stack) {
                display = add(new StackInstanceDisplay(stackInstance, isTop));
                height = display.getMinHeight(width);
                display.setBounds(x, y, width, height);
                y += height + dy;
                isTop = false;
            }
        }
        return new ScrollBounds(totalWidth, y + 1);
    }

    @Override
    protected void setScrollPositionsAfterLayout(float scrollLeft0, float scrollTop0) {
        super.setScrollPositionsAfterLayout(0, 0); //always scroll to top after layout
    }

    private class StackInstanceDisplay extends FDisplayObject {
        private final SpellAbilityStackInstance stackInstance;
        private final boolean isTop;
        private final Color foreColor, backColor;
        private String text;

        private StackInstanceDisplay(SpellAbilityStackInstance stackInstance0, boolean isTop0) {
            stackInstance = stackInstance0;
            isTop = isTop0;
            Card card = stackInstance.getSourceCard();

            text = stackInstance.getStackDescription();
            if (stackInstance.getSpellAbility().isOptionalTrigger() &&
                    card.getController().getController().getLobbyPlayer().equals(localPlayer)) {
                text = "(OPTIONAL) " + text;
            }

            DetailColors color = CardDetailUtil.getBorderColor(card, !stackInstance.getStackDescription().startsWith("Morph "));
            backColor = FSkinColor.fromRGB(color.r, color.g, color.b);
            foreColor = FSkinColor.getHighContrastColor(backColor);
        }

        private float getMinHeight(float width) {
            width -= CARD_WIDTH; //account for card picture
            width -= 3 * PADDING; //account for left and right insets and gap between picture and text
            float height = Math.max(CARD_HEIGHT, textRenderer.getWrappedBounds(text, FONT, width).height);
            height += 2 * PADDING;
            return Math.round(height);
        }

        @Override
        public boolean tap(float x, float y, int count) {
            final Player player = stackInstance.getSpellAbility().getActivatingPlayer();
            final PlayerController controller = player.getController();
            if (stackInstance.getSpellAbility().isOptionalTrigger() && player.getLobbyPlayer() == localPlayer && controller != null) {
                FPopupMenu menu = new FPopupMenu() {
                    @Override
                    protected void buildMenu() {
                        final int triggerID = stackInstance.getSpellAbility().getSourceTrigger();
                        addItem(new FCheckBoxMenuItem("Always Yes",
                                controller.shouldAlwaysAcceptTrigger(triggerID),
                                new FEventHandler() {
                            @Override
                            public void handleEvent(FEvent e) {
                                controller.setShouldAlwaysAcceptTrigger(triggerID);
                            }
                        }));
                        addItem(new FCheckBoxMenuItem("Always No",
                                controller.shouldAlwaysDeclineTrigger(triggerID),
                                new FEventHandler() {
                            @Override
                            public void handleEvent(FEvent e) {
                                controller.setShouldAlwaysDeclineTrigger(triggerID);
                            }
                        }));
                        addItem(new FCheckBoxMenuItem("Always Ask",
                                controller.shouldAlwaysAskTrigger(triggerID),
                                new FEventHandler() {
                            @Override
                            public void handleEvent(FEvent e) {
                                controller.setShouldAlwaysAskTrigger(triggerID);
                            }
                        }));
                        addItem(new FMenuItem("Zoom/Details", new FEventHandler() {
                            @Override
                            public void handleEvent(FEvent e) {
                                CardZoom.show(stackInstance.getSourceCard());
                            }
                        }));
                    }
                };

                menu.show(this, x, y);
            }
            else {
                CardZoom.show(stackInstance.getSourceCard());
            }
            return true;
        }

        @Override
        public void draw(Graphics g) {
            float w = getWidth();
            float h = getHeight();

            if (!isTop) {
                g.setAlphaComposite(ALPHA_COMPOSITE);
            }

            g.fillRect(backColor, 0, 0, w, h);

            float padding = PADDING;
            float cardWidth = CARD_WIDTH;
            float cardHeight = CARD_HEIGHT;
            float x = padding;
            float y = padding;

            CardRenderer.drawCardWithOverlays(g, stackInstance.getSourceCard(), x, y, cardWidth, cardHeight);

            x += cardWidth + padding;
            w -= x + padding;
            h -= y + padding;
            textRenderer.drawText(g, text, FONT, foreColor, x, y, w, h, y, h, true, HAlignment.LEFT, true);

            if (!isTop) {
                g.resetAlphaComposite();
            }
        }
    }
}
