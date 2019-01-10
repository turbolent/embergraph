/**
Copyright (C) SYSTAP, LLC DBA Blazegraph 2011.  All rights reserved.

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
package org.embergraph.rdf.sail;

import java.util.Properties;

import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.axioms.RdfsAxioms;
import org.embergraph.rdf.sail.BigdataSail;
import org.embergraph.rdf.vocab.NoVocabulary;
import org.embergraph.rdf.vocab.RDFSVocabulary;

/**
 * Concrete instance of {@link TestRollbacks} which overrides the properties to
 * enable truth maintenance support in the SAIL.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestRollbacksTM extends TestRollbacks {

	public TestRollbacksTM() {
		super();
	}
	
	public TestRollbacksTM(String name) {
		super(name);
	}
	
	@Override
    public Properties getProperties() {
        
    	final Properties props = super.getProperties();

        props.setProperty(BigdataSail.Options.AXIOMS_CLASS,
                RdfsAxioms.class.getName());
        props.setProperty(BigdataSail.Options.VOCABULARY_CLASS,
                RDFSVocabulary.class.getName());
        props.setProperty(BigdataSail.Options.TRUTH_MAINTENANCE, "true");
        props.setProperty(BigdataSail.Options.JUSTIFY, "true");
        
        return props;

	}
	
}
