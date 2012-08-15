/**
 * 
 */
package uk.ac.ic.kyoto.services;

import uk.ac.ic.kyoto.tokengen.Token;
import uk.ac.ic.kyoto.tradehistory.TradeHistory;
import uk.ac.imperial.presage2.core.environment.EnvironmentService;
import uk.ac.imperial.presage2.core.environment.EnvironmentSharedStateAccess;
import uk.ac.imperial.presage2.core.event.EventBus;
import uk.ac.imperial.presage2.core.event.EventListener;
import uk.ac.imperial.presage2.core.simulator.EndOfTimeCycle;

import com.google.inject.Inject;

/**
 * Environment Service that invokes method in TradeHistory that dumps current
 * tick trade histories onto the database
 * 
 * @author farhanrahman
 * 
 */
public class TradeHistoryService extends EnvironmentService {

	private final TradeHistory tradeHistory;
	private final Token token;

	/**
	 * @param sharedState
	 */
	@Inject
	public TradeHistoryService(EnvironmentSharedStateAccess sharedState,
			TradeHistory history, Token token) {
		super(sharedState);
		this.tradeHistory = history;
		this.token = token;
	}

	@Inject
	public void setEventBus(EventBus eb) {
		eb.subscribe(this);
	}

	public TradeHistory getTradeHistory() {
		return tradeHistory;
	}

	public Token getToken() {
		return token;
	}

	@EventListener
	public void endOfTimeUpdate(EndOfTimeCycle e) {
		tradeHistory.dumpData();
	}

}
