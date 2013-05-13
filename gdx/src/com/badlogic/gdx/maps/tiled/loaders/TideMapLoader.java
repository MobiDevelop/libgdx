
package com.badlogic.gdx.maps.tiled.loaders;

import java.io.IOException;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.ImageResolver;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.ImageResolver.AssetManagerImageResolver;
import com.badlogic.gdx.maps.ImageResolver.DirectImageResolver;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.TiledMapTileSets;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.XmlReader.Element;

/** Implements the Tide format base loader.
 * 
 * @author bmanuel */
public class TideMapLoader extends XmlTiledMapLoader<TiledMap, TideMapLoader.Parameters> implements ConcreteTiledMapLoader<TiledMap> {

	public static class Parameters extends AssetLoaderParameters<TiledMap> {
		/** Whether to load the map for a y-up coordinate system */
		public boolean yUp = true;
		/** generate mipmaps? **/
		public boolean generateMipMaps = false;
		/** The TextureFilter to use for minification **/
		public TextureFilter textureMinFilter = TextureFilter.Nearest;
		/** The TextureFilter to use for magnification **/
		public TextureFilter textureMagFilter = TextureFilter.Nearest;
	}

	private final ObjectMap<String, Texture> textures = new ObjectMap<String, Texture>();
	private AssetManager assetManager;
	private ImageResolver resolver;
	private Parameters parameters;

	public TideMapLoader () {
		super(new InternalFileHandleResolver());
	}

	public TideMapLoader (FileHandleResolver resolver) {
		super(resolver);
	}

	public TiledMap load (String fileName) {
		return load(fileName, createDefaultParameters());
	}

	/** Loads the map data, given the XML root element and an {@link ImageResolver} used to return the tileset Textures
	 * @param root the XML root element
	 * @param mapFile the Filehandle of the tmx file
	 * @return the {@link TiledMap} */
	@Override
	public TiledMap loadMap (FileHandle mapFile) {
		TiledMap map = createTiledMap();

		Element properties = root.getChildByName("properties");
		if (properties != null) {
			loadProperties(map.getProperties(), properties);
		}
		Element tilesheets = root.getChildByName("TileSheets");
		for (Element tilesheet : tilesheets.getChildrenByName("TileSheet")) {
			loadTileSheet(map, tilesheet, mapFile);
		}
		Element layers = root.getChildByName("Layers");
		for (Element layer : layers.getChildrenByName("Layer")) {
			loadLayer(map, layer);
		}

		return map;
	}

	@Override
	public void loadParameters (Parameters parameters, AssetManager assetManager) {
		this.assetManager = assetManager;
		this.parameters = parameters;
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
		return parameters.yUp;
	}

	@Override
	public Array<? extends Object> requestResources (FileHandle mapFile, Parameters parameters) {
		try {
			for (FileHandle textureFile : loadTileSheets(root, mapFile)) {
				Texture texture = new Texture(textureFile, parameters.generateMipMaps);
				texture.setFilter(parameters.textureMinFilter, parameters.textureMagFilter);
				textures.put(textureFile.path(), texture);
			}
		} catch (IOException e) {
			throw new GdxRuntimeException("Couldn't load tilemap '" + mapFile.path() + "'", e);
		}

		return textures.values().toArray();
	}

	@Override
	public Array<AssetDescriptor> requestDependencies (FileHandle mapFile, Parameters parameters) {
		Array<AssetDescriptor> dependencies = new Array<AssetDescriptor>();

		boolean generateMipMaps = (parameters != null ? parameters.generateMipMaps : false);
		TextureLoader.TextureParameter texParams = new TextureParameter();
		texParams.genMipMaps = generateMipMaps;
		if (parameters != null) {
			texParams.minFilter = parameters.textureMinFilter;
			texParams.magFilter = parameters.textureMagFilter;
		}

		try {
			for (FileHandle image : loadTileSheets(root, mapFile)) {
				dependencies.add(new AssetDescriptor(image.path(), Texture.class, texParams));
			}
		} catch (IOException e) {
			throw new GdxRuntimeException("Couldn't load tilemap '" + mapFile.path() + "'", e);
		}

		return dependencies;
	}

