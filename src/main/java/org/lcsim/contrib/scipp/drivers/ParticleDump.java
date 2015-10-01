/*
 * ParticleDump.java
 *
 * Created on July 11, 2013, 10:45 AM
 * Edited on January 9, 2014, 12:31 AM
 * @author Alex Bogert and Christopher Milke
 * Modified by Jane Shtalenkova on
 * April 22, 2015
 */
package org.lcsim.contrib.scipp.drivers;

import org.lcsim.contrib.scipp.beamcal.*;
import org.lcsim.contrib.scipp.beamcal.util.LCIOFileManager;

import org.lcsim.contrib.scipp.beamcal.util.Jroot;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;

import hep.physics.particle.properties.ParticleType;

import org.lcsim.util.Driver;

import java.lang.String;


public class ParticleDump extends Driver {
    
    
    
    //DEFINE XML FUNCTIONS
    //These functions are specially fomatted functions to pull variable data from the xml file
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
    //END DEFINE XML FUNCTIONS





    //This function is called when the program is first started
    //and initializes all persistant data
    public void startOfData() {
        eventNumber = 0;

        try {
            root = new Jroot(jrootFile, "NEW");
	
            root.init("TH2D","posXY","posXY", "XYPosition", 300, -150, 150, 300, -150, 150);
            root.init("TH1D","posz","posz", "Z Position", 18000, 0, 18000);
	    
            
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

    public void printElectron(EventHeader event) {
        
    }
        
        
    //PROCESS FUNCTION
    //This is where the vast bulk of the program is run and controlled
    public void process( EventHeader event ) {
        MCParticle mcp = null;
        System.out.println("\n\n\n\n\n\n**************NEW EVENT*******************\n\n\n");

        
        System.out.println( event.keys() );


        
        //iterate through all FINAL_STATE particles in event
        for (MCParticle p : event.getMCParticles()) {
            
            int state = p.getGeneratorStatus();
            if(state == MCParticle.FINAL_STATE){
                ParticleType type = p.getType();

                String name = type.getName();
                int id = p.getPDGID();
               
            
                //System.out.println(type);
                //System.out.println(name);
                //System.out.println(id);

                //get endpoint and scale to face
                double[] pos = p.getEndPoint().v();


                //fill position plot
                try {
                     root.fill("posXY",pos[0], pos[1]);
                     root.fill("posz",pos[2]);
                }
                catch (java.io.IOException e) {
                     System.out.println(e);
                     System.exit(1);
                }
                //System.out.println("\n");
            }
        }
        
     System.out.println("FINISHED EVENT "  + eventNumber++ + "\n\n\n\n\n");
     
    }//End Process
    

    /*here all the classwide variables are declared*/
    private int eventNumber;
     
    private double faceZ=2500; //face of detector hard code
    public double theta;
    private double escZ=12000;
    private int eCount, pCount;
    private int morePairs = 0;

//xml derived variables
    private String jrootFile = "";

    //variables for jroot file construction and background/signal file reading
    private LCIOFileManager lcioFileMNGR = new LCIOFileManager();
    private Jroot root;
 

    }
