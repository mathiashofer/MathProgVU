package at.mhofer.mathprog.kmst.model;

import java.util.LinkedList;
import java.util.List;

import at.mhofer.mathprog.kmst.data.Edge;
import at.mhofer.mathprog.kmst.data.Instance;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloObjectiveSense;
import ilog.cplex.IloCplex;

/**
 * Single-Commodity-Flow formulation of k-Node MST
 * 
 * @author Mathias
 *
 */
public class SCFKMSTModel implements Model {

	private IloCplex cplex = null;

	private List<List<Integer>> incidenceEdges;

	private Edge[] edges;
	private Edge[] invEdges;

	private int numEdges;

	private int k;

	public SCFKMSTModel(Instance instance, int k) {
		this.incidenceEdges = instance.getIncidentEdges();
		this.edges = instance.getEdges();
		this.invEdges = new Edge[instance.getNumEdges()];
		for (int i = 0; i < invEdges.length; i++) {
			Edge e = this.edges[i];
			this.invEdges[i] = new Edge(e.getV2(), e.getV1(), e.getWeight());
		}
		this.numEdges = instance.getNumEdges();
		this.k = k;
	}

	@Override
	public void build() throws IloException {
		cplex = new IloCplex();

		// Variables
		IloIntVar[] x = cplex.boolVarArray(numEdges);
		IloIntVar[] f = cplex.intVarArray(numEdges, 0, Integer.MAX_VALUE);
		IloIntVar[] invf = cplex.intVarArray(numEdges, 0, Integer.MAX_VALUE);

		// Objective function
		IloLinearIntExpr obj = cplex.linearIntExpr();
		for (int i = 0; i < numEdges; i++) {
			Edge e = this.edges[i];
			obj.addTerm(x[i], e.getWeight());
		}
		cplex.addObjective(IloObjectiveSense.Minimize, obj);

		// Constraints
		int root = 0;

		IloLinearIntExpr c1a = cplex.linearIntExpr();
//		for ()
	}

	private List<Edge> incomingEdges(int v) {
		List<Edge> incomingEdges = new LinkedList<Edge>();
		for (Integer i : incidenceEdges.get(v)) {
			if (this.edges[i].getV2() == v) {
				incomingEdges.add(this.edges[i]);
			} else {
				incomingEdges.add(this.invEdges[i]);
			}
		}
		return incomingEdges;
	}

	private List<Edge> outgoingEdges(int v) {
		List<Edge> outgoingEdges = new LinkedList<Edge>();
		for (Integer i : incidenceEdges.get(v)) {
			if (this.edges[i].getV1() == v) {
				outgoingEdges.add(this.edges[i]);
			} else {
				outgoingEdges.add(this.invEdges[i]);
			}
		}
		return outgoingEdges;
	}

	@Override
	public boolean solve() throws IloException {
		return cplex.solve();
	}

}
