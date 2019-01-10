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
 * Created on Sep 2, 2010
 */

package org.embergraph.bop.constraint;

import java.util.Arrays;

import junit.framework.TestCase2;

import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.Var;
import org.embergraph.bop.bindingSet.ListBindingSet;

/**
 * Unit tests for {@link INHashMap}.
 * 
 * @author <a href="mailto:dmacgbr@users.sourceforge.net">David MacMillan</a>
 * @version $Id:$
 */
public abstract class TestINConstraint extends TestCase2
{
    /**
     * 
     */
    public TestINConstraint ()
    {
    }

    /**
     * @param name
     */
    public TestINConstraint ( String name )
    {
        super ( name ) ;
    }

    /**
     * Unit test for {@link INHashMap#INHashMap(IVariable<T>,IConstant<T>[])}
     */
    public void testConstructor ()
    {
        IVariable<Integer> var = Var.var ( "x" ) ;
        IConstant<Integer> vals [] = new IConstant [] { new Constant<Integer> ( 1 ) } ;

        try { assertTrue ( null != newINConstraint ( null, vals ) ) ; fail ( "IllegalArgumentException expected, lhs was null" ) ; }
        catch ( IllegalArgumentException e ) {}

        try { assertTrue ( null != newINConstraint ( var, null ) ) ; fail ( "IllegalArgumentException expected, rhs was null" ) ; }
        catch ( IllegalArgumentException e ) {}

        try { assertTrue ( null != newINConstraint ( var, new IConstant [] {} ) ) ; fail ( "IllegalArgumentException expected, set was empty" ) ; }
        catch ( IllegalArgumentException e ) {}

        assertTrue ( null != newINConstraint ( var, vals ) ) ;
    }

    /**
     * Unit test for {@link INConstraint#getVariable()}
     */
    public void testGetVariable ()
    {
        Var<?> x = Var.var ( "x" ) ;
        IConstant vals [] = new Constant [] { new Constant<Integer> ( 1 ) } ;

        INConstraint op = newINConstraint ( x, vals ) ;

        assertTrue ( x.equals ( op.getVariable () ) ) ;
    }

    /**
     * Unit test for {@link INConstraint#getSet()}
     */
    public void testGetSet ()
    {
        Var<?> x = Var.var ( "x" ) ;
        IConstant vals [] = new Constant [] { new Constant<Integer> ( 1 ) } ;

        INConstraint op = newINConstraint ( x, vals ) ;

        assertTrue ( Arrays.equals ( vals, op.getSet () ) ) ;
    }

    /**
     * Unit test for {@link INConstraint#accept(IBindingSet)}
     */
    public void testAccept ()
    {
        Var<?> x = Var.var ( "x" ) ;
        Constant<Integer> val1 = new Constant<Integer> ( 1 ) ;
        Constant<Integer> val2 = new Constant<Integer> ( 2 ) ;
        Constant<Integer> val3 = new Constant<Integer> ( 3 ) ;

        INConstraint op = newINConstraint ( x, new IConstant [] { val1, val2 } ) ;

        IBindingSet in = new ListBindingSet ( new IVariable<?> [] { x }, new IConstant [] { val1 } ) ;
        IBindingSet notin = new ListBindingSet ( new IVariable<?> [] { x }, new IConstant [] { val3 } ) ;
        IBindingSet nb = new ListBindingSet ( new IVariable<?> [] {}, new IConstant [] {} ) ;

        assertTrue ( op.get ( in ) ) ;
        assertFalse ( op.get ( notin ) ) ;
        // FIXME Modify to assertFalse() - unbound variables should fail constraints
        assertTrue ( op.get ( nb ) ) ;
    }

    protected abstract INConstraint newINConstraint ( IVariable<?> var, IConstant<?> vals [] ) ;
}
