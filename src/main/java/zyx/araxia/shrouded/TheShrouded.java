package zyx.araxia.shrouded;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import zyx.araxia.shrouded.commands.ArenaSpawnCommand;
import zyx.araxia.shrouded.commands.ArenaLobbyCommand;
import zyx.araxia.shrouded.commands.ArenaRegisterCommand;
import zyx.araxia.shrouded.commands.LeaveSignRegisterCommand;
import zyx.araxia.shrouded.commands.LobbyCountdownCommand;
import zyx.araxia.shrouded.commands.LobbyForceStartCommand;
import zyx.araxia.shrouded.commands.LobbyLeaveCommand;
import zyx.araxia.shrouded.commands.LobbyRegisterCommand;
import zyx.araxia.shrouded.commands.LobbySpawnCommand;
import zyx.araxia.shrouded.commands.SignRegisterCommand;
import zyx.araxia.shrouded.listener.ClassSelectMenuListener;
import zyx.araxia.shrouded.listener.ClassSelectorItemListener;
import zyx.araxia.shrouded.listener.PlayerQuitListener;
import zyx.araxia.shrouded.listener.ResourcePackSendListener;
import zyx.araxia.shrouded.listener.ShroudedItemDropListener;
import zyx.araxia.shrouded.listener.SignClickListener;
import zyx.araxia.shrouded.listener.ArenaVoteMenuListener;
import zyx.araxia.shrouded.listener.ReturnToLobbyListener;
import zyx.araxia.shrouded.listener.SurvivorBombListener;
import zyx.araxia.shrouded.listener.SurvivorHealthPotionListener;
import zyx.araxia.shrouded.listener.SurvivorWebListener;
import zyx.araxia.shrouded.listener.ShroudedSwordStabListener;
import zyx.araxia.shrouded.listener.SurvivorWindChargeListener;
import zyx.araxia.shrouded.lobby.ArenaManager;
import zyx.araxia.shrouded.lobby.LobbyManager;

public class TheShrouded extends JavaPlugin {

        private LobbyManager lobbyManager;
        private ArenaManager arenaManager;
        private ResourcePackServer resourcePackServer;

        @Override
        public void onLoad() {
                installDataPack();
        }

        @Override
        public void onEnable() {
                lobbyManager = new LobbyManager(this);
                arenaManager = new ArenaManager(this);
                lobbyManager.setArenaManager(arenaManager);

                // Load config defaults (writes config.yml to disk on first run)
                saveDefaultConfig();

                // Start resource pack HTTP server if enabled
                if (getConfig().getBoolean("resource-pack.enabled", false)) {
                        int port = getConfig().getInt("resource-pack.port",
                                        8085);
                        String ip = getConfig().getString(
                                        "resource-pack.server-ip", "127.0.0.1");
                        resourcePackServer = new ResourcePackServer(
                                        getDataFolder(), port, getLogger());
                        resourcePackServer.start();
                        getServer().getPluginManager().registerEvents(
                                        new ResourcePackSendListener(
                                                        resourcePackServer, ip),
                                        this);
                }

                // Register commands
                final String registerLobbyName = "shrouded.register.lobby";
                final String registerSignName = "shrouded.register.sign";
                final String registerArenaName = "shrouded.register.arena";
                final String lobbyArenaName = "shrouded.lobby.arena";
                final String lobbyCountdownName = "shrouded.lobby.countdown";
                final String lobbyForceStartName = "shrouded.lobby.forcestart";
                final String lobbyLeaveName = "shrouded.lobby.leave";
                final String registerLeaveSignName = "shrouded.register.leavesign";
                final String lobbySpawnName = "shrouded.lobby.spawn";
                final String arenaSpawnName = "shrouded.arena.spawn";
                PluginCommand lobbyRegisterCmd = getCommand(registerLobbyName);
                PluginCommand signRegisterCmd = getCommand(registerSignName);
                PluginCommand arenaRegisterCmd = getCommand(registerArenaName);
                PluginCommand arenaLobbyCmd = getCommand(lobbyArenaName);
                PluginCommand lobbyCountdownCmd = getCommand(
                                lobbyCountdownName);
                PluginCommand lobbyForceStartCmd = getCommand(
                                lobbyForceStartName);
                PluginCommand lobbyLeaveCmd = getCommand(lobbyLeaveName);
                PluginCommand registerLeaveSignCmd = getCommand(
                                registerLeaveSignName);
                PluginCommand lobbySpawnCmd = getCommand(lobbySpawnName);
                PluginCommand arenaSpawnCmd = getCommand(arenaSpawnName);
                if (lobbyRegisterCmd != null)
                        lobbyRegisterCmd.setExecutor(new LobbyRegisterCommand(
                                        this, lobbyManager));
                if (signRegisterCmd != null)
                        signRegisterCmd.setExecutor(
                                        new SignRegisterCommand(lobbyManager));
                if (arenaRegisterCmd != null)
                        arenaRegisterCmd.setExecutor(new ArenaRegisterCommand(
                                        this, arenaManager));
                if (arenaLobbyCmd != null)
                        arenaLobbyCmd.setExecutor(new ArenaLobbyCommand(
                                        lobbyManager, arenaManager));
                if (lobbyCountdownCmd != null)
                        lobbyCountdownCmd.setExecutor(new LobbyCountdownCommand(
                                        lobbyManager));
                if (lobbyForceStartCmd != null)
                        lobbyForceStartCmd
                                        .setExecutor(new LobbyForceStartCommand(
                                                        lobbyManager));
                if (lobbyLeaveCmd != null)
                        lobbyLeaveCmd.setExecutor(
                                        new LobbyLeaveCommand(lobbyManager));
                if (registerLeaveSignCmd != null)
                        registerLeaveSignCmd.setExecutor(
                                        new LeaveSignRegisterCommand(
                                                        lobbyManager));
                if (lobbySpawnCmd != null)
                        lobbySpawnCmd.setExecutor(
                                        new LobbySpawnCommand(lobbyManager));
                if (arenaSpawnCmd != null)
                        arenaSpawnCmd.setExecutor(
                                        new ArenaSpawnCommand(arenaManager));

                // Register event listeners
                getServer().getPluginManager().registerEvents(
                                new SignClickListener(lobbyManager), this);
                getServer().getPluginManager().registerEvents(
                                new ClassSelectMenuListener(lobbyManager),
                                this);
                getServer().getPluginManager().registerEvents(
                                new ClassSelectorItemListener(lobbyManager),
                                this);
                getServer().getPluginManager().registerEvents(
                                new PlayerQuitListener(lobbyManager), this);
                getServer().getPluginManager().registerEvents(
                                new ShroudedItemDropListener(), this);
                getServer().getPluginManager().registerEvents(
                                new SurvivorHealthPotionListener(getConfig()),
                                this);
                getServer().getPluginManager().registerEvents(
                                new SurvivorBombListener(this), this);
                getServer().getPluginManager().registerEvents(
                                new SurvivorWebListener(this), this);
                getServer().getPluginManager().registerEvents(
                                new SurvivorWindChargeListener(this), this);
                getServer().getPluginManager()
                                .registerEvents(new ReturnToLobbyListener(
                                                lobbyManager, arenaManager),
                                                this);
                getServer().getPluginManager().registerEvents(
                                new ArenaVoteMenuListener(), this);
                getServer().getPluginManager().registerEvents(
                                new ShroudedSwordStabListener(this, lobbyManager),
                                this);

                // TODO: Iterate through player snapshots and restore any
                // players still
                // in
                // lobbies to their pre-lobby state to prevent them from being
                // stranded
                // in limbo
                // until they rejoin the server.
                // TODO: Restore all Arenas to their pre-game state in case the
                // plugin
                // was
                // disabled mid-session, including unclaiming any claimed arenas
                // and
                // resetting
                // any arena blocks that may have been modified.

                getLogger().info("TheShrouded has been enabled!");
        }

