package zyx.araxia.shrouded.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Intercepts outgoing {@code ENTITY_EQUIPMENT} packets and replaces every
 * equipment slot with an empty stack for every viewer that is <em>not</em> the
 * shrouded player themselves.
 *
 * <p>
 * This preserves the first-person experience (the shrouded player still sees
 * their own items in their hand and inventory) while making their equipment
 * invisible to all other players.
 *
 * <h3>Usage</h3>
 * 
 * <pre>{@code
 * // When the shrouded player enters the arena:
 * spoofer.startSpoofing(shroudedPlayer);
 *
 * // When they die or leave:
 * spoofer.stopSpoofing(shroudedPlayer);
 * }</pre>
 */
public class ShroudedEquipmentSpoofer extends PacketAdapter {

    /** UUIDs of players whose equipment should be hidden from others. */
    private final Set<UUID> spoofed = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Maps Minecraft entity ID → player UUID so we can look up the tracked
     * player from the raw integer in the packet without iterating every online
     * player on every packet send.
     */
    private final Map<Integer, UUID> entityIdMap = new ConcurrentHashMap<>();

    public ShroudedEquipmentSpoofer(Plugin plugin) {
        super(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_EQUIPMENT);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.isCancelled())
            return;

        PacketContainer packet = event.getPacket();
        int entityId = packet.getIntegers().read(0);

        UUID shroudedId = entityIdMap.get(entityId);
        if (shroudedId == null || !spoofed.contains(shroudedId))
            return;

        // The shrouded player themselves should still see their own equipment
        if (event.getPlayer().getUniqueId().equals(shroudedId))
            return;

        // Clone the packet and zero out all equipment entries
        List<Pair<EnumWrappers.ItemSlot, ItemStack>> equipment = packet.getSlotStackPairLists().read(0);

        List<Pair<EnumWrappers.ItemSlot, ItemStack>> empty = equipment.stream()
                .map(p -> new Pair<>(p.getFirst(), new ItemStack(Material.AIR)))
                .collect(Collectors.toList());

        PacketContainer spoofedPacket = packet.deepClone();
        spoofedPacket.getSlotStackPairLists().write(0, empty);
        event.setPacket(spoofedPacket);
    }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * Begins hiding {@code player}'s equipment from all other players.
     * Safe to call multiple times for the same player.
     *
     * @param player the shrouded player entering the arena
     */
    public void startSpoofing(Player player) {
        spoofed.add(player.getUniqueId());
        entityIdMap.put(player.getEntityId(), player.getUniqueId());
    }

    /**
     * Stops hiding {@code player}'s equipment.
     * Should be called when the shrouded player dies or leaves the arena.
     *
     * @param player the shrouded player
     */
    public void stopSpoofing(Player player) {
        spoofed.remove(player.getUniqueId());
        entityIdMap.remove(player.getEntityId());
    }

    /**
     * Returns {@code true} if {@code player}'s equipment is currently being
     * hidden from other players.
     */
    public boolean isSpoofing(Player player) {
        return spoofed.contains(player.getUniqueId());
    }
}
