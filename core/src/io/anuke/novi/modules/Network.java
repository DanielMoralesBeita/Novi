package io.anuke.novi.modules;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import io.anuke.novi.Novi;
import io.anuke.novi.entities.Entity;
import io.anuke.novi.network.Registrator;
import io.anuke.novi.network.Syncable;
import io.anuke.novi.network.packets.*;
import io.anuke.ucore.modules.Module;

public class Network extends Module<Novi>{
	public static final String ip = System.getProperty("user.name").equals("anuke") ? "localhost" : "107.11.24.144";
	public static final int port = 7576;
	public static int ping = 0;
	public static final int synctime = 3;
	private boolean connected = true;
	private boolean initialconnect = false;
	Client client;

	public void init(){
		try{
			int buffer = (int)Math.pow(2, 7);
			client = new Client(8192 * buffer, 8192 * buffer);
			Registrator.register(client.getKryo());
			client.addListener(ping == 0 ? new Listen() : new Listener.LagListener(ping, ping, new Listen()));
			client.start();
			client.connect(12000, ip, port, port);
			ConnectPacket packet = new ConnectPacket();
			packet.name = (System.getProperty("user.name"));
			client.sendTCP(packet);
			initialconnect = true;
			Novi.log("Connecting to server..");
		}catch(Exception e){
			Novi.log(e);
			Novi.log("Connection failed!");
		}
	}

	public void sendUpdate(){
		PositionPacket pos = new PositionPacket();
		pos.x = getModule(ClientData.class).player.x;
		pos.y = getModule(ClientData.class).player.y;
		pos.rotation = getModule(ClientData.class).player.getSpriteRotation();
		pos.velocity = getModule(ClientData.class).player.velocity;
		client.sendUDP(pos);
	}

	class Listen extends Listener{
		@Override
		public void received(Connection connection, Object object){
			try{
				if(object instanceof DataPacket){
					DataPacket data = (DataPacket)object;
					getModule(ClientData.class).player.resetID(data.playerid);
					Entity.entities = data.entities;
					getModule(ClientData.class).player.AddSelf();
					Novi.log("Recieved data packet.");
				}else if(object instanceof EffectPacket){
					EffectPacket effect = (EffectPacket)object;
					effect.apply(t);
				}else if(object instanceof Entity){
					Entity entity = (Entity)object;
					entity.onRecieve();
					entity.AddSelf();
					//Novi.log("recieved entity of type " + entity.getClass().getSimpleName());
				}else if(object instanceof EntityRemovePacket){
					EntityRemovePacket remove = (EntityRemovePacket)object;
					if(Entity.entities.containsKey(remove.id)) Entity.entities.get(remove.id).removeEvent();
					Entity.entities.remove(remove.id);
				}else if(object instanceof DeathPacket){
					getModule(ClientData.class).player.deathEvent();
				}else if(object instanceof WorldUpdatePacket){
					WorldUpdatePacket packet = (WorldUpdatePacket)object;
					getModule(ClientData.class).player.health = packet.health;
					for(Long key : packet.updates.keySet()){
						if(Entity.entityExists(key))
						((Syncable)Entity.getEntity(key)).readSync(packet.updates.get(key));
					}
				}
			}catch(Exception e){
				e.printStackTrace();
				Novi.log("Packet recieve error!");
			}
		}
	}

	public boolean connected(){
		return connected;
	}

	public boolean initialconnect(){
		return initialconnect;
	}

	public float pingInFrames(){
		return ((ping * 2f + client.getReturnTripTime()) / 1000f) * Entity.delta() * 60f + 1f;
	}

	@Override
	public void update(){
		if(initialconnect && Gdx.graphics.getFrameId() % synctime == 0) sendUpdate();
		connected = client.isConnected();
		if(Gdx.graphics.getFrameId() % 120 == 0) client.updateReturnTripTime();
	}

	public static String shuffle(String inputString){
		char a[] = inputString.toCharArray();
		for(int i = 0;i < a.length - 1;i ++){
			int j = MathUtils.random(a.length - 1);
			char temp = a[i];
			a[i] = a[j];
			a[j] = temp;
		}
		return new String(a);
	}

}
