definition(
    name: "Mode Change Action Control",
    namespace: "djdizzyd",
    author: "Bryan Copeland",
    description: "Stuff to do on mode changes",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "")

preferences {
	page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: " ", install: true, uninstall: true) {
		section {
			input "thisMode", "mode", title: "Select Mode", submitOnChange: true, required: true
			if(thisMode) app.updateLabel("ModeActions-${thisMode}")
			input "switchOffDevs", "capability.switch", title: "Select Switches to Turn Off", submitOnChange: true, multiple: true
			input "switchOnDevs", "capability.switch", title: "Select Switches to Turn On", submitOnChange:true, multiple: true
			input "dimmerDevs", "capability.switchLevel", title: "Dimmers to set", submitOnChange: true, multiple: true
			if (dimmerDevs) { 
				input "dimmerValue", "number", title: "Dimmer Level", submitOnChange: true, required: true
			}
			input "volumeDevs", "capability.audioVolume", title: "Audio Volume To Adjust", submitOnChange: true, multiple: true
			if (volumeDevs) {
				input "volumeValue", "number", title: "Audio Level", submitOnChange: true, required: true
			}
			input "enableAlexaSwitch", "bool", title: "Enable Alexa Virtual Switch", submitOnChange: true
			input "testButton", "button", title: "Test Actions"
		}
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(location, "mode", modeHandler)
	if (enableAlexaSwitch) { 
		def alexaSwitchDev = getChildDevice("alexaModeControl_${app.id}") 
		if (!alexaSwitchDev) { alexaSwitchDev = addChildDevice("hubitat", "Virtual Switch", "alexaModeControl_${app.id}", null, [label: "alexaModeControl-${thisMode}", name: "aleaModeControl-${thisName}"]) } 
		subscribe(alexaSwitchDev, "switch.on",  alexaHandler)
	} else {
		def alexaSwitchDev = getChildDevice("alexaModeControl_${app.id}") 
		if (alexaSwitchDev) { deleteChildDevice(alexaSwitchDev.getDeviceNetworkId()) }
		subscribe(alexaSwitchDev, "switch.on",  alexaHandler)
	}
	
}

def appButtonHandler(btn) {
	switch(btn) {
		case "testButton": doActions()
			break
	}
}

def doActions() {
	if (switchOffDevs) {
		switchOffDevs.each{ it.off() }
	}
	if (switchOnDevs) {
		switchOnDevs.each { it.on() }
	}
	if (dimmerDevs) {
		dimmerDevs.each { it.setLevel(dimmerValue) }
	}
	if (volumeDevs) {
		volumeDevs.each { it.setVolume(volumeValue) }
	}
}

def alexaSwitch(value) { 
	def alexaSwitchDev = getChildDevice("alexaModeControl_${app.id}")
	if (alexaSwitchDev.currentValue("switch") != value) { 
		unsubscribe(alexaSwitchDev)
		switch(value) {
			case "on": 
				if (alexaSwitchDev.currentValue("switch")=="off") { alexaSwitchDev.on() }
				break
			case "off":
				alexaSwitchDev.off()
				break
		}
		pause(500)
		subscribe(alexaSwitchDev, "switch.on",  alexaHandler)
	}
}

def alexaHandler(evt) {
	log.info "Got alexa mode trigger"
	location.setMode(thisMode)
}

def modeHandler(evt) {
	log.info "Got mode change: ${evt.value}"
	if (evt.value == thisMode) { 
		doActions()
		if (enableAlexaSwitch) { alexaSwitch("on") }
	} else {
		if (enableAlexaSwitch) { alexaSwitch("off") }
	}
}