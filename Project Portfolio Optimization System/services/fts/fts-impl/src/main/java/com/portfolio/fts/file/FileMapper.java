package com.portfolio.fts.file;

import com.portfolio.fts.api.FileResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FileMapper {

    @Mapping(source = "sizeBytes", target = "size")
    FileResponse toResponse(FileEntity entity);
}
