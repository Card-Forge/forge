package forge.gui.control;

public enum PlaybackSpeed {
    SLOW(3),
    NORMAL(1),
    FAST(.1);

    private double modifier = 1;

    PlaybackSpeed(double modifier) {
        this.modifier = modifier;
    }

    public long applyModifier(long milliseconds) {
        return (long) (this.modifier * milliseconds);
    }

    public String nextSpeedText() {
        switch(this) {
            case NORMAL:
                return "10x speed";
            case FAST:
                return "1/3x speed";
            default:
                return "1x speed";
        }
    }

    public PlaybackSpeed nextSpeed() {
        switch(this) {
            case NORMAL:
                return PlaybackSpeed.FAST;
            case FAST:
                return PlaybackSpeed.SLOW;
            default:
                return PlaybackSpeed.NORMAL;
        }
    }
}