        @Override
        public void onDisable() {
                getLogger().info("TheShrouded has been disabled!");
                if (resourcePackServer != null) {
                        resourcePackServer.stop();
                }
                // TODO: Iterate through player snapshots and restore any
                // players still
                // in
                // lobbies to their pre-lobby state to prevent them from being
                // stranded
                // in limbo
                // until they rejoin the server.
                // TODO: Restore all Arenas to their pre-game state in case the
                // plugin
                // was
                // disabled mid-session, including unclaiming any claimed arenas
                // and
                // resetting
                // any arena blocks that may have been modified.
                // TODO: Clean up any active sessions, save data, etc.
                // TODO: Scan player inventories and remove any items with
                // Shrouded
                // in-game tags
                // to prevent smuggling out of the plugin's control.
        }

        public LobbyManager getLobbyManager() {
                return lobbyManager;
        }

        public ArenaManager getArenaManager() {
                return arenaManager;
        }

        // -------------------------------------------------------------------------
        // Data pack installation
        // -------------------------------------------------------------------------

        /**
         * Installs the bundled {@code shrouded-effects} data pack into the
         * primary world's {@code datapacks/} folder so the custom
         * {@code shrouded:potion_cooldown} mob effect is registered by the time
         * {@link #onEnable()} runs.
         * <p>
         * Files are always overwritten so the pack definition stays in sync
         * with the plugin version.
         */
        private void installDataPack() {
                // Resolve the primary world name from server.properties
                // (default:
                // "world")
                String levelName = "world";
                File serverProps = new File("server.properties");
                if (serverProps.exists()) {
                        try (FileInputStream in = new FileInputStream(
                                        serverProps)) {
                                Properties props = new Properties();
                                props.load(in);
                                levelName = props.getProperty("level-name",
                                                "world");
                        } catch (IOException e) {
                                getLogger().warning(
                                                "[TheShrouded] Could not read server.properties — "
                                                                + "using default world name 'world' for data pack installation.");
                        }
                }

                File packRoot = new File(
                                levelName + "/datapacks/shrouded-effects");
                File mobEffectDir = new File(packRoot,
                                "data/shrouded/mob_effect");
                mobEffectDir.mkdirs();

                extractResource("datapack/pack.mcmeta",
                                new File(packRoot, "pack.mcmeta"));
                extractResource("datapack/data/shrouded/mob_effect/potion_cooldown.json",
                                new File(mobEffectDir, "potion_cooldown.json"));
        }

        /**
         * Copies a classpath resource to {@code dest}, always overwriting.
         *
         * @param resourcePath path inside the plugin JAR (relative to resources
         *                     root)
         * @param dest         target file on disk
         */
        private void extractResource(String resourcePath, File dest) {
                try (InputStream in = getClass().getClassLoader()
                                .getResourceAsStream(resourcePath);
                                FileOutputStream out = new FileOutputStream(
                                                dest)) {
                        if (in == null) {
                                getLogger().log(Level.WARNING,
                                                "[TheShrouded] Missing bundled resource: {0}",
                                                resourcePath);
                                return;
                        }
                        in.transferTo(out);
                } catch (IOException e) {
                        getLogger().log(Level.WARNING,
                                        "[TheShrouded] Failed to extract {0}: {1}",
                                        new Object[] {
                                                        resourcePath,
                                                        e.getMessage()
                                        });
                }
        }
}