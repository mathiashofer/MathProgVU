package at.mhofer.mathprog.kmst.model;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;

import at.mhofer.mathprog.kmst.data.Edge;
import at.mhofer.mathprog.kmst.data.Instance;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloObjectiveSense;
import ilog.cplex.IloCplex;

public class CECModel implements Model {

	private static final int ARTIFICIAL_ROOT = 0;

	private IloCplex cplex = null;

	private List<List<Integer>> incidenceEdges;

	private Edge[] edges;
	private Edge[] invEdges;

	private int numEdges;
	private int numNodes;

	private int k;

	private IloIntVar[] x;

	private Map<Edge, IloIntVar> edgeVarMap;

	public CECModel(Instance instance, int k) {
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
		cplex.use(new CycleElimination());

		this.x = cplex.boolVarArray(numEdges);

		this.edgeVarMap = new HashMap<Edge, IloIntVar>();
		for (int i = 0; i < numEdges; i++) {
			edgeVarMap.put(edges[i], x[i]);
			edgeVarMap.put(invEdges[i], x[i]);
		}

		// Objective function
		IloLinearIntExpr obj = cplex.linearIntExpr();
		for (int i = 0; i < numEdges; i++) {
			Edge e = this.edges[i];
			obj.addTerm(x[i], e.getWeight());
		}
		cplex.addObjective(IloObjectiveSense.Minimize, obj);

		// Make sure we get a connected tree
		for (int i = 1; i < numNodes; i++) {
			IloLinearIntExpr inSum = cplex.linearIntExpr();
			for (Edge in : incomingEdges(i)) {
				inSum.addTerm(1, edgeVarMap.get(in));
			}
			for (Edge out : outgoingEdges(i)) {
				cplex.addGe(inSum, edgeVarMap.get(out));
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

	@Override
	public boolean solve() throws IloException {
		boolean ret = cplex.solve();
		System.out.println(cplex.getObjValue());
		return ret;
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

	private class CycleElimination extends IloCplex.LazyConstraintCallback {

		@Override
		protected void main() throws IloException {
			double[] values = this.getValues(x);
			Graph<Integer, Edge> graph = new DefaultUndirectedWeightedGraph<>(Edge.class);

			for (int i = 0; i < numNodes; i++) {
				graph.addVertex(i);
			}

			for (int i = 0; i < numEdges; i++) {
				Edge e = edges[i];
				graph.addEdge(e.getV1(), e.getV2(), e);
				graph.setEdgeWeight(edges[i], 1 - values[i]);
				
//				Edge inve = invEdges[i];
//				graph.addEdge(inve.getV1(), inve.getV2(), inve);
//				graph.setEdgeWeight(invEdges[i], 1 - values[i]);
			}

			ShortestPathAlgorithm<Integer, Edge> dijkstra = new DijkstraShortestPath<Integer, Edge>(graph);
			for (int i = 0; i < numEdges; i++) {
				Edge e = edges[i];
				graph.removeEdge(e);
				GraphPath<Integer, Edge> path = dijkstra.getPath(e.getV2(), e.getV1());
				if (path == null) {
					path = dijkstra.getPath(e.getV1(), e.getV2());
				}
				graph.addEdge(e.getV1(), e.getV2(), e);
				graph.setEdgeWeight(e, 1 - values[i]);
				if (path != null) {					
					double pathWeight = path.getWeight();
					double edgeWeight = graph.getEdgeWeight(e);
					List<Edge> pathEdges = path.getEdgeList();
					if (pathWeight + edgeWeight < 1 && pathEdges.size() > 1) {
						IloLinearIntExpr sum = cplex.linearIntExpr();
						for (Edge pathEdge : pathEdges) {
							sum.addTerm(1, edgeVarMap.get(pathEdge));
						}
						sum.addTerm(1, edgeVarMap.get(e));

						this.add(cplex.le(sum, pathEdges.size()));

//						break;
					}
				}
			}
		}

	}
}
