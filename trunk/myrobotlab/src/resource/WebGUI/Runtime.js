// FIXME - must be an easy way to include full gui & debug 

//-------RuntimeGUI begin---------

function RuntimeGUI(name) {
	ServiceGUI.call(this, name); // call super constructor.
}

RuntimeGUI.prototype = Object.create(ServiceGUI.prototype);
RuntimeGUI.prototype.constructor = RuntimeGUI;

// --- callbacks begin ---
RuntimeGUI.prototype.pulse = function(data) {
	$("#"+this.name+"-display").html(data);
};

RuntimeGUI.prototype.getState = function(data) {
	n = this.name;
	$("#"+n+"-display").html(data);
	if (data[0].isRuntimeRunning) {
		$("#"+n+"-startRuntime").button("option", "label", "stop clock");
	} else {
		$("#"+n+"-startRuntime").button("option", "label", "start clock");
	}

	$("#"+n+"-interval").val(data[0].interval);
};
//--- callbacks end ---

// --- Runtime methods begin ---
RuntimeGUI.prototype.getServiceInfo = function(data) {
	n = this.name;
	var gui = guiMap[this.name];
	
	var possibleServices = data[0].serviceData.serviceInfo;
	for (var property in possibleServices) {
		//alert(property);
		var shortName = property.substring(property.lastIndexOf(".")+1);
		
		//$("<ul class='possibleServices'>").appendTo( "#"+this.name+"-display");
		// <li align='left'>
		$("<a href='#' id='"+shortName+"' class='possibleService'><img class='possibleService' src='/resource/" + shortName + ".png' width='24' height='24' align='left'/> " + shortName + "</a>"+
				"<a target='_blank' class='serviceHelp' href='http://myrobotlab.org/service/"+ shortName +"'><img src='/resource/WebGUI/common/help.png'/></a><br/>" ).appendTo( "#"+this.name+"-display");

		//$("</ul>").appendTo( "#"+this.name+"-display");
		//$("#accordion1").accordion("refresh");
	}
	
	// FIXME - this is working on ALL <A HREFS !!!! - should be targeted by class !
	$(function() {
	    $( ".possibleService" )
	      .button().width("300px")
	      .click(function( event ) {
	        event.preventDefault();
	        $("#dialog-form").dialog("open");
	        $("#service-class").val(event.currentTarget.id);
	        $("#dialog-form").attr("title", "create new "+event.currentTarget.id+" service");
	        //alert($("#service-class").value($(this).attr("id")).attr("id"));
	      });
	  });
	
	$(function() {
	    $( ".serviceHelp" )
	      .button().width("48px");
	  });
	
	var name = $( "#name" );
	var serviceClass = $( "#service-class" );
	
	 $( "#dialog-form" ).dialog({
	      autoOpen: false,
	      height: 300,
	      width: 350,
	      modal: true,
	      buttons: {
	        "create service ": function() {
	          var bValid = true;
	 
	          //bValid = bValid && checkLength( name, "username", 3, 16 );
	          gui.send("createAndStart", new Array(name.val(), serviceClass.val()));
	          if ( bValid ) {
	            $( this ).dialog( "close" );
	          }
	        },
	        cancel: function() {
	          $( this ).dialog( "close" );
	        }
	      },
	      close: function() {
	        //allFields.val( "" ).removeClass( "ui-state-error" );
	      }
	    });
	
};
// --- Runtime methods end ---

// --- overrides begin ---
RuntimeGUI.prototype.attachGUI = function() {
	
	this.subscribe("resolveSuccess", "resolveSuccess");
	this.subscribe("resolveError", "resolveError");
	this.subscribe("resolveBegin", "resolveBegin");
	this.subscribe("resolveEnd", "resolveEnd");
	this.subscribe("newArtifactsDownloaded", "newArtifactsDownloaded");

	this.subscribe("registered", "registered");
	this.subscribe("released", "released");
	this.subscribe("failedDependency", "failedDependency");
	this.subscribe("proposedUpdates", "proposedUpdates");

	// get the service info for the bound runtime (not necessarily local)
	this.subscribe("getServiceInfo", "getServiceInfo");

	//myService.send(boundServiceName, "broadcastState");
	// FIXME !!! - flakey - do to subscribe not processing before this meathod? Dunno???
	//this.getPossibleServices("all");
	this.send("getServiceInfo");
};

RuntimeGUI.prototype.detachGUI = function() {
	this.unsubscribe("pulse", "pulse");
	this.unsubscribe("publishState", "getState");
	// broadcast the initial state
};

RuntimeGUI.prototype.init = function() {

};

RuntimeGUI.prototype.registered = function(data) {
	//alert(data);
	// heavy handed but it works
	this.send("getRegistry");
};
// --- overrides end ---

// --- gui events begin ---

//--- gui events end ---


RuntimeGUI.prototype.getPanel = function() {
	return "<div id='"+this.name+"-display'>"
			+ "</div>" +
			"<div id='dialog-form' title='create new service'> " +
			"  <form>" +
			"  <fieldset>" +
			"    <label for='name'>name</label>" +
			"    <input type='hidden' name='service-class' id='service-class' class='text ui-widget-content ui-corner-all' />" +
			"    <input type='text' name='name' id='name' class='text ui-widget-content ui-corner-all' />" +
			"  </fieldset>" +
			"  </form>" +
			"</div>";
}
