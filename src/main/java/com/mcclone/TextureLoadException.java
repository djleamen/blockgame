/*
 * Custom exception for texture loading failures.
 */

package com.mcclone;

public class TextureLoadException extends RuntimeException {
    
    public TextureLoadException(String message) {
        super(message);
    }
    
    public TextureLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
