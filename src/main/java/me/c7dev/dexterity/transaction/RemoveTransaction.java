package me.c7dev.dexterity.transaction;

import java.util.ArrayList;
import java.util.List;

import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.util.DexBlock;
import me.c7dev.dexterity.util.DexBlockState;

public class RemoveTransaction implements Transaction {

	private DexterityDisplay disp;
	protected List<DexBlockState> states = new ArrayList<>();
	private String label;
	protected boolean isUndone = false, isCommitted = false;
	
	public RemoveTransaction(DexterityDisplay d) {
		if (d == null) return;
		isCommitted = true;
		disp = d;
		label = d.getLabel();
		for (DexBlock db : d.getBlocks()) {
			states.add(db.getState());
		}
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
	
	public DexterityDisplay undo() {
		if (isUndone) return null;
		isUndone = true;
		List<DexBlock> blocks = new ArrayList<>();
		for (DexBlockState state : states) {
			blocks.add(new DexBlock(state));
		}
		disp.setBlocks(blocks, false);
		if (label != null) disp.setLabel(label);
		return disp;
	}
	
	public void redo() {
		if (!isUndone) return;
		isUndone = false;
		
		disp.remove(false);
	}
	
}
