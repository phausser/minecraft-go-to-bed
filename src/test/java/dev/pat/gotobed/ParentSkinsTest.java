package dev.pat.gotobed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ParentSkinsTest {

    @Test
    void textureFilesContainDecodableSignedPropertiesAndMatchingUrls() throws Exception {
        for (String name : new String[] {"papa", "mama"}) {
            JsonObject json = resource(name);
            assertTrue(Base64.getDecoder().decode(json.get("signature").getAsString()).length > 0);
            String url = skin(json).get("url").getAsString();
            assertTrue(url.matches("https?://textures\\.minecraft\\.net/texture/[0-9a-f]{64}"));
            assertEquals(json.get("url").getAsString(), url.replace("http://", "https://"));
        }
    }

    @Test
    void parentsUseDifferentTexturesAndTheCorrectModels() throws Exception {
        JsonObject papa = resource("papa");
        JsonObject mama = resource("mama");
        assertNotEquals(skin(papa).get("url"), skin(mama).get("url"));
        assertNotEquals(payload(papa).get("profileId"), payload(mama).get("profileId"));
        assertEquals("classic", papa.get("model").getAsString());
        assertEquals("slim", mama.get("model").getAsString());
        assertEquals("classic", model(skin(papa)));
        assertEquals("slim", model(skin(mama)));
    }

    @Test
    void bundledImagesHaveSkinDimensionsAndOnlyPapaHasAWhiteBeard() throws Exception {
        for (String name : new String[] {"papa", "mama"}) {
            try (InputStream in = getClass().getResourceAsStream("/skins/" + name + ".png")) {
                assertNotNull(in);
                var image = ImageIO.read(in);
                assertNotNull(image);
                assertEquals(64, image.getWidth());
                assertEquals(64, image.getHeight());
                // Front of the head, lower face: the distinguishing white beard.
                int lowerFace = image.getRGB(11, 14);
                if (name.equals("papa")) {
                    assertEquals(0xfff5f5f5, lowerFace);
                } else {
                    assertNotEquals(0xfff5f5f5, lowerFace);
                }
            }
        }
    }

    private JsonObject resource(String name) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/skins/" + name + ".texture.json")) {
            assertNotNull(in);
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    private static JsonObject payload(JsonObject resource) {
        byte[] decoded = Base64.getDecoder().decode(resource.get("value").getAsString());
        return JsonParser.parseString(new String(decoded, StandardCharsets.UTF_8))
                .getAsJsonObject();
    }

    private static JsonObject skin(JsonObject resource) {
        return payload(resource).getAsJsonObject("textures").getAsJsonObject("SKIN");
    }

    private static String model(JsonObject skin) {
        return skin.has("metadata")
                ? skin.getAsJsonObject("metadata").get("model").getAsString()
                : "classic";
    }
}
