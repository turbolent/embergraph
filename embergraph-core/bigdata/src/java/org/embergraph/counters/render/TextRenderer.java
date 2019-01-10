/*

Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2016.  All rights reserved.

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
/*
 * Created on May 27, 2009
 */

package org.embergraph.counters.render;

import java.io.IOException;
import java.io.Writer;

import com.bigdata.counters.ICounter;
import com.bigdata.counters.query.HistoryTable;
import com.bigdata.counters.query.ICounterSelector;
import com.bigdata.counters.query.PivotTable;
import com.bigdata.counters.query.URLQueryModel;

/**
 * {@link IRenderer} for <code>text/plain</code>.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TextRenderer implements IRenderer {

    /**
     * Describes the state of the controller.
     */
    private final URLQueryModel model;

    /**
     * Selects the counters to be rendered.
     */
    private final ICounterSelector counterSelector;

    /**
     * @param model
     *            Describes the state of the controller (e.g., as parsed from
     *            the URL query parameters).
     * @param counterSelector
     *            Selects the counters to be rendered.
     */
    public TextRenderer(final URLQueryModel model,
            final ICounterSelector counterSelector) {

        if (model == null)
            throw new IllegalArgumentException();

        if (counterSelector == null)
            throw new IllegalArgumentException();

        this.model = model;

        this.counterSelector = counterSelector;

    }

    @Override
    public void render(final Writer w) throws IOException {

        final IRenderer renderer;

        switch (model.reportType) {
        
        case correlated: {
        
            @SuppressWarnings("rawtypes")
            final ICounter[] counters = counterSelector.selectCounters(
                    model.depth, model.pattern, model.fromTime, model.toTime,
                    model.period, true/* historyRequired */);
            
            final HistoryTable historyTable = new HistoryTable(counters,
                    model.period);
            
            renderer = new TabDelimitedHistoryTableRenderer(historyTable,
                    model.pattern, new TextValueFormatter(model));
            
            break;
            
        }
        
        case pivot: {
            
            @SuppressWarnings("rawtypes")
            final ICounter[] counters = counterSelector.selectCounters(
                    model.depth, model.pattern, model.fromTime, model.toTime,
                    model.period, true/* historyRequired */);

            final HistoryTable historyTable = new HistoryTable(counters,
                    model.period);
            
            final PivotTable pivotTable = new PivotTable(model.pattern,
                    model.category, historyTable);
            
            renderer = new TabDelimitedPivotTableRenderer(pivotTable,
                    new TextValueFormatter(model));
            
            break;
            
        }
        
        case events:
        case hierarchy:
        default:
            /*
             * An unrecognized value or unsupported report type.
             */
            throw new UnsupportedOperationException(model.reportType.toString());
        
        }

        renderer.render(w);

    }

}
