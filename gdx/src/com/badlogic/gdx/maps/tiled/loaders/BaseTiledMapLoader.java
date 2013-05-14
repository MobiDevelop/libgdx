
package com.badlogic.gdx.maps.tiled.loaders;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;

/** Represents the required actions to be completed by a concrete loader implementation.
 * 
 * @author bmanuel */
public interface BaseTiledMapLoader<T extends TiledMap> {
	/** The implementer should populate the passed tileset with tiles: the way this happens can be specific to the type of resources
	 * actually being used, see {@link TmxImageMapLoader} and {@link TmxAtlasMapLoader} for such an example. */
	public abstract void loadTiles (TiledMapTileSet tileset, T map, FileHandle mapFile, FileHandle tilesetImage);
}
