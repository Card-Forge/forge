package forge.screens.match.views;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.card.CardRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.card.CardZoom;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.StackItemView;
import forge.game.zone.ZoneType;
import forge.gui.card.CardDetailUtil;
import forge.gui.card.CardDetailUtil.DetailColors;
import forge.gui.interfaces.IGuiGame;
import forge.interfaces.IGameController;
import forge.menu.FCheckBoxMenuItem;
import forge.menu.FDropDown;
import forge.menu.FMenuItem;
import forge.menu.FMenuTab;
import forge.menu.FPopupMenu;
import forge.player.PlayerZoneUpdates;
import forge.screens.match.MatchController;
import forge.screens.match.TargetingOverlay;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.util.Localizer;
import forge.util.TextUtil;
import forge.util.Utils;
import forge.util.collect.FCollectionView;

public class VStack extends FDropDown {
    public static final float CARD_WIDTH = Utils.AVG_FINGER_WIDTH;
    public static final float CARD_HEIGHT = Math.round(CARD_WIDTH * FCardPanel.ASPECT_RATIO);
    public static final float BORDER_THICKNESS = Utils.scale(2);
    public static final float PADDING = Utils.scale(3);
    public static final float MARGINS = Utils.scale(4);
    private static final FSkinFont FONT = FSkinFont.get(11);
    private static final float ALPHA_COMPOSITE = 0.5f;
    private static final TextRenderer textRenderer = new TextRenderer(true);

    private StackInstanceDisplay activeItem;
    private StackItemView activeStackInstance;
    private Map<PlayerView, Object> playersWithValidTargets;
    private PlayerZoneUpdates restorablePlayerZones = null;

    private int stackSize;

    public VStack() {
    }

    @Override
    protected boolean autoHide() {
        return false;
    }

    //temporarily reveal zones targeted by active stack instance
    private void revealTargetZones() {
        if (activeStackInstance == null) { return; }

        PlayerView player = MatchController.instance.getCurrentPlayer();

        final Set<ZoneType> zones = new HashSet<>();
        playersWithValidTargets = new HashMap<>();
        for (final CardView c : activeStackInstance.getTargetCards()) {
            if (c.getZone() != null) {
                zones.add(c.getZone());
                playersWithValidTargets.put(c.getController(), c);
            }
        }
        if (zones.isEmpty() || playersWithValidTargets.isEmpty()) { return; }
        restorablePlayerZones = MatchController.instance.openZones(player, zones, playersWithValidTargets);
    }

    //restore old zones when active stack instance changes
    private void restoreOldZones() {
        if (restorablePlayerZones == null) { return; }
        PlayerView player = MatchController.instance.getCurrentPlayer();
        MatchController.instance.restoreOldZones(player, restorablePlayerZones);
        restorablePlayerZones = null;
    }

    public void checkEmptyStack() { //sort the bug in client when desynch happens
        final FCollectionView<StackItemView> stack = MatchController.instance.getGameView().getStack();
        if(stack!=null)
            if(isVisible() && stack.isEmpty()) { //visible stack but empty already
                hide();
                getMenuTab().setText(Localizer.getInstance().getMessage("lblStack") + " (" + 0 + ")");
            }
    }

