package me.c7dev.tensegrity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import me.c7dev.tensegrity.displays.DexterityDisplay;
import me.c7dev.tensegrity.util.DexBlock;
import me.c7dev.tensegrity.util.DexUtils;

public class DexSession {
	
	public enum EditType {
		TRANSLATE,
		CLONE,
		MERGE,
		DEEPMERGE,
		SCALE,
		ROTATE
	}
	
	private Player p;
	private Location l1, l2;
	private DexterityDisplay selected = null, secondary = null;
	private Dexterity plugin;
	private Vector3f editing_scale = null;
	private Vector following = null;
	private EditType editType = null;
	private Location orig_loc = null;
	private double volume = Integer.MAX_VALUE;
	
	public DexSession(Player player, Dexterity plugin) {
		p = player;
		this.plugin = plugin;
		plugin.setEditSession(player.getUniqueId(), this);
	}
	
	public Location getLocation1() {
		return l1;
	}
	public Location getLocation2() {
		return l2;
	}
	
	public Vector3f getEditingScale() {
		return editing_scale;
	}
	public void setEditingScale(Vector3f scale) {
		editing_scale = scale;
	}
	
	public Player getPlayer() {
		return p;
	}
	
	public DexterityDisplay getSelected() {
		return selected;
	}
	public DexterityDisplay getSecondary() {
		return secondary;
	}
	public void setSelected(DexterityDisplay o, boolean msg) {
		if (o == null) {
			cancelEdit();
			selected = null;
			return;
		}
		UUID editing_lock = null;
		if (selected != null) editing_lock = o.getEditingLock();
		if (editing_lock != null) {
			Player editor = Bukkit.getPlayer(editing_lock);
			if (editor == null) o.setEditingLock(null);
			else {
				if (editing_lock.equals(p.getUniqueId())) p.sendMessage(plugin.getConfigString("must-finish-edit"));
				else p.sendMessage(plugin.getConfigString("cannot-select-with-edit").replaceAll("\\Q%editor%\\E", editor.getName()));
				return;
			}
		}
		
		selected = o;
		if (msg && o.getLabel() != null && p.isOnline()) {
			p.sendMessage(plugin.getConfigString("selected").replaceAll("\\Q%label%\\E", o.getLabel()));
			if (plugin.getConfig().getBoolean("highlight-display-on-select")) plugin.getAPI().tempHighlight(o, 15);
		}
	}
	
	public boolean isFollowing() {
		return following != null;
	}
	
	public void startFollowing() {
		if (selected == null) return;
		following = selected.getCenter().toVector().subtract(DexUtils.blockLoc(p.getLocation()).toVector());
	}
	
	public void stopFollowing() {
		following = null;
	}
	
	public Vector getFollowingOffset() {
		return following.clone();
	}
	
	public void startEdit(DexterityDisplay d, EditType type, boolean swap) {
		if (selected == null || editType != null) return;
		editType = type;
		if (d != selected) {
			if (swap) {
				secondary = selected;
				secondary.setEditingLock(p.getUniqueId());
				selected = d;
			} else {
				setSelected(d, false);
			}
		} else selected.setEditingLock(p.getUniqueId());
		orig_loc = d.getCenter();
	}
	
	public void cancelEdit() {
		if (selected == null) return;
		if (selected != null && secondary != null) selected.remove(false);
		if (editType == EditType.TRANSLATE && orig_loc != null) {
			if (secondary != null) secondary.teleport(orig_loc);
			else selected.teleport(orig_loc);
		}
		finishEdit();
	}
	
	public void finishEdit() {
		if (selected == null) return;
		if (secondary != null) selected = secondary;
		editType = null;
		secondary = null;
		following = null;
		selected.setEditingLock(null);
		stopFollowing();
	}
	
	public EditType getEditType() {
		return editType;
	}
	
	public World getWorld() {
		return l1 == null ? null : l1.getWorld();
	}
	
	public double getSelectionVolume() {
		return volume;
	}
	
	public void setLocation(Location loc, boolean is_l1) {
		setLocation(loc, is_l1, null);
	}
	
	public void setLocation(Location loc, boolean is_l1, Vector scale) {
		
		if (scale == null) DexUtils.blockLoc(loc);
		else scale.multiply(0.5);

		if (is_l1) l1 = loc;
		else l2 = loc;

		if (editType == null) {
			if (l1 != null && l2 != null && l1.getWorld().getName().equals(l2.getWorld().getName())) {
				double xmin = Math.min(l1.getX(), l2.getX()), xmax = Math.max(l1.getX(), l2.getX());
				double ymin = Math.min(l1.getY(), l2.getY()), ymax = Math.max(l1.getY(), l2.getY());
				double zmin = Math.min(l1.getZ(), l2.getZ()), zmax = Math.max(l1.getZ(), l2.getZ());

				volume = Math.abs(xmax-xmin) * Math.abs(ymax-ymin) * Math.abs(zmax-zmin);

				if (volume <= plugin.getMaxVolume()) { //set selected
					List<BlockDisplay> blocks = plugin.getAPI().getBlockDisplaysInRegion(l1, l2, scale);
					if (blocks.size() > 0) {
						DexterityDisplay s = new DexterityDisplay(plugin);
						List<DexBlock> dblocks = new ArrayList<>();
						for (BlockDisplay bd : blocks) {
							dblocks.add(new DexBlock(bd, s));
						}
						s.setEntities(dblocks, true);

						if (selected != null) {
							for (DexBlock db : selected.getBlocks()) {
								if (plugin.getAPI().isInProcess(db.getEntity().getUniqueId())) db.getEntity().setGlowing(false);
							}
						}
						if (plugin.getConfig().getBoolean("highlight-display-on-select")) plugin.getAPI().tempHighlight(s, 30);
						selected = s;
					}
				}
			} else volume = Integer.MAX_VALUE;
		}
		
		p.sendMessage(plugin.getConfigString("set-success").replaceAll("\\Q%number%\\E", is_l1 ? "1" : "2").replaceAll("\\Q%location%\\E", DexUtils.locationString(loc, 0)));
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
