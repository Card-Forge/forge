package forge.screens.match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.math.Vector2;

import forge.Forge.Graphics;
import forge.assets.CardFaceSymbols;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.ImageCache;
import forge.assets.FSkinColor.Colors;
import forge.card.CardDetailUtil;
import forge.card.CardDetailUtil.CardBorderColor;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.match.input.Input;
import forge.match.input.InputPassPriority;
import forge.match.input.InputPayMana;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.screens.FScreen;
import forge.screens.match.views.VCardDisplayArea.CardAreaPanel;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDialog;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FGestureAdapter;
import forge.toolbox.FList;
import forge.util.Callback;
import forge.util.Utils;

public class InputSelectCard {
    private static final float LIST_OPTION_HEIGHT = Utils.AVG_FINGER_HEIGHT;
    private static final long DOUBLE_TAP_INTERVAL = Utils.secondsToTimeSpan(FGestureAdapter.DOUBLE_TAP_INTERVAL); 
    private static CardOptionsList<?> activeList;
    private static CardOptionsList<?>.ListItem pressedItem;
    private static long lastSelectTime;
    private static boolean zoomPressed, detailsPressed, ownerPressed, pannedOverOptions;

    private InputSelectCard() {
    }

    public static void selectCard(CardAreaPanel cardPanel) {
        Input currentInput = FControl.getInputQueue().getInput();
        if (currentInput == null) { return; }

        long now = Gdx.input.getCurrentEventTime();

        if (activeList != null) {
            if (activeList.owner == cardPanel) {
                if (activeList.getCount() > 0 && now - lastSelectTime <= DOUBLE_TAP_INTERVAL) {
                    //auto-select first option if double tapped
                    activeList.getItemAt(0).tap(0, 0, 1);
                }
                return; //don't select already selected card
            }
            CardOptionsList.hide(); //hide previous card options list before showing a new one
        }

        lastSelectTime = now;

        final Card card = cardPanel.getCard();

        if (currentInput instanceof InputPassPriority) {
            CardOptionsList.show(cardPanel,
                    card.getAllPossibleAbilities(FControl.getCurrentPlayer(), true),
                    new Callback<SpellAbility>() {
                @Override
                public void run(SpellAbility result) {
                    FControl.getInputProxy().selectAbility(result);
                }
            });
        }
        else if (currentInput instanceof InputPayMana) {
            CardOptionsList.show(cardPanel,
                    ((InputPayMana)currentInput).getUsefulManaAbilities(card),
                    new Callback<SpellAbility>() {
                @Override
                public void run(SpellAbility result) {
                    FControl.getInputProxy().selectAbility(result);
                }
            });
        }
        else {
            List<String> options = new ArrayList<String>();
            options.add("Select Card");
            CardOptionsList.show(cardPanel, options, new Callback<String>() {
                @Override
                public void run(String result) {
                    FControl.getInputProxy().selectCard(card, null);
                }
            });
        }
    }

    public static boolean handlePan(CardAreaPanel cardPanel, float x, float y, boolean isPanStop) {
        if (pressedItem != null) {
            pressedItem.release(x, y); //prevent pressed item getting stuck
            pressedItem = null;
        }
        zoomPressed = false;
        detailsPressed = false;
        if (isPanStop) {
            pannedOverOptions = false;
        }

        if (activeList == null || activeList.owner != cardPanel) {
            return false;
        }

        if (y < 0) {
            if (activeList.getCount() > 0) {
                int index = Math.round(activeList.getCount() + y / LIST_OPTION_HEIGHT);
                if (index < 0) {
                    index = 0;
                }
                CardOptionsList<?>.ListItem item = activeList.getItemAt(index);
                if (item != null) {
                    if (isPanStop) {
                        item.tap(0, 0, 1);
                    }
                    else {
                        item.press(0, 0);
                        pressedItem = item;
                    }
                    pannedOverOptions = true;
                }
            }
        }
        else if (y > cardPanel.getHeight()) {
            pannedOverOptions = true;
            zoomPressed = cardPanel.getScreenPosition().x + x < FControl.getView().getWidth() / 2;
            detailsPressed = !zoomPressed;
        }

        if (activeList != null) {
            activeList.setVisible(!zoomPressed && !detailsPressed);
        }

        //prevent scrolling card's pane if currently or previously panned over any options
        return pannedOverOptions;
    }

