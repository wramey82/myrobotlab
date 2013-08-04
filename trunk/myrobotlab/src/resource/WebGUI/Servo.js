function ServoGUI(name) {
	ServiceGUI.call(this, name); // call super constructor.
}

ServoGUI.prototype = Object.create(ServiceGUI.prototype);
ServoGUI.prototype.constructor = ServoGUI;

// --- callbacks begin ---
ServoGUI.prototype.pulse = function(data) {
	$("#"+this.name+"-display").html(data);
};

ServoGUI.prototype.getState = function(data) {
	n = this.name;
	//$("#" + this.name + "-controller").combobox();
	
	$(function() { // TODO REMOVE 
	    $(".servo").slider({ 
	    	max: 255,
	    	slide: function( event, ui ) {

	    		var pwmID = $(this).attr("pwmId");
	    		var gui = guiMap[$(this).attr("name")];
	    		gui.send("moveTo",[ui.value])
	    		$("#"+pwmID+"-slider-value").val(ui.value);
	          }
	    });
	  });
};
//--- callbacks end ---

// --- overrides begin ---
ServoGUI.prototype.attachGUI = function() {
	this.subscribe("publishState", "getState");
	// broadcast the initial state
	this.send("broadcastState");
};

ServoGUI.prototype.detachGUI = function() {
	this.unsubscribe("publishState", "getState");
};

ServoGUI.prototype.init = function() {
	//alert("#"+this.name+"-startServo");
	//$("#"+this.name+"-startServo").button().click(ServoGUI.prototype.startServo);
	//$("#"+this.name+"-setInterval").button().click(ServoGUI.prototype.setInterval);

};
// --- overrides end ---

// --- gui events begin ---
ServoGUI.prototype.attach = function(event) {

}

//--- gui events end ---

ServoGUI.prototype.getPanel = function() {
	return "<div>"
	+ "<div name='"+this.name+"' id='"+this.name+"-controller'/>" + 
			+ "<div class='servo' name='"+this.name+"' pwmId='"+this.name+"' id='"+this.name+"-slider'/>" + 
			"<input class='servo-value text ui-widget-content ui-corner-all slider-value' type='text' value='0' name='"+this.name+"' id='"+this.name+"-slider-value'/>"
			+ "</div>";
}
