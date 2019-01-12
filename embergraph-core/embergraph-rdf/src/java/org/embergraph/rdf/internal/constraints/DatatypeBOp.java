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
package org.embergraph.rdf.internal.constraints;

import java.util.Map;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;

import org.embergraph.bop.BOp;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IValueExpression;
import org.embergraph.rdf.error.SparqlTypeErrorException;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.XSD;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.sparql.ast.GlobalAnnotations;

/**
 * Return the datatype of the literal argument.
 */
public class DatatypeBOp extends IVValueExpression<IV>
		implements INeedsMaterialization {

	/**
	 *
	 */
	private static final long serialVersionUID = 7391999162162545704L;

	private static final transient Logger log = Logger.getLogger(DatatypeBOp.class);

    public DatatypeBOp(final IValueExpression<? extends IV> x, final GlobalAnnotations globals) {

        this(new BOp[] { x }, anns(globals));

    }

    /**
     * Required shallow copy constructor.
     */
    public DatatypeBOp(final BOp[] args, final Map<String, Object> anns) {

    	super(args, anns);

        if (args.length != 1 || args[0] == null)
            throw new IllegalArgumentException();

		if (getProperty(Annotations.NAMESPACE) == null)
			throw new IllegalArgumentException();

    }

    /**
     * Constructor required for {@link org.embergraph.bop.BOpUtility#deepCopy(FilterNode)}.
     */
    public DatatypeBOp(final DatatypeBOp op) {
        super(op);
    }

    public IV get(final IBindingSet bs) {

	    final EmbergraphValueFactory vf = super.getValueFactory();

        @SuppressWarnings("rawtypes")
        final IV iv = getAndCheckLiteral(0, bs);

        // not yet bound
        if (iv == null)
        	throw new SparqlTypeErrorException();

        if (log.isDebugEnabled()) {
            log.debug(iv);
        }

        /*
         * We don't need to do this anymore.  asValue(IV) does the right thing,
         * it will let us work with the IV directly in the right cases.  The
         * BOps should no longer be doing this kind of logic directly.
         */
//        if (iv.isInline() && !iv.isExtension()) {
//
//        	return asIV(iv.getDTE().getDatatypeURI(), bs);
//        	
//        }

        final Value val = asValue(iv);

        if (val instanceof Literal) {

        	final Literal literal = (Literal) val;

        	final URI datatype;

			if (literal.getDatatype() != null) {

				// literal with datatype
				datatype = literal.getDatatype();

            } else if (literal.getLanguage() != null) {

                // language-tag literal
                datatype = RDF.LANGSTRING;

			} else if (literal.getLanguage() == null) {

				// simple literal
				datatype = XSD.STRING;

			} else {

				throw new SparqlTypeErrorException();

			}

	    	return asIV(datatype, bs);

        }

        throw new SparqlTypeErrorException();

    }

    /**
     * The DatatypeBOp can evaluate against unmaterialized inline numerics.
     */
    public Requirement getRequirement() {

    	return INeedsMaterialization.Requirement.SOMETIMES;

    }


}
