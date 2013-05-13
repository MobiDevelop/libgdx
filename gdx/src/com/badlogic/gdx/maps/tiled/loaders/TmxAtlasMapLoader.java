
package com.badlogic.gdx.maps.tiled.loaders;

import java.io.IOException;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.XmlReader.Element;

public class TmxAtlasMapLoader extends TmxMapLoader<TiledMap, TmxAtlasMapLoader.Parameters> {

	public static class Parameters extends AssetLoaderParameters<TiledMap> {
		/** Whether to load the map for a y-up coordinate system */
		public boolean yUp = true;

		/** force texture filters? **/
		public boolean forceTextureFilters = false;

		/** The TextureFilter to use for minification, if forceTextureFilter is enabled **/
		public TextureFilter textureMinFilter = TextureFilter.Nearest;

		/** The TextureFilter to use for magnification, if forceTextureFilter is enabled **/
		public TextureFilter textureMagFilter = TextureFilter.Nearest;
	}

	private final ObjectMap<String, TextureAtlas> atlases = new ObjectMap<String, TextureAtlas>();
	private AssetManager assetManager;
	private AtlasResolver resolver;
	private Parameters parameters;

	protected Array<Texture> texturesToFilter = new Array<Texture>();

	public TmxAtlasMapLoader () {
		super(new InternalFileHandleResolver());
	}

	public TmxAtlasMapLoader (FileHandleResolver resolver) {
		super(resolver);
	}

	public TiledMap load (String fileName) {
		return load(fileName, new Parameters());
	}

	@Override
	public TiledMap createTiledMap () {
		return new TiledMap();
	}

	@Override
	public Parameters createDefaultParameters () {
		return new Parameters();
	}

	@Override
	public boolean isYUp () {
		return this.parameters.yUp;
	}

	@Override
	public void loadParameters (Parameters parameters, AssetManager assetManager) {
		this.assetManager = assetManager;
		this.parameters = parameters;
	}

	@Override
	public Array<AssetDescriptor> requestDependencies (FileHandle mapFile, Parameters parameters) {
		Array<AssetDescriptor> dependencies = new Array<AssetDescriptor>();
		Element properties = root.getChildByName("properties");
		if (properties != null) {
			for (Element property : properties.getChildrenByName("property")) {
				String name = property.getAttribute("name");
				String value = property.getAttribute("value");
				if (name.startsWith("atlas")) {
					FileHandle atlasHandle = mapFile.relative(value);
					atlasHandle = resolve(atlasHandle.path());
					dependencies.add(new AssetDescriptor(atlasHandle.path(), TextureAtlas.class));
				}
			}
		} else {
			throw new GdxRuntimeException("Couldn't load tilemap '" + mapFile.path() + "', properties not found");
		}

		return dependencies;
	}

	@Override
	public Array<? extends Object> requestResources (FileHandle mapFile, Parameters parameters) {
		FileHandle atlasFile = null;

		try {
			atlasFile = loadAtlas(root, mapFile);
		} catch (IOException e) {
			throw new GdxRuntimeException("Couldn't load tileset atlas for '" + mapFile.path() + "'");
		}

		TextureAtlas atlas = new TextureAtlas(atlasFile);
		atlases.put(atlasFile.path(), atlas);

		return atlases.values().toArray();
	}

	@Override
	public void populateWithTiles (TiledMapTileSet tileset, TiledMap map, FileHandle mapFile, FileHandle tilesetImage) {
		int firstgid = tileset.getProperties().get("firstgid", Integer.class);

		if (assetManager == null) {
			// sync load
			resolver = new AtlasResolver.DirectAtlasResolver(atlases);
		} else {
			// async load
			resolver = new AtlasResolver.AssetManagerAtlasResolver(assetManager);
		}

		TextureAtlas atlas = null;
		String regionsName = "";
		if (map.getProperties().containsKey("atlas")) {
			FileHandle atlasHandle = mapFile.relative(map.getProperties().get("atlas", String.class));
			atlasHandle = resolve(atlasHandle.path());
			atlas = resolver.getAtlas(atlasHandle.path());
			regionsName = atlasHandle.nameWithoutExtension();

			if (parameters != null && parameters.forceTextureFilters) {
				for (Texture texture : atlas.getTextures()) {
					texturesToFilter.add(texture);
				}
			}
		}

		Array<AtlasRegion> regions = atlas.findRegions(regionsName);
		for (AtlasRegion region : regions) {
			// handle unused tile ids
			if (region != null) {
				StaticTiledMapTile tile = new StaticTiledMapTile(region);

				if (!parameters.yUp) {
					region.flip(false, true);
				}

				int tileid = firstgid + region.index;
				tile.setId(tileid);
				tileset.putTile(tileid, tile);
			}
		}
	}

	@Override
	public void finishLoading (Parameters parameters) {
		for (Texture texture : texturesToFilter) {
			texture.setFilter(this.parameters.textureMinFilter, this.parameters.textureMagFilter);
		}
	}

	private FileHandle loadAtlas (Element root, FileHandle mapFile) throws IOException {

		Element e = root.getChildByName("properties");
		Array<FileHandle> atlases = new Array<FileHandle>();

		if (e != null) {
			for (Element property : e.getChildrenByName("property")) {
				String name = property.getAttribute("name", null);
				String value = property.getAttribute("value", null);
				if (name.equals("atlas")) {
					if (value == null) {
						value = property.getText();
					}

					if (value == null || value.length() == 0) {
						// keep trying until there are no more atlas properties
						continue;
					}

					return mapFile.relative(value);
				}
			}
		}

		throw new IOException("Cannot find a valid atlas definition");
	}

	private interface AtlasResolver {

		public TextureAtlas getAtlas (String name);

		public static class DirectAtlasResolver implements AtlasResolver {

			private final ObjectMap<String, TextureAtlas> atlases;

			public DirectAtlasResolver (ObjectMap<String, TextureAtlas> atlases) {
				this.atlases = atlases;
			}

			@Override
			public TextureAtlas getAtlas (String name) {
				return atlases.get(name);
			}

		}

		public static class AssetManagerAtlasResolver implements AtlasResolver {
			private final AssetManager assetManager;

			public AssetManagerAtlasResolver (AssetManager assetManager) {
				this.assetManager = assetManager;
			}

			@Override
			public TextureAtlas getAtlas (String name) {
				return assetManager.get(name, TextureAtlas.class);
			}
		}
	}
}
