package forge.trackable;

import forge.util.FileUtil;

public class TrackableDeserializer {
    private int index;
    private final String[] pieces;

    public TrackableDeserializer(String filename) {
        pieces = FileUtil.readFileToString(filename).split(String.valueOf(TrackableSerializer.DELIMITER));
    }

    public String readString() {
        return pieces[index++];
    }
    public boolean readBoolean() {
        return readString().equals("1");
    }
    public int readInt() {
        return Integer.parseInt(readString());
    }
    public byte readByte() {
        return Byte.parseByte(readString());
    }
    public long readLong() {
        return Long.parseLong(readString());
    }
    public float readFloat() {
        return Float.parseFloat(readString());
    }
    public double readDouble() {
        return Double.parseDouble(readString());
    }

    public <T extends TrackableObject> TrackableCollection<T> readCollection(TrackableCollection<T> oldValue) {
        return null; //TODO
    }
}
