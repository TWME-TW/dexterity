package me.c7dev.tensegrity.transaction;

import me.c7dev.tensegrity.displays.DexterityDisplay;

public interface Transaction {

	public DexterityDisplay undo();
	
	public void redo();
	
	public boolean isCommitted();
	
	public boolean isUndone();
	
	public boolean isPossible();
	
}
