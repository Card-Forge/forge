package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
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
import forge.Forge;

import java.util.function.Function;

/**
 * Class to create ui elements in the correct style
 */
public class Controls {
    static public TextButton newTextButton(String text) {
        return new TextButton(text, GetSkin());
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
        SelectBox ret = new SelectBox<String>(GetSkin());
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
        SelectBox ret = new SelectBox<Float>(GetSkin());
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
        return new TextField(text, GetSkin());
    }

    static public TextField newTextField(String text, Function<String, Void> func) {
        TextField ret = new TextField(text, GetSkin());
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

    static public TextButton newTextButton(String text, Runnable func) {
        TextButton ret = newTextButton(text);
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
        Slider ret = new Slider(min, max, step, vertical, GetSkin());
        return ret;
    }

    static public CheckBox newCheckBox(String text) {
        CheckBox ret = new CheckBox(text, GetSkin());
        return ret;
    }

    static public BitmapFont getBitmapFont(String fontName) {
        switch (fontName) {
            case "blackbig":
            case "big":
                GetSkin().getFont("default").getData().setScale(2, 2);
                return GetSkin().getFont("default");
            default:
                GetSkin().getFont("default").getData().setScale(1, 1);
                return GetSkin().getFont("default");
        }
    }

    static public Skin GetSkin() {
        FileHandle skinFile = Config.instance().getFile(Paths.SKIN);
        if (!Forge.getAssets().manager().contains(skinFile.path(), Skin.class)) {
            Forge.getAssets().manager().load(skinFile.path(), Skin.class);
            Forge.getAssets().manager().finishLoadingAsset(skinFile.path());
            FileHandle atlasFile = skinFile.sibling(skinFile.nameWithoutExtension() + ".atlas");
            Forge.getAssets().manager().load(atlasFile.path(), TextureAtlas.class);
            Forge.getAssets().manager().finishLoadingAsset(atlasFile.path());
            //font
            FileHandle pixelFont = Config.instance().getFile(Paths.SKIN).sibling("LanaPixel.fnt");
            Forge.getAssets().manager().load(pixelFont.path(), BitmapFont.class);
            Forge.getAssets().manager().finishLoadingAsset(pixelFont.path());
            Forge.getAssets().manager().get(skinFile.path(), Skin.class).add("default", Forge.getAssets().manager().get(pixelFont.path(), BitmapFont.class), BitmapFont.class);
            Forge.getAssets().manager().get(skinFile.path(), Skin.class).addRegions(Forge.getAssets().manager().get(atlasFile.path(), TextureAtlas.class));
            Forge.getAssets().manager().finishLoadingAsset(skinFile.path());
        }
        return Forge.getAssets().manager().get(skinFile.path(), Skin.class);
    }

    public static Label newLabel(String name) {
        Label ret = new Label(name, GetSkin());
        return ret;
    }

    public static Dialog newDialog(String title) {
        Dialog ret = new Dialog(title, GetSkin());
        ret.setMovable(false);
        return ret;
    }


}
