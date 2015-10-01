/*
 * BeamCalorimeterCylinder.java
 * 
 * Created on March 17 2011, 10:26 AM
 * Updated on June  13 2014, 01:43 PM
 * 
 * @author Alex Bogert and Christopher Milke
 * @version 2.0
 * 
 * A virtual structure designed to combine a number of pallets with
 * the same x,y coordinates on different layers. 
 *
 */

package org.lcsim.contrib.scipp.beamcal;

import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterTile;
import org.lcsim.contrib.scipp.beamcal.database.DataBaseBeamCalorimeterTile;
import org.lcsim.contrib.scipp.beamcal.database.TileDataBaseReader;

import java.util.List;
import java.util.HashSet;
import java.util.HashMap;


public interface BeamCalorimeterCylinder 
{   
    //Returns a hashset containing the ids of all the tiles
    //the cylinder contains. Repeat tiles on different layers
    //do not have their ids included (as tile IDs do not include 
    //layer  info, these ids would all be identical if they were included). 
    public HashSet<String> getIDs();
    
    //Returns the ID of the Reference Tile of this Pallet.
    public short[] getRef();
    
    //Returns the energy contained in this cylinder.
    public double getEnergy();
    
    //Returns a string identifying this cylinder.
    public String toString();

    //Adds the given energy of the given tile to the cylinder,
    //if this cylinder contains the tile.    
    public boolean addTile(BeamCalorimeterTile t);
    
    //Same as above, using database tiles.
    public boolean addTile(DataBaseBeamCalorimeterTile t);
    
    //Similar to the above two functions, but adds several tiles at once given 
    //a tilehash of tiles.
    public void addTileList(List< HashMap<String, BeamCalorimeterTile> > tileList);
    
    //Same as the above function, but uses a list of database tiles.
    public void addBGTileList(List< HashMap<String, BeamCalorimeterTile> > tileList, TileDataBaseReader dbreader1) throws java.sql.SQLException;
}
