/* *********************************************************************** *
 * project: org.matsim.*
 * PtControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.kairuns.run;

import java.util.Iterator;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

class KNTransitControler {

	private static boolean useTransit = true ;
	private static boolean useOTFVis = true ;

	public static void main(final String[] args) {
		if ( args.length > 1 ) {
			useOTFVis = Boolean.parseBoolean(args[1]) ;
		}

		Config config;
		if ( args == null || args.length==0 || args[0]==null ){
			config = ConfigUtils.loadConfig(
					"/Users/kainagel/runs-svn/berlin-bvg09/presentation_20100408/bb_10p/1pct-config-local.xml"
//				"/Users/kainagel/runs-svn/berlin-bvg09/presentation_20100408/bb_10p/config-kai-local.xml"
//				"/Users/kainagel/runs-svn/berlin-bvg09/presentation_20100408/m2_schedule_delay/config-kai-local.xml"
						       );
		} else{
			config = ConfigUtils.loadConfig( args[0] );
		}
		
		config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.queue );
		
		if ( useTransit ) {
			config.transit().setUseTransit(true);
			config.transit().setBoardingAcceptance( TransitConfigGroup.BoardingAcceptance.checkStopOnly );
			
			OTFVisConfigGroup visConfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.class ) ;
			visConfig.setColoringScheme( OTFVisConfigGroup.ColoringScheme.bvg ) ;
			visConfig.setDrawTime(true);
			visConfig.setDrawNonMovingItems(true);
			visConfig.setAgentSize(125);
			visConfig.setLinkWidth(10);
			
//			visConfig.setMapOverlayMode(true);
		}

		config.qsim().setVehicleBehavior( QSimConfigGroup.VehicleBehavior.teleport ) ;
		//		config.otfVis().setShowTeleportedAgents(true) ;

		config.plans().setHandlingOfPlansWithoutRoutingMode( PlansConfigGroup.HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier );
		
		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		final Population pop = scenario.getPopulation();
		for ( Iterator it = pop.getPersons().entrySet().iterator() ; it.hasNext() ; ) {
			Entry entry = (Entry) it.next() ;
			Person person = (Person) entry.getValue() ;
			for ( PlanElement pe : person.getSelectedPlan().getPlanElements() ) {
				if ( pe instanceof Activity ) {
					Activity act = (Activity) pe ;
					if ( act.getType().equals("pickup") || act.getType().equals("dropoff") ) {
						it.remove(); 
						break ;
					}
				}
				if ( pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if ( leg.getMode().equals("undefined") ) {
						leg.setMode(TransportMode.ride);
					}
				}
			}
		}
		
//		Collection<Id<Person>> personIdsToRemove = new ArrayList<>() ;
//		for ( Person person : pop.getPersons().values() ) {
//			for ( PlanElement pe : person.getSelectedPlan().getPlanElements() ) {
//				if ( pe instanceof Activity ) {
//					Activity act = (Activity) pe ;
//					if ( act.getType().equals("pickup") || act.getType().equals("dropoff") ) {
//						personIdsToRemove.add( person.getId() ) ;
//					}
//				}
//			}
//		}
//		for ( Id<Person> pid : personIdsToRemove ) {
//			pop.getPersons().remove( pid ) ;
//		}
		
		// ---
		
		final Controler controler = new Controler(scenario) ;
		controler.getConfig().controller().setOverwriteFileSetting( OverwriteFileSetting.overwriteExistingFiles ) ;

		//		Logger.getLogger("main").warn("warning: using randomized pt router!!!!") ;
		//		tc.addOverridingModule(new RandomizedTransitRouterModule());

		if ( useOTFVis ) {

			OTFVisConfigGroup otfconfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class ) ;
			// (this should also materialize material from a config.xml--? kai, nov'15)
			
			otfconfig.setDrawTransitFacilityIds(false);
			otfconfig.setDrawTransitFacilities(false);
			controler.addOverridingModule( new OTFVisLiveModule() );
		}

		// the following is only possible when constructing the mobsim yourself:
		//			if(this.useHeadwayControler){
		//				simulation.getQSimTransitEngine().setAbstractTransitDriverFactory(new FixedHeadwayCycleUmlaufDriverFactory());
		//				this.events.addHandler(new FixedHeadwayControler(simulation));		
		//			}

//		controler.addOverridingModule(new OTFVisFileWriterModule());
		//		tc.setCreateGraphs(false);
		
		ScoringConfigGroup.ActivityParams params = new ScoringConfigGroup.ActivityParams("pt interaction") ;
		params.setScoringThisActivityAtAll(false);
		controler.getConfig().scoring().addActivityParams(params);

		// ---
		
		controler.run();
	}

}
