/**
 * DataBaseBeamCalorimeterTile.java.
 *
 * Created on June 20 2011, 8:19 PM
 * Edited  on May  25 2014, 5:13 AM
 *
 * @author Alex Bogert
 * @version 1.2
 *
 * This class added utilites to the BaseBeamCalorimeterTile class by giving access to 
 * a sqlite data base which contains information about the average energy of the tiles
 */
package org.lcsim.contrib.scipp.beamcal.database;

import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterCylinder;

import org.lcsim.contrib.scipp.beamcal.base.BaseBeamCalorimeterCylinder;

import org.lcsim.contrib.scipp.beamcal.database.CylinderDataBaseReader;

import java.util.List;
import java.util.ArrayList;

public class DataBaseBeamCalorimeterCylinder extends BaseBeamCalorimeterCylinder
    implements BeamCalorimeterCylinder,Comparable
{ 
    //Generates a tile ID from a 2 or 3 
    //dimensional point (no z information is currently used).
    public DataBaseBeamCalorimeterCylinder(BeamCalorimeterCylinder c)
    {
        super(c);
    }
    
    
    //Used to compare DB cylinders by layer and subtracted energy.
    public int compareTo(Object o) {
        if (!(o instanceof DataBaseBeamCalorimeterCylinder)) {
            throw new java.lang.ClassCastException(
                "Can only compare to other DataBaseBeamCalorimeterCylinders");
        }
        int val = 0;
        if (this.getSubtractedEnergy() 
               - ((DataBaseBeamCalorimeterCylinder) o).getSubtractedEnergy() > 0) {
            val = 1;
        }
        else if (this.getSubtractedEnergy()
               - ((DataBaseBeamCalorimeterCylinder) o).getSubtractedEnergy() == 0) {
            val = 0;
        }
        else  {
            val = -1;
        }
        return val;
    }
    
    
    
    //Returns the standard deviation of the energy  
    //deposited on this cylinder as recorded in the sql Database.
    public double getSTDev() {
        return Math.sqrt(this.beamstrahlungAvgSQREnergy  -  this.beamstrahlungAvgEnergy*this.beamstrahlungAvgEnergy);
    }
    
    
    //Returns average-background-subtracted energy of the cylinder.
    public double getSubtractedEnergy() {
        return this.getEnergy() - this.beamstrahlungAvgEnergy;
    }
    
    
    //Returns the average background energy deposited on 
    //this cylinder as recorded in the sql Database.
    public double getBeamStrahlungAvgEnergy() {
        return this.beamstrahlungAvgEnergy;
    }
    
    
    //Returns the row weight of this cylinder as recorded in the sql Database
    public int getBeamStrahlungRowWeight() {
        return this.beamstrahlungRowWeight;
    }
    
    
    //Returns whether this cylinder has had information from
    //the sql database applied to it yet.
    public boolean hasDBInfo() {
        return this.hasDBInfo;
    }
    
    
    //Applies information from the sql database to this tile.
    public boolean setDBInfo(CylinderDataBaseReader energyDB) {
        return this.findEnergyInfoFrom(energyDB);
    }
    
    
     //Obtains average energy, rowWeight, and energy standard
     //deviation from the sql database.
    private boolean findEnergyInfoFrom(CylinderDataBaseReader energyDB) {
            boolean b = true;
            try { 
                Object[] row = energyDB.getRow(this);
                this.beamstrahlungAvgEnergy    = (Double)row[1];
                this.beamstrahlungAvgSQREnergy = (Double)row[2];
                this.beamstrahlungRowWeight    = (Integer)row[3]; //this is the weight of the average
                
                if (this.beamstrahlungRowWeight > 1) this.hasDBInfo = true;
                
            }catch (java.sql.SQLException e) {
                b = false;
            }
            
            return b;
    }



    private boolean hasDBInfo = false;
    private double  beamstrahlungAvgEnergy    = -1;
    private double  beamstrahlungAvgSQREnergy = -1;
    private int     beamstrahlungRowWeight    = -1;
}
