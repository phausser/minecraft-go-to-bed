package dev.pat.gotobed;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ParentSkinsTest {

    @Test
    void textureFilesContainSignedProperties() throws Exception {
        for (String name : new String[] {"papa", "mama"}) {
            String json = Files.readString(Path.of("src/main/resources/skins", name + ".texture.json"));
            assertTrue(json.contains("\"value\""), name + " value");
            assertTrue(json.contains("\"signature\""), name + " signature");
            assertFalse(json.contains("\"value\": \"\""), name + " empty value");
        }
    }
}
