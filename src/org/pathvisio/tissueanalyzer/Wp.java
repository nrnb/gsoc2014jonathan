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

package org.pathvisio.tissueanalyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bridgedb.BridgeDb;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.IDMapperStack;
import org.bridgedb.Xref;
import org.bridgedb.rdb.construct.DBConnector;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.util.FileUtils;
import org.pathvisio.data.DataException;
import org.pathvisio.desktop.data.DBConnDerby;
import org.pathvisio.desktop.gex.CachedData;
import org.pathvisio.desktop.gex.GexManager;
import org.pathvisio.desktop.gex.SimpleGex;
import org.pathvisio.tissueanalyzer.plugin.TissueResult;

/**
 * 
 * Main method that is used to calculate
 * tissue pathway statistics on WikiPathways
 * 
 * @author Jonathan Melius
 * 
 */
public class Wp implements Comparable<Object>{

	private String path_id;
	private Double mean;

	public Wp(String path_id, Double mean){
		this.path_id= path_id;
		this.mean=mean;
	}

	public String getPath_id() {
		return path_id;
	}
	public Double getMean() {
		return mean;
	}

	@Override
	public int compareTo(Object obj) {
		if (! (obj instanceof Wp)) {  
			throw new ClassCastException(  
					"compared object must be instance of DateAndName");  
		}  
		return this.getMean().compareTo(((Wp) obj).getMean());  
	}
	public String toString(){
		return getPath_id()+"\t"+getMean();
	}

	public static void main(String[] args){

		File gpmlDir = new File (args[0]);
		File pgex = new File(args[1]);		
		IDMapper mapper=null;
		try {
			Class.forName("org.bridgedb.rdb.IDMapperRdb");
			mapper = BridgeDb.connect("idmapper-pgdb:"+args[2]);
		} catch (IDMapperException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		String path = args[3];
		
		List<File> fileList = FileUtils.getFiles(gpmlDir, "gpml", false);
		
		
		
		for (File gpml : fileList){
			String name = gpml.getName();
			if (name.startsWith("WP")){
				name = args[4]+"_"+FileUtils.removeExtension(name);
			}
			tissueAnalyser(gpml, pgex, path, mapper, name);
		}

		//		List<List<Wp>> tot = new ArrayList<List<Wp>>();
		//
		//		File txtDir= new File (args[4]);
		//		List<File> fileList = FileUtils.getFiles(txtDir, "txt", false);
		//		for (File txt : fileList){
		//			List<Wp> top = topTen(txt);
		//			tot.add(top);
		//		}
		//		matching(tot);
	}

	public static void tissueAnalyser(File gpml, File pgex, String path, IDMapper mapper, String name){
		GexManager gex = new GexManager();
		DBConnector connector = new DBConnDerby();
		Pathway p = new Pathway();
		Map <Xref,String> labelMap = new HashMap<Xref,String>();
		
		CachedData cache = null;
		Set<Xref> setRefs = null;
		connector.setDbType(DBConnector.TYPE_GEX);

		IDMapperStack currentGdb = new IDMapperStack();
		currentGdb.addIDMapper(mapper);

		try {
			SimpleGex simple = new SimpleGex (pgex.getAbsolutePath(), false, connector);
			gex.setCurrentGex(simple);
			p.readFromXml(gpml, true);
			for (PathwayElement e : p.getDataObjects()){
				labelMap.put(e.getXref(), e.getTextLabel());
			}
			cache = gex.getCachedData();		
			List<Xref> refs = p.getDataNodeXrefs();			
			setRefs = new HashSet<Xref>(refs);
			cache.setMapper(mapper);
			cache.preSeed(setRefs);			
		} catch (DataException  e1) {
			e1.printStackTrace();
		} catch (ConverterException e) {
			e.printStackTrace();
		}
		TissueResult.read(gex, cache, setRefs, path, name, currentGdb,labelMap);
	}

	public static List<Wp> topTen(File txt){
		BufferedReader br = null ;
		List<Wp> subby = null ;
		List<Wp> list = new ArrayList<Wp>();
		try {
			br = new BufferedReader(new FileReader(txt));
			if (br != null) {
				String line = br.readLine();
				while ( line!=null){
					String[] splitty = line.split("\t");
					list.add(new Wp(splitty[0],Double.parseDouble(splitty[3])));
					line = br.readLine();
				}
			}
			Collections.sort(list,Collections.reverseOrder());
			subby = list.subList(0, 9);
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return subby;
	}
	public static void matching(List<List<Wp>>  li){
		HashSet<String> setty = new HashSet<String>();
		for(List<Wp>  lw : li){
			for(Wp wp : lw){
				int i = 0;
				for(List<Wp>  lw2 : li){
					for(Wp wp2 : lw2){
						if ( wp.getPath_id().equals(wp2.getPath_id()) ){
							i += 1;
						}
					}
				}
				if (i>10){
					setty.add(wp.getPath_id()+"\t"+i);
				}
			}

		}
//		for(String ss : setty)
//			System.out.println(ss);
	}
}
