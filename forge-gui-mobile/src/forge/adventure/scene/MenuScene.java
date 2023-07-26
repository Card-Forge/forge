package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TypingAdapter;
import com.github.tommyettinger.textra.TypingLabel;
import forge.Forge;
import forge.adventure.data.DialogData;
import forge.adventure.data.RewardData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;
import forge.adventure.util.Reward;
import forge.card.ColorSet;
import forge.util.Localizer;

import java.util.ArrayList;

/**
 * MenuScene
 * Superclass for menu scenes which do not have HUD but need dialog functionality
 */
public class MenuScene extends UIScene {
    protected final Dialog dialog;
    private final Array<TextraButton> dialogButtonMap = new Array<>();
    protected java.util.List<ChangeListener> dialogCompleteList = new ArrayList<>();

    public MenuScene(String uiFilePath) {
        super(uiFilePath);
        dialog = Controls.newDialog("");
    }

    public Dialog getDialog() {
        return dialog;
    }

    public boolean activate(Array<DialogData> data) { //Method for actors to show their dialogues.
        boolean dialogShown = false;
        if (data != null) {
            for (DialogData dialog : data) {
                if (isConditionOk(dialog.condition)) {
                    loadDialog(dialog);
                    dialogShown = true;
                }
            }
        }
        return dialogShown;
    }

    void setEffects(DialogData.ActionData[] data) {
        if (data == null) return;
        for (DialogData.ActionData E : data) {
            if (E == null) {
                continue;
            }
            if (E.removeItem != null && (!E.removeItem.isEmpty())) { //Removes an item from the player's inventory.
                Current.player().removeItem(E.removeItem);
            }
            if (E.addItem != null && (!E.addItem.isEmpty())) { //Gives an item to the player.
                Current.player().addItem(E.addItem);
            }
            if (E.addLife != 0) { //Gives (positive or negative) life to the player. Cannot go over max health.
                Current.player().heal(E.addLife);
            }
            if (E.addGold != 0) { //Gives (positive or negative) gold to the player.
                if (E.addGold > 0) Current.player().giveGold(E.addGold);
                else Current.player().takeGold(-E.addGold);
            }
            if (E.addShards != 0) { //Gives (positive or negative) mana shards to the player.
                if (E.addShards > 0) Current.player().addShards(E.addShards);
                else Current.player().takeShards(-E.addShards);
            }
            if (E.giveBlessing != null) { //Gives a blessing for your next battle.
                Current.player().addBlessing(E.giveBlessing);
            }
            if (E.setColorIdentity != null && !E.setColorIdentity.isEmpty()) { //Sets color identity (use sparingly)
                Current.player().setColorIdentity(E.setColorIdentity);
            }
            if (E.setCharacterFlag != null && !E.setCharacterFlag.key.isEmpty()) { //Set a quest to given value.
                Current.player().setCharacterFlag(E.setCharacterFlag.key, E.setCharacterFlag.val);
            }
            if (E.advanceCharacterFlag != null && !E.advanceCharacterFlag.isEmpty()) { //Increase a given quest flag by 1.
                Current.player().advanceCharacterFlag(E.advanceCharacterFlag);
            }
            if (E.setQuestFlag != null && !E.setQuestFlag.key.isEmpty()) { //Set a quest to given value.
                Current.player().setQuestFlag(E.setQuestFlag.key, E.setQuestFlag.val);
            }
            if (E.advanceQuestFlag != null && !E.advanceQuestFlag.isEmpty()) { //Increase a given quest flag by 1.
                Current.player().advanceQuestFlag(E.advanceQuestFlag);
            }
            if (E.grantRewards != null && E.grantRewards.length > 0) {
                Array<Reward> ret = new Array<Reward>();
                for (RewardData rdata : E.grantRewards) {
                    ret.addAll(rdata.generate(false, true));
                }
                RewardScene.instance().loadRewards(ret, RewardScene.Type.QuestReward, null);
                Forge.switchScene(RewardScene.instance());
            }
//            if (E.issueQuest != null && (!E.issueQuest.isEmpty())) {
//                emitQuestAccepted();
//            }
        }
    }

