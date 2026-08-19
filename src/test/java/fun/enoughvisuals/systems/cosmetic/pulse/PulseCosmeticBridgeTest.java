package fun.enoughvisuals.systems.cosmetic.pulse;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PulseCosmeticBridgeTest {
    @Test
    void pulseBridgeLoadsAllPastaCosmetics() {
        assertTrue(PulseCosmeticBridge.available(), "Pulse cosmetic runtime should be available");

        // Verify Hats
        List<PulseCosmeticBridge.Item> hats = PulseCosmeticBridge.items("HAT");
        Set<Integer> hatIds = hats.stream().map(PulseCosmeticBridge.Item::id).collect(Collectors.toSet());
        for (int expected : new int[]{201, 202, 203, 204}) {
            assertTrue(hatIds.contains(expected), "Missing hat id: " + expected);
            assertNotNull(PulseCosmeticBridge.modelResource(expected), "Missing model for hat " + expected);
            assertNotNull(PulseCosmeticBridge.textureResource(expected), "Missing texture for hat " + expected);
        }

        // Verify Wings
        List<PulseCosmeticBridge.Item> wings = PulseCosmeticBridge.items("WINGS");
        Set<Integer> wingIds = wings.stream().map(PulseCosmeticBridge.Item::id).collect(Collectors.toSet());
        for (int expected : new int[]{210, 211, 212, 213, 214, 215, 216, 217}) {
            assertTrue(wingIds.contains(expected), "Missing wings id: " + expected);
            assertNotNull(PulseCosmeticBridge.modelResource(expected), "Missing model for wings " + expected);
            assertNotNull(PulseCosmeticBridge.textureResource(expected), "Missing texture for wings " + expected);
        }

        // Verify Pets
        List<PulseCosmeticBridge.Item> pets = PulseCosmeticBridge.items("PET");
        Set<Integer> petIds = pets.stream().map(PulseCosmeticBridge.Item::id).collect(Collectors.toSet());
        for (int expected : new int[]{220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233}) {
            assertTrue(petIds.contains(expected), "Missing pet id: " + expected);
            assertNotNull(PulseCosmeticBridge.modelResource(expected), "Missing model for pet " + expected);
            assertNotNull(PulseCosmeticBridge.textureResource(expected), "Missing texture for pet " + expected);
        }
    }
}
