package forge.menu;

/** A tab or button that tracks and displays a count of unread chat messages. */
public interface IUnreadIndicator {
    void incrementUnread();
    void clearUnread();
}
