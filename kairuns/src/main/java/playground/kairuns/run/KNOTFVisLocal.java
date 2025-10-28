package playground.kairuns.run;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;

class KNOTFVisLocal{


	public static void main( final String[] args ){

		String str = "" ;
		if ( args.length>0 && args[0]!=null ) {
			str = args[0] ;
		}
		String [] fn = {
//				"output_config_reduced.xml",
				"output_config.xml",
				"output_config.xml.gz"
		} ;
		String configFilename=null ;
		for( String abc : fn ){
			if ( new File( abc ).exists() ) {
				configFilename = abc ;
				break ;
			}
		}

		Config config = ConfigUtils.loadConfig( configFilename );
		config.network().setInputFile( str + "output_network.xml.gz" );
		config.plans().setInputFile( str + "output_plans.xml.gz" );
		{
			final String filename = str + "output_transitSchedule.xml.gz";
			if ( new File(  filename ).exists() ){
				config.transit().setTransitScheduleFile( filename );
			}
		}
		{
			final String filename = str + "output_transitVehicles.xml.gz";
			if ( new File(  filename ).exists() ){
				config.transit().setVehiclesFile( filename );
			}
		}
		{
			final String inputFile = str + "output_facilities.xml.gz";
			if ( new File( inputFile ).exists() ){
				config.facilities().setInputFile( inputFile );
			}
		}

		config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );

		config.qsim().setSnapshotStyle( QSimConfigGroup.SnapshotStyle.kinematicWaves );

		OTFVisConfigGroup visConfig = ConfigUtils.addOrGetModule( config, OTFVisConfigGroup.class );
		visConfig.setColoringScheme( OTFVisConfigGroup.ColoringScheme.bvg );
		visConfig.setDrawTime( true );
		visConfig.setDrawNonMovingItems( true );
		visConfig.setAgentSize( 125 );
		visConfig.setLinkWidth( 10 );
		visConfig.setDrawTransitFacilities( false );
		visConfig.setDrawTransitFacilityIds( false );

		for( RoutingConfigGroup.TeleportedModeParams params : config.routing().getModeRoutingParams().values() ){
			if ( params.getTeleportedModeSpeed()==null && params.getTeleportedModeFreespeedFactor()==null ) {
				params.setTeleportedModeSpeed( 2.0 );
			}
		}
		for( ScoringConfigGroup.ActivityParams params : config.scoring().getActivityParams() ){
			if ( params.getTypicalDuration().isUndefined() ) {
				params.setTypicalDuration( 3600. ) ;
			}
		}
		DvrpConfigGroup dvrp = ConfigUtils.addOrGetModule( config, DvrpConfigGroup.class );
		MultiModeDrtConfigGroup mmdrt = ConfigUtils.addOrGetModule( config, MultiModeDrtConfigGroup.class );
		SwissRailRaptorConfigGroup srr = ConfigUtils.addOrGetModule( config, SwissRailRaptorConfigGroup.class );

		// ---

//		ControlerUtils.checkConfigConsistencyAndWriteToLog( config, "before loading scenario" );

		Scenario scenario = ScenarioUtils.loadScenario( config );

		// ---

		final Controler controler = new Controler( scenario );

		controler.addOverridingModule( new OTFVisLiveModule() );

		// ---

		controler.run();


	}
}
