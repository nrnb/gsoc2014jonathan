<?php
require_once ("Article.php");
require_once ("ImagePage.php");
require_once ("wpi/DataSources.php");

/*
 * Generates info text for pathway page - datanode > generate table of datanodes
 */

// ## DEFINE EXTENSION
// Define a setup function
$wgExtensionFunctions [] = 'wfPathwayInfo';
// Add a hook to initialise the magic word
$wgHooks ['LanguageGetMagic'] [] = 'wfPathwayInfo_Magic';
function wfPathwayInfo() {
	global $wgParser;
	$wgParser->setFunctionHook ( 'pathwayInfo', 'getPathwayInfoText' );
}
function getPathwayInfoText(&$parser, $pathway, $type) {
	global $wgRequest;
	$parser->disableCache ();
	try {
		$pathway = Pathway::newFromTitle ( $pathway );
		$oldid = $wgRequest->getval ( 'oldid' );
		if ($oldid) {
			$pathway->setActiveRevision ( $oldid );
		}
		$info = new PathwayInfo ( $parser, $pathway );
		if (method_exists ( $info, $type )) {
			return $info->$type ();
		} else {
			throw new Exception ( "method PathwayInfo->$type doesn't exist" );
		}
	} catch ( Exception $e ) {
		return "Error: $e";
	}
}
function wfPathwayInfo_Magic(&$magicWords, $langCode) {
	$magicWords ['pathwayInfo'] = array (
			0,
			'pathwayInfo' 
	);
	return true;
}

require_once ("Pathways/Pathway.php");
/* Need autoloader here */
class PathwayInfo extends PathwayData {
	private $parser;
	private $gpml;
	private $name;
	function __construct($parser, $pathway) {
		parent::__construct ( $pathway );
		$this->parser = $parser;
		$this->name = $pathway->getName () ;
		$this->gpml = $pathway->getFileName ( $pathway->getName () );
	}
	
