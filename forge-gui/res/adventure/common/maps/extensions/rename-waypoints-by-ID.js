/// <reference types="@mapeditor/tiled-api" />

/*
 * rename-waypoints-by-ID.js
 * Created by TabletopGeneral for Forge Adventure Mode
 *
 * This extension adds a 'Rename waypoints by ID' (Ctrl+Shift+R) action to the Map
 * menu, useful to visually identify points for mob navigation
 * Based on https://github.com/justdaft/tiled-scripts/blob/main/rename-object-by-type.js
 * 
 */

/* global tiled */

function doRenameWaypoints(thing) {
	let count = 0;
	for (let i = thing.layerCount - 1; i >= 0; i--) {
		const layer = thing.layerAt(i);

        if (layer.isGroupLayer) {
			const obj = doRenameWaypoints(layer, "waypoint");
			if (obj) {
				count = count + obj;
			}
		} else if (layer.isObjectLayer) {
			for (const obj of layer.objects) {

				if (obj.name == "waypoint") {
					obj.name = obj.id;
					count++;
				}
			}
		}
	}

	return count;
}


let renameWaypoints = tiled.registerAction("renameWaypoints", function(/* action */) {
	const map = tiled.activeAsset;
	if (!map.isTileMap) {
		tiled.alert("Not a tile map!");
		return;
	}


	const count = doRenameWaypoints(map);

	tiled.alert("Renamed " + count + " waypoints");

});


renameWaypoints.text = "Rename waypoints by ID";
renameWaypoints.shortcut = "Ctrl+Shift+R";

tiled.extendMenu("Map", [
	{ separator: true },
	{ action: "renameWaypoints" },
]);