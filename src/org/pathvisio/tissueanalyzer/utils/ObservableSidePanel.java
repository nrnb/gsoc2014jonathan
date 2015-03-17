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

package org.pathvisio.tissueanalyzer.utils;

import java.util.ArrayList;
import org.pathvisio.tissueanalyzer.gui.TissueWizard;

/**
* Interface to notify the view about the tissues list's change.
* @author Jonathan Melius
* @see TissueWizard
*/ 

public interface ObservableSidePanel {
	public void addObserver(ObserverSidePanel obs);
	public void notifyObservers(ArrayList<String> progress,ArrayList<String> selected);
	public void delOneObserver(ObserverSidePanel obs);
	public void delAllObservers();
}
