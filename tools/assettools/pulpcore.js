// PulpCore 0.10

// Global methods accessed via LiveConnect

function pulpcore_getCookie(name) {
	name = name + "=";
	
	var i;
	if (document.cookie.substring(0, name.length) == name) {
		i = name.length;
	}
	else {
		i = document.cookie.indexOf('; ' + name);
		if (i == -1) {
			return null;
		}
		i += name.length + 2;
	}
	
	var endIndex = document.cookie.indexOf('; ', i);
	if (endIndex == -1) {
		endIndex = document.cookie.length;
	}
	
	return unescape(document.cookie.substring(i, endIndex));
}

function pulpcore_setCookie(name, value, expireDate, path, domain, secure) {
	var expires = new Date();
	
	if (expireDate === null) {
		// Expires in 90 days
		expires.setTime(expires.getTime() + (24 * 60 * 60 * 1000) * 90);
	}
	else {
		expires.setTime(expireDate);
	}
	
	document.cookie = 
		name + "=" + escape(value) +
		"; expires=" + expires.toGMTString() +
		((path) ? "; path=" + path : "") +
		((domain) ? "; domain=" + domain : "") +
		((secure) ? "; secure" : "");
}

function pulpcore_deleteCookie(name, path, domain) {
	document.cookie = name + "=" + 
		((path) ? "; path=" + path : "") +
		((domain) ? "; domain=" + domain : "") +
		"; expires=Thu, 01-Jan-70 00:00:01 GMT";
}

function pulpcore_getBrowserName() {
	return BrowserDetect.browser;
}

function pulpcore_getBrowserVersion() {
	return BrowserDetect.version;
}

function pulpcore_appletLoaded() {
	pulpCoreObject.hideSplash();
	setTimeout("pulpCoreObject.showObject();", 10);
}

// Internal objects

