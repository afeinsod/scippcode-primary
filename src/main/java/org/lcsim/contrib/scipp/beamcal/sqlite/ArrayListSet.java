/*
 * ArrayListSet.java
 *
 * Created on Apr 21, 2011, 9:03 PM
 *
 * Provides access to SQLITE Data base in a usable manner.
 * @author Alex Bogert
 * @version 1
 */
package org.lcsim.contrib.scipp.beamcal.sqlite;

import java.util.ArrayList;
import java.util.Set;
import java.util.Collection;

//The underlying data structure and implementation is handled by ArrayList
public class ArrayListSet extends ArrayList implements Set {

    public ArrayListSet() {
        super();
    } 

    public ArrayListSet(Collection c) {
        super(c);
    } 

    public ArrayListSet(int initialCapacity) {
        super(initialCapacity);
    } 

    public boolean add(Comparable o) {
        boolean add    = false;
        int index      = 0;
        int travelDist = this.size()/2; 
        //add 1 to odd travel distances
        travelDist += (travelDist % 2) == 0 ? 0 : 1;

        while (travelDist > 0) {
            int direction = o.compareTo(this.get(index));
            //Search in the positive direction
            if (direction > 0) {
                index += travelDist; 
                if (travelDist == 1) {
                    direction = o.compareTo(this.get(index));
                    if (direction > 0) {
                        super.add(index + 1, o);
                        add = true;
                    }
                    else if (direction < 0) {
                        super.add(index, o);
                        add = true;
                    }
                    else {
                        break;
                    }
                }
                //add 1 to odd travel distances
                travelDist += (travelDist % 2) == 0 ? 0 : 1;
                travelDist /= 2;
            }
            //Search in the negative direction
            else if (direction < 0) {
                index -= travelDist; 
                if (travelDist == 1) {
                    direction = o.compareTo(this.get(index));
                    if (direction > 0) {
                        super.add(index + 1, o);
                        add = true;
                    }
                    else if (direction < 0) {
                        super.add(index, o);
                        add = true;
                    }
                    else {
                        break;
                    }
                }
                travelDist /= 2;
            }
            //The objects are equal
            else {
                break; 
            }
        }
        return add;
    }
}
