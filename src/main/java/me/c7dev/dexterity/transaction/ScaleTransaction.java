package me.c7dev.dexterity.transaction;

import org.bukkit.util.Vector;

import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.util.DexBlock;

public class ScaleTransaction extends BlockTransaction {
	
	private Vector s1 = null, s2 = null;
	private DexterityDisplay disp;
	
	public ScaleTransaction(DexterityDisplay d) {
		super(d);
		disp = d;
		s1 = disp.getScale();
	}
	
	public void commit() {
		commit(disp.getBlocks());
	}
	
	public void commitEmpty() { //reset scale
		s2 = disp.getScale();
		trans.clear();
		isCommitted = true;
	}
	
	@Override
	public void commit(DexBlock[] blocks) {
		super.commit(blocks);
		s2 = disp.getScale();
	}
	
	@Override
	public DexterityDisplay undo() {
		if (!super.isCommitted || super.isUndone) return null;
		DexterityDisplay d = super.undo();
		if (d != null) disp = d;
		disp.resetScale(s1);
		return d;
	}
	
	@Override
	public void redo() {
		if (!super.isCommitted || !super.isUndone) return;
		super.redo();
		disp.resetScale(s2);
	}

}