function PulpCoreObject() {
	
	function getCodeBase() {
		var codeBase = document.URL;
		if (codeBase.length <= 7 || codeBase.substr(0, 7) != "http://") {
			return "";
		}
		if (codeBase.charAt(codeBase.length - 1) != '/') {
			var index = codeBase.lastIndexOf('/');
			// Don't include the http://
			if (index > 7) {
				codeBase = codeBase.substring(0, index + 1);
			}
			else {
				codeBase += '/';
			}
		}
		return codeBase;
	}
	
	// Private fields
	
	var appletHTML = "";
	var code    = (window.pulpcore_class === undefined) ? "pulpcore.platform.applet.CoreApplet.class" : window.pulpcore_class;
	var width   = (window.pulpcore_width === undefined) ? 640 : window.pulpcore_width;
	var height  = (window.pulpcore_height === undefined) ? 480 : window.pulpcore_height;
	var splash  = (window.pulpcore_splash === undefined) ? "splash.gif" : window.pulpcore_splash;
	var archive = (window.pulpcore_archive === undefined) ? "project.jar" : window.pulpcore_archive;
	var bgcolor = (window.pulpcore_bgcolor === undefined) ? "#000000" : window.pulpcore_bgcolor;
	var fgcolor = (window.pulpcore_fgcolor === undefined) ? "#aaaaaa" : window.pulpcore_fgcolor;
	var scene   = (window.pulpcore_scene === undefined) ? "" : window.pulpcore_scene;
	var assets  = (window.pulpcore_assets === undefined) ? "" : window.pulpcore_assets;
	var params  = (window.pulpcore_params === undefined) ? { } : window.pulpcore_params;
	var codebase = getCodeBase();
	var appletInserted = false;

	BrowserDetect.init();	
	
	// Public methods
	
	this.write = function() {
		document.write(getObjectHTML());
	};
	
	this.hideSplash = function() {
		var splash = document.getElementById('pulpcore_splash');
		splash.style.display = "none";
		splash.style.visibility = "hidden";
	};
	
	this.showObject = function() {
		var gameContainer = document.getElementById('pulpcore_game');
		gameContainer.style.display = "block";
		gameContainer.style.visibility = "visible";
		if (BrowserDetect.browser == "Explorer") {
			gameContainer.style.position = "static";
		}
	};
	
	this.splashLoaded = function(splash) {
		// Prevent this call from occuring again
		// (IE will continue to call onLoad() if the splash loops.)
		if (splash !== null) {
			splash.onload = "";
		}
		pulpCoreObject.insertApplet();
	};
	
	this.insertApplet = function() {
		if (appletInserted) {
			return;
		}
		var gameContainer = document.getElementById('pulpcore_game');
		if (gameContainer === null) {
			setTimeout("pulpCoreObject.insertApplet();", 500);
		}
		else {
			appletInserted = true;
			gameContainer.innerHTML = appletHTML;
			setTimeout("pulpcore_appletLoaded();", navigator.javaEnabled() ? 15000 : 10);
		}
	};
		
	// Private methods
	
	function getObjectHTML() {
		// For a list of design decisions for this method, see object-tag.txt
		appletHTML = "";
		var splashHTML;
		
		// Object tag parameters
		var objectParams =
			'  <param name="code" value="' + code + '" />\n' +
			'  <param name="archive" value="' + archive + '" />\n' +
			'  <param name="boxbgcolor" value="' + bgcolor + '" />\n' +
			'  <param name="boxfgcolor" value="' + fgcolor + '" />\n' +
			'  <param name="boxmessage" value="" />\n';
		if (codebase.length > 0) {
			objectParams += '  <param name="codebase" value="' + codebase + '" />\n';
		}
		if (scene.length > 0) {
			objectParams += '  <param name="scene" value="' + scene + '" />\n';
		}		
		if (assets.length > 0) {
			objectParams += '  <param name="assets" value="' + assets + '" />\n';
		}
		for (var i in params) {
			objectParams += '  <param name="' + i + '" value="' + params[i] + '" />\n';
		}
		objectParams +=	
			'  <param name="mayscript" value="true" />\n' +
			'  <param name="scriptable" value="true" />\n' +
			'  <p style="text-align: center">To play,\n' + 
			'  <a href="http://www.java.com/">install Java now</a>.</p>\n';	
		
		// Create the Object tag. 
		if (BrowserDetect.browser == "Explorer") {
			if (BrowserDetect.version <= 6 && parent.frames.length > 0) {
				// On IE6, if the site is externally framed, LiveConnect will not work.
				// However, IE can use onfocus to emulate the appletLoaded() behavior
				appletHTML = 
				'<object id="pulpcore_object"\n' + 
				'  classid="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93"\n' +
				'  codebase="http://java.sun.com/update/1.6.0/jinstall-6-windows-i586.cab#Version=1,4,0,0"\n' +
				'  onfocus="pulpcore_appletLoaded();"\n' +
				'  width="' + width + '" height="' + height + '">\n' + 
				objectParams +
				'</object>';
			}
			else {
				appletHTML = 
				'<object id="pulpcore_object"\n' + 
				'  classid="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93"\n' +
				'  codebase="http://java.sun.com/update/1.6.0/jinstall-6-windows-i586.cab#Version=1,4,0,0"\n' +
				'  width="' + width + '" height="' + height + '">\n' + 
				objectParams +
				'</object>';
			}
			// Explorer has special code for centering the splash. Also, the game is
			// positioned far left to avoid flicker when the applet is displayed.
			splashHTML =
				'<div id="pulpcore_splash"\n' + 
				'  style="width: ' + width + 'px; height: ' + height + 'px; position: relative; overflow: hidden; text-align: center">\n' +
				'  <div style="position: absolute; top: 50%; left: 50%;">\n' +
				'    <img alt="Loading..." src="' + splash + '"\n' + 
				'    onload="pulpCoreObject.splashLoaded(this)"\n' + 
				'    style="position: relative; top: -50%; left: -50%;" />\n' +
				'  </div>\n' +
				'</div><div id="pulpcore_game" style="position: relative; left: -2000px;"></div>\n';
		}
		else {
			if (BrowserDetect.OS == "Windows" &&
				BrowserDetect.browser == "Safari" && 
				BrowserDetect.version >= "522.11") 
			{
				// Known versions: 522.11 and 522.12
				// Safari 3 beta on Windows doesn't recognize the archive param when
				// the <object> tag is used. For now, use the <applet> tag.
				// LiveConnect also does not work.
				appletHTML =
				'<applet id="pulpcore_object"\n' + 
				'  codebase="' + codebase + '"\n' +
				'  code="' + code + '"\n' +
				'  archive="' + archive + '"\n' +
				'  width="' + width + '" height="' + height + '" mayscript="true">\n' + 
				objectParams +
				'</applet>';
			}
			else {
				// Firefox, Safari, Opera, Mozilla, etc.
				// Note: the minimum version string is ignored?
				appletHTML =
				'<object id="pulpcore_object"\n' + 
				'  classid="java:' + code + '"\n' +
				'  type="application/x-java-applet;version=1.4"\n' + 
				'  width="' + width + '" height="' + height + '">\n' + 
				objectParams +
				'</object>';
			}
			splashHTML =
				'<div id="pulpcore_splash"\n' + 
				'  style="width: ' + width + 'px; height: ' + height + 'px; text-align: center; display: table-cell; vertical-align: middle">\n' +
				'  <img alt="Loading..." src="' + splash + '"\n' +
				'  onload="pulpCoreObject.splashLoaded(this)" />\n' + 
				'</div>\n' +
				'<div id="pulpcore_game" style="visibility: hidden"></div>\n';
		}
		
		// In case splash.onLoad() is never called
		setTimeout("pulpCoreObject.insertApplet();", 1000);
		
		return '<div style="margin: auto; overflow: hidden; text-align: left; width: ' + width + 'px; height: ' + height + 'px; background: ' + bgcolor + '">\n' +
			splashHTML +
			'</div>\n';
	}
}

