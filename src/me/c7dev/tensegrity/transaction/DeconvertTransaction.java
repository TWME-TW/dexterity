package me.c7dev.tensegrity.transaction;

import org.bukkit.Material;
import org.bukkit.block.Block;

import me.c7dev.tensegrity.displays.DexterityDisplay;
import me.c7dev.tensegrity.util.DexBlockState;

public class DeconvertTransaction extends RemoveTransaction {
	
//	private List<SavedBlockState> blocks = new ArrayList<>();
	
	public DeconvertTransaction(DexterityDisplay d) {
		super(d);
//		for (DexBlock db : d.getBlocks()) {
//			blocks.add(new SavedBlockState(db.getLocation(), db.getEntity().getBlock()));
//		}
		isCommitted = true;
	}
	
	@Override
	public DexterityDisplay undo() {
		DexterityDisplay d = super.undo();
		
//		for (SavedBlockState b : blocks) {
//			b.getLocation().getBlock().setType(Material.AIR);
//		}
		for (DexBlockState state : states) {
			state.getLocation().getBlock().setType(Material.AIR);
		}
		
		return d;
	}
	
	@Override
	public void redo() {
		super.redo();
//		for (SavedBlockState state : blocks) {
//			Block b = state.getLocation().getBlock();
//			b.setType(state.getMaterial());
//			b.setBlockData(state.getData());
//		}
		for (DexBlockState state : states) {
			Block b = state.getLocation().getBlock();
			b.setBlockData(state.getBlock());
		}
	}

}
