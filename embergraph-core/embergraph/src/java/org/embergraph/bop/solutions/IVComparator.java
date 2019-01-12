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
/* Portions Copyright Aduna (http://www.aduna-software.com/) (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
/*
 * Created on Aug 8, 2011
 */

package org.embergraph.bop.solutions;

import info.aduna.lang.ObjectUtil;
import java.io.Serializable;
import java.util.Comparator;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.impl.bnode.SidIV;
import org.embergraph.rdf.internal.impl.literal.LiteralExtensionIV;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.datatypes.XMLDatatypeUtil;
import org.openrdf.query.algebra.Compare.CompareOp;
import org.openrdf.query.algebra.evaluation.ValueExprEvaluationException;
import org.openrdf.query.algebra.evaluation.util.QueryEvaluationUtil;
import org.openrdf.query.algebra.evaluation.util.ValueComparator;

/*
* A comparator that compares {@link IV}s according the SPARQL value ordering as specified in <a
 * href="http://www.w3.org/TR/rdf-sparql-query/#modOrderBy">SPARQL Query Language for RDF</a>. This
 * implementation is based on the openrdf {@link ValueComparator} but has been modified to work with
 * {@link IV}s.
 *
 * <p>Note: {@link SidIV} are blank nodes. However, we order SIDs after normal blank nodes since
 * they are modeling somewhat different information.
 *
 * @author james
 * @author Arjohn Kampman
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @author <a href="mailto:mrpersonick@users.sourceforge.net">Mike Personick</a>
 * @version $Id$
 */
public class IVComparator implements Comparator<IV>, Serializable {

  /** */
  private static final long serialVersionUID = 1L;

  @SuppressWarnings({"rawtypes", "unchecked"})
  public int compare(final IV o1, final IV o2) {

    // check equality
    if (ObjectUtil.nullEquals(o1, o2)) {
      return 0;
    }

    // 1. (Lowest) no value assigned to the variable
    if (o1 == null) {
      return -1;
    }
    if (o2 == null) {
      return 1;
    }

    // 2. Blank nodes
    final boolean b1 = o1.isBNode();
    final boolean b2 = o2.isBNode();
    if (b1 && b2) {
      return compareBNodes(o1, o2);
    }
    if (b1) {
      return -1;
    }
    if (b2) {
      return 1;
    }

    // 2.5 Statement identifiers (SIDs)
    final boolean s1 = o1.isStatement();
    final boolean s2 = o2.isStatement();
    if (s1 && s2) {
      return compareSIDs(o1, o2);
    }
    if (s1) {
      return -1;
    }
    if (s2) {
      return 1;
    }

    // 3. IRIs
    final boolean u1 = o1.isURI();
    final boolean u2 = o2.isURI();
    if (u1 && u2) {
      return compareURIs((URI) o1, (URI) o2);
    }
    if (u1) {
      return -1;
    }
    if (u2) {
      return 1;
    }

    // 4. RDF literals
    return compareLiterals((IV<EmbergraphLiteral, ?>) o1, (IV<EmbergraphLiteral, ?>) o2);
  }

  /*
   * Use the natural ordering of the {@link IV}s when they are both blank nodes. This causes the
   * solutions for the same blank node to be "grouped".
   *
   * @see <a href="http://www.openrdf.org/issues/browse/SES-873">Order the same Blank Nodes together
   *     in ORDER BY</a>
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private int compareBNodes(final IV leftBNode, final IV rightBNode) {

    return leftBNode.compareTo(rightBNode);
  }

  /*
   * Use the natural ordering of the {@link SidIV}s when they are both SIDs nodes. This causes the
   * solutions for the same {@link SidIV} to be "grouped".
   *
   * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/505">Exception when using SPARQL
   *     sort & statement identifiers </a>
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private int compareSIDs(final IV leftSid, final IV rightSid) {

    return leftSid.compareTo(rightSid);
  }

  /*
   * Only difference here with Sesame ValueComparator is that we use stringValue() instead of
   * toString().
   */
  private int compareURIs(final URI leftURI, final URI rightURI) {

    return leftURI.stringValue().compareTo(rightURI.stringValue());
  }

