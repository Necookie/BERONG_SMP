package net.necookie.disastersim;

import net.necookie.disastersim.session.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests PasswordHasher in isolation — pure javax.crypto, no Minecraft server needed. */
class PasswordHasherTest {

    @Test
    void roundTripsCorrectPassword() {
        String stored = PasswordHasher.hash("correct horse battery staple");
        assertTrue(PasswordHasher.verify("correct horse battery staple", stored));
    }

    @Test
    void rejectsWrongPassword() {
        String stored = PasswordHasher.hash("correct horse battery staple");
        assertFalse(PasswordHasher.verify("wrong password", stored));
    }

    @Test
    void neverStoresPlaintext() {
        String stored = PasswordHasher.hash("hunter2");
        assertFalse(stored.contains("hunter2"));
    }

    @Test
    void twoHashesOfSamePasswordDiffer() {
        // Different random salts each call, even for the same input.
        String a = PasswordHasher.hash("same-password");
        String b = PasswordHasher.hash("same-password");
        assertNotEquals(a, b);
        assertTrue(PasswordHasher.verify("same-password", a));
        assertTrue(PasswordHasher.verify("same-password", b));
    }

    @Test
    void rejectsTamperedHash() {
        String stored = PasswordHasher.hash("tamper-test");
        String[] parts = stored.split("\\$");
        // Flip the stored hash portion (last segment) to simulate corruption/tampering.
        String tampered = parts[0] + "$" + parts[1] + "$" + parts[2] + "$" + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
        assertFalse(PasswordHasher.verify("tamper-test", tampered));
    }

    @Test
    void rejectsMalformedStoredValue() {
        assertFalse(PasswordHasher.verify("anything", "not-a-valid-hash"));
        assertFalse(PasswordHasher.verify("anything", null));
        assertFalse(PasswordHasher.verify("anything", ""));
    }
}
