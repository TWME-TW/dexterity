package me.c7dev.dexterity.api.events;

import me.c7dev.dexterity.DexSession;
import me.c7dev.dexterity.transaction.Transaction;

public class TransactionRedoEvent extends TransactionEvent {
	
	public TransactionRedoEvent(DexSession session, Transaction transaction) {
		super(session, transaction);
	}

}
