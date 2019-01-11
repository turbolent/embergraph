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
 * Created on Oct 25, 2007
 */

package org.embergraph.rdf.rules;

import org.openrdf.model.vocabulary.RDFS;

import org.embergraph.rdf.vocab.Vocabulary;

public class RuleFastClosure5 extends AbstractRuleFastClosure_5_6_7_9 {

    /**
     * 
     */
    private static final long serialVersionUID = 4659225021853627629L;

    public RuleFastClosure5(String database,
            String focusStore, Vocabulary vocab) {// , Set<Long> D) {

        super("fastClosure5", database, focusStore,//
                vocab.getConstant(RDFS.SUBPROPERTYOF), //
                vocab.getConstant(RDFS.DOMAIN));
                                                                                                                            // D);
        
    }
    
}
