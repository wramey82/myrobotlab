// FIXME - must be an easy way to include full gui & debug 

//-------PythonGUI begin---------

function PythonGUI(name) {
	ServiceGUI.call(this, name); // call super constructor.
	this.editor = ""; // FIXME - null undefined ? 
}

PythonGUI.prototype = Object.create(ServiceGUI.prototype);
PythonGUI.prototype.constructor = PythonGUI;

// --- callbacks begin ---
PythonGUI.prototype.getState = function(data) {
	n = this.name;
	// TODO - get current script
};

PythonGUI.prototype.getScript = function(data) {
	this.editor.setValue(data[0]);
	// TODO - get current script
};

//--- callbacks end ---

// --- overrides begin ---
PythonGUI.prototype.attachGUI = function() {
	//this.subscribe("publishState", "getState"); - trying discreet
	
	subscribe("getScript", "getScript");
	subscribe("finishedExecutingScript", "finishedExecutingScript");
	subscribe("publishStdOut", "getStdOut");
	subscribe("appendScript", "appendScript");
	subscribe("startRecording", "startRecording");

	// broadcast the initial state
	//this.send("broadcastState"); - trying discreet
	this.send("getScript");
};

PythonGUI.prototype.detachGUI = function() {
	this.unsubscribe("publishState", "getState");
};

PythonGUI.prototype.init = function() {	
	
	// WTF - this conflicts - but when I remove it ..
	// everything still appears to work !!
	//var $ = document.getElementById.bind(document); - so JQuery is not happy about 'tis
	var dom = require("ace/lib/dom");

	//add command to all new editor instaces
	require("ace/commands/default_commands").commands.push({
	    name: "Toggle Fullscreen",
	    bindKey: "F11",
	    exec: function(editor) {
	        dom.toggleCssClass(document.body, "fullScreen")
	        dom.toggleCssClass(editor.container, "fullScreen")
	        editor.resize()
	    }
	}, {
	    name: "add",
	    bindKey: "Shift-Return",
	    exec: add
	})

	// create first editor
	//var editor = ace.edit("editor");
	this.editor = ace.edit("editor");
	var theme = "ace/theme/twilight";
	this.editor.setTheme(theme);
	this.editor.session.setMode("ace/mode/python");


	var count = 1;
	function add() {
	    var oldEl = this.editor.container;
	    var pad = document.createElement("div");
	    pad.style.padding = "40px";
	    oldEl.parentNode.insertBefore(pad, oldEl.nextSibling);

	    var el = document.createElement("div")
	    oldEl.parentNode.insertBefore(el, pad.nextSibling);

	    count++
	    this.editor = ace.edit(el)
	    this.editor.setTheme(theme)
	    this.editor.session.setMode("ace/mode/javascript")

	    this.editor.setValue([
	        "this is editor number: ", count, "\n",
	        "using theme \"", theme, "\"\n",
	        ":)"
	    ].join(""), -1)

	    scroll()
	}

	function scroll(speed) {
	    var top = this.editor.container.getBoundingClientRect().top
	    speed = speed || 10
	    if (top > 60 && speed < 500) {
	        if (speed > top - speed - 50)
	            speed = top - speed - 50
	        else
	            setTimeout(scroll, 10, speed + 10)
	        window.scrollBy(0, speed)
	    }
	}

	setTimeout(function(){ window.scrollTo(0,0) }, 10);
	
	$(function() {
	    //$(".python-menu").buttonset().width("300px");
		$(".python-menu").buttonset();
	    $("#run").button().click(function( event ) {
	    	var name = event.currentTarget.name;	    	
	       // event.preventDefault();
		    var gui = guiMap[name];
		    gui.send("exec",[gui.editor.getValue()]);
		   // gui.send("exec", [parseInt(this.id), value]);	
	      });
	  });
	
	//alert(editor.getValue());
};
// --- overrides end ---

// --- gui events begin ---
PythonGUI.prototype.setPorts = function(event) {

	gui.send("broadcastState");
}

//--- gui events end ---


PythonGUI.prototype.getPanel = function() {
	return "<div class='python-menu'>" +
	"<input type='button' id='run' name='"+this.name+"' value='run' /> " +
	"</div>" +
	"<pre id='editor'>print 'One Software To Rule Them All !!!' \n" +
	"</pre>" +
	"<div class='scrollmargin'>" +
	"    <div style='padding:20px'>" +
	"        press F11 to switch to fullscreen mode" +
	"    </div>" +
	"    <span onclick='add()' class='large-button' title='Shift+Enter'>+</span>" +
	"</div>" +
	"" +
	"<script src='/resource/WebGUI/common/ace/src-min/ace.js' type='text/javascript' charset='utf-8'></script>"
	;
}
