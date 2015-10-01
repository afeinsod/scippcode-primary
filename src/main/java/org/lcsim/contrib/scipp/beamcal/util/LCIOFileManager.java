/*
 * LCIOFileManager.java
 *
 * Created on Mar 19, 2011, 11:01 PM
 * Updated on Apr 12, 2011, 8:52  AM
 * @author Alex Bogert
 *
 * The purpose of this class is to over lay beamstrahlung events
 * ontop of high energy electron events.
 */

package org.lcsim.contrib.scipp.beamcal.util;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;

import org.lcsim.event.base.BaseLCSimEvent;
import org.lcsim.event.base.BaseSimCalorimeterHit;

import org.lcsim.util.Driver;
import org.lcsim.util.Driver.NextEventException;

import org.lcsim.lcio.LCIOReader;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;

/**This class can be used to overlay singal events ontop of background in the BeamCalorimeter. More specifically
 * this class will consolidate all SimCalorimeterHits in the event in the "BeamCalHits" collection into a list of
 * BeamCalorimeterTiles backed by the DataBaseBeamCalorimeter.class so they have access to the expected average background
 * energy of the tile. 
 */
public class LCIOFileManager {

    private int errors = 0;

    public LCIOFileManager() {
        errors = 0; 
    }

    public void setFile(String name) {
        this.fileNames.add(name);
        return;
    }

    public void setFiles(List<String> names) {
        this.fileNames.addAll(names);
        return;
    }
    
    public void setFilelist(String listname) {
        try {
            BufferedReader br = new BufferedReader( new FileReader(listname) ); 
            String line;
            while ((line = br.readLine()) != null) {
                this.fileNames.add(line);
            }
        } catch (java.io.IOException e) {
            System.out.println("Error reading in file: \"" + listname + "\"");
            System.exit(1);
        }
        
        return;
    }

    public int getErrorCount() {
        return errors;
    }
    public EventHeader nextEvent() {
        
        EventHeader temp = this.event; 
        try {
            this.getNextEvent(); 
        }
        catch (java.lang.NullPointerException e) {
            System.out.println("WARNING: encountred a NullPointer while reading next event");
            System.out.println("Please investigate this..");
            System.out.println("call getErrorCount() to find the total errors encountered by the LCIOFileManager");
            errors ++;
            this.event = temp;
        }
        catch (java.lang.OutOfMemoryError e) {
            System.out.println("FATAL WARNING: encountred a stack overflow while reading next event");
            System.out.println("Please investigate this..");
            System.out.println("call getErrorCount() to find the total errors encountered by the LCIOFileManager");
            errors ++;
            this.event = temp;
        }
        return this.event;
    }

    /**This will clear all the files in the Driver.*/
    public void clear() {
        fileNames.clear();
        return;
    }

    private void openNextFile() {
        Iterator it = this.fileNames.iterator();

        String name = "";
        if (it.hasNext()) {
            name = (String)it.next();
            it.remove();
        }
        else {
            //throw new java.lang.RuntimeException("No files left.");
            this.file = null;
            return;
        }

        try {
            if (this.file != null) {
                this.closeFile();
            }
            this.file  = new LCIOReader(new File(name));
        }
        catch (java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }
    }

    private void closeFile(){
        try {
            this.file.close();
        }
        catch (java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }
    }

    private void getNextEvent() {
        event = null;
        if (this.file == null) {
            this.openNextFile();
            if (this.file == null) {
                //throw new java.lang.RuntimeException("There are no events left.");
                this.event = null;
            }
        }
       
        if (this.file != null) {
        try {
            this.event = this.file.read();
        }
        catch (java.io.IOException e) {
            this.file = null;
            this.getNextEvent();
        }
        }
        return;
    }
    //The event header containing the signal event information
    private EventHeader              event     = null;

    private LCIOReader               file      = null;

    private List<String>             fileNames = new ArrayList<String>();
}
