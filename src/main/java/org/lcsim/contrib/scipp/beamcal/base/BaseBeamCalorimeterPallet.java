/*
 * BaseBeamCalorimeterPallet.java
 *
 * Created on May 4 2014, 3:59PM
 *
 * @author Alex Bogert and Christopher Milke
 * @version 2.0
 * 
 * The BeamCalorimeterPallet interface is designed to define how the segmentation
 * should be implemented on the Beam Calorimeter. This particular pallet 
 * implemetation is based on only a reference tile (ref) and a Pallet radius (prad).
 * Ref is the center of the pallet. Prad defines how many adjacent tiles are also
 * part of the pallet. With prad=0, ref is the only tile in the pallet. With
 * prad=1, then ref is in the pallet, as well as every tile directly touching ref.
 * With prad=2, ref and its adjacent tiles are in the pallet, as well as every tile
 * directly touching any of the tiles adjacent to ref (exlcuding duplicates of course).
 * This pattern can go up to any arbitrary prad.
 * 
 * NOTE: This is the most geometrically sensitive class in the scipp package.
 * All other classes in the package (currently) are oblivious to the geometry
 * of the detector. However, pallets must know where tiles are in relation to one
 * another in order to determine if they are adjacent. The private function
 * of this class "findSurroundingTiles" is exclusively responsible for finding
 * adjacent tiles. As such, if TileParameters is modified or a new 
 * TileParameters implementation is made, special attention should be paid to 
 * this class, and to "findSurroundingTiles" specifically, to ensure it still works.
 * If an unkown bug suddenly occurs in the program after the geometry has been adjusted,
 * this is likely the first place to check.
 * 
 */
package org.lcsim.contrib.scipp.beamcal.base;

import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterPallet;
import org.lcsim.contrib.scipp.beamcal.TileParameters;
import org.lcsim.contrib.scipp.beamcal.geometry.PolarCoords;

import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterTile;
import org.lcsim.contrib.scipp.beamcal.base.BaseBeamCalorimeterTile;
import org.lcsim.contrib.scipp.beamcal.database.DataBaseBeamCalorimeterTile;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import java.util.Set;
import java.util.HashSet;

import java.util.Map;
import java.util.HashMap;

import java.util.Iterator;

import java.lang.Double;


public class BaseBeamCalorimeterPallet implements BeamCalorimeterPallet, Comparable {

