// FIXME - must be an easy way to include full gui & debug 

//-------ArduinoGUI begin---------

function ArduinoGUI(name) {
	ServiceGUI.call(this, name); // call super constructor.
}

ArduinoGUI.prototype = Object.create(ServiceGUI.prototype);
ArduinoGUI.prototype.constructor = ArduinoGUI;

// --- callbacks begin ---
ArduinoGUI.prototype.pulse = function(data) {
	$("#"+this.name+"-display").html(data);
};

ArduinoGUI.prototype.getState = function(data) {
	n = this.name;
	
	var arduino = data[0];	
	var boards = data[0].targetsTable.arduino.boards;
	var boardType = arduino.boardType;
	var ports = arduino.serialDeviceNames;
	var connected = arduino.connected;
	
	// ports begin ---
	$("#"+this.name+"-ports")
    .find('option')
    .remove()
    .end();
	
	$("#"+this.name+"-ports").append("<option value=''></option>");
	for (var i = 0; i < ports.length; i++) {
		$("#"+this.name+"-ports").append("<option value='"+ports[i]+"' "+((arduino.portName == ports[i])?"selected":"") +">"+ports[i]+"</option>");
	}
	
	if (connected) {
		$("#"+this.name+"-connected").attr("src","/resource/WebGUI/common/button-green.png");
	} else {
		$("#"+this.name+"-connected").attr("src","/resource/WebGUI/common/button-red.png");
	}
	// ports end ---
	// boards begin ---
	$("#"+this.name+"-boards")
    .find('option')
    .remove()
    .end();
	
	$("#"+this.name+"-boards").append("<option value=''></option>");	
	for (var board in boards) {
		$("#"+this.name+"-boards").append("<option value='"+board+"' "+((boardType == board)?"selected":"") +">"+boards[board].name+"</option>");
	}
	// boards end ---
	// pin list begin ---	
	var pinList = arduino.pinList;
	$("#"+this.name+"-pinList").empty();
	var analogPinCount = 0;
	var pinLabel = "";
	for (var i = 2; i < pinList.length; i++) {
		console.log(pinList[i]);
		var pin = pinList[i];
		
		if (pin.type == 1){
			pinLabel = "D" + i;
		} else if (pin.type == 2) {
			pinLabel = "PWM" + i;
		} else if (pin.type == 3) {
			pinLabel = "A" + analogPinCount;
			++analogPinCount;
		} else {
			pinLabel = "?" + i;
		}		
		
		// FIXME - will need this.name in all identifier fields to be unique across 
		// multiple arduinos...
		$("#"+this.name+"-pinList").append(
				"<div class='pin-set'><img id='"+this.name+"-pin-"+i+"-led' src='/resource/WebGUI/common/button-small-"+ ((pin.value == 1)?"green":"grey") +".png' />" +
				"<input type='checkbox' class='pin' name='"+this.name+"' id='"+i+"' "+ ((pin.value == 1)?"checked":"") +"/><label for='"+i+"'>" +pinLabel+ "</label>" +
				
				"<input type='button' class='pinmode' value='out' pidId='"+i+"' name='"+this.name+"' id='"+i+"-test' />" +
				 
				((pin.type == 2)?"<div class='pwm' name='"+this.name+"' pwmId='"+i+"' id='"+i+"-slider'/><input class='pwm-value text ui-widget-content ui-corner-all slider-value' type='text' value='0' name='"+this.name+"' id='"+i+"-slider-value'/>":"") +
						"</div>" +
						"</div>");
	}
	
	$(function() {
	    $(".pin-set").buttonset().width("300px");
	    $(".pin").click(function( event ) {
	       // event.preventDefault();
	        var value = (this.checked)?1:0;
		    var gui = guiMap[this.name];
		    gui.send("digitalWrite", [parseInt(this.id), value]);
		    $("#" + this.name+"-pin-"+this.id+"-led").attr("src",((this.checked)?"/resource/WebGUI/common/button-small-green.png":"/resource/WebGUI/common/button-small-grey.png"));
	    	
	      });
	  });
	
	$(function() {
	    $(".pwm").slider({ 
	    	max: 255,
	    	slide: function( event, ui ) {

	    		var pwmID = $(this).attr("pwmId");
	    		var gui = guiMap[$(this).attr("name")];
	    		gui.send("analogWrite",[parseInt(pwmID), ui.value])
	    		$("#"+pwmID+"-slider-value").val(ui.value);
	          }
	    });
	  });
	
    // FIXME - get data from registry - 
	// parent class "getState" should update registry !!!
	// need to get pinList to determine pin type
	// analog vs digital polling
    $(".pinmode").click(function(){
    	var gui = guiMap[$(this).attr("name")];
    	//var pin = pinList[]
    	if ($(this).val() == "in"){
    		$(this).val("out");
    		gui.send("pinMode",[1]);
    	} else {
    		$(this).val("in");
    		gui.send("pinMode",[0]);
    		//gui.send((pin.type)?"d",[0]);
    	}
   });
	// pin list end ---	
	var oscope = document.getElementById('oscope');
    var context = oscope.getContext('2d');
    
    context.beginPath();
    context.moveTo(100, 150);
    context.lineTo(450, 50);
    //context.lineWidth = 10;
    context.strokeStyle = '#ff0000';
    context.stroke();
    
};

