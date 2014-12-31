package forge.util;

public abstract class Visitor<T> {
    public abstract void visit(T object);

    public void visitAll(Iterable<? extends T> objects) {
        for (T obj : objects) {
            visit(obj);
        }
    }
}
