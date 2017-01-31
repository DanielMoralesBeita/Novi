package io.anuke.novi.modules;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;

import io.anuke.novi.Novi;
import io.anuke.novi.entities.base.Player;
import io.anuke.novi.entities.effects.BreakEffect;
import io.anuke.novi.utils.Draw;
import io.anuke.novi.world.NoviMapRenderer;
import io.anuke.ucore.graphics.Atlas;
import io.anuke.ucore.modules.Module;
import io.anuke.utils.io.GifRecorder;

public class Renderer extends Module<Novi>{
	public float cameraShakeDuration, cameraShakeIntensity, cameraDrag;
	public final float GUIscale = 5f;
	public Network network;
	public SpriteBatch batch; //novi's batch
	public BitmapFont font; //a font for displaying text
	public NoviMapRenderer maprenderer; //used for rendering the map
	public Matrix4 matrix; // matrix used for rendering gui and other things
	public GlyphLayout layout; // used for getting font bounds
	public OrthographicCamera camera; //a camera, seems self explanatory
	public Atlas atlas; //texture atlas
	public int scale = 5; //camera zoom/scale
	public int pixelscale = 1; // pixelation scale
	public Player player; //player object from ClientData module
	public World world; // world module
	public FrameBuffer buffer;
	public GifRecorder recorder;
	public boolean debug = true;

	public Renderer(){
		matrix = new Matrix4();
		batch = new SpriteBatch();
		atlas = new Atlas(Gdx.files.internal("sprites/Novi.pack"));
		font = new BitmapFont(Gdx.files.internal("fonts/font.fnt"));
		font.setUseIntegerPositions(false);
		layout = new GlyphLayout();
		buffer = new FrameBuffer(Format.RGBA8888, Gdx.graphics.getWidth() / pixelscale, Gdx.graphics.getHeight() / pixelscale, false);
		buffer.getColorBufferTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
		recorder = new GifRecorder(batch, 0.2f);
		Draw.init(this);
		
		BreakEffect.createChunks();
	}
	
	@Override
	public void init(){
		camera = new OrthographicCamera(Gdx.graphics.getWidth() / scale, Gdx.graphics.getHeight() / scale);
		player = getModule(ClientData.class).player;
		world = getModule(World.class);
		network = getModule(Network.class);
	}

	@Override
	public void update(){
		updateCamera();
		batch.setProjectionMatrix(camera.combined); //make the batch use the camera projection
		clearScreen();
		doRender();
		updateCamera();
	}

	void doRender(){
		clearScreen();
		maprenderer.setView(camera);
		renderMap();

		batch.begin();
		//drawLayers();
		batch.end();
		batch.setProjectionMatrix(matrix);
		batch.begin();
		drawGUI();
		batch.end();
		batch.setColor(Color.WHITE);
	}

	public void renderMap(){
		for(int x = -1;x <= 1;x ++){
			for(int y = -1;y <= 1;y ++){
				maprenderer.setPosition(x * World.worldSize, y * World.worldSize);
				maprenderer.render();
			}
		}
	}

	public void drawGUI(){
		color(Color.WHITE);
		font.getData().setScale(1f / GUIscale);
		drawc("healthbarcontainer", 0, 0);
		AtlasRegion region = atlas.findRegion("healthbar");
		region.setRegionWidth((int)(region.getRotatedPackedWidth() * player.health / player.getShip().getMaxhealth()));
		batch.draw(region, 1, 1);

		if(debug){
			//	float f = ((WorldUtils.bound(camera.unproject(new Vector3(Gdx.input.getX(),Gdx.graphics.getHeight()/2,0)).x)));
			font.setColor(Color.ORANGE);
			font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 0, ghheight());
			font.draw(batch, "Ping: " + (network.client.getReturnTripTime() + Network.ping * 2), 0, ghheight() - 5);
		}

		if( !network.connected() || !network.initialconnect()){
			color(0, 0, 0, 0.5f);
			batch.draw(atlas.findRegion("blank"), 0, 0, ghwidth(), ghheight());
			color(Color.WHITE);
			drawFont(network.initialconnect() ? "Connecting..." : "Failed to connect to server.", ghwidth() / 2, ghheight() / 2);
		}
		
