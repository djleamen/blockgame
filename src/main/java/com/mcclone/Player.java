/*
 * This file represents a Player in the game.
 * It handles the player's position, movement, and camera controls.
 * It also handles the player's physics, including gravity and jumping.
 */

package com.mcclone;

import org.lwjgl.opengl.GL11;

public class Player {

    private static final float GRAVITY = 0.008f;
    private static final float EYE = 1.62f;

    public float x,y,z;
    private float pitch,yaw;
    private float vy;

    public Player(float x,float y,float z){this.x=x;this.y=y;this.z=z;}

    // physics
    public void tickPhysics(World world) {
        vy -= GRAVITY;
        y += vy;
    
        float ground = world.groundHeight(x, z) + 2.0f; // player is 2 blocks tall
    
        if (vy <= 0 && (y - ground) < 0.5f) {
            y = ground;
            vy = 0;
        }
    }
    public boolean onGround(){return vy==0;}
    public void move(float dx,float dz){x+=dx;z+=dz;}
    public void jump(float v){if(onGround())vy=v;}

    // camera
    public void addYaw(float d){yaw+=d;}
    public void addPitch(float d){pitch=Math.max(-89,Math.min(89,pitch+d));}
    public float getYaw(){return yaw;}
    public float getPitch(){return pitch;}

    public void applyCamera(){
        GL11.glRotatef(-pitch,1,0,0);
        GL11.glRotatef( yaw ,0,1,0);
        GL11.glTranslatef(-x,-y,-z);
    }

    public static float getEYE() {
        return EYE;
    }
}