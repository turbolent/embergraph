/*
Copyright (C) SYSTAP, LLC 2006-2012.  All rights reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package cutthecrap.utils.striterators;

import java.util.Iterator;

/*
* ******************************
 *
 * @author Martyn Cutcher
 */
public class Visitorator implements Iterator {

  private final Iterator m_iter;
  protected final Object m_context;
  private final Visitor m_visitor;

  public Visitorator(Iterator iter, Object context, Visitor visitor) {
    m_iter = iter;
    m_context = context;
    m_visitor = visitor;
  }

  public boolean hasNext() {
    return m_iter.hasNext();
  }

  public Object next() {
    Object obj = m_iter.next();

    m_visitor.visit(obj);

    return obj;
  }

  public void remove() {
    m_iter.remove();
  }
}
