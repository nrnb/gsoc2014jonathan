/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

/*global $, svg:false */

var anatomogramModule = (function ($) {

	"use strict";

	function setHilighting(path, color, opacity) {
		path.style.fill = color;
		path.style.fillOpacity = opacity;
	}

	function togglePathColor(path, evtType, name, select, sex) {

		"use strict";
		if (evtType === undefined) {
			if (select === name)setHilighting(path, "purple", 0.5);			
			else setHilighting(path, "gray", 0.5);
		} else if (evtType === 'mouseenter' || evtType === 'mouseover') {
			setHilighting(path, "blue", 0.7);
		} else if (evtType === 'click'){
			window.location.href =
				"http://test2.wikipathways.org/index.php/Special:TissueAnalyzer?select="+name+"&button=submit&sex="+sex+"";
		} else if (evtType === "mouseout"){
			if (select === name)setHilighting(path, "purple", 0.5);			
			else setHilighting(path, "gray", 0.5);
		} else {
			setHilighting(path, "gray", 0.5);
		}
	}

	function toggleOrganismPartColor(svg, svgPath, select, sex, evt) {

		"use strict";

		var element = svg.getElementById(svgPath.id);
		var evtType = (typeof evt === 'undefined') ? evt : evt.type;

		if (element !== null) {
			if (element.nodeName === 'g') {
				$.each(element.getElementsByTagName('path'), function () {
					togglePathColor(this, evtType, svgPath.name, select, sex);
				});
			} else {				
				togglePathColor(element, evtType, svgPath.name, select, sex);
			}
		}
	}


	function initMouseOverBindingForSvgPath(svgElement, svgPath, select, sex) {

		var headerDiv = $('#heatmap-table th').has("div[data-svg-path-id='" + svgPath.id + "']");

		svgElement.removeEventListener("mouseover", function () {
			togglePathColor(svgElement, "mouseover", svgPath.name, "", sex);
		}, false);

		svgElement.removeEventListener("click", function () {
			togglePathColor(svgElement, "click", svgPath.name, "", sex);
		}, false);

		svgElement.removeEventListener("mouseout", function () {
			togglePathColor(svgElement, "mouseout", svgPath.name, select, sex);
		}, false);

		svgElement.addEventListener("mouseover", function () {
			togglePathColor(svgElement, "mouseover", svgPath.name, "", sex);
		}, false);

		svgElement.addEventListener("click", function () {
			togglePathColor(svgElement, "click", svgPath.name, "", sex);
		}, false);

		svgElement.addEventListener("mouseout", function () {
			togglePathColor(svgElement, "mouseout", svgPath.name, select, sex);
		}, false);
	}


	function initBindingsForAnatomogramPaths(svg, svgPath, select, sex) {
		var svgElement = svg.getElementById(svgPath.id);

		if (svgElement !== null) {
			if (svgElement.nodeName === 'g') {
				$.each(svgElement.getElementsByTagName('path'), function () {
					initMouseOverBindingForSvgPath(this, svgPath, select, sex);
				});
			} else {
				initMouseOverBindingForSvgPath(svgElement, svgPath, select, sex);
			}
		}
	}

	function scaleAnatomogram(svg) {
		var elementById = svg.getElementById('group_all');
		// this is in case anatomogram is hidden
		if (typeof elementById !== 'undefined') {
			elementById.setAttribute('transform', 'scale(2.6)');
		}
	}

	function zoomAnatomogram(svg,coef) {
        var elementById = svg.getElementById('group_all');
        $('svg').svgPan('group_all'); 
        // this is in case anatomogram is hidden
        if (typeof elementById !== 'undefined') {
			var transform = elementById.getAttribute('transform');
			var value = transform.slice(transform.indexOf("(")+1, transform.indexOf(")"));
			var newValue = parseFloat(value)+coef;
			if (newValue<0) newValue=0;
			transform = transform.replace(value,newValue.toString());
            elementById.setAttribute('transform', transform);
        }
    }

	function initAnatomogramBindings(svg, allSvgPathIds, select, sex) {
		$.each(allSvgPathIds, function () {
			initBindingsForAnatomogramPaths(svg, this, select, sex);
		});
	}

	function highlightAllOrganismParts(svg, allSvgPathIds, select, sex) {
		$.each(allSvgPathIds, function () {
			toggleOrganismPartColor(svg, this, select, sex);
		});
	}

	//load anatomogram from given location and display given organism parts
	function loadAnatomogram(location, allSvgPathIds, select, sex) {
		var svg = $('#anatomogramBody').svg('get');
		
		svg.load(location, {
			onLoad:function(){
				scaleAnatomogram(svg);
				highlightAllOrganismParts(svg, allSvgPathIds, select, sex);
				initAnatomogramBindings(svg, allSvgPathIds, select, sex);			
			}
		});
		return svg;
	}

	function init(allSvgPathIds, fileNameMale, fileNameFemale, select, sex) {
		
		//init svg
		$('#anatomogramBody').svg();
		if (sex==="male"){
			var svg = loadAnatomogram(fileNameMale, allSvgPathIds, select, sex);}
		else {
			var svg = loadAnatomogram(fileNameFemale, allSvgPathIds, select, sex);
		}

		if (fileNameMale !== fileNameFemale) {
			//switch sex toggle button
			$("#sex-toggle-image").button().toggle(
					function () {
						$(this).attr("src", "/wpi/data/female_selected.png");
						loadAnatomogram(fileNameFemale, allSvgPathIds, select, "female");
					},
					function () {
						$(this).attr("src","/wpi/data/male_selected.png");
						loadAnatomogram(fileNameMale, allSvgPathIds, select, "male");
					}
			).tooltip();
		} else {
			$("#sex-toggle").hide();
		}
		$("#enable").button().click(
				function () {
					$(this).attr("src", "/wpi/data/enable.png");
					$('svg').svgPan('group_all');
				}
		)
	}

	return {
		init: init
	};

}(jQuery));