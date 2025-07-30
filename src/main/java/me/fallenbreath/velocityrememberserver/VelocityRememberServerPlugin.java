package me.fallenbreath.velocityrememberserver;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Plugin(id = PluginMeta.ID, name = PluginMeta.NAME, version = PluginMeta.VERSION, url = "https://github.com/TISUnion/VelocityRememberServer", description = "A velocity plugin to remember the last server you logged in", authors = {"Fallen_Breath"})
public class VelocityRememberServerPlugin {
    private final ProxyServer server;
    private final Logger logger;
    private final PlayerLocations playerLocations;
    private final Config config;
    private final ConcurrentHashMap<UUID, Long> playerJoinTimes = new ConcurrentHashMap<>();

    @Inject
    public VelocityRememberServerPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.playerLocations = new PlayerLocations(logger, dataDirectory.resolve("locations.yml"));
        this.config = new Config(logger, dataDirectory.resolve("config.yml"));
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.config.load();
        this.playerLocations.load();
        this.server.getEventManager().register(this, PlayerChooseInitialServerEvent.class, this::chooseServer);
        this.server.getEventManager().register(this, ServerConnectedEvent.class, this::recordLastServer);
        this.server.getEventManager().register(this, DisconnectEvent.class, this::onPlayerDisconnect);
    }

    private void chooseServer(PlayerChooseInitialServerEvent event) {
        if (!this.config.isEnabled()) {
            return;
        }

        var playerUuid = event.getPlayer().getGameProfile().getId();
        this.playerJoinTimes.put(playerUuid, System.currentTimeMillis());

        this.playerLocations.getLastServer(playerUuid).flatMap(this.server::getServer).ifPresent(event::setInitialServer);
    }

    private void recordLastServer(ServerConnectedEvent event) {
        if (!this.config.isEnabled()) {
            return;
        }

        this.playerLocations.setLastServer(event.getPlayer().getGameProfile().getId(), event.getServer().getServerInfo().getName());
    }

    private void onPlayerDisconnect(DisconnectEvent event) {
        if (!this.config.isEnabled()) {
            return;
        }

        var playerUuid = event.getPlayer().getGameProfile().getId();
        Long joinTime = this.playerJoinTimes.remove(playerUuid);

        if (joinTime != null) {
            long timeOnline = System.currentTimeMillis() - joinTime;
            long timeoutMs = TimeUnit.SECONDS.toMillis(this.config.getDisconnectTimeoutSeconds());

            if (timeOnline < timeoutMs && !event.getLoginStatus().equals(DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN)) {
                this.playerLocations.removePlayerLocation(playerUuid);
                this.logger.info("Cleared location for player {} due to early disconnect ({}ms < {}ms)", playerUuid, timeOnline, timeoutMs);
            }
        }
    }
}
