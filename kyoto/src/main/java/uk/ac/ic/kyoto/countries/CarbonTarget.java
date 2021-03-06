package uk.ac.ic.kyoto.countries;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import uk.ac.ic.kyoto.CarbonData1990;
import uk.ac.ic.kyoto.countries.AbstractCountry.KyotoMember;
import uk.ac.ic.kyoto.services.CarbonReportingService;
import uk.ac.ic.kyoto.services.GlobalTimeService;
import uk.ac.imperial.presage2.core.environment.EnvironmentService;
import uk.ac.imperial.presage2.core.environment.EnvironmentServiceProvider;
import uk.ac.imperial.presage2.core.environment.EnvironmentSharedStateAccess;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.event.EventBus;
import uk.ac.imperial.presage2.core.event.EventListener;
import uk.ac.imperial.presage2.core.simulator.EndOfTimeCycle;
import uk.ac.imperial.presage2.core.simulator.SimTime;

import com.google.inject.Inject;

/**
 *  Participant environment service for setting of carbon targets. Queried by countries via an action.
 * 
 *  Formula can be found on github wiki.
 *  
 *  @author Jonathan Ely
 */

public class CarbonTarget extends EnvironmentService {
	private Logger logger = Logger.getLogger(CarbonTarget.class);
	private class countryObject  {
		final public AbstractCountry obj;
		
		public double lastSessionTarget = 0;
		public double currentSessionTarget = 0;
		
		public Map<Integer, Double> yearTargets = new ConcurrentHashMap<Integer, Double>();
		
		public double proportion = 0;
		public double penalty = 0;
		
		public countryObject(AbstractCountry country) {
			this.obj = country;
		}
	}
	
	private ArrayList<countryObject> participantCountries= new ArrayList<countryObject>();
	private ArrayList<UUID> cheatersList = new ArrayList<UUID>();
	public ArrayList<UUID> queueToJoin = new ArrayList<UUID>();
	
	private double worldLastSessionTarget = 0;
	private double worldCurrentSessionTarget = 0;
	
	@SuppressWarnings(value = {"unused"})
	private EventBus eb;
	private GlobalTimeService timeService;
	private CarbonReportingService reportingService;
	private Monitor monitor;
	
	private EnvironmentServiceProvider provider;
	
	@Inject
	public CarbonTarget(EnvironmentSharedStateAccess sharedState, EnvironmentServiceProvider provider) {
		super(sharedState);
		
		this.provider = provider;
	}
	
	@Inject
	public void setEB(EventBus eb) {
		this.eb = eb;
		eb.subscribe(this);
	}

	/**
	 * Adds states to the service
	 */
	public void addMemberState(AbstractCountry state) {
		countryObject memberState = new countryObject(state);
		if (!participantCountries.contains(memberState))
			participantCountries.add(memberState);
	}
	
	private void addRejoiningCountries() {
		for (UUID  countryID : queueToJoin) {
			countryObject ref = findCountryObject(countryID);
			ref.currentSessionTarget = getReportedCarbonOutput(countryID, (timeService.getCurrentYear() - 1));
			ref.obj.kyotoMemberLevel = KyotoMember.ANNEXONE;
			monitor.addMemberState(ref.obj);
		}
		queueToJoin.clear();
	}
	
	public double querySessionTarget(UUID countryID) {
		countryObject obj = findCountryObject(countryID);
		return obj.currentSessionTarget;
	}
	
	public double queryYearTarget(UUID countryID, int year) {
		countryObject obj = findCountryObject(countryID);				
		return obj.yearTargets.get(year);
	}
	
	public double queryYearTarget(UUID countryID) {
		return queryYearTarget(countryID, timeService.getCurrentYear());
	}
	
	void addCountryPenalty(UUID countryID, double penaltyValue) {
		countryObject target= findCountryObject(countryID);
		
		if (penaltyValue >= 0) {
			target.penalty = penaltyValue;
		} else {
			target.penalty = 0;
		}
	}

	/*
	 * Function to be called after all countries have been added
	 */
	private void initialise(){
		try {
			this.timeService = provider.getEnvironmentService(GlobalTimeService.class);
		} catch (UnavailableServiceException e) {
			logger.warn("Unable to get environment service 'TimeService'.");
			//e.printStackTrace();
		}
		try {
			this.reportingService = provider.getEnvironmentService(CarbonReportingService.class);
		} catch (UnavailableServiceException e) {
			logger.warn("Unable to get environment service 'CarbonReportingService'.");
			//e.printStackTrace();
		}
		
		try {
			this.monitor = provider.getEnvironmentService(Monitor.class);
		} catch (UnavailableServiceException e) {
			logger.warn("Unable to get environment service 'Monitor'.");
			//e.printStackTrace();
		}
		
		this.worldCurrentSessionTarget = 0;
		
		for (countryObject country : participantCountries) {
			double data = 0;
			try {
				data = CarbonData1990.get(country.obj.getISO());
			} catch (Exception e) {
				logger.warn("1990 Data not Loaded for country: " + country.obj.getName());
				//e.printStackTrace();
			}
			
			country.currentSessionTarget = data;
			this.worldCurrentSessionTarget += data;
		}		
		
		updateSessionTargets();
		updateYearTargets();
	}
	
