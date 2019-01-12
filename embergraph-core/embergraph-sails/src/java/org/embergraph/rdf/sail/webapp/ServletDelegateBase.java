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
package org.embergraph.rdf.sail.webapp;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Base class provides NOP implementations of the {@link IServletDelegate} interface.
 *
 * @author bryan
 */
public class ServletDelegateBase implements IServletDelegate {

  public ServletDelegateBase() {}

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {}

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {}

  @Override
  public void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {}

  @Override
  public void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {}
}
