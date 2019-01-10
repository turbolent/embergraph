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
 * Created on Apr 16, 2008
 */

package com.bigdata.rdf.model;

import org.openrdf.model.BNode;

import com.bigdata.rdf.internal.IV;
import com.bigdata.rdf.internal.impl.bnode.SidIV;
import com.bigdata.rdf.rio.StatementBuffer;
import com.bigdata.rdf.rio.UnificationException;
import com.bigdata.rdf.spo.SPO;
import com.bigdata.rdf.store.AbstractTripleStore;

/**
 * A blank node. Use {@link BigdataValueFactory} to create instances of this
 * class.
 * <p>
 * Note: When {@link AbstractTripleStore.Options#STATEMENT_IDENTIFIERS} is
 * enabled blank nodes in the context position of a statement are recognized as
 * statement identifiers by {@link StatementBuffer}. It coordinates with this
 * class in order to detect when a blank node is a statement identifier and to
 * defer the assertion of statements made using a statement identifier until
 * that statement identifier becomes defined by being paired with a statement.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class BigdataBNodeImpl extends BigdataResourceImpl implements
        BigdataBNode {

    /**
     * 
     */
    private static final long serialVersionUID = 2675602437833048872L;
    
    private final String id;
    
    /**
     * Boolean flag is set during conversion from an RDF interchange syntax
     * into the internal {@link SPO} model if the blank node is a statement
     * identifier.
     */
    private boolean statementIdentifier;

//    public BigdataBNodeImpl(String id) {
//
//        this(null, id);
//
//    }

    /**
     * Used by {@link BigdataValueFactoryImpl}.
     */
    BigdataBNodeImpl(final BigdataValueFactory valueFactory, final String id) {

        this(valueFactory, id, null);

    }
    
    BigdataBNodeImpl(final BigdataValueFactory valueFactory, final String id, 
    		final BigdataStatement stmt) {

        super(valueFactory, null);

        if (id == null)
            throw new IllegalArgumentException();

        this.id = id;
        
        this.sid = stmt;
        if (stmt != null) {
        	this.statementIdentifier = true;
        }

    }
    
    /**
     * Used to detect ungrounded sids (self-referential).
     */
    private transient boolean selfRef = false;
    
    @Override
    public IV getIV() {
    	
    	if (super.iv == null && sid != null) {

//    		if (sid.getSubject() == this || sid.getObject() == this)
//				throw new UnificationException("illegal self-referential sid");
		
    		if (selfRef) {
    			throw new UnificationException("illegal self-referential sid");
    		}
    
    		// temporarily set it to true while we get the IVs on the sid
    		selfRef = true;
    		
    		final IV s = sid.s();
    		final IV p = sid.p();
    		final IV o = sid.o();
    		
    		// if we make it to here then we have a fully grounded sid
    		selfRef = false;
    		
    		if (s != null && p != null && o != null) {
    			setIV(new SidIV(new SPO(s, p, o)));
    		}
    	}
    	
    	return super.iv;
    }
    
    @Override
    public String toString() {

    	if (sid != null) {
    		return "<" + sid.toString() + ">";
    	}
        return "_:" + id;
        
    }
    
    @Override
    public String stringValue() {

        return id;

    }

    @Override
    final public boolean equals(final Object o) {

        if (!(o instanceof BNode))
            return false;
        
        return equals((BNode) o);

    }

    final public boolean equals(final BNode o) {

        if (this == o)
            return true;

        if (o == null)
            return false;
        
		if ((o instanceof BigdataValue) //
				&& isRealIV() && ((BigdataValue)o).isRealIV()
				&& ((BigdataValue) o).getValueFactory() == getValueFactory()) {

			return getIV().equals(((BigdataValue) o).getIV());

        } else if ((o instanceof BigdataBNode) //
				&& isStatementIdentifier() && ((BigdataBNode)o).isStatementIdentifier()
				) {

			return getStatement().equals(((BigdataBNode) o).getStatement());

        }

		return id.equals(o.getID());

    }

    @Override
    final public int hashCode() {

        return id.hashCode();

    }

    @Override
    final public String getID() {

        return id;
        
    }

    @Override
    final public void setStatementIdentifier(final boolean isStmtIdentifier) {

        this.statementIdentifier = isStmtIdentifier;
        
    }

    @Override
    final public boolean isStatementIdentifier() {
        
        return this.statementIdentifier;
        
	}

	/*
	 * Mechanism permitting the attachment of a statement to a blank node when
	 * we know the correlation between the blank node and the statement.
	 * 6/1/2012.
	 */
    
	/**
	 * Marks this as a blank node which models the specified statement.
	 * 
	 * @param sid
	 *            The statement.
	 */
    @Override
	final public void setStatement(final BigdataStatement sid) {
		this.statementIdentifier = true;
		this.sid = sid;
	}
	
	/**
	 * Return the statement modeled by this blank node.
	 */
    @Override
	final public BigdataStatement getStatement() {
		return sid;
	}

	private transient BigdataStatement sid;

}