    public enum AttackOption {
        DECLARE_AS_ATTACKER("Declare as Attacker"),
        REMOVE_FROM_COMBAT("Remove from Combat"),
        ATTACK_THIS_DEFENDER("Attack this Defender"),
        ACTIVATE_BAND("Activate Band"),
        JOIN_BAND("Join Band");

        private String text;

        private AttackOption(String text0) {
            text = text0;
        }

        public String toString() {
            return text;
        }
    }

    private static class CardOptionsList<T> extends FList<T> {
        private static float ALPHA_COMPOSITE = 0.5f;
        private static final FSkinColor BACK_COLOR = FSkinColor.get(Colors.CLR_OVERLAY).alphaColor(ALPHA_COMPOSITE);
        private static final FSkinFont NAME_FONT = FSkinFont.get(16);
        private static final FSkinFont TYPE_FONT = FSkinFont.get(14);
        private static final FSkinFont TEXT_FONT = TYPE_FONT;
        private static final FSkinFont ID_FONT = TEXT_FONT;
        private static final FSkinFont PT_FONT = NAME_FONT;
        private static final float MANA_COST_PADDING = 3;
        private static final float MANA_SYMBOL_SIZE = FSkinImage.MANA_1.getNearestHQWidth(2 * (NAME_FONT.getFont().getCapHeight() - MANA_COST_PADDING));

        private static final Backdrop backdrop = new Backdrop();

        private static <T> void show(CardAreaPanel cardPanel, Collection<T> options, final Callback<T> callback) {
            final CardOptionsList<T> optionsList = new CardOptionsList<T>(cardPanel, options);
            optionsList.setListItemRenderer(new ListItemRenderer<T>() {
                @Override
                public boolean tap(T value, float x, float y, int count) {
                    hide();
                    callback.run(value);
                    return true;
                }

                @Override
                public float getItemHeight() {
                    return LIST_OPTION_HEIGHT;
                }

                @Override
                public void drawValue(Graphics g, Object value, FSkinFont font, FSkinColor foreColor, boolean pressed, float width, float height) {
                    float x = width * FList.INSETS_FACTOR;
                    float y = x;
                    if (!pressed) {
                        foreColor = foreColor.alphaColor(ALPHA_COMPOSITE);
                    }
                    g.drawText(value.toString(), font, foreColor, x, y, width - 2 * x, height - 2 * y, true, HAlignment.CENTER, true);
                }
            });

            FScreen screen = FControl.getView();
            float screenWidth = screen.getWidth();
            float screenHeight = screen.getHeight();

            Vector2 pos = cardPanel.getScreenPosition();
            float height = Math.min(options.size() * LIST_OPTION_HEIGHT + 1, pos.y);
            optionsList.setBounds(0, pos.y - height, screenWidth, height);
            optionsList.setVisible(!options.isEmpty());

            backdrop.setBounds(0, 0, screenWidth, screenHeight);
            screen.add(backdrop);
            screen.add(optionsList);
            activeList = optionsList;
        }

        private final CardAreaPanel owner;

        private CardOptionsList(CardAreaPanel owner0, Iterable<T> options) {
            super(options);
            owner = owner0;
        }

        public static void hide() {
            if (activeList == null) { return; }
            FControl.getView().remove(activeList);
            FControl.getView().remove(backdrop);
            activeList = null;
            zoomPressed = false;
            detailsPressed = false;
            ownerPressed = false;
            pannedOverOptions = false;
        }

        @Override
        protected void drawBackground(Graphics g) {
            g.fillRect(BACK_COLOR, 0, 0, getWidth(), getHeight());
        }

        @Override
        protected void drawOverlay(Graphics g) { //draw top border
            g.drawLine(1, FList.LINE_COLOR, 0, 0, getWidth(), 0);
        }

        private static class Backdrop extends FDisplayObject {
            private Backdrop() {
            }

