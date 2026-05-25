/**
 * Texture loading and registration.
 *
 * <p>Two ways to get a texture into OpenGL:</p>
 * <ul>
 *   <li>{@link #loadTexture(String)} — read a PNG from the classpath.</li>
 *   <li>{@link #registerTexture(String, BufferedImage)} — upload a Java
 *       {@link BufferedImage} produced at runtime (used by
 *       {@link TextureGenerator}).</li>
 * </ul>
 *
 * <p>Either way the texture is cached and addressable by its string key so
 * {@link BlockRenderer} can ask for {@code "grass_top"} without caring how
 * the bytes got there.</p>
 */

package com.mcclone;

import java.awt.image.BufferedImage;
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
 */
public final class TextureLoader {

    /** Cache mapping texture key → OpenGL texture id. */
    private static final Map<String, Integer> textureCache = new HashMap<>();

    private TextureLoader() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Load a PNG texture from the classpath and cache it under its resource path.
     *
     * @param resourcePath the classpath-relative path to the texture resource
     * @return the OpenGL texture id
     * @throws TextureLoadException if the texture cannot be loaded
     */
    public static int loadTexture(String resourcePath) {
        Integer cached = textureCache.get(resourcePath);
        if (cached != null) {
            return cached;
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
                throw new TextureLoadException(
                        "Failed to load texture: " + resourcePath + " - " + stbi_failure_reason());
            }
            int id = uploadRgba(image, w.get(0), h.get(0));
            stbi_image_free(image);
            textureCache.put(resourcePath, id);
            return id;
        }
    }

    /**
     * Upload a {@link BufferedImage} as a new OpenGL texture and cache it under {@code key}.
     * Subsequent lookups via {@link #getTexture(String)} return the same id.
     *
     * @param key   string key callers will use to refer to this texture
     * @param image source image (any size, any type)
     * @return the OpenGL texture id
     */
    public static int registerTexture(String key, BufferedImage image) {
        Integer cached = textureCache.get(key);
        if (cached != null) {
            return cached;
        }
        int w = image.getWidth();
        int h = image.getHeight();
        ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = image.getRGB(x, y);
                buf.put((byte) ((argb >> 16) & 0xFF)); // R
                buf.put((byte) ((argb >> 8) & 0xFF));  // G
                buf.put((byte) (argb & 0xFF));         // B
                buf.put((byte) ((argb >> 24) & 0xFF)); // A
            }
        }
        buf.flip();
        int id = uploadRgba(buf, w, h);
        textureCache.put(key, id);
        return id;
    }

    /**
     * Look up a texture id by key.
     *
     * @param key the key passed to {@link #registerTexture(String, BufferedImage)}
     *            or {@link #loadTexture(String)}
     * @return the texture id, or {@code 0} if no texture is registered under that key
     */
    public static int getTexture(String key) {
        Integer id = textureCache.get(key);
        return id == null ? 0 : id;
    }

    /**
     * Allocate, parameterise and upload an RGBA pixel buffer as a 2D texture.
     */
    private static int uploadRgba(ByteBuffer rgba, int width, int height) {
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, rgba);
        return textureId;
    }

    private static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        ByteBuffer buffer;
        try (InputStream source = TextureLoader.class.getClassLoader().getResourceAsStream(resource)) {
            if (source == null) {
                throw new IOException("Resource not found: " + resource);
            }
            try (ReadableByteChannel rbc = Channels.newChannel(source)) {
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

    /** Enable 2D texturing in OpenGL. */
    public static void enableTextures() { glEnable(GL_TEXTURE_2D); }

    /** Disable 2D texturing in OpenGL. */
    public static void disableTextures() { glDisable(GL_TEXTURE_2D); }

    /**
     * Bind a texture for rendering.
     *
     * @param textureId the OpenGL texture id (0 unbinds)
     */
    public static void bindTexture(int textureId) {
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    /**
     * Delete every cached texture from OpenGL and clear the cache.
     */
    public static void cleanup() {
        for (int textureId : textureCache.values()) {
            glDeleteTextures(textureId);
        }
        textureCache.clear();
    }
}
