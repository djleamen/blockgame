/**
 * Custom exception for texture loading failures.
 */

package com.mcclone;
/**
 * Exception thrown when texture loading fails.
 * This is a runtime exception that wraps underlying IO or image loading errors.
 */public class TextureLoadException extends RuntimeException {
    
    /**
     * Constructs a new TextureLoadException with the specified detail message.
     * 
     * @param message the detail message explaining the failure
     */
    public TextureLoadException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new TextureLoadException with the specified detail message and cause.
     * 
     * @param message the detail message explaining the failure
     * @param cause the underlying cause of the exception
     */
    public TextureLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
