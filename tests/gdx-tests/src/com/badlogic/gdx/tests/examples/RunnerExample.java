package com.badlogic.gdx.tests.examples;

import static com.badlogic.gdx.graphics.g2d.SpriteBatch.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.TiledMapTileSets;
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.tests.utils.GdxTest;
import com.badlogic.gdx.tests.utils.OrthoCamController;

public class RunnerExample extends GdxTest {

	private static final int TILE_SIZE = 32;
	private static final int STAGE_WIDTH = 100;
	private static final int STAGE_HEIGHT = 100;
	
	private int stage;
	
	private TiledMap   map;
	private TiledMapEx map1;
	private TiledMapEx map2;
	
	private TiledMapRenderer renderer;
	private TiledMapRenderer renderer2;
	private OrthographicCamera camera;
	private OrthoCamController cameraController;
	private AssetManager assetManager;
	private Texture tiles;
	private Texture texture;
	private BitmapFont font;
	private SpriteBatch batch;
	
	private TextureRegion[][] tileRegions;
	
	@Override
	public void create() {
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		
		camera = new OrthographicCamera();
		camera.setToOrtho(false, (w / h) * 320, 320);
		camera.update();

		map1 = new TiledMapEx();		
		map1.getLayers().add(new TiledMapTileLayer(STAGE_WIDTH, STAGE_HEIGHT, TILE_SIZE, TILE_SIZE));
		map1.getLayers().add(new TiledMapTileLayer(STAGE_WIDTH, STAGE_HEIGHT, TILE_SIZE, TILE_SIZE));
		
		map2 = new TiledMapEx();
		map2.getLayers().add(new TiledMapTileLayer(STAGE_WIDTH, STAGE_HEIGHT, TILE_SIZE, TILE_SIZE));
		map2.getLayers().add(new TiledMapTileLayer(STAGE_WIDTH, STAGE_HEIGHT, TILE_SIZE, TILE_SIZE));
		
		font = new BitmapFont();
		batch = new SpriteBatch();		
		renderer = new TiledMapExRenderer(map1);
		renderer2 = new TiledMapExRenderer(map2);
		
		tiles = new Texture(Gdx.files.internal("data/maps/tiled/tiles.png"));
		TiledMapTileSet tileset = new TiledMapTileSet();
		TextureRegion[][] splitTiles = TextureRegion.split(tiles, 32, 32);
		for (int y = 0, height = splitTiles.length; y < height; y++) {
			for (int x = 0, width = splitTiles[y].length; x < width; x++) {
				tileset.putTile(y * width + x + 1, new StaticTiledMapTile(splitTiles[y][x]));
			}				
		}
		map1.getTileSets().addTileSet(tileset);
		map2.getTileSets().addTileSet(tileset);
		generateStage();
		System.gc();
	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(100f / 255f, 100f / 255f, 250f / 255f, 1f);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		camera.update();
		renderer.setView(camera);
		renderer.render();
		renderer2.setView(camera);
		renderer2.render();
		camera.position.x += 50 * Gdx.graphics.getDeltaTime();
		generateStageIfNeeded();
	}
	
	public void generateStageIfNeeded() {
		if (camera.position.x + camera.viewportWidth >= stage * STAGE_WIDTH * TILE_SIZE) {
			generateStage();
		}
	}
	
	class TiledMapEx extends TiledMap {
		
		public float x;
		public float y;
		
	}
	
	class TiledMapExRenderer extends BatchTiledMapRenderer {
		
		private float[] vertices = new float[20];
		
