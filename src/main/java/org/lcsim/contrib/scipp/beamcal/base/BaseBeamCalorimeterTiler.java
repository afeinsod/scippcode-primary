/*
 * BaseBeamCalorimeterTiler.java
 *
 * Created on Mar 19, 2011, 11:01 PM
 * Updated on Aug 30, 2014, 02:43 AM
 * @author Alex Bogert and Christopher Milke
 * 
 * @version 3.0
 *
 * Dedicated to Roger, the tiler.
 * 
 * This class can be used to overlay signal events on top of background
 * in the BeamCalorimeter. More specifically this class will consolidate all 
 * SimCalorimeterHits in the event in the "BeamCalHits" collection into a list of
 * BeamCalorimeterTiles. The tiles can be converted to DataBaseBeamCalorimeterTiles
 * providing access to the expected average background energy of the tile. 
 * 
 */
       
package org.lcsim.contrib.scipp.beamcal.base;

import org.lcsim.contrib.scipp.beamcal.TileParameters;
import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterTile;
import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterTiler;
import org.lcsim.contrib.scipp.beamcal.base.BaseBeamCalorimeterTile;
import org.lcsim.contrib.scipp.beamcal.geometry.PolarCoords;

import org.lcsim.contrib.scipp.beamcal.database.DataBaseBeamCalorimeterTile;
import org.lcsim.contrib.scipp.beamcal.database.TileDataBaseReader;

import org.lcsim.event.SimCalorimeterHit;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.Comparator;



public class BaseBeamCalorimeterTiler implements BeamCalorimeterTiler {

    //CONSTRUCTORS
    public BaseBeamCalorimeterTiler(TileParameters new_params, double size, int spread, boolean rmN, boolean algn){
        System.out.println("New parameters set to: " + new_params);
        this.params = new_params;
        this.cellSize = size;
        this.spreadFactor = spread;
        this.removeNegative = rmN;
        this.align = algn;
        
        this.tile_maps = new ArrayList< HashMap<String, BeamCalorimeterTile> >();
        
        for (int layer = 0; layer < number_of_layers; layer++) {
            HashMap<String, BeamCalorimeterTile> map;
            map = new HashMap<String, BeamCalorimeterTile>();
            
            tile_maps.add(map);
        }
    }

 
    //MANIPULATOR FUNCTIONS

    /* "process" will attempt to consolidate SimCalorimeterHits into the tiling of the
       BeamCalorimeter. If the user does not call, clear(). Then processing new hits
       will consolidate the new hits into the existing tiles. This can be used to
       overlay signal events on top of background.
    */

    //Takes each of the given hits, identifies a tile that it fits within,
    //and then applies the energy of the hit to that tile.
    public void process(List<SimCalorimeterHit> simHits) {
        if (simHits.size() == 0) return;
        
        double dim = cellSize / ( (double)spreadFactor );
        double Ediv = (double) (spreadFactor * spreadFactor);
        
        double oldEnergy, spreadEnergy;
        double[] oldPos;
        double[] spreadPos = new double[2];
        int i,j,layer;
        boolean foundTile;
        
        for (SimCalorimeterHit hit : simHits) {
            oldEnergy = hit.getCorrectedEnergy();
            oldPos = hit.getPosition();
            if (align) oldPos = PolarCoords.ZtoBeamOut(oldPos[0],oldPos[1],oldPos[2]);
            layer = hit.getLayerNumber();
            
            if ( removeNegative && (oldPos[2]<0) ) continue;
            
            spreadEnergy = oldEnergy / Ediv;
            for (i = 0; i < spreadFactor; i++) {
                spreadPos[0] = (i*dim) + oldPos[0] + (dim/2.0) - (cellSize/2.0);
                for (j = 0; j < spreadFactor; j++) {
                    spreadPos[1] = (j*dim) + oldPos[1] + (dim/2.0) - (cellSize/2.0);
                    
                    foundTile = this.findTileFor(spreadPos,layer,spreadEnergy);
                    if (!foundTile) this.hash_new_tile(spreadPos,layer,spreadEnergy);
                }
            }
        }
    }
    
    
    //Removes all tiles from the lists.
    public void clear() {
        for ( Map<String, BeamCalorimeterTile> map : tile_maps ) {
            map.clear();
        }
        tile_maps = null;
        
        this.tile_maps = new ArrayList< HashMap<String, BeamCalorimeterTile> >();
        
        for (int layer = 0; layer < number_of_layers; layer++) {
            HashMap<String, BeamCalorimeterTile> map;
            map = new HashMap<String, BeamCalorimeterTile>();
            
            tile_maps.add(map);
        }
    }
    
    
    //Identifies if there already exists a tile
    //which the hit can be applied to, and adds energy
    //to that tile if it does.
    private boolean findTileFor(double[] pos, int layer, double energy) {
        short[] tempID = params.getID(pos[0],pos[1]);
        String key = params.IDtoString(tempID[0],tempID[1]);
        
        if ( tile_maps.get(layer).containsKey(key) ) {
            tile_maps.get(layer).get(key).addEnergy(energy);
            return true;
        }
        
        else return false;
    }
    
    
    //creates a new tile with the given position on the given layer with the given energy,
    //the places into the hash-table list.
    private void hash_new_tile(double[] pos, int layer, double energy) {
        BaseBeamCalorimeterTile new_tile;
        
        new_tile = new BaseBeamCalorimeterTile(this.params,pos,layer,energy);
        String key = new_tile.toString();
        
        tile_maps.get(layer).put(key,new_tile);
    }
    
    
    
