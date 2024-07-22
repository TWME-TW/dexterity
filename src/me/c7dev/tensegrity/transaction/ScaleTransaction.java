package me.c7dev.tensegrity.transaction;

import java.util.List;

import org.bukkit.util.Vector;

import me.c7dev.tensegrity.api.DexRotation;
import me.c7dev.tensegrity.displays.DexterityDisplay;
import me.c7dev.tensegrity.util.DexBlock;

public class ScaleTransaction extends BlockTransaction {
	
	private Vector s1 = null, s2 = null;
	private DexterityDisplay disp;
	
	public ScaleTransaction(DexterityDisplay d) {
		super(d.getBlocks());
		disp = d;
		s1 = disp.getScale();
	}
	
	public void commit() {
		commit(disp.getBlocks());
	}
	
	@Override
	public void commit(List<DexBlock> blocks) {
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
