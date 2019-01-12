/*

Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2018. All rights reserved.
Copyright (C) Embergraph contributors 2019. All rights reserved.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
/*
 * Created on Sep 9, 2015
 */

package org.embergraph.service.geospatial;

import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import org.embergraph.btree.ITuple;
import org.embergraph.btree.KeyOutOfRangeException;
import org.embergraph.btree.filter.Advancer;
import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.btree.keys.KeyBuilder;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.IVUtility;
import org.embergraph.rdf.internal.impl.extensions.GeoSpatialLiteralExtension;
import org.embergraph.rdf.internal.impl.literal.LiteralExtensionIV;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.spo.SPO;

/**
 * Advances the cursor to the next zOrderKey that is greater or equal than the
 * first point in the next region. Note that this next key is not necessarily a
 * hit (but, depending on the data) this might be a miss again.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class ZOrderIndexBigMinAdvancer extends Advancer<SPO> {

   private static final long serialVersionUID = -6438977707376228799L;

   private static final transient Logger log = Logger
         .getLogger(ZOrderIndexBigMinAdvancer.class);
   
   final protected static boolean DEBUG = log.isDebugEnabled();
   
   // Search min (upper left) byte array as z-order string (no leading zero)
   private final byte[] searchMinZOrder;

   // Search max (lower right) byte array as z-order string (no leading zero)
   private final byte[] searchMaxZOrder;

   // the position within the index in which we find the zOrderComponent
   private final int zOrderComponentPos;
   
   // the GeoSpatialLiteralExtension object
   private final GeoSpatialLiteralExtension<EmbergraphValue> litExt;

   private final ZOrderRangeScanUtil rangeScanUtil;
   
   // counters object for statistics
   private final GeoSpatialCounters geoSpatialCounters;
   
   private transient IKeyBuilder keyBuilder;

   public ZOrderIndexBigMinAdvancer(
      final byte[] searchMinZOrder, /* the minimum search key (top left) */
      final byte[] searchMaxZOrder, /* the maximum search key (bottom right) */
      final GeoSpatialLiteralExtension<EmbergraphValue> litExt,
      final int zOrderComponentPos /* position of the zOrder in the index */,
      final GeoSpatialCounters geoSpatialCounters) {

      this.litExt = litExt;
      this.searchMinZOrder = litExt.unpadLeadingZero(searchMinZOrder);
      // this.seachMinLong = litExt.fromZOrderByteArray(this.searchMinZOrder);
      
      this.searchMaxZOrder = litExt.unpadLeadingZero(searchMaxZOrder);
      // this.seachMaxLong = litExt.fromZOrderByteArray(this.searchMaxZOrder);
      
      this.zOrderComponentPos = zOrderComponentPos;
      this.geoSpatialCounters = geoSpatialCounters;

      this.rangeScanUtil = 
         new ZOrderRangeScanUtil(
            this.searchMinZOrder, this.searchMaxZOrder, litExt.getNumDimensions());
      
   }
   
   
   @SuppressWarnings("rawtypes")
   @Override
   protected void advance(final ITuple<SPO> tuple) {

      if (keyBuilder == null) {
         keyBuilder = KeyBuilder.newInstance();
      }
      
      // iterate unless tuple in range is found or we reached the end
      ITuple<SPO> curTuple = tuple; 
      while(curTuple!=null) {
         
         if (DEBUG) {
            log.debug("Advancor visiting tuple:    " + curTuple);
         }
         
         final long rangeCheckCalStart = System.nanoTime();
         
         final byte[] key = curTuple.getKey();
   
         keyBuilder.reset();
   
         // decode components up to (and including) the z-order string
         final IV[] ivs = IVUtility.decode(key,zOrderComponentPos+1);
         
         // encode everything up to (and excluding) the z-order component "as is"
         for (int i=0; i<ivs.length-1; i++) {
            IVUtility.encode(keyBuilder, ivs[i]);
         }
   
         // this is the z-order literal
         @SuppressWarnings("unchecked")
         final LiteralExtensionIV<EmbergraphLiteral> zOrderIv =
            (LiteralExtensionIV<EmbergraphLiteral>)ivs[ivs.length-1];
         
         // current record (aka dividing record) as unsigned
         final byte[] dividingRecord = 
            litExt.toZOrderByteArray(zOrderIv.getDelegate());

         final boolean inRange = rangeScanUtil.isInSearchRange(dividingRecord);

         final long rangeCheckCalEnd = System.nanoTime();
         geoSpatialCounters.addRangeCheckCalculationTime(rangeCheckCalEnd-rangeCheckCalStart);
   
         if (!inRange) {

            // this is a miss
            geoSpatialCounters.registerZOrderIndexMiss();
            
            long bigMinCalStart = System.nanoTime();
            
            if (log.isDebugEnabled()) {
               log.debug("-> tuple " + curTuple + " not in range");
            }

            // calculate bigmin over the z-order component
            final byte[] bigMin = rangeScanUtil.calculateBigMin(dividingRecord);
            
            // pad a zero
            final LiteralExtensionIV bigMinIv = litExt.createIVFromZOrderByteArray(bigMin);
            IVUtility.encode(keyBuilder, bigMinIv);
   
            final long bigMinCalEnd = System.nanoTime();
            geoSpatialCounters.addBigMinCalculationTime(bigMinCalEnd-bigMinCalStart);
            
            // advance to the specified key ...
            try {
               if (log.isDebugEnabled()) {
                  log.debug("-> advancing to bigmin: " + bigMinIv);
               }

               ITuple<SPO> next = src.seek(keyBuilder.getKey());
                     
               // ... or the next higher one
               if (next==null) {
                  next = src.next(); 
               }
      
               // continue iterate
               curTuple = next;

            }  catch (NoSuchElementException e) {
     
               throw new KeyOutOfRangeException("Advancer out of search range");
               
            }
            
         } else {
            
            geoSpatialCounters.registerZOrderIndexHit();
            return;
         }
      }

   }
   

   
}
