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
import forge.util.Callback;
import forge.util.Utils;

public class InputSelectCard {
    private static final float LIST_OPTION_HEIGHT = Utils.AVG_FINGER_HEIGHT;
    private static final long DOUBLE_TAP_INTERVAL = Utils.secondsToTimeSpan(FGestureAdapter.DOUBLE_TAP_INTERVAL); 
    private static CardOptionsList<?> visibleList;
    private static CardOptionsList<?>.ListItem pressedItem;
    private static long lastSelectTime;

    private InputSelectCard() {
    }

    public static void selectCard(CardAreaPanel cardPanel) {
        Input currentInput = FControl.getInputQueue().getInput();
        if (currentInput == null) { return; }

        long now = Gdx.input.getCurrentEventTime();

        if (visibleList != null) {
            boolean isCurrentOwner = (visibleList.owner == cardPanel);
            if (isCurrentOwner && visibleList.getCount() > 0 &&
                    now - lastSelectTime <= DOUBLE_TAP_INTERVAL) {
                //auto-select first option if double tapped
                visibleList.getItemAt(0).tap(0, 0, 1);
            }
            else {
                //otherwise hide options when pressed a second time
                CardOptionsList.hide();
            }
            if (isCurrentOwner) {
                return;
            }
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
        if (visibleList == null || visibleList.owner != cardPanel) {
            return false;
        }
        if (y < 0 && visibleList.getCount() > 0) {
            int index = Math.round(visibleList.getCount() + y / LIST_OPTION_HEIGHT);
            if (index < 0) {
                index = 0;
            }
            CardOptionsList<?>.ListItem item = visibleList.getItemAt(index);
            if (item != null) {
                if (isPanStop) {
                    item.tap(0, 0, 1);
                }
                else {
                    item.press(0, 0);
                    pressedItem = item;
                }
                return true;
            }
        }
        return false;
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
                public void drawValue(Graphics g, Object value, FSkinFont font, FSkinColor foreColor, float width, float height) {
                    float x = width * FList.INSETS_FACTOR;
                    float y = x;
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
            visibleList = optionsList;
        }

        private final CardAreaPanel owner;

        private CardOptionsList(CardAreaPanel owner0, Iterable<T> options) {
            super(options);
            owner = owner0;
        }

        public static void hide() {
            if (visibleList == null) { return; }
            FControl.getView().remove(visibleList);
            FControl.getView().remove(backdrop);
            visibleList = null;
        }

        protected float getUnpressedAlphaComposite() {
            return ALPHA_COMPOSITE;
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
                if (visibleList != null) {
                    CardAreaPanel owner = visibleList.owner;
                    if (!owner.contains(owner.getLeft() + owner.screenToLocalX(x), owner.getTop() + owner.screenToLocalY(y))) {
                        hide(); //auto-hide when backdrop pressed unless on owner
                    }
                }
                return false; //allow press to pass through to object
            }

            @Override
            public void draw(Graphics g) {
                if (visibleList != null) {
                    //draw outline around that owns visible list
                    CardAreaPanel owner = visibleList.owner;
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
                }
            }
        }
    }
}
