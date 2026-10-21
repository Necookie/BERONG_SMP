package net.necookie.disastersim.gametest;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.necookie.disastersim.BerongSMP;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Registers the in-world system tests (catalog type GT). Only wired up when NeoForge's game-test
 * support is enabled (the gameTestServer run), so none of this is active in a normal server.
 * Each test is registered under its catalog case ID in lower case, e.g. {@code berongsmp:gs-24-02},
 * and runs on vanilla's empty structure; the tests drive the mod's own world coordinates directly.
 */
public final class BerongGameTests {

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, BerongSMP.MODID);

    /** case id → (max ticks, body) */
    private static final Map<String, Map.Entry<Integer, Consumer<GameTestHelper>>> TESTS = new LinkedHashMap<>();

    private BerongGameTests() {}

    static void add(String caseId, int maxTicks, Consumer<GameTestHelper> body) {
        // BERONG_GT_ONLY=G-SEC-12,G-COMP-10 reruns just those cases (retests after a fix).
        String only = System.getenv("BERONG_GT_ONLY");
        if (only != null && !only.isBlank()
                && java.util.Arrays.stream(only.split(",")).noneMatch(id -> id.trim().equalsIgnoreCase(caseId))) return;
        // BERONG_GT_PREFIX=FS-,EQ- loads only cases whose id starts with one of the prefixes, so a
        // big campaign can run in small slices instead of registering every case in one JVM.
        String prefix = System.getenv("BERONG_GT_PREFIX");
        if (prefix != null && !prefix.isBlank()
                && java.util.Arrays.stream(prefix.split(",")).noneMatch(p -> caseId.toLowerCase().startsWith(p.trim().toLowerCase()))) return;
        TESTS.put(caseId.toLowerCase(), Map.entry(maxTicks, body));
    }

    public static void register(IEventBus modEventBus) {
        // BERONG_GT_PROFILE=offline runs only the database-unreachable cases (GS-21) against a config
        // whose tursoUrl is a closed port; every other case runs in the default profile.
        if ("offline".equalsIgnoreCase(System.getenv("BERONG_GT_PROFILE"))) {
            OfflineGameTests.registerAll();
        } else if ("ian".equalsIgnoreCase(System.getenv("BERONG_GT_PROFILE"))) {
            // Ian's testing-instrument IDs, 20 variants each (select slices with BERONG_GT_PREFIX).
            IanFireTests.registerAll();
            IanAccountTests.registerAll();
            IanQuakeTests.registerAll();
            IanLoggingTests.registerAll();
            IanAcademyRecoveryTests.registerAll();
        } else if ("category".equalsIgnoreCase(System.getenv("BERONG_GT_PROFILE"))) {
            // Only the category campaign cases (performance, compatibility, usability, security, ...).
            PerfCompGameTests.registerAll();
            UseSecGameTests.registerAll();
            RelDqGameTests.registerAll();
        } else {
            SimulationGameTests.registerAll();
            PerfCompGameTests.registerAll();
            UseSecGameTests.registerAll();
            RelDqGameTests.registerAll();
        }
        for (var e : TESTS.entrySet()) {
            Consumer<GameTestHelper> body = e.getValue().getValue();
            FUNCTIONS.register(e.getKey(), () -> body);
        }
        FUNCTIONS.register(modEventBus);
        modEventBus.addListener(BerongGameTests::onRegisterTests);
    }

    private static void onRegisterTests(RegisterGameTestsEvent event) {
        for (var e : TESTS.entrySet()) {
            Identifier id = Identifier.fromNamespaceAndPath(BerongSMP.MODID, e.getKey());
            // One environment (= one batch) per test: batches run one after another, so no two
            // tests ever share a simulation building at the same time.
            Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(
                    Identifier.fromNamespaceAndPath(BerongSMP.MODID, "env_" + e.getKey()),
                    new TestEnvironmentDefinition.AllOf(List.of()));
            event.registerTest(id, new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id),
                    new TestData<>(env, Identifier.withDefaultNamespace("empty"), e.getValue().getKey(), 0, true)));
        }
    }
}
