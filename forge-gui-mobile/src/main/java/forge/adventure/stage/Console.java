package forge.adventure.stage;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.adventure.util.Controls;

public class Console extends Window {
    private final ScrollPane scroll;
    private  String last = "";
    private final InputLine input;
    private final Table content;

    public void toggle() {
        if(isVisible()) {
            setVisible(false);
            getStage().unfocus(input);
        } else {
            if (!Forge.advFreezePlayerControls) {
                setVisible(true);
                getStage().setKeyboardFocus(input);
            }
        }
    }

    static class InputLine extends TextField {
        private final Console console;

        public InputLine(Console console) {
            super("", Controls.getSkin());
            this.console = console;
            writeEnters=true;
        }

        @Override
        protected InputListener createInputListener () {
            TextField self = this;
            return new TextFieldClickListener()
            {
                @Override
                public boolean keyTyped (InputEvent event, char character) {
                // Disallow "typing" most ASCII control characters, which would show up as a space when onlyFontChars is true.
                switch (character) {
                    case BACKSPACE:
                        break;
                    case TAB:
                        if(self.getText().isEmpty())
                        {
                            self.setText(console.last);
                        }
                        else
                        {
                            self.setText(console.complete(self.getText()));
                            self.setCursorPosition(Integer.MAX_VALUE);
                        }
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
        return ConsoleCommandInterpreter.getInstance().complete(text);
    }

    public void command(String text) {
        Cell<Label> newLine=content.add(text);
        newLine.getActor().setColor(1,1,1,1);
        newLine.growX().align(Align.left|Align.bottom).row();
        last = text; //Preserve last command.
        newLine=content.add(ConsoleCommandInterpreter.getInstance().command(text));
        newLine.getActor().setColor(0.6f,0.6f,0.6f,1);
        newLine.growX().align(Align.left|Align.bottom).row();
        scroll.layout();
        scroll.scrollTo(0, 0, 0, 0);
    }

    public Console() {
        super("", Controls.getSkin());
        content = new Table(Controls.getSkin());
        input   = new InputLine(this);
        scroll  = new ScrollPane(content,new ScrollPane.ScrollPaneStyle());

        add(scroll).grow().row();
        add(input).growX();
    }
}
