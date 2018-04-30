package at.mhofer.mathprog.kmst;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import at.mhofer.mathprog.kmst.data.Instance;
import at.mhofer.mathprog.kmst.model.Model;
import at.mhofer.mathprog.kmst.model.factory.MCFModelFactory;
import at.mhofer.mathprog.kmst.model.factory.ModelFactory;
import at.mhofer.mathprog.kmst.model.factory.SCFModelFactory;
import ilog.concert.IloException;

public class Main {

	public static void main(String[] args) throws Exception {
		runAll();
	}

	private static void runAll() throws FileNotFoundException, IloException {
		ModelFactory[] factories = { new SCFModelFactory(), new MCFModelFactory() };
		String[] outDir = { "scf", "mcf" };
		String[] files = { "data/g01.dat", "data/g02.dat", "data/g03.dat", "data/g04.dat", "data/g05.dat",
				"data/g06.dat", "data/g07.dat", "data/g08.dat" };
		int[][] ks = { { 2, 5 }, { 4, 10 }, { 10, 25 }, { 14, 35 }, { 20, 50 }, { 40, 100 }, { 60, 150 }, { 80, 200 } };

		for (int j = 0; j < factories.length; j++) {
			ModelFactory f = factories[j];
			for (int i = 0; i < files.length; i++) {
				String file = files[i];
				System.out.println(file + "...");
				for (int k : ks[i]) {
					Instance instance = Instance.fromFile(new File(file));
					String outFile = outDir[j] + "/" + file.split("\\/")[1].split("\\.")[0] + "_k" + k + ".out";
					OutputStream out = new PrintStream(new FileOutputStream(outFile));
					Model model = f.create(instance, k);
					model.build();
					model.setOut(out);
					model.solve();
				}
			}
		}
	}

}
