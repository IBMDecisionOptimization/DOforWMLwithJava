package ilog.cplex;

import ilog.concert.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Vector;

final class Relaxations {
    private static final Logger logger = LogManager.getLogger();

    private static final class Relax {
        public static final class Elem {
            final IloAddable obj;
            final double preference;

            public Elem(IloAddable obj, double p) {
                this.obj = obj;
                this.preference = p;
            }

            public String getName() throws IloException {
                if (this.obj.getName() == null) {
                    logger.error("Missing name for relaxing " + obj);
                    throw new IloException("Missing name for relaxing " + obj);
                }
                return obj.getName();
            }
        }

        private int getSize() {
            return elems.size();
        }

        private final Collection<Elem> elems = new Vector<>();


        public Relax() {
        }

        public Relax add(IloAddable obj, double preference) {
            elems.add(new Elem(obj, preference));
            return this;
        }

        private static void fillBuffer(StringBuffer buffer, Relax relax, String tag) throws IloException {
            final String relaxName = "<relax name='";
            final String preference = "' preference='";
            final String end = "'/>";
            buffer.append("<").append(tag).append(">");
            for (final Elem e : relax.elems)
                buffer.append(relaxName).append(e.getName()).append(preference).append(e.preference).append(end);
            buffer.append("</").append(tag).append(">");
        }

        public static byte[] makeFile(Relax rhs, Relax rng, Relax lb, Relax ub) throws IloException {
            final StringBuffer buffer = new StringBuffer();
            buffer.append("<CPLEXFeasopt infeasibilityFile='true' resultNames='true'>");
            if (rhs.getSize() != 0) fillBuffer(buffer, rhs, "rhs");
            if (rng.getSize() != 0) fillBuffer(buffer, rng, "rng");
            if (lb.getSize() != 0) fillBuffer(buffer, lb, "lb");
            if (ub.getSize() != 0) fillBuffer(buffer, ub, "ub");
            buffer.append("</CPLEXFeasopt>");
            String r = buffer.toString();
            logger.info("Relaxations file is " + r);
            return r.getBytes(StandardCharsets.UTF_8);
        }
    }

    private final Relax rhs = new Relax();
    private final Relax rng = new Relax();
    private final Relax lb = new Relax();
    private final Relax ub = new Relax();

    public Relaxations() {
    }

    public int getSize() {
        return rhs.getSize() + rng.getSize() + lb.getSize() + ub.getSize();
    }

    public void add(IloRange r, double p) {
        rng.add(r, p);
    }

    public void add(IloConstraint r, double p) throws IloException {
        if (r instanceof IloRange) {
            IloRange range = (IloRange) r;
            if (range.getLB() == -Double.MAX_VALUE || range.getUB() == Double.MAX_VALUE) {
                rhs.add(r, p);
            } else rng.add(r, p);
        } else
            throw new IloException("Only ranges are supported in WML feasopt.");
    }

    private void _add(IloAddable a, double lbound, double ubound) {
        lb.add(a, lbound);
        ub.add(a, ubound);
    }

    public void add(IloRange r, double lbound, double ubound) {
        _add(r, lbound, ubound);
    }

    public void add(IloNumVar v, double lbound, double ubound) {
        _add(v, lbound, ubound);
    }

    public byte[] makeFile() throws IloException {
        return Relax.makeFile(rhs, rng, lb, ub);
    }
}
