package zyx.araxia.shrouded.game;

import org.bukkit.Material;

public enum PlayerClass {

    SHROUDED(
            "Shrouded",
            "A stealthy combatant who moves unseen through the darkness.",
            Material.LEATHER_CHESTPLATE
    ),
    SURVIVOR(
            "Survivor",
            "A well-rounded fighter with no special tools. Relies on teamwork and awareness.",
            Material.IRON_CHESTPLATE
    );

    private final String displayName;
    private final String description;
    private final Material icon;

    PlayerClass(String displayName, String description, Material icon) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Material getIcon() {
        return icon;
    }

    /**
     * Returns true if this class is the special hidden Shrouded role.
     */
    public boolean isShroudedRole() {
        return this == SHROUDED;
    }

    /**
     * Returns all classes that can be randomly assigned to regular players.
     */
    public static PlayerClass[] regularClasses() {
        return java.util.Arrays.stream(values())
                .filter(c -> !c.isShroudedRole())
                .toArray(PlayerClass[]::new);
    }
}
