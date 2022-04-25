package forge.adventure.stage;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import forge.adventure.util.Controls;

public class Console extends Window {
    private final ScrollPane scroll;

    public void toggle() {
        if(isVisible())
        {
            setVisible(false);
            getStage().unfocus(input);
        }
        else
        {
            setVisible(true);
            getStage().setKeyboardFocus(input);
        }
    }

    class InputLine extends TextField
    {

        private Console console;

        public InputLine(Console console) {
            super("", Controls.GetSkin());
            this.console = console;
            writeEnters=true;
        }
        @Override
        protected InputListener createInputListener () {
            TextField self=this;
            return new TextFieldClickListener()
            {
                @Override
                public boolean keyTyped (InputEvent event, char character) {


                    // Disallow "typing" most ASCII control characters, which would show up as a space when onlyFontChars is true.
                    switch (character) {
                        case BACKSPACE:
                            break;
                        case TAB:
                            self.setText(console.complete(self.getText()));
                            self.setCursorPosition(Integer.MAX_VALUE);
                            break;
                        case NEWLINE:
                        case CARRIAGE_RETURN:
                            console.command(self.getText());
                            self.setText("");
                            return false;
                        default:
                            if (character < 32) return false;
                    }
                    return super.keyTyped(event,character);
                }
            };
        }

    }

    private String complete(String text) {
        return interpreter.complete(text);
    }

    public void command(String text) {
        Cell<Label> newLine=content.add(text);
        newLine.getActor().setColor(1,1,1,1);
        newLine.growX().align(Align.left|Align.bottom).row();
        newLine=content.add(interpreter.command(text));
        newLine.getActor().setColor(0.6f,0.6f,0.6f,1);
        newLine.growX().align(Align.left|Align.bottom).row();
        scroll.layout();
        scroll.scrollTo(0, 0, 0, 0);
    }

    private InputLine input;
    private Table content;
    private ConsoleCommandInterpreter interpreter=new ConsoleCommandInterpreter();
    public Console() {
        super("", Controls.GetSkin());
        content=new Table(Controls.GetSkin());
        input=new InputLine(this);
         scroll=new ScrollPane(content,new ScrollPane.ScrollPaneStyle());

        add(scroll).grow().row();
        add(input).growX();
    }
}
