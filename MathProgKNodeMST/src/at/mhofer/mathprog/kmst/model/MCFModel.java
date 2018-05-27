package at.mhofer.mathprog.kmst.model;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import at.mhofer.mathprog.kmst.data.Edge;
import at.mhofer.mathprog.kmst.data.Instance;
import at.mhofer.mathprog.kmst.data.TupleKeyHashMap;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjectiveSense;
import ilog.cplex.IloCplex;

public class MCFModel implements Model {

	private static final int ARTIFICIAL_ROOT = 0;

	private IloCplex cplex = null;

	private List<List<Integer>> incidenceEdges;

	private Edge[] edges;
	private Edge[] invEdges;

	private int numEdges;
	private int numNodes;

	private int k;

	public MCFModel(Instance instance, int k) {
		this.incidenceEdges = instance.getIncidentEdges();
		this.edges = instance.getEdges();
		this.invEdges = new Edge[instance.getNumEdges()];
		for (int i = 0; i < invEdges.length; i++) {
			Edge e = this.edges[i];
			this.invEdges[i] = new Edge(e.getV2(), e.getV1(), e.getWeight());
		}
		this.numEdges = instance.getNumEdges();
		this.numNodes = instance.getNumNodes();

		this.k = k;
	}

	@Override
	public void build() throws IloException {
		cplex = new IloCplex();

		// Variables
		IloIntVar[] y = cplex.boolVarArray(numNodes);
		IloIntVar[] x = cplex.boolVarArray(numEdges);
		IloNumVar[][] f = new IloNumVar[numNodes][];
		IloNumVar[][] invf = new IloNumVar[numNodes][];

		for (int c = 1; c < numNodes; c++) {
			f[c] = cplex.numVarArray(numEdges, 0, Double.MAX_VALUE);
			invf[c] = cplex.numVarArray(numEdges, 0, Double.MAX_VALUE);
		}

		TupleKeyHashMap<Integer, Edge, IloNumVar> edgeVarMap = new TupleKeyHashMap<Integer, Edge, IloNumVar>();
		Map<Edge, Integer> edgeIndizes = new HashMap<Edge, Integer>();
		for (int c = 1; c < numNodes; c++) {
			for (int i = 0; i < numEdges; i++) {
				edgeVarMap.put(c, edges[i], f[c][i]);
				edgeVarMap.put(c, invEdges[i], invf[c][i]);
				edgeIndizes.put(edges[i], i);
				edgeIndizes.put(invEdges[i], i);
			}
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
//		 for (int c = 1; c < numNodes; c++) {
//		 IloLinearNumExpr c1 = cplex.linearNumExpr();
//		 for (Edge e : outgoingEdges(ARTIFICIAL_ROOT)) {
//		 c1.addTerm(edgeVarMap.get(c, e), 1);
//		 }
//		 cplex.addEq(c1, y[c], "C1");
//		 }

		// C2: each one of the selected nodes must consume its destined
		// commodity, the rest must be 0
		for (int c = 1; c < numNodes; c++) {
			for (int i = 0; i < numNodes; i++) {
				IloLinearNumExpr c2 = cplex.linearNumExpr();
				for (Edge e : outgoingEdges(i)) {
					c2.addTerm(edgeVarMap.get(c, e), 1);
				}
				for (Edge e : incomingEdges(i)) {
					c2.addTerm(edgeVarMap.get(c, e), -1);
				}
				if (i == ARTIFICIAL_ROOT) {
					cplex.addEq(c2, y[c], "C1");
				} else if (i == c) {
					cplex.addEq(c2, cplex.negative(y[c]), "C2");
				} else {
					cplex.addEq(c2, 0, "C2");
				}
			}
		}

//		 cplex.addEq(y[0], 1);

		for (int i = 0; i < numNodes; i++) {
			IloLinearNumExpr c = cplex.linearNumExpr();
			for (Edge e : outgoingEdges(i)) {
				int index = edgeIndizes.get(e);
				c.addTerm(1, x[index]);
			}
			cplex.addGe(c, cplex.prod(2, y[i]));
		}

		// C3: if we select some edge e = (i,j) then y_i and y_j has to be 1
		for (int i = numNodes - 1; i < numEdges; i++) {
			Edge e = edges[i];
			cplex.addLe(x[i], y[e.getV2()], "C3");
			 cplex.addLe(x[i], y[e.getV1()], "C3");
		}

		cplex.addLe(cplex.sum(y), k + 1);

		// C4: control the flow on edge i, i.e. 0 if we do not choose edge i
		for (int c = 1; c < k; c++) {
			for (int i = 1; i < numEdges; i++) {
				cplex.addLe(cplex.sum(invf[c][i], f[c][i]), x[i], "C4");
			}
		}
		
		for (int c = 1; c < k; c++) {
			for (int i = 0; i < numEdges; i++) {
//				if (i < numNodes - 1) {
//					// artificial edges
//					cplex.addEq(f[c][i], 0, "C5");
//				} else {
					cplex.addLe(f[c][i], x[i], "C5");
//				}
			}
		}

		// C5: control the flow on edge i, i.e. 0 if we do not choose edge i
		for (int c = 1; c < k; c++) {
			for (int i = 0; i < numEdges; i++) {
				if (i < numNodes - 1) {
					// artificial edges
					cplex.addEq(invf[c][i], 0, "C5");
				} else {
					cplex.addLe(invf[c][i], x[i], "C5");
				}
			}
		}

		// C6: choose k edges and therefore k+1 nodes, which leads to k nodes
		// without the artificial root node
		cplex.addEq(cplex.sum(x), k, "C6");

		// C7: take only one of the artificial 0-weight edges
		IloLinearIntExpr c6 = cplex.linearIntExpr();
		for (int i = 0; i < numNodes - 1; i++) {
			c6.addTerm(x[i], 1);
		}
		cplex.addLe(c6, 1, "C7");
	}

	@Override
	public void setOut(OutputStream out) {
		cplex.setOut(out);
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

	@Override
	public boolean solve() throws IloException {
		boolean ret = cplex.solve();
		System.out.println(cplex.getObjValue());
		return ret;
	}
}
