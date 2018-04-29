package at.mhofer.mathprog.kmst.model;

import java.util.LinkedList;
import java.util.List;

import at.mhofer.mathprog.kmst.data.Edge;
import at.mhofer.mathprog.kmst.data.Instance;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjectiveSense;
import ilog.cplex.IloCplex;

/**
 * Miller-Tucker-Zemlin subtour elimination
 * 
 * @author Mathias
 *
 */
@Deprecated
public class MTZModel implements Model {

	private static final int ARTIFICIAL_ROOT = 0;

	private IloCplex cplex = null;

	private List<List<Integer>> incidenceEdges;

	private Edge[] edges;

	private int numEdges;
	private int numNodes;

	private int k;

	public MTZModel(Instance instance, int k) {
		this.incidenceEdges = instance.getIncidentEdges();
		this.edges = instance.getEdges();
		this.numEdges = instance.getNumEdges();
		this.numNodes = instance.getNumNodes();

		// add 1, since we have the artificial node and therefore an additional
		// edge
		this.k = k;
	}

	@Override
	public void build() throws IloException {
		cplex = new IloCplex();

		// Variables
		IloIntVar[] x = cplex.boolVarArray(numEdges * 2);
		IloNumVar[] u = cplex.numVarArray(numNodes, 1, k);

		// Map<Edge, IloNumVar> edgeVarMap = new HashMap<Edge, IloNumVar>();
		// for (int i = 0; i < numEdges; i++) {
		// edgeVarMap.put(edges[i], x[i]);
		// }

		// Objective function
		IloLinearIntExpr obj = cplex.linearIntExpr();
		for (int i = 0; i < numEdges; i++) {
			Edge e = this.edges[i];
			obj.addTerm(x[i], e.getWeight());
			obj.addTerm(x[i + numEdges], e.getWeight());
		}
		cplex.addObjective(IloObjectiveSense.Minimize, obj);

		// Constraints
		for (int i = 0; i < numEdges; i++) {
			Edge e = this.edges[i];
			if (e.getV1() != 0 && e.getV2() != 0) {
				cplex.addLe(cplex.sum(u[e.getV1()], x[i]),
						cplex.sum(u[e.getV2()], cplex.prod(k, cplex.sum(1, cplex.negative(x[i])))));

				cplex.addLe(cplex.sum(u[e.getV2()], x[i + numEdges]),
						cplex.sum(u[e.getV1()], cplex.prod(k, cplex.sum(1, cplex.negative(x[i + numEdges])))));
			}
		}

		cplex.addEq(u[0], 1);

		IloLinearNumExpr[] incNodesExpr = new IloLinearNumExpr[numNodes];
		for (int i = 1; i < numNodes; i++) {
			incNodesExpr[i] = cplex.linearNumExpr();
			for (int e : incomingEdges(i)) {
				incNodesExpr[i].addTerm(1, x[e]);
			}
			cplex.addLe(incNodesExpr[i], 1);
		}

		for (int i = 0; i < numEdges; i++) {
			cplex.addLe(cplex.sum(x[i], x[i + numEdges]), 1);
		}

		// Constraints
//		for (int i = 1; i < numNodes; i++) {
//			Edge e = this.edges[i];
//			if (e.getV1() != 0) {
//				cplex.addLe(cplex.prod(k, cplex.sum(1, cplex.negative(incNodesExpr[i]))), u[e.getV1()]);
//			}
//			if (e.getV2() != 0)
//				cplex.addLe(cplex.prod(k, cplex.sum(1, cplex.negative(incNodesExpr[i]))), u[e.getV2()]);
//
//		}

		// C6: choose k edges and therefore k+1 nodes, which leads to k nodes
		// without the artificial root node
		cplex.addEq(cplex.sum(x), k, "C6");

		// C7: take only one of the artificial 0-weight edges
		IloLinearIntExpr c6 = cplex.linearIntExpr();
		for (int i = 0; i < numNodes - 1; i++) {
			c6.addTerm(x[i], 1);
			c6.addTerm(x[i + numEdges], 1);
		}
		cplex.addLe(c6, 1, "C7");
	}

	private List<Integer> incomingEdges(int v) {
		List<Integer> incomingEdges = new LinkedList<Integer>();
		if (v != ARTIFICIAL_ROOT) {
			for (Integer i : incidenceEdges.get(v)) {
				if (this.edges[i].getV2() == v) {
					incomingEdges.add(i);
				} else {
					incomingEdges.add(i + numEdges);
				}
			}
		}
		return incomingEdges;
	}

	@Override
	public boolean solve() throws IloException {
		boolean ret = cplex.solve();
		System.out.println(cplex.getObjValue());
		return ret;
	}

}
