package com.portfolio.fts.file;

import com.portfolio.fts.api.FileResponse;
import java.io.InputStream;

public record FileContent(FileResponse metadata, InputStream stream) {
}
