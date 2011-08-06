
package forge;


import java.util.ArrayList;


class TestMove extends Move {
    private final int    myNumber;
    
    private static int   classNumber = -1;
    private static int[] array;
    
    private static int   classIndex;
    private int          myIndex     = -1;
    
    public static void main(String[] args) {
        test();
    }
    
    //branching 2
    //fully test depths 1 and 2, one test of depth 3
    public static void test() {
        TestMove t;
        
        t = new TestMove(new int[] {4, 1, 6, 3, 2, 7, 6, 9});
        test("1", t.max(t, 3, true) == 7);
        
        t = new TestMove(new int[] {1, 2});
        test("2", t.max(t, 1, true) == 2);
        
        t = new TestMove(new int[] {2, 1});
        test("3", t.max(t, 1, true) == 2);
        

        t = new TestMove(new int[] {1, 2, 3, 4});
        test("4", t.max(t, 2, true) == 3);
        
        t = new TestMove(new int[] {2, 1, 4, 3});
        test("5", t.max(t, 2, true) == 3);
        
        t = new TestMove(new int[] {4, 3, 1, 2});
        test("6", t.max(t, 2, true) == 3);
        
        t = new TestMove(new int[] {3, 4, 2, 1});
        test("7", t.max(t, 2, true) == 3);
    }
    
    public static void test(String message, boolean shouldBeTrue) {
        if(!shouldBeTrue) throw new RuntimeException(message);
    }
    
    public TestMove(int i_array[]) {
        this();
        
        classIndex = 0;
        array = i_array;
    }
    
    public TestMove() {
        myNumber = classNumber;
        classNumber++;
    }
    
    public int getClassNumber() {
        return classNumber;
    }
    
    public int getMyNumber() {
        return myNumber;
    }
    
    @Override
    public Move[] generateMoves() {
        ArrayList<TestMove> list = new ArrayList<TestMove>();
        
        for(int i = 0; i < 2; i++)
            list.add(new TestMove());
        
        Move m[] = new Move[list.size()];
        list.toArray(m);
        return m;
    }
    
    @Override
    public int getScore() {
        if(myIndex == -1) {
            myIndex = classIndex;
            classIndex++;
        }
        return array[myIndex];
    }//getScore()
}
