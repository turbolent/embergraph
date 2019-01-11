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
 * Created on Jun 20, 2008
 */

package org.embergraph.relation.rule;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Mutable program may be used to create a variety of rule executions.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class Program implements IProgram {

    private static final long serialVersionUID = 2774954504183880320L;

    protected static final transient Logger log = Logger.getLogger(Program.class);

    private final String name;
    
    private final boolean parallel;
    
    private final boolean closure;

    private final IQueryOptions queryOptions;
    
    private final List<IStep> steps = new LinkedList<IStep>();
    
//    /**
//     * De-serialization ctor.
//     */
//    public Program() {
//        
//    }
    
    /**
     * An empty program.
     * 
     * @param name
     *            A label for the program.
     * @param parallel
     *            <code>true</code> iff the steps in the program are
     *            parallelizable (this does not imply that they will be executed
     *            in parallel, only that they do not have dependencies among the
     *            steps and hence are in principle parallelizable).
     */
    public Program(String name, boolean parallel) {

        this(name, parallel, false/* closure */, QueryOptions.NONE);

    }

    /**
     * An empty program.
     * 
     * @param name
     *            A label for the program.
     * @param parallel
     *            <code>true</code> iff the steps in the program are
     *            parallelizable (this does not imply that they will be executed
     *            in parallel, only that they do not have dependencies among the
     *            steps and hence are in principle parallelizable).
     * @param queryOptions
     *            Options that will be imposed if the iprogram is evaluated as a
     *            query.
     */
    public Program(String name, boolean parallel, IQueryOptions queryOptions) {

        this(name, parallel, false/* closure */, queryOptions);

    }

    /**
     * An empty program.
     * 
     * @param name
     *            A label for the program.
     * @param parallel
     *            <code>true</code> iff the steps in the program are
     *            parallelizable (this does not imply that they will be executed
     *            in parallel, only that they do not have dependencies among the
     *            steps and hence are in principle parallelizable).
     * @param closure
     *            <code>true</code> iff the steps in the program must be run
     *            until a fixed point is achieved.
     */
    protected Program(String name, boolean parallel, boolean closure) {

        this(name, parallel, closure, QueryOptions.NONE);
    }

    /**
     * Fully specific ctor.
     * 
     * @param parallel
     *            <code>true</code> iff the steps in the program are
     *            parallelizable (this does not imply that they will be executed
     *            in parallel, only that they do not have dependencies among the
     *            steps and hence are in principle parallelizable).
     * @param closure
     *            <code>true</code> iff the steps in the program must be run
     *            until a fixed point is achieved.
     * @param queryOptions
     *            Options that will be imposed if the iprogram is evaluated as a
     *            query.
     */
    private Program(String name, boolean parallel, boolean closure,
            IQueryOptions queryOptions) {
    
        if (name == null)
            throw new IllegalArgumentException();
        
        if (queryOptions == null)
            throw new IllegalArgumentException();
        
        this.name = name;
        
        this.parallel = parallel;
        
        this.closure = closure;
        
        this.queryOptions = queryOptions;
        
    }
    
    public String getName() {
        
        return name;
        
    }

    final public boolean isRule() {
        
        return false;
        
    }
    
    public boolean isParallel() {

        return parallel;
        
    }

    public boolean isClosure() {
        
        return closure;
        
    }

    public IQueryOptions getQueryOptions() {
        
        return queryOptions;
        
    }
    
    public int stepCount() {
        
        return steps.size();
        
    }
    
    public Iterator<IStep> steps() {

        return steps.iterator();
    }

    public IStep[] toArray() {
        
        return steps.toArray(new IStep[stepCount()]);
        
    }
    
    /**
     * Add another step in the program.
     * 
     * @param step
     *            The step.
     */
    public void addStep(final IStep step) {

        if (step == null)
            throw new IllegalArgumentException();

        if (step == this) // no cycles please.
            throw new IllegalArgumentException();
        
        steps.add(step);

    }

    /**
     * Adds the steps to the program.
     * 
     * @param steps
     *            The steps.
     */
    public void addSteps(final Iterator<? extends IStep> steps) {
        
        if (steps == null)
            throw new IllegalArgumentException();
        
        while(steps.hasNext()) {
            
            addStep(steps.next());
            
        }
        
    }

    /**
     * Adds a sub-program consisting of the fixed point closure of the given
     * rules.
     * 
     * @param rules
     *            The rules.
     * 
     * @throws IllegalArgumentException
     *             if <i>rules</i> or any element of <i>rules</i> is
     *             <code>null</code>.
     * @throws IllegalStateException
     *             if <i>this</i> program is parallel.
     */
    public void addClosureOf(IRule[] rules) {
        
        if(isParallel())
            throw new IllegalStateException("parallel program can not embed closure operations.");
        
        if (rules == null)
            throw new IllegalArgumentException();

        if (rules.length == 0)
            throw new IllegalArgumentException();
        
        final String label;
        if(true) {
            
            StringBuilder sb = new StringBuilder();
            
            sb.append("closure[");
            
            int i = 0;
            
            for(IRule r : rules) {
                
                if (i > 0) {
                
                    /*
                     * Note: Do NOT use a comma here - it will break the comma
                     * delimited format written by RuleStats.
                     */
                    sb.append(" ");
                
                }
                
                sb.append( r.getName() );
                
            }
            
            sb.append("]");
            
            label = sb.toString();
            
        } else {
            
            label = "closure(nrules=" + rules.length + ")";
        }
        
        final Program subProgram = new Program(label, true/* parallel */, true/* closure */);

        // add the rules whose closure will be computed into the sub-program.
        subProgram.addSteps(Arrays.asList(rules).iterator());

        /*
         * Add the sub-program to this program.
         * 
         * Note: it will be mapped for truth maintenance if the instance is a
         * MappedProgram and a focus store was specified.
         */
        addStep(subProgram);
        
    }

    /**
     * Adds a sub-program consisting of the fixed point closure of the given
     * rule.
     * 
     * @param rule
     *            The rule.
     * 
     * @throws IllegalArgumentException
     *             if <i>rule</i> is <code>null</code>.
     * @throws IllegalStateException
     *             if <i>this</i> program is parallel.
     */
    public void addClosureOf(IRule rule) {

        if (rule == null)
            throw new IllegalArgumentException();

        addClosureOf(new IRule[] { rule });

    }

    protected StringBuilder toString(int depth) {

        final StringBuilder sb = new StringBuilder();
        
        sb.append(ws, 0, depth);

        sb.append(getClass().getSimpleName());
        
        sb.append("{ name="+getName());
        
        sb.append(", parallel="+isParallel());

        sb.append(", closure="+isClosure());
        
        sb.append(", nsteps="+stepCount());
        
//        sb.append(", steps=" + Arrays.toString(toArray()));

        sb.append("}");
        
        for(IStep step : steps) {
            
            sb.append("\n");

            if (step.isRule()) {

                sb.append(ws, 0, depth + inc);

                sb.append(step.toString());

            } else {

                sb.append(((Program) step).toString(depth + inc));
                
            }
            
        }
        
        return sb;
        
    }
    
    private static final transient int inc = 1;
    private static final transient String ws = "...............................................";
    
    public String toString() {
    
        return toString(0).toString();
        
    }
    
}
