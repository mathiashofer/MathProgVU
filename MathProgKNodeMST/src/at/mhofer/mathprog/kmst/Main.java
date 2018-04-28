package at.mhofer.mathprog.kmst;

import java.io.File;

import at.mhofer.mathprog.kmst.data.Instance;
import at.mhofer.mathprog.kmst.model.Model;
import at.mhofer.mathprog.kmst.model.SCFModel;

public class Main {

	public static void main(String[] args) throws Exception {
		Instance g01 = Instance.fromFile(new File("data/g06.dat"));
		Model scf = new SCFModel(g01, 40);
		scf.build();
		scf.solve();
	}
	
}
