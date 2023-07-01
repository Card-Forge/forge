package forge.adventure.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.github.tommyettinger.textra.TextraButton;

public class KeyBoardDialog extends Dialog {




    public interface ScreenKeyboardFinished {
        void handle(String var1);
    }

    private final Label kbLabel;
    public Actor lastInputField;
    public boolean showGamepadSelector = false, lowercaseKey = true;
    private final TextraButton keyA, keyB, keyC, keyD, keyE, keyF, keyG, keyH, keyI, keyJ, keyK, keyL, keyM, keyN, keyO, keyP,
            keyQ, keyR, keyS, keyT, keyU, keyV, keyW, keyX, keyY, keyZ, key1, key2, key3, key4, key5, key6, key7, key8,
            key9, key0, keyDot, keyComma, keyShift, keyBackspace, keySpace, keyOK, keyAbort;


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
    ScreenKeyboardFinished onFinish;
    public void setOnFinish(ScreenKeyboardFinished finish)
    {
        onFinish=finish;
    }
    private String removeLastChar(String s) {
        return (s == null || s.length() == 0)
                ? ""
                : (s.substring(0, s.length() - 1));
    }

    private String transformKey(String c) {
        return lowercaseKey ? c.toLowerCase() : c.toUpperCase();
    }
    public void toggleShiftOrBackspace(boolean shift) { 
    }

    private void setKeyboardDialogText() {
        if (onFinish != null)
            onFinish.handle(kbLabel.getText().toString());

        result(null);
        hide();
    }
    public KeyBoardDialog() {
        super("", Controls.getSkin());


        kbLabel = Controls.newLabel("");
        kbLabel.setAlignment(Align.center);
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
        keyAbort = Controls.newTextButton("Abort", () -> abortKeyInput());
        this.getContentTable().add(kbLabel).width(220).height(20).colspan(10).expandX().align(Align.center);
        this.getButtonTable().row();
        this.getButtonTable().add(key1).width(20).height(20);
        this.getButtonTable().add(key2).width(20).height(20);
        this.getButtonTable().add(key3).width(20).height(20);
        this.getButtonTable().add(key4).width(20).height(20);
        this.getButtonTable().add(key5).width(20).height(20);
        this.getButtonTable().add(key6).width(20).height(20);
        this.getButtonTable().add(key7).width(20).height(20);
        this.getButtonTable().add(key8).width(20).height(20);
        this.getButtonTable().add(key9).width(20).height(20);
        this.getButtonTable().add(key0).width(20).height(20);
        this.getButtonTable().row();
        this.getButtonTable().add(keyQ).width(20).height(20);
        this.getButtonTable().add(keyW).width(20).height(20);
        this.getButtonTable().add(keyE).width(20).height(20);
        this.getButtonTable().add(keyR).width(20).height(20);
        this.getButtonTable().add(keyT).width(20).height(20);
        this.getButtonTable().add(keyY).width(20).height(20);
        this.getButtonTable().add(keyU).width(20).height(20);
        this.getButtonTable().add(keyI).width(20).height(20);
        this.getButtonTable().add(keyO).width(20).height(20);
        this.getButtonTable().add(keyP).width(20).height(20);
        this.getButtonTable().row();
        this.getButtonTable().add(keyA).width(20).height(20);
        this.getButtonTable().add(keyS).width(20).height(20);
        this.getButtonTable().add(keyD).width(20).height(20);
        this.getButtonTable().add(keyF).width(20).height(20);
        this.getButtonTable().add(keyG).width(20).height(20);
        this.getButtonTable().add(keyH).width(20).height(20);
        this.getButtonTable().add(keyJ).width(20).height(20);
        this.getButtonTable().add(keyK).width(20).height(20);
        this.getButtonTable().add(keyL).width(20).height(20);
        this.getButtonTable().add(keyBackspace).width(20).height(20);
        this.getButtonTable().row();
        this.getButtonTable().add(keyShift).width(20).height(20);
        this.getButtonTable().add(keyZ).width(20).height(20);
        this.getButtonTable().add(keyX).width(20).height(20);
        this.getButtonTable().add(keyC).width(20).height(20);
        this.getButtonTable().add(keyV).width(20).height(20);
        this.getButtonTable().add(keyB).width(20).height(20);
        this.getButtonTable().add(keyN).width(20).height(20);
        this.getButtonTable().add(keyM).width(20).height(20);
        this.getButtonTable().add(keyDot).width(20).height(20);
        this.getButtonTable().add(keyComma).width(20).height(20);
        this.getButtonTable().row();
        this.getButtonTable().add(keySpace).width(150).height(20).colspan(6);
        this.getButtonTable().add(keyOK).width(50).height(20).colspan(2);
        this.getButtonTable().add(keyAbort).width(50).height(20).colspan(2);
        this.setMovable(false);
        this.setKeepWithinStage(true);
        this.setResizable(false);
    }

    private void abortKeyInput() {
        hide();
        result(null);
    }

    public void setText(String text) {
        kbLabel.setText(text);
    }
}
