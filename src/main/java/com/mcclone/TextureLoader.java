/*
 * This file handles texture loading and management.
 * It uses LWJGL's STB library to load PNG images and converts them to OpenGL textures.
 */

package com.mcclone;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.stb.STBImage.stbi_failure_reason;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;
import org.lwjgl.system.MemoryStack;

public final class TextureLoader {
    
    private static final Map<String, Integer> textureCache = new HashMap<>();
    
    private TextureLoader() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static int loadTexture(String resourcePath) {
        // Check if texture is already loaded
        if (textureCache.containsKey(resourcePath)) {
            return textureCache.get(resourcePath);
        }
        
        ByteBuffer imageBuffer;
        try {
            imageBuffer = ioResourceToByteBuffer(resourcePath, 8 * 1024);
        } catch (IOException e) {
            throw new TextureLoadException("Failed to load texture: " + resourcePath, e);
        }
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            
            ByteBuffer image = stbi_load_from_memory(imageBuffer, w, h, channels, 4);
            if (image == null) {
                throw new TextureLoadException("Failed to load texture: " + resourcePath + " - " + stbi_failure_reason());
            }
            
            int width = w.get(0);
            int height = h.get(0);
            
            int textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
            
            stbi_image_free(image);
            
            // Cache the texture
            textureCache.put(resourcePath, textureId);
            
            return textureId;
        }
    }
    
    private static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        ByteBuffer buffer;
        
        try (InputStream source = TextureLoader.class.getClassLoader().getResourceAsStream(resource);
             ReadableByteChannel rbc = Channels.newChannel(source)) {
            
            if (source == null) {
                throw new IOException("Resource not found: " + resource);
            }
            
            buffer = BufferUtils.createByteBuffer(bufferSize);
            
            while (true) {
                int bytes = rbc.read(buffer);
                if (bytes == -1) {
                    break;
                }
                if (buffer.remaining() == 0) {
                    buffer = resizeBuffer(buffer, buffer.capacity() * 3 / 2);
                }
            }
        }
        
        buffer.flip();
        return buffer.slice();
    }
    
    private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }
    
    public static void enableTextures() {
        glEnable(GL_TEXTURE_2D);
    }
    
    public static void disableTextures() {
        glDisable(GL_TEXTURE_2D);
    }
    
    public static void bindTexture(int textureId) {
        glBindTexture(GL_TEXTURE_2D, textureId);
    }
    
    public static void cleanup() {
        for (int textureId : textureCache.values()) {
            glDeleteTextures(textureId);
        }
        textureCache.clear();
    }
}
