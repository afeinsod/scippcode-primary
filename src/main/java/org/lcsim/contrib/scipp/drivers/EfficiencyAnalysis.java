/*
 * EfficiencyAnalysis.java
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
import java.lang.Integer;

public class EfficiencyAnalysis extends Driver {

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
        PalletRadius = n;
    }
    
    
    public void setLastPalletsize(int n) {
        LastPalletRadius = n;
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

    public void setParams(String parameter_string) { 
        params = ScippUtils.generateParams(parameter_string);
    }
    //END DEFINE XML FUNCTIONS


    //This function is called when the program is first started
    //and initializes all persistant data
    public void startOfData() {
        runcount = 0;
        
        tiler = new BaseBeamCalorimeterTiler(params,cellSize,spreadFactor,removeNegative,aligned);
        
        for (int i = 0; i < LastPalletRadius - PalletRadius + 1; i++) {
            BaseBeamCalorimeterScanner s = new BaseBeamCalorimeterScanner(params,stepsize,PalletRadius+i);
            scanners.add(s);
        }
        
        try {
            root = new Jroot(jrootFile,"NEW");
            
            int plotnum = LastPalletRadius - PalletRadius + 1;
            for (int i = 0; i < plotnum; i++) {
                root.init("TProfile","profile"+Integer.toString(i),"eff"+Integer.toString(PalletRadius+i),
                "Efficiency Prad " + Integer.toString(PalletRadius+i), 70, 1, 8, 0.0, 1.0);
            }
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
        //obtain background and signal event
        EventHeader signalEvent = ScippUtils.getProperEvent(lcioFileMNGR);

                
        //extract hits from background
        List<SimCalorimeterHit> bghits = event.get(SimCalorimeterHit.class, "BeamCalHits");
        List<SimCalorimeterHit> signalhits = signalEvent.get(SimCalorimeterHit.class, "BeamCalHits");
        
        //apply hits to the pads of the detector
        this.tiler.clear();
        this.tiler.process(bghits);
        this.tiler.process(signalhits);
        
        int layer = 9;
        
        
        try {
        
            TileDataBaseReader dbreader1 = new TileDataBaseReader(DBtileFile);
            CylinderDataBaseReader dbreader2 = new CylinderDataBaseReader(DBcylFile);
            dbreader2.close();
            
            for (int pradI = 0; pradI <  LastPalletRadius - PalletRadius + 1; pradI++) {
                DBcylFile = DBcylFile.substring(0,DBcylFile.length()-4) + Integer.toString(PalletRadius + pradI) + ".db";
                dbreader2.open(DBcylFile);
                
                //look over all possible pallets and order them by energy
                this.scanners.get(pradI).scanDB( this.tiler.getDBTiles_onLayer(layer,dbreader1) );
                List<BeamCalorimeterPallet> seedPallets = this.scanners.get(pradI).getTopPalletsNoOverlap(50);
                
                
                //extend pallets backwards to form cylinders
                List< HashMap<String, BeamCalorimeterTile> > tileList = tiler.getTiles_betweenLayers(10,40);
                BeamCalorimeterCylinder[] cylinders = new BeamCalorimeterCylinder[seedPallets.size()];
                for (int l = 0; l < cylinders.length; l++) {
                    cylinders[l] = new BaseBeamCalorimeterCylinder( seedPallets.get(l) );
                    cylinders[l].addTileList(tileList);
                }
                
                
                
                //subtract background average from all cylinders and sort them
                DataBaseBeamCalorimeterCylinder dbc = null;
                List<DataBaseBeamCalorimeterCylinder> dbcyls = new ArrayList();
                System.out.println("filling cylinders with database");
                for (int l = 0; l < cylinders.length; l++) {
                    dbc = new DataBaseBeamCalorimeterCylinder(cylinders[l]);
                    dbc.setDBInfo(dbreader2);
                    if ( dbc.hasDBInfo() ) dbcyls.add(dbc);
                    if (dbcyls.size() > 50) break;
                    dbc = null;
                }
                
                
                //perform efficiency rate testing across various sigma cuts
                for (double sigma = 1; sigma <= 8; sigma += 0.1) {
                    int eff = 0;
                    for (DataBaseBeamCalorimeterCylinder c : dbcyls) {
                        if ( c.getSubtractedEnergy() > sigma*c.getSTDev() ) {
                            eff = 1;
                            break;
                        }
                    }
                    root.fill("profile"+Integer.toString(pradI),sigma,eff);
                }
            
            }
            
            dbreader1.close();
            
        }catch (java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }catch (java.sql.SQLException e) {
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
    private int stepsize = 1;
    private BaseBeamCalorimeterTiler tiler;
    private List<BaseBeamCalorimeterScanner> scanners  = new ArrayList<BaseBeamCalorimeterScanner>();
    private int PalletRadius;
    private int LastPalletRadius;
    private double cellSize;
    private int spreadFactor;
    private boolean removeNegative;
    private boolean aligned;
    
    private int runcount;
}
