package io.anuke.novi.systems;

import java.util.HashSet;

import io.anuke.novi.entities.Entities;
import io.anuke.novi.entities.Entity;
import io.anuke.novi.entities.SolidEntity;

public class CollisionSystem extends IteratingSystem{
	HashSet<Long> collided = new HashSet<Long>(); //used for storing collisions each frame so entities don't collide twice
	
	@Override
	public void update(Entity aentity){
		SolidEntity entity = (SolidEntity)aentity;
		for(Entity other : Entities.list()){
			
			if(other.equals(entity) || !(other instanceof SolidEntity) || !entity.inRange((SolidEntity)other, 10 + entity.material.getRectangle().width)) continue;
			if( !collided.contains(other.getID())){
				SolidEntity othersolid = (SolidEntity)other;
				if(othersolid.collides(entity) && entity.collides(othersolid)){
					collisionEvent(entity, othersolid);
					collided.add(entity.getID());
				}
			}
		}
	}
	
	private void collisionEvent(SolidEntity entitya, SolidEntity entityb){
		entitya.collisionEvent(entityb);
		entityb.collisionEvent(entitya);
	}

	public boolean accept(Entity entity){
		return entity instanceof SolidEntity;
	}
}