    @Override
    public void update() {
        activeItem = null;
        activeStackInstance = null; //reset before updating stack
        restoreOldZones();

        final FCollectionView<StackItemView> stack = MatchController.instance.getGameView().getStack();
        if (stackSize != stack.size()) {
            int oldStackSize = stackSize;
            stackSize = stack.size();
            getMenuTab().setText(Localizer.getInstance().getMessage("lblStack") + " (" + stackSize + ")");

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
        float totalWidth;
        if (Forge.isLandscapeMode()) {
            totalWidth = Forge.getScreenWidth() * 0.35f;
        }
        else {
            totalWidth = maxWidth - MatchController.getView().getTopPlayerPanel().getTabs().iterator().next().getRight(); //keep avatar, life total, and hand tab visible to left of stack
        }
        float width = totalWidth - 2 * MARGINS;

        final FCollectionView<StackItemView> stack = MatchController.instance.getGameView().getStack();
        if (stack.isEmpty()) { //show label if stack empty
            FLabel label = add(new FLabel.Builder().text("[" + Localizer.getInstance().getMessage("lblEmpty") + "]").font(FONT).align(Align.center).build());

            float height = Math.round(label.getAutoSizeBounds().height) + 2 * PADDING;
            label.setBounds(x, y, width, height);
            return new ScrollBounds(totalWidth, y + height + MARGINS);
        }
        else {
            //iterate stack in reverse so most recent items appear on bottom
            StackItemView stackInstance = null;
            StackInstanceDisplay display = null;
            float overlap = Math.round(CARD_HEIGHT / 2 + PADDING + BORDER_THICKNESS);
            for (int i = stack.size() - 1; i >= 0; i--) {
                stackInstance = stack.get(i);
                display = new StackInstanceDisplay(stackInstance, width);
                if (activeStackInstance == stackInstance) {
                    activeItem = display;
                }
                else { //only add non-active items here
                    add(display);
                }
                //use full preferred height of display for topmost item on stack, overlap amount for other items
                display.setBounds(x, y, width, i > 0 ? overlap : display.preferredHeight);
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

    protected void updateSizeAndPosition() {
        if (!getRotate90()) {
            super.updateSizeAndPosition();
            return;
        }
        
        FMenuTab menuTab = getMenuTab();
        float screenHeight = Forge.getScreenHeight();
        float width = (screenHeight - 2 * VPrompt.HEIGHT - 2 * VAvatar.HEIGHT) * 4f / 6f;
        float maxVisibleHeight = menuTab.screenPos.x;
        paneSize = updateAndGetPaneSize(width + MatchController.getView().getTopPlayerPanel().getTabs().iterator().next().getRight(), maxVisibleHeight);

        //round width and height so borders appear properly
        paneSize = new ScrollBounds(Math.round(paneSize.getWidth()), Math.round(paneSize.getHeight()));

        setBounds(Math.round(menuTab.screenPos.x - width), Math.round((screenHeight + width) / 2), paneSize.getWidth(), Math.min(paneSize.getHeight(), maxVisibleHeight));
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

            PlayerView activator = activeStackInstance.getActivatingPlayer();
            arrowOrigin = arrowOrigin.add(screenPos.x, screenPos.y);

            StackItemView instance = activeStackInstance;
            while (instance != null) {
                for (CardView c : instance.getTargetCards()) {
                    TargetingOverlay.ArcConnection conn = activator.isOpponentOf(c.getController()) ? TargetingOverlay.ArcConnection.FoesStackTargeting : TargetingOverlay.ArcConnection.FriendsStackTargeting;
                    TargetingOverlay.drawArrow(g, arrowOrigin, c, conn);
                }
                for (PlayerView p : instance.getTargetPlayers()) {
                    TargetingOverlay.ArcConnection conn = activator.isOpponentOf(p) ? TargetingOverlay.ArcConnection.FoesStackTargeting : TargetingOverlay.ArcConnection.FriendsStackTargeting;
                    TargetingOverlay.drawArrow(g, arrowOrigin, p, conn);
                }
                instance = instance.getSubInstance();
            }
        }
    }

    private class StackInstanceDisplay extends FDisplayObject {
        private final StackItemView stackInstance;
        private final Color foreColor, backColor;
        private String text;
        private float preferredHeight;

        private StackInstanceDisplay(StackItemView stackInstance0, float width) {
            stackInstance = stackInstance0;
            CardView card = stackInstance.getSourceCard();

            text = stackInstance.getText();
            if (stackInstance.isOptionalTrigger() &&
                    stackInstance0.getActivatingPlayer().equals(MatchController.instance.getCurrentPlayer())) {
                text = "(OPTIONAL) " + text;
            }

            // TODO: A hacky workaround is currently used to make the game not leak the color information for Morph cards.
            final boolean isFaceDown = card.isFaceDown();
            final DetailColors color = isFaceDown ? CardDetailUtil.DetailColors.FACE_DOWN : CardDetailUtil.getBorderColor(card.getCurrentState(), true); // otherwise doesn't work correctly for face down Morphs
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
            final GameView gameView = MatchController.instance.getGameView();
            final IGuiGame gui = MatchController.instance;
            final IGameController controller = MatchController.instance.getGameController();
            final PlayerView player = MatchController.instance.getCurrentPlayer();
            if (player != null) { //don't show menu if tapping on art
                if (stackInstance.isAbility()) {
                    FPopupMenu menu = new FPopupMenu() {
                        @Override
                        protected void buildMenu() {
                            final String key = stackInstance.getKey();
                            final boolean autoYield = gui.shouldAutoYield(key);
                            addItem(new FCheckBoxMenuItem(Localizer.getInstance().getMessage("cbpAutoYieldMode"), autoYield,
                                    new FEventHandler() {
                                @Override
                                public void handleEvent(FEvent e) {
                                    gui.setShouldAutoYield(key, !autoYield);
                                    if (!autoYield && stackInstance.equals(gameView.peekStack())) {
                                        //auto-pass priority if ability is on top of stack
                                        controller.passPriority();
                                    }
                                }
                            }));
                            if (stackInstance.isOptionalTrigger() && stackInstance.getActivatingPlayer().equals(player)) {
                                final int triggerID = stackInstance.getSourceTrigger();
                                addItem(new FCheckBoxMenuItem(Localizer.getInstance().getMessage("lblAlwaysYes"),
                                        gui.shouldAlwaysAcceptTrigger(triggerID),
                                        new FEventHandler() {
                                    @Override
                                    public void handleEvent(FEvent e) {
                                        if (gui.shouldAlwaysAcceptTrigger(triggerID)) {
                                            gui.setShouldAlwaysAskTrigger(triggerID);
                                        }
                                        else {
                                            gui.setShouldAlwaysAcceptTrigger(triggerID);
                                            if (stackInstance.equals(gameView.peekStack())) {
                                                //auto-yes if ability is on top of stack
                                                controller.selectButtonOk();
                                            }
                                        }
                                    }
                                }));
                                addItem(new FCheckBoxMenuItem(Localizer.getInstance().getMessage("lblAlwaysNo"),
                                        gui.shouldAlwaysDeclineTrigger(triggerID),
                                        new FEventHandler() {
                                    @Override
                                    public void handleEvent(FEvent e) {
                                        if (gui.shouldAlwaysDeclineTrigger(triggerID)) {
                                            gui.setShouldAlwaysAskTrigger(triggerID);
                                        }
                                        else {
                                            gui.setShouldAlwaysDeclineTrigger(triggerID);
                                            if (stackInstance.equals(gameView.peekStack())) {
                                                //auto-no if ability is on top of stack
                                                controller.selectButtonCancel();
                                            }
                                        }
                                    }
                                }));
                            }
                            addItem(new FMenuItem(Localizer.getInstance().getMessage("lblZoomOrDetails"), new FEventHandler() {
                                @Override
                                public void handleEvent(FEvent e) {
                                    CardZoom.show(stackInstance.getSourceCard());
                                }
                            }));
                        }
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
            CardView sourceCard = stackInstance.getSourceCard();

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
            CardRenderer.drawCardWithOverlays(g, sourceCard, x, y, CARD_WIDTH, CARD_HEIGHT, CardStackPosition.Top, true, false, false);

            x += CARD_WIDTH + PADDING;
            w -= x + PADDING - BORDER_THICKNESS;
            h -= y + PADDING - BORDER_THICKNESS;

            String name = sourceCard.getName();
            int index = text.indexOf(name);
            String newtext = "";
            String cId =  "(" + sourceCard.getId() + ")";
            String optionalCostString = !stackInstance.getOptionalCostString().equals("") ? " ("+ stackInstance.getOptionalCostString() + ")" : "";

            if (index == -1) {
                newtext = TextUtil.fastReplace(text.trim(), "  ", " ");
                newtext = TextUtil.fastReplace(TextUtil.fastReplace(newtext,"--","-"),"- -","-");
                newtext = TextUtil.fastReplace(TextUtil.fastReplace(newtext, " and .", ".")," .", ".");
                newtext = TextUtil.fastReplace(TextUtil.fastReplace(newtext, "- - ", "- "), ". .", ".");
                newtext = TextUtil.fastReplace(newtext, "CARDNAME", name);
                textRenderer.drawText(g, name + " " + (name.length() > 1 ? cId : "") + optionalCostString + "\n" + (newtext.length() > 1 ? newtext : ""),
                        FONT, foreColor, x, y, w, h, y, h, true, Align.left, true);

            } else {
                String modifier = (text.substring(0, index).length() > 0) ? "CARDNAME" : "";
                String trimFirst = TextUtil.fastReplace("\n" + text.substring(0, index) + modifier + text.substring(index + name.length()), "- -", "-");
                String trimSecond = TextUtil.fastReplace(trimFirst, name+" "+cId, name);
                newtext = TextUtil.fastReplace(trimSecond, " "+cId, name);

                if(newtext.equals("\n"+name))
                    textRenderer.drawText(g, name + " " + cId + optionalCostString, FONT, foreColor, x, y, w, h, y, h, true, Align.left, true);
                else {
                    newtext = TextUtil.fastReplace(newtext, "  ", " ");
                    newtext = TextUtil.fastReplace(TextUtil.fastReplace(newtext,name+" -","-"), "\n ", "\n");
                    newtext = "\n"+ TextUtil.fastReplace(newtext.trim(),"--","-");
                    newtext = TextUtil.fastReplace(TextUtil.fastReplace(newtext, " and .", ".")," .", ".");
                    newtext = TextUtil.fastReplace(TextUtil.fastReplace(newtext, "- - ", "- "), ". .", ".");
                    newtext = TextUtil.fastReplace(newtext, "CARDNAME", name);
                    newtext = TextUtil.fastReplace(newtext, name+name, name);
                    textRenderer.drawText(g, name+" "+cId + optionalCostString +newtext, FONT, foreColor, x, y, w, h, y, h, true, Align.left, true);
                }
            }

            g.endClip();

            if (needAlpha) {
                g.resetAlphaComposite();
            }
        }
    }
}
