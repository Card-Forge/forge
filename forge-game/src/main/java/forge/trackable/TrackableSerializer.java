package forge.trackable;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class TrackableSerializer {
    public final static char DELIMITER = (char)5;

    private final BufferedWriter bw;

    public TrackableSerializer(String filename) throws IOException {
        bw = new BufferedWriter(new FileWriter(filename));
    }

    public void write(String value) {
        try {
            bw.write(value + DELIMITER);
        }
        catch (Exception e) {}
    }
    public void write(boolean value) {
        write(value ? "1" : "0");
    }
    public void write(int value) {
        write(Integer.toString(value));
    }
    public void write(byte value) {
        write(Byte.toString(value));
    }
    public void write(long value) {
        write(Long.toString(value));
    }
    public void write(float value) {
        write(Float.toString(value));
    }
    public void write(double value) {
        write(Double.toString(value));
    }
    public void write(TrackableIndex<? extends TrackableObject> index) {
        write(index.size());
        for (TrackableObject o : index.values()) {
            o.serialize(this);
        }
    }
    public void write(TrackableCollection<? extends TrackableObject> collection) {
        write(collection.size());
        for (TrackableObject o : collection) {
            write(o.getId()); //only write id as index will store all other information about object
        }
    }
}
