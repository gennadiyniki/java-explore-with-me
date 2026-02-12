package ru.practicum.explorewithme.server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.explorewithme.compilation.dto.CompilationDto;
import ru.practicum.explorewithme.event.dto.EventShortDto;
import ru.practicum.explorewithme.server.entity.Compilation;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CompilationMapper {

    @Mapping(source = "compilation.id", target = "id")
    @Mapping(source = "compilation.pinned", target = "pinned")
    @Mapping(source = "compilation.title", target = "title")
    @Mapping(source = "events", target = "events")
    CompilationDto toDto(Compilation compilation, List<EventShortDto> events);
}
