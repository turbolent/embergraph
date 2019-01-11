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
 * Created on Sep 2, 2010
 */

package org.embergraph.bop.constraint;

import junit.framework.TestCase2;

import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IConstraint;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.Var;
import org.embergraph.bop.bindingSet.ListBindingSet;

/**
 * Unit tests for {@link OR}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestOR extends TestCase2 {

    /**
     * 
     */
    public TestOR() {
    }

    /**
     * @param name
     */
    public TestOR(String name) {
        super(name);
    }

    /**
     * Unit test for {@link OR#OR(IConstraint,IConstraint)}
     */
    public void testConstructor ()
    {
        BooleanValueExpression eq = new EQ ( Var.var ( "x" ), Var.var ( "y" ) ) ;
        BooleanValueExpression ne = new EQ ( Var.var ( "x" ), Var.var ( "y" ) ) ;

        try { assertTrue ( null != new OR ( null, eq ) ) ; fail ( "IllegalArgumentException expected, lhs was null" ) ; }
        catch ( IllegalArgumentException e ) {}

        try { assertTrue ( null != new OR ( eq, null ) ) ; fail ( "IllegalArgumentException expected, rhs was null" ) ; }
        catch ( IllegalArgumentException e ) {}

        assertTrue ( null != new OR ( eq, ne ) ) ;
    }

    /**
     * Unit test for {@link OR#get(IBindingSet)}
     */
    public void testAccept ()
    {
        Var<?> x = Var.var ( "x" ) ;
        Var<?> y = Var.var ( "y" ) ;
        Constant<Integer> val1 = new Constant<Integer> ( 1 ) ;
        Constant<Integer> val2 = new Constant<Integer> ( 2 ) ;

        BooleanValueExpression eq = new EQ ( x, y ) ;
        BooleanValueExpression eqc = new EQConstant ( y, val2 ) ;

        OR op = new OR ( eq, eqc ) ;

        IBindingSet eqlhs = new ListBindingSet ( new IVariable<?> [] { x, y }, new IConstant [] { val1, val1 } ) ;
        IBindingSet eqrhs = new ListBindingSet ( new IVariable<?> [] { x, y }, new IConstant [] { val1, val2 } ) ;
        IBindingSet ne = new ListBindingSet ( new IVariable<?> [] { x, y }, new IConstant [] { val2, val1 } ) ;

        assertTrue ( op.get ( eqlhs ) ) ;
        assertTrue ( op.get ( eqrhs ) ) ;
        assertFalse ( op.get ( ne ) ) ;
    }    
}
