package zyx.araxia.shrouded.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import zyx.araxia.shrouded.item.ShroudedItems;
import zyx.araxia.shrouded.lobby.ArenaManager;
import zyx.araxia.shrouded.lobby.LobbyManager;

/**
 * Handles right-click use of the {@link ShroudedItems#TYPE_RETURN_TO_LOBBY
 * Return to Lobby} item.
 * <p>
 * The item can only be activated while the player is physically located inside
 * a registered arena region. On use, the player is removed from their lobby
 * session.
 */
public class ReturnToLobbyListener implements Listener {

	private final LobbyManager lobbyManager;
	private final ArenaManager arenaManager;

	public ReturnToLobbyListener(LobbyManager lobbyManager,
			ArenaManager arenaManager) {
		this.lobbyManager = lobbyManager;
		this.arenaManager = arenaManager;
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Action action = event.getAction();
		if (action != Action.RIGHT_CLICK_AIR
				&& action != Action.RIGHT_CLICK_BLOCK)
			return;

		// Only handle the main hand to avoid firing twice
		if (event.getHand() != EquipmentSlot.HAND)
			return;

		ItemStack item = event.getItem();
		if (item == null || !ShroudedItems.isShroudedItem(item))
			return;

		String type = item.getItemMeta().getPersistentDataContainer()
				.get(ShroudedItems.ITEM_TYPE, PersistentDataType.STRING);
		if (!ShroudedItems.TYPE_RETURN_TO_LOBBY.equals(type))
			return;

		event.setCancelled(true);

		Player player = event.getPlayer();

		// Enforce that the player is physically inside a registered arena
		if (arenaManager.getArenaContaining(player.getLocation()) == null) {
			player.sendActionBar(Component.text(
					"You must be inside an arena to return to lobby.",
					NamedTextColor.RED));
			return;
		}

		boolean removed = lobbyManager.removePlayerFromSession(player);
		if (removed) {
			player.sendMessage(Component.text("You have returned to the lobby.",
					NamedTextColor.YELLOW));
		} else {
			player.sendActionBar(Component.text(
					"You are not in an active session.", NamedTextColor.RED));
		}
	}
}
