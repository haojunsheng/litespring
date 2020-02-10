package org.litespring.core.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.litespring.util.Assert;

/**
 * {@link Resource} implementation for {@code java.io.File} handles.
 * Supports resolution as a {@code File} and also as a {@code URL}.
 * Implements the extended {@link WritableResource} interface
 *
 * 对java.io.File类型资源的封装，只要是跟File打交道的，基本上都可以使用FileSystemResource。
 * 支持文件，URL形式，实现WritableResource。
 *
 */
public class FileSystemResource implements Resource {

    private final String path;
    private final File file;


    public FileSystemResource(String path) {
        Assert.notNull(path, "Path must not be null");
        this.file = new File(path);
        this.path = path;
    }

    public FileSystemResource(File file) {
        this.path = file.getPath();
        this.file = file;
    }

    public InputStream getInputStream() throws IOException {
        return new FileInputStream(this.file);
    }

    public String getDescription() {
        return "file [" + this.file.getAbsolutePath() + "]";
    }
}