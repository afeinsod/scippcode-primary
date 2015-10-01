/*
 * RadialAnalysis.java
 *
 * Created on Mar 27, 2014, 10:05 PM
 * @author Christopher Milke
 *
 */
package org.lcsim.contrib.scipp.drivers;

import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterTile;
import org.lcsim.contrib.scipp.beamcal.base.BaseBeamCalorimeterTile;
import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterTiler;
import org.lcsim.contrib.scipp.beamcal.base.BaseBeamCalorimeterTiler;
import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterPallet;
import org.lcsim.contrib.scipp.beamcal.base.BaseBeamCalorimeterPallet;
import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterScanner;
import org.lcsim.contrib.scipp.beamcal.base.BaseBeamCalorimeterScanner;
import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterCylinder;
import org.lcsim.contrib.scipp.beamcal.base.BaseBeamCalorimeterCylinder;

import org.lcsim.contrib.scipp.beamcal.database.TileDataBaseReader;
import org.lcsim.contrib.scipp.beamcal.database.DataBaseBeamCalorimeterTile;
import org.lcsim.contrib.scipp.beamcal.database.CylinderDataBaseReader;
import org.lcsim.contrib.scipp.beamcal.database.DataBaseBeamCalorimeterCylinder;

import org.lcsim.contrib.scipp.beamcal.TileParameters;
import org.lcsim.contrib.scipp.beamcal.geometry.PolarCoords;

import org.lcsim.contrib.scipp.beamcal.util.Jroot;
import org.lcsim.contrib.scipp.beamcal.util.LCIOFileManager;
import org.lcsim.contrib.scipp.beamcal.util.ScippUtils;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;

import org.lcsim.util.Driver;
import org.lcsim.util.Driver.NextEventException;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Collections;

import java.lang.String;
import java.lang.Double;
import java.lang.Integer;
import java.lang.Math;

public class RadialEfficiencyAnalysis extends Driver {

    //DEFINE XML FUNCTIONS
    //These functions are specially fomatted functions to pull variable data from the xml file
    /*****************************************************************************************
        XML FUNCTION FORMAT

    //public void setVariablename(variable type) { //the first letter after "set" must be uppercase
    //                                              //but can (must?) be lowercase in xml file
    //    set variable here; 
    //}
    *******************************************************************************************/

    public void setInput1(String s) {
        this.lcioFileMNGR.setFilelist(s);
    }

    public void setOutputfile(String s) {
        this.jrootFile = s;
    }
    
    
    public void setDbtilefile(String s) { 
        this.DBtileFile = s;
    }
    
    
    public void setDbcylfile(String s) {
        this.DBcylFile = s;
    }
    
    
    public void setPalletsize(int n) {
        prad = n;
    }
    
    
    public void setParams(String parameter_string) { 
        params = ScippUtils.generateParams(parameter_string);
    }
    
    
    public void setCellsize(double size) { 
        cellSize = size;
    }
    
    
    public void setSpreadfactor(int spread) { 
        spreadFactor = spread;
    }
    
    
    public void setNegativeremoval(boolean rmn) { 
        removeNegative = rmn;
    }

    public void setBeamOutAligned(boolean algn) {
        aligned = algn;
    }

    public void setSigma(double s) { 
        sigma = s;
    }
    //END DEFINE XML FUNCTIONS

    //This function is called when the program is first started
    //and initializes all persistant data
    public void startOfData() {
        tiler = new BaseBeamCalorimeterTiler(params,cellSize,spreadFactor,removeNegative,aligned);
        scanner = new BaseBeamCalorimeterScanner(params,1,prad);
        runcount = 0;
        eventCount = 0;
        
        try {
            root = new Jroot(jrootFile,"NEW");
                
            root.init("TProfile","insteff","insteff","GeV Instrumental Radial Efficiency", 28, 0, 140, 0.0, 1.2);
            root.init("TProfile","geomeff","geomeff","GeV Geometric Radial Efficiency", 28, 0, 140, 0.0, 1.2);
            root.init("TProfile","totleff","totleff","GeV Total Radial Efficiency", 28, 0, 140, 0.0, 1.2);
            
        }
        catch (java.io.IOException e) {
           System.out.println(e);
           System.exit(1);
        }
    }

    //This function is called after all file runs have finished,
    // and closes any necessary data
    public void endOfData(){
        try {
            root.end();
        }
        catch (java.io.IOException e) {
           System.out.println(e);
           System.exit(1);
        }
    }

