package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Null;
import com.github.tommyettinger.textra.Font;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import com.github.tommyettinger.textra.TypingLabel;
import forge.Forge;
import forge.card.ColorSet;

import java.util.function.Function;

/**
 * Class to create ui elements in the correct style
 */
public class Controls {
    static class LabelFix extends TextraLabel
    {
        public LabelFix(String text)
        {
            super(text, getSkin(),getTextraFont());
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
    static class TextButtonFix extends TextraButton
    {
        public TextButtonFix(@Null String text)
        {
            super(text, Controls.getSkin(),Controls.getTextraFont()) ;
        }

        @Override
        public void setStyle(Button.ButtonStyle style, boolean makeGridGlyphs) {
            super.setStyle(style,makeGridGlyphs);
            this.getTextraLabel().setFont( Controls.getTextraFont());

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
        TextraButton button= new TextButtonFix(text);
       // button.getTextraLabelCell().fill(true,false).expand(true,false);//keep it the same as TextButton
        button.getTextraLabel().setWrap(false);
        return button;
    }
    static public Rectangle getBoundingRect(Actor actor) {
        return new Rectangle(actor.getX(),actor.getY(),actor.getWidth(),actor.getHeight());
    }
    static public boolean actorContainsVector (Actor actor, Vector2 point) {
        if (!actor.isVisible())
            return false;
        return getBoundingRect(actor).contains(point);
    }

    static public SelectBox newComboBox(String[] text, String item, Function<Object, Void> func) {
        SelectBox ret = new SelectBox<String>(getSkin());
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

    static public SelectBox newComboBox(Float[] text, float item, Function<Object, Void> func) {
        SelectBox ret = new SelectBox<Float>(getSkin());
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
                    func.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return ret;
    }

    static public Slider newSlider(float min, float max, float step, boolean vertical) {
        Slider ret = new Slider(min, max, step, vertical, getSkin());
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
                getSkin().getFont("default").getData().setScale(2, 2);
                return getSkin().getFont("default");
            default:
                getSkin().getFont("default").getData().setScale(1, 1);
                return getSkin().getFont("default");
        }
    }

    static public Skin getSkin() {
        FileHandle skinFile = Config.instance().getFile(Paths.SKIN);
        if (!Forge.getAssets().manager().contains(skinFile.path(), Skin.class)) {
            Forge.getAssets().manager().load(skinFile.path(), Skin.class);
            Forge.getAssets().manager().finishLoadingAsset(skinFile.path());
            FileHandle atlasFile = skinFile.sibling(skinFile.nameWithoutExtension() + ".atlas");
            Forge.getAssets().manager().load(atlasFile.path(), TextureAtlas.class);
            Forge.getAssets().manager().finishLoadingAsset(atlasFile.path());
            /*/font skin will load the LanaPixel.fnt now
            FileHandle pixelFont = Config.instance().getFile(Paths.SKIN).sibling("LanaPixel.fnt");
            Forge.getAssets().manager().load(pixelFont.path(), BitmapFont.class);
            Forge.getAssets().manager().finishLoadingAsset(pixelFont.path());
            Forge.getAssets().manager().get(skinFile.path(), Skin.class).add("default", Forge.getAssets().manager().get(pixelFont.path(), BitmapFont.class), BitmapFont.class);
            Forge.getAssets().manager().get(skinFile.path(), Skin.class).addRegions(Forge.getAssets().manager().get(atlasFile.path(), TextureAtlas.class));
            Forge.getAssets().manager().finishLoadingAsset(skinFile.path());
            */

        }
        return Forge.getAssets().manager().get(skinFile.path(), Skin.class);
    }
    public static Label newLabel(String name) {
        Label ret = new Label(name, getSkin());
        return ret;
    }
    static public Color colorFromString(String name)
    {
        String upperCase=name.toUpperCase();
        if(upperCase.startsWith("0X")||upperCase.startsWith("#"))
        {
            return new Color( Long.decode(upperCase).intValue());
        }
        if(upperCase.equals("WHITE"))
            return Color.WHITE;
        if(upperCase.equals("LIGHT_GRAY"))
            return Color.LIGHT_GRAY;
        if(upperCase.equals("GRAY"))
            return Color.GRAY;
        if(upperCase.equals("DARK_GRAY"))
            return Color.DARK_GRAY;
        if(upperCase.equals("BLACK"))
            return Color.BLACK;
        if(upperCase.equals("CLEAR"))
            return Color.CLEAR;
        if(upperCase.equals("BLUE"))
            return Color.BLUE;
        if(upperCase.equals("NAVY"))
            return Color.NAVY;
        if(upperCase.equals("ROYAL"))
            return Color.ROYAL;
        if(upperCase.equals("SLATE"))
            return Color.SLATE;
        if(upperCase.equals("SKY"))
            return Color.SKY;
        if(upperCase.equals("CYAN"))
            return Color.CYAN;
        if(upperCase.equals("TEAL"))
            return Color.TEAL;
        if(upperCase.equals("GREEN"))
            return Color.GREEN;
        if(upperCase.equals("CHARTREUSE"))
            return Color.CHARTREUSE;
        if(upperCase.equals("LIME"))
            return Color.LIME;
        if(upperCase.equals("FOREST"))
            return Color.FOREST;
        if(upperCase.equals("OLIVE"))
            return Color.OLIVE;
        if(upperCase.equals("YELLOW"))
            return Color.YELLOW;
        if(upperCase.equals("GOLD"))
            return Color.GOLD;
        if(upperCase.equals("GOLDENROD"))
            return Color.GOLDENROD;
        if(upperCase.equals("ORANGE"))
            return Color.ORANGE;
        if(upperCase.equals("TAN"))
            return Color.TAN;
        if(upperCase.equals("FIREBRICK"))
            return Color.FIREBRICK;
        if(upperCase.equals("RED"))
            return Color.RED;
        if(upperCase.equals("SCARLET"))
            return Color.SCARLET;
        if(upperCase.equals("CORAL"))
            return Color.CORAL;
        if(upperCase.equals("SALMON"))
            return Color.SALMON;
        if(upperCase.equals("PINK"))
            return Color.PINK;
        if(upperCase.equals("MAGENTA"))
            return Color.MAGENTA;
        if(upperCase.equals("PURPLE"))
            return Color.PURPLE;
        if(upperCase.equals("VIOLET"))
            return Color.VIOLET;
        if(upperCase.equals("PINK"))
            return Color.MAROON;
        return Color.BLACK;
    }

    public static TextraLabel newTextraLabel(String name) {
        TextraLabel ret = new LabelFix(name);
        return ret;
    }

    public static String colorIdToTypingString(ColorSet color)
    {
        String coloerId="";
        if(color.hasWhite())
            coloerId+="[+w]";
        if(color.hasBlue())
            coloerId+="[+u]";
        if(color.hasBlack())
            coloerId+="[+b]";
        if(color.hasRed())
            coloerId+="[+r]";
        if(color.hasGreen())
            coloerId+="[+g]";
        if(color.isColorless())
            coloerId+="[+c]";
        return coloerId;
    }
    public static TypingLabel newTypingLabel(String name) {
        TypingLabel ret = new TypingLabel(name==null?"":name, getSkin(),getTextraFont());
        ret.setVariable("player_name",Current.player().getName());
        ret.setVariable("player_color_id",colorIdToTypingString(Current.player().getColorIdentity()));
        return ret;
    }
    

    public static Dialog newDialog(String title) {
        Dialog ret = new Dialog(title, getSkin());
        ret.setMovable(false);
        return ret;
    }

    static Font textraFont=null;
    static public Font getTextraFont()
    {
        if(textraFont==null)
        {
            textraFont=new Font(getSkin().getFont("default"));
            textraFont.addAtlas(Config.instance().getAtlas(Paths.ITEMS_ATLAS));
            textraFont.adjustLineHeight(0.8f);//not sure why this is needed maybe the font is bad
        }
        return textraFont;
    }
    static public Font getTextraFont(String name)
    {
        Font font=new Font(getSkin().getFont(name));
        font.addAtlas(Config.instance().getAtlas(Paths.ITEMS_ATLAS));
        return font;
    }


}
