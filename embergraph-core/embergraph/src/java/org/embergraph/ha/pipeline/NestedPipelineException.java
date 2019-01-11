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
package org.embergraph.ha.pipeline;

/**
 * An exception that is used to wrap and rethrow a cause whose root cause is
 * another {@link AbstractPipelineException}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class NestedPipelineException extends AbstractPipelineException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public NestedPipelineException() {
        super();
    }

    public NestedPipelineException(String message, Throwable cause) {
        super(message, cause);
    }

    public NestedPipelineException(String message) {
        super(message);
    }

    public NestedPipelineException(Throwable cause) {
        super(cause);
    }

}
