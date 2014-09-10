Smart-Pet-Light
===============

Automated light that responds to outdoor Lux sensors

 The general theory of operation is as follows:
  	
 		* The cats are kept in a bedroom while the owners are away from the house (at work, errands, etc.)
 		* If it gets dark outside while the owners are away (based on a Lux sensor), turn on one or more lights
 		* Also turn on the lights if the owners are Home, but the cats are still in their room with the door closed
 		* If it gets light enough, turn the lights back off (but only the lights we turned on - leave the rest alone)
 
   That's pretty straight forward. But we also don't want to turn on the lights while the owners are sleeping in the
  	"cat room" (bedroom) with the door closed. So...
 
 		* When the house switches to Night mode, we shut off any lights we turned on and stop automating the lights
 		* In the morning, when someone gets up, the house will change back to Home mode (based on motion sensors
 		  or timers or whatever)
 		* We still don't want to automate the lights until we KNOW they are awake - by monitoring when they open the
 		  door to the bedroom
 
 	This is all still flawed, because one person getting up and opening the door will enable the light show for the day.
 	If they leave the door open - no problem, the lights won't go on. But if they close the door again (e.g. leaving
 	early to catch a plane while the spouse sleeps), well, I still have to figure that one out. But this version at
 	least addresses MOST of the corner cases.
 