		public TiledMapExRenderer (TiledMapEx map) {
			super(map);
		}
		@Override
		public void renderObject (MapObject object) {
		}
		@Override
		public void renderTileLayer (TiledMapTileLayer layer) {
			TiledMapEx map = (TiledMapEx) getMap();
			
			final float color = Color.toFloatBits(1, 1, 1, layer.getOpacity());
			
			final int layerWidth = layer.getWidth();
			final int layerHeight = layer.getHeight();
			
			final float layerTileWidth = layer.getTileWidth() * unitScale;
			final float layerTileHeight = layer.getTileHeight() * unitScale;
			
			final int col1 = Math.max(0, (int) ((viewBounds.x - map.x) / layerTileWidth));
			final int col2 = Math.min(layerWidth, (int) (((viewBounds.x - map.x) + viewBounds.width + layerTileWidth) / layerTileWidth));

			final int row1 = Math.max(0, (int) ((viewBounds.y - map.y) / layerTileHeight));
			final int row2 = Math.min(layerHeight, (int) (((viewBounds.y - map.y) + viewBounds.height + layerTileHeight) / layerTileHeight));				
			
			float y = row1 * layerTileHeight + map.y;
			float xStart = col1 * layerTileWidth + map.x;
			final float[] vertices = this.vertices;
			
			for (int row = row1; row < row2; row++) {
				float x = xStart;
				for (int col = col1; col < col2; col++) {
					final TiledMapTileLayer.Cell cell = layer.getCell(col, row);
					if(cell == null) {
						x += layerTileWidth;
						continue;
					}
					final TiledMapTile tile = cell.getTile();
					if (tile != null) {
						if (tile instanceof AnimatedTiledMapTile) continue;
						
						final boolean flipX = cell.getFlipHorizontally();
						final boolean flipY = cell.getFlipVertically();
						final int rotations = cell.getRotation();
						
						TextureRegion region = tile.getTextureRegion();
						
						float x1 = x;
						float y1 = y;
						float x2 = x1 + region.getRegionWidth() * unitScale;
						float y2 = y1 + region.getRegionHeight() * unitScale;
						
						float u1 = region.getU();
						float v1 = region.getV2();
						float u2 = region.getU2();
						float v2 = region.getV();
						
						vertices[X1] = x1;
						vertices[Y1] = y1;
						vertices[C1] = color;
						vertices[U1] = u1;
						vertices[V1] = v1;
						
						vertices[X2] = x1;
						vertices[Y2] = y2;
						vertices[C2] = color;
						vertices[U2] = u1;
						vertices[V2] = v2;
						
						vertices[X3] = x2;
						vertices[Y3] = y2;
						vertices[C3] = color;
						vertices[U3] = u2;
						vertices[V3] = v2;
						
						vertices[X4] = x2;
						vertices[Y4] = y1;
						vertices[C4] = color;
						vertices[U4] = u2;
						vertices[V4] = v1;							
						
						if (flipX) {
							float temp = vertices[U1];
							vertices[U1] = vertices[U3];
							vertices[U3] = temp;
							temp = vertices[U2];
							vertices[U2] = vertices[U4];
							vertices[U4] = temp;
						}
						if (flipY) {
							float temp = vertices[V1];
							vertices[V1] = vertices[V3];
							vertices[V3] = temp;
							temp = vertices[V2];
							vertices[V2] = vertices[V4];
							vertices[V4] = temp;
						}
						if (rotations != 0) {
							switch (rotations) {
								case Cell.ROTATE_90: {
									float tempV = vertices[V1];
									vertices[V1] = vertices[V2];
									vertices[V2] = vertices[V3];
									vertices[V3] = vertices[V4];
									vertices[V4] = tempV;

									float tempU = vertices[U1];
									vertices[U1] = vertices[U2];
									vertices[U2] = vertices[U3];
									vertices[U3] = vertices[U4];
									vertices[U4] = tempU;									
									break;
								}
								case Cell.ROTATE_180: {
									float tempU = vertices[U1];
									vertices[U1] = vertices[U3];
									vertices[U3] = tempU;
									tempU = vertices[U2];
									vertices[U2] = vertices[U4];
									vertices[U4] = tempU;									
									float tempV = vertices[V1];
									vertices[V1] = vertices[V3];
									vertices[V3] = tempV;
									tempV = vertices[V2];
									vertices[V2] = vertices[V4];
									vertices[V4] = tempV;
									break;
								}
								case Cell.ROTATE_270: {
									float tempV = vertices[V1];
									vertices[V1] = vertices[V4];
									vertices[V4] = vertices[V3];
									vertices[V3] = vertices[V2];
									vertices[V2] = tempV;

									float tempU = vertices[U1];
									vertices[U1] = vertices[U4];
									vertices[U4] = vertices[U3];
									vertices[U3] = vertices[U2];
									vertices[U2] = tempU;									
									break;
								}
							}								
						}
						spriteBatch.draw(region.getTexture(), vertices, 0, 20);
						x += layerTileWidth;
					}
				}
				y += layerTileHeight;
			}			
		}
	}
	
	private void generateStage() {
		TiledMapEx map;
		if (stage % 2 == 0) {
			map = map1;
		} else {
			map = map2;
		}
		map.x = stage * STAGE_WIDTH * TILE_SIZE;
		stage++;
		MapLayers layers = map.getLayers();
		TiledMapTileLayer background = (TiledMapTileLayer)layers.get(0);
		TiledMapTileLayer foreground = (TiledMapTileLayer)layers.get(1);
		TiledMapTileSets tilesets = map.getTileSets();
		for (int x = 0, width = background.getWidth(); x < width; x++) {
			if (x % 10 == 0) {
				for (int y = 0, height = background.getHeight(); y < height; y++) {
					Cell cell1 = background.getCell(x + 0, y);
					if (cell1 == null) {
						cell1 = new Cell();
						background.setCell(x + 0, y, cell1);
					}
					cell1.setTile(tilesets.getTile(3));
					Cell cell2 = background.getCell(x + 1, y);
					if (cell2 == null) {
						cell2 = new Cell();
						background.setCell(x + 1, y, cell2);
					}
					cell2.setTile(tilesets.getTile(4));	
				}
				x++;
			} else {
				for (int y = 0, height = background.getHeight(); y < height; y++) {
					Cell cell1 = background.getCell(x + 0, y);
					if (cell1 == null) {
						cell1 = new Cell();
						background.setCell(x, y, cell1);
					}
					cell1.setTile(tilesets.getTile(1));
				}
			}

		}
		for (int x = 0, width = background.getWidth(); x < width; x++) {
			Cell cell1 = foreground.getCell(x, 0);
			if (cell1 == null) {
				cell1 = new Cell();
				foreground.setCell(x, 0, cell1);
			}
			cell1.setTile(tilesets.getTile(2));
		}
		
	}
	
}