            @Override
            public boolean press(float x, float y) {
                if (activeList != null) {
                    CardAreaPanel owner = activeList.owner;
                    if (owner.contains(owner.getLeft() + owner.screenToLocalX(x), owner.getTop() + owner.screenToLocalY(y))) {
                        ownerPressed = true;
                    }
                    else {
                        if (zoomPressed || detailsPressed) {
                            if (y > getHeight() - LIST_OPTION_HEIGHT) {
                                //support pressing to toggle between zoom and details
                                if (x < getWidth() / 2) {
                                    if (detailsPressed) {
                                        zoomPressed = true;
                                        detailsPressed = false;
                                        return true;
                                    }
                                }
                                else if (zoomPressed) {
                                    zoomPressed = false;
                                    detailsPressed = true;
                                    return true;
                                }
                            }
                            //if already selected option pressed again or zoom/details itself pressed,
                            //hide everything on tap as if owner pressed a second time
                            ownerPressed = true;
                            return true;
                        }
                        else {
                            float ownerBottom = owner.getScreenPosition().y + owner.getHeight();
                            if (ownerBottom < y && y < ownerBottom + LIST_OPTION_HEIGHT) {
                                //handle pressing zoom and details options
                                zoomPressed = x < getWidth() / 2;
                                detailsPressed = !zoomPressed;
                                activeList.setVisible(false);
                                return true;
                            }
                        }
                        hide(); //auto-hide when backdrop pressed unless on owner
                    }
                }
                return false; //allow press to pass through to object
            }

            @Override
            public boolean release(float x, float y) {
                //prevent objects below handling release if zoom or details pressed
                return zoomPressed || detailsPressed;
            }

            @Override
            public boolean tap(float x, float y, int count) {
                if (ownerPressed) {
                    hide(); //hide when backdrop tapped over owner
                    return true;
                }
                //prevent objects below handling release if zoom or details pressed
                if (zoomPressed || detailsPressed) {
                    return true;
                }
                return false;
            }

            @Override
            public boolean pan(float x, float y, float deltaX, float deltaY) {
                ownerPressed = false;
                if (activeList != null) {
                    CardAreaPanel owner = activeList.owner;
                    Vector2 pos = owner.getScreenPosition();
                    return handlePan(owner, x - pos.x, y - pos.y, false);
                }
                return false;
            }

            @Override
            public boolean panStop(float x, float y) {
                if (activeList != null) {
                    CardAreaPanel owner = activeList.owner;
                    Vector2 pos = owner.getScreenPosition();
                    return handlePan(owner, x - pos.x, y - pos.y, true);
                }
                return false;
            }

            @Override
            public void draw(Graphics g) {
                if (activeList != null) {
                    //draw outline around that owns visible list
                    CardAreaPanel owner = activeList.owner;
                    Vector2 pos = owner.getScreenPosition();
                    float x = pos.x;
                    float y = pos.y;
                    float h = owner.getHeight();
                    float w = h / FCardPanel.ASPECT_RATIO;
                    if (owner.isTapped()) {
                        h = w;
                        w = owner.getHeight();
                        y = pos.y + owner.getHeight() - h;
                    }
                    if (owner.getAttachedToPanel() != null) {
                        //redraw owner if needed so it appears on top of cards above it in stack
                        owner.draw(g, pos.x, pos.y);
                    }
                    g.drawRect(2, Color.GREEN, x, y, w, h);

                    w = getWidth();
                    x = w / 2;

                    if (zoomPressed || detailsPressed) {
                        h = getHeight();
                        g.fillRect(BACK_COLOR, 0, 0, w, h); //draw backdrop for zoom/details
                        y = h - LIST_OPTION_HEIGHT; //move options to bottom so they're not blocking the zoom/details
                        h = LIST_OPTION_HEIGHT;
                    }
                    else {
                        h = LIST_OPTION_HEIGHT;
                        y = pos.y + owner.getHeight();
                        g.fillRect(BACK_COLOR, 0, y, w, h); //draw backdrop for zoom/details options
                    }

                    //draw zoom/details options
                    FSkinColor foreColor;
                    if (zoomPressed) {
                        drawZoom(g, owner.getCard(), w, y);
                        g.fillRect(FList.PRESSED_COLOR, 0, y, x, h);
                        foreColor = FList.FORE_COLOR;
                    }
                    else {
                        foreColor = FList.FORE_COLOR.alphaColor(ALPHA_COMPOSITE);
                    }
                    g.drawText("Zoom", activeList.getFont(), foreColor, 0, y, x, h, false, HAlignment.CENTER, true);

                    if (detailsPressed) {
                        drawDetails(g, owner.getCard(), w, y);
                        g.fillRect(FList.PRESSED_COLOR, x, y, w - x, h);
                        foreColor = FList.FORE_COLOR;
                    }
                    else {
                        foreColor = FList.FORE_COLOR.alphaColor(ALPHA_COMPOSITE);
                    }
                    g.drawText("Details", activeList.getFont(), foreColor, x, y, w - x, h, false, HAlignment.CENTER, true);

                    g.drawLine(1, FList.LINE_COLOR, 0, y, w, y);
                    g.drawLine(1, FList.LINE_COLOR, x, y, x, y + h);
                    y += h;
                    g.drawLine(1, FList.LINE_COLOR, 0, y, w, y);
                }
            }

