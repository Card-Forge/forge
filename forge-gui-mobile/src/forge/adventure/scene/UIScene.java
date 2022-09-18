package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.github.tommyettinger.textra.TextraButton;
import forge.Forge;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.adventure.util.Selector;
import forge.adventure.util.UIActor;

/**
 * Base class for an GUI scene where the elements are loaded from a json file
 */
public class UIScene extends Scene {
    protected UIActor ui;
    Stage stage;

    String uiFile;
    private Dialog keyboardDialog;
    private Label kbLabel;
    private TextraButton keyA, keyB, keyC, keyD, keyE, keyF, keyG, keyH, keyI, keyJ, keyK, keyL, keyM, keyN, keyO, keyP,
            keyQ, keyR, keyS, keyT, keyU, keyV, keyW, keyX, keyY, keyZ, key1, key2, key3, key4, key5, key6, key7, key8,
            key9, key0, keyDot, keyComma, keyShift, keyBackspace, keySpace, keyOK;
    public Actor lastInputField;
    public boolean showGamepadSelector = false, lowercaseKey = true, kbVisible=false;

    public InputEvent eventEnter, eventExit, eventTouchDown, eventTouchUp;

    public Vector2 pointer = new Vector2();
    public ObjectMap<Integer, Actor> actorObjectMap;
    public Actor selectedActor, selectedKey, lastSelectedKey;
    public int selectedActorIndex = 0;

    public UIScene(String uiFilePath) {

        uiFile = uiFilePath;
        stage = new Stage(new ScalingViewport(Scaling.stretch, getIntendedWidth(), getIntendedHeight())) {
            @Override
            public boolean keyUp(int keycode) {
                if (Forge.hasGamepad())
                    return false;
                return keyPressed(keycode);
            }
            @Override
            public boolean keyDown(int keyCode) {
                if (!Forge.hasGamepad())
                    return false;
                return keyPressed(keyCode);
            }
            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                pointerMoved(screenX, screenY);
                return super.mouseMoved(screenX, screenY);
            }
        };
        ui = new UIActor(Config.instance().getFile(uiFile));
        screenImage = ui.findActor("lastScreen");
        stage.addActor(ui);

