package forge.screens.match.views;

import java.util.Iterator;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.math.Vector2;

import forge.Graphics;
import forge.LobbyPlayer;
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
import forge.game.spellability.TargetChoices;
import forge.game.zone.MagicStack;
import forge.menu.FCheckBoxMenuItem;
import forge.menu.FDropDown;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.screens.match.TargetingOverlay;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.util.Utils;

public class VStack extends FDropDown {
    public static final float CARD_WIDTH = Utils.AVG_FINGER_WIDTH;
    public static final float CARD_HEIGHT = Math.round(CARD_WIDTH * FCardPanel.ASPECT_RATIO);
    public static final float BORDER_THICKNESS = Utils.scaleMin(2);
    public static final float PADDING = Utils.scaleMin(3);
    public static final float MARGINS = Utils.scaleMin(4);
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
        float x = MARGINS;
        float y = MARGINS;
        float totalWidth = Math.min(4 * CARD_WIDTH, maxWidth);
        float width = totalWidth - 2 * MARGINS;

        if (stack.isEmpty()) { //show label if stack empty
            FLabel label = add(new FLabel.Builder().text("[Empty]").font(FONT).align(HAlignment.CENTER).build());

            height = Math.round(label.getAutoSizeBounds().height) + 2 * PADDING;
            label.setBounds(x, y, width, height);
            return new ScrollBounds(totalWidth, y + height + MARGINS);
        }
        else {
            //iterate stack in reverse so most recent items appear on bottom
            height = 0;
            float overlap = Math.round(CARD_HEIGHT / 2 + PADDING + BORDER_THICKNESS);
            Iterator<SpellAbilityStackInstance> iterator = stack.reverseIterator();
            while (true) {
                StackInstanceDisplay display = add(new StackInstanceDisplay(iterator.next(), width));
                if (iterator.hasNext()) { //make items have top half of card be overlapped
                    display.setBounds(x, y, width, overlap);
                    y += overlap;
                }
                else { //use full preferred height of display for bottom item on stack
                    display.setBounds(x, y, width, display.preferredHeight);
                    y += display.preferredHeight;
                    break;
                }
            }
        }
        return new ScrollBounds(totalWidth, y + MARGINS);
    }

    @Override
    protected void setScrollPositionsAfterLayout(float scrollLeft0, float scrollTop0) {
        super.setScrollPositionsAfterLayout(0, 0); //always scroll to top after layout
    }

    @Override
    protected void drawOnContainer(Graphics g) {
        //draw target arrows immediately above stack
        for (FDisplayObject child : getChildren()) {
            Vector2 arrowOrigin = new Vector2(
                    child.getLeft() + VStack.CARD_WIDTH * FCardPanel.TARGET_ORIGIN_FACTOR_X + VStack.PADDING + VStack.BORDER_THICKNESS,
                    child.getTop() + VStack.CARD_HEIGHT * FCardPanel.TARGET_ORIGIN_FACTOR_Y + VStack.PADDING + VStack.BORDER_THICKNESS);

            if (arrowOrigin.y < 0) {
                continue; //don't draw arrow scrolled off top
            }
            if (arrowOrigin.y > getHeight()) {
                break; //don't draw arrow scrolled off bottom
            }

            SpellAbilityStackInstance stackInstance = ((StackInstanceDisplay)child).getStackInstance();
            TargetChoices targets = stackInstance.getSpellAbility().getTargets();
            Player activator = stackInstance.getActivator();
            arrowOrigin = arrowOrigin.add(getScreenPosition());

            for (Card c : targets.getTargetCards()) {
                TargetingOverlay.drawArrow(g, arrowOrigin, c, activator.isOpponentOf(c.getOwner()));
            }
            for (Player p : targets.getTargetPlayers()) {
                TargetingOverlay.drawArrow(g, arrowOrigin, p, activator.isOpponentOf(p));
            }
        }
    }

    private class StackInstanceDisplay extends FDisplayObject {
        private final SpellAbilityStackInstance stackInstance;
        private final Color foreColor, backColor;
        private String text;
        private float preferredHeight;

        private StackInstanceDisplay(SpellAbilityStackInstance stackInstance0, float width) {
            stackInstance = stackInstance0;
            Card card = stackInstance.getSourceCard();

            text = stackInstance.getStackDescription();
            if (stackInstance.getSpellAbility().isOptionalTrigger() &&
                    card.getController().getController().getLobbyPlayer().equals(localPlayer)) {
                text = "(OPTIONAL) " + text;
            }

            DetailColors color = CardDetailUtil.getBorderColor(card, !stackInstance.getStackDescription().startsWith("Morph "));
            backColor = FSkinColor.fromRGB(color.r, color.g, color.b);
            foreColor = FSkinColor.getHighContrastColor(backColor);

            width -= CARD_WIDTH; //account for card picture
            width -= 3 * PADDING + 2 * BORDER_THICKNESS; //account for left and right insets and gap between picture and text
            float height = Math.max(CARD_HEIGHT, textRenderer.getWrappedBounds(text, FONT, width).height);
            height += 2 * (PADDING + BORDER_THICKNESS);
            preferredHeight = Math.round(height);
        }

        public SpellAbilityStackInstance getStackInstance() {
            return stackInstance;
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
        public boolean longPress(float x, float y) {
            CardZoom.show(stackInstance.getSourceCard());
            return true;
        }

        @Override
        public void draw(Graphics g) {
            float x = 0;
            float y = 0;
            float w = getWidth();
            float h = preferredHeight;

            boolean needAlpha = h > getHeight();
            if (needAlpha) { //use alpha for cards below top of stack
                g.setAlphaComposite(ALPHA_COMPOSITE);
            }

            g.startClip(0, 0, w, getHeight()); //clip based on actual height

            g.fillRect(Color.BLACK, x, y, w, h); //draw rectangle for border

            x += BORDER_THICKNESS;
            y += BORDER_THICKNESS;
            w -= 2 * BORDER_THICKNESS;
            h -= 2 * BORDER_THICKNESS;
            g.fillRect(backColor, x, y, w, h);

            x += PADDING;
            y += PADDING;
            CardRenderer.drawCardWithOverlays(g, stackInstance.getSourceCard(), x, y, CARD_WIDTH, CARD_HEIGHT);

            x += CARD_WIDTH + PADDING;
            w -= x + PADDING;
            h -= y + PADDING;
            textRenderer.drawText(g, text, FONT, foreColor, x, y, w, h, y, h, true, HAlignment.LEFT, true);

            g.endClip();

            if (needAlpha) {
                g.resetAlphaComposite();
            }
        }
    }
}
