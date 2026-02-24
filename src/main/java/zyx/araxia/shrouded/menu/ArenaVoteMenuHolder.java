package zyx.araxia.shrouded.menu;

import java.util.Collections;
import java.util.Map;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import zyx.araxia.shrouded.lobby.Arena;
import zyx.araxia.shrouded.lobby.LobbySession;

/**
 * Marker {@link InventoryHolder} for the arena-vote menu.
 * <p>
 * Carries the {@link LobbySession} that owns this vote and a slot-to-arena
 * mapping so the click listener can resolve the voted arena without any
 * global state.
 */
public class ArenaVoteMenuHolder implements InventoryHolder {

    private final LobbySession session;
    private final Map<Integer, Arena> slotArenaMap;

    public ArenaVoteMenuHolder(LobbySession session, Map<Integer, Arena> slotArenaMap) {
        this.session = session;
        this.slotArenaMap = Collections.unmodifiableMap(slotArenaMap);
    }

    public LobbySession getSession() {
        return session;
    }

    /** Returns the arena assigned to {@code slot}, or {@code null} if none. */
    public Arena getArenaAt(int slot) {
        return slotArenaMap.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return null; // purely a marker
    }
}