	private double getReportedCarbonOutput(UUID countryID, int year){
		double result;
		if (cheatersList.contains(countryID)){
			countryObject ref = findCountryObject(countryID);
			result = ref.obj.getCarbonOutput() - ref.obj.getCarbonAbsorption();
		} else {
			if (year < 0) {
				result = CarbonData1990.get(findCountryObject(countryID).obj.getISO());
			} else {
				Map<Integer, Double> reports = reportingService.getReport(countryID);
				int simTime = (timeService.getTicksInYear() * (year +1));
				result = reports.get(simTime);
			}
		}
		return result;
	}
	
	private countryObject findCountryObject(UUID countryID){
		for (countryObject country : participantCountries) {
			if (country.obj.getID() == countryID) {
				return country;
			}
		}
		
		throw new NullPointerException("countryID " + countryID + " does not exist in list of carbon target participants");
	}
	
	@EventListener
	public void onTimeCycle(EndOfTimeCycle e) {
		if (SimTime.get().intValue() == 1) {
			initialise();
		}
	}
	
	public void targetsForMonitor(ArrayList<UUID> theCheaters) {
		cheatersList = theCheaters;
		if (timeService.getCurrentYear() != 0) {
			if ((timeService.getCurrentYear() % timeService.getYearsInSession()) == 0)
			{
				updateSessionTargets();
			}
			
			updateYearTargets();
		}
	}
	
	public void updateYearTargets()
	{
		for (countryObject country : participantCountries) {
			if (country.obj.isKyotoMember() == KyotoMember.ANNEXONE)
				generateYearTarget(country);
		}
	}
	
	/*
	 * Generates end of year target (non binding) based on last reported emissions and session target.
	 */
	private void generateYearTarget(countryObject country)
	{
		int year = timeService.getCurrentYear();
		
		double sessionProgress = (double) ( timeService.getCurrentYear() % GameConst.getYearsInSession()) / GameConst.getYearsInSession();
		double diffTargets = country.lastSessionTarget - country.currentSessionTarget;
		double newTarget = country.lastSessionTarget - (diffTargets * sessionProgress) - country.penalty;
		
		if (newTarget < 0)
			newTarget = 0.0;
		
		logger.info("About to update target for year " + (year));
		country.yearTargets.put(year, newTarget);
		logger.info("Just updated target for year " + (year));
		logger.info("Target = " + country.yearTargets.get(year));
		country.obj.emissionsTarget = newTarget;
	}	
	
	private void updateSessionTargets(){
		addRejoiningCountries();
		
		this.worldLastSessionTarget = this.worldCurrentSessionTarget;
		this.worldCurrentSessionTarget = worldLastSessionTarget * GameConst.getTargetReduction(); 
		double worldOutput = 0;
		double rogueCarbonOutput = 0;
		
		int lastYear = timeService.getCurrentYear() -1;
		int session = timeService.getCurrentSession();
		
		for (countryObject country : participantCountries) {
			double output = getReportedCarbonOutput(country.obj.getID(), lastYear);
			worldOutput += output;
			if(country.obj.isKyotoMember() != KyotoMember.ANNEXONE){
				rogueCarbonOutput += output ;
			}
		}
		
		double kyotoTarget = this.worldCurrentSessionTarget - rogueCarbonOutput;
		
		for (countryObject country : participantCountries) {
			if (country.obj.isKyotoMember() == KyotoMember.ANNEXONE){
				double output = getReportedCarbonOutput(country.obj.getID(), lastYear);
				country.proportion = output / (worldOutput - rogueCarbonOutput);
				logger.info("About to update target for session " + session);
				generateSessionTarget(country, kyotoTarget);
				logger.info("Just updated target for session " + session);
				logger.info("Target = " + country.currentSessionTarget);
			}
		}
	}
	
	/*
	 * Generates end of session target (binding) from 1990 data
	 */
	private void generateSessionTarget(countryObject country, double kyotoTarget)
	{	
		country.lastSessionTarget = country.currentSessionTarget;
		double newTarget = country.proportion * kyotoTarget;
		double diffTargets  = country.lastSessionTarget - newTarget;
		
		if (diffTargets < 0) {
			newTarget = country.lastSessionTarget;
		} else if (diffTargets / country.lastSessionTarget > 0.1) {
			newTarget = country.lastSessionTarget * 0.9;
		}
		
		country.currentSessionTarget = newTarget;
	}
}