	@Override
	public void populateWithTiles (TiledMapTileSet tileset, TiledMap map, FileHandle mapFile, FileHandle tilesetImage) {
		int tilewidth = tileset.getProperties().get("tilewidth", Integer.class);
		int tileheight = tileset.getProperties().get("tileheight", Integer.class);
		int spacingX = tileset.getProperties().get("spacingX", Integer.class);
		int spacingY = tileset.getProperties().get("spacingY", Integer.class);
		int marginX = tileset.getProperties().get("marginX", Integer.class);
		int marginY = tileset.getProperties().get("marginY", Integer.class);
		int firstgid = tileset.getProperties().get("firstgid", Integer.class);

		if (assetManager == null) {
			// sync load
			resolver = new DirectImageResolver(textures);
		} else {
			// async load
			resolver = new AssetManagerImageResolver(assetManager);
		}

		TextureRegion texture = resolver.getImage(tilesetImage.path());
		int stopWidth = texture.getRegionWidth() - tilewidth;
		int stopHeight = texture.getRegionHeight() - tileheight;

		int id = firstgid;

		for (int y = marginY; y <= stopHeight; y += tileheight + spacingY) {
			for (int x = marginX; x <= stopWidth; x += tilewidth + spacingX) {
				TextureRegion tileRegion = new TextureRegion(texture, x, y, tilewidth, tileheight);
				if (!parameters.yUp) {
					tileRegion.flip(false, true);
				}
				TiledMapTile tile = new StaticTiledMapTile(tileRegion);
				tile.setId(id);
				tileset.putTile(id++, tile);
			}
		}

	}

	@Override
	public void finishLoading (Parameters parameters) {
	}

	/** Loads the tilesets
	 * @param root the root XML element
	 * @return a list of filenames for images containing tiles
	 * @throws IOException */
	private Array<FileHandle> loadTileSheets (Element root, FileHandle mapFile) throws IOException {
		Array<FileHandle> images = new Array<FileHandle>();
		Element tilesheets = root.getChildByName("TileSheets");
		for (Element tileset : tilesheets.getChildrenByName("TileSheet")) {
			Element imageSource = tileset.getChildByName("ImageSource");
			FileHandle image = mapFile.relative(imageSource.getText());
			images.add(image);
		}
		return images;
	}

	private void loadTileSheet (TiledMap map, Element element, FileHandle mapFile) {
		if (element.getName().equals("TileSheet")) {
			String id = element.getAttribute("Id");
			String description = element.getChildByName("Description").getText();
			String imageSource = element.getChildByName("ImageSource").getText();

			Element alignment = element.getChildByName("Alignment");
			String sheetSize = alignment.getAttribute("SheetSize");
			String tileSize = alignment.getAttribute("TileSize");
			String margin = alignment.getAttribute("Margin");
			String spacing = alignment.getAttribute("Spacing");

			String[] sheetSizeParts = sheetSize.split(" x ");
			int sheetSizeX = Integer.parseInt(sheetSizeParts[0]);
			int sheetSizeY = Integer.parseInt(sheetSizeParts[1]);

			String[] tileSizeParts = tileSize.split(" x ");
			int tileSizeX = Integer.parseInt(tileSizeParts[0]);
			int tileSizeY = Integer.parseInt(tileSizeParts[1]);

			String[] marginParts = margin.split(" x ");
			int marginX = Integer.parseInt(marginParts[0]);
			int marginY = Integer.parseInt(marginParts[1]);

			String[] spacingParts = margin.split(" x ");
			int spacingX = Integer.parseInt(spacingParts[0]);
			int spacingY = Integer.parseInt(spacingParts[1]);

			FileHandle image = mapFile.relative(imageSource);

			// TODO: Actually load the tilesheet
			// Need to make global ids as Tide doesn't have global ids.
			TiledMapTileSets tilesets = map.getTileSets();
			int firstgid = 1;
			for (TiledMapTileSet tileset : tilesets) {
				firstgid += tileset.size();
			}

			TiledMapTileSet tileset = new TiledMapTileSet();
			tileset.setName(id);
			tileset.getProperties().put("firstgid", firstgid);
			tileset.getProperties().put("tilewidth", tileSizeX);
			tileset.getProperties().put("tileheight", tileSizeY);
			tileset.getProperties().put("spacingX", spacingX);
			tileset.getProperties().put("spacingY", spacingY);
			tileset.getProperties().put("marginX", marginX);
			tileset.getProperties().put("marginY", marginY);
			int gid = firstgid;

			populateWithTiles(tileset, map, mapFile, image);

			Element properties = element.getChildByName("Properties");
			if (properties != null) {
				loadProperties(tileset.getProperties(), properties);
			}

			tilesets.addTileSet(tileset);
		}
	}

