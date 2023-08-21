package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.stage.GameHUD;
import forge.adventure.util.*;

import java.time.LocalTime;

/**
 * Base class for an GUI scene where the elements are loaded from a json file
 */
public class UIScene extends Scene {
    protected UIActor ui;


    public static class Selectable<T extends Actor> {
        public T actor;

        public float getY() {
            Actor act = actor;
            float y = 0;
            while (act != null) {
                y += act.getY();
                act = act.getParent();
            }
            return y;
        }

        public float getX() {
            Actor act = actor;
            float x = 0;
            while (act != null) {
                x += act.getX();
                act = act.getParent();
            }
            return x;
        }

        public Selectable(T newActor) {
            actor = newActor;
        }

        public void onSelect(UIScene scene) {
        }

        public void onDeSelect() {
            //actor.fire(UIScene.eventExit());
        }

        public void onPressDown(UIScene scene) {
            if (actor instanceof TextField) {
                scene.requestTextInput(((TextField) actor).getText(), text -> ((TextField) actor).setText(text));

            }
            actor.fire(UIScene.eventTouchDown());
        }

        public void onPressUp() {
            actor.fire(UIScene.eventTouchUp());
        }

        public float yDiff(Selectable finalOne) {
            return Math.abs(finalOne.getY() - getY());
        }

        public float xDiff(Selectable finalOne) {
            return Math.abs(finalOne.getX() - getX());
        }
    }

    public void showDialog(Dialog dialog) {
        stage.addActor(dialog);
        possibleSelectionStack.add(new Array<>());
        addToSelectable(dialog.getContentTable());
        addToSelectable(dialog.getButtonTable());
        dialog.getColor().a = 0;
        stage.setKeyboardFocus(dialog);
        stage.setScrollFocus(dialog);
        for (Dialog otherDialogs : dialogs)
            otherDialogs.hide();
        dialogs.add(dialog);
        selectFirst();
        dialog.show(stage);

    }

    private void requestTextInput(String text, KeyBoardDialog.ScreenKeyboardFinished e) {
        KeyBoardDialog keyboardDialog=new KeyBoardDialog()
        {
            @Override
            protected void result(@Null Object object) {
                removeDialog();
            }
        };
        keyboardDialog.setText(text);
        keyboardDialog.setOnFinish(e);
        showDialog(keyboardDialog);
        //possibleSelection=keyboardDialog.keys();
    }

    public Array<Array<Selectable>> possibleSelectionStack = new Array<>();
    public Array<Dialog> dialogs = new Array<>();

    public Array<Selectable> getPossibleSelection() {
        if (possibleSelectionStack.isEmpty())
            possibleSelectionStack.add(ui.selectActors);
        return possibleSelectionStack.get(possibleSelectionStack.size - 1);
    }

    protected Stage stage;

    String uiFile;

    public static InputEvent eventTouchUp() {
        InputEvent event = new InputEvent();
        event.setPointer(-1);
        event.setType(InputEvent.Type.touchUp);
        return event;
    }

    public static InputEvent eventTouchDown() {
        InputEvent event = new InputEvent();
        event.setPointer(-1);
        event.setType(InputEvent.Type.touchDown);
        return event;
    }

    public static InputEvent eventExit() {

        InputEvent event = new InputEvent();
        event.setPointer(-1);
        event.setType(InputEvent.Type.exit);
        return event;
    }

    public static InputEvent eventEnter() {
        InputEvent event = new InputEvent();
        event.setPointer(-1);
        event.setType(InputEvent.Type.enter);
        return event;
    }

    @Override
    public boolean buttonUp(Controller controller, int keycode) {
        return stage.keyUp(KeyBinding.controllerButtonToKey(controller, keycode));
    }

    @Override
    public boolean buttonDown(Controller controller, int keycode) {
        return stage.keyDown(KeyBinding.controllerButtonToKey(controller, keycode));
    }