  private int compareLiterals(
      final IV<EmbergraphLiteral, ?> left, final IV<EmbergraphLiteral, ?> right) {

    /*
     * Only thing we need to special case are LiteralExtensionIVs, which
     * are used to model xsd:dateTime.
     */
    if (left instanceof LiteralExtensionIV && right instanceof LiteralExtensionIV) {

      @SuppressWarnings("rawtypes")
      final IV leftDatatype = ((LiteralExtensionIV) left).getExtensionIV();

      @SuppressWarnings("rawtypes")
      final IV rightDatatype = ((LiteralExtensionIV) right).getExtensionIV();

      if (leftDatatype.equals(rightDatatype)) {

        return left.compareTo(right);
      }
    }

    return compareLiterals((Literal) left, (Literal) right);
  }

  /*
   * Taken directly from Sesame's ValueComparator, no modification. Handles inlines nicely since
   * they now implement the Literal interface.
   */
  private int compareLiterals(final Literal leftLit, final Literal rightLit) {
    // Additional constraint for ORDER BY: "A plain literal is lower
    // than an RDF literal with type xsd:string of the same lexical
    // form."

    if (!QueryEvaluationUtil.isStringLiteral(leftLit)
        || !QueryEvaluationUtil.isStringLiteral(rightLit)) {
      try {
        boolean isSmaller = QueryEvaluationUtil.compareLiterals(leftLit, rightLit, CompareOp.LT);

        if (isSmaller) {
          return -1;
        } else {
          return 1;
        }
      } catch (ValueExprEvaluationException e) {
        // literals cannot be compared using the '<' operator, continue
        // below
      }
    }

    int result = 0;

    // Sort by datatype first, plain literals come before datatyped literals
    URI leftDatatype = leftLit.getDatatype();
    URI rightDatatype = rightLit.getDatatype();

    if (leftDatatype != null) {
      if (rightDatatype != null) {
        // Both literals have datatypes
        result = compareDatatypes(leftDatatype, rightDatatype);
      } else {
        result = 1;
      }
    } else if (rightDatatype != null) {
      result = -1;
    }

    if (result == 0) {
      // datatypes are equal or both literals are untyped; sort by language
      // tags, simple literals come before literals with language tags
      String leftLanguage = leftLit.getLanguage();
      String rightLanguage = rightLit.getLanguage();

      if (leftLanguage != null) {
        if (rightLanguage != null) {
          result = leftLanguage.compareTo(rightLanguage);
        } else {
          result = 1;
        }
      } else if (rightLanguage != null) {
        result = -1;
      }
    }

    if (result == 0) {
      // Literals are equal as fas as their datatypes and language tags are
      // concerned, compare their labels
      result = leftLit.getLabel().compareTo(rightLit.getLabel());
    }

    return result;
  }

  /** Taken directly from Sesame's ValueComparator, no modification. */
  private int compareDatatypes(final URI leftDatatype, final URI rightDatatype) {
    if (XMLDatatypeUtil.isNumericDatatype(leftDatatype)) {
      if (XMLDatatypeUtil.isNumericDatatype(rightDatatype)) {
        // both are numeric datatypes
        return compareURIs(leftDatatype, rightDatatype);
      } else {
        return -1;
      }
    } else if (XMLDatatypeUtil.isNumericDatatype(rightDatatype)) {
      return 1;
    } else if (XMLDatatypeUtil.isCalendarDatatype(leftDatatype)) {
      if (XMLDatatypeUtil.isCalendarDatatype(rightDatatype)) {
        // both are calendar datatypes
        return compareURIs(leftDatatype, rightDatatype);
      } else {
        return -1;
      }
    } else if (XMLDatatypeUtil.isCalendarDatatype(rightDatatype)) {
      return 1;
    } else {
      // incompatible or unordered datatypes
      return compareURIs(leftDatatype, rightDatatype);
    }
  }
}
