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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.bridgedb.IDMapperException;
import org.bridgedb.IDMapperStack;
import org.bridgedb.Xref;
import org.pathvisio.core.util.FileUtils;
import org.pathvisio.data.DataException;
import org.pathvisio.data.IRow;
import org.pathvisio.data.ISample;
import org.pathvisio.desktop.gex.CachedData;
import org.pathvisio.desktop.gex.GexManager;

public class TissueResult {

	private String gene;
	private double expression;

	public TissueResult(String gene, double expression){
		this.gene=gene;
		this.expression=expression;
	}
	public String toString(){
		return gene+" "+String.valueOf(expression);			
	}
	public String getGene() {
		return gene;
	}
	public double getExpression() {
		return expression;
	}
	public void setGene(String gene) {
		this.gene = gene;
	}
	public void setExpression(double expression) {
		this.expression = expression;
	}

	public static void read(GexManager gex, CachedData cache, Set<Xref> setRefs,
			String path, String name, IDMapperStack currentGdb,Map <Xref,String> labelMap){

		Collection<? extends ISample> names = null;
		Map<String,List<TissueResult>> data = 
				new TreeMap<String,List<TissueResult>>();

		try {			
			names = gex.getCurrentGex().getOrderedSamples();
			for ( ISample is : names){
				if ( !is.getName().equals(" Gene Name")){
					data.put(is.getName().trim(),new ArrayList<TissueResult>());
				}
			}
			for (Xref ref : labelMap.keySet()){
				List<? extends IRow> pwData = cache.syncGet(ref);
				if (!pwData.isEmpty()){
					for ( ISample is : names) {
						for (IRow ir : pwData){
							if ( !is.getName().equals(" Gene Name")){
								Double value = 0.0;
								try {
									value = (Double) ir.getSampleData(is);
								} catch (ClassCastException e) {
									System.out.println(ir.getSampleData(is));
									e.getStackTrace();
								}								
								String label = labelMap.get(ref);
								String dd = ir.getXref().getId()+" "+label+" "+ir.getXref().getUrl();
								TissueResult tr = new TissueResult(dd,value);								
								data.get(is.getName().trim()).add(tr);
							}
						}
					}					
				}
			}
		} catch (DataException e) {
			e.printStackTrace();
		} catch (IDMapperException e) {
			e.printStackTrace();
		}
		calcul(data,path,name);
	}
	public static void calcul(Map<String,List<TissueResult>> data, String path, String name){
		PrintWriter pw = null;
		name = FileUtils.removeExtension(name);
		String fileName = path + File.separator + name + ".txt";

		try {
			pw = new PrintWriter(new FileOutputStream(fileName));			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		for(Map.Entry<String, List<TissueResult>> entry : data.entrySet()) {
			int length = entry.getValue().size();
			Double i = 0.0;
			double sum = 0;
			String gene = "";
			String measured = "";
			ArrayList<Double> tmp = new ArrayList<Double>();
			for( TissueResult tr: entry.getValue()){
				if (tr.getExpression() >= (2/ Math.log10(2)) ){
					i++;
					if (gene.equals("")){
						gene += tr.getGene();
					}
					else {
						gene += ","+tr.getGene();
					}
				}
				else{
					if (measured.equals("")){
						measured += tr.getGene();
					}
					else {
						measured += ","+tr.getGene();
					}
				}
				//filtered 				
				sum += tr.getExpression();
				tmp.add(tr.getExpression());
			}

			double median;			
			Collections.sort(tmp);
			int s = tmp.size() ;
			if (s>0){
				if (s% 2 == 0){				
					median = ((double)tmp.get(s/2) + (double)tmp.get( (s/2)-1))/2;
				}
				else
					median = tmp.get(s/2);
				median = Math.round(median*100.0)/100.0;
			}
			else{
				median = 0.0;
			}
			double mean = sum/length;
			mean = Math.round(mean*100.0)/100.0;
			double perc = i/length*100;
			perc = Math.round(perc*100.0)/100.0;
			pw.println(entry.getKey()+"\t"+mean+"\t"+perc+"\t"+median+"\t"+gene+"\t"+measured);
			
			
			new File(path + File.separator + "Tissue").mkdir();
			File tissueFile = new File(path + File.separator + "Tissue" +File.separator  + entry.getKey() +".txt");
			try {
				tissueFile.createNewFile();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
			String everything="";			
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(tissueFile));
				if (br != null) {
					StringBuilder sb = new StringBuilder();
					String line = br.readLine();
					boolean flag = false;
					boolean present = true;
					while (line != null) {
						String[] path_fullName = line.split("\t");
						int index = path_fullName[0].indexOf("WP");
						String path_Name = path_fullName[0].substring(index);
						String[] path_id = path_Name.split("_");
						if (name.contains(path_id[0]) & !name.contains(path_id[1])) {							
//						(line.contains(name)){
							//sb.append(name+"\t"+mean+"\t"+perc+"\t"+median+"\n");
							//sb.append(line+"\n");
//							sb.append(line+" *"+"\n");
							flag = true;
						}
						else if (line.contains(name)) {
							present = false;
							sb.append(line+"\n");
						}
						else {
							sb.append(line+"\n");
						}
						line = br.readLine();					
					}
					if (line == null & (flag | present) ){
						sb.append(name+"\t"+mean+"\t"+perc+"\t"+median+"\t"+i.intValue()+"/"+length+"\n");
					}
					everything = sb.toString();
				} 
			}
			catch (IOException e) {
				e.printStackTrace();
			}			
			try {
				PrintWriter tissueWriter = new PrintWriter(new FileOutputStream(tissueFile));
				tissueWriter.print(everything);
				tissueWriter.close();
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}			
		}
		pw.close();
	}
}