    private void loadDialog(DialogData dialog) { //Displays a dialog with dialogue and possible choices.
        setEffects(dialog.action);
        Dialog D = getDialog();
        Localizer L = Forge.getLocalizer();
        D.getTitleTable().clear();
        D.getContentTable().clear();
        D.getButtonTable().clear(); //Clear tables to start fresh.
        D.clearListeners();
//        Sprite sprite = null;

//        Actor actor = stage.getByID(parentID);
//        if (actor instanceof CharacterSprite)
//            sprite = ((CharacterSprite) actor).getAvatar();
        String text; //Check for localized string (locname), otherwise print text.
        if (dialog.loctext != null && !dialog.loctext.isEmpty()) text = L.getMessage(dialog.loctext);
        else text = dialog.text;

        TypingLabel A = Controls.newTypingLabel(text);
        A.setWrap(true);
        Array<TextraButton> buttons = new Array<>();
        A.setTypingListener(new TypingAdapter() {
            @Override
            public void end() {
                float delay = 0.09f;
                for (TextraButton button : buttons) {
                    Timer.schedule(new Timer.Task() {
                        @Override
                        public void run() {
                            button.setVisible(true);
                        }
                    }, delay);
                    delay += 0.10f;
                }
            }
        });
        float width = 250f;

        D.getContentTable().add(A).width(width); //Add() returns a Cell, which is what the width is being applied to.
        if (dialog.options != null) {
            int i = 0;
            for (DialogData option : dialog.options) {
                if (isConditionOk(option.condition)) {
                    String name; //Get localized label if present.
                    if (option.locname != null && !option.locname.isEmpty()) name = L.getMessage(option.locname);
                    else name = option.name;
                    TextraButton B = Controls.newTextButton(name, () -> {
                        loadDialog(option);

                        if (option.callback != null) {
                            option.callback.run(true);
                        }
                    });
                    B.getTextraLabel().setWrap(true); //We want this to wrap in case it's a wordy choice.
                    buttons.add(B);
                    B.setVisible(false);
                    D.getButtonTable().add(B).width(width - 10); //The button table also returns a Cell when adding.
                    //TODO: Reducing the space a tiny bit could help. But should be fine as long as there aren't more than 4-5 options.
                    D.getButtonTable().row(); //Add a row. Tried to allow a few per row but it was a bit erratic.
                    i++;
                }
            }
            D.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    A.skipToTheEnd();
                    super.clicked(event, x, y);
                }
            });
            if (i == 0) {
                hideDialog();
                emitDialogFinished();
            } else
                showDialog(getDialog());
        } else {
            hideDialog();
        }
    }

    public void addDialogCompleteListener(ChangeListener listener) {
        dialogCompleteList.add(listener);
    }

    private void emitDialogFinished() {
        if (dialogCompleteList != null && dialogCompleteList.size() > 0) {
            ChangeListener.ChangeEvent evt = new ChangeListener.ChangeEvent();
            for (ChangeListener listener : dialogCompleteList) {
                listener.changed(evt, null);
            }
        }
    }

    public boolean isConditionOk(DialogData.ConditionData[] data) {
        if (data == null) return true;
        AdventurePlayer player = Current.player();
        for (DialogData.ConditionData condition : data) {
            //TODO:Check for card in inventory.
            if (condition.item != null && !condition.item.isEmpty()) { //Check for an item in player's inventory.
                if (!player.hasItem(condition.item)) {
                    if (!condition.not) return false; //Only return on a false.
                } else if (condition.not) return false;
            }
            if (condition.colorIdentity != null && !condition.colorIdentity.isEmpty()) { //Check for player's color ID.
                if (player.getColorIdentity().hasAllColors(ColorSet.fromNames(condition.colorIdentity.toCharArray()).getColor())) {
                    if (!condition.not) return false;
                } else if (condition.not) return false;
            }
            if (condition.hasGold != 0) { //Check for at least X gold.
                if (player.getGold() < condition.hasGold) {
                    if (!condition.not) return false;
                } else if (condition.not) return false;
            }
            if (condition.hasShards != 0) { //Check for at least X gold.
                if (player.getShards() < condition.hasShards) {
                    if (!condition.not) return false;
                } else if (condition.not) return false;
            }
            if (condition.hasLife != 0) { //Check for at least X life..
                if (player.getLife() < condition.hasLife + 1) {
                    if (!condition.not) return false;
                } else if (condition.not) return false;
            }
            if (condition.hasBlessing != null && !condition.hasBlessing.isEmpty()) { //Check for a named blessing.
                if (!player.hasBlessing(condition.hasBlessing)) {
                    if (!condition.not) return false;
                } else if (condition.not) return false;
            }

            if (condition.getQuestFlag != null) {
                String key = condition.getQuestFlag.key;
                String cond = condition.getQuestFlag.op;
                int val = condition.getQuestFlag.val;
                int QF = player.getQuestFlag(key);
                if (!player.checkQuestFlag(key)) return false; //If the quest is not ongoing, stop.
                if (!checkFlagCondition(QF, cond, val)) {
                    if (!condition.not) return false;
                } else {
                    if (condition.not) return false;
                }
            }
            if (condition.checkQuestFlag != null && !condition.checkQuestFlag.isEmpty()) {
                if (!player.checkQuestFlag(condition.checkQuestFlag)) {
                    if (!condition.not) return false;
                } else if (condition.not) return false;
            }

            if (condition.getCharacterFlag != null) {
                String key = condition.getCharacterFlag.key;
                String cond = condition.getCharacterFlag.op;
                int val = condition.getCharacterFlag.val;
                int QF = player.getCharacterFlag(key);
                if (!player.checkCharacterFlag(key)) return false; //If the quest is not ongoing, stop.
                if (!checkFlagCondition(QF, cond, val)) {
                    if (!condition.not) return false;
                } else {
                    if (condition.not) return false;
                }
            }
            if (condition.checkCharacterFlag != null && !condition.checkCharacterFlag.isEmpty()) {
                if (!player.checkCharacterFlag(condition.checkCharacterFlag)) {
                    if (!condition.not) return false;
                } else if (condition.not) return false;
            }
        }
        return true;
    }

    private boolean checkFlagCondition(int flag, String condition, int value) {
        switch (condition.toUpperCase()) {
            default:
            case "EQUALS":
            case "EQUAL":
            case "=":
                if (flag == value) return true;
            case "LESSTHAN":
            case "<":
                if (flag < value) return true;
            case "MORETHAN":
            case ">":
                if (flag > value) return true;
            case "LE_THAN":
            case "<=":
                if (flag <= value) return true;
            case "ME_THAN":
            case ">=":
                if (flag >= value) return true;
        }
        return false;
    }

    public void showDialog(Array<DialogData> data) {
        if (!activate(data)) {
            return;
        }

        dialogButtonMap.clear();
        for (int i = 0; i < dialog.getButtonTable().getCells().size; i++) {
            dialogButtonMap.add((TextraButton) dialog.getButtonTable().getCells().get(i).getActor());
        }
        dialog.show(stage, Actions.show());
        dialog.setPosition((stage.getWidth() - dialog.getWidth()) / 2, (stage.getHeight() - dialog.getHeight()) / 2);
        if (Forge.hasGamepad() && !dialogButtonMap.isEmpty())
            stage.setKeyboardFocus(dialogButtonMap.first());
    }

    public void hideDialog() {
        dialog.hide();
        dialog.clearListeners();
    }
}
