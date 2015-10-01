/*
 * BeamCalorimeterPallet.java
 *
 * Created on May 4 2014, 3:59PM
 *
 * Authors: Alex Bogart and Christopher Milke
 *
 * The BeamCalorimeterPallet interface is designed to define how the segmentation
 * should be implemented on the Beam Calorimeter. Also note, since the geometry
 * of the Beam Calorimeter may vary with detector versions, the programmer is
 * responsible for this information, e.g. the programmer is responsible
 * for determining whether a Pallet lies in the geometry of the Beam Calorimeter or not.   
 */
package org.lcsim.contrib.scipp.beamcal;

import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterTile;
import org.lcsim.contrib.scipp.beamcal.database.DataBaseBeamCalorimeterTile;

import java.util.HashSet;
import java.util.Map;



public interface BeamCalorimeterPallet extends Comparable {
    
    //Takes a hashmap (Map<TileID,TileEnergy>) containing all tiles and
    //their respective energies, determins which tiles among the hashmap 
    //this pallet is composed of, and add the energy of all of those tiles
    //to this pallet.   
    public int addEnergy( Map<String, BeamCalorimeterTile> tileMap );
    
    public int addDBEnergy( Map<String, DataBaseBeamCalorimeterTile> DBtileMap );
    
    //Returns the layer this pallet was built on.
    public int getLayer();
    
    //Returns the ID of the Reference Tile of this Pallet.
    public short[] getRef();
    
    //Returns the radius of Pallet, where the radius is the number of tiles
    //around the reference tiles.
    public int getPalletRadius();
    
    //Returns the energy stored in this pallet.
    public double  getEnergy();
    
    //Returns the weight of this Pallet. 
    public int  getWeight();
    
    //Takes another Pallet "pal" and returns TRUE if this pallet has any 
    //of the same constituent tiles as pal.
    public boolean overlaps(BeamCalorimeterPallet pal);
    
    //Returns a hashset containing all the tiles this pallet is made up of.
    public HashSet<String> getConstituentTiles();
    
    //Returns the ID of the pallet in string form.
    public String toString();
    
    //Compares this pallet to another, based on energy.
    public int compareTo(Object o);
}


