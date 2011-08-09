package forge;


import org.testng.annotations.Test;

import java.util.ArrayList;


/**
 * <p>MoveTest class.</p>
 *
 * @author Forge
 * @version $Id$
 */
@Test(groups = {"UnitTest"}, timeOut = 1000)
public class MoveTest {

    class MoveConcrete extends Move {

        private final int myNumber;

        /**
         * Constant <code>classNumber=-1</code>
         */
        private int classNumber = -1;
        /**
         * Constant <code>array=</code>
         */
        private int[] array;

        /**
         * Constant <code>classIndex=</code>
         */
        private int classIndex;
        private int myIndex = -1;

        /**
         * <p>Constructor for MoveTest.</p>
         *
         * @param i_array an array of int.
         */
        public MoveConcrete(int i_array[]) {
            this();

            classIndex = 0;
            array = i_array;
        }

        /**
         * <p>Constructor for MoveTest.</p>
         */
        public MoveConcrete() {
            myNumber = classNumber;
            classNumber++;
        }

        /**
         * <p>Getter for the field <code>classNumber</code>.</p>
         *
         * @return a int.
         */
        public int getClassNumber() {
            return classNumber;
        }

        /**
         * <p>Getter for the field <code>myNumber</code>.</p>
         *
         * @return a int.
         */
        public int getMyNumber() {
            return myNumber;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Move[] generateMoves() {
            ArrayList<MoveTest> list = new ArrayList<MoveTest>();

            for (int i = 0; i < 2; i++)
                list.add(new MoveTest());

            Move m[] = new Move[list.size()];
            list.toArray(m);
            return m;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getScore() {
            if (myIndex == -1) {
                myIndex = classIndex;
                classIndex++;
            }
            return array[myIndex];
        }//getScore()
    }

    //branching 2
    //fully test depths 1 and 2, one test of depth 3

    /**
     * <p>test.</p>
     */
    @Test(timeOut = 1000)
    public void MoveTest1() {
        MoveConcrete t;

        t = new MoveConcrete(new int[]{4, 1, 6, 3, 2, 7, 6, 9});
        test("1", t.max(t, 3, true) == 7);

        t = new MoveConcrete(new int[]{1, 2});
        test("2", t.max(t, 1, true) == 2);

        t = new MoveConcrete(new int[]{2, 1});
        test("3", t.max(t, 1, true) == 2);


        t = new MoveConcrete(new int[]{1, 2, 3, 4});
        test("4", t.max(t, 2, true) == 3);

        t = new MoveConcrete(new int[]{2, 1, 4, 3});
        test("5", t.max(t, 2, true) == 3);

        t = new MoveConcrete(new int[]{4, 3, 1, 2});
        test("6", t.max(t, 2, true) == 3);

        t = new MoveConcrete(new int[]{3, 4, 2, 1});
        test("7", t.max(t, 2, true) == 3);
    }

    /**
     * <p>test.</p>
     *
     * @param message      a {@link java.lang.String} object.
     * @param shouldBeTrue a boolean.
     */
    public void test(String message, boolean shouldBeTrue) {
        if (!shouldBeTrue) {
            throw new RuntimeException(message);
        }
    }
}
