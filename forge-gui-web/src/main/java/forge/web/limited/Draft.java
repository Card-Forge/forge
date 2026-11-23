package forge.web.limited;

import forge.web.protocol.ToBrowser.DraftState;

/** A draft that this browser picks cards in. It is either a draft against the computer (OfflineDraft) or a seat at an online table (OnlineDraft). */
public interface Draft {
    /** The state last sent, to send again to a browser that reconnects. Null before the first pack. */
    DraftState latest();

    /** Picks the card at index from the pack shown in state step. A click on a pack that was already passed on is ignored. */
    void pick(int step, int index, boolean sideboard);

    /** Moves the pick at index into the sideboard or back into the main deck. Desktop also lets a drafter sort picks during the draft. */
    void move(int index, boolean sideboard);
}
