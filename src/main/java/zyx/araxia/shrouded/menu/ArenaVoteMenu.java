package zyx.araxia.shrouded.menu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import zyx.araxia.shrouded.lobby.Arena;
import zyx.araxia.shrouded.lobby.LobbySession;

/**
 * Factory for the arena-vote chest GUI.
 * <p>
 * One item is placed per candidate arena. The inventory size is always a
 * multiple of 9 and at least 9. Each item uses a {@link Material#FILLED_MAP}
 * icon with the arena name as its display name. Slots are centred in the first
 * row for up to 9 candidates.
 */
public class ArenaVoteMenu {

    private ArenaVoteMenu() {}

    /**
     * Opens the arena-vote menu for {@code player}.
     *
     * @param player     the player who should see the menu
     * @param session    the owning lobby session (stored in the holder)
     * @param candidates the arenas the player may vote for
     */
    public static void open(Player player, LobbySession session, List<Arena> candidates) {
        int size = Math.max(9, (int) Math.ceil(candidates.size() / 9.0) * 9);

        Map<Integer, Arena> slotMap = new HashMap<>();
        Inventory inv = Bukkit.createInventory(
                new ArenaVoteMenuHolder(session, slotMap),
                size,
                Component.text("Vote for an Arena", NamedTextColor.DARK_AQUA));

        // Centre items in the first row when there are fewer than 9 candidates
        int startSlot = (size == 9) ? (9 - candidates.size()) / 2 : 0;

        for (int i = 0; i < candidates.size(); i++) {
            Arena arena = candidates.get(i);
            int slot = startSlot + i;

            ItemStack item = new ItemStack(Material.FILLED_MAP);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(arena.getName(), NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Region: (" + arena.getX1() + ", " + arena.getY1() + ", " + arena.getZ1()
                            + ") → (" + arena.getX2() + ", " + arena.getY2() + ", " + arena.getZ2() + ")",
                            NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Click to vote!", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false)));
            item.setItemMeta(meta);

            inv.setItem(slot, item);
            slotMap.put(slot, arena);
        }

        player.openInventory(inv);
    }
}
