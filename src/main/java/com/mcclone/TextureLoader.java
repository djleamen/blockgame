/**
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
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBImage.stbi_failure_reason;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;
import org.lwjgl.system.MemoryStack;

/**
 * Utility class for loading and managing OpenGL textures.
 * Handles loading PNG images from resources using STB and converting them to OpenGL textures.
 * Implements texture caching to avoid reloading the same texture multiple times.
 */
public final class TextureLoader {
    
    /** Cache mapping resource paths to OpenGL texture IDs */
    private static final Map<String, Integer> textureCache = new HashMap<>();
    
    /**
     * Private constructor to prevent instantiation of utility class.
     * 
     * @throws UnsupportedOperationException if instantiation is attempted
     */
    private TextureLoader() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Loads a texture from the specified resource path.
     * If the texture has been loaded before, returns the cached texture ID.
     * Uses STB to load PNG images and uploads them to OpenGL.
     * 
     * @param resourcePath the classpath-relative path to the texture resource
     * @return the OpenGL texture ID
     * @throws TextureLoadException if the texture cannot be loaded
     */
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
    
    /**
     * Reads a resource file into a ByteBuffer.
     * Dynamically resizes the buffer if the file is larger than the initial size.
     * 
     * @param resource the classpath-relative path to the resource
     * @param bufferSize the initial buffer size in bytes
     * @return a ByteBuffer containing the resource data
     * @throws IOException if the resource cannot be read
     */
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
    
    /**
     * Resizes a ByteBuffer to a new capacity, preserving existing data.
     * 
     * @param buffer the buffer to resize
     * @param newCapacity the new capacity in bytes
     * @return a new ByteBuffer with the specified capacity
     */
    private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }
    
    /**
     * Enables 2D texturing in OpenGL.
     */
    public static void enableTextures() {
        glEnable(GL_TEXTURE_2D);
    }
    
    /**
     * Disables 2D texturing in OpenGL.
     */
    public static void disableTextures() {
        glDisable(GL_TEXTURE_2D);
    }
    
    /**
     * Binds a texture for rendering.
     * 
     * @param textureId the OpenGL texture ID to bind
     */
    public static void bindTexture(int textureId) {
        glBindTexture(GL_TEXTURE_2D, textureId);
    }
    
    /**
     * Cleans up all loaded textures by deleting them from OpenGL.
     * Clears the texture cache. Should be called on program exit.
     */
    public static void cleanup() {
        for (int textureId : textureCache.values()) {
            glDeleteTextures(textureId);
        }
        textureCache.clear();
    }
}