            private static void drawZoom(Graphics g, Card card, float width, float height) {
                float x = FDialog.INSETS;
                float y = x;
                float w = width - 2 * x;
                float h = height - 2 * y;

                Texture image = ImageCache.getImage(card);

                float ratio = h / w;
                float imageRatio = (float)image.getHeight() / (float)image.getWidth(); //use image ratio rather than normal aspect ratio so it looks better

                if (ratio > imageRatio) {
                    float oldHeight = h;
                    h = w * imageRatio;
                    y += (oldHeight - h) / 2;
                }
                else {
                    float oldWidth = w;
                    w = h / imageRatio;
                    x += (oldWidth - w) / 2;
                }

                //prevent scaling image larger if preference turned off
                if (w > image.getWidth() || h > image.getHeight()) {
                    if (!FModel.getPreferences().getPrefBoolean(FPref.UI_SCALE_LARGER)) {
                        float oldWidth = w;
                        float oldHeight = h;
                        w = image.getWidth();
                        h = image.getHeight();
                        x += (oldWidth - w) / 2;
                        y += (oldHeight - h) / 2;
                    }
                }

                g.drawImage(image, x, y, w, h);
            }

            private static void drawDetails(Graphics g, Card card, float width, float height) {
                float x = FDialog.INSETS;
                float y = x;
                float w = width - 2 * x;
                float h = height - 2 * y;

                float ratio = h / w;
                if (ratio > FCardPanel.ASPECT_RATIO) {
                    float oldHeight = h;
                    h = w * FCardPanel.ASPECT_RATIO;
                    y += (oldHeight - h) / 2;
                }
                else {
                    float oldWidth = w;
                    w = h / FCardPanel.ASPECT_RATIO;
                    x += (oldWidth - w) / 2;
                }

                boolean canShow = !card.isFaceDown() && FControl.mayShowCard(card);

                float blackBorderThickness = w * 0.021f;
                g.fillRect(Color.BLACK, x, y, w, h);
                x += blackBorderThickness;
                y += blackBorderThickness;
                w -= 2 * blackBorderThickness;
                h -= 2 * blackBorderThickness;

                //determine colors for borders
                List<CardBorderColor> borderColors = CardDetailUtil.getBorderColors(card, canShow, true);
                CardBorderColor borderColor = borderColors.get(0);
                Color color1 = FSkinColor.fromRGB(borderColor.r, borderColor.g, borderColor.b);
                Color color2 = null;
                if (borderColors.size() > 1) {
                    borderColor = borderColors.get(1);
                    color2 = FSkinColor.fromRGB(borderColor.r, borderColor.g, borderColor.b);
                }
                if (color2 == null) {
                    g.fillRect(color1, x, y, w, h);
                }
                else {
                    g.fillGradientRect(color1, color2, false, x, y, w, h);
                }

                Color idForeColor = FSkinColor.getHighContrastColor(color1);

                float outerBorderThickness = 2 * blackBorderThickness;
                x += outerBorderThickness;
                y += outerBorderThickness;
                w -= 2 * outerBorderThickness;
                h =  Math.max(MANA_SYMBOL_SIZE + 2 * MANA_COST_PADDING, 2 * NAME_FONT.getFont().getCapHeight()) + 2 * TYPE_FONT.getFont().getCapHeight();

                //draw name/type box
                int nameManaCostStep = 100; //TODO: add better background colors to CardBorderColor enum
                color1 = FSkinColor.stepColor(color1, nameManaCostStep);
                if (color2 != null) {
                    color2 = FSkinColor.stepColor(color2, nameManaCostStep);
                }
                drawCardNameBox(g, card, color1, color2, x, y, w, h);

                float ptBoxHeight = 2 * PT_FONT.getFont().getCapHeight();

                float innerBorderThickness = outerBorderThickness / 2;
                y += h + innerBorderThickness;
                h = height - FDialog.INSETS - blackBorderThickness - ptBoxHeight - 2 * innerBorderThickness - y; 

                drawCardTextBox(g, card, canShow, color1, color2, x, y, w, h);

                y += h + innerBorderThickness;
                h = ptBoxHeight;
                drawCardIdAndPtBox(g, card, idForeColor, color1, color2, x, y, w, h);
            }

