package me.c7dev.dexterity.transaction;

import java.util.List;

import org.bukkit.util.Vector;

import me.c7dev.dexterity.api.DexRotation;
import me.c7dev.dexterity.displays.DexterityDisplay;
import me.c7dev.dexterity.util.DexBlock;

public class RotationTransaction extends BlockTransaction {
	
	private Vector x1 = null, y1 = null, z1 = null, x2 = null, y2 = null, z2 = null;
	private DexterityDisplay disp;
	
	public RotationTransaction(DexterityDisplay d) {
		super(d);
		disp = d;
		DexRotation r = d.getRotationManager(true);
		x1 = r.getXAxis();
		y1 = r.getYAxis();
		z1 = r.getZAxis();
	}
	
	public void commit() {
		commit(disp.getBlocks());
	}
	
	@Override
	public void commit(DexBlock[] blocks) {
		super.commit(blocks);
		DexRotation r = disp.getRotationManager();
		x2 = r.getXAxis();
		y2 = r.getYAxis();
		z2 = r.getZAxis();
	}
	
	@Override
	public DexterityDisplay undo() {
		if (!super.isCommitted || super.isUndone) return null;
		DexterityDisplay d = super.undo();
		if (d != null) disp = d;
		disp.getRotationManager().setAxes(x1, y1, z1);
		return d;
	}
	
	@Override
	public void redo() {
		if (!super.isCommitted || !super.isUndone) return;
		super.redo();
		disp.getRotationManager().setAxes(x2, y2, z2);
	}
	

}
