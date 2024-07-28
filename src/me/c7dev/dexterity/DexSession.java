package me.c7dev.dexterity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import com.sk89q.worldedit.regions.Region;

import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.transaction.BuildTransaction;
import me.c7dev.dexterity.transaction.RemoveTransaction;
import me.c7dev.dexterity.transaction.Transaction;
import me.c7dev.dexterity.util.DexBlock;
import me.c7dev.dexterity.util.DexUtils;
import me.c7dev.dexterity.util.OrientationKey;
import me.c7dev.dexterity.util.RollOffset;

public class DexSession {
	
	public enum EditType {
		TRANSLATE,
		CLONE,
		CLONE_MERGE,
	}
	
	private Player p;
	private Location l1, l2;
	private DexterityDisplay selected, secondary;
	private Dexterity plugin;
	private Vector3f editing_scale;
	private Vector following, l1_scale_offset, l2_scale_offset;
	private EditType editType;
	private Location orig_loc;
	private double volume = Integer.MAX_VALUE;
	private LinkedList<Transaction> toUndo = new LinkedList<>(), toRedo = new LinkedList<>(); //push/pop from first element
	private BuildTransaction t_build;
	private Material mask;
	
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
	
	public Material getMask() {
		return mask;
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
			p.sendMessage(plugin.getConfigString("selected-success").replaceAll("\\Q%label%\\E", o.getLabel()));
			if (plugin.getConfig().getBoolean("highlight-display-on-select")) plugin.api().tempHighlight(o, 15);
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
		if (following == null) return null;
		return following.clone();
	}
	public void setFollowingOffset(Vector v) {
		following = v.clone();
	}
	
	public void clearHistory() {
		toUndo.clear();
		toRedo.clear();
	}
	
	public void pushTransaction(Transaction t) {
		if (!t.isPossible() || t instanceof RemoveTransaction) {
			t_build = null;
			toUndo.clear();
		}
		toRedo.clear();
		if (t_build != null) {
			if (t_build.size() > 0) {
				t_build.commit();
				toUndo.addFirst(t_build);
			}
			if (t != t_build) toUndo.addFirst(t);
			t_build = null;
		}
		else toUndo.addFirst(t);
		trimToSize();
	}
	
	public void pushBlock(DexBlock db, boolean placing) {
		if (db.getDexterityDisplay() == null) return;
		if (t_build == null) t_build = new BuildTransaction(db.getDexterityDisplay());
		else if (!db.getDexterityDisplay().getUniqueId().equals(t_build.getDisplayUniqueId())) {
			t_build.commit();
			pushTransaction(t_build);
			t_build = new BuildTransaction(db.getDexterityDisplay());
		}
		
		if (placing) t_build.addBlock(db);
		else t_build.removeBlock(db);
	}
		
	public void undo() {
		executeUndo(1);
	}
	
	public void undo(int count) {
		if (count < 1) return;
		count = Math.max(Math.min(count, toUndo.size()), 1);
		for (int i = 0; i < count; i++) {
			executeUndo(i == count - 1 ? count : 0);
		}
	}
	
	public void redo() {
		executeRedo(1);
	}
	
	public void redo(int count) {
		if (count < 1) return;
		count = Math.max(Math.min(count, toRedo.size()), 1);
		for (int i = 0; i < count; i++) {
			executeRedo(i == count - 1 ? count : 0);
		}
	}
	
	private void trimToSize() {
		int max = plugin.getConfig().getInt("session-history-size");
		if (max <= 0) {
			toUndo.clear();
			return;
		}
		int toRemove = toUndo.size() - max;
		for (int i = 0; i < toRemove; i++) toUndo.removeLast();
	}
	
