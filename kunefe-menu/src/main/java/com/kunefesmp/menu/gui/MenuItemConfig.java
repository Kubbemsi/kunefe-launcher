package com.kunefesmp.menu.gui;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class MenuItemConfig {

    private final String id;
    private final List<Integer> slots;
    private final Material material;
    private final String name;
    private final List<String> lore;
    private final int amount;
    private final boolean glow;
    private final String skullOwner;
    private final int customModelData;
    private final List<String> actions;

    public MenuItemConfig(String id, List<Integer> slots, Material material, String name,
                          List<String> lore, int amount, boolean glow, String skullOwner,
                          int customModelData, List<String> actions) {
        this.id = id;
        this.slots = slots != null ? slots : new ArrayList<>();
        this.material = material != null ? material : Material.STONE;
        this.name = name != null ? name : "";
        this.lore = lore != null ? lore : new ArrayList<>();
        this.amount = Math.max(1, amount);
        this.glow = glow;
        this.skullOwner = skullOwner;
        this.customModelData = customModelData;
        this.actions = actions != null ? actions : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public List<Integer> getSlots() {
        return slots;
    }

    public Material getMaterial() {
        return material;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public int getAmount() {
        return amount;
    }

    public boolean isGlow() {
        return glow;
    }

    public String getSkullOwner() {
        return skullOwner;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public List<String> getActions() {
        return actions;
    }
}
