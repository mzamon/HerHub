package za.co.herhub;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class HerHubSmokeTest {
    @Test
    public void featureCatalogueHasCompleteEntries() {
        assertEquals(17, Features.ALL.length);

        Set<String> keys = new HashSet<>();
        for (String[] feature : Features.ALL) {
            assertEquals(3, feature.length);
            assertNotNull(feature[0]);
            assertNotNull(feature[1]);
            assertNotNull(feature[2]);
            assertFalse(feature[0].isEmpty());
            assertFalse(feature[1].isEmpty());
            assertFalse(feature[2].isEmpty());
            assertEquals(1, keys.add(feature[0]) ? 1 : 0);
            assertFalse(Features.title(feature[0]).isEmpty());
        }
    }
}
