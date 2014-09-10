/**
 *  Smart Pet Light
 *
 *  Copyright 2014 Barry A. Burke
 *
 *  Updates:
 *		2014-09-04	Update light status on any event (Lux, Door, or Mode change)
 *		2014-09-10	Cleaned up the times when the lights are "automated" & added Theory of Operation below
 *
 *
 *	The general theory of operation is as follows:
 * 	
 *		* The cats are kept in a bedroom while the owners are away from the house (at work, errands, etc.)
 *		* If it gets dark outside while the owners are away (based on a Lux sensor), turn on one or more lights
 *		* Also turn on the lights if the owners are Home, but the cats are still in their room with the door closed
 *		* If it gets light enough, turn the lights back off (but only the lights we turned on - leave the rest alone)
 *
 *  That's pretty straight forward. But we also don't want to turn on the lights while the owners are sleeping in the
 * 	"cat room" (bedroom) with the door closed. So...
 *
 *		* When the house switches to Night mode, we shut off any lights we turned on and stop automating the lights
 *		* In the morning, when someone gets up, the house will change back to Home mode (based on motion sensors
 *		  or timers or whatever)
 *		* We still don't want to automate the lights until we KNOW they are awake - by monitoring when they open the
 *		  door to the bedroom
 *
 *	This is all still flawed, because one person getting up and opening the door will enable the light show for the day.
 *	If they leave the door open - no problem, the lights won't go on. But if they close the door again (e.g. leaving
 *	early to catch a plane while the spouse sleeps), well, I still have to figure that one out. But this version at
 *	least addresses MOST of the corner cases.
 *
 *********************
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Smart Pet Lights",
    namespace: "Convenience",
    author: "Barry A. Burke",
    description: "Turn on a light if it gets dark outside and everyone is Away except the pets. Turn it off if it gets light again while everyone is still away. Cats can see in the dark, so they don't really need this. But the wife insists :o)",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance@2x.png"
)


preferences {
	section("When it gets dark") {
		input "luxSensor", "capability.illuminanceMeasurement", title: "At which Lux sensor?", multi: false, required: true
        input "luxLevel", "number", title: "Darkness Lux level?", default: "400"
	}
    section("And mode is \"Away\" except these pets are home") {
    	input "daCatz", "capability.presenceSensor", title: "Pet presence sensor?", multi: false, required: true
    }
    section("OR, mode is \"Home\" and this door is closed") {
    	input "catDoor", "capability.contactSensor", title: "Door to pet room?", multi: false, required: true
    }
    section("Turn on") {
    	input "lights", "capability.switch", title: "Which switches?", multi: true, required: true
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
//    unschedule()
	initialize()
}

def initialize() {
	log.debug "Initializing - subscribing to $luxSensor"
    
	subscribe(luxSensor, "illuminance", eHandler)
    subscribe(location, eHandler)
	subscribe(catDoor, "contact", doorHandler)
    
    state.lastStatus = "unknown"
    state.offLights = []
    state.lightShow = (location.mode == "Night")? false : true
}

def eHandler(evt) {

	log.debug "$evt.name: $evt.value"
    
    checkLights()
}

// We only want to do the light show when everyone but the cats are Away, OR while the people who are Home aren't
// sleeping. To effect the latter, we stop playing with the lights when the house goes to sleep (Night mode), and we
// don't start again until somebody opens the bedroom door in the morning.
//
def doorHandler(evt) {
	if (evt.value == "open") {
    	if (location.mode == "Home") { 	// we'd better be in Home mode, or a ghost opened the door :)
			state.lightShow = true		// somebody is up, let today's light show begin!
        }
    }   	
}

def checkLights() {
	log.debug "checkLights - pets: ${daCatz.currentPresence}, lastStatus: $state.lastStatus, mode: $location.mode, door: ${catDoor.currentValue('contact')}"
    
    if (daCatz.currentPresence == "present") { 		// Only when the cats are at home
    												// And everyone else is away, OR they're Home and the door is closed
    
    	if ((location.mode=="Away") || ((location.mode=="Home") && (catDoor.latestValue("contact") == "closed"))) {
 
			def lastStatus = state.lastStatus
			if ((lastStatus != "off") && (luxSensor.latestValue("illuminance").integerValue > luxLevel)) {
            	if (state.offLights) {
					state.offLights*.off()			// only turn off lights that we turned on
                    state.offLights = []
                }
				state.lastStatus = "off"
			}
			else if ((lastStatus != "on") && (luxSensor.latestValue("illuminance").integerValue <= luxLevel)) {
            	state.offLights = lights.findAll { it?.currentSwitch == "off" } // Only turn on those lights that are off
        		if (state.offLights) {
                	if (state.lightShow) {
        				state.offSLights*.on()		// only turn on the lights that are currently off
                    }
        		}	
				state.lastStatus = "on"
            }
		}
        else if (location.mode == "Night") {	// Shut off the lights if the house is going to sleep
            if (state.offLights) {
				state.offLights*.off()			// only turn off the lights that we turned on
                state.offLights = []
            }
            state.lastStatus = "off"
            state.lightShow = false				// light show is done for today
        }
	}
}
