package zyx.araxia.shrouded.game;

import org.bukkit.Material;

public enum PlayerClass {

    SHROUDED(
            "Shrouded",
            "A stealthy combatant who moves unseen through the darkness.",
            Material.LEATHER_CHESTPLATE
    );

    private final String displayName;
    private final String description;
    private final Material icon;

    PlayerClass(String displayName, String description, Material icon) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public Material getIcon()      { return icon; }
}
