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

import java.util.ArrayList;

import org.pathvisio.gexplugin.ImportInformation;
import org.pathvisio.tissueanalyzer.utils.ObservableTissue;
import org.pathvisio.tissueanalyzer.utils.ObserverTissue;

/**
* Abstract class to query Expression Atlas human tissues dataset.
* Retrieve the list of tissues and the dataset.
* @author Jonathan Melius
* @see ObservableTissue
*/
public abstract class AbstractAnalyser implements ObservableTissue {

	protected ImportInformation importInformation;
	protected String cutoff;
	protected ArrayList<String> selectedTissues;
	protected String experiment;
	protected final String gene_ID = "Gene ID";
	protected final String gene_Name = "Gene Name";
	protected ArrayList<ObserverTissue> observers;

	public AbstractAnalyser() {
		importInformation = new ImportInformation();
		observers = new ArrayList<ObserverTissue>();
	}

	public abstract void queryExperiment();

	/**
	 * returns the list of tissues from the experiment
	 */
	public abstract void queryTissuesList(String experiment);

	public abstract void settings(String experiment, String outFile);

	public ArrayList<String> getSelectedTissues() {
		return selectedTissues;
	}

	public void setSelectedTissues(ArrayList<String> selectedTissues) {
		this.selectedTissues = selectedTissues;
	}

	public String getCutoff() {
		return cutoff;
	}

	public void setCutoff(String cutoff) {
		this.cutoff = cutoff;
	}

	@Override
	public void addObserver(ObserverTissue obs) {
		// TODO Auto-generated method stub
		observers.add(obs);
	}

	@Override
	public void notifyObservers(ArrayList<String> listOfTissues, String exp) {
		// TODO Auto-generated method stub
		for (ObserverTissue obs : observers){
			obs.update(listOfTissues,exp);
		}
	}

	@Override
	public void notifyObservers(ImportInformation importInformation) {
		// TODO Auto-generated method stub
		for (ObserverTissue obs : observers){
			obs.update(importInformation);
		}
	}

	@Override
	public void delOneObserver(ObserverTissue obs) {
		// TODO Auto-generated method stub
		observers.remove(obs);
	}

	@Override
	public void delAllObservers() {
		// TODO Auto-generated method stub
		
	}

	public void querySelect() {
		// TODO Auto-generated method stub
		
	}

}