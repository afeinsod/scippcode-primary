/*
 * BeamCalorimeterHit.java
 *
 * Created on Mar 17 2011, 10:26 AM
 * Edited  on Feb 17 2014, 01:26 AM
 * 
 * @version 2.1
 *
 * Defines and manages utilities for a tiling scheme on the beam calorimeter
 * detector. 
 * 
 */
package org.lcsim.contrib.scipp.beamcal;

import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.contrib.scipp.beamcal.TileParameters;

import java.lang.Comparable;

public interface BeamCalorimeterTile extends Comparable
{
    //Returns the ID of this tile.
    public short[] getTileID();

    //Returns the layer this tile is on.
    public int getLayer();

    //Returns the energy on this tile.
    public double getEnergy();

    //Returns the weight of this tile,
    //which is essentially the number of hits on the tile. 
    public int getWeight();

    //Attempts to add the SimCalorimeterHit to the tile 
    //if the hit is within the tile's boundries.
    public boolean addHit(SimCalorimeterHit hit);
    
    public void addEnergy(double energy);

    //Returns the tile paramater class of the tile
    public TileParameters getParams();

    //Returns whether the tile contains the point in question
    public boolean contains(float[] point);
    
    //Returns whether the tile contains the point in question
    public boolean contains(double[] point);

    //Returns the tile ID as a string 
    public String toString();

    //Determine if two tiles are equal based on position and energy.
    public boolean equals(BeamCalorimeterTile t);

    //Compare tiles based on their position and energy.
    public int compareTo(Object o);

    //Simulates electronic noise due to radiation damage.
    public void addNoise(double noisiness);
}
