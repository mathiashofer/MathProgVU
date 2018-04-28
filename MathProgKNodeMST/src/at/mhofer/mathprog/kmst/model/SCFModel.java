package at.mhofer.mathprog.kmst.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
public class SCFModel implements Model {

	private static final int ARTIFICIAL_ROOT = 0;

	private IloCplex cplex = null;

	private List<List<Integer>> incidenceEdges;

	private Edge[] edges;
	private Edge[] invEdges;

	private int numEdges;
	private int numNodes;

	private int k;

	public SCFModel(Instance instance, int k) {
		this.incidenceEdges = instance.getIncidentEdges();
		this.edges = instance.getEdges();
		this.invEdges = new Edge[instance.getNumEdges()];
		for (int i = 0; i < invEdges.length; i++) {
			Edge e = this.edges[i];
			this.invEdges[i] = new Edge(e.getV2(), e.getV1(), e.getWeight());
		}
		this.numEdges = instance.getNumEdges();
		this.numNodes = instance.getNumNodes();

		// add 1, since we have the artificial node and therefore an additional
		// edge
		this.k = k;
	}

	IloIntVar[] y = null;

	IloIntVar[] x = null;

	IloIntVar[] f = null;

	IloIntVar[] invf = null;

	@Override
	public void build() throws IloException {
		cplex = new IloCplex();

		// Variables
		y = cplex.boolVarArray(numNodes);
		x = cplex.boolVarArray(numEdges);
		f = cplex.intVarArray(numEdges, 0, Integer.MAX_VALUE);
		invf = cplex.intVarArray(numEdges, 0, Integer.MAX_VALUE);

		Map<Edge, IloIntVar> edgeVarMap = new HashMap<Edge, IloIntVar>();
		for (int i = 0; i < numEdges; i++) {
			edgeVarMap.put(edges[i], f[i]);
			edgeVarMap.put(invEdges[i], invf[i]);
		}

		// Objective function
		IloLinearIntExpr obj = cplex.linearIntExpr();
		for (int i = 0; i < numEdges; i++) {
			Edge e = this.edges[i];
			obj.addTerm(x[i], e.getWeight());
		}
		cplex.addObjective(IloObjectiveSense.Minimize, obj);

		// Constraints
		// C1: send out commodities from root
		IloLinearIntExpr c1 = cplex.linearIntExpr();
		for (Edge e : outgoingEdges(ARTIFICIAL_ROOT)) {
			c1.addTerm(edgeVarMap.get(e), 1);
		}
		cplex.addEq(c1, k, "C1");

		// C2: for each of the selected nodes we consume one commodity and send the rest out, for all other nodes the difference is 0
		for (int i = 1; i < numNodes; i++) {
			IloLinearIntExpr c2 = cplex.linearIntExpr();
			for (Edge e : outgoingEdges(i)) {
				c2.addTerm(edgeVarMap.get(e), 1);
			}
			for (Edge e : incomingEdges(i)) {
				c2.addTerm(edgeVarMap.get(e), -1);
			}
			cplex.addEq(c2, cplex.prod(-1, y[i]), "C2");
		}

		// C3: if we select some edge e = (i,j) then y_i and y_j has to be 1
		for (int i = 0; i < numEdges; i++) {
			Edge e = edges[i];
			cplex.addLe(x[i], y[e.getV2()], "C3");
			cplex.addLe(x[i], y[e.getV1()], "C3");
		}

		// C4: control the flow on edge i, i.e. 0 if we do not choose edge i
		for (int i = 0; i < numEdges; i++) {
			if (i < numNodes - 1) {
				// artificial edges
				cplex.addLe(f[i], cplex.prod(k, x[i]), "C4");
			} else {
				cplex.addLe(f[i], cplex.prod(k - 1, x[i]), "C4");
			}
		}

		// C5: control the flow on edge i, i.e. 0 if we do not choose edge i
		for (int i = 0; i < numEdges; i++) {
			cplex.addLe(invf[i], cplex.prod(k - 1, x[i]), "C5");
		}

		// C6: choose k edges and therefore k+1 nodes, which leads to k nodes without the artificial root node
		cplex.addEq(cplex.sum(x), k, "C6");

		// C7: take only one of the artificial 0-weight edges
		IloLinearIntExpr c6 = cplex.linearIntExpr();
		for (int i = 0; i < numNodes - 1; i++) {
			c6.addTerm(x[i], 1);
		}
		cplex.addEq(c6, 1, "C7");
	}

	/**
	 * delta-
	 * 
	 * deals with the artificial node
	 * 
	 * @param v
	 * @return
	 */
	private List<Edge> incomingEdges(int v) {
		List<Edge> incomingEdges = new LinkedList<Edge>();
		if (v != ARTIFICIAL_ROOT) {
			for (Integer i : incidenceEdges.get(v)) {
				if (this.edges[i].getV2() == v) {
					incomingEdges.add(this.edges[i]);
				} else if (this.invEdges[i].getV2() == v) {
					incomingEdges.add(this.invEdges[i]);
				}
			}
		}
		return incomingEdges;
	}

	/**
	 * delta+
	 * 
	 * deals with the artificial node
	 * 
	 * @param v
	 * @return
	 */
	private List<Edge> outgoingEdges(int v) {
		List<Edge> outgoingEdges = new LinkedList<Edge>();
		for (Integer i : incidenceEdges.get(v)) {
			if (this.edges[i].getV1() == v && this.edges[i].getV2() != ARTIFICIAL_ROOT) {
				outgoingEdges.add(this.edges[i]);
			} else if (this.invEdges[i].getV1() == v && this.invEdges[i].getV2() != ARTIFICIAL_ROOT) {
				outgoingEdges.add(this.invEdges[i]);
			}
		}
		return outgoingEdges;
	}

	private void printVariables(double[] values, String varName) {
		System.out.print("[");
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				System.out.print(", ");
			}
			System.out.print(varName + "_" + i + ": " + values[i]);
		}
		System.out.println("]");
	}

	@Override
	public boolean solve() throws IloException {
		boolean ret = cplex.solve();
		System.out.println(cplex.getObjValue());
		// printVariables(cplex.getValues(x), "x");
		// printVariables(cplex.getValues(f), "f");
		// printVariables(cplex.getValues(invf), "invf");
		return ret;
	}

}
