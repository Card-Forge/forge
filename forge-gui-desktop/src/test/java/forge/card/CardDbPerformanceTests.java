package forge.card;

import forge.item.PaperCard;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import static org.testng.Assert.assertNotNull;

public class CardDbPerformanceTests  extends CardDbTestCase {

    private Set<String> fullDbCardNames = new TreeSet<>();

    @Override
    @BeforeMethod
    public void setup() {
        super.setup();
        Collection<PaperCard> uniqueCards = this.cardDb.getUniqueCards();
        for (PaperCard card : uniqueCards)
            this.fullDbCardNames.add(card.getName());
    }

    @Test(enabled = false)  // disabled to not run in battery
    public void testBenchmarkFullDbGetCardLegacyImplementation() {
        int nRuns = 100;
        long averageTime = 0;
        long minTime = 10000;   // 10 secs
        long maxTime = 0;
        for (int r = 1; r <= nRuns; r++) {
            long start = System.currentTimeMillis();
            for (String name : this.fullDbCardNames) {
                PaperCard card = this.legacyCardDb.getCard(name);
                assertNotNull(card);
            }
            long timeRun = System.currentTimeMillis() - start;
            averageTime += timeRun;
            if (timeRun < minTime)
                minTime = timeRun;
            if (timeRun > maxTime)
                maxTime = timeRun;
        }
        System.out.println("[LEGACY] Total Time (in sec): " + ((double) averageTime)/ 1000);
        System.out.println("[LEGACY] Average Time (in sec): " + ((double) averageTime / nRuns)/ 1000);
        System.out.println("[LEGACY] Best Time (in sec): " + ((double) minTime)/ 1000);
        System.out.println("[LEGACY] Worst Time (in sec): " + ((double) maxTime)/ 1000);
    }

    @Test(enabled = false)  // disabled to not run in battery
    public void testBenchmarkFullDbGetCardNewDbImplementation() {
        int nRuns = 100;
        long averageTime = 0;
        long minTime = 10000;   // 10 secs
        long maxTime = 0;
        for (int r = 1; r <= nRuns; r++) {
            long start = System.currentTimeMillis();
            for (String name : this.fullDbCardNames) {
                PaperCard card = this.cardDb.getCard(name);
                assertNotNull(card);
            }
            long timeRun = System.currentTimeMillis() - start;
            averageTime += timeRun;
            if (timeRun < minTime)
                minTime = timeRun;
            if (timeRun > maxTime)
                maxTime = timeRun;
        }
        System.out.println("[NEW] Total Time (in sec): " + ((double) averageTime)/ 1000);
        System.out.println("[NEW] Average Time (in sec): " + ((double) averageTime / nRuns)/ 1000);
        System.out.println("[NEW] Best Time (in sec): " + ((double) minTime)/ 1000);
        System.out.println("[NEW] Worst Time (in sec): " + ((double) maxTime)/ 1000);
    }

    @Test
    public void testGetCardFullDbNewImplementationToProfile(){
        for (String name : this.fullDbCardNames) {
            PaperCard card = this.cardDb.getCard(name);
            assertNotNull(card);
        }
    }

    @Test
    public void testGetCardFullDbLegacyImplementationToProfile(){
        for (String name : this.fullDbCardNames) {
            PaperCard card = this.legacyCardDb.getCard(name);
            assertNotNull(card);
        }
    }
}
