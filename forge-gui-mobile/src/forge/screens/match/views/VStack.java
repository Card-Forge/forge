package forge.screens.match.views;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.math.Vector2;

import forge.Graphics;
import forge.GuiBase;
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
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetChoices;
import forge.game.zone.MagicStack;
import forge.game.zone.ZoneType;
import forge.match.input.InputConfirm;
import forge.menu.FCheckBoxMenuItem;
import forge.menu.FDropDown;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.screens.match.FControl;
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
    private final Player localPlayer;
    private StackInstanceDisplay activeItem;
    private SpellAbilityStackInstance activeStackInstance;
    private Map<Player, Object> playersWithValidTargets;

    private int stackSize;

    public VStack(MagicStack stack0, Player localPlayer0) {
        stack = stack0;
        localPlayer = localPlayer0;
    }

    @Override
    protected boolean autoHide() {
        return false;
    }

    //temporarily reveal zones targeted by active stack instance
    private void revealTargetZones() {
        if (activeStackInstance == null) { return; }

        final List<ZoneType> zones = activeStackInstance.getZonesToOpen();
        playersWithValidTargets = activeStackInstance.getPlayersWithValidTargets();
        if (zones != null && zones.size() > 0 && playersWithValidTargets != null && playersWithValidTargets.size() > 0) {
            GuiBase.getInterface().openZones(zones, playersWithValidTargets);
        }
    }

    //restore old zones when active stack instance changes
    private void restoreOldZones() {
        if (playersWithValidTargets == null) { return; }
        GuiBase.getInterface().restoreOldZones(playersWithValidTargets);
        playersWithValidTargets = null;
    }

    @Override
    public void update() {
        activeItem = null;
        activeStackInstance = null; //reset before updating stack
        restoreOldZones();

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

        float x = MARGINS;
        float y = MARGINS;
        float totalWidth = Math.min(4 * CARD_WIDTH, maxWidth);
        float width = totalWidth - 2 * MARGINS;

        if (stack.isEmpty()) { //show label if stack empty
            FLabel label = add(new FLabel.Builder().text("[Empty]").font(FONT).align(HAlignment.CENTER).build());

            float height = Math.round(label.getAutoSizeBounds().height) + 2 * PADDING;
            label.setBounds(x, y, width, height);
            return new ScrollBounds(totalWidth, y + height + MARGINS);
        }
        else {
            //iterate stack in reverse so most recent items appear on bottom
            SpellAbilityStackInstance stackInstance = null;
            StackInstanceDisplay display = null;
            float overlap = Math.round(CARD_HEIGHT / 2 + PADDING + BORDER_THICKNESS);
            Iterator<SpellAbilityStackInstance> iterator = stack.reverseIterator();
            while (iterator.hasNext()) {
                stackInstance = iterator.next();
                display = new StackInstanceDisplay(stackInstance, width);
                if (activeStackInstance == stackInstance) {
                    activeItem = display;
                }
                else { //only add non-active items here
                    add(display);
                }
                //use full preferred height of display for topmost item on stack, overlap amount for other items
                display.setBounds(x, y, width, iterator.hasNext() ? overlap : display.preferredHeight);
                y += display.getHeight();
            }
            if (activeStackInstance == null) {
                activeStackInstance = stackInstance; //use topmost item on stack as default active item
                activeItem = display;
            }
            else {
                activeItem.setHeight(display.preferredHeight); //increase active item height to preferred height if needed
                if (activeItem.getBottom() > y) {
                    y = activeItem.getBottom(); //ensure stack height increases if needed
                }
                add(activeItem);
            }
            scrollIntoView(activeItem); //scroll active display into view
            revealTargetZones();
        }
        return new ScrollBounds(totalWidth, y + MARGINS);
    }

    @Override
    protected void setScrollPositionsAfterLayout(float scrollLeft0, float scrollTop0) {
        super.setScrollPositionsAfterLayout(0, 0); //always scroll to top after layout
    }

    @Override
    protected void drawOnContainer(Graphics g) {
        //draw target arrows immediately above stack for active item only
        if (activeItem != null) {
            Vector2 arrowOrigin = new Vector2(
                    activeItem.getLeft() + VStack.CARD_WIDTH * FCardPanel.TARGET_ORIGIN_FACTOR_X + VStack.PADDING + VStack.BORDER_THICKNESS,
                    activeItem.getTop() + VStack.CARD_HEIGHT * FCardPanel.TARGET_ORIGIN_FACTOR_Y + VStack.PADDING + VStack.BORDER_THICKNESS);

            TargetChoices targets = activeStackInstance.getSpellAbility().getTargets();
            Player activator = activeStackInstance.getActivator();
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

        @Override
        public boolean tap(float x, float y, int count) {
            if (activeStackInstance != stackInstance) { //set as active stack instance if not already such
                activeStackInstance = stackInstance;
                restoreOldZones(); //restore old zones before changing active stack instance
                VStack.this.updateSizeAndPosition();
                return true;
            }
            if (localPlayer != null) { //don't show menu if tapping on art
                final SpellAbility ability = stackInstance.getSpellAbility();
                if (ability.isAbility()) {
                    FPopupMenu menu = new FPopupMenu() {
                        @Override
                        protected void buildMenu() {
                            final PlayerController controller = localPlayer.getController();
                            final String key = ability.toUnsuppressedString();
                            final boolean autoYield = controller.shouldAutoYield(key);
                            addItem(new FCheckBoxMenuItem("Auto-Yield",
                                    autoYield,
                                    new FEventHandler() {
                                @Override
                                public void handleEvent(FEvent e) {
                                    controller.setShouldAutoYield(key, !autoYield);
                                    if (!autoYield && stack.peekAbility() == ability) {
                                        //auto-pass priority if ability is on top of stack
                                        FControl.getInputProxy().passPriority();
                                    }
                                }
                            }));
                            if (ability.isOptionalTrigger() && ability.getActivatingPlayer() == localPlayer) {
                                final int triggerID = ability.getSourceTrigger();
                                addItem(new FCheckBoxMenuItem("Always Yes",
                                        controller.shouldAlwaysAcceptTrigger(triggerID),
                                        new FEventHandler() {
                                    @Override
                                    public void handleEvent(FEvent e) {
                                        if (controller.shouldAlwaysAcceptTrigger(triggerID)) {
                                            controller.shouldAlwaysAskTrigger(triggerID);
                                        }
                                        else {
                                            controller.setShouldAlwaysAcceptTrigger(triggerID);
                                            if (stack.peekAbility() == ability &&
                                                    FControl.getInputQueue().getInput() instanceof InputConfirm) {
                                                //auto-yes if ability is on top of stack
                                                FControl.getInputProxy().selectButtonOK();
                                            }
                                        }
                                    }
                                }));
                                addItem(new FCheckBoxMenuItem("Always No",
                                        controller.shouldAlwaysDeclineTrigger(triggerID),
                                        new FEventHandler() {
                                    @Override
                                    public void handleEvent(FEvent e) {
                                        if (controller.shouldAlwaysDeclineTrigger(triggerID)) {
                                            controller.shouldAlwaysAskTrigger(triggerID);
                                        }
                                        else {
                                            controller.setShouldAlwaysDeclineTrigger(triggerID);
                                            if (stack.peekAbility() == ability &&
                                                    FControl.getInputQueue().getInput() instanceof InputConfirm) {
                                                //auto-no if ability is on top of stack
                                                FControl.getInputProxy().selectButtonOK();
                                            }
                                        }
                                    }
                                }));
                            }
                            addItem(new FMenuItem("Zoom/Details", new FEventHandler() {
                                @Override
                                public void handleEvent(FEvent e) {
                                    CardZoom.show(stackInstance.getSourceCard());
                                }
                            }));
                        };
                    };

                    menu.show(this, x, y);
                    return true;
                }
            }
            CardZoom.show(stackInstance.getSourceCard());
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

            boolean needAlpha = (activeStackInstance != stackInstance);
            if (needAlpha) { //use alpha for non-active items on stack
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
