/*
 * BaseBeamCalorimeterScanner.java
 *
 * Created on May 25 2014, 02:21 AM
 * Updated on May 25 2014, 02:00 AM
 * 
 * @author Alex Bogert and Christopher Milke
 * @version 2.0
 * 
 * The Scanner must scan a list of tiles from a single layer of the Beam Calorimeter. 
 * The scanner provides the user with an immutable list of BeamCalorimeterPallets 
 * (accessed via an Iterator), which provide information about their location, energy,
 * layer, and weight. The segmentation will vary with implementation, via the pallets.
 *
 */
package org.lcsim.contrib.scipp.beamcal.base;

import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterScanner;
import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterPallet;
import org.lcsim.contrib.scipp.beamcal.base.BaseBeamCalorimeterPallet;

import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterTile;
import org.lcsim.contrib.scipp.beamcal.base.BaseBeamCalorimeterTile;
import org.lcsim.contrib.scipp.beamcal.database.DataBaseBeamCalorimeterTile;

import org.lcsim.contrib.scipp.beamcal.TileParameters;
import org.lcsim.contrib.scipp.beamcal.geometry.ArcTileParameters;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import java.util.Map;
import java.util.HashMap;
import java.lang.Math;

import java.util.Iterator;


public class BaseBeamCalorimeterScanner implements BeamCalorimeterScanner
{
    /**Create a Scanner with the defined parameters.
       @param stepsize The unit distance the scanner will travel at each step through the calorimeter.
       @param radius   The number of layers of tiles surrounding the reference tiles
     */
     
    public BaseBeamCalorimeterScanner(TileParameters new_params, int new_stepsize, int new_radius) {
        this.params    = new_params;
        this.stepsize  = new_stepsize;
        this.radius = new_radius;
        this.beamCalEdge = params.getEdge();
        
        System.out.println("BeamCalorimeterScanner -- Setting params to " + this.params.toString() );
        System.out.println("BeamCalorimeterScanner -- Setting step size to " + this.stepsize);
        System.out.println("BeamCalorimeterScanner -- Setting radius to " + this.radius);
    }
    

