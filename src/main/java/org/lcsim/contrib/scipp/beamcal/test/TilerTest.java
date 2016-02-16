/*
 * TilerTest.java
 *
 * Created on Mar 27, 2014, 10:05 PM
 * @author Christopher Milke
 *
 */
package org.lcsim.contrib.scipp.beamcal.test;


import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterTiler;
import org.lcsim.contrib.scipp.beamcal.base.BaseBeamCalorimeterTiler;

import org.lcsim.contrib.scipp.beamcal.database.TileDataBaseReader;

import org.lcsim.contrib.scipp.beamcal.TileParameters;
import org.lcsim.contrib.scipp.beamcal.geometry.PhiTileParameters;
import org.lcsim.contrib.scipp.beamcal.geometry.ArcTileParameters;

import org.lcsim.contrib.scipp.lctools.jroot.JROOTFactory;
import org.lcsim.contrib.scipp.beamcal.LCIOFileManager;

import org.lcsim.contrib.scipp.lctools.jroot.TH2.TH2D;
import org.lcsim.contrib.scipp.lctools.jroot.TH1.TH1D;

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

public class TilerTest extends Driver {
    public TilerTest() {} //null constructor

    //DEFINE XML FUNCTIONS
    //These functions are specially fomatted functions to pull variable data from the xml file
    /*****************************************************************************************
        XML FUNCTION FORMAT

    public void setVariablename(variable type) { //the first letter after "set" must be uppercase
                                                  //but can (must?) be lowercase in xml file
        set variable here; 
    }
    *******************************************************************************************/

    public void setSignalfile(String name) {
        this.lcioFileMNGR.setFile(name);
    }

    public void setJrootfile(String s) {
        this.jrootFile = s;
    }
    
    
    public void setDbtilefile(String s) { 
        DBtileFile = s;
    }
    
    
    public void setCellsize(double size) { 
        cellSize = size;
    }
    
    
    public void setSpreadfactor(int spread) { 
        spreadFactor = spread;
    }
    
    
    public void setParams(String parameter_string) { 
        String[] parameters = parameter_string.split(",");
        float[] p = new float[8];
        System.out.println("creating params");
        for (int i = 0; i < parameters.length; i++) {
            p[i] = Float.parseFloat( parameters[i] );
            System.out.println( p[i] );
        }
        
        params = new ArcTileParameters(p[0],p[1],p[2],p[3],p[4],p[5],p[6],p[7]);
    }
    //END DEFINE XML FUNCTIONS


