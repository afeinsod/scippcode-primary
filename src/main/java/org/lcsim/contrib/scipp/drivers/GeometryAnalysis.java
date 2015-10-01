/*
 * GeometryAnalysis.java
 *
 * Created on Mar 27, 2014, 10:05 PM
 * @author Christopher Milke
 *
 */
package org.lcsim.contrib.scipp.drivers;

import org.lcsim.contrib.scipp.beamcal.TileParameters;
import org.lcsim.contrib.scipp.beamcal.geometry.PolarCoords;
import org.lcsim.contrib.scipp.beamcal.util.ScippUtils;
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
import java.lang.Math;

public class GeometryAnalysis extends Driver {

    //DEFINE XML FUNCTIONS
    //These functions are specially fomatted functions to pull variable data from the xml file
    /*****************************************************************************************
        XML FUNCTION FORMAT

    public void setVariablename(variable type) { //the first letter after "set" must be uppercase
                                                  //but can be lowercase in xml file
        set variable here; 
    }
    *******************************************************************************************/

    public void setOutputfile(String s) {
        this.jrootFile = s;
    }
    
    
    public void setResolution(double R) {
        //smaller resolution means MORE detailed plot...yes, it's backwards. sorry.
        this.res = R;
    }
    
    
    public void setParams(String parameter_string) { 
        params = ScippUtils.generateParams(parameter_string);
    }
    
    //END DEFINE XML FUNCTIONS


    //This function is called when the program is first started
    //and initializes all persistent data
    public void startOfData() {
        try {
            root = new Jroot(jrootFile,"NEW");

            root.init("TH2D","graph","tilepic","Tile Picture", 2000, -100, 100, 2000, -100, 100);
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
        double edge = 100;
        int totalArcs = 0;
        
        try {
            double prevRadius = 0;
            
            int lastRing = params.getIDpolar(edge,0)[0];
            for (int ring = 0; ring < lastRing; ring++) {
                double radius = params.getCornerPolar(ring,0)[0];
                
                for (double phi = 0; phi < 2*Math.PI; phi+=res/radius) {
                    double[] xy = PolarCoords.PtoC(radius,phi);
                    root.fill("graph",xy[0],xy[1]);
                }
                
                

                int lastArc = params.getArcsInRing(ring);
                for (int arc = 0; arc < lastArc; arc++) {
                    double phi = params.getCornerPolar(ring,arc)[1];
                    
                    for (double r = radius; r > prevRadius; r -= res) {
                        double[] xy = PolarCoords.PtoC(r,phi);
                        root.fill("graph",xy[0],xy[1]);
                    }
                }
                totalArcs += lastArc;
                
                
                prevRadius = radius;
            }
            
            System.out.println("     TOTAL ARCS = " + totalArcs);
            
        }catch (java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }

    }//End Process
    
    
    

    /*here all the classwide variables are declared*/

    //xml derived variables
    private String jrootFile = "";

    //variables for jroot file construction and background/signal file reading
    private Jroot root;
    private double res;

    //miscellenous variables
    private TileParameters params;
}