// Browser detection script from Peter-Paul Koch at QuirksMode
// http://www.quirksmode.org/js/detect.html
// As of March 11, 2007
var BrowserDetect = {
	init: function () {
		this.browser = this.searchString(this.dataBrowser) || "An unknown browser";
		this.version = this.searchVersion(navigator.userAgent) || 
			this.searchVersion(navigator.appVersion) ||
			"an unknown version";
		this.OS = this.searchString(this.dataOS) || "an unknown OS";
	},
	searchString: function (data) {
		for (var i=0;i<data.length;i++)	{
			var dataString = data[i].string;
			var dataProp = data[i].prop;
			this.versionSearchString = data[i].versionSearch || data[i].identity;
			if (dataString) {
				if (dataString.indexOf(data[i].subString) != -1) {
					return data[i].identity;
				}
			}
			else if (dataProp) {
				return data[i].identity;
			}
		}
	},
	searchVersion: function (dataString) {
		var index = dataString.indexOf(this.versionSearchString);
		if (index == -1) {
			return;
		}
		return parseFloat(dataString.substring(index+this.versionSearchString.length+1));
	},
	dataBrowser: [
		{ 	string: navigator.userAgent,
			subString: "OmniWeb",
			versionSearch: "OmniWeb/",
			identity: "OmniWeb"
		},
		{
			string: navigator.vendor,
			subString: "Apple",
			identity: "Safari"
		},
		{
			prop: window.opera,
			identity: "Opera"
		},
		{
			string: navigator.vendor,
			subString: "iCab",
			identity: "iCab"
		},
		{
			string: navigator.vendor,
			subString: "KDE",
			identity: "Konqueror"
		},
		{
			string: navigator.userAgent,
			subString: "Firefox",
			identity: "Firefox"
		},
		{
			string: navigator.vendor,
			subString: "Camino",
			identity: "Camino"
		},
		{		// for newer Netscapes (6+)
			string: navigator.userAgent,
			subString: "Netscape",
			identity: "Netscape"
		},
		{
			string: navigator.userAgent,
			subString: "MSIE",
			identity: "Explorer",
			versionSearch: "MSIE"
		},
		{
			string: navigator.userAgent,
			subString: "Gecko",
			identity: "Mozilla",
			versionSearch: "rv"
		},
		{ 		// for older Netscapes (4-)
			string: navigator.userAgent,
			subString: "Mozilla",
			identity: "Netscape",
			versionSearch: "Mozilla"
		}
	],
	dataOS : [
		{
			string: navigator.platform,
			subString: "Win",
			identity: "Windows"
		},
		{
			string: navigator.platform,
			subString: "Mac",
			identity: "Mac"
		},
		{
			string: navigator.platform,
			subString: "Linux",
			identity: "Linux"
		}
	]

};

var pulpCoreObject = new PulpCoreObject();
pulpCoreObject.write();