    //ACCESS FUNCTIONS
    
    //returns the entire list of hashtables (all tiles from all layers)
    public List< HashMap<String, BeamCalorimeterTile> > getTiles() {
        return tile_maps;
    }
    
    
    //returns the tiles on the specified layer
    public Map<String, BeamCalorimeterTile> getTiles_onLayer(int layer) {
        return tile_maps.get(layer);
    }
    
    
    //returns the tiles on all the layers between the two provided layers
    //i.e. providing start_layer=3 and end_layer=8 will return tiles from
    //layers 3,4,5,6,7 but NOT from 8.
    public List< HashMap<String, BeamCalorimeterTile> > getTiles_betweenLayers(
            int start_layer, int end_layer) {
                
        return tile_maps.subList(start_layer,end_layer);
    }
    
    
    //same as getTiles, but with DB tiles. Note that this takes a while
    public List< HashMap<String, DataBaseBeamCalorimeterTile> > getDBTiles(
            TileDataBaseReader db) throws java.sql.SQLException{
        
        List< HashMap<String, DataBaseBeamCalorimeterTile> > dbtile_maps;
        dbtile_maps = new ArrayList< HashMap<String, DataBaseBeamCalorimeterTile> >();
        
        for ( int i = 0; i < tile_maps.size(); i++ ) {
            Map<String, BeamCalorimeterTile> map = tile_maps.get(i);
            dbtile_maps.add( create_DBmap(map,db) );
        }
        
        return dbtile_maps;
    }
    
    
    //same as getTiles_onLayer, but with DB tiles. Note that this takes a while
    public Map<String, DataBaseBeamCalorimeterTile> getDBTiles_onLayer(int layer,
            TileDataBaseReader db) throws java.sql.SQLException {
        
        Map<String, BeamCalorimeterTile> map = tile_maps.get(layer);
        return create_DBmap(map,db);
        
        
    }
    
    //same as getTiles_betweenLayers, but with DB tiles. Note that this takes a while
    public List< HashMap<String, DataBaseBeamCalorimeterTile> > getDBTiles_betweenLayers(
            int start_layer, int end_layer, 
            TileDataBaseReader db) throws java.sql.SQLException{
        
        List< HashMap<String, DataBaseBeamCalorimeterTile> > dbtile_maps;
        dbtile_maps = new ArrayList< HashMap<String, DataBaseBeamCalorimeterTile> >();
        
        List< HashMap<String, BeamCalorimeterTile> > sub_maps;
        sub_maps = tile_maps.subList(start_layer,end_layer);
        
        
        for ( int i = 0; i < sub_maps.size(); i++ ) {
            Map<String, BeamCalorimeterTile> map = sub_maps.get(i);
            dbtile_maps.add(create_DBmap(map,db) );
        }
        
        return dbtile_maps;
    }
    
    
    //takes the given normal tile map and goes through each tile in it,
    //generating a DBtile from that tile and adding it to a newly created
    //Dbtile map.
    private HashMap<String, DataBaseBeamCalorimeterTile> create_DBmap(
            Map<String, BeamCalorimeterTile> map, TileDataBaseReader db)
            throws java.sql.SQLException {
                
        HashMap<String, DataBaseBeamCalorimeterTile> DBmap;
        DBmap = new HashMap<String, DataBaseBeamCalorimeterTile>();
        
        DataBaseBeamCalorimeterTile dbtile = null;
        
        for ( Map.Entry<String, BeamCalorimeterTile> entry : map.entrySet() ) {
            String key = entry.getKey();
            BeamCalorimeterTile old_tile = entry.getValue();
            
            dbtile = new DataBaseBeamCalorimeterTile(old_tile);
            dbtile.setDBInfo(db);
            
            DBmap.put(key,dbtile);
        }
        
        return DBmap;
    }
    
    
    
    private TileParameters params;
    private double cellSize;
    private int spreadFactor;
    private boolean removeNegative;
    private boolean align;
    
    private List< HashMap<String, BeamCalorimeterTile> > tile_maps;
    
    private int number_of_layers = 50;
    
    
    
}
