package com.bigdata.rdf.inf;

import java.util.Arrays;

import org.apache.log4j.Logger;

import com.bigdata.bop.BOp;
import com.bigdata.bop.BOpBase;
import com.bigdata.bop.Constant;
import com.bigdata.bop.IBindingSet;
import com.bigdata.bop.IConstant;
import com.bigdata.bop.IConstraint;
import com.bigdata.bop.IPredicate;
import com.bigdata.bop.IVariable;
import com.bigdata.bop.IVariableOrConstant;
import com.bigdata.bop.Var;
import com.bigdata.bop.constraint.BooleanValueExpression;
import com.bigdata.bop.constraint.Constraint;
import com.bigdata.bop.joinGraph.IEvaluationPlan;
import com.bigdata.bop.joinGraph.IEvaluationPlanFactory;
import com.bigdata.bop.joinGraph.fast.DefaultEvaluationPlanFactory2;
import com.bigdata.btree.IIndex;
import com.bigdata.btree.ITupleIterator;
import com.bigdata.rdf.internal.IV;
import com.bigdata.rdf.internal.IVUtility;
import com.bigdata.rdf.rules.RuleContextEnum;
import com.bigdata.rdf.spo.ISPO;
import com.bigdata.rdf.spo.SPO;
import com.bigdata.rdf.spo.SPOKeyOrder;
import com.bigdata.rdf.spo.SPOPredicate;
import com.bigdata.rdf.store.AbstractTripleStore;
import com.bigdata.relation.accesspath.IAccessPath;
import com.bigdata.relation.rule.IAccessPathExpander;
import com.bigdata.relation.rule.IRule;
import com.bigdata.relation.rule.QueryOptions;
import com.bigdata.relation.rule.Rule;
import com.bigdata.relation.rule.eval.ActionEnum;
import com.bigdata.relation.rule.eval.IJoinNexus;
import com.bigdata.relation.rule.eval.IJoinNexusFactory;
import com.bigdata.relation.rule.eval.ISolution;
import com.bigdata.striterator.ChunkedArrayIterator;
import com.bigdata.striterator.IChunkedOrderedIterator;
import com.bigdata.striterator.IKeyOrder;

