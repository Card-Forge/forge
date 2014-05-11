package forge.screens.match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.math.Vector2;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.assets.TextRenderer;
import forge.card.CardRenderer;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.match.input.Input;
import forge.match.input.InputPassPriority;
import forge.match.input.InputPayMana;
import forge.screens.FScreen;
import forge.screens.match.views.VCardDisplayArea.CardAreaPanel;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FGestureAdapter;
import forge.toolbox.FList;
import forge.toolbox.FOverlay;
import forge.util.Callback;
import forge.util.Utils;

public class InputSelectCard {
    private static final float LIST_OPTION_HEIGHT = Utils.AVG_FINGER_HEIGHT;
    private static final long DOUBLE_TAP_INTERVAL = Utils.secondsToTimeSpan(FGestureAdapter.DOUBLE_TAP_INTERVAL); 
    private static CardOptionsList<?> activeList;
    private static boolean simulatedListPress;
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
                    activeList.tap(0, -activeList.getScrollTop(), 1);
                }
                return; //don't select already selected card
            }
            hide(); //hide previous card options list before showing a new one
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

    public static boolean hide() {
        if (activeList == null) { return false; }
        FControl.getView().remove(activeList);
        FControl.getView().remove(CardOptionsList.backdrop);
        activeList = null;
        zoomPressed = false;
        detailsPressed = false;
        ownerPressed = false;
        pannedOverOptions = false;
        return true;
    }

    public static boolean handlePan(CardAreaPanel cardPanel, float x, float y, boolean isPanStop) {
        if (simulatedListPress) {
            //prevent pressed item getting stuck
            if (activeList != null) {
                activeList.release(x, y - activeList.getTop());
            }
            simulatedListPress = false;
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
                //simulate tap or press on list
                float listY = activeList.getHeight() + y;
                if (listY < 0) {
                    listY = 0;
                }
                if (isPanStop) {
                    activeList.tap(x, listY, 1);
                }
                else {
                    activeList.press(x, listY);
                    simulatedListPress = true;
                }
                pannedOverOptions = true;
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
        private static final FSkinColor BACK_COLOR = FSkinColor.get(Colors.CLR_OVERLAY).alphaColor(FOverlay.ALPHA_COMPOSITE);

        private static final Backdrop backdrop = new Backdrop();
        private static final TextRenderer cardOptionRenderer = new TextRenderer(true); //use text renderer to handle mana symbols

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
                public void drawValue(Graphics g, Object value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
                    if (!pressed) {
                        foreColor = foreColor.alphaColor(FOverlay.ALPHA_COMPOSITE);
                    }
                    cardOptionRenderer.drawText(g, value.toString(), font, foreColor, x, y, w, h, true, HAlignment.CENTER, true);
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
                    g.drawRect(FCardPanel.PADDING, Color.GREEN, x, y, w, h);

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
                        CardRenderer.drawZoom(g, owner.getCard(), w, y);
                        g.fillRect(FList.PRESSED_COLOR, 0, y, x, h);
                        foreColor = FList.FORE_COLOR;
                    }
                    else {
                        foreColor = FList.FORE_COLOR.alphaColor(FOverlay.ALPHA_COMPOSITE);
                    }
                    g.drawText("Zoom", activeList.getFont(), foreColor, 0, y, x, h, false, HAlignment.CENTER, true);

                    if (detailsPressed) {
                        CardRenderer.drawDetails(g, owner.getCard(), w, y);
                        g.fillRect(FList.PRESSED_COLOR, x, y, w - x, h);
                        foreColor = FList.FORE_COLOR;
                    }
                    else {
                        foreColor = FList.FORE_COLOR.alphaColor(FOverlay.ALPHA_COMPOSITE);
                    }
                    g.drawText("Details", activeList.getFont(), foreColor, x, y, w - x, h, false, HAlignment.CENTER, true);

                    g.drawLine(1, FList.LINE_COLOR, 0, y, w, y);
                    g.drawLine(1, FList.LINE_COLOR, x, y, x, y + h);
                    y += h;
                    g.drawLine(1, FList.LINE_COLOR, 0, y, w, y);
                }
            }
        }
    }
}