    //PROCESS FUNCTION
    //This is where the vast bulk of the program is run and controlled
    public void process( EventHeader event ) {
        super.process( event );
        EventHeader signalEvent = ScippUtils.getEvent(lcioFileMNGR);
        
        MCParticle electron = ScippUtils.getElectron(signalEvent);
        
        double[] initpos = electron.getEndPoint().v();
        double[] electron_position = PolarCoords.ZtoBeamOut(initpos[0],initpos[1],initpos[2]);
        double electron_radius = Math.hypot(electron_position[0],electron_position[1]);
        double scaled_radius = (zface/electron_position[2]) * electron_radius;
        int layer = 9;

        //process signal and background files to extract the locations of hits 
        // and remove those hits that we don't care about
        List<SimCalorimeterHit> signalhits = signalEvent.get(SimCalorimeterHit.class, "BeamCalHits");
        List<SimCalorimeterHit> bghits     = event.get(SimCalorimeterHit.class, "BeamCalHits");
        
        try {
            if (electron_position[2] > zstart) {
                int geom_eff = 0;
                int eff = 0;
                if (electron_position[2] < zend) { 
                    geom_eff = 1;

                    //apply hits to the geometry of the detector. background hits are 
                    // processed specially so they are overlayed on signal hits
                    this.tiler.clear();
                    this.tiler.process(signalhits);
                    this.tiler.process(bghits);
                    
                    TileDataBaseReader dbreader1 = new TileDataBaseReader(DBtileFile);
                    
                    //look over all possible pallets and order them by energy
                    this.scanner.scanDB( this.tiler.getDBTiles_onLayer(layer,dbreader1) );
                    List<BeamCalorimeterPallet> seedPallets = new ArrayList();
                    seedPallets.addAll( this.scanner.getTopPalletsNoOverlap(50) );
                    dbreader1.close();
                    
                    
                    //extend pallets backwards to form cylinders
                    List< HashMap<String, BeamCalorimeterTile> > tileList = tiler.getTiles_betweenLayers(10,40);
                    BeamCalorimeterCylinder[] cylinders = new BeamCalorimeterCylinder[seedPallets.size()];
                    
                    for (int l = 0; l < cylinders.length; l++) {
                        cylinders[l] = new BaseBeamCalorimeterCylinder( seedPallets.get(l) );
                        cylinders[l].addTileList(tileList);
                    }
                    
                    
                    
                    //subtract background average from all cylinders and sort them
                    CylinderDataBaseReader dbreader2 = new CylinderDataBaseReader(DBcylFile);
                    dbreader2.close();
                    
                    System.out.println("Sorting Cylinders");
                    DataBaseBeamCalorimeterCylinder dbc = null;
                    List<DataBaseBeamCalorimeterCylinder> dbcyls = new ArrayList();
                    System.out.println("filling cylinders with database");
                    for (int l = 0; l < cylinders.length; l++) {
                        dbreader2.open(DBcylFile);
                        dbc = new DataBaseBeamCalorimeterCylinder(cylinders[l]);
                        dbc.setDBInfo(dbreader2);
                        if ( dbc.hasDBInfo() ) dbcyls.add(dbc);
                        if (dbcyls.size() >= 50) break;
                        dbreader2.close();
                        dbc = null;
                    }
                    Collections.sort( dbcyls, Collections.reverseOrder() );
                    
                    
                    //performs a multitude of tests
                    boolean testPassed = false;
                    for (DataBaseBeamCalorimeterCylinder sig_cyl: dbcyls) {
                        if ( sig_cyl.getSubtractedEnergy() > sigma*sig_cyl.getSTDev() ) {
                            eff = 1;
                            break;
                        }
                    }
                    root.fill("insteff",scaled_radius, eff);
                    
                }
                root.fill("geomeff",scaled_radius, geom_eff);
                root.fill("totleff",scaled_radius, eff*geom_eff);
            }

        } catch (java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        } catch (java.sql.SQLException e) {
            System.out.println(e);
            System.exit(1);
        }


        System.out.println("      RUNCOUNT AT " + (++runcount) );
    }//End Process
    
    
    

    /*here all the classwide variables are declared*/

    //xml derived variables
    private String jrootFile = "";

    //variables for jroot file construction and background/signal file reading
    private LCIOFileManager lcioFileMNGR = new LCIOFileManager();
    private Jroot root;
    
    //miscellenous variables
    private String DBtileFile;
    private String DBcylFile;
    private TileParameters params;
    private BaseBeamCalorimeterTiler tiler;
    private BaseBeamCalorimeterScanner scanner;
    private int prad;
    private double cellSize;
    private int spreadFactor;
    private boolean removeNegative;
    private boolean aligned;
    private double sigma;
    private double tileDensity[] = new double[6];
    
    private int runcount;
    private int eventCount;
    List<Double> energyList = new ArrayList<Double>();

    private double zstart = 2000;
    private double zend = 4000; 
    private double zface = 3265;
}