	private void executeUndo(int count) {
		if (t_build != null) {
			t_build.commit();
			pushTransaction(t_build);
			t_build = null;
		}
		
		if (toUndo.size() == 0) {
			if (count > 0) p.sendMessage(plugin.getConfigString("none-undo"));
			return;
		}
		
		if (!toUndo.getFirst().isCommitted()) {
			p.sendMessage(plugin.getConfigString("still-processing"));
			return;
		}
		
		Transaction undo = toUndo.removeFirst();
		
		if (!undo.isPossible()) {
			if (count > 0) p.sendMessage(plugin.getConfigString("cannot-undo"));
			return;
		}
		
		toRedo.addFirst(undo);
		DexterityDisplay set = undo.undo();
		
		if (set != null) {
			selected = set;
		}
		
		if (count > 0) {
			String msg = plugin.getConfigString("undo-success").replaceAll("\\Q%number%\\E", "" + count).replaceAll("\\Q(s)\\E", count == 1 ? "" : "s");
			p.sendMessage(msg);
		}
	}
	
	private void executeRedo(int count) {
		if (toRedo.size() == 0) {
			if (count > 0) p.sendMessage(plugin.getConfigString("none-redo"));
			return;
		}
		
		Transaction redo = toRedo.removeFirst();
		
		if (!redo.isPossible()) {
			if (count > 0) p.sendMessage(plugin.getConfigString("cannot-redo"));
			return;
		}
		
		
		toUndo.addFirst(redo);
		redo.redo();

		if (count > 0) {
			String msg = plugin.getConfigString("redo-success").replaceAll("\\Q%number%\\E", "" + count).replaceAll("\\Q(s)\\E", count == 1 ? "" : "s");
			p.sendMessage(msg);
		}
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
		if (selected != null && secondary != null) {
			selected.remove(false);
			selected = secondary;
			selected.setEditingLock(null);
			secondary = null;
		}
		if (editType == EditType.TRANSLATE && orig_loc != null) {
			if (secondary != null) secondary.teleport(orig_loc);
			else selected.teleport(orig_loc);
		}
		finishEdit();
	}
	
	public void finishEdit() {
		if (selected == null) return;
		if (secondary != null && editType != EditType.CLONE) selected = secondary;
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
		return Math.max(getSelectedVolumeSpace(), getSelectedVolumeCount());
	}
	public double getSelectedVolumeSpace() {
		if (volume == Integer.MAX_VALUE) return 0;
		return volume;
	}
	public double getSelectedVolumeCount() {
		return selected == null ? 0 : selected.getBlocks().size();
	}
	
	public void setLocation(Location loc, boolean is_l1) {
		setLocation(loc, is_l1, true);
	}
	
	public void setLocation(Location loc, boolean is_l1, boolean msg) {
		setContinuousLocation(DexUtils.blockLoc(loc), is_l1, is_l1 ? new Vector(0, 0, 0) : new Vector(1, 1, 1), msg);
	}
	
	public void setContinuousLocation(Location loc, boolean is_l1, Vector scale_offset, boolean msg) {
		
//		if (scale == null) DexUtils.blockLoc(loc);
//		else scale.multiply(0.5);

		
		if (is_l1) {
			l1 = loc;
			l1_scale_offset = scale_offset.clone().multiply(0.5);
		}
		else {
			l2 = loc;
			l2_scale_offset = scale_offset.clone().multiply(0.5);
		}

		selectFromLocations();
		
		if (msg) p.sendMessage(plugin.getConfigString("set-success").replaceAll("\\Q%number%\\E", is_l1 ? "1" : "2").replaceAll("\\Q%location%\\E", DexUtils.locationString(loc, 0)));
	}
	
