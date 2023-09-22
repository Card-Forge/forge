package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.Timer;
import com.github.tommyettinger.textra.Font;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import com.github.tommyettinger.textra.TypingLabel;
import forge.Forge;
import forge.adventure.player.AdventurePlayer;
import forge.card.ColorSet;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;

import java.util.function.Function;

/**
 * Class to create ui elements in the correct style
 */
public class Controls {
    static class LabelFix extends TextraLabel {
        public LabelFix(String text, Font font) {
            super(text, getSkin(), font);
        }

        @Override
        public void setText(@Null String text) {
            this.storedText = text;
            this.layout.setTargetWidth(this.getMaxWidth());
            this.getFont().markup(text, this.layout.clear());
            this.setWidth(this.layout.getWidth() + (this.style != null && this.style.background != null ? this.style.background.getLeftWidth() + this.style.background.getRightWidth() : 0.0F));
            layout();
        }
    }

    static class TextButtonFix extends TextraButton {
        public TextButtonFix(@Null String text) {
            super(text == null ? "NULL" : text, Controls.getSkin(), Controls.getTextraFont());
            addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    SoundSystem.instance.play(SoundEffectType.ButtonPress, false);
                }
            });
        }

        @Override
        public void setStyle(Button.ButtonStyle style, boolean makeGridGlyphs) {
            super.setStyle(style, makeGridGlyphs);
            this.getTextraLabel().setFont(Controls.getTextraFont());

        }

        @Override
        public String getText() {
            return this.getTextraLabel().storedText;
        }

        @Override
        public void setText(@Null String text) {
            getTextraLabel().storedText = text;
            getTextraLabel().layout.setTargetWidth(getTextraLabel().getMaxWidth());
            getTextraLabel().getFont().markup(text, getTextraLabel().layout.clear());
            getTextraLabel().setWidth(getTextraLabel().layout.getWidth() + (getTextraLabel().style != null && getTextraLabel().style.background != null ? getTextraLabel().style.background.getLeftWidth() + getTextraLabel().style.background.getRightWidth() : 0.0F));
            layout();
        }

    }

    static public TextraButton newTextButton(String text) {
        TextraButton button = new TextButtonFix(text);
        button.getTextraLabel().setWrap(false);
        return button;
    }

    static public Rectangle getBoundingRect(Actor actor) {
        return new Rectangle(actor.getX(), actor.getY(), actor.getWidth(), actor.getHeight());
    }

    static public boolean actorContainsVector(Actor actor, Vector2 point) {
        if (actor == null)
            return false;
        if (!actor.isVisible())
            return false;
        return getBoundingRect(actor).contains(point);
    }

    static public boolean actorContainsVector(Array<TextraButton> buttons, Vector2 point) {
        boolean value = false;
        if (buttons == null)
            return false;
        if (buttons.isEmpty())
            return false;
        for (Actor actor : buttons) {
            if (actor == null)
                return false;
            if (!actor.isVisible())
                return false;
            if (getBoundingRect(actor).contains(point))
                value = true;
        }
        return value;
    }

    static public SelectBox<String> newComboBox(String[] text, String item, Function<Object, Void> func) {
        SelectBox<String> ret = newComboBox();
        ret.getStyle().listStyle.selection.setTopHeight(4);
        ret.setItems(text);
        ret.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    func.apply(((SelectBox) actor).getSelected());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        func.apply(item);
        ret.getList().setAlignment(Align.center);
        ret.setSelected(item);
        ret.setAlignment(Align.right);
        return ret;
    }

    static public SelectBox<String> newComboBox(Array<String> text, String item, Function<Object, Void> func) {
        SelectBox<String> ret = newComboBox();
        ret.getStyle().listStyle.selection.setTopHeight(4);
        ret.setItems(text);
        ret.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    func.apply(((SelectBox) actor).getSelected());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        func.apply(item);
        ret.getList().setAlignment(Align.center);
        ret.setSelected(item);
        ret.setAlignment(Align.right);
        return ret;
    }

    static public <T> SelectBox newComboBox() {
        return new SelectBox<T>(getSkin()) {

            @Null
            protected Drawable getBackgroundDrawable() {
                if (this.isDisabled() && this.getStyle().backgroundDisabled != null) {
                    return this.getStyle().backgroundDisabled;
                } else if (this.getScrollPane().hasParent() && this.getStyle().backgroundOpen != null) {
                    return this.getStyle().backgroundOpen;
                } else {
                    return (this.isOver() || hasKeyboardFocus()) && this.getStyle().backgroundOver != null ? this.getStyle().backgroundOver : this.getStyle().background;
                }
            }

            @Override
            protected GlyphLayout drawItem(Batch batch, BitmapFont font, T item, float x, float y, float width) {
                if (getUserObject() != null && getUserObject() instanceof String) { //currently this is used on spellsmith...
                    if (((String) getUserObject()).isEmpty())
                        return super.drawItem(batch, font, (T) Forge.getLocalizer().getMessage("lblSelectingFilter"), x, y, width);
                }
                return super.drawItem(batch, font, item, x, y, width);
            }
        };
    }

    static public SelectBox<Float> newComboBox(Float[] text, float item, Function<Object, Void> func) {
        SelectBox<Float> ret = newComboBox();
        ret.getStyle().listStyle.selection.setTopHeight(4);
        ret.setItems(text);
        ret.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    func.apply(((SelectBox) actor).getSelected());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        func.apply(item);
        ret.getList().setAlignment(Align.center);
        ret.setSelected(item);
        ret.setAlignment(Align.right);
        return ret;
    }

    static public TextField newTextField(String text) {
        return new TextField(text, getSkin());
    }

    static public TextField newTextField(String text, Function<String, Void> func) {
        TextField ret = new TextField(text, getSkin());
        ret.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    func.apply(((TextField) actor).getText());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return ret;
    }

    static public TextraButton newTextButton(String text, Runnable func) {
        TextraButton ret = newTextButton(text);
        ret.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    if (func != null)
                        func.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return ret;
    }

    static public Slider newSlider(float min, float max, float step, boolean vertical) {
        Slider ret = new Slider(min, max, step, vertical, getSkin()) {
            @Override
            protected Drawable getBackgroundDrawable() {
                SliderStyle style = (SliderStyle) super.getStyle();
                if (this.isDisabled() && style.disabledBackground != null) {
                    return style.disabledBackground;
                } else if (this.isDragging() && style.backgroundDown != null) {
                    return style.backgroundDown;
                } else {
                    return (this.isOver() || hasKeyboardFocus()) && style.backgroundOver != null ? style.backgroundOver : style.background;
                }
            }
        };
        return ret;
    }

    static public CheckBox newCheckBox(String text) {
        CheckBox ret = new CheckBox(text, getSkin());
        return ret;
    }

    static public BitmapFont getBitmapFont(String fontName) {
        switch (fontName) {
            case "blackbig":
            case "big":
                return getBitmapFont("default", 2);
            default:
                return getBitmapFont("default", 1);
        }
    }

    static public BitmapFont getBitmapFont(String fontName, float scaleXY) {
        getSkin().getFont(fontName).getData().setScale(scaleXY, scaleXY);
        return getSkin().getFont("default");
    }

    static public Skin getSkin() {
        FileHandle skinFile = Config.instance().getFile(Paths.SKIN);
        Skin skin = Forge.getAssets().manager().get(skinFile.path(), Skin.class, false);
        if (skin == null) {
            Forge.getAssets().manager().load(skinFile.path(), Skin.class);
            Forge.getAssets().manager().finishLoadingAsset(skinFile.path());
            skin = Forge.getAssets().manager().get(skinFile.path(), Skin.class, false);
        }
        return skin;
    }

    public static Label newLabel(String name) {
        Label ret = new Label(name, getSkin());
        return ret;
    }

    static public Color colorFromString(String name) {
        String upperCase = name.toUpperCase();
        if (upperCase.startsWith("0X") || upperCase.startsWith("#")) {
            return new Color(Long.decode(upperCase).intValue());
        }
        if (upperCase.equals("WHITE"))
            return Color.WHITE;
        if (upperCase.equals("LIGHT_GRAY"))
            return Color.LIGHT_GRAY;
        if (upperCase.equals("GRAY"))
            return Color.GRAY;
        if (upperCase.equals("DARK_GRAY"))
            return Color.DARK_GRAY;
        if (upperCase.equals("BLACK"))
            return Color.BLACK;
        if (upperCase.equals("CLEAR"))
            return Color.CLEAR;
        if (upperCase.equals("BLUE"))
            return Color.BLUE;
        if (upperCase.equals("NAVY"))
            return Color.NAVY;
        if (upperCase.equals("ROYAL"))
            return Color.ROYAL;
        if (upperCase.equals("SLATE"))
            return Color.SLATE;
        if (upperCase.equals("SKY"))
            return Color.SKY;
        if (upperCase.equals("CYAN"))
            return Color.CYAN;
        if (upperCase.equals("TEAL"))
            return Color.TEAL;
        if (upperCase.equals("GREEN"))
            return Color.GREEN;
        if (upperCase.equals("CHARTREUSE"))
            return Color.CHARTREUSE;
        if (upperCase.equals("LIME"))
            return Color.LIME;
        if (upperCase.equals("FOREST"))
            return Color.FOREST;
        if (upperCase.equals("OLIVE"))
            return Color.OLIVE;
        if (upperCase.equals("YELLOW"))
            return Color.YELLOW;
        if (upperCase.equals("GOLD"))
            return Color.GOLD;
        if (upperCase.equals("GOLDENROD"))
            return Color.GOLDENROD;
        if (upperCase.equals("ORANGE"))
            return Color.ORANGE;
        if (upperCase.equals("TAN"))
            return Color.TAN;
        if (upperCase.equals("FIREBRICK"))
            return Color.FIREBRICK;
        if (upperCase.equals("RED"))
            return Color.RED;
        if (upperCase.equals("SCARLET"))
            return Color.SCARLET;
        if (upperCase.equals("CORAL"))
            return Color.CORAL;
        if (upperCase.equals("SALMON"))
            return Color.SALMON;
        if (upperCase.equals("PINK"))
            return Color.PINK;
        if (upperCase.equals("MAGENTA"))
            return Color.MAGENTA;
        if (upperCase.equals("PURPLE"))
            return Color.PURPLE;
        if (upperCase.equals("VIOLET"))
            return Color.VIOLET;
        if (upperCase.equals("MAROON"))
            return Color.MAROON;
        return Color.BLACK;
    }

    public static TextraLabel newTextraLabel(String name, Font font) {
        TextraLabel ret = new LabelFix(name, font);
        return ret;
    }

    public static TextraLabel newTextraLabel(String name) {
        return newTextraLabel(name, getTextraFont());
    }

    public static TextraLabel newRewardLabel(String name) {
        return newTextraLabel(name, getRewardHeaderFont());
    }

    public static String colorIdToTypingString(ColorSet color) {
        return colorIdToTypingString(color, false);
    }

    public static String colorIdToTypingString(ColorSet color, boolean vertical) {
        String nextline = vertical ? "\n" : "";
        //NOTE converting to uppercase will use pixelmana.atlas, higher quality pixel mana symbol.
        String colorId = "";
        if (color.hasWhite())
            colorId += "[+w]"+nextline;
        if (color.hasBlue())
            colorId += "[+u]"+nextline;
        if (color.hasBlack())
            colorId += "[+b]"+nextline;
        if (color.hasRed())
            colorId += "[+r]"+nextline;
        if (color.hasGreen())
            colorId += "[+g]"+nextline;
        if (color.isColorless())
            colorId += "[+c]"+nextline;
        return colorId;
    }

    public static TypingLabel newTypingLabel(String name) {
        TypingLabel ret = new TypingLabel(name == null ? "" : name, getSkin(), getTextraFont());
        ret.setVariable("player_name", Current.player().getName());
        ret.setVariable("player_color_id", colorIdToTypingString(Current.player().getColorIdentity()));
        return ret;
    }


    public static Dialog newDialog(String title) {
        Dialog ret = new Dialog(title, getSkin());
        ret.setMovable(false);
        return ret;
    }

    static public Font getKeysFont() {
        return Forge.getAssets().getKeysFont(getSkin().getFont("default"), Config.instance().getAtlas(Paths.KEYS_ATLAS));
    }

    static public Font getTextraFont() {
        return Forge.getAssets().getTextraFont(getSkin().getFont("default"), Config.instance().getAtlas(Paths.ITEMS_ATLAS), Config.instance().getAtlas(Paths.PIXELMANA_ATLAS));
    }

    static public Font getRewardHeaderFont() {
        return Forge.getAssets().getGenericHeaderFont(getSkin().getFont("default"));
    }

    static public Font getTextraFont(String name) {
        return Forge.getAssets().getTextraFont(name, getSkin().getFont(name), Config.instance().getAtlas(Paths.ITEMS_ATLAS));
    }

    static public class AccountingLabel extends TextraLabel {
        private TextraLabel label;
        private final TextraLabel placeholder;
        private String currencyIcon;
        private boolean isShards;
        private int currencyAmount;
        private float animationDelay = 2f; //seconds to wait before replacing intermediate label
        private final String NEGDECOR = "[RED]-";
        private final String POSDECOR = "[GREEN]+";
        private final Timer t = new Timer();

        public AccountingLabel(TextraLabel target, boolean isShards) {
            target.setVisible(false);
            placeholder = target;
            label = Controls.newTextraLabel(target.getName() + "Replacement");
            currencyAmount = isShards ? Current.player().getShards() : Current.player().getGold();
            this.isShards = isShards;

            if (isShards) {
                currencyAmount = Current.player().getShards();
                currencyIcon = "[+Shards]";
                Current.player().onShardsChange(() -> update(AdventurePlayer.current().getShards(), true));
            } else {
                currencyAmount = Current.player().getGold();
                currencyIcon = "[+Gold] "; //fix space since gold sprite is wider than a single glyph
                Current.player().onGoldChange(() -> update(AdventurePlayer.current().getGold(), true));
            }
            label.setText(getLabelText(currencyAmount));
            setName(label.getName());
            replaceLabel(label);
        }

        public void setAnimationDelay(float animationDelay) {
            this.animationDelay = animationDelay;
        }

        public float getAnimationDelay() {
            return animationDelay;
        }

        public void update(int newAmount) {
            update(newAmount, false);
        }

        public void update(int newAmount, boolean animate) {

            if (animate) {
                TextraLabel temporaryLabel = getUpdateLabel(newAmount);
                currencyAmount = newAmount;
                replaceLabel(temporaryLabel);

                t.schedule(new AccountingLabelUpdater(temporaryLabel), animationDelay);
            } else {
                currencyAmount = newAmount;
                drawFinalLabel(false);
            }
        }

        private void drawFinalLabel(boolean fadeIn) {

            TextraLabel finalLabel = getDefaultLabel();
            if (fadeIn) {
                SequenceAction sequence = new SequenceAction();
                sequence.addAction(Actions.alpha(0.5f));
                sequence.addAction(Actions.alpha(1f, 2f, Interpolation.pow2Out));
                finalLabel.addAction(sequence);
            }
            replaceLabel(finalLabel);
        }

        private TextraLabel getDefaultLabel() {
            return Controls.newTextraLabel(getLabelText(currencyAmount));
        }

        private TextraLabel getUpdateLabel(int newAmount) {
            int delta = newAmount - currencyAmount;
            String updateText = delta == 0 ? "" : (delta < 0 ? NEGDECOR + delta * -1 : POSDECOR + delta);
            return Controls.newTextraLabel(getLabelText(currencyAmount, updateText));
        }

        private String getLabelText(int amount) {
            return getLabelText(amount, "");
        }

        private String getLabelText(int amount, String updateText) {
            return amount + " " + currencyIcon + updateText;
        }

        private void replaceLabel(TextraLabel newLabel) {
            newLabel.setName(label.getName());
            newLabel.style = placeholder.style;
            newLabel.setBounds(placeholder.getX(), placeholder.getY(), label.getWidth(), placeholder.getHeight());
            newLabel.setFont(label.getFont());
            newLabel.style = placeholder.style;
            newLabel.layout.setBaseColor(label.layout.getBaseColor());
            newLabel.layout();

            label.remove();
            label = newLabel;
            placeholder.getStage().addActor(label);
        }

        private class AccountingLabelUpdater extends Timer.Task {
            @Override
            public void run() {
                if (label.equals(target)) {
                    drawFinalLabel(true);
                }
            }

            TextraLabel target;

            AccountingLabelUpdater(TextraLabel replacement) {
                this.target = replacement;
            }
        }
    }

    public static TextraLabel newAccountingLabel(TextraLabel target, Boolean isShards) {
        AccountingLabel label = new AccountingLabel(target, isShards);
        return label;
    }
}
