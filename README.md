# SushrutOpenEvent
The Open Event Client for Sushrut

The client is a generic app that has two parts:
a) A standard configuration file, that sets the details of the app (e.g. color scheme, logo of event, link to JSON app data)
b) The app itself
This app uses the json api provided by a server maintained 

## Development Setup
Before you begin, you should already have the Android Studio SDK downloaded and set up correctly. You can find a guide on how to do this here: [Setting up Android Studio](http://developer.android.com/sdk/installing/index.html?pkg=studio)

### Configuring the app

**Configuring Server and Web-App Urls**
- In this file you will see several constant variables that allow you to set useful properties of the app, these include:
	* API_VERSION: Server API version. (Example: "v1")
	* EVENT_ID: ID of the event to load from server. (Example: 1)
	* BASE_URL: The base URL of the server. (Example: "http://springboard.championswimmer.in")
	* SPEAKERS: The file-name of the speakers page of the web app. Added to the end of WEB_APP_URL_BASIC to form full link. (Example: "speakers")
	* TRACKS: The file-name of the tracks page of the web app. Added to the end of WEB_APP_URL_BASIC to form full link. (Example: "tracks")
	* SESSIONS: The file-name of the sessions page of the web app. Added to the end of WEB_APP_URL_BASIC to form full link. (Example: "sessions")
	* MAP: The file-name of the map page of the web app. Added to the end of WEB_APP_URL_BASIC to form full link. (Example: "map")

## Data retrieval
- The orga-server provides the data which is stored in its backend database in a json format.
- The app on startup picks up data from a json file in it's assets folder if the version number of data is -1 which happens when there is no internet connection
- If there is a valid internet connection, the data download starts from the server.
- Also there is a check on the version of data already there in the app's database. If data is stale then only it is downloaded.
- If database is empty then firstly json file in assets is accessed but if internet is available , latest data is downloaded.

##License
This project is currently licensed under the GNU General Public License v3. A copy of LICENSE.md should be present along with the source code. To obtain the software under a different license, please contact Sushrut Health.