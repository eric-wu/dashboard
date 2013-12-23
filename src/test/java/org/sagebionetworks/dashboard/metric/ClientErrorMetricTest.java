package org.sagebionetworks.dashboard.metric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.dashboard.parse.ClientReader;
import org.sagebionetworks.dashboard.parse.ErrorFilter;
import org.sagebionetworks.dashboard.parse.ProdFilter;

public class ClientErrorMetricTest {
    @Test
    public void test() {
        UniqueCountMetric metric = new ClientErrorMetric();
        assertEquals("clientErrorMetric", metric.getName());
        assertTrue(metric.getRecordReader() instanceof ClientReader);
        assertEquals(2, metric.getFilters().size());
        assertTrue(metric.getFilters().get(0) instanceof ProdFilter);
        assertTrue(metric.getFilters().get(1) instanceof ErrorFilter);
    }
}