    public BaseBeamCalorimeterPallet( TileParameters new_params, int ring, int arc, int new_layer, int new_radius) {
        this.params = new_params;
        this.ref[0] = (short)ring;
        this.ref[1] = (short)arc;
        this.layer  = new_layer;
        this.radius = new_radius;        
        findSurroundingTileIDS(ref[0],ref[1],radius);
    }
    
    
    //Takes a hashmap (Map<TileID,TileEnergy>) containing all tiles and
    //their respective energies, determins which tiles among the hashmap 
    //this pallet is composed of, and add the energy of all of those tiles
    //to this pallet. 
    public int addEnergy( Map<String, BeamCalorimeterTile> tileMap ) {
        int number_tiles_energized = 0;
        
        for ( String id : tileSet ) {
           if ( tileMap.containsKey(id) ) {
               double E = tileMap.get(id).getEnergy();
               this.energy += E;
               this.weight++;
               
               number_tiles_energized++;
           }
       }
       
       return number_tiles_energized;
    }
    
    
    //same as above, using DB tiles
    public int addDBEnergy( Map<String, DataBaseBeamCalorimeterTile> DBtileMap ) {
        int number_tiles_energized = 0;
        
        for ( String id : tileSet ) {
           if ( DBtileMap.containsKey(id) ) {
               double E = DBtileMap.get(id).getSubtractedEnergy();
               this.energy += E;
               this.weight++;
               
               number_tiles_energized++;
           }
       }
       
       return number_tiles_energized;
    }
    
    
    //Returns the layer this pallet was built on.
    public int getLayer() {
        return this.layer;
    }
    
    
    //Returns the ID of the Reference Tile of this Pallet.
    public short[] getRef() {
        return this.ref;
    }
    
    
    //Returns the radius of Pallet, where the radius is the "prad"
    //mentioned in the file description at the top.
    public int getPalletRadius() {
        return this.radius;
    }
    
    
    //Returns the energy stored in this pallet.
    public double  getEnergy() {
        return this.energy;
    }
    
    
    //Returns the weight of this Pallet. 
    public int  getWeight() {
        return this.weight;
    }
    
    
    //Returns a hashset containing all the tiles this pallet is made up of.
    public HashSet<String> getConstituentTiles() {
        return tileSet;
    }
    
    
    //Returns the ID of the pallet in string form.
    public String toString() {
        return params.IDtoString(ref[0],ref[1]);
    }
    
    
    //Takes another Pallet "pal" and returns TRUE if this pallet has any 
    //of the same constituent tiles as pal.
    public boolean overlaps(BeamCalorimeterPallet pal) {
        HashSet<String> testlist = pal.getConstituentTiles();
        
        for ( String testID : testlist ) {
            if ( this.tileSet.contains(testID) ) return true;
        }
        
        return false;
    }
    
    
    //Compares this pallet to another, based on energy.
    public int compareTo(Object o) {
        if (!(o instanceof BaseBeamCalorimeterPallet)) {
            throw new java.lang.ClassCastException("Invalid comparison between objects");
        }
        int val = 0;
        if (this.energy - ((BeamCalorimeterPallet) o).getEnergy() > 0) {
            val = 1;
        }
        else if (this.energy - ((BeamCalorimeterPallet) o).getEnergy() == 0) {
            val = 0;
        }
        else  {
            val = -1;
        }
        return val;
    }
    
    
    /* A recursive function used to determine the constituent tiles of the
     * pallet. When given one tile ID (the reference tile's ID) it will identify
     * every tile touching the given tile. It will then pass the list of the
     * identified tiles one at a time to itself, identifying the tiles surrounding
     * each tile in the given list. It will continue this process down as many levels
     * as the given radius "r" (which is the pallet radius). Though the function will
     * NOT add duplicate tiles to the constituent tile list (tileSet), it WILL still
     * search duplicate tiles. This is because it uses a depth first search algorithm,
     * as opposed to the BREADTH first search I (Milke) should have used (oops).
     * I (Milke) have not fixed this due to a lack of time and because there may be
     * little to gain efficiency-wise in doing so (due to pallets only being generated
     * on one layer and the currently small prad).
     * 
     * NOTE: There is special mention of this function at the top of the file.
     */
    private int findSurroundingTileIDS(short ring, short arc, int r) {
        int numberAdded = 0;
        
        //if an ID has not been searched already, add it to the tileSet
        boolean searched = tileSet.contains( params.IDtoString(ring,arc) );
        if (!searched) { 
            tileSet.add( params.IDtoString(ring,arc) );
            numberAdded++;
        }
        
        if (r > 0) {
            //create a list of all surrounding tiles (CW=clockwise,CC=counterclockwise)
            List<short[]> IDlist = new ArrayList<short[]>();
            IDlist.add( new short[]{ring,(short)(arc-1)} ); //arc CW on same ring
            IDlist.add( new short[]{ring,(short)(arc+1)} ); //arc CC on same ring
            
            double corner1 = params.getCornerPolar(ring,arc)[1];   //get CW edge phi
            double corner2 = params.getCornerPolar(ring,arc+1)[1]; //get CC edge phi
            
            short arcA1 = (short)params.getArc(ring+1, corner1); //arc above CW edge
            short arcA2 = (short)params.getArc(ring+1, corner2); //arc above CC edge
            short arcB1 = (short)params.getArc(ring-1, corner1); //arc below CW edge
            short arcB2 = (short)params.getArc(ring-1, corner2); //arc below CC edge
            
            short arc_inc;
            
            //add any arcs between arcA1 and arcA2 (on their ring)
            int N_up = params.getArcsInRing(ring+1);
            int last_arc_up = N_up - 1;
            arc_inc = arcA1;
            IDlist.add( new short[]{(short)(ring+1),arc_inc} );
            while ( arc_inc != arcA2 ) {
                if (++arc_inc > last_arc_up) arc_inc = 0;
                IDlist.add( new short[]{(short)(ring+1),arc_inc} );
            }
                
            //add any arcs between arcB1 and arcB2 (on their ring)
            int N_down = params.getArcsInRing(ring-1);
            int last_arc_down = N_down - 1;
            arc_inc = arcB1;
            IDlist.add( new short[]{(short)(ring-1),arc_inc} );
            while ( arc_inc != arcB2 ) {
                if (++arc_inc > last_arc_down) arc_inc = 0;
                IDlist.add( new short[]{(short)(ring-1),arc_inc} );
            }
            
            
            //recursively find all tiles surrounding the tiles in the IDlist
            for (short[] ID : IDlist)
                numberAdded += findSurroundingTileIDS(ID[0],ID[1],r-1);
        }
        
        return numberAdded;
    }
    
    
    
    private HashSet<String> tileSet = new HashSet<String>();
    private TileParameters params;
    private short[] ref = new short[2];
    private int radius;
    private int layer;
    
    private double energy  = 0.0;
    private int weight  = 0;
    
    
    


    
}


