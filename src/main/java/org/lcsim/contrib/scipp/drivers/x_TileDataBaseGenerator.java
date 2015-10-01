/*
 * x_TileDataBaseGenerator.java
 *
 * Created on Mar 19, 2011, 11:01 PM
 * Updated on Oct 18, 2014, 1:00  AM
 * @author Alex Bogert and Christopher Milke
 *
 * The purpose of this class is to manage a SQLite database
 * ,which stores the average energy for beamstrahlung on a 
 * per tile basis in the Beam Calorimeter
 */

package org.lcsim.contrib.scipp.drivers;

import org.lcsim.contrib.scipp.beamcal.sqlite.SQLITEWrapper;
import org.lcsim.contrib.scipp.beamcal.sqlite.ArrayListSet;

//Interface for accessible BeamCalorimeter Analysis
import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterTile;
import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterTiler;
import org.lcsim.contrib.scipp.beamcal.base.BaseBeamCalorimeterTile;
import org.lcsim.contrib.scipp.beamcal.base.BaseBeamCalorimeterTiler;
import org.lcsim.contrib.scipp.beamcal.TileParameters;

import org.lcsim.contrib.scipp.beamcal.util.ScippUtils;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;

import org.lcsim.util.Driver;

import java.lang.Math;
import java.lang.Double;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import java.io.*;

import java.sql.PreparedStatement;

public class x_TileDataBaseGenerator extends Driver {
    
    //XML functions
    public void setParams(String parameter_string) {
        params = ScippUtils.generateParams(parameter_string);
    }
    
    
    public void setOutputfile(String s) { 
        connectionName = s+".db";
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
    //END XML functions
    
    
    
    public void startOfData() {
        this.tiler = new BaseBeamCalorimeterTiler(params,cellSize,spreadFactor,removeNegative,aligned);
        
        this.avgList = new ArrayList< HashMap<String, Double[]> >();
        for (int layer = 0; layer < number_of_layers; layer++) {
            HashMap<String, Double[]> map;
            map = new HashMap<String, Double[]>();
            
            avgList.add(map);
        }
        
        try {
            this.database = new SQLITEWrapper();
            this.database.connection(connectionName);
            this.initializeNewDB();
            this.eventCount = 0;
        }
        catch (java.sql.SQLException e) {
            System.out.println(e);
            System.exit(1);
        }
        catch (java.lang.ClassNotFoundException e) {
            System.out.println(e);
            System.exit(1);
        }
    }
    
    
    public void endOfData(){
        try {
            PreparedStatement stat = null;
            
            int layer = 0;
            int  endLayer = 49;
            String baselayername = "layer";
            String layername = "";
            HashMap<String, Double[]> avgMap;
            
            for (layer = 0; layer <= endLayer; layer++) {
                layername = baselayername.concat( Integer.toString(layer) );
                System.out.println("Preparing update on " + layername);
                
                stat = this.database.updateStatement(layername);
                
                for ( Map.Entry<String, Double[]> entry : avgList.get(layer).entrySet() ) {
                    
                    String id = entry.getKey();
                    Double[] data = entry.getValue();
                    double totalEnergy = data[0].doubleValue();
                    int weight = (int)Math.round( data[1].doubleValue() );
                    
                    double average = totalEnergy / (double)weight;
                    
                    
                    //update the value of the row
                    ///System.out.println("string = " + id);
                    ///System.out.println("        average = " + average);
                    ///System.out.println("                 weight = " + weight);
                    stat.setString(1,id);
                    stat.setDouble(2,average);
                    stat.setInt   (3,weight);
                    stat.setString(4,id);

                    stat.addBatch();
                }
                
                this.database.commit(stat);
                stat = null;
            }
            
            
            
            
            this.database.close();
        }
        catch (java.sql.SQLException e) {
            System.out.println(e);
            System.exit(1);
        }
    }
    
    
    
    private void initializeNewDB() throws java.sql.SQLException {
        //Since we are calculating running averages, we know how much data will be in our
        //data base at the beginning, so we initialize the rings and arcs with the unique IDs.
        System.out.println("We're initializing the database");
        String tablePrefix = "layer";

        for (Integer i = 0; i < 50; i++) {
            String tableName = tablePrefix.concat(i.toString());
            this.database.addTable(tableName);
            PreparedStatement stat = this.database.insertStatement(tableName);
            
            String rowname = "";
            
            int last_ring = params.getIDpolar(beamCalWidth,0)[0];
            for ( int ring = 0; ring <= last_ring; ring++ ) {
                
                int last_arc = params.getArcsInRing(ring) - 1;
                for (int arc = 0; arc <= last_arc; arc++ ) {
                    rowname = params.IDtoString(ring,arc);
                    
                    //Create a row with our fairly simple rowname.
                    stat.setString(1,rowname);
                    stat.setDouble(2,0.0);
                    stat.setInt(3,0);
                    stat.addBatch();
                }
            }
            
            
            this.database.commit(stat);
        }
        
    }


        //Here we initiate the BeamCalorimeterHit Object then save information
        //about the average energy per tile
        //This is subdivided into groups of layers 5 groups of 10 (layers in Z)
        //Consolidate the hit list into Tiles
        //Update the data base with the energy of the tiles
    public void process( EventHeader event ) {
        super.process(event);
        List<SimCalorimeterHit> hits = event.get(SimCalorimeterHit.class, "BeamCalHits");
        
        System.out.println("processing Tiles" + hits.size());
        tiler.clear(); 
        tiler.process(hits);
        List< HashMap<String, BeamCalorimeterTile> > tiles = tiler.getTiles();
        
        
        double val;
        String id;
        int layer = 0;
        int  endLayer = 49;
        HashMap<String, Double[]> avgMap;
        HashMap<String, BeamCalorimeterTile> map;
        
        for (layer = 0; layer <= endLayer; layer++) {
            avgMap = avgList.get(layer);
            map = tiles.get(layer);
            
            for ( Map.Entry<String, BeamCalorimeterTile> entry : map.entrySet() ) {
                BeamCalorimeterTile tile = entry.getValue();
                
                id  = tile.toString();
                val = tile.getEnergy();
                     
                //update the value of the row
                if ( avgMap.containsKey(id) ) {
                    Double[] data = avgMap.get(id);
                    double prevEnergy = data[0].doubleValue();
                    int prevWeight = (int)Math.round( data[1].doubleValue() );
                    
                    double energy = val + prevEnergy;
                    int    weight = prevWeight+1;
                    
                    data[0] = new Double(energy);
                    data[1] = new Double(weight);
                    
                } else {
                    Double[] data = new Double[2];
                    Double energy = new Double( val );
                    Double weight = new Double( 1.0 );
                    
                    data[0] = energy;
                    data[1] = weight;
                    
                    avgMap.put(id,data);
                }
            }
        }
        
        System.out.println("Finished Event " + eventCount++);
        
    }//End Process
    
    
    
    private BeamCalorimeterTiler tiler;
    private SQLITEWrapper database;
    private int lastLayer;
    private int number_of_layers = 50;
    private int eventCount;

    private String connectionName;
    private List< HashMap<String, Double[]> > avgList;


    private TileParameters params;
    private double cellSize;
    private int spreadFactor;
    private boolean removeNegative;
    private boolean aligned;
    private float beamCalWidth = 180.0F;
}
