
package com.badlogic.gdx.maps.tiled.loaders;

import java.io.IOException;
import java.util.StringTokenizer;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.maps.MapLoader;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.loaders.XmlMapLoader;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;

/** Encapsulates the basicdriving logic of a tiled map loader.
 * 
 * Format specific loaders should extends this class and implement the required callbacks.
 * 
 * Typically, custom, format-specific loaders are split in a partially abstract base class inheriting from {@link TiledMapLoader},
 * whose purpose is to implement the concrete, format-specific, loading of the map, and a concrete loader class inheriting from
 * {@link ConcreteTiledMapLoader}, whose role is to direct and manage concrete resource loading, populate tiles and setup by
 * configuration parameters, such as texture filtering.
 * 
 * The split leads to better separation, and permits loaders to perform resource loading and tile population with custom resource
 * resolvers, such as an Atlas resolver by extending the base format-specific loader.
 * 
 * @author bmanuel */
public abstract class XmlTiledMapLoader<T extends TiledMap, P extends AssetLoaderParameters<T>> extends XmlMapLoader<T, P> {

	// map data
	protected int mapWidthInPixels;
	protected int mapHeightInPixels;

	public XmlTiledMapLoader (FileHandleResolver resolver) {
		super(resolver);
	}

	protected static int unsignedByteToInt (byte b) {
		return (int)b & 0xFF;
	}
}
