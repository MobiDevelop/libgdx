package com.badlogic.gdx.maps.loaders;

import java.io.IOException;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.maps.Map;
import com.badlogic.gdx.maps.MapLoader;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.XmlReader;

public abstract class XmlMapLoader<M extends Map, P extends AssetLoaderParameters<M>> extends AsynchronousAssetLoader<M, P> implements MapLoader<M, P> {

	protected M map;
	
	protected XmlReader reader;
	protected XmlReader.Element root;
	
	public XmlMapLoader (FileHandleResolver resolver) {
		super(resolver);
		reader = new XmlReader();
	}

	/** Implements the synchronous, direct-loading mechanism of loading a map.
	 * 
	 * @param fileName
	 * @param parameters Can be null, in this case the loader defaults will be used
	 * @return a Map */
	public M load (String fileName, P parameters) {
		try {
			loadParameters((parameters == null ? createDefaultParameters() : parameters), null);

			FileHandle mapFile = resolve(fileName);
			root = reader.parse(mapFile);
			Array<? extends Object> resources = requestResources(mapFile, parameters);
			M map = loadMap(mapFile);
			map.setOwnedResources(resources);

			finishLoading(parameters);

			return map;
		} catch (IOException e) {
			throw new GdxRuntimeException("Couldn't load map '" + fileName + "'", e);
		}
	}

	/** From AsynchronousAssetLoader, implements the asynchronous loading mechanism of loading a map. */
	@Override
	public void loadAsync (AssetManager manager, String fileName, P parameter) {
		map = null;

		FileHandle mapFile = resolve(fileName);
		loadParameters((parameter == null ? createDefaultParameters() : parameter), manager);

		try {
			map = loadMap(mapFile);
		} catch (Exception e) {
			throw new GdxRuntimeException("Couldn't load map '" + fileName + "'", e);
		}
	}

	@Override
	public M loadSync (AssetManager manager, String fileName, P parameter) {
		finishLoading(parameter);
		return map;
	}
	
	@Override
	public Array<AssetDescriptor> getDependencies (String fileName, P parameter) {
		Array<AssetDescriptor> dependencies = new Array<AssetDescriptor>();
		try {
			FileHandle mapFile = resolve(fileName);
			root = reader.parse(mapFile);
			return requestDependencies(mapFile, parameter);
		} catch (IOException e) {
			throw new GdxRuntimeException("Couldn't load map '" + fileName + "'", e);
		}
	}	
}