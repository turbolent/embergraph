/**

The Notice below must appear in each file of the Source Code of any
copy you distribute of the Licensed Product.  Contributors to any
Modifications may add their own copyright notices to identify their
own contributions.

License:

The contents of this file are subject to the CognitiveWeb Open Source
License Version 1.1 (the License).  You may not copy or use this file,
in either source code or executable form, except in compliance with
the License.  You may obtain a copy of the License from

  http://www.CognitiveWeb.org/legal/license/

Software distributed under the License is distributed on an AS IS
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.  See
the License for the specific language governing rights and limitations
under the License.

Copyrights:

Portions created by or assigned to CognitiveWeb are Copyright
(c) 2003-2003 CognitiveWeb.  All Rights Reserved.  Contact
information for CognitiveWeb is available at

  http://www.CognitiveWeb.org

Portions Copyright (c) 2002-2003 Bryan Thompson.

Acknowledgements:

Special thanks to the developers of the Jabber Open Source License 1.0
(JOSL), from which this License was derived.  This License contains
terms that differ from JOSL.

Special thanks to the CognitiveWeb Open Source Contributors for their
suggestions and support of the Cognitive Web.

Modifications:

*/
/*
 * Created on Apr 10, 2008
 */

package com.bigdata.striterator;


/**
 * Abstract class for delegation patterns for chunked iterators.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class DelegateChunkedIterator<E> implements IChunkedOrderedIterator<E> {

    final private IChunkedOrderedIterator<E> src;
    
    /**
     * @param src
     *            All methods will delegate to this iterator.
     */
    public DelegateChunkedIterator(final IChunkedOrderedIterator<E> src) {

        if (src == null)
            throw new IllegalArgumentException();

        this.src = src;
        
    }

    @Override
    public void close() {

        src.close();
        
    }

    @Override
    public E next() {
        
        return src.next();
        
    }

    @Override
    public E[] nextChunk() {
        
        return src.nextChunk();
    }

    @Override
    public void remove() {

        src.remove();
        
    }

    @Override
    public boolean hasNext() {

        return src.hasNext();
        
    }

    @Override
    public IKeyOrder<E> getKeyOrder() {

        return src.getKeyOrder();
        
    }

    @Override
    public E[] nextChunk(IKeyOrder<E> keyOrder) {
        
        return src.nextChunk(keyOrder);
    }

}
