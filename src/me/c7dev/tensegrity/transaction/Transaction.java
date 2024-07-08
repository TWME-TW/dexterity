package me.c7dev.tensegrity.transaction;

public interface Transaction {

	public void undo();
	
	public void redo();
	
	public boolean isCommitted();
	
	public boolean isUndone();
	
	public boolean isPossible();
	
}