	/**
	 * Creates a table of all datanodes and their info
	 */
	function datanodes() {
		$table = '<table class="wikitable sortable" id="dnTable">';
		$table .= '<tbody><th>Name2<th>Type<th>Database reference<th>Comment';
		// style="border:1px #AAA solid;margin:1em 1em 0;background:#F9F9F9"
		$all = $this->getElements ( 'DataNode' );
		
		// Check for uniqueness, based on textlabel and xref
		$nodes = array ();
		foreach ( $all as $elm ) {
			$key = $elm ['TextLabel'];
			$key .= $elm->Xref ['ID'];
			$key .= $elm->Xref ['Database'];
			$nodes [( string ) $key] = $elm;
		}
		
		// Create collapse button
		$nrShow = 5;
		$button = "";
		$nrNodes = count ( $nodes );
		if (count ( $nodes ) > $nrShow) {
			$expand = "<b>View all...</b>";
			$collapse = "<b>View last " . ($nrShow) . "...</b>";
			$button = "<table><td width='51%'> <div onClick='" . 'doToggle("dnTable", this, "' . $expand . '", "' . $collapse . '")' . "' style='cursor:pointer;color:#0000FF'>" . "$expand<td width='45%'></table>";
		}
		// Sort and iterate over all elements
		$species = $this->getOrganism ();
		sort ( $nodes );
		$i = 0;
		foreach ( $nodes as $datanode ) {
			$xref = $datanode->Xref;
			$xid = ( string ) $xref ['ID'];
			$xds = ( string ) $xref ['Database'];
			$link = DataSource::getLinkout ( $xid, $xds );
			$id = trim ( $xref ['ID'] );
			if ($link) {
				$l = new Linker ();
				$link = $l->makeExternalLink ( $link, "$id ({$xref['Database']})" );
			} elseif ($id != '') {
				$link = $id;
				if ($xref ['Database'] != '') {
					$link .= ' (' . $xref ['Database'] . ')';
				}
			}
			
			// Add xref info button
			$html = $link;
			if ($xid && $xds) {
				$html = XrefPanel::getXrefHTML ( $xid, $xds, $datanode ['TextLabel'], $link, $this->getOrganism () );
			}
			
			// Comment Data
			$comment = array ();
			$biopaxRef = array ();
			foreach ( $datanode->children () as $child ) {
				if ($child->getName () == 'Comment') {
					$comment [] = ( string ) $child;
				} elseif ($child->getName () == 'BiopaxRef') {
					$biopaxRef [] = ( string ) $child;
				}
			}
			
			$doShow = $i ++ < $nrShow ? "" : " class='toggleMe'";
			$table .= "<tr$doShow>";
			$table .= '<td>' . $datanode ['TextLabel'];
			$table .= '<td class="path-type">' . $datanode ['Type'];
			$table .= '<td class="path-dbref">' . $html;
			$table .= "<td class='path-comment'>";
			
			$table .= self::displayItem ( $comment );
			// http://developers.pathvisio.org/ticket/800#comment:9
			// $table .= self::displayItem( $biopaxRef );
		}
		$table .= '</tbody></table>';
		return array (
				$button . $table,
				'isHTML' => 1,
				'noparse' => 1 
		);
	}
	protected static function displayItem($item) {
		$ret = "";
		if (count ( $item ) > 1) {
			$ret .= "<ul>";
			foreach ( $item as $c ) {
				$ret .= "<li>$c";
			}
			$ret .= "</ul>";
		} elseif (count ( $item ) == 1) {
			$ret .= $item [0];
		}
		return $ret;
	}
	function interactions() {
		$interactions = $this->getInteractions ();
		foreach ( $interactions as $ia ) {
			$table .= "\n|-\n";
			$table .= "| {$ia->getName()}\n";
			$table .= "|";
			$xrefs = $ia->getPublicationXRefs ( $this );
			if (! $xrefs)
				$xrefs = array ();
			foreach ( $xrefs as $ref ) {
				$attr = $ref->attributes ( 'rdf', true );
				$table .= "<cite>" . $attr ['id'] . "</cite>";
			}
		}
		if ($table) {
			$table = "=== Interactions ===\n{|class='wikitable'\n" . $table . "\n|}";
		} else {
			$table = "";
		}
		return $table;
	}
	function tissueTable() {
		$pieces = explode ( ".", $this->gpml );
		$path_name = str_replace ( " ", '_',$this->name);
		$filename = "wpi/data/TissueAnalyzer/Hs_$pieces[1]_$pieces[0].txt";
		$filename2 = "wpi/data/TissueAnalyzer/".$path_name."_".$pieces[0].".txt";
		$filename = (file_exists ( $filename )) ? $filename : $filename2;
		
		if (file_exists ( $filename )) {
			
			$file = fopen ( $filename, r );
			$nrShow = 5;
			$expand = "<b>View all...</b>";
			$collapse = "<b>View first  $nrShow...</b>";
			$id = "tissue";
			//
			$button = "<table><td width='51%'>
					<div onClick='" . 'doToggle("' . $id . '", this, "' . $expand . '", "' . $collapse . '")' . "' style='cursor:pointer;color:#0000FF'>" . "$expand</div><td width='45%'></table>";
			$tags = "
				<style type='text/css'>
					  .my-scale .scale-title {
					    text-align: left;
					    margin-bottom: 8px;
					    font-weight: bold;
					    font-size: 90%;
					    }
					  .my-scale .color-scale ul {
					    margin: 0;
					    padding: 0;
					    float: left;
					    list-style: none;
					    }
					  .my-scale .color-scale ul li {
					    display: block;
					    float: left;
					    width: 50px;
					    margin-bottom: 6px;
					    text-align: center;
					    font-size: 80%;
					    list-style: none;
					    }
					  .my-scale ul.scale-labels li span {
					    display: block;
					    float: left;
					    height: 15px;
					    width: 50px;
					    }
					</style>
					<br><div class='my-scale'>
					<div class='scale-title'>Gradient color scale</div>
					<div class='color-scale'>
					  <ul class='scale-labels'>
					    <li><span style='background:#8c8cb9;'></span>0 - 3</li>
					    <li><span style='background:#7676c3;'></span>3 - 5 </li>
					    <li><span style='background:#5151d6;'></span>5 - 7</li>
					    <li><span style='background:#3e3edf;'></span>7 - 10 </li>
					    <li><span style='background:#0000FE;'></span> >10 </li>
					  </ul>
					</div>
					</div>
					</div>
				$button				
				<table id='tissue' class='wikitable sortable' >
				<tr class='table-blue-tableheadings'>
				<td class='table-blue-headercell'>Tissue name</td>
				<td colspan=2>Median expression </td>
				<td class='table-blue-headercell'>Mean expression</td>
				<td class='table-blue-headercell'>Active gene measured (%)</td>
				<td class='table-blue-headercell'>Ensembl active gene list ( data base link) </td></tr>";
			
			$url = array ();
			$mean = array ();
			$perc = array ();
			$median = array ();
			$active = array ();
			while ( ! feof ( $file ) ) {
				$line = fgets ( $file );
				if ($line == false)
					break;
				$pieces = explode ( "\t", $line );
				$name = str_replace ( " ", '+', $pieces [0] );
				
				$genes = explode ( ",", $pieces [4] );
				$list_genes = "";
				foreach ( $genes as $gene ) {
					$info = explode ( ' ', $gene );
					if (count ( $info ) > 1) {
						$tmp = $info [2];
						$tmp = trim ( $tmp );
						$list_genes .= ' <a target="_blank"	href=' . $tmp . '>' . $info [1] . '</a> ';
					}
				}
				$href = '<a target="_blank" 
						href="http://test2.wikipathways.org/index.php/Special:TissueAnalyzer?select=' . $name . '&button=submit">' . $pieces [0] . '</a>';
				array_push ( $url, $href );
				array_push ( $median, $pieces [3] );
				array_push ( $mean, $pieces [1] );
				array_push ( $perc, $pieces [2] );
				array_push ( $active, $list_genes );
			}
			
			array_multisort ( $median, SORT_NUMERIC, SORT_DESC, 
			$perc, SORT_NUMERIC, SORT_DESC, 
			$mean, SORT_NUMERIC, SORT_DESC, 
			$url, SORT_STRING, SORT_DESC, 
			$active, SORT_STRING, SORT_DESC );
			
			for($i = 0; $i < count ( $mean ); ++ $i) {
				
				$r = 0;
				$g = 0;
				$b = 0;
				
				if ( $median[$i] < 1.5 ) {
					$r = 170;
					$g = 170;
					$b = 170;
				}
				
				elseif ( $median[$i] > 10) {
					$color = 255;
					$r = 0;
					$g = 0;
					$b = 255;
				}
				else {
					$r = 170 - 2 *($median[$i]-1.5)/(10-1.5) * (255-170);
					$g = 170 - 2 * ($median[$i]-1.5)/(10-1.5) * (255-170);
					$b = 170 + ($median[$i]-1.5)/(10-1.5) * (255-170);
				}
				$rgb = array( $r, $g, $b );
				
				$hex = "#";
				$hex .= str_pad(dechex($rgb[0]), 2, "0", STR_PAD_LEFT);
				$hex .= str_pad(dechex($rgb[1]), 2, "0", STR_PAD_LEFT);
				$hex .= str_pad(dechex($rgb[2]), 2, "0", STR_PAD_LEFT);
				
				if ($i < $nrShow)
					$doShow = '';
				else
					$doShow = 'toggleMe';
				
				$tags .= "<tr class=$doShow>
					<td class='table-blue-contentcell'>$url[$i]</td>
					<td class='table-blue-contentcell' align='center'>$median[$i]</td>
					<td bgcolor='$hex'> </td>
					<td class='table-blue-contentcell' align='center'>$mean[$i]</td>
					<td class='table-blue-contentcell' align='center'>$perc[$i]</td>
					<td class='table-blue-contentcell'>" . $active [$i] . "</td></tr>";
			}			
			$tags .= "</table>";
			fclose ( $file );
		}		
		return array (
				$tags,
				'isHTML' => 1,
				'noparse' => 1 
		);
	}
}
