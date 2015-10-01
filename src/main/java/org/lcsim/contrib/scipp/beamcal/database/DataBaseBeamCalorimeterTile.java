/**
 * DataBaseBeamCalorimeterTile.java.
 *
 * Created on June 20 2011, 8:19 PM
 * Edited  on May  25 2014, 4:00 AM
 *
 * @author Alex Bogert
 * @version 1.2
 *
 * This class added utilites to the BaseBeamCalorimeterTile class by giving access to 
 * a sqlite data base which contains information about the average energy of the tiles
 */
package org.lcsim.contrib.scipp.beamcal.database;

import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterTile;

import org.lcsim.contrib.scipp.beamcal.base.BaseBeamCalorimeterTile;

import org.lcsim.contrib.scipp.beamcal.database.TileDataBaseReader;

import java.util.List;
import java.util.ArrayList;

public class DataBaseBeamCalorimeterTile extends BaseBeamCalorimeterTile 
    implements BeamCalorimeterTile,Comparable
{ 
    //Generates a tile ID from a 2 or 3 
    //dimensional point (no z information is currently used).
    public DataBaseBeamCalorimeterTile(BeamCalorimeterTile tile)
    {
        super(tile);
    }
    
    
    //Used to compare DB tiles by layer and subtracted energy.
    public int compareTo(Object o) {
        if (!(o instanceof DataBaseBeamCalorimeterTile)) {
            throw new java.lang.ClassCastException(
                "Can only compare to other DataBaseBeamCalorimeterTiles");
        }
        int val = 0;
        if (this.getSubtractedEnergy() 
               - ((DataBaseBeamCalorimeterTile) o).getSubtractedEnergy() > 0) {
            val = 1;
        }
        else if (this.getSubtractedEnergy()
               - ((DataBaseBeamCalorimeterTile) o).getSubtractedEnergy() == 0) {
            val = 0;
        }
        else  {
            val = -1;
        }
        return val;
    }
    
    
    //Returns average-background-subtracted energy of the tile.
    public double getSubtractedEnergy() {
        return this.getEnergy() - this.beamstrahlungAvgEnergy;
    }
    
    
    //Returns the average background energy deposited on 
    //this tile as recorded in the sql Database.
    public double getBeamStrahlungAvgEnergy() {
        return this.beamstrahlungAvgEnergy;
    }
    
    
    //Returns the row weight of this tile as recorded in the sql Database.
    public int getBeamStrahlungRowWeight() {
        return this.beamstrahlungRowWeight;
    }
    
    
    //Returns whether this tile has had information from
    //the sql database applied to it yet.
    public boolean hasDBInfo() {
        return this.hasDBInfo;
    }
    
    
    //Applies information from the sql database to this tile.
    public void setDBInfo(TileDataBaseReader energyDB) throws java.sql.SQLException {
        this.findEnergyInfoFrom(energyDB);
        this.hasDBInfo = true;
    }
    
    
    //Obtains average energy and rowWeight from the sql database.
    private void findEnergyInfoFrom(TileDataBaseReader energyDB) throws java.sql.SQLException {
            Object[] row = energyDB.getRow(this);
            this.beamstrahlungAvgEnergy = (Double)row[1];
            this.beamstrahlungRowWeight = (Integer)row[2]; //this is the weight of the average
    }

    private boolean hasDBInfo = false;
    private double  beamstrahlungAvgEnergy = -1;
    private int     beamstrahlungRowWeight = -1;
}
