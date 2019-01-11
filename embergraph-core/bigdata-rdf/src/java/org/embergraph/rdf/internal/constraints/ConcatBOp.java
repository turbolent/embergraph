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
package org.embergraph.rdf.internal.constraints;

import java.util.Map;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;

import org.embergraph.bop.BOp;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IValueExpression;
import org.embergraph.rdf.error.SparqlTypeErrorException;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.sparql.ast.GlobalAnnotations;

public class ConcatBOp extends IVValueExpression<IV> implements INeedsMaterialization {

    private static final long serialVersionUID = 5894411703430694650L;

    public ConcatBOp(final GlobalAnnotations globals, IValueExpression<? extends IV>... args) {

        super(args, anns(globals));
    }

    /**
     * Required shallow copy constructor.
     */
    public ConcatBOp(final BOp[] args, final Map<String, Object> anns) {

        super(args, anns);

        if (args.length < 1 || args[0] == null)
            throw new IllegalArgumentException();

    }

    /**
     * Constructor required for {@link org.embergraph.bop.BOpUtility#deepCopy(FilterNode)}.
     */
    public ConcatBOp(final ConcatBOp op) {
        super(op);
    }

	@Override
	public Requirement getRequirement() {
		return Requirement.SOMETIMES;
	}

    @Override
    public IV get(final IBindingSet bs) {
        URI datatype = null;
        String lang = null;
        boolean allSame = true;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arity(); i++) {
            @SuppressWarnings("rawtypes")
            final IV v = getAndCheckLiteral(i, bs);

            if (v.isNumeric()) {
                throw new SparqlTypeErrorException();
            }
            
            String label = null;
            if (allSame) {
                final Literal lit = asLiteral(v);
                label = lit.getLabel();
                if (lit.getDatatype() != null) {
                    if (lang != null) {
                        allSame = false;
                    } else if (datatype == null) {
                        if (i == 0) {
                            datatype = lit.getDatatype();
                        } else {
                            allSame = false;
                        }
                    } else if (!datatype.equals(lit.getDatatype())) {
                        allSame = false;
                    }
                } else if (lit.getLanguage() != null) {
                    if (datatype != null) {
                        allSame = false;
                    } else if (lang == null) {
                        if (i == 0) {
                            lang = lit.getLanguage();
                        } else {
                            allSame = false;
                        }
                    } else if (!lang.equals(lit.getLanguage())) {
                        allSame = false;
                    }
                } else {
                    allSame = false;
                }
            } else {
                label = literalLabel(v);
            }
            sb.append(label);
        }
        if (allSame) {
            if (datatype != null) {
                return super.asIV(getValueFactory().createLiteral(sb.toString(),datatype), bs);
            } else if (lang != null) {
                return super.asIV(getValueFactory().createLiteral(sb.toString(),lang), bs);
            }
        }
        return super.asIV(getValueFactory().createLiteral(sb.toString()), bs);

    }

}
