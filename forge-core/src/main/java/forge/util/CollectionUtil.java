package forge.util;

import forge.util.collect.FCollection;

import java.util.*;

public class CollectionUtil {
    public static <T> void shuffle(List<T> list) {
        shuffle(list, MyRandom.getRandom());
    }

    public static <T> void shuffle(List<T> list, Random random) {
        if (list instanceof FCollection) {
            //FCollection -> copyonwritearraylist is not compatible, use different method
            shuffleList(list, random);
        } else {
            //use Collections -> shuffle(LIST, RANDOM) since it's not FCollection
            Collections.shuffle(list, random);
        }
    }

    public static <T> void shuffleList(List<T> a, Random r) {
        int n = a.size();
        for (int i = 0; i < n; i++) {
            int change = i + r.nextInt(n - i);
            swap(a, i, change);
        }
    }

    private static <T> void swap(List<T> a, int i, int change) {
        T helper = a.get(i);
        a.set(i, a.get(change));
        a.set(change, helper);
    }

    public static <T> void reverse(List<T> list) {
        if (list == null || list.isEmpty())
            return;
        if (list instanceof FCollection) {
            //FCollection -> copyonwritearraylist is not compatible, use different method
            reverseWithRecursion(list, 0, list.size() - 1);
        } else {
            Collections.reverse(list);
        }
    }

    public static <T> void reverseWithRecursion(List<T> list) {
        if (list.size() > 1) {
            T value = list.remove(0);
            reverseWithRecursion(list);
            list.add(value);
        }
    }

    public static <T> void reverseWithRecursion(List<T> list, int startIndex, int lastIndex) {
        if (startIndex < lastIndex) {
            T t = list.get(lastIndex);
            list.set(lastIndex, list.get(startIndex));
            list.set(startIndex, t);
            startIndex++;
            lastIndex--;
            reverseWithRecursion(list, startIndex, lastIndex);
        }
    }
}