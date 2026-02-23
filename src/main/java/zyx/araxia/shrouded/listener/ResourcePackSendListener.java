package zyx.araxia.shrouded.listener;

import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import zyx.araxia.shrouded.ResourcePackServer;

import java.net.URI;
import java.util.UUID;

/**
 * Sends the plugin resource pack to every player when they join, using the
 * URL and SHA-1 hash provided by {@link ResourcePackServer}.
 */
public class ResourcePackSendListener implements Listener {

    private final ResourcePackServer packServer;
    private final String serverIp;

    public ResourcePackSendListener(ResourcePackServer packServer, String serverIp) {
        this.packServer = packServer;
        this.serverIp = serverIp;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!packServer.isRunning())
            return;

        String url = packServer.getUrl(serverIp);
        String hash = packServer.getSha1Hex();

        ResourcePackInfo info = ResourcePackInfo.resourcePackInfo()
                .id(UUID.nameUUIDFromBytes(url.getBytes()))
                .uri(URI.create(url))
                .hash(hash)
                .build();

        event.getPlayer().sendResourcePacks(
                ResourcePackRequest.resourcePackRequest()
                        .packs(info)
                        .required(true)
                        .build());
    }
}
