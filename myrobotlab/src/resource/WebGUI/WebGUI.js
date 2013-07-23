// FIXME - must be an easy way to include full gui & debug 

//-------WebGUIGUI begin---------

function WebGUIGUI(name) {
	ServiceGUI.call(this, name); // call super constructor.
}

WebGUIGUI.prototype = Object.create(ServiceGUI.prototype);
WebGUIGUI.prototype.constructor = WebGUIGUI;

// --- callbacks begin ---
WebGUIGUI.prototype.getState = function(data) {
	n = this.name;
	$("#"+n+"-httpPort").val(data[0].httpPort);
	$("#"+n+"-wsPort").val(data[0].wsPort);
};
//--- callbacks end ---

// --- overrides begin ---
WebGUIGUI.prototype.attachGUI = function() {
	this.subscribe("publishState", "getState");
	// broadcast the initial state
	this.send("broadcastState");
};

WebGUIGUI.prototype.detachGUI = function() {
	this.unsubscribe("publishState", "getState");
	// broadcast the initial state
};

WebGUIGUI.prototype.init = function() {
	//alert(this.key("display"));
	//$("#"+this.name+"-startWebGUI").button().click(WebGUIGUI.prototype.startWebGUI);
	//$("#"+this.name+"-setInterval").button().click(WebGUIGUI.prototype.setInterval);

	// FIXME - simply do with css classes
	/*
	$("#"+this.name+"-httpPort").button()
	  .css({
	          'font' : 'inherit',
	    'text-align' : 'left',
	       'outline' : 'none',
	        'cursor' : 'text'
	  });
	
	$("#"+this.name+"-wsPort").button()
	  .css({
	          'font' : 'inherit',
	    'text-align' : 'left',
	       'outline' : 'none',
	        'cursor' : 'text'
	  });
	*/
	
	$("#"+this.name+"-setPorts").button().click(WebGUIGUI.prototype.setPorts);

};
// --- overrides end ---

// --- gui events begin ---
WebGUIGUI.prototype.setPorts = function(event) {

	var gui = guiMap[this.name];
	var httpPort = $("#"+this.name+"-httpPort").val();
	var wsPort	 = $("#"+this.name+"-wsPort").val();
	alert(httpPort);
	// FIXME - implement
	//gui.send()
	

	gui.send("broadcastState");
}

//--- gui events end ---


WebGUIGUI.prototype.getPanel = function() {
	return "<div>"
			+ "	http port       <input class='text ui-widget-content ui-corner-all' id='"+this.name+"-httpPort' type='text' value=''></input>"
			+ "	web socket port <input class='text ui-widget-content ui-corner-all' id='"+this.name+"-wsPort' type='text' value=''></input>"
			+ "	<input id='"+this.name+"-setPorts' type='button' value='set'/>"
			+ "</div>";
}
