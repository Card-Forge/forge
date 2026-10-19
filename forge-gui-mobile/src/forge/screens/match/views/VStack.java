package forge.screens.match.views;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.TextRenderer;
import forge.card.CardRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.card.CardZoom;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.StackItemView;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.YieldUpdate;
import forge.gui.card.CardDetailUtil;
import forge.gui.card.CardDetailUtil.DetailColors;
import forge.interfaces.IGameController;
import forge.menu.FCheckBoxMenuItem;
import forge.menu.FDropDown;
import forge.menu.FMenuItem;
import forge.menu.FMenuTab;
import forge.menu.FPopupMenu;
import forge.player.AutoYieldStore.TriggerDecision;
import forge.screens.match.CardFlightOverlay;
import forge.screens.match.MatchController;
import forge.screens.match.MatchScreen;
import forge.screens.match.TargetingOverlay;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FLabel;
import forge.toolbox.FSelectionBox;
import forge.util.TextUtil;
import forge.util.ThreadUtil;
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
    private static final Color GLASS_EDGE = new Color(1f, 1f, 1f, 0.18f);
    private static final Color GLASS_PANE = new Color(35 / 255f, 40 / 255f, 44 / 255f, 0.6f);
    private static final TextRenderer textRenderer = new TextRenderer(true);

    private StackInstanceDisplay activeItem;
    private StackItemView activeStackInstance;
    private Map<PlayerView, Object> playersWithValidTargets;

    private int stackSize;
    private final Map<Integer, StackInstanceDisplay> displayById = new HashMap<>(); // rebuilt on every layout

    public VStack() {
    }

    //compact layout: the stack slides in from, and back out to, the right edge of the screen
    private static final float COMPACT_SLIDE_SECONDS = 0.18f;
    private float compactSlide = 1f, compactSlideTarget = 1f, compactRestX;
    private CompactSlideAnimation compactSlideAnimation;

    private static final boolean COMPACT_SLIDE_ANIMATED = false; //the slide made some people dizzy, so the stack just appears

    private static boolean slides() {
        return COMPACT_SLIDE_ANIMATED && VPlayerPanel.isCompactLayout();
    }

    @Override
    public void show() {
        if (!slides()) {
            compactSlide = compactSlideTarget = 1f;
            super.show();
            return;
        }
        compactSlideTarget = 1f;
        if (!isVisible()) {
            compactSlide = 0f; //starts off screen
            super.show();
        } else {
            update(); //was sliding away: bring it up to date
        }
        startCompactSlide();
    }

    @Override
    public void hide() {
        if (!slides() || !isVisible()) {
            compactSlide = compactSlideTarget = 1f;
            super.hide();
            return;
        }
        compactSlideTarget = 0f;
        startCompactSlide();
    }

    private void startCompactSlide() {
        if (compactSlideAnimation == null) {
            compactSlideAnimation = new CompactSlideAnimation();
            compactSlideAnimation.start();
        }
    }

    private void applyCompactSlide() {
        setBounds(Math.round(compactRestX + (1f - compactSlide) * getWidth()), getTop(), getWidth(), getHeight());
    }

    private class CompactSlideAnimation extends ForgeAnimation {
        @Override
        protected boolean advance(float dt) {
            final float step = dt / COMPACT_SLIDE_SECONDS;
            if (compactSlide < compactSlideTarget) {
                compactSlide = Math.min(compactSlideTarget, compactSlide + step);
            } else {
                compactSlide = Math.max(compactSlideTarget, compactSlide - step);
            }
            applyCompactSlide();
            return compactSlide != compactSlideTarget;
        }

        @Override
        protected void onEnd(boolean endingAll) {
            compactSlideAnimation = null;
            if (compactSlideTarget <= 0f && isVisible()) {
                VStack.super.hide(); //fully out of sight
            }
        }
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
        FCollectionView<CardView> targetCards = activeStackInstance.getTargetCards();
        if (targetCards != null) {
            for (final CardView c : targetCards) {
                if (c.getZone() != null) {
                    zones.add(c.getZone());
                    playersWithValidTargets.put(c.getController(), c);
                }
            }
        }
        if (zones.isEmpty() || playersWithValidTargets.isEmpty()) { return; }
        MatchController.instance.openZones(player, zones, playersWithValidTargets);
    }

    //restore old zones when active stack instance changes
    private void restoreOldZones() {
        if (playersWithValidTargets == null) { return; }
        MatchController.instance.restoreOldZones(playersWithValidTargets);
        playersWithValidTargets = null;
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
            getMenuTab().setText(Forge.getLocalizer().getMessage("lblStack") + " (" + stackSize + ")");

            if (stackSize > 0) {
                if (!isVisible() || compactSlideTarget <= 0f) { //compact layout: also when it is sliding away
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
        displayById.clear();

        final float margins = VPlayerPanel.isCompactLayout() ? 0 : MARGINS; //compact: the cards fill the pane, no gaps above or beside them
        float x = margins;
        float y = margins;
        float totalWidth;
        if (Forge.isLandscapeMode()) {
            totalWidth = Forge.getScreenWidth() * 0.35f;
        }
        else {
            totalWidth = maxWidth - MatchController.getView().getTopPlayerPanel().getTabs().iterator().next().getRight(); //keep avatar, life total, and hand tab visible to left of stack
        }
        float width = totalWidth - 2 * margins;

        final FCollectionView<StackItemView> stack = MatchController.instance.getGameView().getStack();
        if (stack.isEmpty()) { //show label if stack empty
            FLabel label = add(new FLabel.Builder().text("[" + Forge.getLocalizer().getMessage("lblEmpty") + "]").font(FONT).align(Align.center).build());

            float height = Math.round(label.getAutoSizeBounds().height) + 2 * PADDING;
            label.setBounds(x, y, width, height);
            return new ScrollBounds(totalWidth, y + height + margins);
        }
        else {
            //iterate stack in reverse so most recent items appear on bottom
            StackItemView stackInstance = null;
            StackInstanceDisplay display = null;
            float overlap = Math.round(CARD_HEIGHT / 2 + PADDING + BORDER_THICKNESS);
            for (int i = stack.size() - 1; i >= 0; i--) {
                stackInstance = stack.get(i);
                display = new StackInstanceDisplay(stackInstance, width);
                displayById.put(stackInstance.getId(), display);
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
                if (display != null)
                    activeItem.setHeight(display.preferredHeight); //increase active item height to preferred height if needed
                if (activeItem.getBottom() > y) {
                    y = activeItem.getBottom(); //ensure stack height increases if needed
                }
                add(activeItem);
            }
            scrollIntoView(activeItem); //scroll active display into view
            revealTargetZones();
        }
        return new ScrollBounds(totalWidth, y + margins);
    }

    protected void updateSizeAndPosition() {
        if (!getRotate90()) {
            super.updateSizeAndPosition();
            if (slides()) { //shift what the drop down placed by how far it has slid in
                compactRestX = getLeft();
                applyCompactSlide();
            }
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

    //frosted cards sit on translucent glass over any battlefield, so the card name gets a thin dark outline
    private static void drawStackText(Graphics g, String text, int nameLength, FSkinFont font, Color color, float x, float y, float w, float h, float visibleY, float visibleH, boolean wrap, int align, boolean center) {
        if (VPlayerPanel.isFrosted()) {
            textRenderer.drawTextShadowed(g, text, nameLength, font, color, x, y, w, h, visibleY, visibleH, wrap, align, center);
        } else {
            textRenderer.drawText(g, text, font, color, x, y, w, h, visibleY, visibleH, wrap, align, center);
        }
    }

    private static final Color GLASS_BASE = new Color(30 / 255f, 35 / 255f, 40 / 255f, 0.92f); //dark backing: battlefield cards never show through an item
    private static final float GLASS_ALPHA_COMPOSITE = 0.8f;

    private static Color glassTint(Color c, float k) {
        return new Color(c.r * k, c.g * k, c.b * k, 0.92f); //a soft wash of the card colour over the base
    }

    @Override
    protected void drawBackground(Graphics g) {
        if (!VPlayerPanel.isCompactLayout()) { super.drawBackground(g); return; }
        if (activeItem == null) { //empty stack: a frosted pane holds the label, otherwise the cards float over the match
            g.fillRect(GLASS_PANE, 0, 0, getWidth(), getHeight());
            g.drawRect(1, FDropDown.getBorderColor().getColor(), 0, 0, getWidth(), getHeight());
        }
    }

    @Override
    protected void drawOverlay(Graphics g) {
        if (!VPlayerPanel.isCompactLayout()) { super.drawOverlay(g); }
    }

    @Override
    protected void drawOnContainer(Graphics g) {
        //draw target arrows immediately above stack for active item only
        if (activeItem != null) {
            Vector2 arrowOrigin = new Vector2(
                    activeItem.getLeft() + VStack.CARD_WIDTH * FCardPanel.TARGET_ORIGIN_FACTOR_X + VStack.PADDING + VStack.BORDER_THICKNESS,
                    activeItem.getTop() + VStack.CARD_HEIGHT * FCardPanel.TARGET_ORIGIN_FACTOR_Y + VStack.PADDING + VStack.BORDER_THICKNESS);

            PlayerView activator = activeStackInstance == null ? null : activeStackInstance.getActivatingPlayer();
            arrowOrigin = arrowOrigin.add(screenPos.x, screenPos.y);

            // one arrow per targeted stack entry, however many modes aim at it; created on first use, as this runs every frame
            Set<Integer> drawnStackTargets = null;
            StackItemView instance = activeStackInstance;
            while (instance != null) {
                for (CardView c : instance.getTargetCards()) {
                    TargetingOverlay.ArcConnection conn = activator != null && activator.isOpponentOf(c.getController()) ? TargetingOverlay.ArcConnection.FoesStackTargeting : TargetingOverlay.ArcConnection.FriendsStackTargeting;
                    TargetingOverlay.drawArrow(g, arrowOrigin, VCardDisplayArea.CardAreaPanel.get(c).getTargetingArrowOrigin(), conn);
                }
                for (PlayerView p : instance.getTargetPlayers()) {
                    TargetingOverlay.ArcConnection conn = activator != null && activator.isOpponentOf(p) ? TargetingOverlay.ArcConnection.FoesStackTargeting : TargetingOverlay.ArcConnection.FriendsStackTargeting;
                    TargetingOverlay.drawArrow(g, arrowOrigin, MatchScreen.getPlayerPanel(p).getAvatar().getTargetingArrowOrigin(), conn);
                }

                // Stack items aimed at other stack items (counterspells and the like) point at that item's entry in this view.
                // Walked per sub-instance like the targets above: a modal spell (Sublime Epiphany, Charm effects) keeps each
                // chosen mode's targets on its own sub-instance, the head instance itself has none.
                final FCollectionView<StackItemView> targetedItems = instance.getTargetStackItems();
                if (targetedItems != null && !targetedItems.isEmpty()) {
                    if (drawnStackTargets == null) { drawnStackTargets = new HashSet<>(); }
                    final float edge = screenPos.x + BORDER_THICKNESS;
                    final Vector2 from = new Vector2(edge + activeItem.getLeft(),
                            arrowOrigin.y); // same height as the arrows to cards and players
                    for (StackItemView targeted : targetedItems) {
                        final StackInstanceDisplay td = displayById.get(targeted.getId());
                        if (td == null || td == activeItem || !drawnStackTargets.add(targeted.getId())) { continue; }
                        // aim at the visible top part of the entry, the lower part is covered by the next item
                        final Vector2 to = new Vector2(edge + td.getLeft(),
                                screenPos.y + td.getTop() + BORDER_THICKNESS + PADDING + CARD_HEIGHT * 0.25f);
                        final PlayerView owner = targeted.getActivatingPlayer();
                        final TargetingOverlay.ArcConnection conn = activator != null && owner != null && activator.isOpponentOf(owner)
                                ? TargetingOverlay.ArcConnection.FoesStackTargeting : TargetingOverlay.ArcConnection.FriendsStackTargeting;
                        TargetingOverlay.drawStackArrow(g, from, to, conn);
                    }
                }
                instance = instance.getSubInstance();
            }
        }
    }

    public class StackInstanceDisplay extends FDisplayObject {
        public final StackItemView stackInstance;
        private final Color foreColor, backColor, backColor2; //backColor2: the far end of the gradient on glass cards
        private String text;
        private float preferredHeight;
        private final Rectangle cardBounds = new Rectangle(0, 0, 0, 0);
        private boolean launched; // set once this item's card has flown off; stays hidden until the stack is rebuilt

        private StackInstanceDisplay(StackItemView stackInstance0, float width) {
            stackInstance = stackInstance0;
            CardView card = stackInstance.getSourceCard();

            text = stackInstance.getText()
                .replace("\\r", "\r")
                .replace("\\n", "\n");
            if (stackInstance.isOptionalTrigger() &&
                    stackInstance0.getActivatingPlayer().equals(MatchController.instance.getCurrentPlayer())) {
                text = "(OPTIONAL) " + text;
            }

            // TODO: A hacky workaround is currently used to make the game not leak the color information for Morph cards.
            final boolean isFaceDown = card.isFaceDown();
            final DetailColors color = isFaceDown ? CardDetailUtil.DetailColors.FACE_DOWN : CardDetailUtil.getBorderColor(card.getCurrentState(), true); // otherwise doesn't work correctly for face down Morphs
            backColor = FSkinColor.fromRGB(color.r, color.g, color.b);
            Color end = backColor; //multicolour cards blend from their first colour to their last
            if (!isFaceDown) {
                final List<DetailColors> all = CardDetailUtil.getBorderColors(card.getCurrentState(), true);
                final DetailColors last = all.get(all.size() - 1);
                end = FSkinColor.fromRGB(last.r, last.g, last.b);
            }
            backColor2 = end;
            foreColor = VPlayerPanel.isFrosted() ? Color.WHITE : FSkinColor.getHighContrastColor(backColor); //glass cards are dark

            width -= CARD_WIDTH; //account for card picture
            width -= 3 * PADDING + 2 * BORDER_THICKNESS; //account for left and right insets and gap between picture and text
            float height = Math.max(CARD_HEIGHT, textRenderer.getWrappedBounds(text, FONT, width).height);
            height += 2 * (PADDING + BORDER_THICKNESS);
            preferredHeight = Math.round(height);
        }

        // an item the current prompt allows picking, e.g. the target of a counterspell or Stifle
        private boolean isSelectableTarget() {
            return MatchController.instance.isSelectable(stackInstance);
        }

        private void showCardOrMenu(CardView sourceCard, FPopupMenu menu, float x, float y) {
            if (sourceCard != null && cardBounds.contains(x, y))
                CardZoom.show(sourceCard);
            else
                menu.show(this, x, y);
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (isSelectableTarget()) { // a stack target is being chosen: tapping the entry picks (or unpicks) it
                activeStackInstance = stackInstance;
                //must invoke in game thread in case a dialog needs to be shown
                ThreadUtil.invokeInGameThread(() -> {
                    MatchController.instance.pickStackItem(stackInstance);
                    Gdx.graphics.requestRendering();
                });
                return true;
            }
            if (activeStackInstance != stackInstance) { //set as active stack instance if not already such
                activeStackInstance = stackInstance;
                restoreOldZones(); //restore old zones before changing active stack instance
                VStack.this.updateSizeAndPosition();
                return true;
            }
            final IGameController controller = MatchController.instance.getGameController();
            final PlayerView player = MatchController.instance.getCurrentPlayer();
            if (player != null) { //don't show menu if tapping on art
                FPopupMenu menu = new FPopupMenu() {
                    @Override
                    protected void buildMenu() {
                        if (stackInstance.isAbility()) {
                            final String key = stackInstance.getKey();
                            final boolean autoYield = controller.shouldAutoYield(key);
                            addItem(new FCheckBoxMenuItem(Forge.getLocalizer().getMessage("cbpAutoYieldMode"), autoYield,
                                    e -> {
                                        boolean abilityScope = controller.getYieldController().isAbilityScope();
                                        controller.setShouldAutoYield(key, !autoYield, abilityScope);
                                    }));
                            if (stackInstance.isOptionalTrigger() && stackInstance.getActivatingPlayer().equals(player)) {
                                if (!key.isEmpty()) {
                                    final boolean abilityScope = controller.getYieldController().isAbilityScope();
                                    addItem(new FCheckBoxMenuItem(Forge.getLocalizer().getMessage("lblAlwaysYes"),
                                            controller.getTriggerDecision(key) == TriggerDecision.ACCEPT,
                                            e -> controller.setTriggerDecision(key,
                                                    controller.getTriggerDecision(key) == TriggerDecision.ACCEPT ? TriggerDecision.ASK : TriggerDecision.ACCEPT,
                                                    abilityScope)));
                                    addItem(new FCheckBoxMenuItem(Forge.getLocalizer().getMessage("lblAlwaysNo"),
                                            controller.getTriggerDecision(key) == TriggerDecision.DECLINE,
                                            e -> controller.setTriggerDecision(key,
                                                    controller.getTriggerDecision(key) == TriggerDecision.DECLINE ? TriggerDecision.ASK : TriggerDecision.DECLINE,
                                                    abilityScope)));
                                }
                            }
                        }
                        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblYieldToStack"),
                                Forge.hdbuttons ? FSkinImage.HDYIELD : FSkinImage.WARNING,
                                e -> controller.sendYieldUpdate(new YieldUpdate.StackYield(player, true, true))));
                        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblYieldToEntireStack"),
                                Forge.hdbuttons ? FSkinImage.HDYIELD : FSkinImage.WARNING,
                                e -> controller.sendYieldUpdate(new YieldUpdate.StackYield(player, true, false))));
                        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblZoomOrDetails"), e -> CardZoom.show(stackInstance.getSourceCard())));
                    }
                };
                // tapping on small cardView should zoom the card otherwise show menu
                showCardOrMenu(stackInstance.getSourceCard(), menu, x, y);
                return true;
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
            float xx = 0;
            float yy = 0;
            CardView sourceCard = stackInstance.getSourceCard();

            // sticky: once this item's card has flown off, keep the art hidden
            if (!launched && !stackInstance.isAbility() && CardFlightOverlay.isLeavingStack(sourceCard.getId())) {
                launched = true;
            }

            // entries keep their usual look (the active one bright, the rest faded); only a chosen target is shown bright
            final boolean pickable = isSelectableTarget();
            final boolean picked = MatchController.instance.isHighlighted(stackInstance);
            boolean needAlpha = (activeStackInstance != stackInstance) && !picked;
            if (needAlpha) {
                g.setAlphaComposite(VPlayerPanel.isFrosted() ? GLASS_ALPHA_COMPOSITE : ALPHA_COMPOSITE);
            }

            g.startClip(0, 0, w, getHeight()); //clip based on actual height

            final boolean glass = VPlayerPanel.isFrosted();
            final Color borderColor = picked ? FSelectionBox.CHOSEN_COLOR : isHovered() ? Color.GRAY : glass ? GLASS_EDGE : Color.BLACK;
            if (glass) { //frosted glass card: lively translucent colour gradient, outlined only when hovered or picked
                final Color c1 = glassTint(backColor, 0.80f);
                final Color c2 = backColor2.equals(backColor) ? glassTint(backColor, 0.50f) : glassTint(backColor2, 0.80f);
                g.fillRect(GLASS_BASE, x, y, w, h);
                g.fillGradientRect(c1, c2, false, x, y, w, h);
                if (picked || isHovered()) {
                    g.drawRect(Math.max(1f, Utils.scale(1.5f)), picked ? FSelectionBox.CHOSEN_COLOR : FDropDown.getBorderColor().getColor(), x, y, w, h);
                }
            } else {
                g.fillRect(borderColor, x, y, w, h); //draw rectangle for border
            }
            if (pickable && !picked) { // same moving dashes as every other valid target, over the border ring
                FSelectionBox.draw(g, BORDER_THICKNESS, x, y, w, h);
            }

            x += BORDER_THICKNESS;
            y += BORDER_THICKNESS;
            w -= 2 * BORDER_THICKNESS;
            h -= 2 * BORDER_THICKNESS;
            if (!glass) {
                g.fillRect(backColor, x, y, w, h);
            }
            if (picked) { // chosen: a second solid frame inside the border so it is easy to see
                FSelectionBox.drawChosen(g, BORDER_THICKNESS, x, y, w, h);
            }

            x += PADDING;
            y += PADDING;
            cardBounds.set(x, y, CARD_WIDTH, CARD_HEIGHT);
            xx = x;
            yy = y;
            if (!launched && !stackInstance.isAbility()) { // record the rect only while the card is really here
                CardFlightOverlay.noteStackRect(sourceCard.getId(),
                    VStack.this.screenPos.x + getLeft() + cardBounds.x,
                    VStack.this.screenPos.y + getTop() + cardBounds.y,
                    cardBounds.width, cardBounds.height);
            }
            x += CARD_WIDTH + PADDING;
            w -= x + PADDING - BORDER_THICKNESS;
            h -= y + PADDING - BORDER_THICKNESS;

            String name = sourceCard.getName();
            int index = text.indexOf(name);
            String newtext = "";
            String cId =  "(" + sourceCard.getId() + ")";
            String optionalCostString = !stackInstance.getOptionalCostString().isEmpty() ? " ("+ stackInstance.getOptionalCostString() + ")" : "";

            if (index == -1) {
                newtext = TextUtil.fastReplace(text.trim(), "  ", " ");
                newtext = TextUtil.fastReplace(TextUtil.fastReplace(newtext,"--","-"),"- -","-");
                newtext = TextUtil.fastReplace(TextUtil.fastReplace(newtext, " and .", ".")," .", ".");
                newtext = TextUtil.fastReplace(TextUtil.fastReplace(newtext, "- - ", "- "), ". .", ".");
                newtext = TextUtil.fastReplace(newtext, "CARDNAME", name);
                drawStackText(g, name + " " + (name.length() > 1 ? cId : "") + optionalCostString + "\n" + (newtext.length() > 1 ? newtext : ""), name.length(),
                    FONT, foreColor, x, y, w, h, y, h, true, Align.left, true);

            } else {
                String modifier = (text.substring(0, index).length() > 0) ? "CARDNAME" : "";
                String trimFirst = TextUtil.fastReplace("\n" + text.substring(0, index) + modifier + text.substring(index + name.length()), "- -", "-");
                String trimSecond = TextUtil.fastReplace(trimFirst, name+" "+cId, name);
                newtext = TextUtil.fastReplace(trimSecond, " "+cId, name);

                if(newtext.equals("\n"+name))
                    drawStackText(g, name + " " + cId + optionalCostString, name.length(), FONT, foreColor, x, y, w, h, y, h, true, Align.left, true);
                else {
                    newtext = TextUtil.fastReplace(newtext, "  ", " ");
                    newtext = TextUtil.fastReplace(TextUtil.fastReplace(newtext,name+" -","-"), "\n ", "\n");
                    newtext = "\n"+ TextUtil.fastReplace(newtext.trim(),"--","-");
                    newtext = TextUtil.fastReplace(TextUtil.fastReplace(newtext, " and .", ".")," .", ".");
                    newtext = TextUtil.fastReplace(TextUtil.fastReplace(newtext, "- - ", "- "), ". .", ".");
                    newtext = TextUtil.fastReplace(newtext, "CARDNAME", name);
                    newtext = TextUtil.fastReplace(newtext, name+name, name);
                    drawStackText(g, name+" "+cId + optionalCostString +newtext, name.length(), FONT, foreColor, x, y, w, h, y, h, true, Align.left, true);
                }
            }
            if (!launched) { // replaces the early return
                CardRenderer.drawCardWithOverlays(g, sourceCard, xx, yy, CARD_WIDTH, CARD_HEIGHT, CardStackPosition.Top, true, false, false);
            } else {
                g.drawRect(BORDER_THICKNESS / 2, borderColor, xx, yy, CARD_WIDTH, CARD_HEIGHT);
            }
            g.endClip();

            if (needAlpha) {
                g.resetAlphaComposite();
            }
        }
    }
}