public class OwlSameAsPropertiesExpandingIterator implements
        IChunkedOrderedIterator<ISPO> {
    protected final static Logger log =
            Logger.getLogger(OwlSameAsPropertiesExpandingIterator.class);

    private final IChunkedOrderedIterator<ISPO> src;

    private final IKeyOrder<ISPO> keyOrder;

    private final IV s, p, o;

    private final AbstractTripleStore db;

    private final IV sameAs;

    private IChunkedOrderedIterator<ISolution> solutions;

    private IAccessPathExpander<ISPO> sameAsSelfExpander;

    public OwlSameAsPropertiesExpandingIterator(IV s, IV p, IV o,
            AbstractTripleStore db, final IV sameAs,
            final IKeyOrder<ISPO> keyOrder) {
        this.db = db;
        this.s = s;
        this.p = p;
        this.o = o;
        this.sameAs = sameAs;
        this.keyOrder = keyOrder;
        this.sameAsSelfExpander = new SameAsSelfExpander();
        if (p == sameAs) {
            // we don't need to run the expander when the predicate is
            // owl:sameAs, the forward chainer takes care of that case
            this.src = db.getAccessPath(s, p, o).iterator();
        } else {
            this.src = null;
            try {
                if (s != null && o != null) {
                    accessSPO();
                } else if (s != null && o == null) {
                    accessSP();
                } else if (s == null && o != null) {
                    accessPO();
                } else if (s == null && o == null) {
                    accessP();
                } else
                    throw new AssertionError();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void accessSPO() throws Exception {
        /*
         construct S ?p O
         where {
         ?sameS sameAs S .
         ?sameO sameAs O .
         ?sameS ?p ?sameO
         }
         */
        final String SPO = db.getSPORelation().getNamespace();
        final IVariable<IV> _sameS = Var.var("sameS");
        final IVariable<IV> _sameO = Var.var("sameO");
        final IConstant<IV> sameAs = new Constant<IV>(this.sameAs);
        final IConstant<IV> s = new Constant<IV>(this.s);
        final IConstant<IV> o = new Constant<IV>(this.o);
        final IVariableOrConstant<IV> _p =
                this.p != null ? new Constant<IV>(this.p) : Var.var("p");
        final SPOPredicate head = new SPOPredicate(SPO, s, _p, o);
        final IRule rule =
                new Rule("sameAsSPO", head, new IPredicate[] {
                        new SPOPredicate(SPO, _sameS, sameAs, s,
                                sameAsSelfExpander),
                        new SPOPredicate(SPO, _sameO, sameAs, o,
                                sameAsSelfExpander),
                        new SPOPredicate(SPO, _sameS, _p, _sameO) },
                        QueryOptions.DISTINCT, // distinct
                        // constraints on the rule.
                        new IConstraint[] { Constraint.wrap(new RejectSameAsSelf(head.s(), head
                                .p(), head.o())) });
        runQuery(rule);
    }

    private void accessSP() throws Exception {
        /*
         construct S ?p ?sameO
         where {
         ?sameS sameAs S .
         ?sameS ?p ?o .
         ?o sameAs ?sameO
         }
         */
        final String SPO = db.getSPORelation().getNamespace();
        final IVariable<IV> _sameS = Var.var("sameS");
        final IVariable<IV> _sameO = Var.var("sameO");
        final IConstant<IV> sameAs = new Constant<IV>(this.sameAs);
        final IConstant<IV> s = new Constant<IV>(this.s);
        final IVariable<IV> _o = Var.var("o");
        final IVariableOrConstant<IV> _p =
                this.p != null ? new Constant<IV>(this.p) : Var.var("p");
        final SPOPredicate head = new SPOPredicate(SPO, s, _p, _sameO);
        final IRule rule =
                new Rule("sameAsSP", head, new IPredicate[] {
                        new SPOPredicate(SPO, _sameS, sameAs, s,
                                sameAsSelfExpander), // ?s sameAs y
                        new SPOPredicate(SPO, _sameS, _p, _o), // ?s b ?o  -> y b w
                        new SPOPredicate(SPO, _o, sameAs, _sameO, true,
                                sameAsSelfExpander) },
                        QueryOptions.DISTINCT, // distinct
                        // constraints on the rule.
                        new IConstraint[] { Constraint.wrap(new RejectSameAsSelf(head.s(), head
                                .p(), head.o())) });
        runQuery(rule);
    }

    private void accessPO() throws Exception {
        /*
         construct ?sameS ?p O
         where {
         ?sameO sameAs O .
         ?s ?p ?sameO .
         ?s sameAs ?sameS
         }
         */
        final String SPO = db.getSPORelation().getNamespace();
        final IVariable<IV> _sameS = Var.var("sameS");
        final IVariable<IV> _sameO = Var.var("sameO");
        final IConstant<IV> sameAs = new Constant<IV>(this.sameAs);
        final IVariable<IV> _s = Var.var("s");
        final IConstant<IV> o = new Constant<IV>(this.o);
        final IVariableOrConstant<IV> _p =
                this.p != null ? new Constant<IV>(this.p) : Var.var("p");
        final SPOPredicate head = new SPOPredicate(SPO, _sameS, _p, o);
        final IRule rule =
                new Rule("sameAsPO", head, new IPredicate[] {
                        new SPOPredicate(SPO, _sameO, sameAs, o,
                                sameAsSelfExpander),
                        new SPOPredicate(SPO, _s, _p, _sameO),
                        new SPOPredicate(SPO, _s, sameAs, _sameS, true,
                                sameAsSelfExpander) }, 
                        QueryOptions.DISTINCT, // distinct
                        // constraints on the rule.
                        new IConstraint[] { Constraint.wrap(new RejectSameAsSelf(head.s(), head
                                .p(), head.o())) });
        runQuery(rule);
    }

    private void accessP() throws Exception {
        /*
         construct ?sameS ?p ?sameO
         where {
         ?s sameAs ?sameS .
         ?o sameAs ?sameO .
         ?s ?p ?o
         }
         */
        final String SPO = db.getSPORelation().getNamespace();
        final IVariable<IV> _sameS = Var.var("sameS");
        final IVariable<IV> _sameO = Var.var("sameO");
        final IConstant<IV> sameAs = new Constant<IV>(this.sameAs);
        final IVariable<IV> _s = Var.var("s");
        final IVariable<IV> _o = Var.var("o");
        final IVariableOrConstant<IV> _p =
                this.p != null ? new Constant<IV>(this.p) : Var.var("p");
        final SPOPredicate head = new SPOPredicate(SPO, _sameS, _p, _sameO);
        final IRule rule =
                new Rule("sameAsP", head, new IPredicate[] {
                        new SPOPredicate(SPO, _sameS, sameAs, _s, true, 
                                sameAsSelfExpander),
                        new SPOPredicate(SPO, _sameO, sameAs, _o, true,
                                sameAsSelfExpander),
                        new SPOPredicate(SPO, _s, _p, _o) }, 
                        QueryOptions.DISTINCT, // distinct
                        // constraints on the rule.
                        new IConstraint[] { Constraint.wrap(new RejectSameAsSelf(head.s(), head
                                .p(), head.o())) });
        runQuery(rule);
    }

    private void runQuery(IRule rule)
            throws Exception {
        // run the query as a native rule.
        final IEvaluationPlanFactory planFactory =
                DefaultEvaluationPlanFactory2.INSTANCE;
        final IJoinNexusFactory joinNexusFactory =
                db.newJoinNexusFactory(RuleContextEnum.HighLevelQuery,
                        ActionEnum.Query, IJoinNexus.ELEMENT, null, // filter
                        false, // justify 
                        false, // backchain
                        planFactory);
        final IJoinNexus joinNexus =
                joinNexusFactory.newInstance(db.getIndexManager());
        if (log.isInfoEnabled()) {
            final IEvaluationPlan plan = planFactory.newPlan(joinNexus, rule);
            StringBuilder sb = new StringBuilder();
            int order[] = plan.getOrder();
            for (int i = 0; i < order.length; i++) {
                sb.append(order[i]);
                if (i < order.length - 1) {
                    sb.append(",");
                }
            }
            log.info("order: [" + sb.toString() + "]");
        }
        this.solutions = joinNexus.runQuery(rule);
        // this.resolverator = resolverator;
    }

    public IKeyOrder<ISPO> getKeyOrder() {
        if (src != null) {
            return src.getKeyOrder();
        }
        return keyOrder;
    }

    private ISPO[] chunk;

    private int i = 0;

    public ISPO[] nextChunk() {
        if (src != null) {
            return src.nextChunk();
        }
        final int chunkSize = 10000;
        ISPO[] s = new ISPO[chunkSize];
        int n = 0;
        while (hasNext() && n < chunkSize) {
            ISolution<ISPO> solution = solutions.next();
            // s[n++] = resolverator.resolve(solution);
            ISPO spo = solution.get();
            spo = new SPO(spo.s(), spo.p(), spo.o());
            s[n++] = spo;
        }
        
        // copy so that stmts[] is dense.
        ISPO[] stmts = new ISPO[n];
        System.arraycopy(s, 0, stmts, 0, n);
        
        // fill in the explicit/inferred information, sort by SPO key order
        // since we will use the SPO index to do the value completion
        stmts = db.bulkCompleteStatements(stmts);
        
        // resort into desired order
        Arrays.sort(stmts, 0, n, this.keyOrder.getComparator());
        
        return stmts;
    }

    public ISPO[] nextChunk(IKeyOrder<ISPO> keyOrder) {
        if (src != null) {
            return src.nextChunk(keyOrder);
        }
        if (keyOrder == null)
            throw new IllegalArgumentException();
        ISPO[] stmts = nextChunk();
        if (this.keyOrder != keyOrder) {
            // sort into the required order.
            Arrays.sort(stmts, 0, stmts.length, keyOrder.getComparator());
        }
        return stmts;
    }

    public ISPO next() {
        if (src != null) {
            return src.next();
        }
        if (chunk == null || i == chunk.length) {
            chunk = nextChunk();
            i = 0;
            if (log.isInfoEnabled()) 
                log.info("got a chunk, length = " + chunk.length);
        }
        return chunk[i++];
    }

    public void remove() {
        if (src != null) {
            src.remove();
            return;
        }
        throw new UnsupportedOperationException();
    }

    public void close() {
        if (src != null) {
            src.close();
            return;
        }
        if (solutions != null) {
            solutions.close();
        }
    }

    public boolean hasNext() {
        if (src != null) {
            return src.hasNext();
        }
        if (chunk != null) {
            return i < chunk.length;
        }
        if (solutions != null) {
            return solutions.hasNext();
        }
        return false;
    }

    private class SameAsSelfExpander implements IAccessPathExpander<ISPO> {
        public boolean backchain() {
            return false;
        }
        public boolean runFirst() {
            return false;
        }
        public IAccessPath<ISPO> getAccessPath(
                final IAccessPath<ISPO> accessPath) {
            return new SameAsSelfAccessPath(accessPath);
        }
    };

    private class SameAsSelfAccessPath implements IAccessPath<ISPO> {
        private IAccessPath<ISPO> accessPath;

        private SPO spo;

        public SameAsSelfAccessPath(IAccessPath<ISPO> accessPath) {
            this.accessPath = accessPath;
            final IVariableOrConstant<IV> p =
                    accessPath.getPredicate().get(1);
            if (!p.isConstant() || !IVUtility.equals(p.get(), sameAs)) {
                throw new UnsupportedOperationException("p must be owl:sameAs");
            }
        }

        private IChunkedOrderedIterator<ISPO> getAppender() {
            final IVariableOrConstant<IV> s =
                    accessPath.getPredicate().get(0);
            final IVariableOrConstant<IV> o =
                    accessPath.getPredicate().get(2);
            if (s.isVar() && o.isVar()) {
                throw new UnsupportedOperationException(
                        "s and o cannot both be variables");
            }
            if (s.isConstant() && o.isConstant()
                    && s.get().equals(o.get()) == false) {
                /*
                 * if s and o are both constants, then there is nothing for this
                 * appender to append, unless they are equal to each other, in
                 * which case we can append that solution
                 */
                this.spo = null;
            } else {
                final IV constant = s.isConstant() ? s.get() : o.get();
                this.spo =
                        s.isConstant() ? new SPO(s.get(), sameAs, constant)
                                : new SPO(constant, sameAs, o.get());
                if (log.isInfoEnabled()) 
                    log.info("appending SPO: " + spo.toString(db));
            }
            if (spo != null) {
                return new ChunkedArrayIterator<ISPO>(1, new SPO[] { spo },
                        SPOKeyOrder.SPO);
            } else {
                return null;
            }
        }

        @Override
        public IIndex getIndex() {
            return accessPath.getIndex();
        }

        @Override
        public IKeyOrder<ISPO> getKeyOrder() {
            return accessPath.getKeyOrder();
        }

        @Override
        public IPredicate<ISPO> getPredicate() {
            return accessPath.getPredicate();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public IChunkedOrderedIterator<ISPO> iterator() {
            final IChunkedOrderedIterator<ISPO> appender = getAppender();
            final IChunkedOrderedIterator<ISPO> delegate =
                    accessPath.iterator();
            if (appender == null) {
                return delegate;
            }
            return new IChunkedOrderedIterator<ISPO>() {
                public ISPO next() {
                    if (delegate.hasNext()) {
                        return delegate.next();
                    } else {
                        return appender.next();
                    }
                }

                public ISPO[] nextChunk() {
                    if (delegate.hasNext()) {
                        return delegate.nextChunk();
                    } else {
                        return appender.nextChunk();
                    }
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }

                public boolean hasNext() {
                    return delegate.hasNext() || appender.hasNext();
                }

                public IKeyOrder<ISPO> getKeyOrder() {
                    return delegate.getKeyOrder();
                }

                public ISPO[] nextChunk(IKeyOrder<ISPO> keyOrder) {
                    if (delegate.hasNext()) {
                        return delegate.nextChunk(keyOrder);
                    } else {
                        return appender.nextChunk(keyOrder);
                    }
                }

                public void close() {
                    delegate.close();
                    appender.close();
                }
            };
        }

//        public IChunkedOrderedIterator<ISPO> iterator(int limit, int capacity) {
//            throw new UnsupportedOperationException();
//        }

        @Override
        public IChunkedOrderedIterator<ISPO> iterator(long offset, long limit, int capacity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long rangeCount(boolean exact) {
            return accessPath.rangeCount(exact) + 1;
        }

//        @Override
//        public ITupleIterator<ISPO> rangeIterator() {
//            throw new UnsupportedOperationException();
//        }

        @Override
        public long removeAll() {
            throw new UnsupportedOperationException();
        }
    };

    private class RejectSameAsSelf extends BOpBase implements BooleanValueExpression {

        /**
		 * 
		 */
		private static final long serialVersionUID = -7877904606067597254L;

		public RejectSameAsSelf(final IVariableOrConstant<IV> _s,
                final IVariableOrConstant<IV> _p,
                final IVariableOrConstant<IV> _o) {

            super(new BOp[] { _s, _p, _o }, null/*annotations*/);
            
        }

        public Boolean get(final IBindingSet bindings) {
            final IV sVal = getValue((IVariableOrConstant)get(0/*_s*/), bindings);
            final IV pVal = getValue((IVariableOrConstant)get(1/*_p*/), bindings);
            final IV oVal = getValue((IVariableOrConstant)get(2/*_o*/), bindings);
            // not fully bound yet, just ignore for now
            if (sVal == null || pVal == null || oVal == null) {
                return true;
            }
            if (IVUtility.equals(pVal, sameAs) && IVUtility.equals(sVal, oVal)) {
                return false;
            }
            return true;
        }
        
        private IV getValue(final IVariableOrConstant<IV> _x,
                final IBindingSet bindings) {
            final IV val;
            if (_x.isConstant()) {
                val = _x.get();
            } else {
                final IConstant<IV> bound = bindings.get((IVariable<IV>) _x);
                val = bound != null ? bound.get() : null;
            }
            return val;
        }
    }
}