            private static void drawCardNameBox(Graphics g, Card card, Color color1, Color color2, float x, float y, float w, float h) {
                if (color2 == null) {
                    g.fillRect(color1, x, y, w, h);
                }
                else {
                    g.fillGradientRect(color1, color2, false, x, y, w, h);
                }
                g.drawRect(1, Color.BLACK, x, y, w, h);

                float padding = h / 8;

                //make sure name/mana cost row height is tall enough for both
                h = Math.max(MANA_SYMBOL_SIZE + 2 * MANA_COST_PADDING, 2 * NAME_FONT.getFont().getCapHeight());

                float manaCostWidth = CardFaceSymbols.getWidth(card.getManaCost(), MANA_SYMBOL_SIZE) + MANA_COST_PADDING;
                CardFaceSymbols.drawManaCost(g, card.getManaCost(), x + w - manaCostWidth, y + (h - MANA_SYMBOL_SIZE) / 2, MANA_SYMBOL_SIZE);

                x += padding;
                w -= 2 * padding;
                g.drawText(card.getName(), NAME_FONT, Color.BLACK, x, y, w - manaCostWidth - padding, h, false, HAlignment.LEFT, true);
                y += h;
                h = 2 * TYPE_FONT.getFont().getCapHeight();
                g.drawText(CardDetailUtil.formatCardType(card), TYPE_FONT, Color.BLACK, x, y, w, h, false, HAlignment.LEFT, true);
            }

            private static void drawCardTextBox(Graphics g, Card card, boolean canShow, Color color1, Color color2, float x, float y, float w, float h) {
                g.fillRect(Color.WHITE, x, y, w, h);
                g.drawRect(1, Color.BLACK, x, y, w, h);

                float padX = TEXT_FONT.getFont().getCapHeight() / 2;
                float padY = padX + 2; //add a little more vertical padding
                x += padX;
                y += padY;
                w -= 2 * padX;
                h -= 2 * padY;
                g.drawText(CardDetailUtil.composeCardText(card, canShow), TEXT_FONT, Color.BLACK, x, y, w, h, true, HAlignment.LEFT, false);
            }

            private static void drawCardIdAndPtBox(Graphics g, Card card, Color idForeColor, Color color1, Color color2, float x, float y, float w, float h) {
                g.drawText(CardDetailUtil.formatCardId(card), ID_FONT, idForeColor, x, y + ID_FONT.getFont().getCapHeight() / 2, w, h, false, HAlignment.LEFT, false);

                String text = CardDetailUtil.formatPowerToughness(card);
                if (StringUtils.isEmpty(text)) { return; }

                float padding = PT_FONT.getFont().getCapHeight() / 2;
                float boxWidth = PT_FONT.getFont().getBounds(text).width + 2 * padding;
                x += w - boxWidth;
                w = boxWidth;

                if (color2 == null) {
                    g.fillRect(color1, x, y, w, h);
                }
                else {
                    g.fillGradientRect(color1, color2, false, x, y, w, h);
                }
                g.drawRect(1, Color.BLACK, x, y, w, h);
                g.drawText(text, PT_FONT, Color.BLACK, x, y, w, h, false, HAlignment.CENTER, true);
            }
        }
    }
}
