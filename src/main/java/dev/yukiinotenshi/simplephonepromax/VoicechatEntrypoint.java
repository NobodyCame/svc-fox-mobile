package dev.yukiinotenshi.simplephonepromax;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public final class VoicechatEntrypoint implements VoicechatPlugin {
    private final VoicechatPlugin client = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
            ? new VoicechatPluginImpl() : null;

    @Override public String getPluginId() { return "svc-fox-mobile"; }

    @Override public void initialize(VoicechatApi api) {
        if (client != null) client.initialize(api);
    }

    @Override public void registerEvents(EventRegistration registration) {
        if (client != null) client.registerEvents(registration);
    }
}