	private void selectFromLocations() {
		if (editType == null) {
			if (l1 != null && l2 != null && l1.getWorld().getName().equals(l2.getWorld().getName())) {
				if (l1_scale_offset == null) l1_scale_offset = new Vector(0, 0, 0);
				if (l2_scale_offset == null) l2_scale_offset = new Vector(1, 1, 1);
				
//				double xmin = Math.min(l1.getX(), l2.getX()), xmax = Math.max(l1.getX(), l2.getX());
//				double ymin = Math.min(l1.getY(), l2.getY()), ymax = Math.max(l1.getY(), l2.getY());
//				double zmin = Math.min(l1.getZ(), l2.getZ()), zmax = Math.max(l1.getZ(), l2.getZ());
//
//				volume = Math.abs(xmax-xmin) * Math.abs(ymax-ymin) * Math.abs(zmax-zmin);

//				if (volume <= plugin.getMaxVolume()) { //set selected
				int maxvol = plugin.getMaxVolume();
				List<BlockDisplay> blocks = plugin.api().getBlockDisplaysInRegionContinuous(l1, l2, l1_scale_offset, l2_scale_offset);
				if (blocks.size() > 0) {
					DexterityDisplay s = new DexterityDisplay(plugin);
					List<DexBlock> dblocks = new ArrayList<>();
					HashMap<OrientationKey, RollOffset> roll_cache = new HashMap<>();
					
					for (BlockDisplay bd : blocks) {
						if (mask != null && bd.getBlock().getMaterial() != mask) continue;

						DexBlock db = plugin.getMappedDisplay(bd.getUniqueId());
						if (db == null) {
							db = new DexBlock(bd, s);
							db.loadRoll(roll_cache); //TODO possibly make this async
						}
						else if (db.getDexterityDisplay().isListed()) continue;
						if (db.getDexterityDisplay().getEditingLock() == null) db.setDexterityDisplay(s);
						dblocks.add(db);
						if (dblocks.size() >= maxvol) break;
					}

					if (dblocks.size() == 0) {
						setSelected(null, false);
						return;
					}

					s.setEntities(dblocks, true);

					highlightSelected(s);
					selected = s;
				}
			} else volume = Integer.MAX_VALUE;
		}
	}
	
	private void highlightSelected(DexterityDisplay new_disp) {
		if (!plugin.getConfig().getBoolean("highlight-display-on-select")) return;
		plugin.api().unTempHighlight(selected);
		plugin.api().tempHighlight(new_disp, 30);
	}
	
	public void setMask(Material mat) {
		if (mat == Material.AIR) mat = null;
		if (mask == mat) return;
		mask = mat;
		if (mat != null) {
			if (selected != null) {
				DexterityDisplay s = new DexterityDisplay(plugin, selected.getCenter(), selected.getScale());
				List<DexBlock> dblocks = new ArrayList<>();
				for (DexBlock db : selected.getBlocks()) {
					if (db.getEntity().getBlock().getMaterial() == mat) dblocks.add(db);
				}
				if (dblocks.size() == s.getBlocks().size()) selectFromLocations();

				if (dblocks.size() == 0) setSelected(null, false);
				else {
					s.setEntities(dblocks, true);
					highlightSelected(s);
					setSelected(s, false);
				}
			}
		} else selectFromLocations();
		
		
	}
	
	public void clearLocationSelection() {
		l1 = null;
		l2 = null;
	}
	
//	public void openAnimationEditor() {
//		if (selected == null) return;
//		
//		int rows = Math.max(Math.min(selected.getAnimations().size(), 9), 3);
//		Inventory inv = Bukkit.createInventory(null, 9*rows, plugin.getConfigString("animation-editor-title", "Animation Editor"));
//		
//		for (int i = 0; i < rows; i++) {
//			inv.setItem(9*i, DexUtils.createItem(Material.GRAY_STAINED_GLASS_PANE, 1, "Animation " + (i+1)));
//		}
//		
//		int j = 0;
//		for (; j < selected.getAnimations().size(); j++) {
//			
//			if (j >= rows) break;
//		}
//		if (j < rows - 1) inv.setItem(j, DexUtils.createItem(Material.LIME_WOOL, 1, "§aAdd Next Animation"));
//		
//		p.openInventory(inv);
//	}

}
