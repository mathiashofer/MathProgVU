package at.mhofer.mathprog.kmst.model.factory;

import at.mhofer.mathprog.kmst.data.Instance;
import at.mhofer.mathprog.kmst.model.CECModel;
import at.mhofer.mathprog.kmst.model.Model;

public class CECModelFactory implements ModelFactory{

	@Override
	public Model create(Instance instance, int k) {
		return new CECModel(instance, k);
	}

}
