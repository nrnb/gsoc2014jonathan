// TissueAnalyzer plugin for Pathvisio
// Copyright 2014 BiGCaT Bioinformatics
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.pathvisio.tissueanalyzer.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.pathvisio.core.util.FileUtils;

/**
* Choose the way to query the Expression Atlas human tissues datasets.
* @author Jonathan Melius
*/

public class TissueControler {
	private AbstractAnalyser analyser;

	public TissueControler(AbstractAnalyser tissueanalyser){
		this.analyser = tissueanalyser;
	}

	public void control(String exp, String txtOutput){
		String outFile = null;
		File f = new File(txtOutput);
		try {
			f.getCanonicalPath();
			f=FileUtils.replaceExtension(f, "pgex");
			outFile = f.getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
		analyser.settings(exp,outFile);
	}

	public void queryTissuesList(String experiment){		
		analyser.queryTissuesList(experiment);		
	}

	public void query(ArrayList<String> selectedTissues, String cutoff){
		analyser.setCutoff(cutoff);
		analyser.setSelectedTissues(selectedTissues);
		analyser.queryExperiment();
	}
	
	public void setAbstractAnalyser(AbstractAnalyser analyser){
		this.analyser=analyser;
	}
}
