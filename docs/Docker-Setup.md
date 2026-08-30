Directions here to use a docker container to play Forge:

# FORGE-DESKTOP-SNAPSHOT

Pull docker image.
```
- docker pull xanxerdocker/forge-desktop-snapshot
```

Run container and remove container after exit:
```  
- docker run --rm -it -p 3002:3000 forge-desktop-snapshot bash
```

Run container in detached mode. Will retain data after exit:
```
- docker run -d -it -p 3002:3000 forge-desktop-snapshot bash
```
Access container at localhost:3002 in your web browser. 

Once inside the container. Run the setup script using sudo:
```
- bash setup_forge_desktop.sh
```
After the setup is complete. To play forge-desktop-SNAPSHOT or forge-adventure mode run: 
```
- bash forge_game_selector.sh
```

# FORGE DEV ENVIRONMENT:
Dockerized apps for a forge development environment
- intellij-ce
- Magic Set Editor 2 - Advanced
- Tiled - Map Editor

Pull docker images.
```
- docker pull xanxerdocker/intellij-ce-ide
- docker pull xanxerdocker/magic-set-editor-2-advanced
- docker pull xanxerdocker/tiled-map-editor
```
# Intellij-ce IDE container

Run Intellij-ce  container and remove container after exit:
```
- docker run --rm -it -p 3003:3000 xanxerdocker/intellij-cd-ide bash
```
Run detached Intellij-ce container. Will retain data after exit")
```
- docker run -d -it -p 3003:3000 xanxerdocker/intellij-ce-ide bash
```
Access container via localhost:3003 in web browser and to start application run:
```
- bash run_intellij-ce.sh
```


# Magic Set Editor 2 Advanced

Run Magic Set Editor 2 - Advanced container and remove container after exit.
```
- docker run --rm -it -p 3004:3000 xanxerdocker/magic-set-editor-2-advanced bash
```

Run detached: Magic Set Editor 2 - Advanced container. Will retain data after exit.
```
- docker run -d -it -p 3004:3000 xanxerdocker/magic-set-editor-2-advanced bash
```

Access container via localhost:3004 in web browser.

Once inside the container to compile the app and finish setup run using sudo:
```
- bash install-mse-adv-full.sh
```

After the app is compiled, to start app run:
```
- bash run_mse.sh
```

# Tiled - Map Editor

Run Tiled - Map Editor container and remove container after exit:
```
- docker run --rm -it -p 3005:3000 xanxerdocker/tiled-map-editor bash
```

Run detached Tiled - Map Editor container. Will retain data after exit.
```
- docker run -d -it -p 3005:3000 xanxerdocker/tiled-map-editor bash
```

Access container via localhost:3005 in web browser and to start application run:
```
- bash run_tiled_map_editor.sh
```