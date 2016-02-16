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
import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterTiler;
import org.lcsim.contrib.scipp.beamcal.geometry.PolarCoords;

import org.lcsim.event.SimCalorimeterHit;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.Comparator;


public class BaseBeamCalorimeterTiler implements BeamCalorimeterTiler {
    

    //CONSTRUCTOR
    public BaseBeamCalorimeterTiler(TileParameters new_params, double size, int spread, boolean rmN, boolean algn){
        System.out.println("New parameters set to: " + new_params);
        this.params = new_params;
        this.cellSize = size;
        this.spreadFactor = spread;
        this.removeNegative = rmN;
        this.align = algn;
        
        this.tiles = new ArrayList(number_of_layers);
        
        for (int layer = 0; layer < number_of_layers; layer++) {
            HashMap<String, Double> map= new HashMap();
            tiles.add(layer, map);
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
                    short[] tempID = params.getID(spreadPos[0],spreadPos[1]);
                    String key = params.IDtoString(tempID[0],tempID[1]);
                    
                    if (tiles.get(layer).containsKey(key)) {
                        tiles.get(layer).put(key, tiles.get(layer).get(key) + spreadEnergy);
                    }
                    else{
                        tiles.get(layer).put(key, spreadEnergy);
                    }
                }
            }
        }
    }
    
    
    //Removes all tiles from the lists.
    public void clear() {
        for ( Map<String, Double> map : tiles ) {
            map.clear();
        }
        tiles = null;
        this.tiles = new ArrayList(number_of_layers);
        
        for (int layer = 0; layer < number_of_layers; layer++) {
            HashMap<String, Double> map= new HashMap();
            tiles.add(layer, map);
        }
    }
  
    
    //ACCESS FUNCTIONS
    
    //returns the entire list of hashtables (all tiles from all layers)
    public List< HashMap<String, Double> > getTiles() {
        return tiles;
    }
    
    
    //returns the tiles on the specified layer
    public Map<String, Double> getTiles_onLayer(int layer) {
        return tiles.get(layer);
    }
    
    
    //returns the tiles on all the layers between the two provided layers
    //i.e. providing start_layer=3 and end_layer=8 will return tiles from
    //layers 3,4,5,6,7 but NOT from 8.
    public List< HashMap<String, Double> > getTiles_betweenLayers(
            int start_layer, int end_layer) {
                
        return tiles.subList(start_layer,end_layer);
    }
    
    
    private TileParameters params;
    private double cellSize;
    private int spreadFactor;
    private boolean removeNegative;
    private boolean align;
    private int number_of_layers = 50;
    private ArrayList<HashMap<String, Double>> tiles; 
}