    //Scans over the provided tiles and creates a list of all generated pallets.
    public void scan(Map<String, BeamCalorimeterTile> tileMap) {
        this.reset();
        this.tileHash = tileMap;
        this.scanHashedTiles();
        return;
    }
    
    
    //Does the same as above, but using Database tiles.
    public void scanDB(Map<String, DataBaseBeamCalorimeterTile> DBtileMap) {
        this.reset();
        this.DBtileHash = DBtileMap;
        this.scanHashedDBTiles();
        return;
    }
    
    
    //Re-performs the scan operation, after clearing out the database list.
    public void rescan( int new_stepsize, int new_radius ) {
        this.clear();
        this.stepsize = new_stepsize;
        this.radius = new_radius;
        this.scanHashedTiles();
        return;
    }
    
    
    //Reset the scanner to a fresh state.
    public void reset() {
        this.clear();
        return;
    }
    
    
    //creates a list of pallets without any associated energy. Mostly used by the DBcylGenerator
    public List<BeamCalorimeterPallet> makeBlankPallets() {
        List<BeamCalorimeterPallet> blankPalletList  = new ArrayList<BeamCalorimeterPallet>();
        int layer = -1;
        
        for (int ring = radius; ring <= params.getLastRing() - radius; ring++) {
            for (int arc = 0; arc < params.getArcsInRing(ring); arc+=stepsize) {
                BaseBeamCalorimeterPallet new_blank_pallet = new BaseBeamCalorimeterPallet(params, ring, arc, layer, radius);
                blankPalletList.add( (BeamCalorimeterPallet)new_blank_pallet );
            }
        }
        
        return blankPalletList;
    }
    
    
    //Removes all tiles from the tileHash and all pallets from the palletList
    private void clear() {
        System.out.println("CLEARING TILE HASH IN CLEAR METHOD");
        if ( this.tileHash != null ) this.tileHash.clear();
        if ( this.DBtileHash != null ) this.DBtileHash.clear();
        if ( this.palletList != null ) this.palletList.clear();
        return;
    }
    
    
    //Returns the minimum energy pallet in the event.
    public BeamCalorimeterPallet getMinimumPallet() {
        if (this.palletList.size() == 0) {
            throw new java.lang.RuntimeException("Cannot get Minimum Pallet no pallets in scanner.");
        }
        
        Collections.sort(this.palletList);
        return this.palletList.get(0);
    }
    
    
    //Returns the maximum energy pallet in the event.
    public BeamCalorimeterPallet getMaxPallet() {
        if (this.palletList.size() == 0) {
            throw new java.lang.RuntimeException("Cannot get Max Pallet no pallets in scanner.");
        }
        
        Collections.sort(this.palletList);
        Collections.reverse(this.palletList);
        return this.palletList.get(0);
    }
    
    
    //Returns a list of found pallets in decending order of energy.
    public List<BeamCalorimeterPallet> getPallets() {
        Collections.sort(this.palletList);
        Collections.reverse(this.palletList);
        return this.palletList;
    }
    
    
    public int getPalletRadius() {
        return radius; 
    }
    
    
    //Same as above, but only returns the top "n" highest energy pallets.
    public List<BeamCalorimeterPallet> getTopPallets(int n) {
        List<BeamCalorimeterPallet> TopPalletList  = new ArrayList<BeamCalorimeterPallet>();
        
        Collections.sort(this.palletList);
        Collections.reverse(this.palletList);
        
        int i = 1;
        for (BeamCalorimeterPallet pal : palletList) {
            TopPalletList.add(pal);
            if ( i++ >= n ) break;
        }
        
        return TopPalletList;
    }
    
    
    //Same as above, but only returns the top "n" highest energy pallets
    //that do not overlap one another.
    public List<BeamCalorimeterPallet> getTopPalletsNoOverlap(int n) {
        List<BeamCalorimeterPallet> TopPalletList  = new ArrayList<BeamCalorimeterPallet>();
        
        Collections.sort(this.palletList);
        Collections.reverse(this.palletList);
        
        int i = 1;
        for (BeamCalorimeterPallet pal : palletList) {
            boolean does_overlap = false;
            
            for (BeamCalorimeterPallet toppal : TopPalletList) {
                if ( toppal.overlaps(pal) ) {
                    does_overlap = true;
                    break;
                }
            }
            
            if (!does_overlap) {
                TopPalletList.add(pal);
                if ( i++ >= n ) break;
            }
        }
            
            
        return TopPalletList;
    }
    
    
    //Generates an initial pallet at the center of the detector, and moves it in a 
    //counter-clockwise spiral outwards to the edge of the detector, generating a
    //pallet at every stepsize.  
    private void scanHashedTiles() {
        int layer = -1;
        
        for (int ring = radius; ring <= params.getLastRing() - radius; ring++) {
            for (int arc = 0; arc < params.getArcsInRing(ring); arc+=stepsize) {
                BaseBeamCalorimeterPallet new_pallet = new BaseBeamCalorimeterPallet(params, ring, arc, layer, radius);
                new_pallet.addEnergy(this.tileHash);
                
                if (new_pallet.getWeight() != 0) {
                    this.palletList.add( (BeamCalorimeterPallet)new_pallet );
                } 
            }
        }
  
        System.out.println("Found : "+this.palletList.size()+" pallets [radius="+this.radius+"] with energy in the event.");
        return;
    }
    
    
    //same as above using DB tiles
    private void scanHashedDBTiles() {
        int layer = -1;
        
        for (int ring = radius; ring <= params.getLastRing() - radius; ring++) {
            for (int arc = 0; arc < params.getArcsInRing(ring); arc+=stepsize) {
                BaseBeamCalorimeterPallet new_pallet = new BaseBeamCalorimeterPallet(params, ring, arc, layer, radius);
                new_pallet.addDBEnergy(this.DBtileHash);
                
                if (new_pallet.getWeight() != 0) {
                    this.palletList.add( (BeamCalorimeterPallet)new_pallet );
                } 
            }
        }
        
        System.out.println("Found : "+this.palletList.size()+" pallets [radius="+this.radius+"] with energy in the event.");
        return;
    }
    
    
    //Finds and generates the next pallet in the spiral scanning performed in the
    //above three functions.
    private BaseBeamCalorimeterPallet nextPallet(BaseBeamCalorimeterPallet parent, int layer) {
        BaseBeamCalorimeterPallet pallet = null;
        
        int current_ring = parent.getRef()[0];
        int current_arc  = parent.getRef()[1];
        
        int last_arc = params.getArcsInRing(current_ring) - 1;
        if ( current_arc < last_arc ) {//If arc hasnt gone all the way around then increment phi
            pallet = new BaseBeamCalorimeterPallet(params, current_ring, current_arc+stepsize, layer, radius);
        }else {//else increment the ring and set arc to 0
            current_arc = 0;
            pallet = new BaseBeamCalorimeterPallet(params, current_ring+1, current_arc, layer, radius);
        }
        
        return pallet;
    }
    
    
    
    private TileParameters params;
    private int stepsize;
    private int radius;
    
    private List<BeamCalorimeterPallet> palletList  = new ArrayList<BeamCalorimeterPallet>();
    private Map<String, BeamCalorimeterTile> tileHash = null;
    private Map<String, DataBaseBeamCalorimeterTile> DBtileHash = null;
    
    //Beam Calorimeter edge radius
    private float beamCalEdge = -1;
}
