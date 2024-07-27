package me.c7dev.tensegrity.transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;

import me.c7dev.tensegrity.displays.DexterityDisplay;
import me.c7dev.tensegrity.util.DexBlock;

public class BlockTransaction implements Transaction {
	
	protected HashMap<UUID, BlockTransactionLine> trans = new HashMap<>();
	protected boolean isUndone = false, isCommitted = false;
	
	public BlockTransaction() {
	}
	
	public BlockTransaction(List<DexBlock> blocks) {
		this(blocks, null);
	}
	
	public BlockTransaction(List<DexBlock> blocks, Material mat) {
		if (mat == null) {
			for (DexBlock db : blocks) trans.put(db.getEntity().getUniqueId(), new BlockTransactionLine(db));
		} else {
			for (DexBlock db : blocks) {
				if (db.getEntity().getBlock().getMaterial() == mat) trans.put(db.getEntity().getUniqueId(), new BlockTransactionLine(db));
			}
		}
	}
	
	public void addBlock(DexBlock block) {
		if (trans.containsKey(block.getEntity().getUniqueId())) return;
		trans.put(block.getEntity().getUniqueId(), new BlockTransactionLine(block));
	}
	
	public void commitBlock(DexBlock db) {
		BlockTransactionLine t = trans.get(db.getEntity().getUniqueId());
		if (t == null) return;
		
		t.commit(db.getState());
		isCommitted = true;
	}
	
	public void commit(List<DexBlock> blocks) {
		commit(blocks, null);
	}
	public void commit(List<DexBlock> blocks, Material mask) {
		if (isCommitted) return;
		isCommitted = true;
		if (mask == null) {
			for (DexBlock db : blocks) commitBlock(db);
		} else {
			for (DexBlock db : blocks) {
				if (db.getEntity().getBlock().getMaterial() == mask) commitBlock(db);
			}
		}
	}
	public void commitEmpty() { //no blocks
		isCommitted = true;
	}
	
	public DexterityDisplay undo() {
		if (!isCommitted || isUndone) return null;
		isUndone = true;
		for (Entry<UUID, BlockTransactionLine> entry : trans.entrySet()) entry.getValue().undo();
		return null;
	}
	
	public void redo() {
		if (!isCommitted || !isUndone) return;
		isUndone = false;
		
		for (Entry<UUID, BlockTransactionLine> entry : trans.entrySet()) entry.getValue().redo();
	}
	
	public boolean isPossible() {
		return true;
	}
	
	public boolean isCommitted() {
		return isCommitted;
	}
	
	public boolean isUndone() {
		return isUndone;
	}

}
