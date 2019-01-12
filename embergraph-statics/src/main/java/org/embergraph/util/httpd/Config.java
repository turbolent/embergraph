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
package org.embergraph.util.httpd;

/**
 * A utility class to store configuration settings. Introduced to consolidate the HTTP port per
 * BLZG-1166.
 *
 * @author beebs
 */
public final class Config {

  public static final int HTTP_PORT = 9999;

  public static final String PATH = "";

  public static final String DEFAULT_HOST = "localhost";

  public static final String DEFAULT_ENDPOINT =
      "http://" + DEFAULT_HOST + ":" + HTTP_PORT + "/" + PATH;
}
