package forge.quest;

import forge.game.player.IHasIcon;

public interface IQuestEvent extends IHasIcon {
    String getFullTitle();
    String getDescription();
    void select();
    boolean hasImage();
}
