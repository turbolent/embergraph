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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import org.openrdf.model.Literal;

import org.embergraph.bop.BOp;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IValueExpression;
import org.embergraph.rdf.error.SparqlTypeErrorException;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.model.BigdataLiteral;
import org.embergraph.rdf.sparql.ast.GlobalAnnotations;

public class EncodeForURIBOp extends IVValueExpression<IV> implements INeedsMaterialization {

    private static final long serialVersionUID = -8448763718374010166L;

    public EncodeForURIBOp(final IValueExpression<? extends IV> x, 
    		final GlobalAnnotations globals) {
        super(x, globals);
    }

    public EncodeForURIBOp(BOp[] args, Map<String, Object> anns) {
        super(args, anns);
        if (args.length != 1 || args[0] == null)
            throw new IllegalArgumentException();
    }

    public EncodeForURIBOp(EncodeForURIBOp op) {
        super(op);
    }

	@Override
	public Requirement getRequirement() {
		return Requirement.SOMETIMES;
	}

	@Override
    public IV get(final IBindingSet bs) throws SparqlTypeErrorException {
        final Literal lit = getAndCheckLiteralValue(0, bs);
        try {
            final BigdataLiteral str = getValueFactory().createLiteral(URLEncoder.encode(lit.getLabel(), "UTF-8").replace("+", "%20"));
            return super.asIV(str, bs);
        } catch (UnsupportedEncodingException uee) {
            throw new SparqlTypeErrorException();
        }
    }
}
