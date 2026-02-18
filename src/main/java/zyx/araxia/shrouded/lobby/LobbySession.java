package zyx.araxia.shrouded.lobby;

import org.bukkit.entity.Player;
import zyx.araxia.shrouded.game.PlayerClass;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks the players currently inside a lobby and their chosen class.
 * This is a runtime-only object — it is not persisted to JSON.
 */
public class LobbySession {

    private final Lobby lobby;
    // null value = player joined but hasn't picked a class yet
    private final Map<UUID, PlayerClass> players = new HashMap<>();

    public LobbySession(Lobby lobby) {
        this.lobby = lobby;
    }

    public Lobby getLobby() { return lobby; }

    public boolean isFull() {
        return players.size() >= lobby.getMaxPlayers();
    }

    public boolean contains(UUID uuid) {
        return players.containsKey(uuid);
    }

    /**
     * Adds a player to the session.
     *
     * @return false if the lobby is already full.
     */
    public boolean add(Player player) {
        if (isFull()) return false;
        players.put(player.getUniqueId(), null);
        return true;
    }

    public void remove(UUID uuid) {
        players.remove(uuid);
    }

    public void setClass(UUID uuid, PlayerClass playerClass) {
        if (players.containsKey(uuid)) {
            players.put(uuid, playerClass);
        }
    }

    public PlayerClass getChosenClass(UUID uuid) {
        return players.get(uuid);
    }

    public int getPlayerCount() {
        return players.size();
    }

    public Map<UUID, PlayerClass> getPlayers() {
        return Collections.unmodifiableMap(players);
    }
}