		recorder.update();

	}

	public void clearScreen(){
		Color clear = Color.SKY.cpy().sub(0.1f, 0.1f, 0.1f, 0f);
		Gdx.gl.glClearColor(clear.r, clear.g, clear.b, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
	}

	void drawWorld(){
		/*
		Random rand = new Random();
		rand.setSeed(1);
		float scl = 1000f;
		float time = (float)Gdx.graphics.getFrameId() / 10f + 1000f - player.x / 100f;
		for(int i = 1;i <= 100;i ++){
			float randx = (rand.nextFloat() - 0.5f) * scl;
			float randy = (rand.nextFloat() - 0.5f) * scl;
			int iscl = (i % 5 + 1);
			float airadd = (iscl * time) % 2000f - 1000f;
			layer("cloud" + (i % 7 + 1), camera.position.x + airadd + randx, camera.position.y + randy).setLayer( -2f);
		}
		*/
	}

	void updateCamera(){
		camera.position.set(player.x, player.y, 0f);
		shakeCamera();
		//limitCamera();
		camera.update();
	}

	void shakeCamera(){
		if(cameraShakeDuration > 0){
			cameraShakeDuration -= Novi.delta();
			camera.position.x += MathUtils.random( -cameraShakeIntensity, cameraShakeIntensity);
			camera.position.y += MathUtils.random( -cameraShakeIntensity, cameraShakeIntensity);
			cameraShakeIntensity -= cameraDrag * Novi.delta();
		}
	}

	void limitCamera(){
		//limit camera position, snap to sides
		if(camera.position.x - camera.viewportWidth / 2 * camera.zoom < 0) camera.position.x = camera.viewportWidth / 2 * camera.zoom;
		if(camera.position.y - camera.viewportHeight / 2 * camera.zoom < 0) camera.position.y = camera.viewportHeight / 2 * camera.zoom;
		if(camera.position.x + camera.viewportWidth / 2 * camera.zoom > world.worldWidthPixels()) camera.position.x = world.worldWidthPixels() - camera.viewportWidth / 2 * camera.zoom;
		if(camera.position.y + camera.viewportHeight / 2 * camera.zoom > world.worldHeightPixels()) camera.position.y = world.worldHeightPixels() - camera.viewportHeight / 2 * camera.zoom;
	}

	public void resize(int width, int height){
		matrix.setToOrtho2D(0, 0, width / GUIscale, height / GUIscale);
		camera.setToOrtho(false, width / scale, height / scale); //resize camera
	}

	public void shakeCamera(float duration, float intensity){
		if(cameraShakeDuration > 0 && cameraShakeIntensity > intensity) return;
		cameraShakeIntensity = intensity;
		cameraShakeDuration = duration;
		cameraDrag = cameraShakeIntensity / cameraShakeDuration;
	}


	public void zoom(float amount){
		if(camera.zoom + amount < 0 || (camera.zoom + amount) * camera.viewportWidth > world.worldWidthPixels()) return;
		if(camera.zoom < 3 || amount < 0) camera.zoom += amount;
	}

	public GlyphLayout getBounds(String text){
		layout.setText(font, text);
		return layout;
	}

	public void drawFont(String text, float x, float y){
		layout.setText(font, text);
		font.draw(batch, text, x - layout.width / 2, y + layout.height / 2);
	}

	//returns screen width / scale
	public float ghwidth(){
		return Gdx.graphics.getWidth() / GUIscale;
	}

	//returns screen height / scale
	public float ghheight(){
		return Gdx.graphics.getHeight() / GUIscale;
	}

	public void color(Color color){
		batch.setColor(color);
	}

	public void color(float r, float g, float b, float a){
		batch.setColor(new Color(r, g, b, a));
	}

	public float overlapx(float i, float r){
		if(MathUtils.isEqual(i, camera.position.x, (camera.viewportWidth * camera.zoom) / 2f + 10f + r)){
			return i;
		}else{
			return i < World.worldSize / 2f ? i + World.worldSize : i - World.worldSize;
		}
	}

	public float overlapy(float i, float r){
		if(MathUtils.isEqual(i, camera.position.y, (camera.viewportHeight * camera.zoom) / 2f + 10 + r)){
			return i;
		}else{
			return i < World.worldSize / 2f ? i + World.worldSize : i - World.worldSize;
		}
	}

	public float overlapx(float i){
		if(MathUtils.isEqual(i, camera.position.x, (camera.viewportWidth * camera.zoom) / 2f + 10f)){
			return i;
		}else{
			return i < World.worldSize / 2f ? i + World.worldSize : i - World.worldSize;
		}
	}

	public float overlapy(float i){
		if(MathUtils.isEqual(i, camera.position.y, (camera.viewportHeight * camera.zoom) / 2f + 10f)){
			return i;
		}else{
			return i < World.worldSize / 2f ? i + World.worldSize : i - World.worldSize;
		}
	}

	//utility/shortcut draw method
	public void draw(String region, float x, float y){
		batch.draw(atlas.findRegion(region), x - atlas.regionWidth(region) / 2, y - atlas.regionHeight(region) / 2);
	}

	public void drawc(String region, float x, float y){
		batch.draw(atlas.findRegion(region), x, y);
	}

	public void drawscl(String region, float x, float y, float scl){
		batch.draw(atlas.findRegion(region), x - atlas.regionWidth(region) / 2 * scl, y - atlas.regionHeight(region) / 2 * scl, atlas.regionHeight(region) * scl, atlas.regionHeight(region) * scl);
	}

	public void draw(String region, float x, float y, float rotation){
		batch.draw(atlas.findRegion(region), x - atlas.regionWidth(region) / 2, y - atlas.regionHeight(region) / 2, atlas.regionWidth(region) / 2, atlas.regionHeight(region) / 2, atlas.regionWidth(region), atlas.regionHeight(region), 1f, 1f, rotation);
	}

}
