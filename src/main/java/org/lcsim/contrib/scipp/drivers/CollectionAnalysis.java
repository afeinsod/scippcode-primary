/*
 * CollectionAnalysis.java
 *
 * Created on July 11, 2013, 10:45 AM
 * Edited on March 12, 2015, 04:36 PM
 * @author Christopher Milke
 *
 */
package org.lcsim.contrib.scipp.drivers;

import org.lcsim.contrib.scipp.beamcal.util.LCIOFileManager;
import org.lcsim.contrib.scipp.beamcal.util.Jroot;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.util.Driver;
import org.lcsim.util.Driver.NextEventException;

import java.security.KeyStore.Entry;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Collections;
import java.lang.String;

public class CollectionAnalysis extends Driver {
    
    
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
    
    
    //END DEFINE XML FUNCTIONS




    //This function is called when the program is first started
    //and initializes all persistent data
    public void startOfData() {
        eventNumber = 0;
        String root_mode = "NEW";
        int init_capacity = 100000000;
        
        try {
            root = new Jroot(jrootFile,root_mode);//,init_capacity);
            
            root.proc( "TH2D *averagescatter1 = new TProfile2D(\"posxy\",\"X Y Hit Occupancy Over All Layers\", 200, -350, 350, 200, -350, 350)" );        
            root.proc( "TProfile2D *averagemap10 = new TProfile2D(\"heat10\",\"X Y Energy on Layer 10 \", 200, -350, 350, 200, -350, 350)" );                
            root.proc( "TProfile2D *averagemapAll = new TProfile2D(\"heatAverage\",\"X Y Average Energy over All Layers \", 200, -350, 350, 200, -350, 350)" );                     
            
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
        
        int check_layer = 49;
        boolean use_limit = false;
        boolean reject_negative = true;
        
        int hit_count = 0;
        
        HashMap<Long,double[]> totals = new HashMap<Long,double[]>();
        
        for (SimCalorimeterHit hit : hits) {
            double[] vec = hit.getPosition();
            int layer = hit.getLayerNumber();
            double energy = hit.getCorrectedEnergy();
            
            if ( reject_negative && (vec[2]<0) ); //pass over event
            else {            
                int xid = (int)( (vec[0]+150) / 3.5 );
                int yid = (int)( (vec[1]+150) / 3.5 );
                long id = xid*1000 + yid;
                Long ID = new Long( id );
                double[] val = new double[5]; //[vec0,vec1,numHits,energy,layerEnergy]
                val[0] = vec[0];
                val[1] = vec[1];
                val[2] = 1;
                val[3] = energy;
                val[4] = ( layer == check_layer ) ? energy : 0;
                
                
                if ( totals.containsKey(ID) ) {
                    double[] old = totals.get(ID);
                    double[] temp = {val[0],old[1],val[2]+old[2],val[3]+old[3],val[4]+old[4]};
                    
                    totals.put(ID, temp);
                }
                else {
                    totals.put(ID,val);
                }
                
                
            }
               
        }
            
        try {
            for ( double[] val : totals.values() ) {
                root.proc("averagescatter1->Fill("+val[0]+","+val[1]+","+val[2]+")");
                root.proc( "averagemapAll->Fill("+val[0]+","+val[1]+","+val[3]+")" );
                root.proc( "averagemap10->Fill("+val[0]+","+val[1]+","+val[4]+")" );
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
}
