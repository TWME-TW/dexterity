package me.c7dev.dexterity.transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;

import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.util.DexBlock;
import me.c7dev.dexterity.util.DexBlockState;

public class BuildTransaction implements Transaction {
	
	private List<DexBlock> blocks = new ArrayList<>();
	private List<DexBlockState> states = new ArrayList<>();
	private boolean isUndone = false, isCommitted = false;
	private DexterityDisplay disp;
	
	public BuildTransaction(DexterityDisplay d) {
		this.disp = d;
	}
	
	public boolean isPossible() {
		return true;
	}
	
	public boolean isUndone() {
		return isUndone;
	}
	
	public boolean isCommitted() {
		return isCommitted;
	}
	
	public void addBlock(DexBlock db) {
		if (isCommitted) return;
		blocks.add(db);
	}
	
	public void removeBlock(DexBlock db) {
		if (isCommitted) return;
		if (!blocks.remove(db)) {
			states.add(db.getState());
		}
	}
	
	public int size() {
		return Math.max(blocks.size(), states.size());
	}
	
	public void commit() {
		isCommitted = true;
	}
	
	public UUID getDisplayUniqueId() {
		return disp.getUniqueId();
	}
	
	public DexterityDisplay undo() {
		if (isUndone || !isCommitted) return null;
		isUndone = true;
		List<DexBlock> to_add = new ArrayList<>();
		for (DexBlockState s : states) {
			DexBlock db = new DexBlock(s);
			to_add.add(db);
		}
		states.clear();
		
		for (DexBlock db : blocks) {
			states.add(db.getState());
			db.remove();
		}
		blocks = to_add;
		return null;
	}
	
	public void redo() {
		if (!isUndone || !isCommitted) return;
		isUndone = false;
		List<DexBlockState> to_add = new ArrayList<>();
		for (DexBlock db : blocks) {
			to_add.add(db.getState());
			db.remove();
		}
		
		blocks.clear();
		for (DexBlockState s : states) {
			DexBlock db = new DexBlock(s);
			blocks.add(db);
		}
		states = to_add;
	}
	

}
