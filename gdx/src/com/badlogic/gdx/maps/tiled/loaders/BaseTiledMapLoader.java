
package com.badlogic.gdx.maps.tiled.loaders;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader.Element;

/** Represents the required actions to be completed by a format-specific Tiled loader
 * 
 * Format specific loaders should implement the following callbacks.
 * 
 * The order of the callbacks is the order in which they will get called, with the exception that {@link #requestDependencies}
 * <b>OR</b> {@link #requestResources} will be called, in case of synchronous or asynchronous loading, respectively.
 * 
 * @author bmanuel */
public interface BaseTiledMapLoader<T extends TiledMap, P extends AssetLoaderParameters<T>> {
	/** Called when the loader is requested to load a map, giving a chance to a loader to perform one-time initialization, given the
	 * loading parameters and the {@link AssetManager}, if any.
	 * 
	 * A synchronous loading request is recognized whenever the passed {@link AssetManager} instance is null, otherwise an
	 * asynchronous loading has been requested: this is useful for loaders to know, since a case-specific ImageResolver can be
	 * instantiated.
	 * 
	 * @param parameters the requested loader configuration or null if the default configuration is requested
	 * @param assetManager can be null, means the loading is synchronous */
	public abstract void loadParameters (P parameters, AssetManager assetManager);

	/** Called only when a <b>synchronous</b> loading is requested, let the loader to load and report back the resources allocated
	 * for the specified map to be loaded.
	 * 
	 * @param mapFile the requested map file to load
	 * @param root the root document of the map, can be used to traverse the map definition and detect dependencies
	 * @param parameters the requested loader configuration
	 * @return the Disposable resources allocated by the loader */
	public abstract Array<? extends Object> requestResources (FileHandle mapFile, Element root, P parameters);

	/** Called only when an <b>asynchronous</b> loading is requested, let the loader to report back of any asset it depends on for
	 * loading the specified map file: the loader can return <b>null</b> to signal that there are no dependencies.
	 * 
	 * @param mapFile the requested map file to load
	 * @param root the root document of the map, can be used to traverse the map definition and detect dependencies
	 * @param parameters the requested loader configuration
	 * @return other assets that this asset depends on, and thus need to be loaded first, or null if there are no dependencies */
	public abstract Array<AssetDescriptor> requestDependencies (FileHandle mapFile, Element root, P parameters);

	/** Request the loader to concretely load the specified map, given the root element and the map file: traversing the passed
	 * document should be the preferred method, but a loader is free to implement whatever method is preferred.
	 * 
	 * @param root The root document of the map
	 * @param mapFile the FileHandle to the specified map file
	 * @return a TiledMap instance */
	public abstract T loadTilemap (Element root, FileHandle mapFile);

	/** Called when a TiledMap loading has complete, gives a chance to loaders to perform final cleanup or setup, the requested
	 * loader configuration is passed along for convenience.
	 * 
	 * @param parameters the requested loader configuration */
	public abstract void finishLoading (P parameters);

	/** Called when the loading mechanism request a concrete instance of a TiledMap object to be constructed and returned */
	public abstract T createTiledMap ();

	/** Whether or not this map is following the y-up convention */
	public abstract boolean isYUp ();
}
