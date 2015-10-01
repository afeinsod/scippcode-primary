/*
 * BeamCalorimeterTiler.java
 *
 * Created on Sep 03, 2011, 10:13 PM
 * Updated on Aug 30, 2014, 02:43 AM
 * @author Alex Bogert and Christopher Milke
 *
 * Dedicated to Roger, the tiler.
 * 
/* This interface can be used to overlay singal events ontop of background
 * in the BeamCalorimeter. More specifically this class will consolidate all 
 * SimCalorimeterHits in the event in the "BeamCalHits" collection into a list of
 * BeamCalorimeterTiles. The tiles can be converted to DataBaseBeamCalorimeterTiles
 * providing access to the expected average background energy of the tile. 
 *
 */
package org.lcsim.contrib.scipp.beamcal;

import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterTile;
import org.lcsim.contrib.scipp.beamcal.database.DataBaseBeamCalorimeterTile;
import org.lcsim.contrib.scipp.beamcal.database.TileDataBaseReader;

import org.lcsim.event.SimCalorimeterHit;

import java.util.List;
import java.util.Map;
import java.util.HashMap;



public interface BeamCalorimeterTiler {
 
    //MANIPULATOR FUNCTIONS

    /* "process" will attempt to consolidate SimCalorimeterHits into the tiling of the
       BeamCalorimeter. If the user does not call, clear(). Then processing new hits
       will consolidate the new hits into the existing tiles. This can be used to
       overlay signal events on top of background.
    */

    //Takes each of the given hits, identifies a tile that it fits within,
    //and then applies the energy of the hit to that tile.
    public void process(List<SimCalorimeterHit> simHits);
    
    
    //Removes all tiles from the lists.
    public void clear();
    
    
    
    //ACCESS FUNCTIONS
    
    public List< HashMap<String, BeamCalorimeterTile> > getTiles();
    
    
    public Map<String, BeamCalorimeterTile> getTiles_onLayer(int layer);
    
    
    public List< HashMap<String, BeamCalorimeterTile> > getTiles_betweenLayers(
            int start_layer, int end_layer);
    
    
    public List< HashMap<String, DataBaseBeamCalorimeterTile> > getDBTiles(
            TileDataBaseReader db) throws java.sql.SQLException;
    
    
    public Map<String, DataBaseBeamCalorimeterTile> getDBTiles_onLayer(int layer,
            TileDataBaseReader db) throws java.sql.SQLException;
            
            
    public List< HashMap<String, DataBaseBeamCalorimeterTile> > getDBTiles_betweenLayers(
            int start_layer, int end_layer, 
            TileDataBaseReader db) throws java.sql.SQLException;
}
