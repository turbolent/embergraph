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
 * Created on Mar 9, 2012
 */

package org.embergraph.rdf.sparql.ast.service;

/** @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a> */
public abstract class ServiceOptionsBase implements IServiceOptions {

  /**
   * The location of the propertyFile for setting service properties. This to allow overriding the
   * default value via passing a Java Property at the command line.
   */
  private boolean isRunFirst = false;

  private boolean useLBS = false;

  @Override
  public boolean isRunFirst() {
    return isRunFirst;
  }

  public void setRunFirst(final boolean newValue) {
    this.isRunFirst = newValue;
  }

  @Override
  public boolean isEmbergraphLBS() {
    return useLBS;
  }

  public void setEmbergraphLBS(final boolean newValue) {
    this.useLBS = newValue;
  }
}