    protected void addToSelectable(Table table) {
        for (Cell cell : table.getCells()) {
            if (cell.getActor() != null && cell.getActor().getClass() != Actor.class && !(cell.getActor() instanceof Label) && !(cell.getActor() instanceof TextraLabel))
                getPossibleSelection().add(new Selectable(cell.getActor()));
        }
    }

    protected void addToSelectable(Button button)//prevent to addToSelectable(Table) fallback
    {
        getPossibleSelection().add(new Selectable(button));
    }

    protected void addToSelectable(Actor button) {
        getPossibleSelection().add(new Selectable(button));
    }

    protected void addToSelectable(Selectable selectable) {
        getPossibleSelection().add(selectable);
    }

    protected void clearSelectable() {
        getPossibleSelection().clear();
    }

    public UIScene(String uiFilePath) {
        uiFile = uiFilePath;
        stage = new Stage(new ScalingViewport(Scaling.stretch, getIntendedWidth(), getIntendedHeight())) {
            @Override
            public boolean keyUp(int keycode) {
                keyReleased(keycode);
                return super.keyUp(keycode);
            }

            @Override
            public boolean keyDown(int keyCode) {
                keyPressed(keyCode);
                return super.keyDown(keyCode);
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                pointerMoved(screenX, screenY);
                return super.mouseMoved(screenX, screenY);
            }
        };
        ui = new UIActor(Config.instance().getFile(uiFile));
        for (Actor actor : ui.getChildren()) {
            if (actor instanceof ScrollPane)
                stage.setScrollFocus(actor);
        }
        possibleSelectionStack.add(ui.selectActors);
        screenImage = ui.findActor("lastScreen");
        stage.addActor(ui);
    }

    public void removeDialog() {

        if (!dialogs.isEmpty()) {
            dialogs.get(dialogs.size - 1).remove();
            dialogs.removeIndex(dialogs.size - 1);

            if (!dialogs.isEmpty())
                dialogs.get(dialogs.size - 1).show(stage);
        }
        if (possibleSelectionStack.isEmpty()) {
            getPossibleSelection();
        } else {
            possibleSelectionStack.removeIndex(possibleSelectionStack.size - 1);
        }
    }

    public Dialog createGenericDialog(String title, String label, String stringYes, String stringNo, Runnable runnableYes, Runnable runnableNo) {
        Dialog dialog = new Dialog(title == null ? "" : title, Controls.getSkin());
        if (label != null)
            dialog.text(label);
        TextraButton yes = Controls.newTextButton(stringYes, runnableYes);
        dialog.button(yes);
        if (stringNo != null) {
            TextraButton no = Controls.newTextButton(stringNo, runnableNo);
            dialog.button(no);
        }
        return dialog;
    }

    static float timeOfDay = 8.0f;
    float targetTime = 8.0f;

    public static float getTimeOfDay() {
        return timeOfDay;
    }

    public void setTargetTime(float val) {
        targetTime = val;
    }

    public void setTimeOfDay(float val) {
        timeOfDay = val;
    }

    @Override
    public void dispose() {
        if (stage != null)
            stage.dispose();
    }

    @Override
    public void act(float delta) {
        stage.act(delta);
        if (timeOfDay < targetTime) {
            timeOfDay += (delta * 1.5f);
            if (timeOfDay > targetTime)
                timeOfDay = targetTime;
        } else if (timeOfDay > targetTime) {
            timeOfDay -= (delta * 1.5f);
            if (timeOfDay < targetTime)
                timeOfDay = targetTime;
        }
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.draw();
    }

    public UIActor getUI() {
        return ui;
    }


    public boolean back() {
        GameHUD.getInstance().getTouchpad().setVisible(false);
        Forge.switchToLast();
        return true;
    }

    public Selectable getSelected() {
        for (Selectable selectable : getPossibleSelection()) {
            if (stage.getKeyboardFocus() == selectable.actor)
                return selectable;
        }
        return null;
    }

