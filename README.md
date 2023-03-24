# DreamGrid
This is a simple launcher for Oculus Quest and Pico VR headsets supporting official/SideQuest apps and games.

## Compile the launcher
* Open **Launcher** in Android Studio
* Ensure USB debugging is enabled on your headset
* Hit play button in Android Studio

## Convert icons from SideQuest
* Collect JSON data from SideQuest (could be done using Chrome Debug Console)
* Join JSONs together
* Format the JSONs to be human readable
* Place the JSON into txt file into `SideIcons/localdb.json`
* Create `SideIcons/out/` folder
* Open **SideIcons** in Eclipse
* Run the project
