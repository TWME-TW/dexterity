package me.c7dev.tensegrity;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import me.c7dev.tensegrity.displays.DexterityDisplay;
import me.c7dev.tensegrity.displays.animation.Animation;
import me.c7dev.tensegrity.util.DexUtils;
import net.md_5.bungee.api.ChatColor;

public class DexSession {
	
	private Player p;
	private Location l1, l2;
	private List<DexterityDisplay> displays = new ArrayList<>();
	private DexterityDisplay selected = null;
	private Dexterity plugin;
	private ChatColor cc, cc2;
	private double click_cooldown = 0;
	
	public DexSession(Player player, Dexterity plugin) {
		p = player;
		this.plugin = plugin;
		cc = plugin.getChatColor(); cc2 = plugin.getChatColor2();
		plugin.setEditSession(player.getUniqueId(), this);
	}
	
	public Location getLocation1() {
		return l1;
	}
	public Location getLocation2() {
		return l2;
	}
	
	public List<DexterityDisplay> getEdits(){
		return displays;
	}
	public Player getPlayer() {
		return p;
	}
	
	public DexterityDisplay getSelected() {
		return selected;
	}
	public void setSelected(DexterityDisplay o) {
		selected = o;
	}
	
	public World getWorld() {
		return l1 == null ? null : l1.getWorld();
	}
	
	public void setLocation(Location loc, boolean is_l1) {
		int decimals = 3;
		if (System.currentTimeMillis() - click_cooldown < 100) return;
		click_cooldown = System.currentTimeMillis();
		
		loc.setX(loc.getBlockX());
		loc.setY(loc.getBlockY());
		loc.setZ(loc.getBlockZ());
		decimals = 0;
		loc.setYaw(0);
		loc.setPitch(0);
		
		World world = getWorld();
		if (world != null && !loc.getWorld().getName().equals(world.getName())) {
			p.sendMessage("§4Error: §cPoints must be set in the same world! Use /dex desel");
			return;
		}
		
		if (is_l1) l1 = loc;
		else l2 = loc;
			
		p.sendMessage(cc + "Set point #" + (is_l1 ? 1 : 2) + " to " + cc2 + DexUtils.locationString(loc, decimals));
	}
	
	public void clearLocationSelection() {
		l1 = null;
		l2 = null;
	}
	
	public void openAnimationEditor() {
		if (selected == null) return;
		
		int rows = Math.max(Math.min(selected.getAnimations().size(), 9), 3);
		Inventory inv = Bukkit.createInventory(null, 9*rows, plugin.getConfigString("animation-editor-title", "Animation Editor"));
		
		for (int i = 0; i < rows; i++) {
			inv.setItem(9*i, DexUtils.createItem(Material.GRAY_STAINED_GLASS_PANE, 1, "Animation " + (i+1)));
		}
		
		int j = 0;
		for (; j < selected.getAnimations().size(); j++) {
			
			if (j >= rows) break;
		}
		if (j < rows - 1) inv.setItem(j, DexUtils.createItem(Material.LIME_WOOL, 1, "§aAdd Next Animation"));
		
		p.openInventory(inv);
	}

}
