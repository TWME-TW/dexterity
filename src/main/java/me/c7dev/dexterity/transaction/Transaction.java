package me.c7dev.dexterity.transaction;

import me.c7dev.dexterity.displays.DexterityDisplay;

public interface Transaction {

	public DexterityDisplay undo();
	
	public void redo();
	
	public boolean isCommitted();
	
	public boolean isUndone();
	
	public boolean isPossible();
	
}
