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
 * Created on Aug 26, 2008
 */

package org.embergraph.rdf.vocab;

import java.util.Iterator;
import org.embergraph.bop.IConstant;
import org.embergraph.rdf.internal.IV;
import org.openrdf.model.Value;

/*
 * Interface for a pre-defined vocabulary.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface Vocabulary {

  /*
   * The namespace of the owning {@link LexiconRelation}.
   *
   * @return
   */
  String getNamespace();

  /*
   * The #of defined {@link Value}s.
   *
   * @throws IllegalStateException if the values have not been defined.
   */
  int size();

  /*
   * The {@link Value}s in an arbitrary order.
   *
   * @throws IllegalStateException if the values have not been defined.
   */
  Iterator<? extends Value> values();

  /*
   * The term identifier for the pre-defined {@link Value}.
   *
   * @param value The value.
   * @return The {@link IV} for that {@link Value} -or- <code>null</code> if the {@link Value} was
   *     not defined by this {@link Vocabulary}.
   */
  /*
   * Note: Prior to the TERMS_REFACTOR_BRANCH this would throw an exception
   * if the Value was not declared by the Vocabulary.
   */
  IV get(Value value);

  /*
   * Returns the {@link IConstant} for the {@link Value}.
   *
   * @param value The value.
   * @return The {@link IConstant}.
   * @throws IllegalArgumentException if that {@link Value} is not defined for this vocabulary.
   */
  IConstant<IV> getConstant(Value value);

  /*
   * Reverse lookup of an {@link IV} defined by this vocabulary.
   *
   * @param iv The {@link IV}.
   * @return The {@link EmbergraphValue} -or- <code>null</code> if the {@link IV} was not defined by
   *     the vocabulary.
   * @since TERMS_REFACTOR_BRANCH
   */
  Value asValue(IV iv);
}
