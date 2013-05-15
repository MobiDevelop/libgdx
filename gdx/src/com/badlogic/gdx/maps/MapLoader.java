package com.badlogic.gdx.maps;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader.Element;

public interface MapLoader<M extends Map, P extends AssetLoaderParameters<M>> {
	/** Called when the loader is requested to load a map, giving a chance to a loader to perform one-time initialization, given the
	 * loading parameters and the {@link AssetManager}, if any.
	 * 
	 * A synchronous loading request is recognized whenever the passed {@link AssetManager} instance is null, otherwise an
	 * asynchronous loading has been requested: this is useful for loaders to know, since a case-specific ImageResolver can be
	 * instantiated.
	 * 
	 * @param parameters the requested loader configuration
	 * @param assetManager can be null, means the loading is synchronous */
	public abstract void loadParameters (P parameters, AssetManager assetManager);

	/** Called only when a <b>synchronous</b> loading is requested, let the loader to load and report back the resources allocated
	 * for the specified map to be loaded.
	 * 
	 * @param mapFile the requested map file to load
	 * @param parameters the requested loader configuration
	 * @return the resources allocated by the loader */
	public abstract Array<? extends Object> requestResources (FileHandle mapFile, P parameters);

	/** Called only when an <b>asynchronous</b> loading is requested, let the loader to report back of any asset it depends on for
	 * loading the specified map file: the loader can return <b>null</b> to signal that there are no dependencies.
	 * 
	 * @param mapFile the requested map file to load
	 * @param parameters the requested loader configuration
	 * @return other assets that this asset depends on, and thus need to be loaded first, or null if there are no dependencies */
	public abstract Array<AssetDescriptor> requestDependencies (FileHandle mapFile, P parameters);

	/** Request the loader to concretely load the specified map, given the root element and the map file: traversing the passed
	 * document should be the preferred method, but a loader is free to implement whatever method is preferred.
	 * 
	 * @param mapFile the FileHandle to the specified map file
	 * @return a Map instance */
	public abstract M loadMap (FileHandle mapFile);

	/** Called when a Map loading has complete, gives a chance to loaders to perform final cleanup or setup, the requested
	 * loader configuration is passed along for convenience.
	 * 
	 * @param parameters the requested loader configuration */
	public abstract void finishLoading (P parameters);

	/** Called when the loading mechanism request new default parameters to be constructed by the loader
	 * 
	 * @return a new instance of the loader default parameters */
	public abstract P createDefaultParameters ();

	public abstract M createMap();
	
	public abstract boolean isYUp();
}