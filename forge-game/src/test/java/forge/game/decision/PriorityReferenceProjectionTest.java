package forge.game.decision;

import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class PriorityReferenceProjectionTest {

    @Test
    public void projectionExcludesProcessAndLatencyButKeepsSemanticChanges() {
        final String header = "event_type,process_id,decision_sequence_id,subdecision_index,"
                + "request_generation_ns,native_callback_ns,selection_mapping,status";
        final String first = "PRIORITY,41,7,0,100,200,MAPPED,DECISION";
        final String measurementOnlyChanged = "PRIORITY,99,7,0,900,800,MAPPED,DECISION";
        final String semanticsChanged = "PRIORITY,99,7,0,900,800,UNMAPPED,DECISION";

        final List<String> left = PriorityReferenceProjection.project(header, List.of(first));
        final List<String> measurement = PriorityReferenceProjection.project(header,
                List.of(measurementOnlyChanged));
        final List<String> semantics = PriorityReferenceProjection.project(header, List.of(semanticsChanged));

        assertEquals(left, measurement);
        assertEquals(PriorityReferenceProjection.hash(left), PriorityReferenceProjection.hash(measurement));
        assertNotEquals(left, semantics);
    }

    @Test
    public void columnValuesReuseTheStrictCsvParser() {
        assertEquals(PriorityReferenceProjection.columnValues("event_type,process_id,detail",
                List.of("PRIORITY,41,plain", "MODE,41,\"value,with,commas\""), "process_id"),
                Set.of("41"));
    }
}
