/*
 * TileDataBaseReader.java
 *
 * Created on Mar  19, 2011, 11:01 PM
 * Updated on June 12, 2014, 02:17  AM
 * @author Alex Bogert
 *
 * The purpose of this class is to access a SQLite database,
 * which stores the average energy for beamstrahlung on a 
 * per tile basis in the Beam Calorimeter
 */

package org.lcsim.contrib.scipp.beamcal.database;

import org.lcsim.contrib.scipp.beamcal.sqlite.SQLITEWrapper;

import org.lcsim.contrib.scipp.beamcal.BeamCalorimeterTile;

public class TileDataBaseReader {
    public TileDataBaseReader() {
    }

    public TileDataBaseReader(String connectionName) {
        this.open(connectionName);
    }

    public void open(String connectionName) {
        try {
            this.database = new SQLITEWrapper();
            this.database.connection(connectionName);
        }
        catch (java.sql.SQLException e) {
            System.out.println(e);
            System.exit(1);
        }
        catch (java.lang.ClassNotFoundException e) {
            System.out.println(e);
            System.exit(1);
        }
        return;
    }
    
    
    public void close() throws java.sql.SQLException {
        this.database.close();
    }
    
    
    //select the row from the database related to the BeamCal Hit
    public Object[] getRow(BeamCalorimeterTile tile) throws java.sql.SQLException {
        return this.getRow( tile.toString(), tile.getLayer() );
    }
    
    
    //select the weight from the database related to the BeamCal Hit
    public int getWeight(BeamCalorimeterTile tile) throws java.sql.SQLException {
        return this.getWeight( tile.toString(), tile.getLayer() );
    }
    
    
    //select the average from the database related to the BeamCal Hit
    public double getAvgStrahlungEnergy(BeamCalorimeterTile tile) throws java.sql.SQLException {
        return this.getAvgStrahlungEnergy( tile.toString(), tile.getLayer() );
    }
    
    
    public double getAvgStrahlungEnergy(String tileID, int layer) throws java.sql.SQLException {
        return this.database.getDouble( tileID, this.getLayerName(layer) );
    }
    
    
    public int getWeight(String tileID, int layer) throws java.sql.SQLException {
        return this.database.getInt( tileID, this.getLayerName(layer) );
    }
    
    
    public Object[] getRow(String tileID, String s) throws java.sql.SQLException {
        return this.database.getRow(tileID, s);
    }
    
    
    public Object[] getRow(String tileID, int layer) throws java.sql.SQLException {
        return this.database.getRow( tileID, this.getLayerName(layer) );
    }


    private String getLayerName(int layer) {
        Integer layernum  = layer;
        String  layername = "layer";
        return layername.concat(layernum.toString());
    }


    private SQLITEWrapper database;
}
