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
	$("#"+n+"-display").html(data);
	if (data[0].isServoRunning) {
		$("#"+n+"-startServo").button("option", "label", "stop clock");
	} else {
		$("#"+n+"-startServo").button("option", "label", "start clock");
	}

	$("#"+n+"-interval").val(data[0].interval);
};
//--- callbacks end ---

// --- overrides begin ---
ServoGUI.prototype.attachGUI = function() {
	this.subscribe("pulse", "pulse");
	this.subscribe("publishState", "getState");
	// broadcast the initial state
	this.send("broadcastState");
};

ServoGUI.prototype.detachGUI = function() {
	this.unsubscribe("pulse", "pulse");
	this.unsubscribe("publishState", "getState");
	// broadcast the initial state
};

ServoGUI.prototype.init = function() {
	//alert("#"+this.name+"-startServo");
	$("#"+this.name+"-startServo").button().click(ServoGUI.prototype.startServo);
	$("#"+this.name+"-setInterval").button().click(ServoGUI.prototype.setInterval);

};
// --- overrides end ---

// --- gui events begin ---
ServoGUI.prototype.startServo = function(event) {

	//alert(this.key("startServo"));
	startServo = $("#"+this.name+"-startServo");
	//alert(startServo.attr("name"));
	clockGUI = guiMap[this.name];
	if (startServo.val() == "start clock") {
		startServo.val("stop clock");
		clockGUI.send("startServo", null); // FIXME null shouldn't be required
	} else {
		startServo.val("start clock");
		clockGUI.send("stopServo", null);
	}

	clockGUI.send("broadcastState");
}

ServoGUI.prototype.setInterval = function() {
	clockGUI = guiMap[this.name];
	clockGUI.send("setInterval", [ parseInt($("#"+this.name+"-interval") .val()) ]);
	clockGUI.send("broadcastState");
}
//--- gui events end ---


ServoGUI.prototype.getPanel = function() {
	return "<div>"
			+ "	<div id='"+this.name+"-display'>clock wtf?</div>"
			+ "	<input id='"+this.name+"-startServo' type='button' name='"+this.name+"' value='start clock'/>"
			+ "	<input id='"+this.name+"-setInterval' type='button' name='"+this.name+"' value='set interval'/>"
			+ "	interval <input id='"+this.name+"-interval' type='text' name='"+this.name+"' value='1000'></input>ms"
			+ "</div>";
}
