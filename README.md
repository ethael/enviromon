# enviromon
Samsung Galaxy S4 (Note 3) Temperature, Humidity, Atmospheric pressure, Light and Noise intensity monitor project

<img src="https://github.com/user-attachments/assets/4990cd7c-2e2f-4527-91b1-8ef01ea43054" alt="Enviromon" width="300">

# Purpose
- 24/7 monitoring of the surrounding environment
- control other smart home devices like bulbs, fans, heating, aircondition based on gathered information
- gather long term atmospheric/weather data
- setup a "device per room" model for full potential

# Features
- gather temperature, humidity, atmospheric pressure, light & noise intensity, charging status and battery level from the device sensors
- show temperature and humidity on the display
- if there is a `OUTDOOR` device available, show also outdoor temperature and humidity on every other device
- upload gathered sensor data to server every minute (and obtain/update outdoor data if available)
- turn the display automatically on and off based on surrounding noise levels (threshold configurable in the `config.properties` file)
- change the display brightness based on surrounding light conditions (threshold configurable in the `config.properties` file)
- show the debug metadata in the bottom of the display if the debug mode is on (configurable in the `config.properties` file)
- automatic (per device) temperature correction ratio, if your device's sensors have some kind of constant shift (configurable in the `config.properties` file)

# Possible extension ideas (not implemented)
- code a weather forecast algorithm (detect sudden drops in atmospheric pressure)
- show more data on the display (todays calendar, weather forecast...)
- use it to directly control non smart devices (because the device has both bluetooth and even IRDA capabilities!)
- add a button (or reimplement the app's noise detector for endless prompt listening) and use it as your voice command input and voice response output
- utilize the phone camera as a simple security cam triggered by motion, sudden light drop or noise level change
- use it as a night light triggered by noise
- notify about power outtages, humidity spikes (has someone forgot to open the window after shower?), temperature drops (has someone forgot to close windows/door in the winter?)

# Project structure
- android app
- python server (`server` directory)
- stl file for 3D printing the frame (standalone or wall mount) and battery replacement kit (`print` directory)

# Prerequisites
- Samsung Galaxy S4 (I9505) or Samsung Galaxy Note 3 (N9005) device
- Flash the device with Lineage OS ROM. Version 18.1 (Android 11) works best for me

# Installation
- client: 
  - open the project in android studio
  - modify the server IP and port in the `config.properties`
  - run the project with connected device in adb debug mode enabled (or build the apk and copy on device to install)
- server: 
  - copy the content of the `server` project directory on your private server
  - install `python` and `mariadb`
  - open the `install.sh` and check if the credentials for mariadb work for you and you can also adjust credentials for this project
  - run `install.sh`
  - in `server.py`, modify the `execution_dir` variable to match the directory where you have copied the content of the `server` project folder
  - run `server.py`
  - run `test.sh` to test if it works
  - direct run of `server.py` is only for preview/debugging purposes. in production, you should run it as a WSGI service that will be registered in your OS init system (like systemd, openrc or runit)
    - WSGI and init system setup is out of scope of this howto, please ask your favourite LLM to generate it for you

# Addons
- print the table stand and/or wall mount version or use the measures from the stl file and design your own one
- if you are a bit paranoid and you don't want to keep the device with battery always on AC (I did for years in the past and the rule of thumb is to replace battery once a year), and you don't mind loosing your "free UPS" and "power outage detector", then use my 3D design for battery bypass and follow these instructions:
  - cut the micro usb end from the cable
  - remove about 10cm of spaghetti tubing and alu foil
  - keep only red (+) and black (-) wires. the other two are data cables, you don't need those 
  - print the 3D model
  - run the wires through the holes in the model
  - solder it to the position, or make a copper lump and fix it in the position with a heat gun
  - **CAUTION!** red wire is in the first position (on the edge), if you mix it, you may destroy the device
  - **CAUTION!** this will bypass battery. charger will provide its 5V directly to the device. original battery provides only 3.8V. this means that you should step down the voltage using diodes or other electrical components. I don't care and run it on 5V, because I am lazy and I trust the Koreans, that their components can handle a bit more stress.
  - **CAUTION!** the 3D design for Galaxy Note 3 is sufficiently accurate in the area of electrical pins. Galaxy S4 is a bit more faded ;) I couldn't be bothered. But still working though. If you care, you can practice your tinkercad skills a bit and fix it.

# Device configuration for best results
- dark amoled mode: on
- wake on plug: off
- tap to sleep: off
- screen saver: off
- vibrations: off
- display always on: on
- diode off
- disable battery optimisation
- allow app permissions manually
- allow unlimited data usage
- enable app pinning and pin the enviromon app

# Gallery

Plain phone

<img src="https://github.com/user-attachments/assets/ca97d2a9-dc97-4fd3-971c-ff53ec922255" alt="Plain phone" width="400">

Phone in the frame

<img src="https://github.com/user-attachments/assets/aa064d38-84d9-4875-8b3b-1a5963a9e222" alt="Phone in the frame" width="400">

Original battery (don't forget the plus and minus if you are about to bypass the battery)

<img src="https://github.com/user-attachments/assets/8c651c0e-97bc-4469-9727-a447f0f469b2" alt="Original battery" width="400">

Battery bypass in the assembly process

<img src="https://github.com/user-attachments/assets/bad6a181-6392-4129-969f-e681678f535b" alt="Battery bypass" width="400">

The sweet result

<img src="https://github.com/user-attachments/assets/dcf689d9-092d-473c-aa72-59a9a4407da4" alt="The sweet result" width="400">



