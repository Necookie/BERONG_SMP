package net.necookie.disastersim;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.necookie.disastersim.session.TursoClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests TursoClient.parseRows() JSON parsing in isolation — no HTTP, no Minecraft server.
 * parseRows() is pure Gson parsing; the only Minecraft reference is BerongSMP.LOGGER
 * which is only reached on parse errors, not in the happy path tested here.
 */
class TursoClientParseTest {

    private static final String EMPTY_RESULTS = "{\"results\":[]}";

    private static final String ONE_ROW =
        "{\"results\":[{\"response\":{\"result\":{" +
        "\"cols\":[{\"name\":\"id\"},{\"name\":\"student_name\"}]," +
        "\"rows\":[[{\"type\":\"integer\",\"value\":\"42\"}," +
                  "{\"type\":\"text\",\"value\":\"Alice\"}]]" +
        "}}}]}";

    private static final String TWO_ROWS =
        "{\"results\":[{\"response\":{\"result\":{" +
        "\"cols\":[{\"name\":\"id\"},{\"name\":\"score\"}]," +
        "\"rows\":[[{\"type\":\"integer\",\"value\":\"1\"}," +
                   "{\"type\":\"integer\",\"value\":\"75\"}]," +
                  "[{\"type\":\"integer\",\"value\":\"2\"}," +
                   "{\"type\":\"integer\",\"value\":\"90\"}]]" +
        "}}}]}";

    @Test
    void nullInputReturnsEmptyArray() {
        JsonArray result = TursoClient.parseRows(null);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void emptyResultsReturnsEmptyArray() {
        JsonArray result = TursoClient.parseRows(EMPTY_RESULTS);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void parsesSingleRowCorrectly() {
        JsonArray rows = TursoClient.parseRows(ONE_ROW);
        assertEquals(1, rows.size());
        JsonObject row = rows.get(0).getAsJsonObject();
        assertEquals("42", row.get("id").getAsString());
        assertEquals("Alice", row.get("student_name").getAsString());
    }

    @Test
    void parsesTwoRowsCorrectly() {
        JsonArray rows = TursoClient.parseRows(TWO_ROWS);
        assertEquals(2, rows.size());
        assertEquals("1", rows.get(0).getAsJsonObject().get("id").getAsString());
        assertEquals("2", rows.get(1).getAsJsonObject().get("id").getAsString());
        assertEquals("75", rows.get(0).getAsJsonObject().get("score").getAsString());
        assertEquals("90", rows.get(1).getAsJsonObject().get("score").getAsString());
    }

    // NOTE: tests for malformed/empty-string inputs are intentionally excluded.
    // Those inputs trigger the catch block in parseRows() which calls BerongSMP.LOGGER.warn(),
    // and BerongSMP's static initializer uses NeoForge's LogUtils which is unavailable outside
    // a full game runtime. Run error-path coverage as in-game game tests instead.
}
