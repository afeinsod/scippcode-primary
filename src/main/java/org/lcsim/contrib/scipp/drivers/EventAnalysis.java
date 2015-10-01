/*
 * EventAnalysis.java
 *
 * Created on July 11, 2013, 10:45 AM
 * Edited on August 26, 2015, 02:21 AM
 * @author Christopher Milke
 *
 */
package org.lcsim.contrib.scipp.drivers;

import org.lcsim.contrib.scipp.beamcal.util.LCIOFileManager;
import org.lcsim.contrib.scipp.beamcal.geometry.PolarCoords;
import org.lcsim.contrib.scipp.beamcal.util.Jroot;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;

import org.lcsim.util.Driver;
import org.lcsim.util.Driver.NextEventException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Collections;

import java.lang.String;

public class EventAnalysis extends Driver {
    
    
    //DEFINE XML FUNCTIONS
    //These functions are specially formatted functions to pull variable data from the xml file
    /*****************************************************************************************
        XML FUNCTION FORMAT

    public void setVariablename(variable type) { //the first letter after "set" must be uppercase
                                                  //but can (must?) be lowercase in xml file
        set variable here; 
    }
    *******************************************************************************************/

    public void setOutputfile(String s) {
        this.jrootFile = s;
    }


    public void setBeamOutAligned(boolean algn) {
        aligned = algn;
    }
    
    
    //END DEFINE XML FUNCTIONS




    //This function is called when the program is first started
    //and initializes all persistent data
    public void startOfData() {
        eventNumber = 0;
        String root_mode = "NEW";
        
        try {
            root = new Jroot(jrootFile,root_mode);
            
            root.init("TH1D","hist1","posz","z position",2000,2000,4000);
            root.init("TH2D","scatter1","posxy","X Y Hit Occupancy Over All Layers", 350, -175, 175, 350, -175, 175);        
            root.init("TH2D","heatmapAll","heatAll","X Y Energy over All Layers", 350, -175, 175, 350, -175, 175);
            
        } catch(java.io.IOException e) {
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
        List<SimCalorimeterHit> hits = event.get(SimCalorimeterHit.class, "BeamCalHits");
        
        int check_layer = 0;
        int hit_count_limit = 100;
        boolean use_limit = false;
        boolean reject_negative = true;
        
        int hit_count = 0;
        
        try {
            for (SimCalorimeterHit hit : hits) {
                double[] vec = hit.getPosition();
                if (aligned) vec = PolarCoords.ZtoBeamOut(vec[0],vec[1],vec[2]);
                int layer = hit.getLayerNumber();
                
                if ( reject_negative && (vec[2]<0) ); //pass over event
                else {
                    root.fill("hist1",vec[2]);
                    root.fill("scatter1",vec[0],vec[1]);            
                    
                    double energy = hit.getCorrectedEnergy();
                    root.fill("heatmapAll",vec[0],vec[1],energy); 
                }
                
                if ( use_limit && (hit_count++ > hit_count_limit) ) break;    
            }
        
        } catch(java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }

        System.out.println("finished event "+ ++eventNumber);        

    }//End Process


    /*here all the classwide variables are declared*/
    private int eventNumber;


    //xml derived variables
    private String jrootFile = "";

    //variables for jroot file construction and background/signal file reading
    private LCIOFileManager lcioFileMNGR = new LCIOFileManager();
    private Jroot root;
    private boolean aligned;
}
