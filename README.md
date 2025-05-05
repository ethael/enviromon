# enviromon
Samsung Galaxy S4 (Note 3) Temperature, Humidity, Atmospheric pressure, Light and Noise intensity monitor project

# Project structure
- android app
- python server (`server` directory)
- stl file for 3D printing the frame (standalone or wall mount) and battery replacement kit (`print` directory)

# Prerequisites
- Samsung Galaxy S4 or Samsung Galaxy Note 3 device
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
- print the table stand and/or wall mount version or use the measures from stl file and design your own
- if you are a bit paranoid (like me) and you don't want to keep the device with battery always on AC, and you don't mind loosing your "free UPS", then use my 3D design for battery bypass.
  - manual
    - cut the micro usb end from the cable
    - remove about 10cm of spaghetti tubing and alu foil
    - keep only red (+) and black (-) wires. the other two are data cables, you don't need them 
    - print the 3D model
    - run the wires through the holes in the model
    - solder it to the position, or make a copper lump and fix it in the position with heat gun
    - CAUTION! red wire is in the first position (the edge position), if you mix it, you may destroy the device
    - CAUTION! this will bypass battery. charger will provide its 5V directly to the device. original battery provides only 3.8V. this means that you should step down the voltage using diodes or other electrical components. I didn't care and run it on 5V, because I am lazy and I trust the Koreans, that their components can handle a bit more stress.


# Gallery
