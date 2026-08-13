package forge.adventure.stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import forge.Forge;
import forge.adventure.util.Controls;

public class Console extends Window {
    private final ScrollPane scroll;
    private String last = "";
    private final InputLine input;
    private final Table content;

    public void toggle() {
        if (isVisible()) {
            setVisible(false);
            getStage().unfocus(input);
            Gdx.input.setOnscreenKeyboardVisible(false);
        } else {
            if (!Forge.advFreezePlayerControls) {
                setVisible(true);
                getStage().setKeyboardFocus(input);
            }
        }
    }

    static class InputLine extends TextField {
        private final Console console;
        private final Array<String> commands = new Array<>();
        private int index;
        private final TextField textField;

        public InputLine(Console console) {
            super("", Controls.getSkin());
            this.console = console;
            writeEnters = true;
            textField = this;
        }

        @Override
        protected InputListener createInputListener() {
            return new TextFieldClickListener() {
                @Override
                public boolean keyUp(InputEvent event, int keycode) {
                    switch (keycode) {
                        case Input.Keys.UP:
                            if (!textField.getText().isEmpty()) {
                                index = commands.indexOf(textField.getText(), false) - 1;
                                if (index >= 0 && index < commands.size) {
                                    textField.setText(commands.get(index));
                                    console.last = textField.getText();
                                    index = commands.indexOf(console.last, false);
                                    textField.setCursorPosition(Integer.MAX_VALUE);
                                } else {
                                    index = 0;
                                    textField.setText(commands.get(index));
                                    textField.setCursorPosition(Integer.MAX_VALUE);
                                }
                            } else if (!commands.isEmpty()) {
                                textField.setText(commands.get(commands.size - 1));
                                console.last = textField.getText();
                                index = commands.indexOf(console.last, false);
                                textField.setCursorPosition(Integer.MAX_VALUE);
                            }
                            break;
                        case Input.Keys.DOWN:
                            if (!textField.getText().isEmpty()) {
                                index = commands.indexOf(textField.getText(), false) + 1;
                                if (index >= 0 && index < commands.size) {
                                    textField.setText(commands.get(index));
                                    console.last = textField.getText();
                                    index = commands.indexOf(console.last, false);
                                    textField.setCursorPosition(Integer.MAX_VALUE);
                                } else {
                                    index = commands.size - 1;
                                    textField.setText(commands.get(index));
                                    textField.setCursorPosition(Integer.MAX_VALUE);
                                }
                            } else if (!commands.isEmpty()) {
                                textField.setText(commands.get(0));
                                console.last = textField.getText();
                                index = commands.indexOf(console.last, false);
                                textField.setCursorPosition(Integer.MAX_VALUE);
                            }
                            break;
                        case Input.Keys.LEFT:
                            if (!textField.getText().isEmpty()) {
                                if ((textField.getCursorPosition() - 1) >= 0)
                                    textField.setCursorPosition(textField.getCursorPosition() - 1);
                            }
                            break;
                        case Input.Keys.RIGHT:
                            if (!textField.getText().isEmpty()) {
                                if ((textField.getCursorPosition() + 1) <= textField.getText().length())
                                    textField.setCursorPosition(textField.getCursorPosition() + 1);
                            }
                            break;
                        case Input.Keys.V:
                            if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
                                if (Forge.getClipboard().hasContents()) {
                                    textField.appendText(Forge.getClipboard().getContents());
                                    textField.setCursorPosition(Integer.MAX_VALUE);
                                }
                            }
                        default:
                            break;
                    }
                    return super.keyUp(event, keycode);
                }

                @Override
                public boolean keyTyped(InputEvent event, char character) {
                    // Disallow "typing" most ASCII control characters, which would show up as a space when onlyFontChars is true.
                    switch (character) {
                        case BACKSPACE:
                            break;
                        case TAB:
                            if (textField.getText().isEmpty()) {
                                textField.setText(console.last);
                            } else {
                                textField.setText(console.complete(textField.getText()));
                                textField.setCursorPosition(Integer.MAX_VALUE);
                            }
                            break;
                        case NEWLINE:
                        case CARRIAGE_RETURN:
                            commands.add(textField.getText());
                            console.command(textField.getText());
                            textField.setText("");
                            return false;
                        default:
                            if (character < 32)
                                return false;
                    }
                    return super.keyTyped(event, character);
                }
            };
        }

    }

    private String complete(String text) {
        return ConsoleCommandInterpreter.getInstance().complete(text);
    }

    public void command(String text) {
        if (text.equalsIgnoreCase("exit")) {
            toggle();
            return;
        }
        Cell<Label> newLine = content.add(text);
        newLine.getActor().setColor(1, 1, 1, 1);
        newLine.growX().align(Align.left | Align.bottom).row();
        last = text; //Preserve last command.
        newLine = content.add(ConsoleCommandInterpreter.getInstance().command(text));
        newLine.getActor().setColor(0.6f, 0.6f, 0.6f, 1);
        newLine.growX().align(Align.left | Align.bottom).row();
        scroll.layout();
        scroll.scrollTo(0, 0, 0, 0);
    }

    public Console() {
        super("", Controls.getSkin());
        content = new Table(Controls.getSkin());
        input = new InputLine(this);
        scroll = new ScrollPane(content, new ScrollPane.ScrollPaneStyle());

        add(scroll).grow().row();
        add(input).growX();
    }
}
