
package com.badlogic.gdx.maps.tiled;

import com.badlogic.gdx.files.FileHandle;

/** Represents the required actions to be completed by a concrete loader implementation.
 * 
 * @author bmanuel */
public interface ConcreteMapLoader<T extends TiledMap> {
	/** The implementer should construct and return a concrete Tiled map object */
	public abstract T createTiledMap ();

	/** Whether or not this map is following the y-up convention */
	public abstract boolean isYUp ();

	/** The implementer should populate the passed tileset with tiles: the way this happens can be specific to the type of resources
	 * actually being used, see {@link TmxMapLoader} and {@link TmxAtlasMapLoader} for such an example. */
	public abstract void populateWithTiles (TiledMapTileSet tileset, T map, FileHandle mapFile, FileHandle tilesetImage);
}