    //This function is called when the program is first started
    //and initializes all persistant data
    public void startOfData() {
        tiler = new BaseBeamCalorimeterTiler(params,cellSize,spreadFactor,true);
        
        
        try {
            this.factory.open(this.jrootFile); 

            this.graph = this.factory.newTH2D("tilepic",
                "Tile Picture", 3000, -150F, 150F, 3000, -150F, 150F);
                
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
            this.factory.close();
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
        
        //Get electron signal event and ensure it actually hits the detector
        EventHeader signalEvent = this.lcioFileMNGR.nextEvent();
        System.out.println("retrieved signal event");

        if (signalEvent == null)
            throw new java.lang.RuntimeException("There are no signal events left. Process ending");

        MCParticle electron = this.getElectron(signalEvent);

        if (electron == null) {
            throw new NextEventException();
        }

        while (! this.testElectron(electron)) {
            signalEvent = this.lcioFileMNGR.nextEvent();
            if (signalEvent == null)
            throw new java.lang.RuntimeException("There are no signal events left. Process ending");

            electron    = this.getElectron(signalEvent);
            if (electron == null) {
                throw new NextEventException();
            }
        }
        
        int layer = 9;
        double res = 0.1;

        //process signal and background files to extract the locations of hits 
        // and remove those hits that we don't care about
        List<SimCalorimeterHit> signalhits = signalEvent.get(SimCalorimeterHit.class, "BeamCalHits");
        List<SimCalorimeterHit> bghits     = event.get(SimCalorimeterHit.class, "BeamCalHits");
        
        try {
            double[] layerEnergy = new double[50];
            
            
            
            HashMap<String, Double> layerMap = null;
            
            
            //apply hits to the geometry of the detector. background hits are 
            // processed specially so they are overlayed on signal hits
            this.tiler.clear();
            ///this.tiler.process(signalhits);
            this.tiler.process(bghits);
            System.out.println("All hits processed");
            
            ArrayList< HashMap<String, Double> > alltiles;
            alltiles = tiler.getTiles();
            
            int i = 0;
            for ( HashMap<String, Double> map : alltiles ) {
                if (i == layer) layerMap = map;
                    
                
                double totalEnergy = 0;
                
                System.out.println("\n\n_______________________________________\n\n");
                System.out.println("SHOWING LAYER " + i + "\n");
                
                for ( Map.Entry<String, Double> entry : map.entrySet() ) {
                    String key = entry.getKey();
                    double energy = entry.getValue();
                    totalEnergy += energy;
                    
                    System.out.println("tile key/energy = " + key + ", " + energy);
                }
                System.out.println("\nTotal Energy = " + totalEnergy);
                System.out.println("Layer Energy = " + layerEnergy[i]);
                
                i++;
            }
            
            
            
            short[] tileID = {0,0};
            short[] testID;
            
            double x,y;
            double edge = 20;
            
            for( x = -edge; x < edge ; x += res ) {
                System.out.println("x="+x);
                for ( y = -edge ; y < edge ; y += res ) {
                    
                    testID = getID(x,y,layerMap);
                    
                    if (tileID[0] != testID[0] || tileID[1] != testID[1])
                         graph.fill(x,y);
                    
                    tileID = testID;
                }
            }

            for( y = -edge; y < edge ; y += res ) {
                System.out.println("y="+y);
                for ( x = -edge ; x < edge ; x += res ) {
                    
                    testID = getID(x,y,layerMap);
                    
                    if (tileID[0] != testID[0] || tileID[1] != testID[1])
                         graph.fill(x,y);
                    
                    tileID = testID;
                }
            }
            
            
            
            
            

 
        }catch (java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }/*catch (java.sql.SQLException e) {
            System.out.println(e);
            System.exit(1);
        }*/

    }//End Process
    
    
    private short[] getID(double x, double y, HashMap<String, Double> layerMap) {
        short[] badID = {0,0};
        
        for ( Map.Entry<String, Double> entry : layerMap.entrySet() ) {
            String key = entry.getKey();
            short[] IDs = this.params.StringtoID(key);
            double energy = entry.getValue();
            short[] pointID = this.params.getID(x,y); 
            if (IDs[0] == pointID[0] && IDs[1] == pointID[1]) {
                return IDs;
        }
        return badID;
    }
    
    
    
    //extracts electron data from signal file
    private MCParticle getElectron(EventHeader event) {
        MCParticle mcp = null;
        for (MCParticle p : event.getMCParticles()) {
            if (Math.abs(p.getPDGID()) == 11 && p.getGeneratorStatus() == MCParticle.FINAL_STATE) {
                mcp = p;
                break;
            }
        }
        return mcp;
    }
    
    
    //ensures the electron hits the detector 
    //(can be used to limit studies to one part of the detector)
    private boolean testElectron(MCParticle electron) {
        double[] vec = electron.getEndPoint().v();
        double   rorg = Math.hypot(vec[0], vec[1]);

        //Reject electrons that do not decay at the front of the beamcal
        boolean results = true;
        if (rorg >= 90 || vec[2] < 2500 || vec[2] > 3500) {
            results = false;
        }

        //Reject electrons going down the beam pipe
        if ( (vec[0] <= 34 && vec[0] >= 4) && (vec[1] <= 15 && vec[1] >= -15) ) {
            results = false;
        }
        if ( (vec[0] >= -30 && vec[0] <= -15) && (vec[1] <= 10 && vec[1] >= -10) ) {
            results = false;
        }
        return results;
    }
    

    /*here all the classwide variables are declared*/

    //xml derived variables
    private String jrootFile = "";

    //variables for jroot file construction and background/signal file reading
    private LCIOFileManager lcioFileMNGR = new LCIOFileManager();
    private JROOTFactory factory = new JROOTFactory();
    private TH2D graph;

    //miscellenous variables
    private String DBtileFile;
    private TileParameters params;
    private BaseBeamCalorimeterTiler tiler;
    private double cellSize;
    private int spreadFactor;
}
