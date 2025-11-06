package com.github.qubership.vsec.utils;

import com.netcracker.qubership.vsec.utils.MiscUtils;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MiscUtilsTest {

    @Test
    public void jsonExtractorTest() {
        String testStr = """
                ```json
                {
                    "key1": "valueA",
                    "key2": "valueB"
                }
                ```
                """;

        String expStr = """
                {
                    "key1": "valueA",
                    "key2": "valueB"
                }""";

        String actStr = MiscUtils.getJsonFromMDQuotedString(testStr);
        assertEquals(expStr, actStr);
    }
}
