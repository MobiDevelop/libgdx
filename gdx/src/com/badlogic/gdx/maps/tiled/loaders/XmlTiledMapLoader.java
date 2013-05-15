
package com.badlogic.gdx.maps.tiled.loaders;

import java.io.IOException;
import java.util.StringTokenizer;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;

/** Encapsulates the basic driving logic of a tiled map loader.
 * 
 * Xml-based format specific loaders should extends this class and implement the required callbacks.
 * 
 * @author bmanuel */
public abstract class XmlTiledMapLoader<T extends TiledMap, P extends AssetLoaderParameters<T>> extends
	AsynchronousAssetLoader<T, P> implements BaseTiledMapLoader<T, P> {

	// xml parsing
	protected XmlReader xml = new XmlReader();
	protected Element root;

	// map data
	protected int mapWidthInPixels;
	protected int mapHeightInPixels;
	protected T map;

	public XmlTiledMapLoader (FileHandleResolver resolver) {
		super(resolver);
	}

	/** Implements the synchronous, direct-loading mechanism of loading a tiled map.
	 * 
	 * @param fileName
	 * @param parameters Can be null, in this case the loader defaults will be used
	 * @return a TiledMap */
	public T load (String fileName, P parameters) {
		try {
			loadParameters(parameters, null);

			FileHandle mapFile = resolve(fileName);
			root = xml.parse(mapFile);
			Array<? extends Object> resources = requestResources(mapFile, root, parameters);
			T map = loadTilemap(root, mapFile);
			map.setOwnedResources(resources);

			finishLoading(parameters);

			return map;
		} catch (IOException e) {
			throw new GdxRuntimeException("Couldn't load tilemap '" + fileName + "'", e);
		}
	}

	/** From AsynchronousAssetLoader, implements the asynchronous loading mechanism of loading a tiled map. */
	@Override
	public void loadAsync (AssetManager manager, String fileName, P parameter) {
		map = null;

		FileHandle mapFile = resolve(fileName);
		loadParameters(parameter, manager);

		try {
			map = loadTilemap(root, mapFile);
		} catch (Exception e) {
			throw new GdxRuntimeException("Couldn't load tilemap '" + fileName + "'", e);
		}
	}

	@Override
	public T loadSync (AssetManager manager, String fileName, P parameter) {
		finishLoading(parameter);
		return map;
	}

	@Override
	public Array<AssetDescriptor> getDependencies (String fileName, P parameter) {
		Array<AssetDescriptor> dependencies = new Array<AssetDescriptor>();
		try {
			FileHandle mapFile = resolve(fileName);
			root = xml.parse(mapFile);
			return requestDependencies(mapFile, root, parameter);
		} catch (IOException e) {
			throw new GdxRuntimeException("Couldn't load tilemap '" + fileName + "'", e);
		}
	}

	/** Common utilities shared across all the hierarchy */

	protected static FileHandle getRelativeFileHandle (FileHandle file, String path) {
		StringTokenizer tokenizer = new StringTokenizer(path, "\\/");
		FileHandle result = file.parent();
		while (tokenizer.hasMoreElements()) {
			String token = tokenizer.nextToken();
			if (token.equals(".."))
				result = result.parent();
			else {
				result = result.child(token);
			}
		}
		return result;
	}

	protected static int unsignedByteToInt (byte b) {
		return (int)b & 0xFF;
	}
}
