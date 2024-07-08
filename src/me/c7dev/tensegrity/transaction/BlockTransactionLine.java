package me.c7dev.tensegrity.transaction;

import java.util.UUID;

import me.c7dev.tensegrity.util.DexBlock;
import me.c7dev.tensegrity.util.DexBlockState;

public class BlockTransactionLine {
	
	private DexBlock db;
	private DexBlockState from, to = null; //if to is null and committed, old block was deleted
	private boolean isUndone = false, committed = false;

	public BlockTransactionLine(DexBlock block) {
		this.db = block;
		this.from = block.getState();
	}
	
	public void commit(DexBlockState to) {
		if (committed) return;
		this.to = to;
		committed = true;
	}
	
	public DexBlockState getFromState() {
		return from;
	}
	
	public UUID undo() {
		if (!committed || isUndone) return null;
		isUndone = true;
		UUID ret = null;
		
		if (to == null || db.getEntity().isDead()) {
			db = new DexBlock(from);
			ret = db.getEntity().getUniqueId();
		} else {
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
