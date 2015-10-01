/*
 * BaseBeamCalorimeterCylinder.java.
 *
 * Created on Mar 17 2011, 10:26 AM
 * Updated on May 25 2014, 04:00 AM
 *
 * @author Alex Bogert and Christopher Milke
 * @version 2.0
 *
 * A virtual structure designed to combine a number of pallets with
 * the same x,y coordinates on different layers.
 */
 
package org.lcsim.contrib.scipp.beamcal.base;

import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterTile;
import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterCylinder;
import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterPallet;

import org.lcsim.contrib.scipp.beamcal.database.DataBaseBeamCalorimeterTile;
import org.lcsim.contrib.scipp.beamcal.database.TileDataBaseReader;

import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.base.BaseSimCalorimeterHit;

import org.lcsim.event.MCParticle;

import java.lang.String;
import java.lang.Double;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;


public class BaseBeamCalorimeterCylinder implements BeamCalorimeterCylinder { 
    
    public BaseBeamCalorimeterCylinder(BeamCalorimeterCylinder c) {
        this.ref      = c.getRef();
        this.StringID = c.toString();
        this.ids      = c.getIDs();
        this.energy   = c.getEnergy();
    }
    
    
    public BaseBeamCalorimeterCylinder(BeamCalorimeterPallet p) {
        this.ref      = p.getRef();
        this.StringID = p.toString();
        this.ids      = p.getConstituentTiles();
    }
    
    //this constructor is exclusively used in CylinderDataBaseGenerator
    public BaseBeamCalorimeterCylinder(BeamCalorimeterPallet p, double Penergy) {
        this.ref      = p.getRef();
        this.StringID = p.toString();
        this.ids      = p.getConstituentTiles();
        this.energy   = Penergy;
    }
    
    
    //Returns the ID of the Reference Tile of this Pallet.
    public short[] getRef() {
        return this.ref;
    }
    
    
    //Returns the energy contained in this cylinder.
    public double getEnergy() {
        return this.energy;
    }
    
    
    //Returns a hashset containing the ids of all the tiles
    //the cylinder contains. Repeat tiles on different layers
    //do not have their ids included (as tile IDs do not include 
    //layer  info, these ids would all be identical if they were included).
    public HashSet<String> getIDs() {
        return this.ids;
    }
    
    
    //Returns a string identifying this cylinder.
    public String toString() {
        return this.StringID;
    }
    
    
    //Adds the given energy of the given tile to the cylinder,
    //if this cylinder contains the tile. 
    public boolean addTile(BeamCalorimeterTile t) {
        String id = t.toString();
        if ( this.ids.contains(id) ) {
            double E = t.getEnergy();
            this.energy += E;
            
            return true;
        }
        
        return false;
    }
    
    
    //Same as above, using database tiles.
    public boolean addTile(DataBaseBeamCalorimeterTile t) {
        String id = t.toString();
        if ( this.ids.contains(id) ) {
            double E = t.getSubtractedEnergy();
            this.energy += E;
            
            return true;
        }
        
        return false;
    }
    
    
    
    //Similar to the above two functions, but adds several tiles at once given 
    //a tilehash of tiles.
    public void addTileList(List< HashMap<String, BeamCalorimeterTile> > tileList) {
        for ( String id : ids ) {
            for ( HashMap<String, BeamCalorimeterTile> map : tileList ) {
                if ( map.containsKey(id) ) {
                    double E = map.get(id).getEnergy();
                    this.energy += E;
                }
            }
        }
    }
    
    
    
    //Same as the above function, but uses a list of tiles and an OPENED database reader.
    public void addBGTileList(List< HashMap<String, BeamCalorimeterTile> > tileList, TileDataBaseReader dbreader1) throws java.sql.SQLException {
        int layer = 0;
        for ( HashMap<String, BeamCalorimeterTile> map : tileList ) {
            for ( String id : ids ) {
                if ( map.containsKey(id) ) {
                    Object[] row = dbreader1.getRow(id,layer);
                    double AverageE = (Double)row[1];
                    
                    double E = map.get(id).getEnergy();
                    double subtractedE = E - AverageE;
                    
                    this.energy += subtractedE;
                }
            }
            
            layer++;
        }
    }
    
    

    
    
    
    private String StringID;
    private HashSet<String> ids = new HashSet<String>();
    private short[] ref = new short[2];
    private double energy = 0.0;
}
