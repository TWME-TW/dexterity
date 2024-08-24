package me.c7dev.dexterity.api.events;

import me.c7dev.dexterity.DexSession;
import me.c7dev.dexterity.transaction.Transaction;

public class TransactionUndoEvent extends TransactionEvent {
	
	public TransactionUndoEvent(DexSession session, Transaction transaction) {
		super(session, transaction);
	}


}
