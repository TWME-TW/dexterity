package me.c7dev.dexterity.transaction;

import java.util.UUID;

import me.c7dev.dexterity.Dexterity;
import me.c7dev.dexterity.util.DexBlock;
import me.c7dev.dexterity.util.DexBlockState;

/**
 * Stores the old and new state of a particular {@link DexBlock}
 */
public class BlockTransactionLine {
	
	private DexBlock db;
	private DexBlockState from, to = null; //if to is null and committed, old block was deleted
	private boolean isUndone = false, committed = false;

	public BlockTransactionLine(DexBlock block) { //TODO make this more advanced with deltas
		this.db = block;
		this.from = block.getState();
	}
	
	public boolean isCommitted() {
		return committed;
	}
	
	public void commit(DexBlockState to) {
		if (committed) return;
		this.to = to;
		committed = true;
	}
	
	public DexBlockState getFromState() {
		return from;
	}
	
	public void refresh(Dexterity plugin) {
		if (to == null || (db != null && db.getEntity().isDead())) db = plugin.getMappedDisplay(db.getUniqueId());
	}
	
	public UUID undo() {
		if (!committed || isUndone) return null;
		isUndone = true;
		UUID ret = null;
		
		if (to == null || (db != null && db.getEntity().isDead())) {
			db = new DexBlock(from);
			ret = db.getUniqueId();
		} else if (db != null) {
			db.loadState(from);
		}
		return ret;
	}
	
	public void redo() {
		if (!committed || !isUndone) return;
		isUndone = false;
		
		if (to != null) {
			if (db.getEntity().isDead()) db = new DexBlock(to);
			else db.loadState(to);
		} else db.remove(); //TODO test that dex display is set correctly on dexblock after undo/redo transactions
		
	}
	
}
