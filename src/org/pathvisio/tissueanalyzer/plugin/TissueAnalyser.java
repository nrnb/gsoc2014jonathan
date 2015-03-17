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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bridgedb.DataSource;
import org.pathvisio.gexplugin.ImportInformation;

/**
 * Query Expression Atlas database by the web service.
 * Retrieve the list of tissues and the dataset.
 * @see AbstractAnalyser
 * @author Jonathan Melius
 */
public class TissueAnalyser extends AbstractAnalyser {

	public TissueAnalyser () {
		super();
	}

	public TissueAnalyser (ImportInformation importInformation,String experiment) {
		this.importInformation=importInformation;
		this.experiment=experiment;
	}

	public void queryExperiment(){
		String organQuery="";
		for (String organ : selectedTissues){
			organ = organ.replaceAll("\\s", "+");
			organQuery += "&queryFactorValues="+organ;
		}	
		String tDir = System.getProperty("java.io.tmpdir");
		File filename = null;
		try {
			filename = File.createTempFile(tDir+"AtlasQuery", ".tmp");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		filename.deleteOnExit();
		URL url = null;
		ArrayList<String> result = new ArrayList<String>();
		try {
			url = new URL("http://www.ebi.ac.uk/gxa/experiments/"+
					experiment+".tsv?"+
					"accessKey=&serializedFilterFactors="+
					"&queryFactorType=ORGANISM_PART" +
					"&rootContext=&heatmapMatrixSize=50"+
					"&displayLevels=false"+
					"&displayGeneDistribution=false" +
					"&geneQuery=&exactMatch=true"+ 
					"&_exactMatch=on&_geneSetMatch=on"+
					organQuery+
					"&_queryFactorValues=1" +
					"&specific=true" +
					"&_specific=on" +
					"&cutoff="+cutoff);						



			InputStream is = url.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));


			String line;
			boolean dataRow = false;
			while ((line = br.readLine()) != null )
			{
				StringBuilder tmp = new StringBuilder();
				if (dataRow){
					Pattern p = Pattern.compile("\t");
					Matcher m = p.matcher(line);
					String replace = m.replaceAll("\t0");
					ArrayList<String> data = new ArrayList<String>(Arrays.asList(replace.split("\t")));
					for (int i=2;i<data.size();i++){
						if (data.get(i).matches("[-+]?\\d+(\\.\\d+)?")){
							double logi = Math.log10(Double.parseDouble(data.get(i))+1);
							double log2 = logi / Math.log10(2);
							log2 = Math.round(log2*100.0)/100.0;
							data.set(i, String.valueOf(log2));
						}
						else{
							data.set(i,"0.0");
						}
					}
					for (String s : data)
					{
						tmp.append(s);
						tmp.append("\t");
					}
				}
				else{
					tmp.append(line);
				}
				if ( line.contains(gene_ID) ) {
					dataRow = true;
				}
				//if (tmp!="")result.add(tmp);
				result.add(tmp.toString());
			}
			br.close();
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new FileOutputStream(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		for (String club : result)
			pw.println(club);
		pw.close();
		//		ReadableByteChannel rbc = Channels.newChannel(url.openStream());
		//		FileOutputStream fos = new FileOutputStream(filename);		
		//		fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

		DataSource ds = DataSource.getBySystemCode("En");

		try {
			importInformation.setTxtFile(filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
		importInformation.setFirstDataRow(4);
		importInformation.setFirstHeaderRow(3);
		importInformation.guessSettings();
		importInformation.setDelimiter("\t");
		importInformation.setSyscodeFixed(true);
		importInformation.setDataSource(ds);
		importInformation.setIdColumn(0);
		notifyObservers(importInformation);

		//	} catch (IOException e) {
		//		e.printStackTrace();
		//	}
	}

	/**
	 * update the list of tissues from the experiment
	 */
	public void queryTissuesList(String experiment) {
		ArrayList<String> tissuesList = new ArrayList<String>();
		try { 
			URL url = new URL ("http://www.ebi.ac.uk/gxa/experiments/"
					+ experiment+".tsv?accessKey=&serializedFilterFactors="
					+ "&queryFactorType=ORGANISM_PART&rootContext="
					+ "&heatmapMatrixSize=50"
					+ "&displayLevels=false&displayGeneDistribution=true"
					+ "&geneQuery=&exactMatch=true&_exactMatch=on"
					+ "&_geneSetMatch=on&_queryFactorValues=1"
					+ "&specific=true&_specific=on&cutoff=10000");

			InputStream is = url.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			while ((line = br.readLine()) != null )
			{								
				if ( line.contains(gene_ID) ) {
					tissuesList = new ArrayList<String>(Arrays.asList(line.split("\t")));
					tissuesList.remove(gene_Name);
					tissuesList.remove(gene_ID);
				}				
			}
			br.close();
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		notifyObservers(tissuesList,experiment);
	}

	public void settings(String experiment,String outFile){
		importInformation.setGexName(outFile);
		this.experiment=experiment;
		notifyObservers(importInformation);
	}

	public void querySelect(){
		String organQuery="";
		for (String organ : selectedTissues){
			organ = organ.replaceAll("\\s", "+");
			organQuery += "&queryFactorValues="+organ;
		}	
		String tDir = System.getProperty("java.io.tmpdir");
		File filename = null;
		try {
			filename = File.createTempFile(tDir+"AtlasQuery", ".tmp");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		filename.deleteOnExit();
		ArrayList<String> result = new ArrayList<String>();
		URL url = null;
		try {
			url = new URL("http://www.ebi.ac.uk/gxa/experiments/E-MTAB-513.tsv?"+
					"accessKey=&serializedFilterFactors="+
					"&queryFactorType=ORGANISM_PART" +
					"&rootContext=&heatmapMatrixSize=50"+
					"&displayLevels=false&displayGeneDistribution=false" +
					"&geneQuery=&exactMatch=true&_exactMatch=on&_geneSetMatch=on"+
					organQuery+
					"&_queryFactorValues=1" +
					"&specific=true" +
					"&_specific=on" +
					"&cutoff="+cutoff+"");
		}
		catch (MalformedURLException e1) {
			e1.printStackTrace();
		}			

		try {
			InputStream is = url.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));

			ArrayList<Integer> indexList = new ArrayList<Integer>();
			String line;
			boolean dataRow = false;
			while ((line = br.readLine()) != null )
			{	
				String tmp = "";
				if (dataRow){
					Pattern p = Pattern.compile("\t");
					Matcher m = p.matcher(line);
					String replace = m.replaceAll("\t0");
					ArrayList<String> data = new ArrayList<String>(Arrays.asList(replace.split("\t")));
					for (int index : indexList){
						if (index==0){
							tmp +=data.get(index)+"\t";
						}
						else {
							if (data.get(index).matches("[-+]?\\d+(\\.\\d+)?")){
								double logi = Math.log10(Double.parseDouble(data.get(index))+1);
								double log2 = logi / Math.log10(2);
								log2 = Math.round(log2*100.0)/100.0;
								tmp +=String.valueOf(log2)+"\t";
								data.set(index, String.valueOf(log2));
							}
							else{
								tmp +="0.0"+"\t";
								data.set(index,"0.0");
							}
						}
					}
					//System.out.println("data"+tmp);

				}
				if ( line.contains(gene_ID) ) {
					ArrayList<String> data = new ArrayList<String>(Arrays.asList(line.split("\t")));
					for ( String header : data){
						if ( header.equals(gene_ID) || selectedTissues.contains(header) ) {
							int index = data.lastIndexOf(header);
							indexList.add(index);
							tmp +=header+"\t";
							//System.out.println(index);
						}    					
					}
					dataRow = true;
					//System.out.println(tmp);
				}
				if (tmp!="")result.add(tmp);
			}
			br.close();
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new FileOutputStream(filename));//filename
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		for (String res : result)
			pw.println(res);
		pw.close();

		DataSource ds = DataSource.getBySystemCode("En");

		try {
			importInformation.setTxtFile(filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
		importInformation.setFirstDataRow(1);
		importInformation.setFirstHeaderRow(0);
		importInformation.guessSettings();
		importInformation.setDelimiter("\t");
		importInformation.setSyscodeFixed(true);
		importInformation.setDataSource(ds);
		importInformation.setIdColumn(0);
		notifyObservers(importInformation);
	}

}
