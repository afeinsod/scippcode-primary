/*
 * BaseBeamCalorimeterTile.java.
 *
 * Created on Mar 17 2011, 10:26 AM
 * Updated on Aug 30 2014, 02:42 AM
 *
 * @author Alex Bogert and Christopher Milke
 * @version 2.1 
 * 
 * This class represents a Tiling scheme implemented on the 
 * BeamCalorimeter Geometery for the current
 * sid02 design(2011).
 * 
 * Version 2.0 Notes: 
 * Modified from a square grid to a radial tiling 
 * scheme with added flexibility and implements
 * the ability to add electronic noise
 */
package org.lcsim.contrib.scipp.beamcal.base;

import org.lcsim.contrib.scipp.beamcal.TileParameters;
import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterTile;

import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.base.BaseSimCalorimeterHit;

import org.lcsim.event.MCParticle;

import java.lang.String;

import java.util.List;
import java.util.ArrayList;

public class BaseBeamCalorimeterTile implements BeamCalorimeterTile,Comparable
{ 
    //CONSTRUCTORS
    public BaseBeamCalorimeterTile(BeamCalorimeterTile t)
    {
        this.params = t.getParams();
        this.tileID = t.getTileID();
        this.layer  = t.getLayer();
        this.weight = t.getWeight();
        this.energy = t.getEnergy();
    }
    
    public BaseBeamCalorimeterTile(SimCalorimeterHit hit, TileParameters new_params)
    {
        this( hit.getPosition(), new_params );
        this.addHit(hit);
    }
    
    
    public BaseBeamCalorimeterTile(float[] point, TileParameters new_params) {
        if (point.length > 3 || point.length < 2) {
            throw new java.lang.RuntimeException("tile can only move to a 2 or 3 dimensional point tile length " + point.length);
        }
        
        double[] dpoint = new double[2];
        dpoint[0] = point[0];
        dpoint[1] = point[1];

        this.params = new_params;
        this.calculateTileIDInformation(dpoint);
        this.weight = 0;
        this.energy = 0;
        this.layer  = 0;
    }
    
    
    //Generates a tile ID from a 2 or 3 dimensional point (no z information is currently used). 
    public BaseBeamCalorimeterTile(double[] point, TileParameters new_params)
    {
        if (point.length > 3 || point.length < 2) {
            throw new java.lang.RuntimeException("tile can only move to a 2 or 3 dimensional point tile length " + point.length);
        }
  
        this.params = new_params;
        this.calculateTileIDInformation(point);
        this.weight = 0; 
        this.energy = 0;
        this.layer  = 0;
    }
    
    
    public BaseBeamCalorimeterTile(TileParameters new_params, double[] point, int L, double E)
    {
        if (point.length > 3 || point.length < 2) {
            throw new java.lang.RuntimeException("tile can only move to a 2 or 3 dimensional point tile length " + point.length);
        }
  
        this.params = new_params;
        this.calculateTileIDInformation(point);
        this.weight = 1; 
        this.energy = E;
        this.layer  = L;
    }
    //END CONSTRUCTORS
    
    
    
    //PUBLIC FUNCTIONS
    
    //takes a SimCalorimeterHit, extracts the useful information from it,
    //and adds it to this tile
    public boolean addHit(SimCalorimeterHit hit) {
        boolean hitAdded = false; 
        if (this.contains( hit.getPosition() )) {
            this.energy += hit.getCorrectedEnergy();
            this.layer  = hit.getLayerNumber();
            this.weight++;
            this.zPosition = (float)hit.getPosition()[2];
            hitAdded = true;
        }
        return hitAdded;
    }
    
    
    //adds the given energy to this tile, and bumps this tile's weight
    public void addEnergy(double E) {
        this.energy += E;
        this.weight++;
    }


    //Returns the layer this tile is on.
    public int getLayer() {
        return this.layer;
    }


    //Returns the tile paramater class of the tile
    public TileParameters getParams() {
        return this.params;
    }


    //Compare tiles based on their position and energy.
    //Returns a negative integer, zero, or a positive integer if
    //this object is less than, equal to, or greater than the specified object.
    public int compareTo(Object o) {
        if (!(o instanceof BeamCalorimeterTile)) {
            throw new java.lang.ClassCastException("Can only compare to other BeamCalorimeterTiles");
        }
        int val = 0;
        if (this.energy - ((BeamCalorimeterTile) o).getEnergy() > 0) {
            val = 1;
        }
        else if (this.energy - ((BeamCalorimeterTile) o).getEnergy() == 0) {
            val = 0;
        }
        else  {
            val = -1;
        }
        return val;
    }


    //Determine if two tiles are equal based on position and energy.
    //Two BeamCalorimeterHits are consider equal 
    //if they occupy the same tile and layer.
    public boolean equals(BeamCalorimeterTile o) {
        if (!(o instanceof BaseBeamCalorimeterTile)) {
            System.out.println("Not equal because not instance of");
            return false;
        }

        boolean equality = false;
        short[] oID = o.getTileID();
        if ( this.tileID[0] == oID[0] && this.tileID[1] == oID[1] ) { 
            equality = true;
        }
        return equality;
    }


    // Returns whether the tile contains the point in question
    public boolean contains(double[] point) {
        short[] pointID = this.params.getID(point[0],point[1]); 
        return (this.tileID[0] == pointID[0] && this.tileID[1] == pointID[1]);
    }


    // Returns whether the tile contains the point in question
    public boolean contains(float[] point) {
        double[] p = { point[0], point[1] };
        return this.contains(p);
    }


    //Returns the ID of this tile.
    public short[] getTileID() {
        return this.tileID;
    }
    
    
    //Returns the energy on this tile.
    public double getEnergy() {
        return this.energy;
    }


    //Returns the weight of this tile,
    //which is essentially the number of hits on the tile.
    public int getWeight() {
        return this.weight;
    }
    
    
    //Takes the position of a hit and generates an ID around it.
    private void calculateTileIDInformation(double[] position) {
        this.tileID = this.params.getID(position[0],position[1]);
        return; 
    }


    //Returns a string of the tile ID.
    public String toString() {
        return params.IDtoString(tileID[0],tileID[1]);
    }


    //Simulates electronic noise due to radiation damage.
    //The noise added is randomly generated from a gaussian
    //and noise level.
    public void addNoise(double noisiness) {
        this.energy += this.randomGaussian() * noisiness;
    }

    //Implementation of the Box-Muller Transform to produce
    //a random gaussian from two random uniform numbers
    private double randomGaussian() {
        double random1 = Math.random();
        double random2 = Math.random();

        double sinx = Math.sin(2*Math.PI*random1);
        double lny  = Math.log(random2);

        //box-muller transform
        double gaussian_number = sinx * Math.sqrt( (-2) * lny );

        return gaussian_number;
    }

   
    private TileParameters params;

    private short[] tileID;

    private float zPosition;

    private int layer  = -1;
    private int weight = 0;
    private double energy = 0.0;
}
