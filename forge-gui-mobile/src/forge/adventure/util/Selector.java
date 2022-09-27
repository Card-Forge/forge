package forge.adventure.util;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.github.tommyettinger.textra.TextraButton;

/**
 * UI element to click through options, can be configured in an UiActor
 */
public class Selector extends Table {
    private final ImageButton leftArrow;
    private final ImageButton rightArrow;
    private final TextraButton label;
    private int currentIndex = 0;
    private Array<String> textList;


    public Selector() {
        Selector self=this;
        ImageButton.ImageButtonStyle leftArrowStyle = Controls.getSkin().get("leftarrow", ImageButton.ImageButtonStyle.class);
        leftArrow = new ImageButton(leftArrowStyle)
        {
            @Override
            public boolean hasKeyboardFocus()
            {
                return  self.hasKeyboardFocus();
            }
        };

        ImageButton.ImageButtonStyle rightArrowStyle = Controls.getSkin().get("rightarrow", ImageButton.ImageButtonStyle.class);
        rightArrow = new ImageButton(rightArrowStyle)
        {
            @Override
            public boolean hasKeyboardFocus()
            {
                return  self.hasKeyboardFocus();
            }
        };

        label = new Controls.TextButtonFix("")
        {
            @Override
            public boolean hasKeyboardFocus()
            {
                return  self.hasKeyboardFocus();
            }
        };
        add(leftArrow).pad(2);
        add(label).expand().fill();
        add(rightArrow).pad(2);
        leftArrow.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                setCurrentIndex(currentIndex - 1);
            }
        });
        rightArrow.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                setCurrentIndex(currentIndex + 1);
            }
        });

        addListener(new InputListener()
        {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if(KeyBinding.Left.isPressed(keycode))
                    setCurrentIndex(currentIndex - 1);
                if(KeyBinding.Right.isPressed(keycode))
                    setCurrentIndex(currentIndex + 1);
                return true;
            }
        });
    }

    @Override
    protected void sizeChanged() {
        super.sizeChanged();

        leftArrow.setWidth(getHeight());
        leftArrow.setHeight(getHeight());
        rightArrow.setWidth(getHeight());
        rightArrow.setHeight(getHeight());
        label.setHeight(getHeight());
        label.setX(getHeight());
        label.setWidth(getWidth() - (getHeight() * 2));
        rightArrow.setX(getWidth() - getHeight());
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        currentIndex %= textList.size;
        if (currentIndex < 0) {
            currentIndex += textList.size;
        }
        int oldIndex = currentIndex;
        this.currentIndex = currentIndex;
        label.setText(textList.get(currentIndex));
        label.layout();
        ChangeListener.ChangeEvent changeEvent = Pools.obtain(ChangeListener.ChangeEvent.class);
        if (fire(changeEvent)) {
            this.currentIndex = oldIndex;
            label.setText(textList.get(currentIndex));
            label.layout();
        }
        Pools.free(changeEvent);
    }

    public String getText() {
        return textList.get(currentIndex);
    }
    public TextraButton getLabel() {
        return label;
    }
    public ImageButton getLeftArrow() {
        return leftArrow;
    }
    public ImageButton getRightArrow() {
        return rightArrow;
    }

    public Array<String> getTextList() {
        return textList;
    }

    public void setTextList(Array<String> textList) {
        this.textList = textList;
        setCurrentIndex(currentIndex);
    }

    public void setTextList(String[] strings) {
        this.textList = new Array<>(strings);
        setCurrentIndex(currentIndex);
    }

}
