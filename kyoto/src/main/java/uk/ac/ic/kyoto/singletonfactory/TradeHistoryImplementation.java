package uk.ac.ic.kyoto.singletonfactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import uk.ac.ic.kyoto.countries.OfferMessage;
import uk.ac.ic.kyoto.services.Decoder;
import uk.ac.ic.kyoto.tradehistory.TradeHistory;
import uk.ac.imperial.presage2.core.Time;
import uk.ac.imperial.presage2.core.db.StorageService;
import uk.ac.imperial.presage2.core.db.persistent.PersistentEnvironment;
import uk.ac.imperial.presage2.core.simulator.SimTime;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Singleton class implementation providing access to the history of trades that
 * has happened between participants.
 * 
 * @author farhanrahman
 * 
 */
@Singleton
public class TradeHistoryImplementation implements TradeHistory {
	private static Map<Integer, Map<UUID, OfferMessage>> history = new HashMap<Integer, Map<UUID, OfferMessage>>();

	private Logger logger = Logger.getLogger(TradeHistoryImplementation.class);

	private StorageService sto;

	@Inject
	void setStorageService(StorageService sto) {
		this.sto = sto;
	}

	/**
	 * @return returns a history of all the trade histories in the form of a map
	 *         simulation time -> map of trades that happened in that simulation
	 *         time
	 */
	public final Map<Integer, Map<UUID, OfferMessage>> getHistory() {
		synchronized (history) {
			return (new HashMap<Integer, Map<UUID, OfferMessage>>(history));
		}
	}

	/**
	 * @param simTime
	 * @return the history of trades for simulation time = simTime
	 */
	public Map<UUID, OfferMessage> getHistoryForTime(Time simTime) {
		synchronized (history) {
			return history.get(simTime.intValue());
		}
	}

	/**
	 * @param id
	 * @return true if trade of trade id = id has been registered in the map.
	 */
	public boolean tradeExists(UUID id) {
		synchronized (history) {
			for (Integer time : history.keySet()) {
				for (UUID uid : history.get(time).keySet()) {
					if (uid.equals(id))
						return true;
				}
			}
		}
		return false;
	}

	/**
	 * updates trade history with the given information
	 * 
	 * @param simTime
	 *            , tradeID, trade
	 */
	public void addToHistory(Time simTime, UUID tradeID, OfferMessage trade) {
		synchronized (history) {
			Map<UUID, OfferMessage> t = history.get(simTime.intValue());
			if (t == null) {
				t = new HashMap<UUID, OfferMessage>();
			}
			t.put(tradeID, trade);
			history.put(simTime.intValue(), t);
		}
	}

	/**
	 * removes trade from the history with UUID = id
	 * 
	 * @param id
	 */
	public void removeTradeHistoryWithID(UUID id) {
		synchronized (history) {
			for (Integer time : history.keySet()) {
				for (UUID uid : history.get(time).keySet()) {
					if (uid.equals(id))
						history.get(time).remove(uid);
				}
			}
		}
	}

	/**
	 * Dumps the trade data in the current tick into the database
	 */
	public void dumpData() {

		synchronized (history) {
			Integer simTick = SimTime.get().intValue();
			Map<UUID, OfferMessage> trades = history.get(simTick);
			PersistentEnvironment s = sto.getSimulation().getEnvironment();
			if (trades != null) {
				// insert trade key set
				s.setProperty("trades", simTick,
						StringUtils.join(trades.keySet(), ','));
				for (UUID id : trades.keySet()) {
					// individual trade data
					OfferMessage o = trades.get(id);

					s.setProperty("trade." + id + ".broadcaster", simTick,
							Decoder.getCountryISOForID(o.getBroadCaster()));
					s.setProperty("trade." + id + ".initiator", simTick,
							Decoder.getCountryISOForID(o.getInitiator()));
					s.setProperty("trade." + id + ".quantity", simTick,
							Double.toString(o.getOfferQuantity()));
					s.setProperty("trade." + id + ".unitCost", simTick,
							Double.toString(o.getOfferUnitCost()));
					s.setProperty("trade." + id + ".tradeType", simTick, o
							.getOfferType().name());
					s.setProperty("trade." + id + ".investmentType", simTick, o
							.getOfferInvestmentType().name());
				}
			}
		}

	}

}