package com.quorum.tessera.config.util;

import com.quorum.tessera.config.ServiceLoaderUtil;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

public interface FilesDelegate {

    /**
     * @see java.nio.file.Files#notExists(java.nio.file.Path,
     * java.nio.file.LinkOption...)
     */
    default boolean notExists(Path path, LinkOption... options) {
        return Files.notExists(path, options);
    }

    /**
     * @see java.nio.file.Files#deleteIfExists(java.nio.file.Path)
     */
    default boolean deleteIfExists(Path path) {
        return IOCallback.execute(() -> Files.deleteIfExists(path));
    }

    /**
     * @see java.nio.file.Files#createFile(java.nio.file.Path, java.nio.file.attribute.FileAttribute...) 
     */
    default Path createFile(Path path, FileAttribute... attributes) {

        return IOCallback.execute(() -> Files.createFile(path, attributes));

    }

    /**
     * @see java.nio.file.Files#newInputStream(java.nio.file.Path, java.nio.file.OpenOption...) 
     */
    default InputStream newInputStream(Path path,OpenOption... options) {
            return IOCallback.execute(() -> Files.newInputStream(path, options));
    }
    
    /**
     * @see java.nio.file.Files#exists(java.nio.file.Path, java.nio.file.LinkOption...) 
     */
    default boolean exists(Path path,LinkOption... options) {
        return Files.exists(path, options);
    }
    
    static FilesDelegate create() {
        return ServiceLoaderUtil.load(FilesDelegate.class).orElse(new FilesDelegate() {
        });
    }

}
