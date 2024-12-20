package me.c7dev.dexterity.api.events;

import me.c7dev.dexterity.DexSession;
import me.c7dev.dexterity.transaction.Transaction;

public class TransactionCompletionEvent extends TransactionEvent {
	
	public TransactionCompletionEvent(DexSession session, Transaction transaction) {
		super(session, transaction);
	}

}
