package net.necookie.disastersim.scenario;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.necookie.disastersim.common.simulation.SimulationEventLogger;
import net.necookie.disastersim.session.PasswordHasher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** System-test cases for accounts (GS-01/02), and event serialization (GS-19-01).
 * Zone bounds (GS-22-01/02) run in the GameTest suite because Minecraft classes are not on the unit-test classpath. */
class PlatformScenarioTest {

    @Test @DisplayName("GS-01-01")
    void passwordHashHasDocumentedFormat() {
        String h = PasswordHasher.hash("Student#2026");
        assertTrue(h.matches("pbkdf2_sha256\\$210000\\$[^$]+\\$[^$]+"), h);
    }

    @Test @DisplayName("GS-01-02")
    void eachHashHasItsOwnSalt() {
        String a = PasswordHasher.hash("Student#2026");
        String b = PasswordHasher.hash("Student#2026");
        assertNotEquals(a, b);
        assertTrue(PasswordHasher.verify("Student#2026", a));
        assertTrue(PasswordHasher.verify("Student#2026", b));
    }

    @Test @DisplayName("GS-02-01")
    void correctPasswordVerifies() {
        assertTrue(PasswordHasher.verify("Student#2026", PasswordHasher.hash("Student#2026")));
    }

    @Test @DisplayName("GS-02-02")
    void wrongPasswordIsRejected() {
        assertFalse(PasswordHasher.verify("student#2026", PasswordHasher.hash("Student#2026")));
    }

    @Test @DisplayName("GS-19-01")
    void eventSerializesWithTypeTimestampAndData() {
        SimulationEventLogger logger = new SimulationEventLogger();
        logger.log("SIM_START", Map.of("sim_type", "FIRE", "x", 35.0));
        JsonArray arr = JsonParser.parseString(logger.toJson()).getAsJsonArray();
        JsonObject e = arr.get(0).getAsJsonObject();
        assertEquals("SIM_START", e.get("type").getAsString());
        assertTrue(e.get("tOffsetMs").getAsLong() >= 0);
        assertEquals("FIRE", e.getAsJsonObject("data").get("sim_type").getAsString());
    }
}
