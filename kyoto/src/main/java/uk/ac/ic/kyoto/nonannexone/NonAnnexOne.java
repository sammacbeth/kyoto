package uk.ac.ic.kyoto.nonannexone;

import java.util.UUID;

import uk.ac.ic.kyoto.countries.AbstractCountry;
import uk.ac.imperial.presage2.core.messaging.Input;

public class NonAnnexOne extends AbstractCountry {


	public NonAnnexOne(UUID id, String name, String ISO, double landArea, double arableLandArea, double GDP,
			double GDPRate, double energyOutput, double carbonOutput){
		super(id, name, ISO, landArea, arableLandArea, GDP,
				GDPRate, energyOutput, carbonOutput);
	}

	@Override
	protected void processInput(Input in) {
		// TODO Auto-generated method stub

	}

	@Override
	public void yearlyFunction() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sessionFunction() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void initialiseCountry() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void behaviour() {
		// TODO Auto-generated method stub
		
	}

}
