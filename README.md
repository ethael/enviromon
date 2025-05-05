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
- batt


# Gallery
