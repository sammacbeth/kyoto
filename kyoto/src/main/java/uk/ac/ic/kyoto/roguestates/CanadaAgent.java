package uk.ac.ic.kyoto.roguestates;

import java.util.Set;
import java.util.UUID;

import uk.ac.imperial.presage2.core.environment.ParticipantSharedState;
import uk.ac.imperial.presage2.core.messaging.Input;

public class CanadaAgent extends NonParticipant {

	public CanadaAgent(UUID id, String name,String ISO, double landArea, double arableLandArea, double GDP,
			double GDPRate, long availableToSpend, long emissionsTarget, long carbonOffset,
			long energyOutput, long carbonOutput) {
		super(id, name, ISO, landArea, arableLandArea, GDP,
				GDPRate, availableToSpend, emissionsTarget, carbonOffset,
				energyOutput, carbonOutput);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected Set<ParticipantSharedState> getSharedState() {
		return super.getSharedState();
	}

	@Override
	protected void processInput(Input input) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void YearlyFunction() {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void SessionFunction() {
		if (carbonOutput - carbonOffset > emissionsTarget) {
			// leave Kyoto here
		}
	}
	
	@Override
	public void initialiseCountry() {
		//carbonOutput = 80;
//		try {
//			tradeProtocol = new TradeProtocol(getID(), authkey, environment, network) {
//				@Override
//				protected boolean acceptExchange(NetworkAddress from,
//						Trade trade) {
//					if (carbonOutput - emissionsTarget + carbonOffset > 0) {
//						return true;
//					}
//					return true;
//				}
//			};
//		} catch (FSMException e) {
//			logger.warn(e.getMessage(), e);
//			e.printStackTrace();
//		}
	}
	
	@Override
	public void behaviour() {
//		Set<NetworkAddress> nodes = network.getConnectedNodes();
//		for (NetworkAddress i: nodes) {
//			try {
//				tradeProtocol.offer(i, 10, 5, TradeType.BUY);
//			} catch (FSMException e) {
//				e.printStackTrace();
//			}
//		}
		if (availableToSpend > 0) {
			try {
				carbonReductionHandler.invest((long) (availableToSpend*0.1));
				System.out.println("Spending " + availableToSpend* 0.1 + " on carbon reduction. Current carbon output is " + carbonOutput + ".");
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
				e.printStackTrace();
			}
		}
		
		System.out.println("I have this much money: " + availableToSpend + ".");
	}

}