	private void loadLayer (TiledMap map, Element element) {
		if (element.getName().equals("Layer")) {
			String id = element.getAttribute("Id");
			String visible = element.getAttribute("Visible");

			Element dimensions = element.getChildByName("Dimensions");
			String layerSize = dimensions.getAttribute("LayerSize");
			String tileSize = dimensions.getAttribute("TileSize");

			String[] layerSizeParts = layerSize.split(" x ");
			int layerSizeX = Integer.parseInt(layerSizeParts[0]);
			int layerSizeY = Integer.parseInt(layerSizeParts[1]);

			String[] tileSizeParts = tileSize.split(" x ");
			int tileSizeX = Integer.parseInt(tileSizeParts[0]);
			int tileSizeY = Integer.parseInt(tileSizeParts[1]);

			TiledMapTileLayer layer = new TiledMapTileLayer(layerSizeX, layerSizeY, tileSizeX, tileSizeY);
			Element tileArray = element.getChildByName("TileArray");
			Array<Element> rows = tileArray.getChildrenByName("Row");
			TiledMapTileSets tilesets = map.getTileSets();
			TiledMapTileSet currentTileSet = null;
			int firstgid = 0;
			int x, y;
			for (int row = 0, rowCount = rows.size; row < rowCount; row++) {
				Element currentRow = rows.get(row);
				y = rowCount - 1 - row;
				x = 0;
				for (int child = 0, childCount = currentRow.getChildCount(); child < childCount; child++) {
					Element currentChild = currentRow.getChild(child);
					String name = currentChild.getName();
					if (name.equals("TileSheet")) {
						currentTileSet = tilesets.getTileSet(currentChild.getAttribute("Ref"));
						firstgid = currentTileSet.getProperties().get("firstgid", Integer.class);
					} else if (name.equals("Null")) {
						x += currentChild.getIntAttribute("Count");
					} else if (name.equals("Static")) {
						Cell cell = new Cell();
						cell.setTile(currentTileSet.getTile(firstgid + currentChild.getIntAttribute("Index")));
						layer.setCell(x++, y, cell);
					} else if (name.equals("Animated")) {
						// Create an AnimatedTile
						int interval = currentChild.getInt("Interval");
						Element frames = currentChild.getChildByName("Frames");
						Array<StaticTiledMapTile> frameTiles = new Array<StaticTiledMapTile>();
						for (int frameChild = 0, frameChildCount = frames.getChildCount(); frameChild < frameChildCount; frameChild++) {
							Element frame = frames.getChild(frameChild);
							String frameName = frame.getName();
							if (frameName.equals("TileSheet")) {
								currentTileSet = tilesets.getTileSet(frame.getAttribute("Ref"));
								firstgid = currentTileSet.getProperties().get("firstgid", Integer.class);
							} else if (frameName.equals("Static")) {
								frameTiles.add((StaticTiledMapTile)currentTileSet.getTile(firstgid + frame.getIntAttribute("Index")));
							}
						}
						Cell cell = new Cell();
						cell.setTile(new AnimatedTiledMapTile(interval / 1000f, frameTiles));
						layer.setCell(x++, y, cell); // TODO: Reuse existing animated tiles
					}
				}
			}
			map.getLayers().add(layer);
		}
	}
	
	protected void loadProperties (MapProperties properties, Element element) {
		if (element.getName().equals("properties")) {
			for (Element property : element.getChildrenByName("property")) {
				String name = property.getAttribute("name", null);
				String value = property.getAttribute("value", null);
				if (value == null) {
					value = property.getText();
				}
				properties.put(name, value);
			}
		}
	}
	
}