        eventTouchDown = new InputEvent();
        eventTouchDown.setPointer(-1);
        eventTouchDown.setType(InputEvent.Type.touchDown);
        eventTouchUp = new InputEvent();
        eventTouchUp.setPointer(-1);
        eventTouchUp.setType(InputEvent.Type.touchUp);
        eventEnter = new InputEvent();
        eventEnter.setPointer(-1);
        eventEnter.setType(InputEvent.Type.enter);
        eventExit = new InputEvent();
        eventExit.setPointer(-1);
        eventExit.setType(InputEvent.Type.exit);
        actorObjectMap = new ObjectMap<>();
        keyboardDialog = Controls.newDialog("");
        kbLabel = Controls.newLabel("");
        kbLabel.setAlignment(Align.center);
        kbLabel.setColor(Color.CYAN);
        kbLabel.setFontScale(1.5f,1.5f);
        keyA = Controls.newTextButton("A", () -> kbLabel.setText(kbLabel.getText()+transformKey("A")));
        keyB = Controls.newTextButton("B", () -> kbLabel.setText(kbLabel.getText()+transformKey("B")));
        keyC = Controls.newTextButton("C", () -> kbLabel.setText(kbLabel.getText()+transformKey("C")));
        keyD = Controls.newTextButton("D", () -> kbLabel.setText(kbLabel.getText()+transformKey("D")));
        keyE = Controls.newTextButton("E", () -> kbLabel.setText(kbLabel.getText()+transformKey("E")));
        keyF = Controls.newTextButton("F", () -> kbLabel.setText(kbLabel.getText()+transformKey("F")));
        keyG = Controls.newTextButton("G", () -> kbLabel.setText(kbLabel.getText()+transformKey("G")));
        keyH = Controls.newTextButton("H", () -> kbLabel.setText(kbLabel.getText()+transformKey("H")));
        keyI = Controls.newTextButton("I", () -> kbLabel.setText(kbLabel.getText()+transformKey("I")));
        keyJ = Controls.newTextButton("J", () -> kbLabel.setText(kbLabel.getText()+transformKey("J")));
        keyK = Controls.newTextButton("K", () -> kbLabel.setText(kbLabel.getText()+transformKey("K")));
        keyL = Controls.newTextButton("L", () -> kbLabel.setText(kbLabel.getText()+transformKey("L")));
        keyM = Controls.newTextButton("M", () -> kbLabel.setText(kbLabel.getText()+transformKey("M")));
        keyN = Controls.newTextButton("N", () -> kbLabel.setText(kbLabel.getText()+transformKey("N")));
        keyO = Controls.newTextButton("O", () -> kbLabel.setText(kbLabel.getText()+transformKey("O")));
        keyP = Controls.newTextButton("P", () -> kbLabel.setText(kbLabel.getText()+transformKey("P")));
        keyQ = Controls.newTextButton("Q", () -> kbLabel.setText(kbLabel.getText()+transformKey("Q")));
        keyR = Controls.newTextButton("R", () -> kbLabel.setText(kbLabel.getText()+transformKey("R")));
        keyS = Controls.newTextButton("S", () -> kbLabel.setText(kbLabel.getText()+transformKey("S")));
        keyT = Controls.newTextButton("T", () -> kbLabel.setText(kbLabel.getText()+transformKey("T")));
        keyU = Controls.newTextButton("U", () -> kbLabel.setText(kbLabel.getText()+transformKey("U")));
        keyV = Controls.newTextButton("V", () -> kbLabel.setText(kbLabel.getText()+transformKey("V")));
        keyW = Controls.newTextButton("W", () -> kbLabel.setText(kbLabel.getText()+transformKey("W")));
        keyX = Controls.newTextButton("X", () -> kbLabel.setText(kbLabel.getText()+transformKey("X")));
        keyY = Controls.newTextButton("Y", () -> kbLabel.setText(kbLabel.getText()+transformKey("Y")));
        keyZ = Controls.newTextButton("Z", () -> kbLabel.setText(kbLabel.getText()+transformKey("Z")));
        key1 = Controls.newTextButton("1", () -> kbLabel.setText(kbLabel.getText()+"1"));
        key2 = Controls.newTextButton("2", () -> kbLabel.setText(kbLabel.getText()+"2"));
        key3 = Controls.newTextButton("3", () -> kbLabel.setText(kbLabel.getText()+"3"));
        key4 = Controls.newTextButton("4", () -> kbLabel.setText(kbLabel.getText()+"4"));
        key5 = Controls.newTextButton("5", () -> kbLabel.setText(kbLabel.getText()+"5"));
        key6 = Controls.newTextButton("6", () -> kbLabel.setText(kbLabel.getText()+"6"));
        key7 = Controls.newTextButton("7", () -> kbLabel.setText(kbLabel.getText()+"7"));
        key8 = Controls.newTextButton("8", () -> kbLabel.setText(kbLabel.getText()+"8"));
        key9 = Controls.newTextButton("9", () -> kbLabel.setText(kbLabel.getText()+"9"));
        key0 = Controls.newTextButton("0", () -> kbLabel.setText(kbLabel.getText()+"0"));
        keyDot = Controls.newTextButton(".", () -> kbLabel.setText(kbLabel.getText()+"."));
        keyComma = Controls.newTextButton(",", () -> kbLabel.setText(kbLabel.getText()+","));
        keyShift = Controls.newTextButton("Aa", () -> shiftKey());
        keyBackspace = Controls.newTextButton("<<", () -> kbLabel.setText(removeLastChar(String.valueOf(kbLabel.getText()))));
        keySpace = Controls.newTextButton("SPACE", () -> kbLabel.setText(kbLabel.getText()+" "));
        keyOK = Controls.newTextButton("OK", () -> setKeyboardDialogText());
        keyboardDialog.getContentTable().add(kbLabel).width(220).height(20).colspan(10).expandX().align(Align.center);
        keyboardDialog.getButtonTable().row();
        keyboardDialog.getButtonTable().add(key1).width(20).height(20);
        keyboardDialog.getButtonTable().add(key2).width(20).height(20);
        keyboardDialog.getButtonTable().add(key3).width(20).height(20);
        keyboardDialog.getButtonTable().add(key4).width(20).height(20);
        keyboardDialog.getButtonTable().add(key5).width(20).height(20);
        keyboardDialog.getButtonTable().add(key6).width(20).height(20);
        keyboardDialog.getButtonTable().add(key7).width(20).height(20);
        keyboardDialog.getButtonTable().add(key8).width(20).height(20);
        keyboardDialog.getButtonTable().add(key9).width(20).height(20);
        keyboardDialog.getButtonTable().add(key0).width(20).height(20);
        keyboardDialog.getButtonTable().row();
        keyboardDialog.getButtonTable().add(keyQ).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyW).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyE).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyR).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyT).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyY).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyU).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyI).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyO).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyP).width(20).height(20);
        keyboardDialog.getButtonTable().row();
        keyboardDialog.getButtonTable().add(keyA).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyS).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyD).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyF).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyG).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyH).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyJ).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyK).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyL).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyBackspace).width(20).height(20);
        keyboardDialog.getButtonTable().row();
        keyboardDialog.getButtonTable().add(keyShift).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyZ).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyX).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyC).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyV).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyB).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyN).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyM).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyDot).width(20).height(20);
        keyboardDialog.getButtonTable().add(keyComma).width(20).height(20);
        keyboardDialog.getButtonTable().row();
        keyboardDialog.getButtonTable().add(keySpace).width(150).height(20).colspan(6);
        keyboardDialog.getButtonTable().add(keyOK).width(100).height(20).colspan(4);
        keyboardDialog.setKeepWithinStage(true);
        keyboardDialog.setResizable(false);
    }

    @Override
    public void dispose() {
        if (stage != null)
            stage.dispose();
    }

    @Override
    public void act(float delta) {
        stage.act(delta);
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

    public boolean keyPressed(int keycode) {
        return true;
    }

    public boolean pointerMoved(int screenX, int screenY) {
        return false;
    }

    public void performTouch(Actor actor) {
        if (actor == null)
            return;
        actor.fire(eventTouchDown);
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                actor.fire(eventTouchUp);
            }
        }, 0.10f);
    }

    private String transformKey(String c) {
        return lowercaseKey ? c.toLowerCase() : c.toUpperCase();
    }
    public void toggleShiftOrBackspace(boolean shift) {
        if (shift)
            performTouch(keyShift);
        else
            performTouch(keyBackspace);
    }
    public void hideOnScreenKeyboard() {
        kbVisible = false;
        keyboardDialog.hide();
        selectedActor = lastInputField;
    }
    private void shiftKey() {
        lowercaseKey = !lowercaseKey;
        keyShift.setColor(lowercaseKey ? Color.WHITE : Color.CYAN);
        keyA.setText(lowercaseKey ? "a" : "A");
        keyB.setText(lowercaseKey ? "b" : "B");
        keyC.setText(lowercaseKey ? "c" : "C");
        keyD.setText(lowercaseKey ? "d" : "D");
        keyE.setText(lowercaseKey ? "e" : "E");
        keyF.setText(lowercaseKey ? "f" : "F");
        keyG.setText(lowercaseKey ? "g" : "G");
        keyH.setText(lowercaseKey ? "h" : "H");
        keyI.setText(lowercaseKey ? "i" : "I");
        keyJ.setText(lowercaseKey ? "j" : "J");
        keyK.setText(lowercaseKey ? "k" : "K");
        keyL.setText(lowercaseKey ? "l" : "L");
        keyM.setText(lowercaseKey ? "m" : "M");
        keyN.setText(lowercaseKey ? "n" : "N");
        keyO.setText(lowercaseKey ? "o" : "O");
        keyP.setText(lowercaseKey ? "p" : "P");
        keyQ.setText(lowercaseKey ? "q" : "Q");
        keyR.setText(lowercaseKey ? "r" : "R");
        keyS.setText(lowercaseKey ? "s" : "S");
        keyT.setText(lowercaseKey ? "t" : "T");
        keyU.setText(lowercaseKey ? "u" : "U");
        keyV.setText(lowercaseKey ? "v" : "V");
        keyW.setText(lowercaseKey ? "w" : "W");
        keyX.setText(lowercaseKey ? "x" : "X");
        keyY.setText(lowercaseKey ? "y" : "Y");
        keyZ.setText(lowercaseKey ? "z" : "Z");
    }
    public void setSelectedKey(int keyCode) {
        switch(keyCode) {
            case Input.Keys.DPAD_DOWN:
                if (selectedKey == null)
                    selectActor(key1, false);
                else if (selectedKey == key1)
                    selectActor(keyQ, false);
                else if (selectedKey == keyQ)
                    selectActor(keyA, false);
                else if (selectedKey == keyA)
                    selectActor(keyShift, false);
                else if (selectedKey == keyShift)
                    selectActor(keySpace, false);
                else if (selectedKey == key2)
                    selectActor(keyW, false);
                else if (selectedKey == keyW)
                    selectActor(keyS, false);
                else if (selectedKey == keyS)
                    selectActor(keyZ, false);
                else if (selectedKey == keyZ)
                    selectActor(keySpace, false);
                else if (selectedKey == key3)
                    selectActor(keyE, false);
                else if (selectedKey == keyE)
                    selectActor(keyD, false);
                else if (selectedKey == keyD)
                    selectActor(keyX, false);
                else if (selectedKey == keyX)
                    selectActor(keySpace, false);
                else if (selectedKey == key4)
                    selectActor(keyR, false);
                else if (selectedKey == keyR)
                    selectActor(keyF, false);
                else if (selectedKey == keyF)
                    selectActor(keyC, false);
                else if (selectedKey == keyC)
                    selectActor(keySpace, false);
                else if (selectedKey == key5)
                    selectActor(keyT, false);
                else if (selectedKey == keyT)
                    selectActor(keyG, false);
                else if (selectedKey == keyG)
                    selectActor(keyV, false);
                else if (selectedKey == keyV)
                    selectActor(keySpace, false);
                else if (selectedKey == key6)
                    selectActor(keyY, false);
                else if (selectedKey == keyY)
                    selectActor(keyH, false);
                else if (selectedKey == keyH)
                    selectActor(keyB, false);
                else if (selectedKey == keyB)
                    selectActor(keySpace, false);
                else if (selectedKey == key7)
                    selectActor(keyU, false);
                else if (selectedKey == keyU)
                    selectActor(keyJ, false);
                else if (selectedKey == keyJ)
                    selectActor(keyN, false);
                else if (selectedKey == keyN)
                    selectActor(keyOK, false);
                else if (selectedKey == key8)
                    selectActor(keyI, false);
                else if (selectedKey == keyI)
                    selectActor(keyK, false);
                else if (selectedKey == keyK)
                    selectActor(keyM, false);
                else if (selectedKey == keyM)
                    selectActor(keyOK, false);
                else if (selectedKey == key9)
                    selectActor(keyO, false);
                else if (selectedKey == keyO)
                    selectActor(keyL, false);
                else if (selectedKey == keyL)
                    selectActor(keyDot, false);
                else if (selectedKey == keyDot)
                    selectActor(keyOK, false);
                else if (selectedKey == key0)
                    selectActor(keyP, false);
                else if (selectedKey == keyP)
                    selectActor(keyBackspace, false);
                else if (selectedKey == keyBackspace)
                    selectActor(keyComma, false);
                else if (selectedKey == keyComma)
                    selectActor(keyOK, false);
                break;
            case Input.Keys.DPAD_UP:
                if (selectedKey == null)
                    selectActor(key1, false);
                else if (selectedKey == keySpace || selectedKey == keyOK)
                    selectActor(lastSelectedKey, false);
                else if (selectedKey == keyShift)
                    selectActor(keyA, false);
                else if (selectedKey == keyA)
                    selectActor(keyQ, false);
                else if (selectedKey == keyQ)
                    selectActor(key1, false);
                else if (selectedKey == keyZ)
                    selectActor(keyS, false);
                else if (selectedKey == keyS)
                    selectActor(keyW, false);
                else if (selectedKey == keyW)
                    selectActor(key2, false);
                else if (selectedKey == keyX)
                    selectActor(keyD, false);
                else if (selectedKey == keyD)
                    selectActor(keyE, false);
                else if (selectedKey == keyE)
                    selectActor(key3, false);
                else if (selectedKey == keyC)
                    selectActor(keyF, false);
                else if (selectedKey == keyF)
                    selectActor(keyR, false);
                else if (selectedKey == keyR)
                    selectActor(key4, false);
                else if (selectedKey == keyV)
                    selectActor(keyG, false);
                else if (selectedKey == keyG)
                    selectActor(keyT, false);
                else if (selectedKey == keyT)
                    selectActor(key5, false);
                else if (selectedKey == keyB)
                    selectActor(keyH, false);
                else if (selectedKey == keyH)
                    selectActor(keyY, false);
                else if (selectedKey == keyY)
                    selectActor(key6, false);
                else if (selectedKey == keyN)
                    selectActor(keyJ, false);
                else if (selectedKey == keyJ)
                    selectActor(keyU, false);
                else if (selectedKey == keyU)
                    selectActor(key7, false);
                else if (selectedKey == keyM)
                    selectActor(keyK, false);
                else if (selectedKey == keyK)
                    selectActor(keyI, false);
                else if (selectedKey == keyI)
                    selectActor(key8, false);
                else if (selectedKey == keyDot)
                    selectActor(keyL, false);
                else if (selectedKey == keyL)
                    selectActor(keyO, false);
                else if (selectedKey == keyO)
                    selectActor(key9, false);
                else if (selectedKey == keyComma)
                    selectActor(keyBackspace, false);
                else if (selectedKey == keyBackspace)
                    selectActor(keyP, false);
                else if (selectedKey == keyP)
                    selectActor(key0, false);
                break;
            case Input.Keys.DPAD_LEFT:
                if (selectedKey == null)
                    selectActor(key1, false);
                else if (selectedKey == keyOK)
                    selectActor(keySpace, false);
                else if (selectedKey == keyComma)
                    selectActor(keyDot, false);
                else if (selectedKey == keyDot)
                    selectActor(keyM, false);
                else if (selectedKey == keyM)
                    selectActor(keyN, false);
                else if (selectedKey == keyN)
                    selectActor(keyB, false);
                else if (selectedKey == keyB)
                    selectActor(keyV, false);
                else if (selectedKey == keyV)
                    selectActor(keyC, false);
                else if (selectedKey == keyC)
                    selectActor(keyX, false);
                else if (selectedKey == keyX)
                    selectActor(keyZ, false);
                else if (selectedKey == keyZ)
                    selectActor(keyShift, false);
                else if (selectedKey == keyBackspace)
                    selectActor(keyL, false);
                else if (selectedKey == keyL)
                    selectActor(keyK, false);
                else if (selectedKey == keyK)
                    selectActor(keyJ, false);
                else if (selectedKey == keyJ)
                    selectActor(keyH, false);
                else if (selectedKey == keyH)
                    selectActor(keyG, false);
                else if (selectedKey == keyG)
                    selectActor(keyF, false);
                else if (selectedKey == keyF)
                    selectActor(keyD, false);
                else if (selectedKey == keyD)
                    selectActor(keyS, false);
                else if (selectedKey == keyS)
                    selectActor(keyA, false);
                else if (selectedKey == keyP)
                    selectActor(keyO, false);
                else if (selectedKey == keyO)
                    selectActor(keyI, false);
                else if (selectedKey == keyI)
                    selectActor(keyU, false);
                else if (selectedKey == keyU)
                    selectActor(keyY, false);
                else if (selectedKey == keyY)
                    selectActor(keyT, false);
                else if (selectedKey == keyT)
                    selectActor(keyR, false);
                else if (selectedKey == keyR)
                    selectActor(keyE, false);
                else if (selectedKey == keyE)
                    selectActor(keyW, false);
                else if (selectedKey == keyW)
                    selectActor(keyQ, false);
                else if (selectedKey == key0)
                    selectActor(key9, false);
                else if (selectedKey == key9)
                    selectActor(key8, false);
                else if (selectedKey == key8)
                    selectActor(key7, false);
                else if (selectedKey == key7)
                    selectActor(key6, false);
                else if (selectedKey == key6)
                    selectActor(key5, false);
                else if (selectedKey == key5)
                    selectActor(key4, false);
                else if (selectedKey == key4)
                    selectActor(key3, false);
                else if (selectedKey == key3)
                    selectActor(key2, false);
                else if (selectedKey == key2)
                    selectActor(key1, false);
                break;
            case Input.Keys.DPAD_RIGHT:
                if (selectedKey == null)
                    selectActor(key1, false);
                else if (selectedKey == key1)
                    selectActor(key2, false);
                else if (selectedKey == key2)
                    selectActor(key3, false);
                else if (selectedKey == key3)
                    selectActor(key4, false);
                else if (selectedKey == key4)
                    selectActor(key5, false);
                else if (selectedKey == key5)
                    selectActor(key6, false);
                else if (selectedKey == key6)
                    selectActor(key7, false);
                else if (selectedKey == key7)
                    selectActor(key8, false);
                else if (selectedKey == key8)
                    selectActor(key9, false);
                else if (selectedKey == key9)
                    selectActor(key0, false);
                else if (selectedKey == keyQ)
                    selectActor(keyW, false);
                else if (selectedKey == keyW)
                    selectActor(keyE, false);
                else if (selectedKey == keyE)
                    selectActor(keyR, false);
                else if (selectedKey == keyR)
                    selectActor(keyT, false);
                else if (selectedKey == keyT)
                    selectActor(keyY, false);
                else if (selectedKey == keyY)
                    selectActor(keyU, false);
                else if (selectedKey == keyU)
                    selectActor(keyI, false);
                else if (selectedKey == keyI)
                    selectActor(keyO, false);
                else if (selectedKey == keyO)
                    selectActor(keyP, false);
                else if (selectedKey == keyA)
                    selectActor(keyS, false);
                else if (selectedKey == keyS)
                    selectActor(keyD, false);
                else if (selectedKey == keyD)
                    selectActor(keyF, false);
                else if (selectedKey == keyF)
                    selectActor(keyG, false);
                else if (selectedKey == keyG)
                    selectActor(keyH, false);
                else if (selectedKey == keyH)
                    selectActor(keyJ, false);
                else if (selectedKey == keyJ)
                    selectActor(keyK, false);
                else if (selectedKey == keyK)
                    selectActor(keyL, false);
                else if (selectedKey == keyL)
                    selectActor(keyBackspace, false);
                else if (selectedKey == keyShift)
                    selectActor(keyZ, false);
                else if (selectedKey == keyZ)
                    selectActor(keyX, false);
                else if (selectedKey == keyX)
                    selectActor(keyC, false);
                else if (selectedKey == keyC)
                    selectActor(keyV, false);
                else if (selectedKey == keyV)
                    selectActor(keyB, false);
                else if (selectedKey == keyB)
                    selectActor(keyN, false);
                else if (selectedKey == keyN)
                    selectActor(keyM, false);
                else if (selectedKey == keyM)
                    selectActor(keyDot, false);
                else if (selectedKey == keyDot)
                    selectActor(keyComma, false);
                else if (selectedKey == keySpace)
                    selectActor(keyOK, false);
                break;
            default:
                break;
        }
    }
    private String removeLastChar(String s) {
        return (s == null || s.length() == 0)
                ? ""
                : (s.substring(0, s.length() - 1));
    }
    private void setKeyboardDialogText() {
        if (lastInputField == null)
            return;
        if (lastInputField instanceof TextField) {
            if (!kbLabel.getText().isEmpty()) {
                ((TextField) lastInputField).setText(String.valueOf(kbLabel.getText()));
                kbLabel.setText("");
            }
        }
        kbVisible = false;
        keyboardDialog.hide();
        selectActor(lastInputField, false);
    }
    public void showOnScreenKeyboard(String text) {
        kbVisible = true;
        if (lowercaseKey)
            shiftKey();
        kbLabel.setText(text);
        keyboardDialog.show(stage);
        selectActor(key1, false);
    }
    public void keyOK() {
        selectActor(keyOK, false);
    }

    public void clearActorObjects() {
        actorObjectMap.clear();
        selectedActor = null;
    }
    public void addActorObject(Actor actor) {
        int index = actorObjectMap.size;
        actorObjectMap.put(index, actor);
    }
    public void unselectActors() {
        if (actorObjectMap.isEmpty())
            return;
        for (Actor actor : actorObjectMap.values()) {
            if (actor != null) {
                if (actor instanceof TextButton)
                    ((TextButton) actor).fire(eventExit);
                else if (actor instanceof Selector)
                    ((Selector) actor).fire(eventExit);
                else if (actor instanceof TextField) {
                    if (stage.getKeyboardFocus() == actor)
                        stage.setKeyboardFocus(null);
                } else if (actor instanceof ImageButton) {
                    ((ImageButton) actor).setChecked(false);
                } else {
                    actor.fire(eventExit);
                }
            }
        }
    }
    public void selectNextActor(boolean press) {
        if (actorObjectMap.isEmpty())
            return;
        if (selectedActor == null) {
            selectActor(actorObjectMap.get(0), press);
        } else {
            selectActor(actorObjectMap.get(selectedActorIndex+1), press);
        }
    }
    public void selectPreviousActor(boolean press) {
        if (actorObjectMap.isEmpty())
            return;
        if (selectedActor == null) {
            selectActor(actorObjectMap.get(0), press);
        } else {
            selectActor(actorObjectMap.get(selectedActorIndex-1), press);
        }
    }
    public void selectActor(Actor actor, boolean press) {
        if (actor == null)
            return;
        if (kbVisible) {
            if (selectedKey != null) {
                selectedKey.fire(eventExit);
                if (selectedKey instanceof TextraButton)
                    if (!(selectedKey == keyOK || selectedKey == keySpace))
                        lastSelectedKey = selectedKey;
            }
            selectedKey = actor;
            selectedKey.fire(eventEnter);
            if (press)
                performTouch(selectedKey);
        } else {
            unselectActors();
            if (actorObjectMap.isEmpty())
                return;
            Integer key = actorObjectMap.findKey(actor, true);
            if (key == null)
                return;
            Actor a = actorObjectMap.get(key);
            if (a != null) {
                if (a instanceof TextraButton)
                    a.fire(eventEnter);
                else if (a instanceof Selector)
                    ((Selector) a).fire(eventEnter);
                else if (a instanceof TextField) {
                    stage.setKeyboardFocus(a);
                } else if (a instanceof ImageButton) {
                    ((ImageButton) a).setChecked(true);
                }
                selectedActor = a;
                selectedActorIndex = key;
                if (press)
                    performTouch(a);
            }
        }
    }
    public void selectCurrent() {
        Actor current = actorObjectMap.get(selectedActorIndex);
        if (current == null)
            current = actorObjectMap.get(0);
        selectActor(current, false);
    }
    public void updateHovered() {
        for (Actor a : actorObjectMap.values()) {
            if (a != null && Controls.actorContainsVector(a, pointer)) {
                selectActor(a, false);
                break;
            }
        }
    }

    Image screenImage;
    TextureRegion backgroundTexture;

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
        Gdx.input.setInputProcessor(stage); //Start taking input from the ui
        super.enter();
    }

    @Override
    public boolean leave() {
        clearActorObjects();
        selectedActor = null;
        return super.leave();
    }
}
