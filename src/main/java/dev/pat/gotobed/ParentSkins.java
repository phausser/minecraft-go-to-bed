package dev.pat.gotobed;

import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.profile.PlayerTextures;

final class ParentSkins {

    private static final Pattern STRING_FIELD =
            Pattern.compile("\"(value|signature)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final UUID PAPA_UUID = UUID.fromString("6b0d7e0a-0b0d-4a0a-9a01-000000000001");
    private static final UUID MAMA_UUID = UUID.fromString("6b0d7e0a-0b0d-4a0a-9a01-000000000002");

    private final ResolvableProfile papa;
    private final ResolvableProfile mama;

    private ParentSkins(ResolvableProfile papa, ResolvableProfile mama) {
        this.papa = papa;
        this.mama = mama;
    }

    static ParentSkins load(GoToBedPlugin plugin) {
        return new ParentSkins(
                loadOne(plugin, "skins/papa.texture.json", PAPA_UUID, PlayerTextures.SkinModel.CLASSIC),
                loadOne(plugin, "skins/mama.texture.json", MAMA_UUID, PlayerTextures.SkinModel.SLIM));
    }

    ResolvableProfile profile(Speaker speaker) {
        return speaker == Speaker.PAPA ? papa : mama;
    }

    private static ResolvableProfile loadOne(
            GoToBedPlugin plugin, String resource, UUID uuid, PlayerTextures.SkinModel model) {
        String json = readResource(plugin, resource);
        String value = field(json, "value");
        String signature = field(json, "signature");
        // Unique UUID, no profile name: otherwise the client caches one skin for both
        // mannequins (filler UUID) or resolves "Papa"/"Mama" as real accounts.
        return ResolvableProfile.resolvableProfile()
                .uuid(uuid)
                .addProperty(new ProfileProperty("textures", value, signature))
                .skinPatch(patch -> patch.model(model))
                .build();
    }

    private static String readResource(GoToBedPlugin plugin, String resource) {
        try (InputStream in = plugin.getResource(resource)) {
            if (in == null) {
                throw new IllegalStateException("Resource fehlt: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Resource unlesbar: " + resource, e);
        }
    }

    private static String field(String json, String key) {
        Matcher matcher = STRING_FIELD.matcher(json);
        while (matcher.find()) {
            if (key.equals(matcher.group(1))) {
                return matcher.group(2);
            }
        }
        throw new IllegalStateException("Feld fehlt in Skin-JSON: " + key);
    }
}
