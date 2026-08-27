package cn.wubo.sql.forge;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MetricsService 单测：覆盖 counter 累加 / maxLatency CAS / snapshot 格式。
 */
class MetricsServiceTest {

    @Test
    void recordCall_accumulatesCounts() {
        MetricsService m = new MetricsService();
        m.recordCall("jsonSelect", 10, true);
        m.recordCall("jsonSelect", 20, true);
        m.recordCall("jsonSelect", 30, false);

        Map<String, Object> snap = m.snapshot();
        assertNotNull(snap);
        Map<String, Object> tools = (Map<String, Object>) snap.get("tools");
        Map<String, Object> t = (Map<String, Object>) tools.get("jsonSelect");
        assertEquals(3, ((Number) t.get("calls")).intValue());
        assertEquals(1, ((Number) t.get("errors")).intValue());
        assertEquals(20.0, ((Number) t.get("avgLatencyMs")).doubleValue());
        assertEquals(30, ((Number) t.get("maxLatencyMs")).intValue());
    }

    @Test
    void recordCall_maxLatencyTracksCorrectly() {
        MetricsService m = new MetricsService();
        m.recordCall("tool", 5, true);
        m.recordCall("tool", 100, true);
        m.recordCall("tool", 50, true);
        // max 应该是 100，不会被后续更小的值覆盖
        m.recordCall("tool", 10, true);

        Map<String, Object> snap = m.snapshot();
        Map<String, Object> t = (Map<String, Object>)
                ((Map<String, Object>) snap.get("tools")).get("tool");
        assertEquals(100, ((Number) t.get("maxLatencyMs")).intValue());
    }

    @Test
    void snapshot_includesUptime() throws Exception {
        MetricsService m = new MetricsService();
        Thread.sleep(50);
        Map<String, Object> snap = m.snapshot();
        assertNotNull(snap.get("startedAtMs"));
        long uptime = ((Number) snap.get("uptimeSec")).longValue();
        assertTrue(uptime >= 0);
    }

    @Test
    void multipleTools_trackedSeparately() {
        MetricsService m = new MetricsService();
        m.recordCall("jsonSelect", 10, true);
        m.recordCall("getMetaDataDatabase", 5, true);
        m.recordCall("amisTemplateSave", 50, true);

        Map<String, Object> snap = m.snapshot();
        Map<String, Object> tools = (Map<String, Object>) snap.get("tools");
        assertEquals(3, tools.size());
        assertTrue(tools.containsKey("jsonSelect"));
        assertTrue(tools.containsKey("getMetaDataDatabase"));
        assertTrue(tools.containsKey("amisTemplateSave"));
    }
}
