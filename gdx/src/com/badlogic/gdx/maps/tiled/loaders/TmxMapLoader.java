
package com.badlogic.gdx.maps.tiled.loaders;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;

import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.maps.ImageResolver;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.TiledMapTileSets;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Polyline;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.XmlReader.Element;

/** Implements the TMX format base loader.
 * 
 * @author bmanuel */
public abstract class TmxMapLoader<T extends TiledMap, P extends AssetLoaderParameters<T>> extends XmlTiledMapLoader<T, P>
	implements ConcreteTiledMapLoader<T> {

	// tmx-specific constants
	protected static final int FLAG_FLIP_HORIZONTALLY = 0x80000000;
	protected static final int FLAG_FLIP_VERTICALLY = 0x40000000;
	protected static final int FLAG_FLIP_DIAGONALLY = 0x20000000;
	protected static final int MASK_CLEAR = 0xE0000000;

	public TmxMapLoader () {
		super(new InternalFileHandleResolver());
	}

	public TmxMapLoader (FileHandleResolver resolver) {
		super(resolver);
	}

	/** Loads the map data, given the XML root element and an {@link ImageResolver} used to return the tileset Textures
	 * @param root the XML root element
	 * @param mapFile the Filehandle of the tmx file
	 * @return the {@link TiledMap} */
	@Override
	public T loadMap (FileHandle mapFile) {
		T map = createTiledMap();

		String mapOrientation = root.getAttribute("orientation", null);
		int mapWidth = root.getIntAttribute("width", 0);
		int mapHeight = root.getIntAttribute("height", 0);
		int tileWidth = root.getIntAttribute("tilewidth", 0);
		int tileHeight = root.getIntAttribute("tileheight", 0);
		String mapBackgroundColor = root.getAttribute("backgroundcolor", null);

		MapProperties mapProperties = map.getProperties();
		if (mapOrientation != null) {
			mapProperties.put("orientation", mapOrientation);
		}
		mapProperties.put("width", mapWidth);
		mapProperties.put("height", mapHeight);
		mapProperties.put("tilewidth", tileWidth);
		mapProperties.put("tileheight", tileHeight);
		if (mapBackgroundColor != null) {
			mapProperties.put("backgroundcolor", mapBackgroundColor);
		}
		mapWidthInPixels = mapWidth * tileWidth;
		mapHeightInPixels = mapHeight * tileHeight;

		Element properties = root.getChildByName("properties");
		if (properties != null) {
			loadProperties(map.getProperties(), properties);
		}
		Array<Element> tilesets = root.getChildrenByName("tileset");
		for (Element element : tilesets) {
			loadTileSet(map, element, mapFile);
			root.removeChild(element);
		}
		for (int i = 0, j = root.getChildCount(); i < j; i++) {
			Element element = root.getChild(i);
			String name = element.getName();
			if (name.equals("layer")) {
				loadTileLayer(map, element);
			} else if (name.equals("objectgroup")) {
				loadObjectGroup(map, element);
			}
		}

		return map;
	}

	/** Loads the specified tileset data, adding it to the collection of the specified map, given the XML element, the mapFile and
	 * an {@link ImageResolver} used to retrieve the tileset Textures.
	 * 
	 * <p>
	 * Default tileset's property keys that are loaded by default are:
	 * </p>
	 * 
	 * <ul>
	 * <li><em>firstgid</em>, (int, defaults to 1) the first valid global id used for tile numbering</li>
	 * <li><em>imagesource</em>, (String, defaults to empty string) the tileset source image filename</li>
	 * <li><em>imagewidth</em>, (int, defaults to 0) the tileset source image width</li>
	 * <li><em>imageheight</em>, (int, defaults to 0) the tileset source image height</li>
	 * <li><em>tilewidth</em>, (int, defaults to 0) the tile width</li>
	 * <li><em>tileheight</em>, (int, defaults to 0) the tile height</li>
	 * <li><em>margin</em>, (int, defaults to 0) the tileset margin</li>
	 * <li><em>spacing</em>, (int, defaults to 0) the tileset spacing</li>
	 * </ul>
	 * 
	 * <p>
	 * The values are extracted from the specified Tmx file, if a value can't be found then the default is used.
	 * </p>
	 * @param map the Map whose tilesets collection will be populated
	 * @param element the XML element identifying the tileset to load
	 * @param mapFile the Filehandle of the tmx file */
	protected void loadTileSet (T map, Element element, FileHandle mapFile) {
		if (element.getName().equals("tileset")) {
			String name = element.get("name", null);
			int firstgid = element.getIntAttribute("firstgid", 1);
			int tilewidth = element.getIntAttribute("tilewidth", 0);
			int tileheight = element.getIntAttribute("tileheight", 0);
			int spacing = element.getIntAttribute("spacing", 0);
			int margin = element.getIntAttribute("margin", 0);
			String source = element.getAttribute("source", null);

			String imageSource = "";
			int imageWidth = 0, imageHeight = 0;

			FileHandle image = null;
			if (source != null) {
				FileHandle tsx = mapFile.relative(source);
				try {
					element = reader.parse(tsx);
					name = element.get("name", null);
					tilewidth = element.getIntAttribute("tilewidth", 0);
					tileheight = element.getIntAttribute("tileheight", 0);
					spacing = element.getIntAttribute("spacing", 0);
					margin = element.getIntAttribute("margin", 0);
					imageSource = element.getChildByName("image").getAttribute("source");
					imageWidth = element.getChildByName("image").getIntAttribute("width", 0);
					imageHeight = element.getChildByName("image").getIntAttribute("height", 0);
					image = tsx.relative(imageSource);
				} catch (IOException e) {
					throw new GdxRuntimeException("Error parsing external tileset.");
				}
			} else {
				imageSource = element.getChildByName("image").getAttribute("source");
				imageWidth = element.getChildByName("image").getIntAttribute("width", 0);
				imageHeight = element.getChildByName("image").getIntAttribute("height", 0);
				image = mapFile.relative(imageSource);
			}

			TiledMapTileSet tileset = new TiledMapTileSet();
			MapProperties props = tileset.getProperties();
			tileset.setName(name);
			props.put("firstgid", firstgid);
			props.put("imagesource", imageSource);
			props.put("imagewidth", imageWidth);
			props.put("imageheight", imageHeight);
			props.put("tilewidth", tilewidth);
			props.put("tileheight", tileheight);
			props.put("margin", margin);
			props.put("spacing", spacing);

			populateWithTiles(tileset, map, mapFile, image);

			Array<Element> tileElements = element.getChildrenByName("tile");

			for (Element tileElement : tileElements) {
				int localtid = tileElement.getIntAttribute("id", 0);
				TiledMapTile tile = tileset.getTile(firstgid + localtid);
				if (tile != null) {
					Element properties = tileElement.getChildByName("properties");
					if (properties != null) {
						loadProperties(tile.getProperties(), properties);
					}
				}
			}

			Element properties = element.getChildByName("properties");
			if (properties != null) {
				loadProperties(tileset.getProperties(), properties);
			}
			map.getTileSets().addTileSet(tileset);
		}
	}

	/** Load one layer (a 'layer' tag).
	 * @param map
	 * @param element */
	protected void loadTileLayer (TiledMap map, Element element) {
		if (element.getName().equals("layer")) {
			String name = element.getAttribute("name", null);
			int width = element.getIntAttribute("width", 0);
			int height = element.getIntAttribute("height", 0);
			int tileWidth = element.getParent().getIntAttribute("tilewidth", 0);
			int tileHeight = element.getParent().getIntAttribute("tileheight", 0);
			boolean visible = element.getIntAttribute("visible", 1) == 1;
			float opacity = element.getFloatAttribute("opacity", 1.0f);
			TiledMapTileLayer layer = new TiledMapTileLayer(width, height, tileWidth, tileHeight);
			layer.setVisible(visible);
			layer.setOpacity(opacity);
			layer.setName(name);

			TiledMapTileSets tilesets = map.getTileSets();

			Element data = element.getChildByName("data");
			String encoding = data.getAttribute("encoding", null);
			String compression = data.getAttribute("compression", null);
			if (encoding == null) { // no 'encoding' attribute means that the encoding is XML
				throw new GdxRuntimeException("Unsupported encoding (XML) for TMX Layer Data");
			}
			if (encoding.equals("csv")) {
				String[] array = data.getText().split(",");
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						int id = (int)Long.parseLong(array[y * width + x].trim());

						final boolean flipHorizontally = ((id & FLAG_FLIP_HORIZONTALLY) != 0);
						final boolean flipVertically = ((id & FLAG_FLIP_VERTICALLY) != 0);
						final boolean flipDiagonally = ((id & FLAG_FLIP_DIAGONALLY) != 0);

						id = id & ~MASK_CLEAR;

						tilesets.getTile(id);
						TiledMapTile tile = tilesets.getTile(id);
						if (tile != null) {
							Cell cell = createTileLayerCell(flipHorizontally, flipVertically, flipDiagonally);
							cell.setTile(tile);
							layer.setCell(x, isYUp() ? height - 1 - y : y, cell);
						}
					}
				}
			} else {
				if (encoding.equals("base64")) {
					byte[] bytes = Base64Coder.decode(data.getText());
					if (compression == null) {
						int read = 0;
						for (int y = 0; y < height; y++) {
							for (int x = 0; x < width; x++) {

								int id = unsignedByteToInt(bytes[read++]) | unsignedByteToInt(bytes[read++]) << 8
									| unsignedByteToInt(bytes[read++]) << 16 | unsignedByteToInt(bytes[read++]) << 24;

								final boolean flipHorizontally = ((id & FLAG_FLIP_HORIZONTALLY) != 0);
								final boolean flipVertically = ((id & FLAG_FLIP_VERTICALLY) != 0);
								final boolean flipDiagonally = ((id & FLAG_FLIP_DIAGONALLY) != 0);

								id = id & ~MASK_CLEAR;

								tilesets.getTile(id);
								TiledMapTile tile = tilesets.getTile(id);
								if (tile != null) {
									Cell cell = createTileLayerCell(flipHorizontally, flipVertically, flipDiagonally);
									cell.setTile(tile);
									layer.setCell(x, isYUp() ? height - 1 - y : y, cell);
								}
							}
						}
					} else if (compression.equals("gzip")) {
						GZIPInputStream GZIS = null;
						try {
							GZIS = new GZIPInputStream(new ByteArrayInputStream(bytes), bytes.length);
						} catch (IOException e) {
							throw new GdxRuntimeException("Error Reading TMX Layer Data - IOException: " + e.getMessage());
						}

						byte[] temp = new byte[4];
						for (int y = 0; y < height; y++) {
							for (int x = 0; x < width; x++) {
								try {
									GZIS.read(temp, 0, 4);
									int id = unsignedByteToInt(temp[0]) | unsignedByteToInt(temp[1]) << 8
										| unsignedByteToInt(temp[2]) << 16 | unsignedByteToInt(temp[3]) << 24;

									final boolean flipHorizontally = ((id & FLAG_FLIP_HORIZONTALLY) != 0);
									final boolean flipVertically = ((id & FLAG_FLIP_VERTICALLY) != 0);
									final boolean flipDiagonally = ((id & FLAG_FLIP_DIAGONALLY) != 0);

									id = id & ~MASK_CLEAR;

									tilesets.getTile(id);
									TiledMapTile tile = tilesets.getTile(id);
									if (tile != null) {
										Cell cell = createTileLayerCell(flipHorizontally, flipVertically, flipDiagonally);
										cell.setTile(tile);
										layer.setCell(x, isYUp() ? height - 1 - y : y, cell);
									}
								} catch (IOException e) {
									throw new GdxRuntimeException("Error Reading TMX Layer Data.", e);
								}
							}
						}
					} else if (compression.equals("zlib")) {
						Inflater zlib = new Inflater();

						byte[] temp = new byte[4];

						zlib.setInput(bytes, 0, bytes.length);

						for (int y = 0; y < height; y++) {
							for (int x = 0; x < width; x++) {
								try {
									zlib.inflate(temp, 0, 4);
									int id = unsignedByteToInt(temp[0]) | unsignedByteToInt(temp[1]) << 8
										| unsignedByteToInt(temp[2]) << 16 | unsignedByteToInt(temp[3]) << 24;

									final boolean flipHorizontally = ((id & FLAG_FLIP_HORIZONTALLY) != 0);
									final boolean flipVertically = ((id & FLAG_FLIP_VERTICALLY) != 0);
									final boolean flipDiagonally = ((id & FLAG_FLIP_DIAGONALLY) != 0);

									id = id & ~MASK_CLEAR;

									tilesets.getTile(id);
									TiledMapTile tile = tilesets.getTile(id);
									if (tile != null) {
										Cell cell = createTileLayerCell(flipHorizontally, flipVertically, flipDiagonally);
										cell.setTile(tile);
										layer.setCell(x, isYUp() ? height - 1 - y : y, cell);
									}

								} catch (DataFormatException e) {
									throw new GdxRuntimeException("Error Reading TMX Layer Data.", e);
								}
							}
						}
					}
				} else {
					// any other value of 'encoding' is one we're not aware of, probably a feature of a future version of Tiled
					throw new GdxRuntimeException("Unrecognised encoding (" + encoding + ") for TMX Layer Data");
				}
			}
			Element properties = element.getChildByName("properties");
			if (properties != null) {
				loadProperties(layer.getProperties(), properties);
			}
			map.getLayers().add(layer);
		}
	}

	protected void loadObjectGroup (TiledMap map, Element element) {
		if (element.getName().equals("objectgroup")) {
			String name = element.getAttribute("name", null);
			MapLayer layer = new MapLayer();
			layer.setName(name);
			Element properties = element.getChildByName("properties");
			if (properties != null) {
				loadProperties(layer.getProperties(), properties);
			}

			for (Element objectElement : element.getChildrenByName("object")) {
				loadObject(layer, objectElement);
			}

			map.getLayers().add(layer);
		}
	}

	protected void loadObject (MapLayer layer, Element element) {
		if (element.getName().equals("object")) {
			MapObject object = null;

			int x = element.getIntAttribute("x", 0);
			int y = (isYUp() ? mapHeightInPixels - element.getIntAttribute("y", 0) : element.getIntAttribute("y", 0));

			int width = element.getIntAttribute("width", 0);
			int height = element.getIntAttribute("height", 0);

			if (element.getChildCount() > 0) {
				Element child = null;
				if ((child = element.getChildByName("polygon")) != null) {
					String[] points = child.getAttribute("points").split(" ");
					float[] vertices = new float[points.length * 2];
					for (int i = 0; i < points.length; i++) {
						String[] point = points[i].split(",");
						vertices[i * 2] = Integer.parseInt(point[0]);
						vertices[i * 2 + 1] = Integer.parseInt(point[1]);
						if (isYUp()) {
							vertices[i * 2 + 1] *= -1;
						}
					}
					Polygon polygon = new Polygon(vertices);
					polygon.setPosition(x, y);
					object = new PolygonMapObject(polygon);
				} else if ((child = element.getChildByName("polyline")) != null) {
					String[] points = child.getAttribute("points").split(" ");
					float[] vertices = new float[points.length * 2];
					for (int i = 0; i < points.length; i++) {
						String[] point = points[i].split(",");
						vertices[i * 2] = Integer.parseInt(point[0]);
						vertices[i * 2 + 1] = Integer.parseInt(point[1]);
						if (isYUp()) {
							vertices[i * 2 + 1] *= -1;
						}
					}
					Polyline polyline = new Polyline(vertices);
					polyline.setPosition(x, y);
					object = new PolylineMapObject(polyline);
				} else if ((child = element.getChildByName("ellipse")) != null) {
					object = new EllipseMapObject(x, isYUp() ? y - height : y, width, height);
				}
			}
			if (object == null) {
				object = new RectangleMapObject(x, isYUp() ? y - height : y, width, height);
			}
			object.setName(element.getAttribute("name", null));
			String type = element.getAttribute("type", null);
			if (type != null) {
				object.getProperties().put("type", type);
			}
			object.getProperties().put("x", x);
			object.getProperties().put("y", isYUp() ? y - height : y);
			object.setVisible(element.getIntAttribute("visible", 1) == 1);
			Element properties = element.getChildByName("properties");
			if (properties != null) {
				loadProperties(object.getProperties(), properties);
			}
			layer.getObjects().add(object);
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
	
	protected Cell createTileLayerCell (boolean flipHorizontally, boolean flipVertically, boolean flipDiagonally) {
		Cell cell = new Cell();
		if (flipDiagonally) {
			if (flipHorizontally && flipVertically) {
				cell.setFlipHorizontally(true);
				cell.setRotation(isYUp() ? Cell.ROTATE_270 : Cell.ROTATE_90);
			} else if (flipHorizontally) {
				cell.setRotation(isYUp() ? Cell.ROTATE_270 : Cell.ROTATE_90);
			} else if (flipVertically) {
				cell.setRotation(isYUp() ? Cell.ROTATE_90 : Cell.ROTATE_270);
			} else {
				cell.setFlipVertically(true);
				cell.setRotation(isYUp() ? Cell.ROTATE_270 : Cell.ROTATE_90);
			}
		} else {
			cell.setFlipHorizontally(flipHorizontally);
			cell.setFlipVertically(flipVertically);
		}
		return cell;
	}

}
