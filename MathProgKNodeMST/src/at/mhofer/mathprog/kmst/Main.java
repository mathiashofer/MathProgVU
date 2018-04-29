package at.mhofer.mathprog.kmst;

import java.io.File;

import at.mhofer.mathprog.kmst.data.Instance;
import at.mhofer.mathprog.kmst.model.MCFModel;
import at.mhofer.mathprog.kmst.model.Model;

public class Main {

	public static void main(String[] args) throws Exception {
		Instance g01 = Instance.fromFile(new File("data/g01.dat"));
		Model scf = new MCFModel(g01, 5);
		scf.build();
		scf.solve();
	}
	
}
