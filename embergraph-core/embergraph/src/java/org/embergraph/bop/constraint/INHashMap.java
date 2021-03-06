/*
 * The Notice below must appear in each file of the Source Code of any copy you distribute of the
 * Licensed Product. Contributors to any Modifications may add their own copyright notices to
 * identify their own contributions.
 *
 * <p>License:
 *
 * <p>The contents of this file are subject to the CognitiveWeb Open Source License Version 1.1 (the
 * License). You may not copy or use this file, in either source code or executable form, except in
 * compliance with the License. You may obtain a copy of the License from
 *
 * <p>http://www.CognitiveWeb.org/legal/license/
 *
 * <p>Software distributed under the License is distributed on an AS IS basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>Copyrights:
 *
 * <p>Portions created by or assigned to CognitiveWeb are Copyright (c) 2003-2003 CognitiveWeb. All
 * Rights Reserved. Contact information for CognitiveWeb is available at
 *
 * <p>http://www.CognitiveWeb.org
 *
 * <p>Portions Copyright (c) 2002-2003 Bryan Thompson.
 *
 * <p>Acknowledgements:
 *
 * <p>Special thanks to the developers of the Jabber Open Source License 1.0 (JOSL), from which this
 * License was derived. This License contains terms that differ from JOSL.
 *
 * <p>Special thanks to the CognitiveWeb Open Source Contributors for their suggestions and support
 * of the Cognitive Web.
 *
 * <p>Modifications:
 */
/*
 * Created on Jun 17, 2008
 */

package org.embergraph.bop.constraint;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.embergraph.bop.BOp;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.NV;

/*
 * A constraint that a variable may only take on the bindings enumerated by some set.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class INHashMap<T> extends INConstraint<T> {

  /** */
  private static final long serialVersionUID = 8032412126003678642L;

  /*
   * The variable (cached).
   *
   * <p>Note: This cache is not serialized and is compiled on demand when the operator is used.
   */
  private transient volatile IVariable<T> var;

  /*
   * The sorted data (cached).
   *
   * <p>Note: This cache is not serialized and is compiled on demand when the operator is used.
   */
  private transient volatile ConcurrentHashMap<T, T> set;

  /** Deep copy constructor. */
  public INHashMap(final INHashMap<T> op) {
    super(op);
  }

  /** Shallow copy constructor. */
  public INHashMap(final BOp[] args, final Map<String, Object> annotations) {
    super(args, annotations);
  }

  /*
   * @param x Some variable.
   * @param set A set of legal term identifiers providing a constraint on the allowable values for
   *     that variable.
   */
  public INHashMap(final IVariable<T> x, final IConstant<T>[] set) {

    super(new BOp[] {}, NV.asMap(new NV(Annotations.VARIABLE, x), new NV(Annotations.SET, set)));
  }

  private void init() {

    var = getVariable();

    // populate the cache.
    final IConstant<T>[] a = getSet();

    set = new ConcurrentHashMap<>(a.length);

    for (IConstant<T> t : a) {

      final T val = t.get();

      set.put(val, val);
    }
  }

  public Boolean get(final IBindingSet bindingSet) {

    if (var == null) {

      synchronized (this) {
        if (var == null) {

          // init() is guarded by double-checked locking pattern.
          init();
        }
      }
    }

    // get binding for "x".
    @SuppressWarnings("unchecked")
    final IConstant<T> x = bindingSet.get(var);

    if (x == null) {

      // not yet bound : FIXME Modify to return false - variables must be bound
      return true;
    }

    final T v = x.get();

    final boolean found = set.containsKey(v);

    return found;
  }
}
