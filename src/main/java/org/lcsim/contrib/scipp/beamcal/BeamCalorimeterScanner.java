/*
 * BeamCalorimeterScanner.java
 *
 * Created on July 5 2011, 06:32 PM
 * Updated on May 25 2014, 02:00 AM
 * @author Alex Bogert
 * @version 2.0
 *
 * The Scanner must scan a list of tiles from a single layer of the Beam Calorimeter. 
 * The scanner provides the user with an immutable list of BeamCalorimeterPallets 
 * (accessed via an Iterator), which provide information about their location, energy,
 * layer, and weight. The segmentation will vary with implementation, via the pallets.  
 */

package org.lcsim.contrib.scipp.beamcal;

import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterPallet;

import org.lcsim.contrib.scipp.beamcal.database.DataBaseBeamCalorimeterTile;


import java.util.List;
import java.util.Map;


public interface BeamCalorimeterScanner
{
    //Scans over the provided tiles, using only the tiles on the given layer,
    //and creates a list of all generated pallets.
    public void scan( Map<String, BeamCalorimeterTile> tileMap );
    
    //Does the same as above, but using Database tiles.
    public void scanDB( Map<String, DataBaseBeamCalorimeterTile> DBtileMap );

    //Re-performs the scan operation, after clearing out the database list.
    public void rescan( int new_stepsize, int new_radius );
    
    public List<BeamCalorimeterPallet> makeBlankPallets();

    //Returns a list of found pallets in decending order of energy.
    public List<BeamCalorimeterPallet> getPallets();
    
    //Returns the maximum energy pallet in the event.
    public BeamCalorimeterPallet getMaxPallet();
    
    //Returns the minimum energy pallet in the event.
    public BeamCalorimeterPallet getMinimumPallet();
    
    //Reset the scanner to a fresh state.
    public void reset();
}
