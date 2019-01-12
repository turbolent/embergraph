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
 * Created on March 11, 2008
 */

package org.embergraph.rdf.internal.constraints;

import org.embergraph.bop.Constant;
import org.embergraph.bop.bindingSet.ListBindingSet;
import org.embergraph.journal.ITx;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.sparql.ast.DummyConstantNode;
import org.embergraph.rdf.sparql.ast.GlobalAnnotations;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.ProxyTestCase;

/**
 * Test suite for {@link SubstrBOp}.
 * 
 * @author <a href="mailto:mpersonick@users.sourceforge.net">Mike Personick</a>
 * @version $Id$
 */
public class TestSubstrBOp extends ProxyTestCase {

//	private static final Logger log = Logger.getLogger(TestSubstrBOp.class);
	
    /**
     * 
     */
    public TestSubstrBOp() {
        super();
    }

    /**
     * @param name
     */
    public TestSubstrBOp(String name) {
        super(name);
    }
    
//    @Override
//    public Properties getProperties() {
//    	final Properties props = super.getProperties();
//    	props.setProperty(EmbergraphSail.Options.INLINE_DATE_TIMES, "true");
//    	return props;
//    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void test_substr() {
        
        final AbstractTripleStore db = getStore();

        try {

            final EmbergraphValueFactory vf = db.getValueFactory();
            
            final EmbergraphLiteral plain_text = vf.createLiteral("plain text");
            
            db.addTerms( new EmbergraphValue[] { plain_text} );
            
            final IV _0 = DummyConstantNode.toDummyIV(vf.createLiteral(0));
            final IV _1 = DummyConstantNode.toDummyIV(vf.createLiteral(1));
            final IV _3 = DummyConstantNode.toDummyIV(vf.createLiteral(3));
            final IV _9999 = DummyConstantNode.toDummyIV(vf.createLiteral(9999));
            
            final ListBindingSet emptyBindingSet = new ListBindingSet();

            // substr("plain text",1,3)
            {
                final IV expected = DummyConstantNode.toDummyIV(vf
                        .createLiteral("pla"));

                // Cache the value on the IV.
                plain_text.getIV().setValue(plain_text);

                final IV actual = new SubstrBOp(
                        new Constant<IV>(plain_text.getIV()),
                        new Constant<IV>(_1),
                        new Constant<IV>(_3),
                        new GlobalAnnotations(vf.getNamespace(), ITx.READ_COMMITTED)
                ).get(emptyBindingSet);

                assertEquals(expected, actual);
            }

            // substr("plain text",1,9999)
            {
                final IV expected = DummyConstantNode.toDummyIV(vf
                        .createLiteral("plain text"));

                // Cache the value on the IV.
                plain_text.getIV().setValue(plain_text);

                final IV actual = new SubstrBOp(
                        new Constant<IV>(plain_text.getIV()),
                        new Constant<IV>(_1),
                        new Constant<IV>(_9999),
                        new GlobalAnnotations(vf.getNamespace(), ITx.READ_COMMITTED)
                ).get(emptyBindingSet);

                assertEquals(expected, actual);
            }

            // substr("plain text",0,3)
            {
                final IV expected = DummyConstantNode.toDummyIV(vf
                        .createLiteral("pl"));

                // Cache the value on the IV.
                plain_text.getIV().setValue(plain_text);

                final IV actual = new SubstrBOp(
                        new Constant<IV>(plain_text.getIV()),
                        new Constant<IV>(_0),
                        new Constant<IV>(_3),
                        new GlobalAnnotations(vf.getNamespace(), ITx.READ_COMMITTED)
                ).get(emptyBindingSet);

                assertEquals(expected, actual);
            }

        } finally {
            
            db.__tearDownUnitTest();
            
        }
        
    }
}
