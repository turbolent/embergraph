/**

Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2016.  All rights reserved.

Contact:
     SYSTAP, LLC DBA Blazegraph
     2501 Calvert ST NW #106
     Washington, DC 20008
     licenses@blazegraph.com

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
 * Created on May 21, 2007
 */
package com.bigdata.rdf.lexicon;

import com.bigdata.btree.IIndex;
import com.bigdata.btree.proc.AbstractKeyArrayIndexProcedure;
import com.bigdata.btree.proc.AbstractKeyArrayIndexProcedureConstructor;
import com.bigdata.btree.proc.IParallelizableIndexProcedure;
import com.bigdata.btree.proc.IResultHandler;
import com.bigdata.btree.raba.IRaba;
import com.bigdata.btree.raba.codec.IRabaCoder;
import com.bigdata.rdf.internal.impl.TermId;
import com.bigdata.rdf.model.BigdataValueSerializer;
import com.bigdata.relation.IMutableRelationIndexWriteProcedure;
import com.bigdata.service.ndx.NopAggregator;

/**
 * Unisolated write operation makes consistent assertions on the
 * <em>id:term</em> index based on the data developed by the {@link Term2IdWriteProc}
 * operation.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class Id2TermWriteProc extends AbstractKeyArrayIndexProcedure<Void> implements
        IParallelizableIndexProcedure<Void>, IMutableRelationIndexWriteProcedure<Void> {

    /**
     * 
     */
    private static final long serialVersionUID = -5480378815444534653L;

    /**
     * Enables validation that a pre-assigned term identifier is being
     * consistently mapped onto the same term. Errors are reported if, for
     * example, the index has a record that a term identifier is mapped onto one
     * URL but the procedure was invoked with a different URI paired to that
     * term identifiers. When such errors are reported, they generally indicate
     * a problem with the TERM2ID index where it is failing to maintain a
     * consistent mapping.
     * <p>
     * Validation may be disabled for releases, however it is not really that
     * much overhead since the operation is on the in-memory representation.
     * 
     * @deprecated Validation can not be reasonably applied it the Unicode
     *             collation is less than Identical. It also has problems for
     *             datatype literals if different lexical forms are all mapped
     *             onto the same key,e.g.,
     * 
     *             <pre>
     * 12&circ;&circ;&lt;xsd:float&gt;
     * 12.0&circ;&circ;&lt;xsd:float&gt;
     * 12.00&circ;&circ;&lt;xsd:float&gt;
     * </pre>
     * 
     *             will all be mapped to the same key and hence would give the
     *             appearance of a conflict if we were to reject any of these
     *             forms when another of the forms was already present under the
     *             key.
     *             
     * Note: Now it's not only deprecated, but the code that relies on it has
     * been commented out.  This is because it makes assumptions about how
     * {@link TermId} objects are encoded and decoded.  Under the legacy model
     * they were simple longs.  After the lexicon refactor we use the byte
     * flags also.  So if we ever decide to do validation again here, we need
     * to figure out how to give this class access to an {@link IIVEncoder}. 
     */
    static private transient final boolean validate = false;
    
    @Override
    public final boolean isReadOnly() {
        
        return false;
        
    }
    
    /**
     * De-serialization constructor.
     */
    public Id2TermWriteProc() {
        
    }
    
    protected Id2TermWriteProc(final IRabaCoder keysCoder, final IRabaCoder valsCoder,
            int fromIndex, int toIndex, byte[][] keys, byte[][] vals) {

        super(keysCoder, valsCoder, fromIndex, toIndex, keys, vals);
        
        assert vals != null;
        
    }
    
    public static class Id2TermWriteProcConstructor extends
            AbstractKeyArrayIndexProcedureConstructor<Id2TermWriteProc> {

        public static Id2TermWriteProcConstructor INSTANCE = new Id2TermWriteProcConstructor();

        /**
         * Values are required.
         */
        @Override
        public final boolean sendValues() {
            
            return true;
            
        }

        private Id2TermWriteProcConstructor() {}
        
        @Override
        public Id2TermWriteProc newInstance(final IRabaCoder keysCoder,
                final IRabaCoder valsCoder, final int fromIndex,
                final int toIndex, final byte[][] keys, final byte[][] vals) {

            return new Id2TermWriteProc(keysCoder, valsCoder, fromIndex, toIndex,
                    keys, vals);

        }

    }

    /**
     * Conditionally inserts each key-value pair into the index. The keys are
     * the term identifiers. The values are the terms as serialized by
     * {@link BigdataValueSerializer}. Since a conditional insert is used, the
     * operation does not cause terms that are already known to the ids index to
     * be re-inserted, thereby reducing writes of dirty index nodes.
     * 
     * @param ndx
     *            The index.
     * 
     * @return <code>null</code>.
     */
    @Override
    public Void applyOnce(final IIndex ndx, final IRaba keys, final IRaba vals) {
        
    	final int n = keys.size();
        
        for (int i = 0; i < n; i++) {

            // Note: the key is the term identifier.
            // @todo copy key/val into reused buffers to reduce allocation.
            final byte[] key = keys.get(i);
            
//            // Note: the value is the serialized term (and never a BNode).
//            final byte[] val;
//
//            if (validate) {
//
//                // The term identifier.
//                final long id = KeyBuilder.decodeLong(key, 0);
//
//                assert id != TermId.NULL;
//                
//                // Note: BNodes are not allowed in the reverse index.
//                assert ! VTE.isBNode(id);
//                
//                // Note: SIDS are not allowed in the reverse index.
//                assert ! VTE.isStatement(id);
//                
//                /*
//                 * When the term identifier is found in the reverse mapping
//                 * this code path validates that the serialized term is the
//                 * same.
//                 */
//                final byte[] oldval = ndx.lookup(key);
//                
//                val = getValue(i);
//                
//                if( oldval == null ) {
//                    
//                    if (ndx.insert(key, val) != null) {
//
//                        throw new AssertionError();
//
//                    }
//                    
//                } else {
//
//                    /*
//                     * Note: This would fail if the serialization of the term
//                     * was changed for an existing database instance. In order
//                     * to validate when different serialization formats might be
//                     * in use you have to actually deserialize the terms.
//                     * However, I have the validation logic here just as a
//                     * sanity check while getting the basic system running - it
//                     * is not meant to be deployed.
//                     */
//
//                    if (! BytesUtil.bytesEqual(val, oldval)) {
//
//                        final char suffix;
//                        if (VTE.isLiteral(id))
//                            suffix = 'L';
//                        else if (VTE.isURI(id))
//                            suffix = 'U';
//                        else if (VTE.isBNode(id))
//                            suffix = 'B';
//                        else if (VTE.isStatement(id))
//                            suffix = 'S';
//                        else
//                            suffix = '?';
//
//                        /*
//                         * We have to go one step further and compare the
//                         * deserialized value in order to decide if there is
//                         * really an inconsistency in the index. For example,
//                         * "abc@en" and "abc@EN" encode as different byte[]s,
//                         * but they are EQUALS() for RDF since the language code
//                         * comparison is case insensitive. The same problem can
//                         * occur for data type literals, since lexically
//                         * distinct literals are are mapped onto the same point
//                         * in the data type space (the same key). However,
//                         * comparison based on data type equality is not really
//                         * provided for by BigdataLiteral, so we get into
//                         * trouble if we attempt to detect errors based on
//                         * datatype literals.
//                         */
//                        final BigdataValueSerializer valSer = new BigdataValueSerializer(
//                                new ValueFactoryImpl());
//
//                        final Value term = valSer.deserialize(val);
//                        final Value oldterm = valSer.deserialize(oldval);
//                        
//                        if (!term.equals(oldterm)) {
//                            
//                            log.error("term=" + term);
//                            log.error("oldterm=" + oldterm);
//                            log.error("id=" + id + suffix);
//                            log.error("key=" + BytesUtil.toString(key));
//                            log.error("val=" + Arrays.toString(val));
//                            log.error("oldval=" + Arrays.toString(oldval));
//                            if (ndx.getIndexMetadata().getPartitionMetadata() != null)
//                                log.error(ndx.getIndexMetadata()
//                                        .getPartitionMetadata().toString());
//
//                            throw new RuntimeException(
//                                    "Consistency problem: id=" + id);
//                        }
//
//                    }
//                    
//                }
//                
//            } else {
                
                /*
                 * This code path does not validate that the term identifier
                 * is mapped to the same term. This is the code path that
                 * you SHOULD use.
                 */

            // See BLZG-1539
            ndx.putIfAbsent(key, vals.get(i));
            
//                if (!ndx.contains(key)) {
//
//                    val = vals.get(i);
//                    
//                    if (ndx.insert(key, val) != null) {
//
//                        throw new AssertionError();
//
//                    }
//
//                }

//            }
            
        }
        
        return null;
        
    }

    /**
	 * Nothing is returned, so nothing to aggregate, but uses a
	 * {@link NopAggregator} to preserve striping against a local index.
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected IResultHandler<Void, Void> newAggregator() {

		// NOP aggegrator preserves striping against the index.
		return NopAggregator.INSTANCE;

	}

}
