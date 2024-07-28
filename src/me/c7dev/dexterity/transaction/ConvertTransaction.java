package me.c7dev.dexterity.transaction;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.Block;

import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.util.DexBlock;
import me.c7dev.dexterity.util.SavedBlockState;

public class ConvertTransaction implements Transaction {
	
	private boolean isCommitted = false, isUndone = false;
	private List<SavedBlockState> blocks = new ArrayList<>();
	private List<DexBlock> dexblocks = new ArrayList<>();
	private DexterityDisplay disp = null;

	public ConvertTransaction() {
	}
	
	public void addBlock(SavedBlockState from, DexBlock to) {
		blocks.add(from);
		dexblocks.add(to);
		isCommitted = true;
	}
	
	public DexterityDisplay undo() {
		if (!isCommitted || isUndone) return null;
		isUndone = true;
		for (DexBlock db : dexblocks) db.remove();
		dexblocks.clear();
		for (SavedBlockState state : blocks) {
			Block b = state.getLocation().getBlock();
			b.setType(state.getMaterial());
			b.setBlockData(state.getData());
		}
		return null;
	}
	
	public void redo() {
//		if (!isCommitted || !isUndone) return null;
//		isUndone = false;
//		disp = new DexterityDisplay(plugin);
//		for (SavedBlockState state : blocks) {
//			Block b = state.getLocation().getBlock();
//			DexBlock db = new DexBlock(b, disp);
//			dexblocks.add(db);
//		}
//		return disp;
	}
	
	public boolean isUndone() {
		return isUndone;
	}
	
	public boolean isCommitted() {
		return isCommitted;
	}
	
	public boolean isPossible() {
		return !isCommitted || !isUndone;
	}
	
}
