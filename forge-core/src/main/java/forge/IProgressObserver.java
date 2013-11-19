package forge;

public interface IProgressObserver{
    void setOperationName(String name, boolean usePercents);
    void report(int current, int total);
    
    // does nothing, used when they pass null instead of an instance 
    public final static IProgressObserver emptyObserver = new IProgressObserver() {
        @Override public void setOperationName(String name, boolean usePercents) {}
        @Override public void report(int current, int total) {}
    };
}