    public boolean keyReleased(int keycode) {
        ui.pressUp(keycode);
        if (!dialogShowing()) {
            Button pressedButton = ui.buttonPressed(keycode);
            if (pressedButton != null) {
                if (pressedButton.isVisible())
                    pressedButton.fire(eventTouchUp());
            }
        }
        if (KeyBinding.Use.isPressed(keycode)) {
            if (getSelected() != null)
                getSelected().onPressUp();//order is important, this might remove a dialog

        }

        return true;
    }

    public boolean keyPressed(int keycode) {
        Selectable selection = getSelected();

        if (KeyBinding.Use.isPressed(keycode)) {
            if (selection != null) {
                selection.onPressDown(this);
                return true;
            }
        }

        ui.pressDown(keycode);
        if (stage.getKeyboardFocus() instanceof SelectBox) {
            SelectBox box = (SelectBox) stage.getKeyboardFocus();
            if (box.getScrollPane().hasParent()) {
                if (KeyBinding.Use.isPressed(keycode)) {
                    box.getSelection().choose(box.getList().getSelected());
                    box.getScrollPane().hide();
                }
                return false;
            }
        }


        if (KeyBinding.Back.isPressed(keycode) && selection != null) {
            selection.onDeSelect();
            stage.setKeyboardFocus(null);
        }
        if (KeyBinding.ScrollUp.isPressed(keycode)) {
            Actor focus = stage.getScrollFocus();
            if (focus != null && focus instanceof ScrollPane) {
                ScrollPane scroll = ((ScrollPane) focus);
                scroll.setScrollY(scroll.getScrollY() - 20);
            }
        }
        if (KeyBinding.ScrollDown.isPressed(keycode)) {
            Actor focus = stage.getScrollFocus();
            if (focus != null && focus instanceof ScrollPane) {
                ScrollPane scroll = ((ScrollPane) focus);
                scroll.setScrollY(scroll.getScrollY() + 20);
            }
        }
        if (KeyBinding.Down.isPressed(keycode))
            selectNextDown();
        if (KeyBinding.Up.isPressed(keycode))
            selectNextUp();
        if (!(stage.getKeyboardFocus() instanceof Selector) && !(stage.getKeyboardFocus() instanceof TextField) && !(stage.getKeyboardFocus() instanceof Slider)) {
            if (KeyBinding.Right.isPressed(keycode))
                selectNextRight();
            if (KeyBinding.Left.isPressed(keycode))
                selectNextLeft();
        }
        if (!dialogShowing()) {
            Button pressedButton = ui.buttonPressed(keycode);
            if (pressedButton != null) {
                if (pressedButton.isVisible())
                    pressedButton.fire(eventTouchDown());
            }
        }
        return true;
    }

    private boolean dialogShowing() {
        return !dialogs.isEmpty();
    }


    @Override
    public void disconnected(final Controller controller) {
        ui.controllerDisconnected();
    }

    @Override
    public void connected(final Controller controller) {
        selectFirst();
        ui.controllerConnected();
    }

    public boolean pointerMoved(int screenX, int screenY) {
        unselectActors();
        return false;
    }