ArduinoGUI.prototype.publishPin = function(data) {
	alert(data[0]);
}

//--- callbacks end ---

// --- overrides begin ---
ArduinoGUI.prototype.attachGUI = function() {
	this.subscribe("getTargetsTable", "getTargetsTable");
	this.subscribe("publishState", "getState");
	this.subscribe("publishPin", "publishPin");
	// broadcast the initial state
	
	//this.send("getTargetsTable");
	this.send("broadcastState");
};

ArduinoGUI.prototype.detachGUI = function() {
	// broadcast the initial state
};

ArduinoGUI.prototype.init = function() {
	
	//$(document).ready(function(){
	
	var gui = guiMap[this.name];
	$("#"+this.name+"-ports").change(function() {
		//var port = $("#"+this.name+"-ports").find(":selected").text();
		  gui.connect();
	});
	
	// load oscope background begin ---
	var background = document.getElementById('oscope');
    var context = background.getContext('2d');
    var imageObj = new Image();
    
    imageObj.onload = function() {
      context.drawImage(imageObj, 0, 20);
    };
    
    imageObj.src = '/resource/WebGUI/Arduino/grid.png';
	// load oscope background end ---
    
	//}

	//$(".digital-pin").button();
};
// --- overrides end ---

// --- gui events begin ---
/*
ArduinoGUI.prototype.getTargetsTable = function(data) {
	alert(Object.keys(data));
}
 position: absolute;
    top: 40px;
    left: 65px;
*/
ArduinoGUI.prototype.connect = function() {
	var port = $("#"+this.name+"-ports").find(":selected").text();
	this.send("connect", new Array(port, 57600, 8, 1, 0));
}
//--- gui events end ---


ArduinoGUI.prototype.getPanel = function() {
	return "<div class='ui-widget'>" +
	"  <label>Port: </label>" +
	"  <select class='text ui-widget-content ui-corner-all' id='"+this.name+"-ports' name='"+this.name+"'>" +
	"    <option value=''>Select one...</option>" +
	"  </select>"
	+ 
	"    <img id='"+this.name+"-connected' name='"+this.name+"' src='/resource/WebGUI/common/button-red.png' />" 
	+

	"  <label>Board: </label>" +
	"  <select class='text ui-widget-content ui-corner-all' id='"+this.name+"-boards' name='"+this.name+"'>" +
	"    <option value=''>Select one...</option>" +
	"  </select><br/>" +

	// pin list
	"<div id='"+this.name+"-pinList'></div>" +
	
	// oscope
	"<div id='oscope-container'>" +
	"<canvas class='oscope' id='oscope' width='600' height='400'></canvas>" +
	"</div>" + 
	//" <img src='/resource/WebGUI/Arduino/arduino.duemilanove.200.pins.png' />" +
	"</div>"  
	;
}
