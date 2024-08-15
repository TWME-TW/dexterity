package me.c7dev.dexterity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import com.sk89q.worldedit.regions.Region;

import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.transaction.BlockTransaction;
import me.c7dev.dexterity.transaction.BuildTransaction;
import me.c7dev.dexterity.transaction.RemoveTransaction;
import me.c7dev.dexterity.transaction.Transaction;
import me.c7dev.dexterity.util.DexBlock;
import me.c7dev.dexterity.util.DexUtils;
import me.c7dev.dexterity.util.Mask;

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
	private Transaction editTransaction;
	private Location orig_loc;
	private double volume = Integer.MAX_VALUE;
	private LinkedList<Transaction> toUndo = new LinkedList<>(), toRedo = new LinkedList<>(); //push/pop from first element
	private BuildTransaction t_build;
	private Mask mask;
	private boolean cancel_physics = false;
	
	/**
	 * Initializes a new session for a player
	 * @param player
	 * @param plugin
	 */
	public DexSession(Player player, Dexterity plugin) {
		if (player == null || plugin == null || !player.isOnline()) throw new IllegalArgumentException("Player must be online!");
		p = player;
		this.plugin = plugin;
		plugin.setEditSession(player.getUniqueId(), this);
		
		if (plugin.usingWorldEdit()) {
			Region r = plugin.getSelection(player);
			if (r != null) {
				if (r.getMinimumPoint() != null) l1 = DexUtils.location(p.getWorld(), r.getMinimumPoint());
				if (r.getMaximumPoint() != null) l2 = DexUtils.location(p.getWorld(), r.getMaximumPoint());
				selectFromLocations();
			}
		}
	}
	
	/**
	 * Retrieves first location set by player
	 * @return
	 */
	public Location getLocation1() {
		return l1;
	}
	/**
	 * Retrieves second location set by player
	 * @return
	 */
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
	
	public Mask getMask() {
		return mask;
	}
	
	public DexterityDisplay getSelected() {
		return selected;
	}
	
	public DexterityDisplay getSecondary() {
		return secondary;
	}
	
	/**
	 * Gets whether the blocks between the 2 locations have physics updates
	 * @return true if block physics events should be cancelled
	 */
	public boolean isCancellingPhysics() {
		return (hasLocationsSet() && cancel_physics);
	}
	
	/**
	 * Sets whether the blocks between the 2 locations have physics updates
	 * @param b
	 */
	public void setCancelPhysics(boolean b) {
		cancel_physics = b;
	}
	
	/**
	 * Changes the player's selection
	 * @param o The new selection, or null for no selection
	 * @param msg true if the player should be notified in chat
	 */
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
			plugin.api().tempHighlight(o, 15);
		}
	}
	
	/**
	 * Returns true if the selection is following the player, such as in a translation edit
	 * @return
	 * @see #startFollowing()
	 * @see #stopFollowing()
	 */
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
	
	/**
	 * Deletes the edit history, removing any undo or redo transactions
	 */
	public void clearHistory() {
		toUndo.clear();
		toRedo.clear();
	}
	
	/**
	 * Adds an edit transaction to the stack
	 * @param t
	 */
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
	
	/**
	 * Adds a modified block to the working {@link BuildTransaction}
	 * @param db
	 * @param placing true if player is placing the {@link DexBlock}, false if breaking
	 */
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
		
	/**
	 * Executes 1 undo
	 */
	public void undo() {
		executeUndo(1);
	}
	
	/**
	 * Executes a number of undo(s)
	 * @param count The number of undos to execute
	 */
	public void undo(int count) {
		if (count < 1) return;
		count = Math.max(Math.min(count, toUndo.size()), 1);
		for (int i = 0; i < count; i++) {
			executeUndo(i == count - 1 ? count : 0);
		}
	}
	
	/**
	 * Executes 1 redo
	 */
	public void redo() {
		executeRedo(1);
	}
	
	/**
	 * Executes a number of redo(s)
	 * @param count The number of redos to execute
	 */
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
	
	/**
	 * Enters the player into an edit session
	 * @param d The new selection to set as primary
	 * @param type
	 * @param swap If true and there exists a selection already, current selection will be reselected after edit session is over
	 */
	public void startEdit(DexterityDisplay d, EditType type, boolean swap) {
		startEdit(d, type, swap, null);
	}
	
	/**
	 * Enters the player into an edit session
	 * @param d The new selection to set as primary
	 * @param type
	 * @param swap If true and there exists a selection already, current selection will be reselected after edit session is over
	 * @param t Transaction to commit any blocks to during edit session
	 */
	public void startEdit(DexterityDisplay d, EditType type, boolean swap, Transaction t) {
		if (selected == null || editType != null) return;
		editType = type;
		editTransaction = t;
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
	
	/**
	 * Removes player from any edit session and restore to previous state
	 */
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
		editTransaction = null;
		finishEdit();
	}
	
	/**
	 * Completes the edit session
	 */
	public void finishEdit() {
		if (selected == null) return;
		if (secondary != null) {
			secondary.setEditingLock(null);
			if (editType != EditType.CLONE) selected = secondary;
		}
		secondary = null;
		following = null;
		selected.setEditingLock(null);
		stopFollowing();
		if (editTransaction != null) {
			switch (editType) {
			case TRANSLATE:
				BlockTransaction t = (BlockTransaction) editTransaction;
				t.commit(selected.getBlocks());
				t.commitCenter(selected.getCenter());
			default:
			}
			pushTransaction(editTransaction);
		}
		editType = null;
	}
	
	public EditType getEditType() {
		return editType;
	}
	
	public World getWorld() {
		return l1 == null ? null : l1.getWorld();
	}
	
	/**
	 * Returns the max between number of entities and volume between 2 locations
	 * @return
	 */
	public double getSelectionVolume() {
		return Math.max(getSelectedVolumeSpace(), getSelectedVolumeCount());
	}
	
	/**
	 * Returns true if locations are set and valid.
	 * @return
	 */
	public boolean hasLocationsSet() {
		return (l1 != null && l2 != null && l1.getWorld().getName().equals(l2.getWorld().getName()));
	}
	
	/**
	 * Returns the number of blocks of volume between 2 locations cuboid
	 * @return
	 */
	public double getSelectedVolumeSpace() {
		if (hasLocationsSet()) return volume;
		else return 0;
	}
	
	/**
	 * Returns the largest volume defined by the dexterity.maxvolume.# permission
	 * @return Min of configured max volume and volume from permissions
	 */
	public double getPermittedVolume() {
		for (PermissionAttachmentInfo perm : p.getEffectivePermissions()) {
			if (perm.getPermission().startsWith("dexterity.maxvolume.")) {
				try {
					double r = Double.parseDouble(perm.getPermission().replaceAll("dexterity\\.maxvolume\\.", ""));
					if (r < plugin.getMaxVolume()) return r;
					break;
				} catch (Exception ex) {
					Bukkit.getLogger().warning("Permission '" + perm.getPermission() + "' is invalid!");
					break;
				}
			}
		}
		return plugin.getMaxVolume();
	}
	
	/**
	 * Returns the number of {@link DexBlock} in the selection
	 * @return Integer count of blocks or 0 if nothing selected
	 */
	public double getSelectedVolumeCount() {
		return selected == null ? 0 : selected.getBlocks().length;
	}
	
	/**
	 * Sets the first or second location to a block
	 * @param loc
	 * @param is_l1 true if setting the first location
	 */
	public void setLocation(Location loc, boolean is_l1) {
		setLocation(loc, is_l1, true);
	}
	
	/**
	 * Sets the first or second location to a block
	 * @param loc
	 * @param is_l1 true if setting the first location
	 * @param msg true if player should be notified in chat
	 */
	public void setLocation(Location loc, boolean is_l1, boolean msg) {
		setContinuousLocation(DexUtils.blockLoc(loc), is_l1, is_l1 ? new Vector(0, 0, 0) : new Vector(1, 1, 1), msg);
		if (hasLocationsSet()) volume = DexUtils.getBlockVolume(l1, l2);
	}
	
	/**
	 * Precisely sets the first or second location
	 * @param loc
	 * @param is_l1 true if setting the first location
	 * @param scale_offset The offset added to the minimum or maximum coordinate once both locations are set
	 * @param msg true if player should be notified in chat
	 */
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
			if (hasLocationsSet()) {
				if (getSelectedVolumeSpace() > Math.min(plugin.getMaxVolume(), getPermittedVolume())) {
					setSelected(null, false);
					return;
				}
				DexterityDisplay d = plugin.api().selectFromLocations(l1, l2, mask, l1_scale_offset, l2_scale_offset);
				if (d == null) setSelected(null, false);
				else {
					highlightSelected(d);
					selected = d;
				}
				volume = DexUtils.getVolume(l1.clone().add(l1_scale_offset), l2.clone().add(l2_scale_offset));
			} else volume = 0;
		}
	}
	
	private void highlightSelected(DexterityDisplay new_disp) {
		if (selected != null) plugin.api().unTempHighlight(selected);
		plugin.api().tempHighlight(new_disp, 30);
	}
	
	public void setMask(Mask mask) {
		this.mask = mask;
		if (mask != null) {
			if (selected != null) {
				DexterityDisplay s = new DexterityDisplay(plugin, selected.getCenter(), selected.getScale());
				List<DexBlock> dblocks = new ArrayList<>();
				for (DexBlock db : selected.getBlocks()) {
					if (mask.isAllowed(db.getEntity().getBlock().getMaterial())) dblocks.add(db);
				}
				if (dblocks.size() == s.getBlocks().length) selectFromLocations();

				if (dblocks.size() == 0) setSelected(null, false);
				else {
					s.setBlocks(dblocks, true);
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
	
}