    public void performTouch(Actor actor) {
        if (actor == null)
            return;
        actor.fire(eventTouchDown());
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                actor.fire(eventTouchUp());
            }
        }, 0.10f);
    }

    public void unselectActors() {
        for (Selectable selectable : getPossibleSelection()) {
            selectable.onDeSelect();
        }
    }

    Array<Selectable> visibleSelection() {
        Array<Selectable> selectables = new Array<>();
        for (Selectable selectable : getPossibleSelection()) {
            if (selectable.actor.isVisible()) {
                if (selectable.actor instanceof Button) {
                    if (!((Button) selectable.actor).isDisabled())
                        selectables.add(selectable);
                } else {
                    selectables.add(selectable);
                }
            }
        }
        return selectables;
    }

    public void selectNextDown() {
        if (getSelected() == null) {
            selectFirst();
        } else {
            Selectable current = getSelected();
            Array<Selectable> candidates = new Array<>();
            for (Selectable selectable : visibleSelection()) {
                if (selectable.xDiff(current) < 0.1 && selectable != current)
                    candidates.add(selectable);
            }
            if (candidates.isEmpty())
                candidates.addAll(visibleSelection());
            Selectable finalOne = null;
            Selectable fallback = null;
            for (Selectable candidate : candidates) {
                if (fallback == null || candidate.getY() > fallback.getY())
                    fallback = candidate;
                if (candidate.getY() < current.getY() && (finalOne == null || current.yDiff(candidate) < current.yDiff(finalOne))) {
                    finalOne = candidate;
                }
            }
            if (finalOne == null)
                for (Selectable candidate : visibleSelection()) {
                    if (candidate.getY() < current.getY() && (finalOne == null || current.yDiff(candidate) < current.yDiff(finalOne))) {
                        finalOne = candidate;
                    }
                }
            if (finalOne != null)
                selectActor(finalOne);
            else if (fallback != null)
                selectActor(fallback);

        }
    }

    private void selectNextLeft() {
        if (getSelected() == null) {
            selectFirst();
        } else {
            Selectable current = getSelected();
            Array<Selectable> candidates = new Array<>();
            for (Selectable selectable : visibleSelection()) {
                if (selectable.yDiff(current) < 0.1 && selectable != current)
                    candidates.add(selectable);
            }
            if (candidates.isEmpty())
                candidates.addAll(visibleSelection());
            Selectable finalOne = null;
            Selectable fallback = null;
            for (Selectable candidate : candidates) {
                if (fallback == null || candidate.getX() > fallback.getX())
                    fallback = candidate;
                if (candidate.getX() < current.getX() && (finalOne == null || current.xDiff(candidate) < current.xDiff(finalOne))) {
                    finalOne = candidate;
                }
            }
            if (finalOne == null)
                for (Selectable candidate : visibleSelection()) {
                    if (candidate.getX() < current.getX() && (finalOne == null || current.xDiff(candidate) < current.xDiff(finalOne))) {
                        finalOne = candidate;
                    }
                }
            if (finalOne != null)
                selectActor(finalOne);
            else if (fallback != null)
                selectActor(fallback);

        }
    }

    private void selectNextRight() {
        if (getSelected() == null) {
            selectFirst();
        } else {
            Selectable current = getSelected();
            Array<Selectable> candidates = new Array<>();
            for (Selectable selectable : visibleSelection()) {
                if (selectable.yDiff(current) < 0.1 && selectable != current)
                    candidates.add(selectable);
            }
            if (candidates.isEmpty())
                candidates.addAll(visibleSelection());
            Selectable finalOne = null;
            Selectable fallback = null;
            for (Selectable candidate : candidates) {
                if (fallback == null || candidate.getX() < fallback.getX())
                    fallback = candidate;
                if (candidate.getX() > current.getX() && (finalOne == null || current.xDiff(candidate) < current.xDiff(finalOne))) {
                    finalOne = candidate;
                }
            }
            if (finalOne == null)
                for (Selectable candidate : visibleSelection()) {
                    if (candidate.getX() > current.getX() && (finalOne == null || current.xDiff(candidate) < current.xDiff(finalOne))) {
                        finalOne = candidate;
                    }
                }
            if (finalOne != null)
                selectActor(finalOne);
            else if (fallback != null)
                selectActor(fallback);

        }
    }


    public void selectNextUp() {
        if (getSelected() == null) {
            selectFirst();
        } else {
            Selectable current = getSelected();
            Array<Selectable> candidates = new Array<>();
            for (Selectable selectable : visibleSelection()) {
                if (selectable.xDiff(current) < 0.1 && selectable != current)
                    candidates.add(selectable);
            }
            if (candidates.isEmpty())
                candidates.addAll(visibleSelection());
            Selectable finalOne = null;
            Selectable fallback = null;
            for (Selectable candidate : candidates) {
                if (fallback == null || candidate.getY() < fallback.getY())
                    fallback = candidate;
                if (candidate.getY() > current.getY() && (finalOne == null || current.yDiff(candidate) < current.yDiff(finalOne))) {
                    finalOne = candidate;
                }
            }
            if (finalOne == null)//allowAllNow
                for (Selectable candidate : visibleSelection()) {
                    if (candidate.getY() > current.getY() && (finalOne == null || current.yDiff(candidate) < current.yDiff(finalOne))) {
                        finalOne = candidate;
                    }
                }
            if (finalOne != null)
                selectActor(finalOne);
            else if (fallback != null)
                selectActor(fallback);

        }
    }

    private void selectFirst() {
        Selectable result = null;
        for (Selectable candidate : getPossibleSelection()) {
            if (result == null || candidate.getY() > result.getY()) {
                result = candidate;
            }
        }
        selectActor(result);
    }

    ScrollPane scrollPaneOfActor(Actor actor) {
        while (actor != null) {
            if (actor.getParent() instanceof ScrollPane) {
                return (ScrollPane) actor.getParent();
            }
            actor = actor.getParent();
        }
        return null;
    }

    public void selectActor(Selectable actor) {
        unselectActors();
        if (actor == null) return;
        stage.setKeyboardFocus(actor.actor);
        ScrollPane scrollPane = scrollPaneOfActor(actor.actor);
        if (scrollPane != null) {
            scrollPane.scrollTo(actor.actor.getX(), actor.actor.getY(), actor.actor.getWidth(), actor.actor.getHeight(), false, false);
        }
        actor.onSelect(this);
    }

    Image screenImage;
    TextureRegion backgroundTexture;

    @Override
    public boolean leave() {
        stage.cancelTouchFocus();
        updateInput();
        return super.leave();
    }

    @Override
    public void enter() {
        if (screenImage != null) {
            //create from lastPreview from header...
            try {
                backgroundTexture = new TextureRegion(Forge.lastPreview);
                backgroundTexture.flip(false, true);
                screenImage.setDrawable(new TextureRegionDrawable(backgroundTexture));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Gdx.input.setInputProcessor(stage);
        updateBG(false);
        super.enter();
    }

    void updateBG(boolean animate) {
        if (Config.instance().getSettingData().dayNightBG) {
            int hour = LocalTime.now().getHour();
            if (animate)
                setTimeOfDay(hour + 3 > 23 ? hour + 3 - 23 : hour + 3);
            switch (hour) {
                case 8:
                case 9:
                    setTargetTime(6f);
                    break;
                case 10:
                case 11:
                    setTargetTime(7f);
                    break;
                case 12:
                case 13:
                    setTargetTime(8f);
                    break;
                case 14:
                case 15:
                    setTargetTime(11f);
                    break;
                case 16:
                case 17:
                    setTargetTime(12f);
                    break;
                case 18:
                case 19:
                    setTargetTime(14f);
                    break;
                case 20:
                case 21:
                    setTargetTime(15f);
                    break;
                case 22:
                case 23:
                    setTargetTime(16f);
                    break;
                case 0:
                case 1:
                    setTargetTime(17f);
                    break;
                case 2:
                case 3:
                    setTargetTime(20f);
                    break;
                case 4:
                case 5:
                    setTargetTime(23f);
                    break;
                case 6:
                case 7:
                    setTargetTime(28f);
                    break;
            }
        }
    }

    public TextureRegion getUIBackground() {
        try {
            Actor a = ui.getChild(0);
            if (a instanceof Image) {
                Drawable d = ((Image) a).getDrawable();
                if (d instanceof TextureRegionDrawable) {
                    return ((TextureRegionDrawable) d).getRegion();
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    @Override
    public void updateInput() {
        super.updateInput();
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }
}
