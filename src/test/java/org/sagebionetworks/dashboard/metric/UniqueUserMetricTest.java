package org.sagebionetworks.dashboard.metric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.dashboard.model.AccessRecord;
import org.sagebionetworks.dashboard.parse.ProdFilter;
import org.sagebionetworks.dashboard.parse.UserIdReader;

public class UniqueUserMetricTest {
    @Test
    public void test() {
        UniqueCountMetric<AccessRecord,String> metricToWrite = new UniqueUserMetric();
        assertEquals("uniqueUserMetric", metricToWrite.getName());
        assertTrue(metricToWrite.getRecordReader() instanceof UserIdReader);
        assertEquals(2, metricToWrite.getFilters().size());
        assertTrue(metricToWrite.getFilters().get(0) instanceof ProdFilter);
    }
}
