package me.c7dev.dexterity.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import me.c7dev.dexterity.DexSession;
import me.c7dev.dexterity.transaction.Transaction;

public class TransactionCompletionEvent extends Event {
	
	private Transaction t;
	private DexSession s;
	
	public TransactionCompletionEvent(DexSession session, Transaction transaction) {
		s = session;
		t = transaction;
	}
	
	public Transaction getTransaction() {
		return t;
	}
	
	public DexSession getSession() {
		return s;
	}
	
	private static final HandlerList handlers = new HandlerList();
	
	public HandlerList getHandlers() {
		return handlers;
	}
	
	static public HandlerList getHandlerList() {
		return handlers;
	}

}
