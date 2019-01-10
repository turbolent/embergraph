package com.bigdata.counters.render;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.util.Date;
import java.util.regex.Pattern;

import com.bigdata.counters.History;
import com.bigdata.counters.ICounter;
import com.bigdata.counters.query.URLQueryModel;

/**
 * Datum specific formatting of {@link ICounter} values (not thread-safe since
 * the {@link Format} objects are not thread-safe).
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
abstract public class ValueFormatter {

    /**
     * Used to format double and float counter values.
     */
    final protected DecimalFormat decimalFormat;

    /**
     * Used to format counter values that can be inferred to be a percentage.
     */
    final protected NumberFormat percentFormat;

    /**
     * Used to format integer and long counter values.
     */
    final protected NumberFormat integerFormat;

    /**
     * Used to format the units of time when expressed as elapsed units since
     * the first sample of a {@link History}.
     */
    final protected DecimalFormat unitsFormat;

    /**
     * Used to format date/time values.
     */
    final protected Format dateFormat;

    public ValueFormatter(final URLQueryModel model) {

        this.decimalFormat = model.decimalFormat;

        this.percentFormat = model.percentFormat;

        this.integerFormat = model.integerFormat;

        this.unitsFormat = model.unitsFormat;

        this.dateFormat = model.dateFormat;

    }

    /**
     * A pattern matching the occurrence of the word "percent" in a counter
     * name. Leading and trailing wildcards are used and the match is
     * case-insensitive.
     */
    static protected final Pattern percent_pattern = Pattern.compile(
            ".*percent.*", Pattern.CASE_INSENSITIVE);

    /**
     * Formats a counter value as a String.
     * 
     * @param counter
     *            The counter.
     * @param value
     *            The counter value (MAY be <code>null</code>).
     * 
     * @return The formatted value.
     */
    abstract public String value(final ICounter counter, final Object val);

    /**
     * Write the full counter path.
     */
    abstract public void writeFullPath(Writer w, String path)
            throws IOException;

    /**
     * Write a partial counter path.
     * 
     * @param rootDepth
     *            The path components will be shown beginning at this depth -
     *            ZERO (0) is the root.
     */
    abstract public void writePath(Writer w, String path, int rootDepth)
            throws IOException;

    /**
     * Format a timestamp as a date.
     * 
     * @param timestamp
     *            The timestamp value.
     * 
     * @return The formatted value.
     */
    public String date(final long timestamp) {

        if (dateFormat != null) {

            return dateFormat.format(new Date(timestamp));

        } else {

            return Long.toString(timestamp);

        }

    }

}
