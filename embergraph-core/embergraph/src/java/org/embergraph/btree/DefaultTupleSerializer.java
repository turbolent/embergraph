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
 * Created on May 28, 2008
 */

package org.embergraph.btree;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Properties;

import org.embergraph.btree.keys.DefaultKeyBuilderFactory;
import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.btree.keys.IKeyBuilderFactory;
import org.embergraph.btree.keys.KeyBuilder;
import org.embergraph.btree.keys.ThreadLocalKeyBuilderFactory;
import org.embergraph.btree.raba.codec.CanonicalHuffmanRabaCoder;
import org.embergraph.btree.raba.codec.IRabaCoder;
import org.embergraph.btree.raba.codec.FrontCodedRabaCoder.DefaultFrontCodedRabaCoder;
import org.embergraph.io.SerializerUtil;

/**
 * Default implementation uses the {@link KeyBuilder} to format the object as a
 * key and uses Java default serialization for the value. You only need to
 * subclass this if you want to use custom (de-)serialization of the value,
 * custom conversion of the application key to an unsigned byte[], or if you
 * have a special type of application key such that you are able to decode the
 * unsigned byte[] and materialize the corresponding application key.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class DefaultTupleSerializer<K extends Object, V extends Object>
        implements ITupleSerializer<K, V>, Externalizable {

    /**
     * 
     */
    private static final long serialVersionUID = 2211020411074955099L;

    /**
     * The default for {@link IKeyBuilderFactory}.
     * 
     * @deprecated by {@link IndexMetadata.Options#KEY_BUILDER_FACTORY}
     */
    static public final IKeyBuilderFactory getDefaultKeyBuilderFactory() {
        
        return new DefaultKeyBuilderFactory(new Properties());
    }
    
    /**
     * The default for {@link #getLeafKeysCoder()} (compression for the keys
     * stored in a leaf).
     * 
     * @deprecated by {@link IndexMetadata.Options#LEAF_KEYS_CODER}
     */
    static public final IRabaCoder getDefaultLeafKeysCoder() {
        
        return DefaultFrontCodedRabaCoder.INSTANCE;
//        return PrefixSerializer.INSTANCE;
        
    }
    
    /**
     * The default for {@link #getLeafValuesCoder()} (compression for
     * the values stored in a leaf).
     * 
     * @deprecated by {@link IndexMetadata.Options#LEAF_VALUES_CODER}
     */
    static public final IRabaCoder getDefaultValuesCoder() {
        
        return CanonicalHuffmanRabaCoder.INSTANCE;
//        return DefaultDataSerializer.INSTANCE;
        
    }
    
    private IRabaCoder leafKeysCoder;
    private IRabaCoder leafValsCoder;

    @Override
    final public IRabaCoder getLeafKeysCoder() {
        
        return leafKeysCoder;
        
    }

    @Override
    final public IRabaCoder getLeafValuesCoder() {

        return leafValsCoder;
        
    }

    /**
     * Override the {@link #getLeafKeysCoder()}. It is NOT safe to change
     * this value once data have been stored in an {@link IIndex} using another
     * value as existing data MAY become unreadable.
     * 
     * @param leafKeysCoder
     *            The new value.
     */
    final public void setLeafKeysCoder(final IRabaCoder leafKeysCoder) {

        if (leafKeysCoder == null)
            throw new IllegalArgumentException();

        this.leafKeysCoder = leafKeysCoder;
        
    }

    /**
     * Override the {@link #getLeafValuesCoder()}. It is NOT safe to change
     * this value once data have been stored in an {@link IIndex} using another
     * value as existing data MAY become unreadable.
     * 
     * @param valuesCoder
     *            The new value.
     */
    final public void setLeafValuesCoder(final IRabaCoder valuesCoder) {
        
        if (valuesCoder == null)
            throw new IllegalArgumentException();

        this.leafValsCoder = valuesCoder;
        
    }

    /**
     * Factory for a new instance using default values for the
     * {@link #getKeyBuilder()}, the {@link #getLeafKeysCoder()}, and the
     * {@link #getLeafValuesCoder()}.
     */
    public static ITupleSerializer newInstance() {

        return new DefaultTupleSerializer(getDefaultKeyBuilderFactory());
        
    }

    /**
     * The factory specified to the ctor.
     */
    private IKeyBuilderFactory delegateKeyBuilderFactory;

    /**
     * The {@link #delegateKeyBuilderFactory} wrapped up in thread-local
     * factory.
     */
    private transient IKeyBuilderFactory threadLocalKeyBuilderFactory;
    
    /**
     * De-serialization ctor <em>only</em>.
     */
    public DefaultTupleSerializer() {
        
    }

    /**
     * 
     * @param keyBuilderFactory
     *            The {@link IKeyBuilderFactory}, which will be automatically
     *            wrapped up by a {@link ThreadLocalKeyBuilderFactory}.
     */
    public DefaultTupleSerializer(final IKeyBuilderFactory keyBuilderFactory) {

        this(keyBuilderFactory, getDefaultLeafKeysCoder(),
                getDefaultValuesCoder());

    }

    public DefaultTupleSerializer(final IKeyBuilderFactory keyBuilderFactory,
            final IRabaCoder leafKeysCoder, final IRabaCoder leafValsCoder) {

        if (keyBuilderFactory == null)
            throw new IllegalArgumentException();

        if (leafKeysCoder == null)
            throw new IllegalArgumentException();

        if (leafValsCoder == null)
            throw new IllegalArgumentException();
        
        threadLocalKeyBuilderFactory = new ThreadLocalKeyBuilderFactory(
                keyBuilderFactory);
        
        this.delegateKeyBuilderFactory = keyBuilderFactory;

        this.leafKeysCoder = leafKeysCoder;
        
        this.leafValsCoder = leafValsCoder;
        
    }

    @Override
    public String toString() {

        final StringBuilder sb = new StringBuilder();

        sb.append(getClass().getName()+"{");
        sb.append(", keyBuilderFactory="+delegateKeyBuilderFactory);
        sb.append(", leafKeysCoder=" + leafKeysCoder);//.getClass().getName());
        sb.append(", leafValuesCoder=" + leafValsCoder);//.getClass().getName());
        sb.append("}");
        
        return sb.toString();
        
    }
    
    /**
     * A thread-local {@link IKeyBuilder} instance.
     * <p>
     * Note: By default, the {@link #getKeyBuilder()} uses whatever default is
     * in place on the host/JVM where the {@link DefaultTupleSerializer}
     * instance was first created. That backing {@link IKeyBuilderFactory}
     * object is serialized along with the {@link DefaultTupleSerializer} so
     * that the specific configuration values are persisted, even when the
     * {@link DefaultTupleSerializer} is de-serialized on a different host.
     */
    @Override
    final public IKeyBuilder getKeyBuilder() {

        if(threadLocalKeyBuilderFactory == null) {
            
            /*
             * This can happen if you use the de-serialization ctor by mistake.
             */
            
            throw new IllegalStateException();
            
        }
        
        /*
         * TODO This should probably to a reset() before returning the object.
         * However, we need to verify that no callers are assuming that it does
         * NOT do a reset and implicitly relying on passing the intermediate key
         * via the return value (which would be very bad style).
         */
        return threadLocalKeyBuilderFactory.getKeyBuilder();

    }

    @Override
    final public IKeyBuilder getPrimaryKeyBuilder() {

        if(threadLocalKeyBuilderFactory == null) {
            
            /*
             * This can happen if you use the de-serialization ctor by mistake.
             */
            
            throw new IllegalStateException();
            
        }
        
        /*
         * TODO This should probably to a reset() before returning the object.
         * However, we need to verify that no callers are assuming that it does
         * NOT do a reset and implicitly relying on passing the intermediate key
         * via the return value (which would be very bad style).
         */
        return threadLocalKeyBuilderFactory.getPrimaryKeyBuilder();

    }
    
    @Override
    public byte[] serializeKey(final Object obj) {

        if (obj == null)
            throw new IllegalArgumentException();

        return getKeyBuilder().reset().append(obj).getKey();
        
    }

    /**
     * Serializes the object as a byte[] using Java default serialization.
     * 
     * @param obj
     *            The object to be serialized (MAY be <code>null</code>).
     * 
     * @return The serialized representation of the object as a byte[] -or-
     *         <code>null</code> if the reference is <code>null</code>.
     */
    @Override
    public byte[] serializeVal(final V obj) {

        return SerializerUtil.serialize(obj);
        
    }

    /**
     * De-serializes an object from the {@link ITuple#getValue() value} stored
     * in the tuple (ignores the key stored in the tuple).
     */
    @Override
    public V deserialize(ITuple tuple) {

        if (tuple == null)
            throw new IllegalArgumentException();

        // @todo tuple.getValueStream()
        return (V)SerializerUtil.deserialize(tuple.getValue());
        
    }

    /**
     * This is an unsupported operation. Additional information is required to
     * either decode the internal unsigned byte[] keys or to extract the key
     * from the de-serialized value (if it is being stored in that value). You
     * can either write your own {@link ITupleSerializer} or you can specialize
     * this one so that it can de-serialize your keys using whichever approach
     * makes the most sense for your data.
     * 
     * @throws UnsupportedOperationException
     *             always.
     */
    @Override
    public K deserializeKey(ITuple tuple) {
        
        throw new UnsupportedOperationException();
        
    }

    /**
     * The initial version.
     * <p>
     * Note: Explicit versioning for the {@link DefaultTupleSerializer} was
     * introduced with inlining of datatype literals for the RDF database.
     */
    private final static transient byte VERSION0 = 0;

    /**
     * The current version.
     */
    private final static transient byte VERSION = VERSION0;

    @Override
    public void readExternal(final ObjectInput in) throws IOException,
            ClassNotFoundException {

        final byte version = in.readByte();
        switch (version) {
        case VERSION0:
            delegateKeyBuilderFactory = (IKeyBuilderFactory) in.readObject();
            threadLocalKeyBuilderFactory = new ThreadLocalKeyBuilderFactory(
                    delegateKeyBuilderFactory);
            leafKeysCoder = (IRabaCoder) in.readObject();
            leafValsCoder = (IRabaCoder) in.readObject();
            break;
        default:
            throw new UnsupportedOperationException("Unknown version: "
                    + version);
        }

    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {

        out.writeByte(VERSION);
        
        out.writeObject(delegateKeyBuilderFactory);
        
        out.writeObject(leafKeysCoder);

        out.writeObject(leafValsCoder);
        
    }

}
